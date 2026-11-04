package com.smartclassroom.assistant;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Bài 10.8: Ollama chạy trên backend, không API key, không ghi cơ sở dữ liệu.
 * Luôn ưu tiên quy tắc Java ở Controller; chỉ gửi câu hỏi người dùng sang Ollama.
 */
@Service
public class LocalAiService {
    private final ObjectMapper json;
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2)).build();

    @Value("${hpuni.local-ai.enabled:false}")
    private boolean enabled;
    @Value("${hpuni.local-ai.url:http://127.0.0.1:11434/api/chat}")
    private String url;
    @Value("${hpuni.local-ai.model:qwen2.5:3b}")
    private String model;

    public LocalAiService(ObjectMapper json) { this.json = json; }

    // Dành cho các client cũ không gửi header chọn chế độ.
    public boolean enabled() { return enabled; }
    public Optional<ChatReply> tryAnswer(String question) {
        return tryAnswer(question, enabled);
    }

    /** Chế độ AI được chọn theo từng request; false = KHÔNG gọi Ollama. */
    public Optional<ChatReply> tryAnswer(String question, boolean useOllama) {
        if (!useOllama || question == null || question.isBlank()) return Optional.empty();
        try {
            // Không truyền dữ liệu phòng, lớp, sinh viên, hoặc thông tin database.
            Map<String, Object> payload = Map.of(
                    "model", model,
                    "stream", false,
                    "options", Map.of("num_predict", 400),
                    "messages", List.of(
                            Map.of("role", "system", "content",
                                    "Bạn là capybara hpuni, trợ lý học tập HPUNI. " +
                                    "Luôn trả lời tiếng Việt. Không bịa dữ liệu phòng/lịch, " +
                                    "không khẳng định đã thay đổi database. " +
                                    "Nếu cần dữ liệu trường, đề nghị dùng chức năng tra cứu của phần mềm."),
                            Map.of("role", "user", "content", question)));
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(45))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return Optional.empty();
            JsonNode node = json.readTree(response.body());
            String content = node.path("message").path("content").asText("").trim();
            if (content.isEmpty()) return Optional.empty();
            return Optional.of(new ChatReply(content, "local-ai"));
        } catch (Exception ignored) {
            // Tắt Ollama / timeout / model lỗi => bộ quy tắc Java vẫn hoạt động.
            return Optional.empty();
        }
    }

    /**
     * Kiểm tra nhanh Ollama + model qua GET /api/tags (không sinh văn bản).
     * Chỉ cấu hình backend mới quyết định được URL/model, không lấy từ browser.
     */
    public Map<String, Object> availability() {
        try {
            URI chatUri = URI.create(url);
            URI tagsUri = new URI(chatUri.getScheme(), null,
                    chatUri.getHost(), chatUri.getPort(), "/api/tags", null, null);
            HttpRequest request = HttpRequest.newBuilder(tagsUri)
                    .timeout(Duration.ofSeconds(3)).GET().build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return Map.of("available", false, "model", model,
                        "message", "Không truy cập được API danh sách model Ollama.");
            }
            JsonNode models = json.readTree(response.body()).path("models");
            boolean found = false;
            for (int i = 0; i < models.size(); i++) {
                JsonNode item = models.get(i);
                if (model.equalsIgnoreCase(item.path("name").asText(""))
                        || model.equalsIgnoreCase(item.path("model").asText(""))) {
                    found = true;
                    break;
                }
            }
            return Map.of("available", found, "model", model,
                    "message", found ? "Ollama sẵn sàng."
                            : "Ollama đang chạy nhưng chưa có model " + model
                            + ". Hãy chạy: ollama pull " + model);
        } catch (Exception ignored) {
            return Map.of("available", false, "model", model,
                    "message", "Ollama chưa chạy hoặc không thể kết nối tại máy chủ Spring Boot.");
        }
    }
}
