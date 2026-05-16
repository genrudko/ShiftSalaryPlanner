package com.vigilante.shiftsalaryplanner.settings

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AssistantAiProvider(
    val label: String,
    val shortLabel: String,
    val defaultModel: String,
    val keyPlaceholder: String,
    val freeTierHint: String
) {
    GIGACHAT(
        label = "GigaChat",
        shortLabel = "GigaChat",
        defaultModel = "GigaChat-2",
        keyPlaceholder = "Authorization key",
        freeTierHint = "Freemium от Сбера: удобно для русского языка и команд планировщика."
    ),
    GEMINI(
        label = "Google Gemini",
        shortLabel = "Gemini",
        defaultModel = "gemini-2.5-flash",
        keyPlaceholder = "AIza...",
        freeTierHint = "Бесплатный лимит Gemini API через Google AI Studio."
    ),
    OPENAI(
        label = "OpenAI",
        shortLabel = "OpenAI",
        defaultModel = "gpt-5.4-mini",
        keyPlaceholder = "sk-...",
        freeTierHint = "Платный API OpenAI. Подписка ChatGPT Plus отдельно не оплачивает API."
    );

    companion object {
        const val DEFAULT_OPENAI_MODEL = "gpt-5.4-mini"

        fun fromStoredName(value: String?): AssistantAiProvider {
            return entries.firstOrNull { it.name == value } ?: GIGACHAT
        }
    }
}

enum class AssistantVoiceReplyMode(
    val label: String,
    val description: String
) {
    SYSTEM(
        label = "Локальный",
        description = "Бесплатный системный TTS-движок Android."
    ),
    OPENAI(
        label = "Нейро OpenAI",
        description = "Более живой AI-голос через OpenAI Audio API."
    ),
    GEMINI(
        label = "Gemini",
        description = "AI-голос Gemini TTS через Google AI Studio API."
    ),
    SALUTE_SPEECH(
        label = "SaluteSpeech",
        description = "Голос Сбера через отдельный SaluteSpeech API."
    );

    companion object {
        fun fromStoredName(value: String?): AssistantVoiceReplyMode {
            return entries.firstOrNull { it.name == value } ?: SYSTEM
        }
    }
}

data class AssistantAiModelOption(
    val id: String,
    val title: String,
    val description: String,
    val supportsImages: Boolean = true,
    val recommended: Boolean = false
)

fun AssistantAiProvider.modelOptions(): List<AssistantAiModelOption> {
    return when (this) {
        AssistantAiProvider.GIGACHAT -> listOf(
            AssistantAiModelOption(
                id = "GigaChat-2",
                title = "GigaChat 2 Lite",
                description = "Быстро и дешевле, хорошо для простых команд.",
                supportsImages = true,
                recommended = true
            ),
            AssistantAiModelOption(
                id = "GigaChat-2-Pro",
                title = "GigaChat 2 Pro",
                description = "Лучше следует сложным инструкциям.",
                supportsImages = true
            ),
            AssistantAiModelOption(
                id = "GigaChat-2-Max",
                title = "GigaChat 2 Max",
                description = "Самая сильная модель GigaChat.",
                supportsImages = true
            )
        )
        AssistantAiProvider.GEMINI -> listOf(
            AssistantAiModelOption(
                id = "gemini-2.5-flash",
                title = "Gemini 2.5 Flash",
                description = "Лучший баланс скорости, цены и понимания скриншотов.",
                recommended = true
            ),
            AssistantAiModelOption(
                id = "gemini-2.5-flash-lite",
                title = "Gemini 2.5 Flash-Lite",
                description = "Самый быстрый и экономичный вариант."
            ),
            AssistantAiModelOption(
                id = "gemini-2.5-pro",
                title = "Gemini 2.5 Pro",
                description = "Для сложного разбора графиков и неоднозначных команд."
            ),
            AssistantAiModelOption(
                id = "gemini-flash-latest",
                title = "Gemini Flash latest",
                description = "Автоматически актуальная Flash-ветка."
            )
        )
        AssistantAiProvider.OPENAI -> listOf(
            AssistantAiModelOption(
                id = "gpt-5.4-mini",
                title = "GPT-5.4 mini",
                description = "Рекомендуемый баланс качества, vision и стоимости.",
                recommended = true
            ),
            AssistantAiModelOption(
                id = "gpt-5.4-nano",
                title = "GPT-5.4 nano",
                description = "Самый быстрый и экономичный вариант."
            ),
            AssistantAiModelOption(
                id = "gpt-5.4",
                title = "GPT-5.4",
                description = "Сильнее для сложных формулировок и длинного контекста."
            ),
            AssistantAiModelOption(
                id = "gpt-5.5",
                title = "GPT-5.5",
                description = "Флагманская модель, если доступна в проекте."
            ),
            AssistantAiModelOption(
                id = "gpt-4.1-mini",
                title = "GPT-4.1 mini",
                description = "Совместимый запасной вариант для старых проектов."
            )
        )
    }
}

