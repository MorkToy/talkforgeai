package com.talkforgeai.talkforgeaiserver.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talkforgeai.talkforgeaiserver.openai.dto.OpenAIRequest;
import com.talkforgeai.talkforgeaiserver.openai.dto.OpenAIResponse;
import com.talkforgeai.talkforgeaiserver.properties.OpenAIProperties;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class OpenAIChatService {
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OpenAIProperties openAIProperties;
    private final OkHttpClient client;
    Logger logger = LoggerFactory.getLogger(OpenAIChatService.class);

    public OpenAIChatService(OpenAIProperties openAIProperties, OkHttpClient client) {
        this.openAIProperties = openAIProperties;
        this.client = client;
    }

    public OpenAIResponse submit(OpenAIRequest openAIRequest) {
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

            logger.debug("Sending message: {}", message);

            try (Response response = client.newCall(request).execute()) {

                return objectMapper.readValue(response.body().string(), OpenAIResponse.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
