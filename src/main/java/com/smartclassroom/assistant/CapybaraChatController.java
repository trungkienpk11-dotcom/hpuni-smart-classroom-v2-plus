package com.smartclassroom.assistant;

import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.text.Normalizer;
import java.util.Locale;

@RestController
@RequestMapping("/api/assistant")
public class CapybaraChatController {

    private final CapybaraChatService service;
    private final LocalAiService localAi;
    private final CapybaraConversationService conversation;
    private final CapybaraPageContextService pageContext;

    public CapybaraChatController(
            CapybaraChatService service, LocalAiService localAi,
            CapybaraConversationService conversation,
            CapybaraPageContextService pageContext) {

        this.service = service;
        this.localAi = localAi;
        this.conversation = conversation;
        this.pageContext = pageContext;
    }

    @GetMapping("/health")
    public Map<String, String> health() {

        return Map.of(
                "status", "ok",
                "name", "capybara hpuni",
                "mode", localAi.enabled() ? "local-ai-optional" : "basic"
        );
    }

    // Bài 10.8: kiểm tra Ollama trên máy CHẠY BACKEND. Không gọi AI chỉ để kiểm tra.
    @GetMapping(value = "/ai-status", produces = "application/json;charset=UTF-8")
    public Map<String, Object> aiStatus() {
        return localAi.availability();
    }

    // Giữ tương thích cho các bài kiểm thử Java trước đó gọi trực tiếp chat(request).
    public ChatReply chat(ChatRequest request) {
        return chat(request, null);
    }

    @PostMapping(value = "/chat", produces = "application/json;charset=UTF-8")
    public ChatReply chat(
            @RequestBody ChatRequest request,
            @RequestHeader(value = "X-HPUNI-AI-Mode", required = false) String aiMode) {
        // ON/OFF theo từng yêu cầu, không thay đổi cấu hình của người dùng khác.
        // Không gửi header => giữ tương thích với Postman/PowerShell Bài 10.7.
        boolean useOllama = "ollama".equalsIgnoreCase(aiMode)
                || (aiMode == null && localAi.enabled());

        // Validate trước khi gửi ra mô hình local để tránh request quá dài.
        if (request == null || request.message() == null || request.message().isBlank() || request.message().length() > 500)
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                "Tin nhắn phải có từ 1 đến 500 ký tự.");
        // Bài 10.5: Ưu tiên quy trình hỏi-đáp nhiều lượt của tìm phòng trống.
        // Nếu không có conversationId hoặc không phải chuỗi hỏi tiếp, giữ nguyên chatbot 10.4.
        var contextual = conversation.tryAnswer(request);
        if (contextual.isPresent()) return contextual.get();

        // Câu hỏi "trang này" trả lời bằng cấu hình trang ở Bài 10.6.
        // Các câu hỏi cụ thể giữ luồng của Bài 10.4/10.5.
        var pageAnswer = pageContext.tryAnswer(request);
        if (pageAnswer.isPresent()) return pageAnswer.get();

        // Những câu hỏi LÝ THUYẾT về thuật toán phải được hỏi AI trước khi
        // bộ quy tắc nhận nhầm cụm "xếp lịch" thành lệnh mở chức năng.
        // Không ảnh hưởng các lệnh điều hướng hoặc tra cứu thông thường.
        if (isConceptualAiQuestion(request.message())) {
            var conceptualAnswer = localAi.tryAnswer(request.message(), useOllama);
            if (conceptualAnswer.isPresent()) return conceptualAnswer.get();
            // AI OFF / Ollama lỗi: quay về luồng Java như cũ.
        }

        // Ưu tiên toàn bộ tra cứu, hướng dẫn, điều hướng đã được lập trình.
        ChatReply deterministic = service.answer(request);
        if (!"unhandled".equals(deterministic.mode())) {
            return deterministic;
        }

        // Chỉ chuyển câu hỏi ngoài phạm vi quy tắc sang AI local.
        // Ollama lỗi/tắt/chưa tải model => trả lại lời giải thích từ chatbot cũ.
        return localAi.tryAnswer(request.message(), useOllama).orElse(deterministic);

    }

    /**
     * Chỉ nhận các câu hỏi học thuật có chủ đích rõ ràng.
     * Không dùng cho "Mở xếp lịch tự động" hay "Cách xếp lịch thử".
     */
    private static boolean isConceptualAiQuestion(String message) {
        if (message == null) return false;
        String q = Normalizer.normalize(message.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('đ', 'd');
        boolean topic = q.contains("thuat toan") || q.contains("algorithm")
                || q.contains("greedy") || q.contains("tham lam");
        boolean explanation = q.contains("giai thich") || q.contains("la gi")
                || q.contains("nguyen ly") || q.contains("ung dung")
                || q.contains("phan tich") || q.contains("so sanh")
                || q.contains("hoat dong nhu the nao") || q.contains("tai sao");
        return topic && explanation;
    }
}