data class AssistantAiSettings(
    val advancedModeEnabled: Boolean = false,
    val provider: AssistantAiProvider = AssistantAiProvider.GIGACHAT,
    val openAiApiKey: String = "",
    val openAiModel: String = AssistantAiProvider.OPENAI.defaultModel,
    val geminiApiKey: String = "",
    val geminiModel: String = AssistantAiProvider.GEMINI.defaultModel,
    val gigaChatApiKey: String = "",
    val gigaChatModel: String = AssistantAiProvider.GIGACHAT.defaultModel,
    val confirmBeforeActions: Boolean = true,
    val confirmNoteActions: Boolean = false,
    val sendFinancialContextToAi: Boolean = false,
    val sendChatHistoryToAi: Boolean = true,
    val assistantMemory: String = "",
    val voiceRepliesEnabled: Boolean = false,
    val voiceReplyMode: AssistantVoiceReplyMode = AssistantVoiceReplyMode.SYSTEM,
    val systemTtsEnginePackage: String = "",
    val openAiTtsModel: String = "gpt-4o-mini-tts",
    val openAiTtsVoice: String = "coral",
    val openAiTtsInstructions: String = "Говори по-русски спокойно, дружелюбно и кратко.",
    val geminiTtsModel: String = "gemini-2.5-flash-preview-tts",
    val geminiTtsVoice: String = "Kore",
    val geminiTtsInstructions: String = "Say in a warm, concise Russian assistant voice:",
    val saluteSpeechAuthorizationKey: String = "",
    val saluteSpeechScope: String = "SALUTE_SPEECH_PERS",
    val saluteSpeechVoice: String = "Nec_24000",
    val chatBackgroundColorHex: String = "",
    val userBubbleColorHex: String = "",
    val assistantBubbleColorHex: String = ""
) {
    val selectedApiKey: String
        get() = when (provider) {
            AssistantAiProvider.GIGACHAT -> gigaChatApiKey
            AssistantAiProvider.GEMINI -> geminiApiKey
            AssistantAiProvider.OPENAI -> openAiApiKey
        }

    val selectedModel: String
        get() = when (provider) {
            AssistantAiProvider.GIGACHAT -> gigaChatModel.ifBlank { provider.defaultModel }
            AssistantAiProvider.GEMINI -> geminiModel.ifBlank { provider.defaultModel }
            AssistantAiProvider.OPENAI -> openAiModel.ifBlank { provider.defaultModel }
        }

    val hasApiKey: Boolean
        get() = selectedApiKey.isNotBlank()

    fun withSelectedApiKey(value: String): AssistantAiSettings {
        return when (provider) {
            AssistantAiProvider.GIGACHAT -> copy(gigaChatApiKey = value)
            AssistantAiProvider.GEMINI -> copy(geminiApiKey = value)
            AssistantAiProvider.OPENAI -> copy(openAiApiKey = value)
        }
    }

    fun withSelectedModel(value: String): AssistantAiSettings {
        val model = value.ifBlank { provider.defaultModel }
        return when (provider) {
            AssistantAiProvider.GIGACHAT -> copy(gigaChatModel = model)
            AssistantAiProvider.GEMINI -> copy(geminiModel = model)
            AssistantAiProvider.OPENAI -> copy(openAiModel = model)
        }
    }

    companion object {
        const val DEFAULT_OPENAI_MODEL = "gpt-5.4-mini"
    }
}

class AssistantAiSettingsStore(context: Context) {
    private val prefs = context.profileSharedPreferences(PREFS_NAME)
    private val _settingsFlow = MutableStateFlow(load())

    val settingsFlow: Flow<AssistantAiSettings> = _settingsFlow.asStateFlow()

