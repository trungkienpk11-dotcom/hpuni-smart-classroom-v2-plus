package com.smartclassroom.assistant;

import com.smartclassroom.catalog.CatalogRepository;
import com.smartclassroom.catalog.Course;
import com.smartclassroom.catalog.Teacher;
import com.smartclassroom.room.Room;
import com.smartclassroom.room.RoomRepository;
import com.smartclassroom.schedule.ScheduleRepository;
import com.smartclassroom.schedule.ScheduleSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bài 10.3 - Capybara HPUNI: trợ lý quy tắc có truy vấn thật, KHÔNG cần API key.
 * Chỉ gọi các phương thức đọc dữ liệu; KHÔNG có INSERT/UPDATE/DELETE.
 */
@Service
@Transactional(readOnly = true)
public class CapybaraChatService {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Pattern ROOM_CODE = Pattern.compile("\\b[A-Z][0-9]{3,5}\\b");
    private static final Pattern ISO_DATE = Pattern.compile("\\b(\\d{4}-\\d{2}-\\d{2})\\b");
    private static final Pattern VN_DATE = Pattern.compile("\\b(\\d{1,2}/\\d{1,2}/\\d{4})\\b");
    private static final Pattern TIME = Pattern.compile("(?<![0-9])([01]?[0-9]|2[0-3])(?::([0-5][0-9])|h([0-5][0-9])?|\\s*gio)(?![0-9])");
    private static final Pattern CAPACITY = Pattern.compile("(?<![0-9:h])\\b([1-9][0-9]{0,3})\\s*(?:sinh vien|sv|nguoi|cho)\\b|\\bsi so\\s*:?\\s*([1-9][0-9]{0,3})\\b");
    private static final DateTimeFormatter VN_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final List<String> DEFAULT_HINTS = List.of(
            "Tìm phòng trống ngày mai lúc 09:00 cho 40 sinh viên",
            "Lịch học tuần này", "Thống kê khoa", "Mở thời khóa biểu");

    private final RoomRepository rooms;
    private final ScheduleRepository schedules;
    private final CatalogRepository catalog;

    public CapybaraChatService(RoomRepository rooms,
                               ScheduleRepository schedules,
                               CatalogRepository catalog) {
        this.rooms = rooms;
        this.schedules = schedules;
        this.catalog = catalog;
    }

    public ChatReply answer(ChatRequest request) {
        if (request == null || request.message() == null ||
                request.message().isBlank() || request.message().length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tin nhắn phải có từ 1 đến 500 ký tự.");
        }

        String raw = request.message().trim();
        String q = plain(raw);

        // 1) Điều hướng là hành động click ở frontend, KHÔNG ghi dữ liệu.
        String page = navigation(q);
        if (page != null) {
            return navigate("Mình đã tìm thấy mục phù hợp: " + pageLabel(page) +
                    ". Nhấn nút bên dưới để chuyển đến trang này. Mình không tự thay đổi dữ liệu.", page);
        }

        // 2) Hướng dẫn thao tác theo chức năng hiện có trong V2+.
        CapybaraWebGuide.Topic guide = CapybaraWebGuide.explain(q);
        if (guide != null) return guide.toReply();

        // 3) Câu hỏi dùng dữ liệu thật trong database.
        if (containsAny(q, "phong trong", "phong con trong", "tim phong", "phong ranh", "phong nao trong")) {
            return availableRooms(q);
        }
        Matcher roomMatch = ROOM_CODE.matcher(raw.toUpperCase(Locale.ROOT));
        if (roomMatch.find() && (q.contains("phong") || q.matches("[a-z][0-9]{3,5}"))) {
            return roomDetail(roomMatch.group());
        }
        if (containsAny(q, "phong duoc dung nhieu", "phong su dung nhieu", "phong nao duoc xep nhieu", "phong nhieu lich", "phong nao duoc dung nhieu", "phong nao su dung nhieu")) {
            return busiestRooms(q);
        }
        if (containsAny(q, "xep lich", "lap lich tu dong", "lich thu", "xep thoi khoa bieu")) {
            return navigate("Xếp lịch tự động gồm ba bước: chọn tuần/học kỳ, bấm Xếp lịch thử để xem đề xuất, " +
                    "sau đó kiểm tra cảnh báo và chỉ phê duyệt lưu khi đã xác nhận lịch hợp lý.", "autoSchedule");
        }
        if (containsAny(q, "lich hoc", "thoi khoa bieu", "lich giang", "buoi hoc", "lich tuan")) {
            return schedule(q, raw);
        }
        if (containsAny(q, "thong ke", "bao cao", "bao nhieu", "tong so", "so luong", "tong quan")) {
            return summary();
        }
        if (containsAny(q, "giang vien", "giao vien", "danh sach thay")) {
            return teachers();
        }
        if (containsAny(q, "lop hoc phan", "hoc phan")) {
            return offerings();
        }
        if (containsAny(q, "mon hoc", "danh sach mon")) {
            return courses();
        }
        if (containsAny(q, "lop sinh vien", "danh sach lop")) {
            return groups();
        }
        if (containsAny(q, "danh sach phong", "co bao nhieu phong", "phong hoc", "may tinh", "may chieu")) {
            return listRooms(q);
        }
        if (containsAny(q, "huong dan", "lam sao", "cach su dung", "tro giup", "ban lam duoc gi", "giup do", "chuc nang", "lam duoc nhung gi")) {
            return help();
        }
        if (containsAny(q, "chao", "hello", "xin chao", "cam on", "xin loi") || q.equals("hi")) {
            return reply("Xin chào! 🐾 Mình là capybara hpuni. Mình giúp bạn tra cứu phòng và lịch thật, " +
                    "giải thích từng chức năng của website, và có nút đưa bạn tới trang tương ứng. " +
                    "Mình không tự thêm/sửa/xóa hoặc duyệt lịch.", DEFAULT_HINTS);
        }
        return new ChatReply("Mình chưa hiểu rõ yêu cầu này bằng bộ quy tắc nội bộ. " +
                "Bạn có thể hỏi 'Cách thêm giảng viên', 'Cách xác nhận lịch', 'Tìm phòng trống ngày mai lúc 9 giờ', " +
                "hoặc 'Mở trang thống kê báo cáo'.", "unhandled", null,
                List.of("Bạn làm được gì?", "Cách xếp lịch thử", "Mở quản lý phòng học", "Mở thống kê báo cáo"));
    }

