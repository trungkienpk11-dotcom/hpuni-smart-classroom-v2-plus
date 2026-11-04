package com.smartclassroom.assistant;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * : hội thoại nhiều lượt để tìm phòng trống, hoàn toàn dựa trên quy tắc.
 * Chỉ lưu các điều kiện tìm phòng trong RAM, KHÔNG lưu nội dung chat hoặc ghi DB.
 * Các kết quả được kiểm tra lại bằng CapybaraChatService (API đọc dữ liệu thật).
 */
@Service
public class CapybaraConversationService {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VN_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    private static final DateTimeFormatter VN_PARSE = DateTimeFormatter.ofPattern("d/M/uuuu")
            .withResolverStyle(java.time.format.ResolverStyle.STRICT);
    private static final Pattern TOKEN = Pattern.compile("[A-Za-z0-9_-]{12,80}");
    private static final Pattern DATE_ISO = Pattern.compile("\\b(\\d{4}-\\d{2}-\\d{2})\\b");
    private static final Pattern DATE_VN = Pattern.compile("\\b(\\d{1,2}/\\d{1,2}/\\d{4})\\b");
    private static final Pattern CLOCK = Pattern.compile("(?<!\\d)([01]?\\d|2[0-3])(?:[:h]([0-5]\\d)?|\\s*gio\\s*([0-5]\\d)?)(?!\\d)");
    private static final Pattern CAPACITY = Pattern.compile("(?<![0-9:h])\\b([1-9]\\d{0,3})\\s*(?:sinh vien|sv|nguoi|cho|hoc sinh)\\b|\\bsi so\\s*:?\\s*([1-9]\\d{0,3})\\b");
    private static final Pattern BARE_NUMBER = Pattern.compile("^([1-9]\\d{0,3})$");
    private static final long TTL_MS = Duration.ofMinutes(20).toMillis();
    private static final int MAX_SESSIONS = 250;

    private final ConcurrentHashMap<String, RoomSearchContext> contexts = new ConcurrentHashMap<>();
    private final CapybaraChatService rules;

    public CapybaraConversationService(CapybaraChatService rules) {
        this.rules = rules;
    }

