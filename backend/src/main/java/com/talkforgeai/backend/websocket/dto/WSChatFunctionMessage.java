/*
 * Copyright (c) 2023 Jean Schmitz.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.talkforgeai.backend.websocket.dto;


import com.talkforgeai.service.openai.dto.OpenAIChatMessage;

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