    private ChatReply roomDetail(String code) {
        return rooms.findByCode(code)
                .map(room -> reply("Thông tin phòng " + room.code() + ": " + room.name() +
                        ", " + room.capacity() + " chỗ, máy tính: " + yesNo(room.computers()) +
                        ", máy chiếu: " + yesNo(room.projector()) +
                        ". Đây là thông tin phòng, chưa phải xác nhận còn trống.",
                        List.of("Tìm phòng trống ngày mai lúc 09:00 cho 40 sinh viên", "Mở quản lý phòng học")))
                .orElseGet(() -> reply("Chưa tìm thấy phòng " + code + " trong dữ liệu hiện tại.",
                        List.of("Danh sách phòng học")));
    }

    private ChatReply listRooms(String q) {
        List<Room> found = rooms.findAll().stream()
                .filter(r -> !containsAny(q, "may tinh") || Boolean.TRUE.equals(r.computers()))
                .filter(r -> !containsAny(q, "may chieu") || Boolean.TRUE.equals(r.projector()))
                .toList();
        if (found.isEmpty()) return reply("Chưa có phòng nào đáp ứng yêu cầu trong cơ sở dữ liệu.",
                List.of("Danh sách phòng học", "Mở quản lý phòng học"));
        StringBuilder b = new StringBuilder("Đang có ").append(found.size()).append(" phòng phù hợp trong dữ liệu:");
        found.stream().limit(10).forEach(r -> b.append("\n• ").append(r.code())
                .append(" — ").append(r.name()).append(" (").append(r.capacity()).append(" chỗ)")
                .append(Boolean.TRUE.equals(r.computers()) ? ", máy tính" : "")
                .append(Boolean.TRUE.equals(r.projector()) ? ", máy chiếu" : ""));
        if (found.size() > 10) b.append("\n… Và ").append(found.size() - 10).append(" phòng khác.");
        b.append("\nLưu ý: chưa kiểm tra trống theo giờ.");
        return reply(b.toString(), List.of("Mở quản lý phòng học", "Tìm phòng trống ngày mai lúc 09:00 cho 40 sinh viên"));
    }

