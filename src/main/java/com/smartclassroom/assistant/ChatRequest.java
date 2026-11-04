package com.smartclassroom.assistant;

/**
 * : pageContext là mã trang hiển thị (không phải URL hoặc nội dung riêng tư).
 * Client 10.4/10.5 vẫn tương thích do trường mới có thể null.
 */
public record ChatRequest(String message, String conversationId, String pageContext) {
    public ChatRequest(String message) {
        this(message, null, null);
    }
    public ChatRequest(String message, String conversationId) {
        this(message, conversationId, null);
    }
}
