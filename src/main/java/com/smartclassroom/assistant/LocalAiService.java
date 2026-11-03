package com.smartclassroom.assistant;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

/** Tùy chọn: Ollama LOCAL; không dùng khóa API, không cho phép write database. */
@Service
public class LocalAiService {
    private final ObjectMapper json;
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    @Value("${hpuni.local-ai.enabled:false}") private boolean enabled;
    @Value("${hpuni.local-ai.url:http://127.0.0.1:11434/api/chat}") private String url;
    @Value("${hpuni.local-ai.model:qwen2.5:3b}") private String model;
    public LocalAiService(ObjectMapper json) {this.json=json;}
    public boolean enabled() {return enabled;}
    public Optional<ChatReply> tryAnswer(String question) {
        if(!enabled || question==null || question.isBlank()) return Optional.empty();
        try {
            // Chỉ gửi câu hỏi người dùng, KHÔNG gửi bảng sinh viên hoặc dữ liệu riêng tư.
            Map<String,Object> payload=Map.of("model",model,"stream",false,"options",Map.of("num_predict",400),
              "messages",List.of(Map.of("role","system","content",
                "Bạn là capybara hpuni, trợ lý học tập HPUNI. Luôn trả lời tiếng Việt. " +
                "Không bịa dữ liệu phòng/lịch, không khẳng định đã thay đổi database. " +
                "Nếu cần dữ liệu trường, đề nghị dùng chức năng tra cứu của phần mềm."),
                Map.of("role","user","content",question)));
            HttpRequest request=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(45))
              .header("Content-Type","application/json")
              .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload))).build();
            HttpResponse<String> response=client.send(request,HttpResponse.BodyHandlers.ofString());
            if(response.statusCode()!=200)return Optional.empty();
            JsonNode node=json.readTree(response.body());
            String content=node.path("message").path("content").asText("").trim();
            if(content.isEmpty())return Optional.empty();
            return Optional.of(new ChatReply(content,"local-ai"));
        } catch(Exception e) {return Optional.empty();}
    }
}
