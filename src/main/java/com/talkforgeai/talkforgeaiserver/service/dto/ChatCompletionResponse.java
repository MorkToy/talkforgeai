package com.talkforgeai.talkforgeaiserver.service.dto;

import com.theokanning.openai.completion.chat.ChatMessage;

import java.util.List;

public record ChatCompletionResponse(List<ChatMessage> processedMessages) {
}
