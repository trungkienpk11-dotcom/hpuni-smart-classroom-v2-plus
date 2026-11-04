package com.smartclassroom.assistant;

import java.util.List;
import java.util.Map;

/**
 * : trả lời + gợi ý + thao tác điều hướng AN TOÀN.
 * Giữ constructor hai tham số để không ảnh hưởng LocalAiService của bài 10.2.
 */
public record ChatReply(
        String reply,
        String mode,
        Map<String, String> action,
        List<String> suggestions
) {
    public ChatReply(String reply, String mode) {
        this(reply, mode, null, List.of());
    }
}
