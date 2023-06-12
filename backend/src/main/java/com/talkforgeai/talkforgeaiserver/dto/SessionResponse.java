package com.talkforgeai.talkforgeaiserver.dto;

import com.theokanning.openai.completion.chat.ChatMessage;

import java.util.List;
import java.util.UUID;

public record SessionResponse(UUID id,
                              String title,
                              String description,
                              List<ChatMessage> chatMessages,
                              PersonaResponse persona) {

}
