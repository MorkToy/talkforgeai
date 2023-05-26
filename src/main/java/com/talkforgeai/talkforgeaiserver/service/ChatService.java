package com.talkforgeai.talkforgeaiserver.service;

import com.talkforgeai.talkforgeaiserver.domain.ChatMessageType;
import com.talkforgeai.talkforgeaiserver.domain.ChatSessionEntity;
import com.talkforgeai.talkforgeaiserver.domain.PersonaEntity;
import com.talkforgeai.talkforgeaiserver.exception.PersonaException;
import com.talkforgeai.talkforgeaiserver.exception.SessionException;
import com.talkforgeai.talkforgeaiserver.service.dto.ChatCompletionRequest;
import com.talkforgeai.talkforgeaiserver.service.dto.ChatCompletionResponse;
import com.talkforgeai.talkforgeaiserver.service.dto.NewChatSessionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ChatService {

    private final OpenAIChatService openAIChatService;
    private final PersonaService personaService;

    private final SessionService sessionService;

    private final MessageService messageService;

    public ChatService(OpenAIChatService openAIChatService,
                       SessionService sessionService,
                       PersonaService personaService,
                       MessageService messageService) {
        this.openAIChatService = openAIChatService;
        this.sessionService = sessionService;
        this.personaService = personaService;
        this.messageService = messageService;
    }

    public UUID create(NewChatSessionRequest request) {
        PersonaEntity persona = personaService.getPersonaByName(request.personaName())
            .orElseThrow(() -> new PersonaException("Persona not found: " + request.personaName()));

        ChatSessionEntity session
            = sessionService.createChatSession(persona, new ArrayList<>(), new ArrayList<>());

        return session.getId();
    }

    public ChatCompletionResponse submit(UUID sessionId, ChatCompletionRequest request) {
        ChatSessionEntity session = sessionService.getSession(sessionId)
            .orElseThrow(() -> new SessionException("Session not found: " + sessionId));

        PersonaEntity persona = session.getPersona();
        List<ChatMessage> previousMessages = getPreviousMessages(session);

        ChatMessage newUserMessage = new ChatMessage(ChatMessageRole.USER.value(), request.prompt());
        // TODO Postprocessing of new user message
        ChatMessage processedNewUserMessage = new ChatMessage(ChatMessageRole.USER.value(), request.prompt());

        List<ChatMessage> messagePayload = composeMessagePayload(previousMessages, processedNewUserMessage, persona);

        List<ChatCompletionChoice> choices = openAIChatService.submit(messagePayload);

        List<ChatMessage> responseMessages = choices.stream()
            .map(ChatCompletionChoice::getMessage)
            .toList();

        List<ChatMessage> messagesToSave = new ArrayList<>();
        messagesToSave.add(newUserMessage);
        messagesToSave.addAll(responseMessages);

        List<ChatMessage> processedMessagesToSave = new ArrayList<>();
        // TODO Postprocessing of response assistant messages
        List<ChatMessage> processedResponseMessages = new ArrayList<>();
        processedResponseMessages.addAll(responseMessages);

        processedMessagesToSave.add(processedNewUserMessage);
        processedMessagesToSave.addAll(processedResponseMessages);

        ChatSessionEntity updatedSession
            = sessionService.updateChatSession(sessionId, messagesToSave, processedMessagesToSave);

        return createResponse(newUserMessage, processedNewUserMessage, responseMessages,
            processedMessagesToSave, updatedSession);
    }

    private List<ChatMessage> getPreviousMessages(ChatSessionEntity session) {
        List<ChatMessage> previousMessages;
        previousMessages = session.getChatMessages().stream()
            .filter(m -> m.getType() == ChatMessageType.UNPROCESSED)
            .map(messageService::mapToDto)
            .toList();
        return previousMessages;
    }

    private ChatCompletionResponse createResponse(ChatMessage newUserMessage, ChatMessage processedNewUserMessage, List<ChatMessage> responseMessages, List<ChatMessage> processedMessagesToSave, ChatSessionEntity updatedSession) {
        List<ChatMessage> unprocessedMessagesForResponse = new ArrayList<>();
        unprocessedMessagesForResponse.add(newUserMessage);
        unprocessedMessagesForResponse.addAll(responseMessages);
        List<ChatMessage> proceccedMessagesForResponse = new ArrayList<>();
        proceccedMessagesForResponse.add(processedNewUserMessage);
        proceccedMessagesForResponse.addAll(processedMessagesToSave);

        return new ChatCompletionResponse(updatedSession.getId().toString(), unprocessedMessagesForResponse, unprocessedMessagesForResponse);
    }

    private List<ChatMessage> composeMessagePayload(List<ChatMessage> previousMessages, ChatMessage newMessage, PersonaEntity persona) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), SystemService.IMAGE_GEN_SYSTEM));
        messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), persona.getSystem()));
        messages.addAll(previousMessages);
        messages.add(newMessage);
        return messages;
    }

}