    /** Trả Optional.empty để controller sử dụng hoàn toàn luồng xử lý 10.4 cũ. */
    public Optional<ChatReply> tryAnswer(ChatRequest request) {
        if (request == null || request.message() == null || request.conversationId() == null
                || !TOKEN.matcher(request.conversationId()).matches()) return Optional.empty();

        String token = request.conversationId();
        String q = plain(request.message().trim());
        String message = request.message().trim();
        pruneIfNecessary();

        if (has(q, "huy tim phong", "huy yeu cau", "bo qua tim phong", "xoa ngu canh", "bat dau lai tim phong")) {
            contexts.remove(token);
            return Optional.of(say("Mình đã xóa điều kiện tìm phòng trong cuộc trò chuyện này. " +
                    "Bạn có thể bắt đầu lại bằng câu: 'Tìm phòng trống'.", List.of("Tìm phòng trống", "Mở thời khóa biểu")));
        }

        // Các lệnh mở trang hoặc câu hỏi nghiệp vụ khác ưu tiên xử lý 10.4 như trước.
        if (otherIntent(q)) {
            contexts.remove(token);
            return Optional.empty();
        }

        boolean starting = newSearch(q);
        RoomSearchContext context = contexts.get(token);
        if (context != null && System.currentTimeMillis() - context.lastTouched > TTL_MS) {
            contexts.remove(token, context);
            context = null;
        }

        if (starting) {
            context = new RoomSearchContext(); // Yêu cầu tìm mới không mang điều kiện của lần cũ.
            contexts.put(token, context);
        } else if (context == null || !followUp(q, context)) {
            return Optional.empty();
        }

        synchronized (context) {
            context.lastTouched = System.currentTimeMillis();
            try {
                updateContext(context, q, starting);
            } catch (DateTimeException | IllegalArgumentException ex) {
                return Optional.of(say("Ngày/giờ chưa hợp lệ. Bạn hãy gửi ngày theo dạng 09/11/2026 " +
                        "hoặc giờ theo dạng 09:00, 09h30 nhé.", List.of("Ngày mai", "09:00", "Hủy tìm phòng")));
            }

            if (context.date == null) {
                return Optional.of(say("🐾 Bạn muốn tìm phòng vào ngày nào? Ví dụ: 'Ngày mai' hoặc '09/11/2026'.",
                        List.of("Hôm nay", "Ngày mai", "09/11/2026", "Hủy tìm phòng")));
            }
            if (context.start == null) {
                return Optional.of(say("Mình nhớ ngày " + formatDate(context.date) + ". " +
                        "Bạn muốn bắt đầu lúc mấy giờ? Ví dụ: '09:00'.",
                        List.of("07:00", "09:00", "13:30", "Hủy tìm phòng")));
            }
            if (context.capacity == null) {
                return Optional.of(say("Mình nhớ ngày " + formatDate(context.date) + ", lúc " +
                        context.start + ". Bạn cần phòng cho bao nhiêu sinh viên?",
                        List.of("20 sinh viên", "40 sinh viên", "60 sinh viên", "Hủy tìm phòng")));
            }
            if (context.start.isAfter(LocalTime.of(22, 29)) && context.end == null) {
                return Optional.of(say("Giờ bắt đầu khá muộn. Bạn hãy nêu giờ kết thúc trong cùng ngày " +
                        "(ví dụ 22:30 đến 23:30), hoặc chọn giờ sớm hơn.",
                        List.of("21:00", "Hủy tìm phòng")));
            }
            LocalTime end = context.end != null ? context.end : context.start.plusMinutes(90);
            if (!context.start.isBefore(end)) {
                return Optional.of(say("Giờ kết thúc phải sau giờ bắt đầu trong cùng ngày. " +
                        "Hãy gửi khoảng giờ mới, ví dụ '09:00 đến 10:30'.",
                        List.of("09:00 đến 10:30", "13:30 đến 15:00", "Hủy tìm phòng")));
            }

            context.completed = true;
            String query = "Tìm phòng trống ngày " + context.date + " lúc " + context.start +
                    " đến " + end + " cho " + context.capacity + " sinh viên" +
                    (context.projector ? " có máy chiếu" : "") +
                    (context.computers ? " có máy tính" : "");
            ChatReply found = rules.answer(new ChatRequest(query));
            String prefix = "🐾 Mình đã ghép các thông tin của cuộc trò chuyện:\n" +
                    "• Ngày: " + formatDate(context.date) + "\n" +
                    "• Giờ: " + context.start + "–" + end + "\n" +
                    "• Sĩ số: " + context.capacity + " người\n" +
                    "• Thiết bị cần có: " + devices(context) + "\n" +
                    (context.end == null ? "• Chưa nêu giờ kết thúc, mặc định 90 phút.\n" : "") +
                    "\n";
            return Optional.of(new ChatReply(prefix + found.reply(), "contextual-rule", found.action(),
                    List.of("Thêm máy chiếu", "Đổi sang 10:00", "Ngày mai", "Hủy tìm phòng")));
        }
    }

    private void updateContext(RoomSearchContext context, String q, boolean starting) {
        LocalDate date = extractDate(q);
        if (date != null) context.date = date;

        List<LocalTime> times = extractTimes(q);
        if (!times.isEmpty()) {
            if (times.size() >= 2) {
                context.start = times.get(0);
                context.end = times.get(1);
            } else if (has(q, "gio ket thuc", "ket thuc luc", "den gio", "toi gio") && context.start != null) {
                context.end = times.get(0);
            } else {
                context.start = times.get(0);
                // Nếu người dùng thay đổi giờ bắt đầu, bỏ giờ kết thúc cũ để mặc định 90 phút.
                context.end = null;
            }
        }

        Matcher match = CAPACITY.matcher(q);
        if (match.find()) {
            context.capacity = Integer.parseInt(match.group(1) != null ? match.group(1) : match.group(2));
        } else if (context.capacity == null && !starting && BARE_NUMBER.matcher(q).matches()) {
            context.capacity = Integer.parseInt(q);
        }

        if (q.contains("may chieu")) {
            context.projector = !has(q, "khong can may chieu", "bo may chieu", "khong may chieu", "khong yeu cau may chieu");
        }
        if (q.contains("may tinh")) {
            context.computers = !has(q, "khong can may tinh", "bo may tinh", "khong may tinh", "khong yeu cau may tinh");
        }
    }

