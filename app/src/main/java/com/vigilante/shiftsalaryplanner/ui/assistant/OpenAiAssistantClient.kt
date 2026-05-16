package com.vigilante.shiftsalaryplanner

import android.util.Base64
import com.vigilante.shiftsalaryplanner.settings.AssistantAiSettings
import com.vigilante.shiftsalaryplanner.settings.AssistantAiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.cert.CertPathValidatorException
import java.time.LocalDate
import java.util.UUID
import javax.net.ssl.SSLHandshakeException

data class OpenAiAssistantRequestContext(
    val activeWorkplaceId: String,
    val activeWorkplaceName: String,
    val shifts: List<AssistantShiftOption>,
    val scheduledShifts: List<AssistantScheduledShift>,
    val upcomingPayments: List<UpcomingPaymentItem>,
    val financeContext: AssistantFinanceContext,
    val todaySummary: String,
    val tomorrowSummary: String,
    val nextAlarmSummary: String,
    val includeFinancialContext: Boolean = false,
    val assistantMemory: String = "",
    val recentMessages: List<String> = emptyList()
)

data class AssistantImageAttachment(
    val mimeType: String,
    val base64Data: String,
    val displayName: String = "Изображение",
    val byteSize: Int = 0
)

object AiAssistantClient {
    private const val RESPONSES_URL = "https://api.openai.com/v1/responses"
    private const val OPENAI_SPEECH_URL = "https://api.openai.com/v1/audio/speech"
    private const val GEMINI_URL_PREFIX = "https://generativelanguage.googleapis.com/v1beta/models/"
    private const val GIGACHAT_TOKEN_URL = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth"
    private const val GIGACHAT_CHAT_URL = "https://gigachat.devices.sberbank.ru/api/v1/chat/completions"
    private const val GIGACHAT_FILES_URL = "https://gigachat.devices.sberbank.ru/api/v1/files"
    private const val SALUTE_SPEECH_SYNTH_URL = "https://smartspeech.sber.ru/rest/v1/text:synthesize"
    private const val GIGACHAT_SCOPE = "GIGACHAT_API_PERS"

    suspend fun requestActionJson(
        settings: AssistantAiSettings,
        command: String,
        context: OpenAiAssistantRequestContext,
        imageAttachments: List<AssistantImageAttachment> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        runCatching {
            when (settings.provider) {
                AssistantAiProvider.GIGACHAT -> requestGigaChatActionJson(settings, command, context, imageAttachments)
                AssistantAiProvider.GEMINI -> requestGeminiActionJson(settings, command, context, imageAttachments)
                AssistantAiProvider.OPENAI -> requestOpenAiActionJson(settings, command, context, imageAttachments)
            }
        }.getOrElse { throwable ->
            formatTransportError(settings.provider, throwable)?.let { friendlyMessage ->
                error(friendlyMessage)
            }
            throw throwable
        }
    }

    suspend fun requestOpenAiSpeech(
        settings: AssistantAiSettings,
        text: String
    ): ByteArray = withContext(Dispatchers.IO) {
        val apiKey = settings.openAiApiKey.trim()
        if (apiKey.isBlank()) {
            error("Для нейро-голоса нужен OpenAI API-ключ.")
        }
        val cleanedText = text.trim().take(4_000)
        if (cleanedText.isBlank()) {
            error("Нет текста для озвучки.")
        }

        val connection = (URL(OPENAI_SPEECH_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 45_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json")
        }

        val model = settings.openAiTtsModel.ifBlank { "gpt-4o-mini-tts" }
        val body = JSONObject()
            .put("model", model)
            .put("voice", settings.openAiTtsVoice.ifBlank { "coral" })
            .put("input", cleanedText)
            .put("response_format", "mp3")
        if (model.contains("gpt-4o", ignoreCase = true)) {
            body.put(
                "instructions",
                settings.openAiTtsInstructions.ifBlank {
                    "Говори по-русски спокойно, дружелюбно и кратко."
                }
            )
        }

        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(body.toString())
        }

        val responseCode = connection.responseCode
        val bytes = if (responseCode in 200..299) {
            connection.inputStream.use { it.readBytes() }
        } else {
            val responseText = connection.errorStream.bufferedReader(Charsets.UTF_8).use(BufferedReader::readText)
            connection.disconnect()
            error("OpenAI TTS: ${formatOpenAiError(responseCode, responseText)}")
        }
        connection.disconnect()
        bytes
    }

