package com.talkforgeai.backend.chat.service;

import com.talkforgeai.backend.chat.exception.ChatException;
import com.talkforgeai.backend.persona.domain.GlobalSystem;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class SystemService {

    public static final String IMAGE_GEN_SYSTEM = "Create an image content in English to generate an image whenever instructed to output an image. " +
            "The content should inspire creativity and imagination, and be vividly descriptive. Describe the scene from the view of a third-person stranger. For example, " +
            "\"A painting of a stormy sea with a lighthouse in the distance,\" or \"A photograph of a city street at night, illuminated by neon lights.\". " +
            "Place the content between the following tags: <image-prompt></image-prompt>.";

    public String getContent(GlobalSystem system) {
        if (Objects.requireNonNull(system) == GlobalSystem.IMAGE_GEN) {
            return IMAGE_GEN_SYSTEM;
        }

        throw new ChatException("Unknown global system: " + system);
    }
}