    private ChatReply availableRooms(String q) {
        LocalDate date;
        try { date = detectDate(q); }
        catch (java.time.DateTimeException error) {
            return reply("Ngày không hợp lệ. Hãy nhập ngày dạng 09/11/2026 hoặc 2026-11-09.",
                    List.of("Tìm phòng trống ngày mai lúc 09:00 cho 40 sinh viên"));
        }
        if (date == null) return reply("Bạn muốn tìm phòng vào ngày nào? " +
                "Ví dụ: 'Tìm phòng trống ngày mai lúc 09:00 cho 40 sinh viên'.",
                List.of("Tìm phòng trống ngày mai lúc 09:00 cho 40 sinh viên"));
        List<LocalTime> times = parseTimes(q);
        if (times.isEmpty()) return reply("Bạn muốn tìm phòng lúc mấy giờ? " +
                "Hãy gửi cả ngày và giờ, ví dụ: 'Tìm phòng trống 09/11/2026 lúc 09:00 cho 40 sinh viên'.",
                List.of("Tìm phòng trống ngày mai lúc 09:00 cho 40 sinh viên"));
        LocalTime start = times.get(0);
        LocalTime end;
        try { end = times.size() > 1 ? times.get(1) : start.plusMinutes(90); }
        catch (java.time.DateTimeException ex) { return reply("Giờ bắt đầu không hợp lệ.", DEFAULT_HINTS); }
        if (!start.isBefore(end)) {
            return reply("Giờ kết thúc phải sau giờ bắt đầu trong cùng ngày. Ví dụ: '09:00 đến 10:30'.",
                    List.of("Tìm phòng trống ngày mai 09:00 đến 10:30 cho 40 sinh viên"));
        }
        int cap = parseCapacity(q);
        boolean computers = containsAny(q, "may tinh", "phong may") && !q.contains("khong can may tinh");
        boolean projector = q.contains("may chieu") && !q.contains("khong can may chieu");
        List<Room> matches = schedules.findAvailableRooms(date, start, end, cap).stream()
                .filter(r -> !computers || Boolean.TRUE.equals(r.computers()))
                .filter(r -> !projector || Boolean.TRUE.equals(r.projector()))
                .toList();
        String conditions = "ngày " + date.format(VN_FORMAT) + ", " + start + "–" + end +
                ", ít nhất " + cap + " chỗ" + (computers ? ", có máy tính" : "") +
                (projector ? ", có máy chiếu" : "");
        if (matches.isEmpty()) {
            return navigate("Không tìm thấy phòng trống phù hợp " + conditions +
                    ". Bạn có thể thử đổi giờ, sức chứa hoặc thiết bị.", "availability");
        }
        StringBuilder b = new StringBuilder("Có ").append(matches.size())
                .append(" phòng trống theo lịch đã lưu (" ).append(conditions).append("):");
        matches.stream().limit(8).forEach(r -> b.append("\n• ").append(r.code())
                .append(" — ").append(r.name()).append(" (").append(r.capacity()).append(" chỗ)"));
        if (matches.size() > 8) b.append("\n… Và ").append(matches.size() - 8).append(" phòng khác.");
        if (times.size() == 1) b.append("\nMặc định kiểm tra 90 phút vì bạn chưa nêu giờ kết thúc.");
        if (cap == 1) b.append("\nBạn chưa nêu sĩ số, nên dùng mức tối thiểu 1 chỗ.");
        b.append("\nLưu ý: dựa trên lịch học đã lưu, chưa xét đặt phòng ngoài hệ thống.");
        return reply(b.toString(), List.of("Mở tìm phòng trống", "Mở thời khóa biểu"));
    }