    private static LocalDate extractDate(String q) {
        Matcher m = DATE_ISO.matcher(q);
        if (m.find()) return LocalDate.parse(m.group(1));
        m = DATE_VN.matcher(q);
        if (m.find()) return LocalDate.parse(m.group(1), VN_PARSE);
        LocalDate today = LocalDate.now(VN);
        if (q.contains("ngay kia") || q.contains("ngay mot")) return today.plusDays(2);
        if (q.contains("ngay mai")) return today.plusDays(1);
        if (q.contains("hom nay")) return today;
        return null;
    }

    private static List<LocalTime> extractTimes(String q) {
        Matcher m = CLOCK.matcher(q);
        java.util.ArrayList<LocalTime> times = new java.util.ArrayList<>();
        while (m.find() && times.size() < 2) {
            int hour = Integer.parseInt(m.group(1));
            String mm = m.group(2) != null ? m.group(2) : m.group(3);
            int minute = mm == null || mm.isBlank() ? 0 : Integer.parseInt(mm);
            String suffix = q.substring(m.end(), Math.min(q.length(), m.end()+12));
            if ((suffix.matches("^\\s*(?:chieu|toi).*")) && hour > 0 && hour < 12) hour += 12;
            times.add(LocalTime.of(hour, minute));
        }
        return times;
    }

    private static boolean newSearch(String q) {
        return has(q, "tim phong trong", "phong con trong", "phong trong", "phong ranh", "tim phong");
    }

    private static boolean otherIntent(String q) {
        if (has(q, "cach ", "huong dan ", "lam sao ", "quy trinh ", "cach nao")) return true;
        if (q.startsWith("mo ") || q.startsWith("vao ") || q.startsWith("chuyen sang ") ||
                q.startsWith("dua minh ") || q.startsWith("cho minh den ") ||
                q.startsWith("minh muon vao ") || q.startsWith("toi muon vao ")) return true;
        return has(q, "giang vien", "giao vien", "hoc phan", "mon hoc", "thong ke", "bao cao", "thoi khoa bieu", "lich hoc", "xep lich");
    }

    private static boolean followUp(String q, RoomSearchContext c) {
        if (has(q, "may chieu", "may tinh", "doi sang", "doi gio", "doi ngay", "phong khac", "loc them")) return true;
        if (DATE_ISO.matcher(q).find() || DATE_VN.matcher(q).find() ||
                has(q, "ngay mai", "ngay kia", "hom nay", "ngay mot")) return true;
        if (CLOCK.matcher(q).find() || CAPACITY.matcher(q).find()) return true;
        return c.capacity == null && BARE_NUMBER.matcher(q).matches();
    }

    private void pruneIfNecessary() {
        if (contexts.size() <= MAX_SESSIONS) return;
        long now = System.currentTimeMillis();
        contexts.entrySet().removeIf(e -> now - e.getValue().lastTouched > TTL_MS);
        int excess = contexts.size() - MAX_SESSIONS;
        if (excess > 0) {
            contexts.entrySet().stream().sorted(Comparator.comparingLong(e -> e.getValue().lastTouched))
                    .limit(excess).map(Map.Entry::getKey).toList().forEach(contexts::remove);
        }
    }

    private static boolean has(String q, String... phrases) {
        for (String p : phrases) if (q.contains(p)) return true;
        return false;
    }

    private static String plain(String v) {
        return Normalizer.normalize(v.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('đ','d');
    }

    private static String devices(RoomSearchContext c) {
        if (c.computers && c.projector) return "máy tính và máy chiếu";
        if (c.computers) return "máy tính";
        if (c.projector) return "máy chiếu";
        return "không yêu cầu thiết bị riêng";
    }

    private static String formatDate(LocalDate date) {
        return date.format(VN_DATE_FORMAT);
    }

    private static ChatReply say(String text, List<String> suggestions) {
        return new ChatReply(text, "contextual-rule", null, suggestions);
    }

    private static final class RoomSearchContext {
        private volatile long lastTouched = System.currentTimeMillis();
        private LocalDate date;
        private LocalTime start;
        private LocalTime end;
        private Integer capacity;
        private boolean projector;
        private boolean computers;
        private boolean completed;
    }
}
