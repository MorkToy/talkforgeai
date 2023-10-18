package com.talkforgeai.backend.persona.service;

import com.talkforgeai.backend.persona.domain.PersonaEntity;
import com.talkforgeai.backend.persona.dto.PersonaDto;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.talkforgeai.backend.persona.service.PersonaProperties.values;
import static java.util.Objects.requireNonNullElse;

@Component
public class PersonaMapper {
    public PersonaDto mapPersonaResponse(PersonaEntity personaEntity) {
        return new PersonaDto(
                personaEntity.getId(),
                personaEntity.getName(),
                personaEntity.getDescription(),
                personaEntity.getSystem(),
                personaEntity.getRequestFunctions(),
                "/api/v1/persona/image/" + personaEntity.getImagePath(),
                personaEntity.getImagePath(),
                mapProperties(personaEntity.getProperties())
        );
    }

    Map<String, String> mapProperties(Map<String, String> properties) {
        Map<String, String> mappedProperties = new HashMap<>();

        Arrays.stream(values()).forEach(property -> {
            mappedProperties.put(
                    property.getKey(),
                    requireNonNullElse(properties.get(property.getKey()), property.getDefaultValue())
            );
        });

        return mappedProperties;
    }

    PersonaEntity mapPersonaDto(PersonaDto personaDto) {
        PersonaEntity entity = new PersonaEntity();

        entity.setId(personaDto.personaId());
        entity.setName(personaDto.name());
        entity.setDescription(personaDto.description());
        entity.setSystem(personaDto.system());
        entity.setRequestFunctions(personaDto.requestFunctions());
        entity.setImagePath(personaDto.imagePath());
        entity.setProperties(mapProperties(personaDto.properties()));
        return entity;
    }
}
