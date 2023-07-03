package com.talkforgeai.backend.websocket.dto;


import com.talkforgeai.service.openai.dto.OpenAIChatMessage;

import java.util.UUID;

public class WSChatResponseMessage extends WebsocketMessage {

    private OpenAIChatMessage message;

    public WSChatResponseMessage(UUID sessionId, OpenAIChatMessage message) {
        super(sessionId, WebsocketMessageType.RESPONSE);
        this.message = message;
    }


    public OpenAIChatMessage getMessage() {
        return message;
    }

    public void setMessage(OpenAIChatMessage message) {
        this.message = message;
    }
}
