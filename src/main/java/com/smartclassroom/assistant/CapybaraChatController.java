package com.smartclassroom.assistant;

import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/assistant")
public class CapybaraChatController {

    private final CapybaraChatService service;
    private final LocalAiService localAi;

    public CapybaraChatController(
            CapybaraChatService service, LocalAiService localAi) {

        this.service = service;
        this.localAi = localAi;
    }

    @GetMapping("/health")
    public Map<String, String> health() {

        return Map.of(
                "status", "ok",
                "name", "capybara hpuni",
                "mode", localAi.enabled() ? "local-ai-optional" : "basic"
        );
    }

    @PostMapping("/chat")
    public ChatReply chat(
            @RequestBody ChatRequest request) {

        // Validate trước khi gửi ra mô hình local để tránh request quá dài.
        if (request == null || request.message() == null || request.message().isBlank() || request.message().length() > 500)
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                "Tin nhắn phải có từ 1 đến 500 ký tự.");
        // Những câu tra cứu dữ liệu trường luôn dùng service thật, không để mô hình đoán.
        if (request != null && request.message() != null) {
            String normalized=java.text.Normalizer.normalize(request.message().toLowerCase(java.util.Locale.ROOT),java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace('đ','d');
            if (!normalized.contains("phong") && !normalized.contains("lich") &&
                !normalized.contains("thoi khoa bieu") && !normalized.contains("giang vien")) {
                var answer=localAi.tryAnswer(request.message());
                if(answer.isPresent())return answer.get();
            }
        }
        return service.answer(request);
    }
}
