package com.talkforgeai.backend.chat.service;

import com.talkforgeai.backend.chat.domain.ChatMessageType;
import com.talkforgeai.backend.chat.dto.ChatCompletionRequest;
import com.talkforgeai.backend.chat.repository.FunctionRepository;
import com.talkforgeai.backend.persona.domain.PersonaEntity;
import com.talkforgeai.backend.persona.domain.RequestFunction;
import com.talkforgeai.backend.persona.service.PersonaProperties;
import com.talkforgeai.backend.session.domain.ChatSessionEntity;
import com.talkforgeai.backend.session.exception.SessionException;
import com.talkforgeai.backend.session.service.SessionService;
import com.talkforgeai.backend.websocket.dto.WSChatStatusMessage;
import com.talkforgeai.backend.websocket.service.WebSocketService;
import com.talkforgeai.service.openai.OpenAIChatService;
import com.talkforgeai.service.openai.dto.OpenAIChatMessage;
import com.talkforgeai.service.openai.dto.OpenAIChatRequest;
import com.talkforgeai.service.openai.dto.OpenAIChatStreamResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ChatStreamService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatStreamService.class);

    private final OpenAIChatService openAIChatService;
    private final SessionService sessionService;
    private final WebSocketService webSocketService;
    private final FunctionRepository functionRepository;


    public ChatStreamService(OpenAIChatService openAIChatService,
                             SessionService sessionService,
                             WebSocketService webSocketService,
                             FunctionRepository functionRepository) {
        this.openAIChatService = openAIChatService;
        this.sessionService = sessionService;
        this.webSocketService = webSocketService;
        this.functionRepository = functionRepository;
    }

    public Flux<ServerSentEvent<OpenAIChatStreamResponse.StreamResponseChoice>> submit(ChatCompletionRequest request) {
        ChatSessionEntity session = sessionService.getById(request.sessionId())
                .orElseThrow(() -> new SessionException("Session not found: " + request.sessionId()));

        PersonaEntity persona = session.getPersona();
        List<OpenAIChatMessage> previousMessages = sessionService.getPreviousMessages(session);
        boolean isFirstSubmitInSession = previousMessages.isEmpty();

        OpenAIChatMessage newUserMessage = new OpenAIChatMessage(OpenAIChatMessage.Role.USER, request.content());
        sessionService.saveMessage(session.getId(), newUserMessage, ChatMessageType.UNPROCESSED);

        // TODO Postprocessing of new user delta
        OpenAIChatMessage processedNewUserMessage = new OpenAIChatMessage(OpenAIChatMessage.Role.USER, request.content());
        sessionService.saveMessage(session.getId(), processedNewUserMessage, ChatMessageType.PROCESSED);

        List<OpenAIChatMessage> messagePayload = sessionService.composeMessagePayload(previousMessages, processedNewUserMessage, persona);

        webSocketService.sendMessage(
                new WSChatStatusMessage(request.sessionId(), "Thinking...")
        );

        return submit(session.getId(), messagePayload, persona);
//        return new SubmitResult(session, isFirstSubmitInSession, newUserMessage, processedNewUserMessage, response);
    }

    private Flux<ServerSentEvent<OpenAIChatStreamResponse.StreamResponseChoice>> submit(UUID sessionId, List<OpenAIChatMessage> messages, PersonaEntity persona) {

        try {
            OpenAIChatRequest request = new OpenAIChatRequest();
            request.setMessages(messages);

            Map<String, String> properties = persona.getProperties();

            if (persona.getProperties().containsKey(PersonaProperties.CHATGPT_MAX_TOKENS)) {
                request.setMaxTokens(Integer.valueOf(properties.get(PersonaProperties.CHATGPT_MAX_TOKENS)));
            }

            if (properties.containsKey(PersonaProperties.CHATGPT_TOP_P)) {
                request.setTopP(Double.valueOf(properties.get(PersonaProperties.CHATGPT_TOP_P)));
            }

            if (properties.containsKey(PersonaProperties.CHATGPT_MODEL)) {
                request.setModel(properties.get(PersonaProperties.CHATGPT_MODEL));
            }

            if (properties.containsKey(PersonaProperties.CHATGPT_FREQUENCY_PENALTY)) {
                request.setFrequencyPenalty(Double.valueOf(properties.get(PersonaProperties.CHATGPT_FREQUENCY_PENALTY)));
            }

            if (properties.containsKey(PersonaProperties.CHATGPT_FREQUENCY_PENALTY)) {
                request.setPresencePenalty(Double.valueOf(properties.get(PersonaProperties.CHATGPT_PRESENCE_PENALTY)));
            }

            if (properties.containsKey(PersonaProperties.CHATGPT_TEMPERATURE)) {
                request.setTemperature(Double.valueOf(properties.get(PersonaProperties.CHATGPT_TEMPERATURE)));
            }


            List<RequestFunction> requestFunctions = persona.getRequestFunctions();
            if (!requestFunctions.isEmpty()) {
                request.setFunctions(functionRepository.getByRequestFunctions(requestFunctions));
            }

            StringBuilder finalContent = new StringBuilder();

            return openAIChatService.stream(request, message -> {
            }).doOnNext(response -> {
                finalContent.append(response.data().delta().content());
            }).doOnComplete(() -> {
                handleResultMessage(sessionId, new OpenAIChatMessage(
                        OpenAIChatMessage.Role.ASSISTANT,
                        finalContent.toString()));
            });
        } catch (Exception e) {
            LOGGER.error("Error while submitting chat request.", e);
        }

        return null;
    }

    private void handleResultMessage(UUID sessionId, OpenAIChatMessage message) {
        sessionService.saveMessage(sessionId, message, ChatMessageType.UNPROCESSED);

        webSocketService.sendMessage(
                new WSChatStatusMessage(sessionId, "")
        );
    }
}