    fun save(settings: AssistantAiSettings) {
        prefs.edit {
            putBoolean(KEY_ADVANCED_ENABLED, settings.advancedModeEnabled)
            putString(KEY_PROVIDER, settings.provider.name)
            putString(KEY_OPENAI_API_KEY, settings.openAiApiKey)
            putString(KEY_OPENAI_MODEL, settings.openAiModel)
            putString(KEY_GEMINI_API_KEY, settings.geminiApiKey)
            putString(KEY_GEMINI_MODEL, settings.geminiModel)
            putString(KEY_GIGACHAT_API_KEY, settings.gigaChatApiKey)
            putString(KEY_GIGACHAT_MODEL, settings.gigaChatModel)
            putBoolean(KEY_CONFIRM_BEFORE_ACTIONS, settings.confirmBeforeActions)
            putBoolean(KEY_CONFIRM_NOTE_ACTIONS, settings.confirmNoteActions)
            putBoolean(KEY_SEND_FINANCIAL_CONTEXT, settings.sendFinancialContextToAi)
            putBoolean(KEY_SEND_CHAT_HISTORY, settings.sendChatHistoryToAi)
            putString(KEY_ASSISTANT_MEMORY, settings.assistantMemory)
            putBoolean(KEY_VOICE_REPLIES_ENABLED, settings.voiceRepliesEnabled)
            putString(KEY_VOICE_REPLY_MODE, settings.voiceReplyMode.name)
            putString(KEY_SYSTEM_TTS_ENGINE, settings.systemTtsEnginePackage)
            putString(KEY_OPENAI_TTS_MODEL, settings.openAiTtsModel)
            putString(KEY_OPENAI_TTS_VOICE, settings.openAiTtsVoice)
            putString(KEY_OPENAI_TTS_INSTRUCTIONS, settings.openAiTtsInstructions)
            putString(KEY_GEMINI_TTS_MODEL, settings.geminiTtsModel)
            putString(KEY_GEMINI_TTS_VOICE, settings.geminiTtsVoice)
            putString(KEY_GEMINI_TTS_INSTRUCTIONS, settings.geminiTtsInstructions)
            putString(KEY_SALUTE_SPEECH_AUTHORIZATION_KEY, settings.saluteSpeechAuthorizationKey)
            putString(KEY_SALUTE_SPEECH_SCOPE, settings.saluteSpeechScope)
            putString(KEY_SALUTE_SPEECH_VOICE, settings.saluteSpeechVoice)
            putString(KEY_CHAT_BACKGROUND_COLOR, settings.chatBackgroundColorHex)
            putString(KEY_USER_BUBBLE_COLOR, settings.userBubbleColorHex)
            putString(KEY_ASSISTANT_BUBBLE_COLOR, settings.assistantBubbleColorHex)
        }
        _settingsFlow.value = settings
    }

    private fun load(): AssistantAiSettings {
        val legacyApiKey = prefs.getString(KEY_API_KEY, "").orEmpty()
        val legacyModel = prefs.getString(KEY_MODEL, AssistantAiSettings.DEFAULT_OPENAI_MODEL)
            .orEmpty()
            .ifBlank { AssistantAiSettings.DEFAULT_OPENAI_MODEL }
        val storedProvider = prefs.getString(KEY_PROVIDER, null)
        return AssistantAiSettings(
            advancedModeEnabled = prefs.getBoolean(KEY_ADVANCED_ENABLED, false),
            provider = if (storedProvider == null && legacyApiKey.isNotBlank()) {
                AssistantAiProvider.OPENAI
            } else {
                AssistantAiProvider.fromStoredName(storedProvider)
            },
            openAiApiKey = prefs.getString(KEY_OPENAI_API_KEY, legacyApiKey).orEmpty(),
            openAiModel = prefs.getString(KEY_OPENAI_MODEL, legacyModel)
                .orEmpty()
                .ifBlank { AssistantAiProvider.OPENAI.defaultModel },
            geminiApiKey = prefs.getString(KEY_GEMINI_API_KEY, "").orEmpty(),
            geminiModel = prefs.getString(KEY_GEMINI_MODEL, AssistantAiProvider.GEMINI.defaultModel)
                .orEmpty()
                .ifBlank { AssistantAiProvider.GEMINI.defaultModel },
            gigaChatApiKey = prefs.getString(KEY_GIGACHAT_API_KEY, "").orEmpty(),
            gigaChatModel = prefs.getString(KEY_GIGACHAT_MODEL, AssistantAiProvider.GIGACHAT.defaultModel)
                .orEmpty()
                .ifBlank { AssistantAiProvider.GIGACHAT.defaultModel },
            confirmBeforeActions = prefs.getBoolean(KEY_CONFIRM_BEFORE_ACTIONS, true),
            confirmNoteActions = prefs.getBoolean(KEY_CONFIRM_NOTE_ACTIONS, false),
            sendFinancialContextToAi = prefs.getBoolean(KEY_SEND_FINANCIAL_CONTEXT, false),
            sendChatHistoryToAi = prefs.getBoolean(KEY_SEND_CHAT_HISTORY, true),
            assistantMemory = prefs.getString(KEY_ASSISTANT_MEMORY, "").orEmpty(),
            voiceRepliesEnabled = prefs.getBoolean(KEY_VOICE_REPLIES_ENABLED, false),
            voiceReplyMode = AssistantVoiceReplyMode.fromStoredName(prefs.getString(KEY_VOICE_REPLY_MODE, null)),
            systemTtsEnginePackage = prefs.getString(KEY_SYSTEM_TTS_ENGINE, "").orEmpty(),
            openAiTtsModel = prefs.getString(KEY_OPENAI_TTS_MODEL, "gpt-4o-mini-tts")
                .orEmpty()
                .ifBlank { "gpt-4o-mini-tts" },
            openAiTtsVoice = prefs.getString(KEY_OPENAI_TTS_VOICE, "coral")
                .orEmpty()
                .ifBlank { "coral" },
            openAiTtsInstructions = prefs.getString(
                KEY_OPENAI_TTS_INSTRUCTIONS,
                "Говори по-русски спокойно, дружелюбно и кратко."
            ).orEmpty(),
            geminiTtsModel = prefs.getString(KEY_GEMINI_TTS_MODEL, "gemini-2.5-flash-preview-tts")
                .orEmpty()
                .ifBlank { "gemini-2.5-flash-preview-tts" },
            geminiTtsVoice = prefs.getString(KEY_GEMINI_TTS_VOICE, "Kore")
                .orEmpty()
                .ifBlank { "Kore" },
            geminiTtsInstructions = prefs.getString(
                KEY_GEMINI_TTS_INSTRUCTIONS,
                "Say in a warm, concise Russian assistant voice:"
            ).orEmpty(),
            saluteSpeechAuthorizationKey = prefs.getString(KEY_SALUTE_SPEECH_AUTHORIZATION_KEY, "").orEmpty(),
            saluteSpeechScope = prefs.getString(KEY_SALUTE_SPEECH_SCOPE, "SALUTE_SPEECH_PERS")
                .orEmpty()
                .ifBlank { "SALUTE_SPEECH_PERS" },
            saluteSpeechVoice = prefs.getString(KEY_SALUTE_SPEECH_VOICE, "Nec_24000")
                .orEmpty()
                .ifBlank { "Nec_24000" },
            chatBackgroundColorHex = prefs.getString(KEY_CHAT_BACKGROUND_COLOR, "").orEmpty(),
            userBubbleColorHex = prefs.getString(KEY_USER_BUBBLE_COLOR, "").orEmpty(),
            assistantBubbleColorHex = prefs.getString(KEY_ASSISTANT_BUBBLE_COLOR, "").orEmpty()
        )
    }

