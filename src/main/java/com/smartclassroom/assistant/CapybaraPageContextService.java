package com.smartclassroom.assistant;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * : giải thích giao diện theo trang hiện tại. Không ghi database và
 * không tin cậy tên trang do client gửi: chỉ chấp nhận một danh sách cố định.
 * Không thay thế bộ quy tắc tra cứu/lịch và bộ nhớ tìm phòng từ Bài 10.5.
 */
@Service
public final class CapybaraPageContextService {

    private record PageInfo(
            String label, String purpose, String steps,
            String troubleshooting, List<String> suggestions) {
    }

    private static final Map<String, PageInfo> PAGES = Map.ofEntries(
            Map.entry("dashboard", new PageInfo(
                    "Tổng quan",
                    "Tóm tắt số phòng, giảng viên, lớp học phần, lịch học trong tuần và thao tác nhanh.",
                    "1. Đọc các thẻ số liệu tổng hợp.\n2. Chọn 'Xem thời khóa biểu' để xem lịch đã lưu.\n3. Chọn 'Xếp lịch tự động' khi cần đề xuất lịch.\n4. Mở các chức năng từ thanh điều hướng bên trái.",
                    "Nếu số liệu chưa cập nhật, tải lại trang và kiểm tra Spring Boot. Số buổi theo tuần chỉ lấy lịch đã lưu, không tính bản xếp thử.",
                    List.of("Mở thời khóa biểu", "Mở quản lý phòng học", "Mở thống kê báo cáo"))),
            Map.entry("rooms", new PageInfo(
                    "Quản lý phòng học",
                    "Quản lý mã phòng, tên phòng, sức chứa và trang thiết bị.",
                    "1. Nhấn '+ Thêm phòng học'.\n2. Nhập mã, tên, sức chứa, loại phòng và thiết bị.\n3. Nhấn Lưu trong hộp thoại.\n4. Sử dụng ô tìm kiếm để lọc phòng hoặc nút Sửa/Xóa trên từng dòng.",
                    "Nếu không lưu được, kiểm tra mã phòng có bị trùng, sức chứa có hợp lệ và thông báo ở cuối bảng. Nếu xóa bị từ chối, phòng có thể đang được sử dụng trong lịch.",
                    List.of("Danh sách phòng học", "Phòng B201", "Mở tìm phòng trống"))),
            Map.entry("teachers", new PageInfo(
                    "Quản lý giảng viên",
                    "Quản lý danh sách giảng viên phục vụ phân công và xếp lịch.",
                    "1. Nhấn '+ Thêm giảng viên'.\n2. Điền mã, họ tên và email.\n3. Nhấn Lưu.\n4. Tìm giảng viên bằng mã/tên/email; dùng Sửa/Xóa khi cần.",
                    "Nếu danh sách trống, kiểm tra dữ liệu giảng viên đã được tạo và API backend có hoạt động. Nếu xóa thất bại, hãy kiểm tra giảng viên có đang gắn với lớp học phần không.",
                    List.of("Danh sách giảng viên", "Mở quản lý lớp học phần", "Cách khai báo lịch bận"))),
            Map.entry("groups", new PageInfo(
                    "Quản lý lớp sinh viên",
                    "Quản lý mã lớp, tên lớp và sĩ số; sĩ số quyết định sức chứa phòng phù hợp.",
                    "1. Nhấn '+ Thêm lớp sinh viên'.\n2. Điền mã lớp, tên và sĩ số.\n3. Nhấn Lưu.\n4. Tìm kiếm hoặc chọn Sửa/Xóa trong bảng.",
                    "Nếu không lưu, hãy kiểm tra mã lớp không trùng và sĩ số là số dương. Sĩ số vượt sức chứa phòng có thể khiến xếp lịch không tìm được phòng phù hợp.",
                    List.of("Mở quản lý lớp học phần", "Mở quản lý phòng học", "Mở xếp lịch tự động"))),
            Map.entry("courses", new PageInfo(
                    "Quản lý môn học",
                    "Quản lý mã môn, tên môn và số tín chỉ.",
                    "1. Nhấn '+ Thêm môn học'.\n2. Điền mã, tên môn và số tín chỉ.\n3. Lưu dữ liệu.\n4. Tạo lớp học phần sau khi đã có môn học và giảng viên.",
                    "Nếu danh sách môn trống, cần tạo môn trước khi tạo lớp học phần. Kiểm tra số tín chỉ và mã trùng nếu lưu không thành công.",
                    List.of("Mở quản lý lớp học phần", "Danh sách môn học", "Mở xếp lịch tự động"))),
            Map.entry("offerings", new PageInfo(
                    "Quản lý lớp học phần",
                    "Liên kết môn học với giảng viên, lớp sinh viên, học kỳ và số buổi học mỗi tuần.",
                    "1. Đảm bảo đã có môn, giảng viên và lớp sinh viên.\n2. Nhấn '+ Tạo lớp học phần'.\n3. Chọn các danh mục, năm học, học kỳ và số buổi mỗi tuần.\n4. Nhấn Lưu, sau đó mở Xếp lịch tự động.",
                    "Nếu ô lựa chọn trống, kiểm tra danh mục Môn học/Giảng viên/Lớp sinh viên. Nếu không xếp được lịch, kiểm tra số buổi/tuần, sĩ số và các ràng buộc.",
                    List.of("Mở quản lý môn học", "Mở quản lý giảng viên", "Mở xếp lịch tự động"))),
            Map.entry("schedule", new PageInfo(
                    "Thời khóa biểu",
                    "Xem và quản lý các buổi học đã lưu theo tuần.",
                    "1. Chọn ngày trong tuần hoặc nút Tuần trước/Tuần sau.\n2. Lọc theo phòng, lớp hoặc giảng viên; có thể Xóa lọc.\n3. Nhấn '+ Thêm buổi học' hoặc Sửa/Xóa trên lịch.\n4. Nhấn 'Xuất CSV' để tải lịch đã lưu.",
                    "Nếu chỉ thấy 'Đang tải thời khóa biểu', kiểm tra server/API lịch và lỗi Console F12. Nếu chưa thấy lịch sau xếp thử, cần phê duyệt & lưu lịch ở Xếp lịch tự động trước.",
                    List.of("Lịch học tuần này", "Mở xếp lịch tự động", "Cách xuất thời khóa biểu"))),
            Map.entry("autoSchedule", new PageInfo(
                    "Xếp lịch tự động",
                    "Tạo phương án xếp lịch thử, kiểm tra ràng buộc và phê duyệt lưu nếu phù hợp.",
                    "1. Chọn học kỳ, năm học và tuần.\n2. Nhấn 'Xếp lịch thử' (chưa lưu).\n3. Đọc bảng đề xuất, cảnh báo và các buổi chưa xếp.\n4. Khi chắc chắn, nhấn 'Phê duyệt & lưu lịch'.\n5. Mở Thời khóa biểu để kiểm tra lịch đã lưu.",
                    "Nếu không có đề xuất, kiểm tra lớp học phần, phòng đủ sức chứa, lịch bận giảng viên và yêu cầu thiết bị. Nếu nút lưu bị khóa, hãy tạo và kiểm tra phương án xếp thử trước.",
                    List.of("Cách xếp lịch thử", "Cách xác nhận lịch", "Cách khai báo lịch bận giảng viên"))),
            Map.entry("availability", new PageInfo(
                    "Tìm phòng trống",
                    "Tra cứu phòng đáp ứng ngày, khoảng giờ, sĩ số và thiết bị theo lịch đã lưu.",
                    "1. Chọn Ngày học.\n2. Chọn giờ Bắt đầu và Kết thúc.\n3. Nhập Sức chứa tối thiểu.\n4. Chọn Có máy tính hoặc Có máy chiếu nếu cần.\n5. Nhấn 'Tìm phòng phù hợp'. Bạn cũng có thể hỏi trực tiếp trong chat.",
                    "Nếu không có phòng phù hợp, hãy kiểm tra khoảng giờ, sĩ số, thiết bị và các buổi học đã lưu. Kết quả không bao gồm đặt phòng ngoài hệ thống.",
                    List.of("Tìm phòng trống", "Mở quản lý phòng học", "Mở thời khóa biểu"))),
            Map.entry("reports", new PageInfo(
                    "Thống kê & báo cáo",
                    "Đếm số buổi đã lưu theo tuần, tỷ lệ phòng được xếp lịch và chi tiết theo phòng.",
                    "1. Chọn tuần hoặc nhấn mũi tên chuyển tuần.\n2. Xem các thẻ thống kê và biểu đồ theo ngày.\n3. Kiểm tra xếp hạng và bảng chi tiết phòng.\n4. Nhấn 'Xuất báo cáo CSV' để xuất dữ liệu.",
                    "Nếu biểu đồ toàn số 0, hãy kiểm tra tuần đang chọn và chắc chắn đã có buổi được lưu; bản xếp thử chưa lưu sẽ không tính vào báo cáo.",
                    List.of("Thống kê khoa", "Mở thời khóa biểu", "Cách xuất CSV")))
    );

