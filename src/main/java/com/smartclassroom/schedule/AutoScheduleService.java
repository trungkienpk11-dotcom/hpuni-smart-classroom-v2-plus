package com.smartclassroom.schedule;

import com.smartclassroom.room.Room;
import com.smartclassroom.room.RoomRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Comparator;
import java.time.temporal.ChronoUnit;
@Service
public class AutoScheduleService {

    private final AutoScheduleRepository repository;
    private final RoomRepository roomRepository;
    private final ScheduleService scheduleService;
    private final SchedulingRuleService ruleService;


    public AutoScheduleService(
            AutoScheduleRepository repository,
            RoomRepository roomRepository,
            ScheduleService scheduleService,
            SchedulingRuleService ruleService) {

        this.repository = repository;
        this.roomRepository = roomRepository;
        this.scheduleService = scheduleService;
        this.ruleService = ruleService;
    }


    // Các khung giờ được phép xếp
    private record TimeSlot(
            LocalTime start,
            LocalTime end
    ) {}

    private static final List<TimeSlot> SLOTS = List.of(
            new TimeSlot(
                    LocalTime.of(7, 0),
                    LocalTime.of(9, 0)
            ),
            new TimeSlot(
                    LocalTime.of(9, 15),
                    LocalTime.of(11, 15)
            ),
            new TimeSlot(
                    LocalTime.of(13, 0),
                    LocalTime.of(15, 0)
            ),
            new TimeSlot(
                    LocalTime.of(15, 15),
                    LocalTime.of(17, 15)
            )
    );

    // Một phương án đang được thuật toán đánh giá

    private record Candidate(
            Room room,
            LocalDate date,
            LocalTime start,
            LocalTime end,
            long score,
            String explanation
    ) {}


    // 1. XEM TRƯỚC, KHÔNG GHI DATABASE
    public AutoSchedulePlan preview(
            AutoScheduleRequest request) {

        return generate(request);
    }

    // 2. XÁC NHẬN VÀ LƯU LỊCH ĐƯỢC ĐỀ XUẤT
    @Transactional
    public AutoSchedulePlan apply(
            AutoScheduleRequest request) {

        // Tính lại bằng dữ liệu hiện tại
        AutoSchedulePlan plan = generate(request);

        for (AutoScheduleSuggestion suggestion
                : plan.suggestions()) {

            ScheduleRequest newSession = new ScheduleRequest(
                    suggestion.offeringId(),
                    suggestion.roomId(),
                    suggestion.sessionDate(),
                    suggestion.startTime(),
                    suggestion.endTime()
            );

            // Dùng kiểm tra nghiệp vụ ở Bài 7
            scheduleService.create(newSession);
        }

        return plan;
    }