    suspend fun requestGeminiSpeech(
        settings: AssistantAiSettings,
        text: String
    ): ByteArray = withContext(Dispatchers.IO) {
        val apiKey = settings.geminiApiKey.trim()
        if (apiKey.isBlank()) {
            error("Для Gemini TTS нужен Gemini API-ключ.")
        }
        val cleanedText = text.trim().take(4_000)
        if (cleanedText.isBlank()) {
            error("Нет текста для озвучки.")
        }
        val model = settings.geminiTtsModel.ifBlank { "gemini-2.5-flash-preview-tts" }
        val encodedModel = URLEncoder.encode(model, "UTF-8")
        val connection = (URL("$GEMINI_URL_PREFIX$encodedModel:generateContent").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 45_000
            doOutput = true
            setRequestProperty("x-goog-api-key", apiKey)
            setRequestProperty("Content-Type", "application/json")
        }

        val inputText = "${settings.geminiTtsInstructions.ifBlank { "Say in Russian:" }}\n$cleanedText"
        val body = JSONObject()
            .put(
                "contents",
                JSONArray().put(
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", inputText))
                    )
                )
            )
            .put(
                "generationConfig",
                JSONObject()
                    .put("responseModalities", JSONArray().put("AUDIO"))
                    .put(
                        "speechConfig",
                        JSONObject().put(
                            "voiceConfig",
                            JSONObject().put(
                                "prebuiltVoiceConfig",
                                JSONObject().put("voiceName", settings.geminiTtsVoice.ifBlank { "Kore" })
                            )
                        )
                    )
            )

        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(body.toString())
        }

        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        val responseText = stream.bufferedReader(Charsets.UTF_8).use(BufferedReader::readText)
        connection.disconnect()

        if (responseCode !in 200..299) {
            error("Gemini TTS: ${formatGeminiError(responseCode, responseText)}")
        }
        val base64Audio = extractGeminiAudioBase64(responseText)
            .ifBlank { error("Gemini TTS не вернул аудио.") }
        val pcmBytes = Base64.decode(base64Audio, Base64.DEFAULT)
        pcm16ToWav(pcmBytes, sampleRate = 24_000, channels = 1)
    }

    suspend fun requestSaluteSpeech(
        settings: AssistantAiSettings,
        text: String
    ): ByteArray = withContext(Dispatchers.IO) {
        val authorizationKey = settings.saluteSpeechAuthorizationKey.trim()
        if (authorizationKey.isBlank()) {
            error("Для SaluteSpeech нужен отдельный Authorization key.")
        }
        val cleanedText = text.trim().take(4_000)
        if (cleanedText.isBlank()) {
            error("Нет текста для озвучки.")
        }
        val accessToken = requestSaluteSpeechAccessToken(
            authorizationKey = authorizationKey,
            scope = settings.saluteSpeechScope.ifBlank { "SALUTE_SPEECH_PERS" }
        )
        val voice = URLEncoder.encode(settings.saluteSpeechVoice.ifBlank { "Nec_24000" }, "UTF-8")
        val connection = (URL("$SALUTE_SPEECH_SYNTH_URL?format=wav16&voice=$voice").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 45_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Content-Type", "application/text")
            setRequestProperty("Accept", "audio/wav")
        }

        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(cleanedText)
        }

        val responseCode = connection.responseCode
        val bytes = if (responseCode in 200..299) {
            connection.inputStream.use { it.readBytes() }
        } else {
            val responseText = connection.errorStream.bufferedReader(Charsets.UTF_8).use(BufferedReader::readText)
            connection.disconnect()
            error("SaluteSpeech: ${formatGigaChatError(responseCode, responseText)}")
        }
        connection.disconnect()
        bytes
    }

    private fun requestOpenAiActionJson(
        settings: AssistantAiSettings,
        command: String,
        context: OpenAiAssistantRequestContext,
        imageAttachments: List<AssistantImageAttachment>
    ): String {
        val connection = (URL(RESPONSES_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer ${settings.openAiApiKey.trim()}")
            setRequestProperty("Content-Type", "application/json")
        }

        val body = JSONObject()
            .put("model", settings.openAiModel.ifBlank { AssistantAiProvider.OPENAI.defaultModel })
            .put("instructions", assistantInstructions())
            .put("max_output_tokens", 700)
            .put(
                "text",
                JSONObject().put(
                    "format",
                    JSONObject().put("type", "json_object")
                )
            )
            .put("input", buildOpenAiInput(command, context, imageAttachments))

        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(body.toString())
        }

        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }
        val responseText = stream.bufferedReader(Charsets.UTF_8).use(BufferedReader::readText)
        connection.disconnect()

        if (responseCode !in 200..299) {
            error("OpenAI API: ${formatOpenAiError(responseCode, responseText)}")
        }

        return sanitizeJsonText(extractOpenAiOutputText(responseText))
    }

    private fun requestGeminiActionJson(
        settings: AssistantAiSettings,
        command: String,
        context: OpenAiAssistantRequestContext,
        imageAttachments: List<AssistantImageAttachment>
    ): String {
        val model = settings.geminiModel.ifBlank { AssistantAiProvider.GEMINI.defaultModel }
        val encodedModel = URLEncoder.encode(model, "UTF-8")
        val connection = (URL("$GEMINI_URL_PREFIX$encodedModel:generateContent").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("x-goog-api-key", settings.geminiApiKey.trim())
            setRequestProperty("Content-Type", "application/json")
        }

        val body = JSONObject()
            .put(
                "systemInstruction",
                JSONObject().put(
                    "parts",
                    JSONArray().put(JSONObject().put("text", assistantInstructions()))
                )
            )
            .put(
                "contents",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put("parts", buildGeminiParts(command, context, imageAttachments))
                )
            )
            .put(
                "generationConfig",
                JSONObject()
                    .put("responseMimeType", "application/json")
                    .put("maxOutputTokens", 700)
                    .put("temperature", 0.15)
            )

        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(body.toString())
        }

        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        val responseText = stream.bufferedReader(Charsets.UTF_8).use(BufferedReader::readText)
        connection.disconnect()

        if (responseCode !in 200..299) {
            error("Gemini API: ${formatGeminiError(responseCode, responseText)}")
        }

        return sanitizeJsonText(extractGeminiOutputText(responseText))
    }

    private fun requestGigaChatActionJson(
        settings: AssistantAiSettings,
        command: String,
        context: OpenAiAssistantRequestContext,
        imageAttachments: List<AssistantImageAttachment>
    ): String {
        val accessToken = requestGigaChatAccessToken(settings.gigaChatApiKey)
        val uploadedFileIds = imageAttachments
            .take(MAX_INLINE_IMAGES)
            .map { image -> uploadGigaChatImage(accessToken, image) }
        val connection = (URL(GIGACHAT_CHAT_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 45_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }

        val userMessage = JSONObject()
            .put("role", "user")
            .put(
                "content",
                if (uploadedFileIds.isEmpty()) {
                    buildInput(command, context)
                } else {
                    buildVisionInputText(command, context, imageAttachments)
                }
            )
        if (uploadedFileIds.isNotEmpty()) {
            userMessage.put(
                "attachments",
                JSONArray().apply {
                    uploadedFileIds.forEach { fileId -> put(fileId) }
                }
            )
        }

        val body = JSONObject()
            .put("model", settings.gigaChatModel.ifBlank { AssistantAiProvider.GIGACHAT.defaultModel })
            .put("temperature", 0.15)
            .put("max_tokens", 700)
            .put(
                "messages",
                JSONArray()
                    .put(
                        JSONObject()
                            .put("role", "system")
                            .put("content", assistantInstructions())
                    )
                    .put(userMessage)
            )

        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(body.toString())
        }

        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        val responseText = stream.bufferedReader(Charsets.UTF_8).use(BufferedReader::readText)
        connection.disconnect()

        if (responseCode !in 200..299) {
            error("GigaChat API: ${formatGigaChatError(responseCode, responseText)}")
        }

        return sanitizeJsonText(extractChatCompletionOutputText(responseText))
    }

    private fun uploadGigaChatImage(
        accessToken: String,
        image: AssistantImageAttachment
    ): String {
        val boundary = "----ShiftSalaryPlanner${UUID.randomUUID()}"
        val imageBytes = Base64.decode(image.base64Data, Base64.DEFAULT)
        val connection = (URL(GIGACHAT_FILES_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 45_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }

        DataOutputStream(connection.outputStream).use { output ->
            output.writeUtf8("--$boundary\r\n")
            output.writeUtf8("Content-Disposition: form-data; name=\"purpose\"\r\n\r\n")
            output.writeUtf8("general\r\n")
            output.writeUtf8("--$boundary\r\n")
            output.writeUtf8(
                "Content-Disposition: form-data; name=\"file\"; filename=\"${gigaChatFileName(image)}\"\r\n"
            )
            output.writeUtf8("Content-Type: ${image.mimeType}\r\n\r\n")
            output.write(imageBytes)
            output.writeUtf8("\r\n--$boundary--\r\n")
            output.flush()
        }

        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        val responseText = stream.bufferedReader(Charsets.UTF_8).use(BufferedReader::readText)
        connection.disconnect()

        if (responseCode !in 200..299) {
            error("GigaChat Files: ${formatGigaChatError(responseCode, responseText)}")
        }

        return JSONObject(responseText).optString("id")
            .ifBlank { JSONObject(responseText).optString("file_id") }
            .ifBlank { error("GigaChat Files не вернул id загруженного изображения.") }
    }

    private fun DataOutputStream.writeUtf8(value: String) {
        write(value.toByteArray(Charsets.UTF_8))
    }

    private fun gigaChatFileName(image: AssistantImageAttachment): String {
        val extension = when (image.mimeType.lowercase()) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            else -> "jpg"
        }
        return "assistant_${UUID.randomUUID()}.$extension"
    }

    private fun requestGigaChatAccessToken(authorizationKey: String): String {
        val connection = (URL(GIGACHAT_TOKEN_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("Authorization", "Basic ${authorizationKey.trim()}")
            setRequestProperty("RqUID", UUID.randomUUID().toString())
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            setRequestProperty("Accept", "application/json")
        }

        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
            writer.write("scope=$GIGACHAT_SCOPE")
        }

        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        val responseText = stream.bufferedReader(Charsets.UTF_8).use(BufferedReader::readText)
        connection.disconnect()

        if (responseCode !in 200..299) {
            error(formatGigaChatError(responseCode, responseText))
        }

        return JSONObject(responseText).optString("access_token")
            .ifBlank { error("GigaChat не вернул access_token.") }
    }

    private fun requestSaluteSpeechAccessToken(
        authorizationKey: String,
        scope: String
    ): String {
        val connection = (URL(GIGACHAT_TOKEN_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("Authorization", "Basic ${authorizationKey.trim()}")
            setRequestProperty("RqUID", UUID.randomUUID().toString())
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            setRequestProperty("Accept", "application/json")
        }

        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
            writer.write("scope=${URLEncoder.encode(scope, "UTF-8")}")
        }

        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        val responseText = stream.bufferedReader(Charsets.UTF_8).use(BufferedReader::readText)
        connection.disconnect()

        if (responseCode !in 200..299) {
            error(formatGigaChatError(responseCode, responseText))
        }

        return JSONObject(responseText).optString("access_token")
            .ifBlank { error("SaluteSpeech не вернул access_token.") }
    }

    private fun formatOpenAiError(responseCode: Int, responseText: String): String {
        val error = runCatching { JSONObject(responseText).optJSONObject("error") }.getOrNull()
        val message = error?.optString("message").orEmpty()
        val code = error?.optString("code").orEmpty()
        val type = error?.optString("type").orEmpty()
        val lower = "$message $code $type".lowercase()
        return when {
            responseCode == 401 || lower.contains("invalid_api_key") -> {
                "ключ API не принят. Проверь, что вставлен именно OpenAI API key, а не токен ChatGPT."
            }
            responseCode == 403 || lower.contains("billing") || lower.contains("quota") || lower.contains("insufficient_quota") -> {
                "нет доступа к API-биллингу или закончилась квота. ChatGPT Plus не включает API-расходы автоматически, для API нужен отдельный billing в OpenAI Platform."
            }
            responseCode == 404 || lower.contains("model") && lower.contains("not") -> {
                "модель недоступна для этого ключа. Попробуй сбросить модель на ${AssistantAiSettings.DEFAULT_OPENAI_MODEL}."
            }
            responseCode == 429 -> {
                "слишком много запросов или лимит проекта. Подожди немного либо проверь лимиты проекта."
            }
            lower.contains("json_object") || lower.contains("json") -> {
                message.ifBlank { "модель не вернула корректный JSON. Попробуй повторить команду." }
            }
            else -> message.ifBlank { "HTTP $responseCode" }
        }
    }

    private fun formatGeminiError(responseCode: Int, responseText: String): String {
        val error = runCatching { JSONObject(responseText).optJSONObject("error") }.getOrNull()
        val message = error?.optString("message").orEmpty()
        val lower = message.lowercase()
        return when {
            responseCode == 400 && lower.contains("api key") -> "ключ Gemini не принят. Проверь ключ из Google AI Studio."
            responseCode == 403 -> "нет доступа к Gemini API для этого ключа или проекта."
            responseCode == 404 || lower.contains("model") -> "модель Gemini недоступна. Попробуй ${AssistantAiProvider.GEMINI.defaultModel}."
            responseCode == 429 -> "лимит Gemini API исчерпан, попробуй позже."
            else -> message.ifBlank { "HTTP $responseCode" }
        }
    }

    private fun formatGigaChatError(responseCode: Int, responseText: String): String {
        val json = runCatching { JSONObject(responseText) }.getOrNull()
        val message = json?.optString("message").orEmpty()
            .ifBlank { json?.optString("error_description").orEmpty() }
            .ifBlank { json?.optString("error").orEmpty() }
        return when (responseCode) {
            401 -> "ключ авторизации не принят или OAuth-токен недействителен. Проверь ключ GigaChat API."
            403 -> "нет доступа к GigaChat API со scope $GIGACHAT_SCOPE. Проверь тариф/Freemium и тип ключа."
            404 -> "endpoint или модель GigaChat недоступны. Попробуй модель ${AssistantAiProvider.GIGACHAT.defaultModel}."
            429 -> "лимит GigaChat API исчерпан, попробуй позже."
            else -> message.ifBlank { "HTTP $responseCode" }
        }
    }

    private fun formatTransportError(provider: AssistantAiProvider, throwable: Throwable): String? {
        if (provider != AssistantAiProvider.GIGACHAT || !throwable.isCertificateTrustError()) return null
        return "Android не доверяет TLS-сертификату GigaChat. В приложение добавлены сертификаты Минцифры для доменов GigaChat, но если ошибка осталась, проверь дату/время устройства и обнови сборку."
    }

    private fun Throwable.isCertificateTrustError(): Boolean {
        return generateSequence(this as Throwable?) { it.cause }
            .any { error ->
                error is SSLHandshakeException ||
                    error is CertPathValidatorException ||
                    error.message?.contains("Trust anchor", ignoreCase = true) == true ||
                    error.message?.contains("certificate", ignoreCase = true) == true &&
                    error.message?.contains("trust", ignoreCase = true) == true
            }
    }

    private fun buildInput(command: String, context: OpenAiAssistantRequestContext): String {
        val contextJson = JSONObject()
            .put("today", LocalDate.now().toString())
            .put("activeWorkplaceId", context.activeWorkplaceId)
            .put("activeWorkplaceName", context.activeWorkplaceName)
            .put("todaySummary", context.todaySummary)
            .put("tomorrowSummary", context.tomorrowSummary)
            .put("nextAlarmSummary", context.nextAlarmSummary)
            .put("assistantMemory", context.assistantMemory.take(900))
            .put("recentMessages", JSONArray(context.recentMessages.takeLast(10)))
            .put(
                "finance",
                if (context.includeFinancialContext) {
                    JSONObject()
                        .put("periodLabel", context.financeContext.periodLabel)
                        .put("grossTotal", context.financeContext.grossTotal)
                        .put("netTotal", context.financeContext.netTotal)
                        .put("ndfl", context.financeContext.ndfl)
                        .put("netAdvance", context.financeContext.netAdvance)
                        .put("netSalary", context.financeContext.netSalary)
                        .put("actualAdvance", context.financeContext.actualAdvance)
                        .put("actualSalary", context.financeContext.actualSalary)
                        .put("paymentDifferenceToleranceRub", context.financeContext.paymentDifferenceToleranceRub)
                } else {
                    JSONObject()
                }
            )
            .put(
                "shifts",
                JSONArray(context.shifts.take(80).map { shift ->
                    JSONObject()
                        .put("code", shift.code)
                        .put("displayCode", shift.displayCode)
                        .put("title", shift.title)
                        .put("workplaceId", shift.workplaceId)
                        .put("workplaceName", shift.workplaceName)
                        .put("totalHours", shift.totalHours)
                        .put("breakHours", shift.breakHours)
                        .put("nightHours", shift.nightHours)
                        .put("start", formatNullableClock(shift.startHour, shift.startMinute))
                        .put("end", formatNullableClock(shift.endHour, shift.endMinute))
                })
            )
            .put(
                "scheduledShifts",
                JSONArray(context.scheduledShifts
                    .filter { !it.date.isBefore(LocalDate.now().minusDays(7)) }
                    .take(120)
                    .map { scheduled ->
                        JSONObject()
                            .put("date", scheduled.date.toString())
                            .put("shiftCode", scheduled.shift.code)
                            .put("displayCode", scheduled.shift.displayCode)
                            .put("workplaceId", scheduled.shift.workplaceId)
                            .put("workplaceName", scheduled.shift.workplaceName)
                    })
            )
            .put(
                "upcomingPayments",
                if (context.includeFinancialContext) {
                    JSONArray(context.upcomingPayments.take(8).map { payment ->
                        JSONObject()
                            .put("title", payment.title)
                            .put("date", payment.date.toString())
                    })
                } else {
                    JSONArray()
                }
            )
            .put("financialContextIncluded", context.includeFinancialContext)

        return """
            Верни ответ строго как JSON object.

            Контекст приложения:
            $contextJson

            Команда пользователя:
            $command
        """.trimIndent()
    }

    private fun buildOpenAiInput(
        command: String,
        context: OpenAiAssistantRequestContext,
        imageAttachments: List<AssistantImageAttachment>
    ): Any {
        if (imageAttachments.isEmpty()) return buildInput(command, context)
        val content = JSONArray()
            .put(
                JSONObject()
                    .put("type", "input_text")
                    .put("text", buildVisionInputText(command, context, imageAttachments))
            )
        imageAttachments.take(MAX_INLINE_IMAGES).forEach { image ->
            content.put(
                JSONObject()
                    .put("type", "input_image")
                    .put("image_url", "data:${image.mimeType};base64,${image.base64Data}")
            )
        }
        return JSONArray().put(
            JSONObject()
                .put("role", "user")
                .put("content", content)
        )
    }

    private fun buildGeminiParts(
        command: String,
        context: OpenAiAssistantRequestContext,
        imageAttachments: List<AssistantImageAttachment>
    ): JSONArray {
        val parts = JSONArray()
            .put(JSONObject().put("text", buildVisionInputText(command, context, imageAttachments)))
        imageAttachments.take(MAX_INLINE_IMAGES).forEach { image ->
            parts.put(
                JSONObject().put(
                    "inlineData",
                    JSONObject()
                        .put("mimeType", image.mimeType)
                        .put("data", image.base64Data)
                )
            )
        }
        return parts
    }

    private fun buildVisionInputText(
        command: String,
        context: OpenAiAssistantRequestContext,
        imageAttachments: List<AssistantImageAttachment>
    ): String {
        val imageSummary = imageAttachments.take(MAX_INLINE_IMAGES).joinToString("; ") { image ->
            "${image.displayName} (${image.mimeType}, ${image.byteSize / 1024} KB)"
        }
        return buildInput(command, context) + """

            Прикреплены изображения: $imageSummary.
            Если на изображении график/таблица/скриншот календаря, сначала распознай даты и коды смен, затем верни подходящий JSON action.
        """.trimIndent()
    }

    private fun assistantInstructions(): String {
        return """
            Ты ИИ-парсер для Android-приложения планировщика смен и зарплаты.
            Верни только валидный JSON без Markdown.
            Не выдумывай коды смен: используй code из контекста.
            Если пользователь не уточнил работу, предпочитай activeWorkplaceId.
            Поддерживаемые action:
            - assign_shift: date, shiftCode, message
            - assign_shift_range: rangeStart, rangeEnd, shiftCode или shiftCodes[], stepDays, dayFilter (all/weekdays/weekends), message
            - assign_shift_dates: assignments массив объектов {date, shiftCode}, message. Используй для импортированного графика списком дат.
            - create_note: date, noteTitle, noteBody, message
            - configure_alarm: shiftCode, hour, minute, minutesBefore, message
            - configure_alarms: shiftCodes[], hour, minute, minutesBefore, message. Используй для команды "создай будильники для всех смен/шаблонов".
            - clear_day: date, message
            - open_tab: tab (CALENDAR,TODAY,NOTES,FINANCE,ALARMS,SHIFTS,SETTINGS), message
            - answer: answerText, message
            - update_memory: memoryText, clearMemory, message
            - needs_clarification: question, message
            - unknown: message
            Для вопросов "проверь график", "сводка недели/месяца", "есть ли пустые дни/пересечения" верни answer с кратким анализом scheduledShifts.
            Для вопросов про план/факт выплат, расхождения, аванс, НДФЛ и объяснение расчёта используй finance и верни answer.
            Для вставленного текста графика или OCR со скриншота верни assign_shift_dates, если видишь пары дата + код смены.
            Если пользователь просит "запомни", "помни", "сохрани правило", верни update_memory.
            Если не хватает даты, кода смены, текста заметки или времени будильника, предпочитай needs_clarification.
            Формат дат: YYYY-MM-DD. Если действие рискованное или неоднозначное, верни unknown с вопросом.
        """.trimIndent()
    }

    private fun extractOpenAiOutputText(responseText: String): String {
        val root = JSONObject(responseText)
        root.optString("output_text").takeIf { it.isNotBlank() }?.let { return it }

        val output = root.optJSONArray("output") ?: return ""
        for (i in 0 until output.length()) {
            val item = output.optJSONObject(i) ?: continue
            val content = item.optJSONArray("content") ?: continue
            for (j in 0 until content.length()) {
                val block = content.optJSONObject(j) ?: continue
                block.optString("text").takeIf { it.isNotBlank() }?.let { return it }
            }
        }
        return ""
    }

    private fun extractGeminiOutputText(responseText: String): String {
        val candidates = JSONObject(responseText).optJSONArray("candidates") ?: return ""
        for (i in 0 until candidates.length()) {
            val content = candidates.optJSONObject(i)
                ?.optJSONObject("content")
                ?.optJSONArray("parts") ?: continue
            for (j in 0 until content.length()) {
                content.optJSONObject(j)
                    ?.optString("text")
                    ?.takeIf { it.isNotBlank() }
                    ?.let { return it }
            }
        }
        return ""
    }

    private fun extractGeminiAudioBase64(responseText: String): String {
        val candidates = JSONObject(responseText).optJSONArray("candidates") ?: return ""
        for (i in 0 until candidates.length()) {
            val parts = candidates.optJSONObject(i)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?: continue
            for (j in 0 until parts.length()) {
                parts.optJSONObject(j)
                    ?.optJSONObject("inlineData")
                    ?.optString("data")
                    ?.takeIf { it.isNotBlank() }
                    ?.let { return it }
                parts.optJSONObject(j)
                    ?.optJSONObject("inline_data")
                    ?.optString("data")
                    ?.takeIf { it.isNotBlank() }
                    ?.let { return it }
            }
        }
        return ""
    }

    private fun extractChatCompletionOutputText(responseText: String): String {
        val choices = JSONObject(responseText).optJSONArray("choices") ?: return ""
        for (i in 0 until choices.length()) {
            choices.optJSONObject(i)
                ?.optJSONObject("message")
                ?.optString("content")
                ?.takeIf { it.isNotBlank() }
                ?.let { return it }
        }
        return ""
    }

    private fun sanitizeJsonText(text: String): String {
        return text
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }

    private fun formatNullableClock(hour: Int?, minute: Int?): String? {
        if (hour == null || minute == null) return null
        return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
    }

    private fun pcm16ToWav(
        pcmBytes: ByteArray,
        sampleRate: Int,
        channels: Int
    ): ByteArray {
        val byteRate = sampleRate * channels * 2
        val totalDataLen = pcmBytes.size + 36
        return ByteArrayOutputStream(pcmBytes.size + 44).use { output ->
            output.write("RIFF".toByteArray(Charsets.US_ASCII))
            output.writeLittleEndianInt(totalDataLen)
            output.write("WAVE".toByteArray(Charsets.US_ASCII))
            output.write("fmt ".toByteArray(Charsets.US_ASCII))
            output.writeLittleEndianInt(16)
            output.writeLittleEndianShort(1)
            output.writeLittleEndianShort(channels)
            output.writeLittleEndianInt(sampleRate)
            output.writeLittleEndianInt(byteRate)
            output.writeLittleEndianShort(channels * 2)
            output.writeLittleEndianShort(16)
            output.write("data".toByteArray(Charsets.US_ASCII))
            output.writeLittleEndianInt(pcmBytes.size)
            output.write(pcmBytes)
            output.toByteArray()
        }
    }

    private fun ByteArrayOutputStream.writeLittleEndianInt(value: Int) {
        write(value and 0xFF)
        write(value shr 8 and 0xFF)
        write(value shr 16 and 0xFF)
        write(value shr 24 and 0xFF)
    }

    private fun ByteArrayOutputStream.writeLittleEndianShort(value: Int) {
        write(value and 0xFF)
        write(value shr 8 and 0xFF)
    }

    private const val MAX_INLINE_IMAGES = 3
}