    public Optional<ChatReply> tryAnswer(ChatRequest request) {
        if (request == null || request.message() == null || request.pageContext() == null)
            return Optional.empty();
        PageInfo page = PAGES.get(request.pageContext());
        if (page == null) return Optional.empty(); // Không tin các page lạ do client gửi.

        String q = plain(request.message().trim());
        boolean currentPage = has(q,
                "trang nay", "muc nay", "o day", "tai day", "man hinh nay", "dang o trang nao",
                "trang hien tai", "toi dang o dau", "minh dang o dau", "chuc nang nay", "cho nay");
        boolean genericHelp = hasExact(q,
                "huong dan", "tro giup", "huong dan trang", "huong dan trang hien tai",
                "toi nen lam gi", "minh nen lam gi", "lam gi tiep", "buoc tiep theo", "cach su dung", "cach dung trang nay");
        boolean diagnose = has(q, "khong hien", "khong tai", "khong thay", "bi loi", "khong duoc", "khong hoat dong", "trong tron", "khong co du lieu", "cho mai")
                && (currentPage || hasExact(q, "khong tai", "khong hien", "khong thay du lieu", "bi loi", "khong hoat dong"));

        // Chỉ trả lời câu tham chiếu tới màn hình hiện tại. Câu hỏi cụ thể như
        // "Cách thêm giảng viên" hoặc "Lịch tuần sau" đi tới luồng 10.4/10.5.
        if (!currentPage && !genericHelp && !diagnose) return Optional.empty();
        // Dành cho lời hỏi về dữ liệu chung chứ không cướp ý định tìm phòng.
        if (has(q, "tim phong trong", "tim phong ngay", "lich hoc tuan", "thong ke khoa") && !currentPage)
            return Optional.empty();

        String answer;
        if (has(q, "dang o trang nao", "toi dang o dau", "minh dang o dau", "trang hien tai la gi")) {
            answer = "🐾 Bạn đang ở trang '" + page.label + "' trên HPUNI Smart Classroom V2+. " + page.purpose;
        } else if (diagnose || has(q, "tai sao", "loi gi", "xu ly loi")) {
            answer = "🐾 Hướng kiểm tra ở trang '" + page.label + "':\n" + page.troubleshooting +
                    "\nMình chưa đọc được lỗi cụ thể trong Console hoặc trên máy bạn, nên đây là các bước kiểm tra chứ chưa phải chẩn đoán chắc chắn.";
        } else if (has(q, "lam gi", "buoc tiep", "bat dau", "nut nao", "them", "sua", "xoa", "luu", "xuat", "loc", "huong dan", "cach dung", "cach su dung") || genericHelp) {
            answer = "🐾 Bạn đang xem '" + page.label + "'. Các bước thực hiện:\n" + page.steps;
        } else {
            answer = "🐾 Trang '" + page.label + "': " + page.purpose + "\n\nCách sử dụng:\n" + page.steps;
        }
        return Optional.of(new ChatReply(answer, "page-aware-rule", null, page.suggestions));
    }

    private static boolean has(String q, String... words) {
        for (String word : words) if (q.contains(word)) return true;
        return false;
    }

    private static boolean hasExact(String q, String... words) {
        for (String word : words) if (q.equals(word) || q.equals(word + "?")) return true;
        return false;
    }

    private static String plain(String input) {
        return Normalizer.normalize(input.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('đ', 'd').trim();
    }
}
