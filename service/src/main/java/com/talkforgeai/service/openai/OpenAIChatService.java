package com.talkforgeai.service.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talkforgeai.service.openai.dto.OpenAIChatMessage;
import com.talkforgeai.service.openai.dto.OpenAIChatRequest;
import com.talkforgeai.service.openai.dto.OpenAIChatResponse;
import com.talkforgeai.service.openai.dto.OpenAIChatStreamResponse;
import com.talkforgeai.service.properties.OpenAIProperties;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Service
public class OpenAIChatService {
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    public static final Logger LOGGER = LoggerFactory.getLogger(OpenAIChatService.class);
    private final OpenAIProperties openAIProperties;
    private final OkHttpClient client;

    private final Executor taskExecutor;

    public OpenAIChatService(OpenAIProperties openAIProperties, OkHttpClient client, @Qualifier("sseTaskExecutor") Executor taskExecutor) {
        this.openAIProperties = openAIProperties;
        this.client = client;
        this.taskExecutor = taskExecutor;
    }

    public OpenAIChatResponse submit(OpenAIChatRequest openAIRequest) {
        ObjectMapper objectMapper = new ObjectMapper();

        String message = null;
        try {
            message = objectMapper.writeValueAsString(openAIRequest);

            RequestBody body = RequestBody.create(message, JSON);
            Request request = new Request.Builder()
                    .url(openAIProperties.chatUrl())
                    .header("Authorization", "Bearer " + openAIProperties.apiKey())
                    .post(body)
                    .build();

            //logger.debug("Sending delta: {}", message);

            try (Response response = client.newCall(request).execute()) {

                return objectMapper.readValue(response.body().string(), OpenAIChatResponse.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public SseEmitter stream(OpenAIChatRequest openAIRequest, ResultCallback resultCallback) {
        SseEmitter emitter = new SseEmitter();
        openAIRequest.setStream(true);
        ObjectMapper objectMapper = new ObjectMapper();

        taskExecutor.execute(() -> {
            String message = null;
            try {
                message = objectMapper.writeValueAsString(openAIRequest);

                RequestBody body = RequestBody.create(message, JSON);
                Request request = new Request.Builder()
                        .url(openAIProperties.chatUrl())
                        .header("Authorization", "Bearer " + openAIProperties.apiKey())
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                LOGGER.error("Response received: {}", response);
                StringBuilder finalContent = new StringBuilder();
                boolean isFunctionCall = false;
                StringBuilder finalFunctionArguments = new StringBuilder();
                String functionName = null;

                if (!response.isSuccessful()) {
                    LOGGER.error("Response not successful: {}", response);
                    emitter.completeWithError(new RuntimeException("Unexpected code " + response));
                } else {
                    parseStreamResponse(resultCallback, response, isFunctionCall, functionName, finalFunctionArguments, finalContent, emitter);
                }

            } catch (IOException e) {
                LOGGER.error("Error while streaming.", e);
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private void parseStreamResponse(ResultCallback resultCallback, Response response, boolean isFunctionCall, String functionName, StringBuilder finalFunctionArguments, StringBuilder finalContent, SseEmitter emitter) throws IOException {
        ResponseBody responseBody = response.body();
        try (BufferedReader bufferedReader = new BufferedReader(responseBody.charStream())) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                Optional<OpenAIChatStreamResponse.StreamResponseChoice> responseChoice = parseLine(line);
                if (responseChoice.isPresent()) {
                    OpenAIChatMessage delta = responseChoice.get().delta();

                    if (delta.functionCall() != null) {
                        isFunctionCall = true;
                        if (functionName == null) {
                            functionName = delta.functionCall().name();
                        }
                        finalFunctionArguments.append(delta.functionCall().arguments());
                    } else {
                        finalContent.append(delta.content());
                    }

                    String responseChunk = choiceToJSON(responseChoice.get());
                    LOGGER.info("SENDING: {}", responseChunk);
                    emitter.send(responseChunk, org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);
                    TimeUnit.MILLISECONDS.sleep(50);
                }
            }
        } catch (IOException | InterruptedException ex) {
            emitter.completeWithError(ex);
            LOGGER.error("Error while streaming.", ex);
        }

        if (isFunctionCall) {
            OpenAIChatMessage.FunctionCall functionCall
                    = new OpenAIChatMessage.FunctionCall(functionName, finalFunctionArguments.toString());
            resultCallback.call(new OpenAIChatMessage(functionCall));
        } else {
            resultCallback.call(new OpenAIChatMessage(OpenAIChatMessage.Role.ASSISTANT, finalContent.toString()));
        }

        LOGGER.info("SENDING stream finished");
//        emitter.send(
//                "stream-done", org.springframework.http.MediaType.TEXT_PLAIN
//        );
        emitter.send(SseEmitter.event().name("complete").data("Stream has ended"));
        LOGGER.info("SENDING complete");
        emitter.complete();
    }

    private String choiceToJSON(OpenAIChatStreamResponse.StreamResponseChoice choice) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(choice);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Cannot parse choice.", e);
        }
    }

    private Optional<OpenAIChatStreamResponse.StreamResponseChoice> parseLine(String line) {
        ObjectMapper objectMapper = new ObjectMapper();

        int jsonStartIndex = line.indexOf('{');
        if (jsonStartIndex == -1) {
            return Optional.empty();
        }

        String jsonContent = line.substring(jsonStartIndex);

        try {
            OpenAIChatStreamResponse openAIChatStreamResponse = objectMapper.readValue(jsonContent, OpenAIChatStreamResponse.class);
            OpenAIChatStreamResponse.StreamResponseChoice streamResponseChoice = openAIChatStreamResponse.choices().get(0);
            if (streamResponseChoice.finishReason() == OpenAIChatStreamResponse.StreamResponseChoice.FinishReason.STOP) {
                return Optional.empty();
            }
            return Optional.of(streamResponseChoice);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error while parsing chunk line from stream.", e);
        }
    }
}
