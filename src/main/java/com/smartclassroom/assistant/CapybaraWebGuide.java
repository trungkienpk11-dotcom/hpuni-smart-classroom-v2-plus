package com.smartclassroom.assistant;

import java.util.List;
import java.util.Map;

/**
 * – kho hướng dẫn CHỈ ĐỌC cho các tính năng đã có trên V2+.
 * Không gọi database, không ghi dữ liệu, không chạy JavaScript từ câu hỏi.
 * Tên trang là các khóa điều hướng được frontend cho phép.
 */
public final class CapybaraWebGuide {
    private CapybaraWebGuide() {}

    public record Topic(String answer, String page, List<String> suggestions) {
        public ChatReply toReply() {
            Map<String, String> action = page == null ? null : Map.of("type", "navigate", "page", page);
            return new ChatReply(answer, "rule-based", action, suggestions);
        }
    }

    private static Topic topic(String answer, String page, String... suggestions) {
        return new Topic(answer, page, List.of(suggestions));
    }

    private static boolean has(String q, String... phrases) {
        for (String p : phrases) if (q.contains(p)) return true;
        return false;
    }

    /** Các hướng dẫn theo ý định, không phải mô hình AI tạo sinh. Đầu vào đã bỏ dấu. */
    public static Topic explain(String q) {
        boolean how = has(q, "cach ", "lam sao", "huong dan", "nhu the nao", "the nao", "quy trinh", "muon ",
                "can lam gi", "tai sao", "khong duoc", "khong hien", "bi loi", "khong thay", "co the ",
                "them ", "sua ", "xoa ", "cap nhat ", "tao ", "chinh sua ", "nhap ", "xuat ", "luu ", "doi ", "phuc hoi ");
        // Câu hỏi về sử dụng giao diện → hướng dẫn trước khi truy vấn dữ liệu.
        if (has(q, "xep lich thu", "xem truoc lich", "lich thu") && (how || has(q,"khac", "luu", "xac nhan", "phuong an"))) {
            return topic("Xếp lịch thử chỉ tạo phương án đề xuất, CHƯA ghi vào dữ liệu.\n1. Mở Xếp lịch tự động.\n2. Chọn học kỳ, năm học và tuần.\n3. Nhấn 'Xếp lịch thử'.\n4. Xem bảng đề xuất và cảnh báo.\n5. Nếu phù hợp, nhấn 'Phê duyệt & lưu lịch'.\nMuốn xem lịch đã lưu, mở Thời khóa biểu. Mình không tự xác nhận thay bạn.", "autoSchedule",
                    "Mở xếp lịch tự động", "Cách xác nhận lịch", "Kiểm tra trùng lịch");
        }
        if (has(q, "xac nhan", "phe duyet", "luu lich", "luu phuong an") && has(q, "lich", "buoi", "de xuat")) {
            return topic("Chỉ xác nhận sau khi đã xem kết quả xếp thử. Trên trang Xếp lịch tự động, kiểm tra đủ phòng, lớp, giảng viên, ngày giờ và mục 'Cảnh báo/Chưa xếp được'; sau đó nhấn 'Phê duyệt & lưu lịch'. Hệ thống kiểm tra lại các ràng buộc trước khi ghi. Nếu chưa có đề xuất, nút sẽ bị khóa. Chatbot không tự lưu dữ liệu.", "autoSchedule",
                    "Mở xếp lịch tự động", "Mở thời khóa biểu");
        }
        if (has(q, "lich ban", "giang vien ban", "teacher busy", "khong ranh", "ngay ban")) {
            return topic("Để khai báo lịch bận giảng viên:\n1. Mở Xếp lịch tự động, kéo đến 'Quy tắc xếp lịch nâng cao'.\n2. Chọn giảng viên, ngày bận, giờ bắt đầu và kết thúc.\n3. Nhập lý do rồi nhấn '+ Thêm lịch bận'.\n4. Xếp lịch thử lại để áp dụng ràng buộc. Nếu ô chọn trống, hãy kiểm tra danh sách giảng viên ở mục Quản lý giảng viên.", "autoSchedule",
                    "Mở quản lý giảng viên", "Mở xếp lịch tự động");
        }
        if (has(q, "yeu cau phong", "thiet bi theo mon", "yeu cau thiet bi", "can may chieu", "can may tinh", "yeu cau may chieu", "yeu cau may tinh", "may chieu theo mon", "may tinh theo mon") && how) {
            return topic("Trong Xếp lịch tự động → 'Quy tắc xếp lịch nâng cao' → 'Yêu cầu phòng học theo môn': chọn môn, tích 'Bắt buộc có máy tính' hoặc 'Bắt buộc có máy chiếu' và nhấn 'Lưu yêu cầu thiết bị'. Khi chạy Xếp lịch thử lần tiếp theo, hệ thống sẽ ưu tiên đáp ứng các ràng buộc đó.", "autoSchedule",
                    "Mở xếp lịch tự động", "Danh sách môn học");
        }
        if (has(q,"trung lich", "xung dot", "trung phong", "trung giang vien", "trung lop")) {
            return topic("Hệ thống kiểm tra xung đột khi lưu lịch: một phòng, giảng viên hoặc lớp sinh viên không được có hai buổi học trùng khoảng thời gian. Xếp lịch tự động cũng kiểm tra sức chứa và yêu cầu thiết bị. Bạn có thể xem các buổi đã lưu ở Thời khóa biểu, sau đó sửa/xóa buổi phù hợp trên giao diện. Mình chỉ tra cứu, không thay đổi lịch.", "schedule",
                    "Mở thời khóa biểu", "Mở xếp lịch tự động");
        }
        if (has(q,"quy tac xep lich", "rang buoc xep lich", "thuat toan xep lich", "xep lich thong minh")) {
            return topic("Khi xếp lịch, hệ thống cần xét ràng buộc phòng không trùng giờ, giảng viên và lớp không trùng buổi, phòng đủ sức chứa, đáp ứng yêu cầu máy tính/máy chiếu và lịch bận giảng viên đã khai báo. Quy trình an toàn là Xếp lịch thử → kiểm tra cảnh báo → Phê duyệt & lưu lịch. Hệ thống có thể không tìm được đủ buổi nếu điều kiện quá chặt.", "autoSchedule",
                    "Mở xếp lịch tự động", "Cách thêm lịch bận giảng viên");
        }
        if (has(q,"xep lich", "lap thoi khoa bieu", "lich tu dong") && how) {
            return topic("Quy trình xếp lịch: khai báo đầy đủ Phòng học → Giảng viên → Lớp sinh viên → Môn học → Lớp học phần. Sau đó mở Xếp lịch tự động, chọn tuần/học kỳ/năm học, nhấn 'Xếp lịch thử', kiểm tra kết quả rồi chọn 'Phê duyệt & lưu lịch'. Chỉ bản đã duyệt mới xuất hiện trong Thời khóa biểu.", "autoSchedule",
                    "Mở xếp lịch tự động", "Cách xem thời khóa biểu");
        }
        if (has(q,"xuat", "csv", "excel", "tai bao cao", "tai lich")) {
            if(has(q,"lich", "thoi khoa", "buoi")) return topic("Để xuất thời khóa biểu, mở Thời khóa biểu, chọn tuần và bộ lọc mong muốn, rồi nhấn 'Xuất CSV'. Nếu muốn xuất bản đề xuất chưa lưu, vào Xếp lịch tự động và chọn 'CSV đề xuất' sau khi xếp thử. CSV có thể mở bằng Excel; đây không phải file .xlsx.", "schedule",
                    "Mở thời khóa biểu", "Mở xếp lịch tự động");
            return topic("Để xuất báo cáo, mở Thống kê & báo cáo, chọn tuần cần xem và nhấn 'Xuất báo cáo CSV'. File CSV tổng hợp dữ liệu lịch học đã lưu (không phải số liệu cảm biến hay điểm danh). Bạn có thể mở bằng Excel.", "reports",
                    "Mở thống kê báo cáo", "Thống kê khoa");
        }
        if (has(q,"thong ke", "bao cao", "bieu do", "so buoi", "su dung phong") && how) {
            return topic("Ở trang Thống kê & báo cáo, chọn tuần hoặc bấm mũi tên chuyển tuần. Trang hiển thị số buổi đã lưu, phòng được xếp lịch, biểu đồ theo ngày và bảng chi tiết từng phòng. Chọn 'Xuất báo cáo CSV' để tải về. Dữ liệu chỉ tính theo lịch đã lưu, không phải mức độ sử dụng phòng thực tế.", "reports",
                    "Mở thống kê báo cáo", "Thống kê khoa");
        }
        if (has(q,"thoi khoa bieu", "lich hoc", "buoi hoc", "lich giang") && how) {
            return topic("Để sử dụng Thời khóa biểu:\n1. Mở Thời khóa biểu.\n2. Chọn ngày thuộc tuần cần xem, hoặc bấm Tuần trước/Tuần sau.\n3. Lọc theo phòng, lớp hoặc giảng viên nếu cần.\n4. Dùng '+ Thêm buổi học' để tạo lịch; có thể sửa/xóa trong từng thẻ buổi học.\n5. Chọn 'Xuất CSV' để tải lịch. Bản xếp thử chưa lưu sẽ KHÔNG xuất hiện ở đây.", "schedule",
                    "Mở thời khóa biểu", "Cách xếp lịch thử");
        }
        if (has(q,"tim phong trong", "phong con trong", "phong ranh", "tim phong") && how) {
            return topic("Mở Tìm phòng trống, chọn ngày, giờ bắt đầu/kết thúc và số chỗ cần dùng; nếu cần, lọc thêm thiết bị. Hoặc hỏi mình trực tiếp: 'Tìm phòng trống ngày mai lúc 09:00 cho 40 sinh viên có máy chiếu'. Kết quả dựa trên thời khóa biểu đã lưu, chưa tính các đặt phòng ngoài hệ thống.", "availability",
                    "Mở tìm phòng trống", "Tìm phòng trống ngày mai lúc 09:00 cho 40 sinh viên");
        }
        if (has(q,"phong hoc", "phong may", "phong chieu", "quan ly phong", "them phong", "sua phong", "xoa phong", "tao phong") && how) {
            return topic("Quản lý phòng học: mở Phòng học → nhấn Thêm phòng, nhập mã phòng, tên, sức chứa, loại phòng và thiết bị → Lưu. Để chỉnh sửa hoặc xóa, sử dụng nút tương ứng trên từng phòng. Nếu phòng đã có lịch, thao tác xóa có thể bị từ chối để bảo toàn dữ liệu.", "rooms",
                    "Mở quản lý phòng học", "Danh sách phòng học");
        }
        if (has(q,"giang vien", "giao vien", "them thay", "xoa thay" ) && how) {
            return topic("Quản lý giảng viên: mở Giảng viên → Thêm giảng viên → nhập mã, họ tên và email → Lưu. Các nút Sửa/Xóa giúp cập nhật danh sách. Giảng viên được dùng khi tạo Lớp học phần và xếp lịch; hãy kiểm tra quan hệ dữ liệu trước khi xóa.", "teachers",
                    "Mở quản lý giảng viên", "Danh sách giảng viên");
        }
        if (has(q,"lop sinh vien", "si so", "nhom sinh vien", "them lop", "sua lop", "xoa lop") && how) {
            return topic("Quản lý lớp sinh viên: mở Lớp sinh viên → Thêm lớp → nhập mã lớp, tên và sĩ số → Lưu. Sĩ số là điều kiện quan trọng khi tìm phòng/xếp lịch: phòng phải đủ sức chứa. Sửa/Xóa dùng các nút trên từng lớp.", "groups",
                    "Mở quản lý lớp sinh viên", "Danh sách lớp sinh viên");
        }
        if (has(q,"lop hoc phan", "hoc phan") && how) {
            return topic("Lớp học phần là liên kết giữa một môn học, giảng viên và lớp sinh viên. Để tạo: mở Lớp học phần → Thêm học phần → chọn môn, giảng viên, lớp, học kỳ, năm học và số buổi/tuần → Lưu. Phải có dữ liệu danh mục trước khi tạo lớp học phần.", "offerings",
                    "Mở quản lý lớp học phần", "Cách xếp lịch thử");
        }
        if (has(q,"mon hoc", "tin chi", "hoc phan", "them mon", "sua mon", "xoa mon") && how) {
            return topic("Quản lý môn học: mở Môn học → Thêm môn → nhập mã, tên môn và số tín chỉ → Lưu. Muốn sửa hoặc xóa, dùng các nút trong danh sách. Sau đó tạo Lớp học phần để gán môn cho lớp và giảng viên.", "courses",
                    "Mở quản lý môn học", "Mở quản lý lớp học phần");
        }
        if (has(q,"tim kiem", "thanh tim kiem", "search", "sidebar", "thanh ben", "menu ben")) {
            return topic("Thanh điều hướng bên trái dùng để chuyển giữa các trang quản lý. Ô tìm kiếm ở thanh trên hỗ trợ tìm nhanh phòng, giảng viên và môn học. Trên màn hình nhỏ, hãy mở nút menu để xem sidebar. Bạn cũng có thể nhắn 'Mở quản lý phòng học' và nhấn nút điều hướng mình cung cấp.", "dashboard",
                    "Mở tổng quan", "Mở quản lý phòng học");
        }
        if (has(q,"chatbot", "capybara", "api key", "khong can api", "quyen truy cap", "bao mat", "ai co the")) {
            return topic("capybara hpuni hiện dùng luật xử lý tiếng Việt và API nội bộ, KHÔNG cần API key. Mình có thể hướng dẫn chức năng, đọc một số dữ liệu và mở các trang; không tự thêm/sửa/xóa hay phê duyệt lịch. Truy vấn phòng trống phụ thuộc lịch đã lưu, nên không bảo đảm chính xác với lịch đặt ngoài hệ thống. Nếu câu hỏi nằm ngoài phạm vi, mình sẽ nói rõ thay vì đoán.", null,
                    "Bạn giúp được gì?", "Mở thời khóa biểu", "Mở xếp lịch tự động");
        }
        if (has(q,"khong hien", "bi loi", "loi fetch", "loi tai", "build fail", "khong chay")) {
            return topic("Nếu trang bị 'Failed to fetch' hoặc không tải dữ liệu: kiểm tra Spring Boot còn chạy trên cổng 8081 và thử mở /api/health. Nhấn Ctrl+Shift+R để làm mới cache. Nếu lỗi build, đọc dòng [ERROR] đầu tiên trong Terminal; không xóa database hoặc chép đè toàn bộ source khi chưa xác định nguyên nhân.", null,
                    "Mở tổng quan", "Hướng dẫn sử dụng");
        }
        if (has(q,"tong quan", "trang chu", "dashboard") && how) {
            return topic("Trang Tổng quan cho thấy số phòng, số giảng viên, số lớp học phần, lịch tuần và các thao tác nhanh. Các ô thống kê dựa trên dữ liệu hệ thống; bạn có thể mở tiếp Thời khóa biểu hoặc Báo cáo để xem chi tiết.", "dashboard",
                    "Mở tổng quan", "Mở thống kê báo cáo");
        }
        return null;
    }
}
