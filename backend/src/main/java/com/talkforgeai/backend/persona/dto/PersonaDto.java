package com.talkforgeai.backend.persona.dto;

import com.talkforgeai.backend.persona.domain.GlobalSystem;
import com.talkforgeai.backend.persona.domain.RequestFunction;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PersonaDto(UUID personaId,
                         String name,
                         String description,
                         String system,

                         List<GlobalSystem> globalSystems,
                         List<RequestFunction> requestFunctions,
                         String imageUrl,
                         String imagePath,
                         Map<String, String> properties) {
}
