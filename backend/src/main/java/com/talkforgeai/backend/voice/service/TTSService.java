package com.talkforgeai.backend.voice.service;

import com.talkforgeai.backend.persona.domain.PersonaEntity;
import com.talkforgeai.backend.persona.domain.PropertyCategory;
import com.talkforgeai.backend.persona.domain.PropertyEntity;
import com.talkforgeai.backend.persona.service.PersonaService;
import com.talkforgeai.backend.voice.dto.TTSRequest;
import com.talkforgeai.service.elevenlabs.ElevenLabsService;
import com.talkforgeai.service.elevenlabs.ElevenlabsRequestProperties;
import com.talkforgeai.service.elevenlabs.dto.ElevenLabsRequest;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class TTSService {

    private final PersonaService personaService;
    private final ElevenLabsService elevenLabsService;

    public TTSService(ElevenLabsService elevenLabsService, PersonaService personaService) {
        this.personaService = personaService;
        this.elevenLabsService = elevenLabsService;
    }

    public byte[] streamVoice(TTSRequest TTSRequest) {
        PersonaEntity persona = personaService.getPersonaById(TTSRequest.personaId())
                .orElseThrow(() -> new RuntimeException("Persona not found:  " + TTSRequest.personaId()));

        Map<String, String> elevenlabsProperties = mapToElevenlabsProperties(persona.getProperties());

        ElevenLabsRequest request = new ElevenLabsRequest(
                TTSRequest.text(),
                elevenlabsProperties.get(ElevenlabsRequestProperties.VOICE_ID),
                elevenlabsProperties.get(ElevenlabsRequestProperties.MODEL_ID),
                new ElevenLabsRequest.VoiceSettings()
        );

        return elevenLabsService.stream(request);
    }

    private Map<String, String> mapToElevenlabsProperties(Map<String, PropertyEntity> properties) {
        Map<String, String> elevenlabsProperties = new HashMap<>();
        properties.forEach((key, value) -> {
            if (value.getCategory() == PropertyCategory.ELEVENLABS) {
                elevenlabsProperties.put(key, value.getPropertyValue());
            }
        });
        return elevenlabsProperties;
    }

}