    // 3. THUẬT TOÁN TẠO PHƯƠNG ÁN
    private AutoSchedulePlan generate(
            AutoScheduleRequest request) {

        AutoScheduleRequest input = validate(request);

        LocalDate monday = input.weekStart();

        var teacherBusy = ruleService.getBusyForWeek(monday);


        var offerings = repository.findOfferings(
                input.semester(),
                input.academicYear()
        );


        var existing = repository.findOccupied(monday);

        var occupied = new ArrayList<>(existing);


        List<Room> rooms =
                new ArrayList<>(roomRepository.findAll());

        rooms.sort(Comparator.comparing(Room::id));


        List<AutoScheduleSuggestion> suggestions =
                new ArrayList<>();

        List<String> unscheduled = new ArrayList<>();

        int existingSessions = 0;
        int neededSessions = 0;

        if (offerings.isEmpty()) {
            unscheduled.add(
                    "Không có lớp học phần trong học kỳ đã chọn"
            );
        }

        // Xử lý từng lớp học phần
        for (AutoScheduleRepository.Offering offering
                : offerings) {

            // Đếm số buổi đã xếp của học phần này

            long alreadyScheduled = existing.stream()
                    .filter(s ->
                            s.offeringId() == offering.id()
                    )
                    .count();

            existingSessions += (int) alreadyScheduled;

// Cảnh báo nếu đã xếp quá số buổi quy định
            if (alreadyScheduled > offering.sessionsPerWeek()) {

                unscheduled.add(
                        offering.courseName() + " - " +
                                offering.groupCode() +
                                ": đã có " + alreadyScheduled +
                                " buổi, vượt định mức " +
                                offering.sessionsPerWeek() +
                                " buổi/tuần. Cần kiểm tra lại."
                );
            }

            int remaining = Math.max(
                    0,
                    offering.sessionsPerWeek()
                            - (int) alreadyScheduled
            );

            neededSessions += remaining;

            // Xếp các buổi còn thiếu
            for (int i = 0; i < remaining; i++) {

                Candidate best = findBestCandidate(
                        offering,
                        monday,
                        rooms,
                        occupied,
                        teacherBusy
                );


                if (best == null) {

                    int notPlaced = remaining - i;

                    unscheduled.add(
                            offering.courseName() + " - " +
                                    offering.groupCode() +
                                    ": còn " + notPlaced +
                                    " buổi chưa xếp được"
                    );

                    break;
                }

                // Thêm đề xuất vào kết quả
                suggestions.add(
                        new AutoScheduleSuggestion(
                                offering.id(),
                                offering.courseName(),
                                offering.teacherName(),
                                offering.groupCode(),
                                best.room().id(),
                                best.room().code(),
                                best.date(),
                                best.start(),
                                best.end(),
                                best.explanation()
                        )
                );

                // Giữ chỗ trong bộ nhớ để các buổi
                // tiếp theo không xếp trùng
                occupied.add(
                        new AutoScheduleRepository.Occupied(
                                offering.id(),
                                best.room().id(),
                                offering.teacherId(),
                                offering.groupId(),
                                best.date(),
                                best.start(),
                                best.end()
                        )
                );
            }
        }

        return new AutoSchedulePlan(
                monday,
                existingSessions,
                neededSessions,
                suggestions,
                unscheduled
        );
    }

    // 4. TÌM PHƯƠNG ÁN PHÙ HỢP NHẤT

