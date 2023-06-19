package com.talkforgeai.talkforgeaiserver.dto.ws;

import com.talkforgeai.talkforgeaiserver.openai.dto.OpenAIChatMessage;

import java.util.UUID;

public class WSChatFunctionMessage extends WebsocketMessage {

    OpenAIChatMessage message;

    public WSChatFunctionMessage(UUID sessionId, OpenAIChatMessage message) {
        super(sessionId, WebsocketMessageType.FUNCTION_CALL);
        this.message = message;
    }

    public OpenAIChatMessage getMessage() {
        return message;
    }

    public void setMessage(OpenAIChatMessage message) {
        this.message = message;
    }
}
