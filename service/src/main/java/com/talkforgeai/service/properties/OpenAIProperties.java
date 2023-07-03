package com.talkforgeai.service.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openai")
public record OpenAIProperties(String apiKey, String chatUrl, String imageUrl) {

}