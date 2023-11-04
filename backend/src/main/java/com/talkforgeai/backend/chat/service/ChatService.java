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

package com.talkforgeai.backend.chat.service;

import com.talkforgeai.backend.chat.domain.ChatMessageEntity;
import com.talkforgeai.backend.chat.domain.ChatMessageType;
import com.talkforgeai.backend.chat.dto.ChatCompletionRequest;
import com.talkforgeai.backend.chat.dto.ChatCompletionResponse;
import com.talkforgeai.backend.chat.exception.ChatException;
import com.talkforgeai.backend.chat.repository.FunctionRepository;
import com.talkforgeai.backend.persona.domain.PersonaEntity;
import com.talkforgeai.backend.persona.domain.PersonaPropertyValue;
import com.talkforgeai.backend.persona.domain.RequestFunction;
import com.talkforgeai.backend.persona.exception.PersonaException;
import com.talkforgeai.backend.persona.service.PersonaProperties;
import com.talkforgeai.backend.persona.service.PersonaService;
import com.talkforgeai.backend.session.domain.ChatSessionEntity;
import com.talkforgeai.backend.session.dto.NewChatSessionRequest;
import com.talkforgeai.backend.session.exception.SessionException;
import com.talkforgeai.backend.session.service.SessionService;
import com.talkforgeai.backend.transformers.MessageProcessor;
import com.talkforgeai.backend.websocket.dto.WSChatFunctionMessage;
import com.talkforgeai.backend.websocket.dto.WSChatResponseMessage;
import com.talkforgeai.backend.websocket.dto.WSChatStatusMessage;
import com.talkforgeai.backend.websocket.service.WebSocketService;
import com.talkforgeai.service.openai.OpenAIChatService;
import com.talkforgeai.service.openai.dto.OpenAIChatMessage;
import com.talkforgeai.service.openai.dto.OpenAIChatRequest;
import com.talkforgeai.service.openai.dto.OpenAIChatResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ChatService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatService.class);

    private final OpenAIChatService openAIChatService;
    private final PersonaService personaService;
    private final SessionService sessionService;
    private final WebSocketService webSocketService;
    private final MessageProcessor messageProcessor;
    private final FunctionRepository functionRepository;

    public ChatService(OpenAIChatService openAIChatService,
                       SessionService sessionService,
                       PersonaService personaService,
                       WebSocketService webSocketService,
                       MessageProcessor messageProcessor,
                       FunctionRepository functionRepository) {
        this.openAIChatService = openAIChatService;
        this.sessionService = sessionService;
        this.personaService = personaService;
        this.webSocketService = webSocketService;
        this.messageProcessor = messageProcessor;
        this.functionRepository = functionRepository;
    }

    private boolean isFunctionCallFromAssistant(OpenAIChatMessage message) {
        return message.functionCall() != null && message.role() == OpenAIChatMessage.Role.ASSISTANT;
    }

    public ChatCompletionResponse submitFuncConfirmation(UUID sessionId) {
        try {

            ChatSessionEntity session = sessionService.getById(sessionId)
                    .orElseThrow(() -> new SessionException("Session not found: " + sessionId));

            OpenAIChatMessage message = getLastMessage(session)
                    .orElseThrow(() -> new SessionException("No previous delta found."));

            if (!isFunctionCallFromAssistant(message)) {
                throw new SessionException("Last delta is not a function.");
            }

            LOGGER.info("Processing function: " + message.functionCall());

            String proccessedFuncContent = "Email send";
            OpenAIChatMessage proccessedFuncMessage
                    = new OpenAIChatMessage(OpenAIChatMessage.Role.USER, proccessedFuncContent, message.functionCall().name());

            ChatCompletionRequest request
                    = new ChatCompletionRequest(sessionId, proccessedFuncContent, message.functionCall().name());

            LOGGER.info("Submitting chat completion request for session: {}", request.sessionId());

            return submitChatRequest(request, List.of(proccessedFuncMessage));
        } catch (Exception ex) {
            throw new ChatException("Error while confirmation of function.", ex);
        }
    }

    public ChatCompletionResponse submitChatRequest(ChatCompletionRequest request) {
        return this.submitChatRequest(request, new ArrayList<>());
    }

    @Nullable
    public ChatCompletionResponse submitChatRequest(ChatCompletionRequest request, List<OpenAIChatMessage> additionalResponseMessages) {
        SubmitResult submitResult = submit(request);
        OpenAIChatMessage processedResponseMessage
                = postProcessSubmitResult(request, submitResult);

        if (processedResponseMessage.functionCall() != null && processedResponseMessage.functionCall().name() != null) {
            OpenAIChatMessage funcMessage = new OpenAIChatMessage(
                    OpenAIChatMessage.Role.FUNCTION,
                    processedResponseMessage.functionCall().name(),
                    processedResponseMessage.functionCall()
            );

            webSocketService.sendMessage(
                    new WSChatFunctionMessage(request.sessionId(), funcMessage)
            );
        } else {
            webSocketService.sendMessage(
                    new WSChatResponseMessage(request.sessionId(), processedResponseMessage)
            );
        }

        List<OpenAIChatMessage> responseMessages = new ArrayList<>(additionalResponseMessages);
        responseMessages.add(processedResponseMessage);
        return new ChatCompletionResponse(request.sessionId(), responseMessages);
    }

    public UUID create(NewChatSessionRequest request) {
        LOGGER.info("Creating new chat session for persona: {}", request.personaId());

        PersonaEntity persona = personaService.getPersonaById(request.personaId())
                .orElseThrow(() -> new PersonaException("Persona not found: " + request.personaId()));

        ChatSessionEntity session
                = sessionService.create(persona, new ArrayList<>(), new ArrayList<>());

        return session.getId();
    }

    private OpenAIChatMessage postProcessSubmitResult(ChatCompletionRequest request, SubmitResult submitResult) {
        if (submitResult.response().choices().isEmpty()) {
            throw new ChatException("Choices are empty.");
        }

        OpenAIChatResponse.ResponseChoice choice = submitResult.response().choices().get(0);
        LOGGER.info("Finish reason: {}", choice.finishReason());
        OpenAIChatMessage responseMessage = choice.message();

        List<OpenAIChatMessage> messagesToSave = new ArrayList<>();
        messagesToSave.add(submitResult.newUserMessage());
        messagesToSave.add(responseMessage);

        List<OpenAIChatMessage> processedMessagesToSave = new ArrayList<>();

        webSocketService.sendMessage(
                new WSChatStatusMessage(request.sessionId(), "Processing...")
        );

        OpenAIChatMessage processedResponseMessage = responseMessage;
        if (responseMessage.content() != null) {
            processedResponseMessage = messageProcessor.transform(responseMessage, submitResult.session().getId());
        }

        processedMessagesToSave.add(submitResult.processedNewUserMessage());
        processedMessagesToSave.add(processedResponseMessage);

        if (submitResult.isFirstSubmitInSession()) {
            sessionService.update(request.sessionId(), submitResult.newUserMessage().content(), "<empty>");
        }

        sessionService.update(request.sessionId(), messagesToSave, processedMessagesToSave);

        webSocketService.sendMessage(
                new WSChatStatusMessage(request.sessionId(), "")
        );

        return processedResponseMessage;

    }

    @NotNull
    private SubmitResult submit(ChatCompletionRequest request) {
        ChatSessionEntity session = sessionService.getById(request.sessionId())
                .orElseThrow(() -> new SessionException("Session not found: " + request.sessionId()));

        PersonaEntity persona = session.getPersona();
        List<OpenAIChatMessage> previousMessages = sessionService.getPreviousMessages(session);
        boolean isFirstSubmitInSession = previousMessages.isEmpty();

        OpenAIChatMessage newUserMessage = new OpenAIChatMessage(OpenAIChatMessage.Role.USER, request.content());
        // TODO Postprocessing of new user delta
        OpenAIChatMessage processedNewUserMessage = new OpenAIChatMessage(OpenAIChatMessage.Role.USER, request.content());

        List<OpenAIChatMessage> messagePayload = sessionService.composeMessagePayload(previousMessages, processedNewUserMessage, persona);

        webSocketService.sendMessage(
                new WSChatStatusMessage(request.sessionId(), "Thinking...")
        );

        OpenAIChatResponse response = submit(messagePayload, persona);
        return new SubmitResult(session, isFirstSubmitInSession, newUserMessage, processedNewUserMessage, response);
    }


    private Optional<OpenAIChatMessage> getLastMessage(ChatSessionEntity session) {
        List<OpenAIChatMessage> previousMessages = sessionService.getPreviousMessages(session);
        LOGGER.info("Previous messages: {}", previousMessages);

        if (previousMessages != null && !previousMessages.isEmpty()) {
            return Optional.of(previousMessages.get(previousMessages.size() - 1));
        }
        return Optional.empty();
    }

    private OpenAIChatResponse submit(List<OpenAIChatMessage> messages, PersonaEntity persona) {
        Map<String, PersonaPropertyValue> properties = persona.getProperties();

        try {
            OpenAIChatRequest request = new OpenAIChatRequest();
            request.setMessages(messages);

            if (properties.containsKey(PersonaProperties.CHATGPT_TOP_P.getKey())) {
                PersonaPropertyValue property = properties.get(PersonaProperties.CHATGPT_TOP_P.getKey());
                request.setTopP(Double.valueOf(property.getPropertyValue()));
            }

            if (properties.containsKey(PersonaProperties.CHATGPT_MODEL.getKey())) {
                PersonaPropertyValue property = properties.get(PersonaProperties.CHATGPT_MODEL.getKey());
                request.setModel(property.getPropertyValue());
            }

            if (properties.containsKey(PersonaProperties.CHATGPT_PRESENCE_PENALTY.getKey())) {
                PersonaPropertyValue property = properties.get(PersonaProperties.CHATGPT_PRESENCE_PENALTY.getKey());
                request.setFrequencyPenalty(Double.valueOf(property.getPropertyValue()));
            }

            if (properties.containsKey(PersonaProperties.CHATGPT_FREQUENCY_PENALTY.getKey())) {
                PersonaPropertyValue property = properties.get(PersonaProperties.CHATGPT_FREQUENCY_PENALTY.getKey());
                request.setPresencePenalty(Double.valueOf(property.getPropertyValue()));
            }

            if (properties.containsKey(PersonaProperties.CHATGPT_TEMPERATURE.getKey())) {
                PersonaPropertyValue property = properties.get(PersonaProperties.CHATGPT_TEMPERATURE.getKey());
                request.setTemperature(Double.valueOf(property.getPropertyValue()));
            }

            List<RequestFunction> requestFunctions = persona.getRequestFunctions();
            if (!requestFunctions.isEmpty()) {
                request.setFunctions(functionRepository.getByRequestFunctions(requestFunctions));
            }

            return openAIChatService.submit(request);
        } catch (Exception e) {
            LOGGER.error("Error while submitting chat request.", e);
        }

        return null;
    }

    public Optional<OpenAIChatMessage> postProcessLastMessage(UUID sessionId) {
        List<ChatMessageEntity> messages = sessionService.getMessages(sessionId, ChatMessageType.UNPROCESSED);

        ChatMessageEntity lastMessage;
        if (!messages.isEmpty()) {
            lastMessage = messages.get(messages.size() - 1);
            OpenAIChatMessage openAIChatMessage = sessionService.mapToOpenAIMessage(lastMessage);
            OpenAIChatMessage transformed = messageProcessor.transform(openAIChatMessage, sessionId);

            sessionService.saveMessage(sessionId, transformed, ChatMessageType.PROCESSED);
            return Optional.of(transformed);
        }
        return Optional.empty();
    }

    private record SubmitResult(ChatSessionEntity session,
                                boolean isFirstSubmitInSession,
                                OpenAIChatMessage newUserMessage,
                                OpenAIChatMessage processedNewUserMessage,
                                OpenAIChatResponse response) {
    }

}
