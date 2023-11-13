/*
 * Copyright (c) 2023 Jean Schmitz.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.talkforgeai.backend.assistant.service;


import com.talkforgeai.backend.assistant.domain.AssistantPropertyValue;
import com.talkforgeai.backend.assistant.dto.AssistantDto;
import com.talkforgeai.service.openai.assistant.dto.Assistant;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.talkforgeai.backend.persona.service.PersonaProperties.values;
import static java.util.Objects.requireNonNullElse;

@Component
public class AssistantMapper {

    AssistantDto mapAssistantDto(Assistant assistant, Map<String, String> properties) {
        return new AssistantDto(
                assistant.id(),
                assistant.object(),
                assistant.createdAt(),
                assistant.name(),
                assistant.description(),
                assistant.model(),
                assistant.instructions(),
                assistant.tools(),
                assistant.fileIds(),
                assistant.metadata(),
                properties
        );
    }

    public Map<String, String> mapAssistantProperties(Map<String, AssistantPropertyValue> properties) {
        Map<String, String> mappedProperties = new HashMap<>();

        Arrays.stream(values()).forEach(property -> {
            AssistantPropertyValue propertyValue = properties.get(property.getKey());
            if (propertyValue != null) {
                mappedProperties.put(
                        property.getKey(),
                        requireNonNullElse(propertyValue.getPropertyValue(), property.getKey())
                );
            }
        });

        return mappedProperties;
    }

    public Map<String, AssistantPropertyValue> mapProperties(Map<String, String> properties) {
        Map<String, AssistantPropertyValue> mappedProperties = new HashMap<>();

        Arrays.stream(values()).forEach(property -> {
            String propertyValue = properties.get(property.getKey());
            AssistantPropertyValue assistantPropertyValue = new AssistantPropertyValue();
            assistantPropertyValue.setPropertyValue(propertyValue);
            mappedProperties.put(property.getKey(), assistantPropertyValue);
        });

        return mappedProperties;
    }
}
