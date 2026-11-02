
package com.smartclassroom.schedule;

import com.smartclassroom.room.Room;
import com.smartclassroom.room.RoomRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
public class ScheduleService {

    private final ScheduleRepository repository;
    private final RoomRepository roomRepository;

    public ScheduleService(
            ScheduleRepository repository,
            RoomRepository roomRepository) {

        this.repository = repository;
        this.roomRepository = roomRepository;
    }

    // 1. Lấy lịch theo tuần
    public List<ScheduleSession> getWeek(LocalDate date) {

        if (date == null) {
            throw badRequest("Thiếu ngày xem lịch");
        }

        LocalDate monday = date.with(
                TemporalAdjusters.previousOrSame(
                        DayOfWeek.MONDAY
                )
        );

        return repository.findWeek(monday);
    }

    // 2. Xem một buổi học
    public ScheduleSession getById(long id) {

        return repository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy buổi học"
                        )
                );
    }

    // 3. Thêm lịch học
    @Transactional
    public ScheduleSession create(ScheduleRequest request) {

        validate(request, -1L);

        try {
            return repository.insert(request);

        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Không thể lưu lịch do ràng buộc dữ liệu"
            );
        }
    }

    // 4. Sửa lịch học
    @Transactional
    public ScheduleSession update(
            long id,
            ScheduleRequest request) {

        getById(id);

        validate(request, id);

        try {
            int updated = repository.update(id, request);

            if (updated == 0) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy buổi học"
                );
            }

            return getById(id);

        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Không thể cập nhật lịch"
            );
        }
    }

    // 5. Xóa lịch học
    public void delete(long id) {

        int deleted = repository.delete(id);

        if (deleted == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy buổi học"
            );
        }
    }

    // 6. Tìm phòng trống
    public List<Room> findAvailableRooms(
            LocalDate date,
            LocalTime start,
            LocalTime end,
            int minCapacity) {

        validateTime(date, start, end);

        if (minCapacity < 1) {
            throw badRequest(
                    "Sức chứa tối thiểu phải lớn hơn 0"
            );
        }

        return repository.findAvailableRooms(
                date, start, end, minCapacity
        );
    }

    // 7. Kiểm tra dữ liệu và phát hiện xung đột
    private void validate(
            ScheduleRequest request,
            long excludeId) {

        if (request == null ||
                request.offeringId() == null ||
                request.roomId() == null ||
                request.offeringId() <= 0 ||
                request.roomId() <= 0) {

            throw badRequest(
                    "Phải chọn lớp học phần và phòng học"
            );
        }

        validateTime(
                request.sessionDate(),
                request.startTime(),
                request.endTime()
        );

        var info = repository
                .findOfferingInfo(request.offeringId())
                .orElseThrow(() ->
                        badRequest("Lớp học phần không tồn tại")
                );

        Room room = roomRepository
                .findById(request.roomId())
                .orElseThrow(() ->
                        badRequest("Phòng học không tồn tại")
                );

        // Kiểm tra sức chứa
        if (room.capacity() < info.studentCount()) {
            throw badRequest(
                    "Phòng " + room.code()
                            + " không đủ sức chứa "
                            + info.studentCount() + " sinh viên"
            );
        }

        LocalDate date = request.sessionDate();
        LocalTime start = request.startTime();
        LocalTime end = request.endTime();

        // Kiểm tra phòng trùng giờ
        if (repository.hasRoomConflict(
                request.roomId(), date,
                start, end, excludeId)) {

            throw conflict(
                    "Phòng học đã có lịch trong khoảng giờ này"
            );
        }

        // Kiểm tra giảng viên trùng giờ
        if (repository.hasTeacherConflict(
                info.teacherId(), date,
                start, end, excludeId)) {

            throw conflict(
                    "Giảng viên đã có lịch trong khoảng giờ này"
            );
        }

        // Kiểm tra lớp sinh viên trùng giờ
        if (repository.hasGroupConflict(
                info.groupId(), date,
                start, end, excludeId)) {

            throw conflict(
                    "Lớp sinh viên đã có lịch trong khoảng giờ này"
            );
        }
    }

    private void validateTime(
            LocalDate date,
            LocalTime start,
            LocalTime end) {

        if (date == null || start == null || end == null) {
            throw badRequest(
                    "Thiếu ngày học hoặc giờ học"
            );
        }

        if (!start.isBefore(end)) {
            throw badRequest(
                    "Giờ bắt đầu phải nhỏ hơn giờ kết thúc"
            );
        }
    }

    private ResponseStatusException badRequest(String msg) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST, msg
        );
    }

    private ResponseStatusException conflict(String msg) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT, msg
        );
    }
}