    private ChatReply schedule(String q, String raw) {
        LocalDate d;
        try { d = detectDate(q); }
        catch (java.time.DateTimeException e) {
            return reply("Ngày không hợp lệ. Hãy dùng dd/MM/yyyy hoặc yyyy-MM-dd.", List.of("Lịch học tuần này"));
        }
        LocalDate today = LocalDate.now(VN_ZONE);
        if (d == null) d = today;
        LocalDate monday = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        boolean todayOnly = q.contains("hom nay") || q.contains("ngay mai") || ISO_DATE.matcher(q).find() || VN_DATE.matcher(q).find();
        List<ScheduleSession> all = schedules.findWeek(monday);
        final LocalDate selectedDate = d;
        List<ScheduleSession> filtered = todayOnly ? all.stream().filter(s -> selectedDate.equals(s.sessionDate())).toList() : all;
        // Lọc theo mã phòng, mã lớp hoặc tên giảng viên nếu câu hỏi có nêu rõ.
        Matcher code = ROOM_CODE.matcher(raw.toUpperCase(Locale.ROOT));
        if (code.find()) {
            String room = code.group();
            filtered = filtered.stream().filter(s -> room.equalsIgnoreCase(s.roomCode())).toList();
        }
        for (Teacher teacher : catalog.findTeachers()) {
            String teacherName = plain(teacher.fullName());
            if (q.contains(teacherName)) {
                filtered = filtered.stream().filter(s -> plain(s.teacherName()).equals(teacherName)).toList();
                break;
            }
        }
        if (filtered.isEmpty()) return navigate("Không thấy buổi học phù hợp " +
                (todayOnly ? "ngày " + d.format(VN_FORMAT) : "tuần " + monday.format(VN_FORMAT)) +
                " trong lịch đã lưu. Hãy mở Thời khóa biểu để kiểm tra thêm.", "schedule");
        StringBuilder b = new StringBuilder("Có ").append(filtered.size()).append(" buổi học ")
                .append(todayOnly ? "ngày " + d.format(VN_FORMAT) : "trong tuần " + monday.format(VN_FORMAT)).append(":");
        filtered.stream().limit(9).forEach(s -> b.append("\n• ").append(s.sessionDate().format(VN_FORMAT))
                .append(" ").append(s.startTime()).append("–").append(s.endTime())
                .append(" | ").append(s.courseName()).append(" / ").append(s.groupCode())
                .append(" | Phòng ").append(s.roomCode())
                .append(" | GV: ").append(s.teacherName()));
        if (filtered.size() > 9) b.append("\n… Và ").append(filtered.size()-9).append(" buổi khác.");
        return reply(b.toString(), List.of("Mở thời khóa biểu", "Thống kê khoa"));
    }

