package com.talkforgeai.backend.transformers;

import com.talkforgeai.backend.transformers.dto.TransformerContext;
import com.talkforgeai.service.openai.OpenAIImageService;
import com.talkforgeai.service.openai.dto.OpenAIChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class MessageProcessor {
    private final OpenAIImageService openAIImageService;
    Logger logger = LoggerFactory.getLogger(MessageProcessor.class);
    List<Transformer> transformers = new ArrayList<>();

    public MessageProcessor(OpenAIImageService imageService) {
        this.openAIImageService = imageService;

        transformers.add(new CodeBlockTransformer());
        transformers.add(new ImageDownloadTransformer(openAIImageService));

    }

    public OpenAIChatMessage transform(OpenAIChatMessage message, UUID sessionId, Path dataDirectory) {
        if (message.content() == null || message.content().isEmpty()) {
            return new OpenAIChatMessage(message.role(), "");
        }

        String processedContent = message.content();

        TransformerContext context = new TransformerContext(sessionId, dataDirectory);

        for (Transformer t : transformers) {
            logger.info("Transforming with " + t.getClass().getName() + "...");
            processedContent = t.process(processedContent, context);
        }

        logger.info("Transformation done.");
        return new OpenAIChatMessage(message.role(), processedContent);
    }

}