    private Candidate findBestCandidate(
            AutoScheduleRepository.Offering offering,
            LocalDate monday,
            List<Room> rooms,
            List<AutoScheduleRepository.Occupied> occupied,
            List<SchedulingRuleService.TeacherBusy> teacherBusy) {

        Candidate best = null;

        // Duyệt từ thứ Hai đến thứ Bảy
        for (int day = 0; day < 6; day++) {

            LocalDate date = monday.plusDays(day);

            // Mỗi học phần tối đa một buổi/ngày
            boolean hasSessionThatDay = occupied.stream()
                    .anyMatch(s ->
                            s.offeringId() == offering.id()
                                    && s.date().equals(date)
                    );

            if (hasSessionThatDay) {
                continue;
            }

            // Tính khoảng cách với các buổi cùng học phần
            long minGap = occupied.stream()
                    .filter(s -> s.offeringId() == offering.id())
                    .mapToLong(s ->
                            Math.abs(
                                    ChronoUnit.DAYS.between(
                                            s.date(), date
                                    )
                            )
                    )
                    .min()
                    .orElse(99L);

            // Duyệt các ca học
            for (int slotIndex = 0;
                 slotIndex < SLOTS.size();
                 slotIndex++) {

                TimeSlot slot = SLOTS.get(slotIndex);

                // Kiểm tra giảng viên có bận không
                boolean busy = teacherBusy.stream()
                        .anyMatch(b ->
                                b.teacherId() == offering.teacherId()
                                        && b.date().equals(date)
                                        && overlaps(
                                        b.start(),
                                        b.end(),
                                        slot.start(),
                                        slot.end()
                                )
                        );

                if (busy) {
                    continue;
                }

                // Duyệt các phòng học
                for (Room room : rooms) {

                    // Quy tắc 1: Phòng đủ sức chứa
                    if (room.capacity()
                            < offering.studentCount()) {
                        continue;
                    }

                    // Quy tắc 2: Môn cần máy tính
                    if (offering.needsComputers()
                            && !room.computers()) {
                        continue;
                    }

                    // Quy tắc 3: Môn cần máy chiếu
                    if (offering.needsProjector()
                            && !room.projector()) {
                        continue;
                    }

                    // Quy tắc 4: Không trùng lịch
                    boolean conflict = occupied.stream()
                            .anyMatch(s ->
                                    s.date().equals(date)
                                            && overlaps(
                                            s.start(),
                                            s.end(),
                                            slot.start(),
                                            slot.end()
                                    )
                                            && (
                                            s.roomId() == room.id()
                                                    || s.teacherId()
                                                    == offering.teacherId()
                                                    || s.groupId()
                                                    == offering.groupId()
                                    )
                            );

                    if (conflict) {
                        continue;
                    }

                    // Đếm tổng buổi đã xếp trong ngày
                    long dailyLoad = occupied.stream()
                            .filter(s -> s.date().equals(date))
                            .count();

                    // Đếm buổi của chính lớp sinh viên
                    long groupDailyLoad = occupied.stream()
                            .filter(s ->
                                    s.date().equals(date)
                                            && s.groupId() == offering.groupId()
                            )
                            .count();

                    int wastedSeats =
                            room.capacity() - offering.studentCount();

                    // Phạt nặng nếu cùng môn học hai ngày liền
                    long gapPenalty = minGap == 1 ? 500 : 0;

                    // Ưu tiên phân tán lịch và phòng vừa đủ
                    long score =
                            wastedSeats * 3L
                                    + dailyLoad * 5L
                                    + groupDailyLoad * 25L
                                    + gapPenalty
                                    + day * 2L
                                    + slotIndex;

                    String spacing = minGap == 99
                            ? "buổi đầu tiên trong tuần"
                            : "cách buổi cùng môn gần nhất " +
                            minGap + " ngày";

                    String explanation =
                            "Phòng " + room.code() +
                                    " đủ " + room.capacity() +
                                    " chỗ; dư " + wastedSeats +
                                    " ghế; " + spacing +
                                    "; không trùng lịch hoặc giờ bận; " +
                                    "điểm " + score;

                    Candidate candidate = new Candidate(
                            room,
                            date,
                            slot.start(),
                            slot.end(),
                            score,
                            explanation
                    );

                    if (best == null ||
                            candidate.score() < best.score()) {

                        best = candidate;
                    }
                }
            }
        }

        return best;
    }

    // 5. KIỂM TRA HAI KHOẢNG GIỜ GIAO NHAU
    private boolean overlaps(
            LocalTime start1,
            LocalTime end1,
            LocalTime start2,
            LocalTime end2) {

        return start1.isBefore(end2)
                && end1.isAfter(start2);
    }

    // 6. KIỂM TRA THÔNG TIN YÊU CẦU
    private AutoScheduleRequest validate(
            AutoScheduleRequest request) {

        if (request == null ||
                request.weekStart() == null) {

            throw badRequest("Thiếu tuần cần xếp lịch");
        }

        String semester = request.semester() == null
                ? ""
                : request.semester()
                .trim()
                .toUpperCase(Locale.ROOT);

        String year = request.academicYear() == null
                ? ""
                : request.academicYear().trim();

        if (!Set.of("HK1", "HK2", "HK3")
                .contains(semester)) {

            throw badRequest("Học kỳ không hợp lệ");
        }

        if (!year.matches("20\\d{2}-20\\d{2}")) {
            throw badRequest(
                    "Năm học phải có dạng 2026-2027"
            );
        }

        int startYear = Integer.parseInt(
                year.substring(0, 4)
        );

        int endYear = Integer.parseInt(
                year.substring(5, 9)
        );

        if (endYear != startYear + 1) {
            throw badRequest(
                    "Năm kết thúc phải liền sau năm bắt đầu"
            );
        }

        LocalDate monday = request.weekStart().with(
                TemporalAdjusters.previousOrSame(
                        DayOfWeek.MONDAY
                )
        );

        return new AutoScheduleRequest(
                semester,
                year,
                monday
        );
    }

    private ResponseStatusException badRequest(
            String message) {

        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }
}
