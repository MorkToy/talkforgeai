package com.talkforgeai.talkforgeaiserver.dto;

import java.util.UUID;

public record ChatCompletionRequest(UUID sessionId,
                                    String content,
                                    String name) {

    public ChatCompletionRequest(UUID sessionId, String content) {
        this(sessionId, content, null);
    }
}
