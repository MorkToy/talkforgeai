package com.talkforgeai.talkforgeaiserver.service.dto;

import com.theokanning.openai.completion.chat.ChatMessage;

import java.util.List;

public record ChatCompletionResponse(String sessionId,
                                     List<ChatMessage> unprocessedMessages,
                                     List<ChatMessage> processedMessages) {
}
