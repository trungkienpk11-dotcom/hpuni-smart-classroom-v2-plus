package com.smartclassroom.assistant;

import com.smartclassroom.room.Room;
import com.smartclassroom.room.RoomRepository;
import com.smartclassroom.schedule.ScheduleRepository;
import com.smartclassroom.schedule.ScheduleSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CapybaraChatService {
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Pattern ROOM_CODE = Pattern.compile("\\b[A-Z]\\d{3,5}\\b");

    private final RoomRepository roomRepository;
    private final ScheduleRepository scheduleRepository;

    public CapybaraChatService(RoomRepository roomRepository,
                               ScheduleRepository scheduleRepository) {
        this.roomRepository = roomRepository;
        this.scheduleRepository = scheduleRepository;
    }

    public ChatReply answer(ChatRequest request) {
        if (request == null || request.message() == null ||
                request.message().isBlank() || request.message().length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tin nhắn phải có từ 1 đến 500 ký tự.");
        }

        String input = request.message().trim();
        String q = plain(input);

        if (q.contains("phong trong")) {
            return reply("Để kiểm tra phòng trống chính xác, mình cần ngày, giờ bắt đầu, giờ kết thúc và sức chứa. " +
                    "Bạn có thể dùng mục Thời khóa biểu → Tìm phòng trống. Ở Bài 10.3 mình sẽ nhận các thông tin này trực tiếp trong chat.");
        }

        Matcher codeMatcher = ROOM_CODE.matcher(input.toUpperCase(Locale.ROOT));
        if (q.contains("phong") && codeMatcher.find()) {
            String code = codeMatcher.group();
            return roomRepository.findByCode(code)
                    .map(room -> reply("Phòng " + room.code() + " – " + room.name() +
                            ", sức chứa " + room.capacity() + " chỗ; máy chiếu: " + yesNo(room.projector()) +
                            "; máy tính: " + yesNo(room.computers()) +
                            ". Đây là thông tin phòng, không phải xác nhận phòng đang trống."))
                    .orElseGet(() -> reply("Mình không tìm thấy mã phòng " + code + " trong dữ liệu hiện tại."));
        }

        if (q.contains("phong") || q.contains("may tinh") || q.contains("may chieu")) {
            List<Room> rooms = roomRepository.findAll();
            if (rooms.isEmpty()) return reply("Hiện chưa có phòng học nào trong database.");
            StringBuilder result = new StringBuilder("Hệ thống hiện có " + rooms.size() + " phòng. ");
            rooms.stream().limit(8).forEach(room -> result
                    .append("\n• ").append(room.code())
                    .append(" (sức chứa ").append(room.capacity())
                    .append(", máy tính: ").append(yesNo(room.computers()))
                    .append(")"));
            if (rooms.size() > 8) result.append("\n… và ").append(rooms.size() - 8).append(" phòng khác.");
            return reply(result.toString());
        }

        if (q.contains("lich") || q.contains("thoi khoa bieu")) {
            LocalDate today = LocalDate.now(VN_ZONE);
            LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            List<ScheduleSession> sessions = scheduleRepository.findWeek(monday);
            boolean todayOnly = q.contains("hom nay");
            if (todayOnly) sessions = sessions.stream()
                    .filter(s -> s.sessionDate().equals(today)).toList();
            if (sessions.isEmpty()) return reply(todayOnly
                    ? "Hôm nay chưa có buổi học nào được lưu trong thời khóa biểu."
                    : "Tuần hiện tại (từ " + monday + ") chưa có buổi học nào được lưu.");
            StringBuilder result = new StringBuilder((todayOnly ? "Hôm nay" : "Tuần bắt đầu " + monday) +
                    " có " + sessions.size() + " buổi học:");
            sessions.stream().limit(8).forEach(s -> result
                    .append("\n• ").append(s.sessionDate()).append(" ")
                    .append(s.startTime()).append(" – ")
                    .append(s.courseName()).append("/").append(s.groupCode())
                    .append(" tại ").append(s.roomCode()));
            if (sessions.size() > 8) result.append("\n… còn ").append(sessions.size() - 8).append(" buổi khác.");
            return reply(result.toString());
        }

        if (q.contains("xep lich")) {
            return reply("Để xếp lịch: mở mục Xếp lịch tự động → chọn học kỳ, năm học và tuần → Xếp lịch thử. " +
                    "Kiểm tra kết quả, sau đó mới nhấn Xác nhận lưu lịch. Mình chưa tự sửa thời khóa biểu qua chat.");
        }

        if (q.contains("chao") || q.contains("hello") || q.contains("hi ") || q.equals("hi")) {
            return reply("Chào bạn! Mình là capybara hpuni 🐾. Mình đang ở chế độ cơ bản và có thể đọc dữ liệu phòng học hoặc lịch tuần này. Bạn cần mình giúp gì?");
        }

        return reply("Mình là capybara hpuni 🐾. Hiện mình trả lời theo quy tắc, chưa kết nối mô hình AI. " +
                "Bạn thử hỏi: 'Danh sách phòng học', 'Phòng B201', 'Lịch tuần này' hoặc 'Cách xếp lịch'.");
    }

    private static String plain(String text) {
        return Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd');
    }

    private static String yesNo(Boolean value) {
        return Boolean.TRUE.equals(value) ? "có" : "không";
    }

    private static ChatReply reply(String message) {
        return new ChatReply(message, "basic");
    }
}
