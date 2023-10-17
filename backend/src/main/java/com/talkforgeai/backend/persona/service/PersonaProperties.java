package com.talkforgeai.backend.persona.service;

public enum PersonaProperties {
    VOICE_TYPE("voice_type", ""),

    SPEECHAPI_VOICE("speechAPI_voice", ""),

    ELEVENLABS_VOICEID("elevenlabs_voiceId", ""),
    ELEVENLABS_MODELID("elevenlabs_modelId", "eleven_monolingual_v2"),
    ELEVENLABS_SIMILARITYBOOST("elevenlabs_similarityBoost", "0"),
    ELEVENLABS_STABILITY("elevenlabs_stability", "0"),

    CHATGPT_MODEL("chatgpt_model", "gpt-4"),
    CHATGPT_MAX_TOKENS("chatgpt_maxTokens", "4096"),
    CHATGPT_TEMPERATURE("chatgpt_temperature", "0.7"),
    CHATGPT_TOP_P("chatgpt_topP", "1.0"),
    CHATGPT_FREQUENCY_PENALTY("chatgpt_frequencyPenalty", "0"),
    CHATGPT_PRESENCE_PENALTY("chatgpt_presencePenalty", "0");

    private final String key;
    private final String defaultValue;

    PersonaProperties(String key, String defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getKey() {
        return key;
    }
}