    /** Thống kê sử dụng phòng từ lịch đã xác nhận lưu trong tuần. */
    private ChatReply busiestRooms(String q) {
        LocalDate today = LocalDate.now(VN_ZONE);
        LocalDate d = q.contains("tuan sau") ? today.plusWeeks(1) : today;
        LocalDate monday = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Map<String, Long> counts = new java.util.HashMap<>();
        for (ScheduleSession session : schedules.findWeek(monday)) {
            counts.merge(session.roomCode(), 1L, Long::sum);
        }
        if (counts.isEmpty()) {
            return navigate("Tuần " + monday.format(VN_FORMAT) + " chưa có buổi học đã lưu để thống kê sử dụng phòng.", "reports");
        }
        StringBuilder b = new StringBuilder("Các phòng được xếp nhiều buổi nhất tuần ")
                .append(monday.format(VN_FORMAT)).append(":");
        counts.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5).forEach(e -> b.append("\n• ").append(e.getKey()).append(": ").append(e.getValue()).append(" buổi"));
        b.append("\nSố liệu dựa trên lịch đã lưu, không phải thống kê sử dụng phòng thực tế.");
        return navigate(b.toString(), "reports");
    }

    private ChatReply summary() {
        int roomCount = rooms.findAll().size();
        int teacherCount = catalog.findTeachers().size();
        int groupCount = catalog.findGroups().size();
        int courseCount = catalog.findCourses().size();
        int offeringCount = catalog.findOfferings().size();
        LocalDate monday = LocalDate.now(VN_ZONE).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<ScheduleSession> sessions = schedules.findWeek(monday);
        long usedRooms = sessions.stream().map(ScheduleSession::roomId).distinct().count();
        return navigate("📊 Thống kê dữ liệu hiện tại của HPUNI:\n" +
                "• " + roomCount + " phòng học\n" +
                "• " + teacherCount + " giảng viên\n" +
                "• " + groupCount + " lớp sinh viên\n" +
                "• " + courseCount + " môn học\n" +
                "• " + offeringCount + " lớp học phần\n" +
                "• Tuần " + monday.format(VN_FORMAT) + ": " + sessions.size() +
                " buổi đã lưu, " + usedRooms + " phòng được sử dụng.\n" +
                "Đây là thống kê theo dữ liệu hệ thống, không phải đo mức sử dụng thực tế.", "reports");
    }

    private ChatReply teachers() {
        List<Teacher> teachers = catalog.findTeachers();
        StringBuilder b = new StringBuilder("Hệ thống hiện có ").append(teachers.size()).append(" giảng viên:");
        teachers.stream().limit(10).forEach(t -> b.append("\n• ").append(t.code()).append(" — ").append(t.fullName()));
        if (teachers.size() > 10) b.append("\n… Và ").append(teachers.size()-10).append(" giảng viên khác.");
        return reply(b.toString(), List.of("Mở quản lý giảng viên", "Lịch học tuần này"));
    }

    private ChatReply courses() {
        List<Course> courses = catalog.findCourses();
        StringBuilder b = new StringBuilder("Hệ thống hiện có ").append(courses.size()).append(" môn học:");
        courses.stream().limit(10).forEach(c -> b.append("\n• ").append(c.code()).append(" — ").append(c.name()));
        if (courses.size() > 10) b.append("\n… Và ").append(courses.size()-10).append(" môn khác.");
        return reply(b.toString(), List.of("Mở quản lý môn học"));
    }

    private ChatReply groups() {
        var groups = catalog.findGroups();
        StringBuilder b = new StringBuilder("Có ").append(groups.size()).append(" lớp sinh viên:");
        groups.stream().limit(10).forEach(g -> b.append("\n• ").append(g.code())
                .append(" — ").append(g.name()).append(" (").append(g.studentCount()).append(" SV)"));
        if (groups.size() > 10) b.append("\n… Và ").append(groups.size()-10).append(" lớp khác.");
        return reply(b.toString(), List.of("Mở quản lý lớp sinh viên"));
    }

    private ChatReply offerings() {
        var offerings = catalog.findOfferings();
        StringBuilder b = new StringBuilder("Có ").append(offerings.size()).append(" lớp học phần:");
        offerings.stream().limit(8).forEach(o -> b.append("\n• ").append(o.courseName())
                .append(" / ").append(o.groupCode()).append(" / GV: ").append(o.teacherName()));
        if (offerings.size() > 8) b.append("\n… Và ").append(offerings.size()-8).append(" lớp học phần khác.");
        return reply(b.toString(), List.of("Mở quản lý lớp học phần", "Mở xếp lịch tự động"));
    }

    private ChatReply help() {
        return reply("Mình là capybara hpuni 🐾 — trợ lý theo quy tắc, không cần API key.\n" +
                "CÁC NHÓM CHỨC NĂNG MÌNH HỖ TRỢ:\n" +
                "• Tra cứu thật: phòng học, máy chiếu/máy tính, giảng viên, môn học, lớp sinh viên, lớp học phần.\n" +
                "• Tìm phòng trống theo ngày, giờ, sĩ số và thiết bị (theo lịch đã lưu).\n" +
                "• Xem lịch tuần này/tuần sau, thống kê khoa, phòng sử dụng nhiều nhất.\n" +
                "• Hướng dẫn thao tác: thêm/sửa/xóa danh mục, tạo lớp học phần, xếp thử, phê duyệt, kiểm tra trùng lịch, xuất CSV, quy tắc lịch bận.\n" +
                "• Điều hướng: 'Mở quản lý phòng học', 'Cho mình vào trang giảng viên', 'Đưa mình đến thống kê báo cáo'...\n" +
                "Lưu ý: mình không tự thay đổi dữ liệu, không thay thế người duyệt lịch và không trả lời tự do như AI tạo sinh.",
                List.of("Cách xếp lịch thử", "Cách thêm giảng viên", "Tìm phòng trống ngày mai lúc 09:00 cho 40 sinh viên", "Mở thống kê báo cáo"));
    }

    /** Chỉ trả về page ID đã được whitelist bên frontend. */
    private static String navigation(String q) {
        // Câu hỏi 'cách mở trang' là hướng dẫn, không tự nhận thành lệnh mở trang.
        if (containsAny(q, "cach mo", "huong dan mo", "lam sao mo", "cach vao", "lam sao vao")) return null;
        boolean wantsPage = q.startsWith("mo ") || q.startsWith("hay mo ") ||
                q.startsWith("cho minh mo ") || q.startsWith("mo giup ") ||
                q.startsWith("vao ") || q.startsWith("cho minh vao ") ||
                q.startsWith("toi muon vao ") || q.startsWith("minh muon vao ") ||
                q.startsWith("toi muon mo ") || q.startsWith("minh muon mo ") ||
                q.startsWith("chuyen ") || q.startsWith("chuyen sang ") ||
                q.startsWith("di toi ") || q.startsWith("den ") ||
                q.startsWith("dua minh den ") || q.startsWith("dua minh toi ") || q.startsWith("dua toi den ") ||
                q.startsWith("cho minh den ") || q.startsWith("cho minh toi ") ||
                q.startsWith("dan minh toi ") || q.startsWith("truy cap ") ||
                q.startsWith("vao trang ") || q.startsWith("den trang ") ||
                q.startsWith("toi trang ") || q.startsWith("toi muon xem trang ") ||
                q.startsWith("cho minh xem trang ") || q.startsWith("bat trang ") ||
                q.startsWith("hien trang ") || q.startsWith("link den ") ||
                q.startsWith("cho minh link ") || q.startsWith("toi can den ") ||
                q.startsWith("minh can den ") || q.startsWith("giup minh di den ") ||
                q.startsWith("minh muon truy cap ") || q.startsWith("toi muon truy cap ") ||
                q.startsWith("chi duong toi ");
        if (!wantsPage) return null;
        if (containsAny(q, "xep lich", "lap lich tu dong", "xep thoi khoa")) return "autoSchedule";
        if (containsAny(q, "phong trong", "tim phong", "phong ranh", "con trong")) return "availability";
        if (containsAny(q, "thoi khoa", "lich hoc", "lich giang", "buoi hoc")) return "schedule";
        if (containsAny(q, "lop hoc phan", "hoc phan", "lop mon")) return "offerings";
        if (containsAny(q, "lop sinh vien", "danh sach lop", "quan ly lop")) return "groups";
        if (containsAny(q, "giang vien", "giao vien", "quan ly thay")) return "teachers";
        if (containsAny(q, "mon hoc", "quan ly mon")) return "courses";
        if (containsAny(q, "phong hoc", "quan ly phong", "danh sach phong")) return "rooms";
        if (containsAny(q, "thong ke", "bao cao", "bieu do")) return "reports";
        if (containsAny(q, "tong quan", "trang chu", "dashboard")) return "dashboard";
        return null;
    }

    private static String pageLabel(String page) {
        return switch (page) {
            case "dashboard" -> "Tổng quan";
            case "rooms" -> "Quản lý phòng học";
            case "teachers" -> "Quản lý giảng viên";
            case "groups" -> "Quản lý lớp sinh viên";
            case "courses" -> "Quản lý môn học";
            case "offerings" -> "Quản lý lớp học phần";
            case "schedule" -> "Thời khóa biểu";
            case "autoSchedule" -> "Xếp lịch tự động";
            case "availability" -> "Tìm phòng trống";
            case "reports" -> "Thống kê & báo cáo";
            default -> "chức năng";
        };
    }

    private static List<LocalTime> parseTimes(String q) {
        List<LocalTime> result = new ArrayList<>();
        Matcher m = TIME.matcher(q);
        while (m.find() && result.size() < 2) {
            int hour = Integer.parseInt(m.group(1));
            String minutes = m.group(2) != null ? m.group(2) : m.group(3);
            result.add(LocalTime.of(hour, minutes == null ? 0 : Integer.parseInt(minutes)));
        }
        return result;
    }

    private static LocalDate detectDate(String q) {
        Matcher isoMatch = ISO_DATE.matcher(q);
        if (isoMatch.find()) return LocalDate.parse(isoMatch.group(1));
        Matcher vnMatch = VN_DATE.matcher(q);
        if (vnMatch.find()) return LocalDate.parse(vnMatch.group(1), VN_FORMAT);
        LocalDate today = LocalDate.now(VN_ZONE);
        if (q.contains("ngay mai")) return today.plusDays(1);
        if (q.contains("hom nay")) return today;
        if (q.contains("tuan sau")) return today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        if (q.contains("tuan nay")) return today;
        return null;
    }

    private static int parseCapacity(String q) {
        Matcher m = CAPACITY.matcher(q);
        if (m.find()) return Integer.parseInt(m.group(1) == null ? m.group(2) : m.group(1));
        return 1;
    }

    private static String plain(String value) {
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd');
    }

    private static boolean containsAny(String source, String... needles) {
        for (String needle : needles) if (source.contains(needle)) return true;
        return false;
    }

    private static String yesNo(Boolean value) { return Boolean.TRUE.equals(value) ? "có" : "không"; }

    private static ChatReply reply(String text, List<String> hints) {
        return new ChatReply(text, "rule-based", null, hints);
    }

    private static ChatReply navigate(String text, String page) {
        return new ChatReply(text, "rule-based", Map.of("type", "navigate", "page", page), DEFAULT_HINTS);
    }
}