    companion object {
        const val PREFS_NAME = "assistant_ai_settings"

        private const val KEY_ADVANCED_ENABLED = "advanced_enabled"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_MODEL = "model"
        private const val KEY_PROVIDER = "provider"
        private const val KEY_OPENAI_API_KEY = "openai_api_key"
        private const val KEY_OPENAI_MODEL = "openai_model"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_GEMINI_MODEL = "gemini_model"
        private const val KEY_GIGACHAT_API_KEY = "gigachat_api_key"
        private const val KEY_GIGACHAT_MODEL = "gigachat_model"
        private const val KEY_CONFIRM_BEFORE_ACTIONS = "confirm_before_actions"
        private const val KEY_CONFIRM_NOTE_ACTIONS = "confirm_note_actions"
        private const val KEY_SEND_FINANCIAL_CONTEXT = "send_financial_context"
        private const val KEY_SEND_CHAT_HISTORY = "send_chat_history"
        private const val KEY_ASSISTANT_MEMORY = "assistant_memory"
        private const val KEY_VOICE_REPLIES_ENABLED = "voice_replies_enabled"
        private const val KEY_VOICE_REPLY_MODE = "voice_reply_mode"
        private const val KEY_SYSTEM_TTS_ENGINE = "system_tts_engine"
        private const val KEY_OPENAI_TTS_MODEL = "openai_tts_model"
        private const val KEY_OPENAI_TTS_VOICE = "openai_tts_voice"
        private const val KEY_OPENAI_TTS_INSTRUCTIONS = "openai_tts_instructions"
        private const val KEY_GEMINI_TTS_MODEL = "gemini_tts_model"
        private const val KEY_GEMINI_TTS_VOICE = "gemini_tts_voice"
        private const val KEY_GEMINI_TTS_INSTRUCTIONS = "gemini_tts_instructions"
        private const val KEY_SALUTE_SPEECH_AUTHORIZATION_KEY = "salute_speech_authorization_key"
        private const val KEY_SALUTE_SPEECH_SCOPE = "salute_speech_scope"
        private const val KEY_SALUTE_SPEECH_VOICE = "salute_speech_voice"
        private const val KEY_CHAT_BACKGROUND_COLOR = "chat_background_color"
        private const val KEY_USER_BUBBLE_COLOR = "user_bubble_color"
        private const val KEY_ASSISTANT_BUBBLE_COLOR = "assistant_bubble_color"
    }
}
