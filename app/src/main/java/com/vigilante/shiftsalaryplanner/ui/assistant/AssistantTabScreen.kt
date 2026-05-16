package com.vigilante.shiftsalaryplanner

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.NoteAdd
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.vigilante.shiftsalaryplanner.settings.AssistantAiSettings
import com.vigilante.shiftsalaryplanner.settings.AssistantAiProvider
import com.vigilante.shiftsalaryplanner.settings.AssistantAiModelOption
import com.vigilante.shiftsalaryplanner.settings.AssistantVoiceReplyMode
import com.vigilante.shiftsalaryplanner.settings.modelOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

data class AssistantShiftOption(
    val code: String,
    val displayCode: String,
    val title: String,
    val workplaceId: String,
    val workplaceName: String,
    val totalHours: Double = 0.0,
    val breakHours: Double = 0.0,
    val nightHours: Double = 0.0,
    val startHour: Int? = null,
    val startMinute: Int? = null,
    val endHour: Int? = null,
    val endMinute: Int? = null
)

data class AssistantScheduledShift(
    val date: LocalDate,
    val shift: AssistantShiftOption
)

data class AssistantFinanceContext(
    val periodLabel: String,
    val grossTotal: Double,
    val netTotal: Double,
    val ndfl: Double,
    val netAdvance: Double,
    val netSalary: Double,
    val actualAdvance: Double,
    val actualSalary: Double,
    val paymentDifferenceToleranceRub: Double
)

private data class AssistantDatedShiftAssignment(
    val date: LocalDate,
    val shift: AssistantShiftOption
)

private enum class AssistantActionKind {
    ASSIGN_SHIFT,
    ASSIGN_SHIFT_RANGE,
    ASSIGN_SHIFT_DATES,
    CONFIGURE_SHIFT_ALARM,
    CONFIGURE_SHIFT_ALARMS,
    CLEAR_DAY,
    CREATE_NOTE,
    OPEN_TAB,
    ANSWER,
    UPDATE_MEMORY,
    UNKNOWN
}

private enum class AssistantClarificationType {
    SHIFT,
    DATE,
    NOTE_TEXT,
    GENERIC
}

private enum class AssistantRangeDayFilter {
    ALL,
    WEEKDAYS,
    WEEKENDS
}

private data class AssistantDraftAction(
    val kind: AssistantActionKind,
    val title: String,
    val description: String,
    val date: LocalDate? = null,
    val rangeStart: LocalDate? = null,
    val rangeEnd: LocalDate? = null,
    val rangeStepDays: Int = 1,
    val rangeDayFilter: AssistantRangeDayFilter = AssistantRangeDayFilter.ALL,
    val shift: AssistantShiftOption? = null,
    val shiftSequence: List<AssistantShiftOption> = emptyList(),
    val datedShiftAssignments: List<AssistantDatedShiftAssignment> = emptyList(),
    val alarmHour: Int? = null,
    val alarmMinute: Int? = null,
    val alarmMinutesBefore: Int? = null,
    val noteTitle: String = "",
    val noteBody: String = "",
    val targetTab: BottomTab? = null,
    val answerText: String = "",
    val memoryText: String = "",
    val clearMemory: Boolean = false,
    val clarificationPrompt: String = "",
    val clarificationType: AssistantClarificationType = AssistantClarificationType.GENERIC,
    val canExecute: Boolean = true
)

private fun AssistantDraftAction.requiresConfirmation(settings: AssistantAiSettings): Boolean {
    if (!canExecute || !settings.confirmBeforeActions) return false
    if (kind == AssistantActionKind.CREATE_NOTE && !settings.confirmNoteActions) return false
    return kind in setOf(
        AssistantActionKind.ASSIGN_SHIFT,
        AssistantActionKind.ASSIGN_SHIFT_RANGE,
        AssistantActionKind.ASSIGN_SHIFT_DATES,
        AssistantActionKind.CONFIGURE_SHIFT_ALARM,
        AssistantActionKind.CONFIGURE_SHIFT_ALARMS,
        AssistantActionKind.CLEAR_DAY,
        AssistantActionKind.CREATE_NOTE,
        AssistantActionKind.UPDATE_MEMORY
    )
}

private data class AssistantChatMessage(
    val text: String,
    val fromUser: Boolean
)

private val AssistantChatColorPalette = listOf(
    "#F4F8F7",
    "#E5F4EF",
    "#D9ECFF",
    "#FFF4C7",
    "#FFE2D5",
    "#F1DDFC",
    "#EAF1E2",
    "#ECEFF5",
    "#17201E",
    "#1E2A33",
    "#2B2440",
    "#33221E"
)

private val AssistantOpenAiTtsModels = listOf(
    "gpt-4o-mini-tts" to "Живой",
    "tts-1" to "Быстрый",
    "tts-1-hd" to "Качественный"
)

private val AssistantOpenAiTtsVoices = listOf(
    "coral",
    "alloy",
    "ash",
    "ballad",
    "echo",
    "fable",
    "nova",
    "onyx",
    "sage",
    "shimmer"
)

private val AssistantGeminiTtsModels = listOf(
    "gemini-2.5-flash-preview-tts" to "Flash",
    "gemini-2.5-pro-preview-tts" to "Pro",
    "gemini-3.1-flash-tts-preview" to "3.1 Flash"
)

private val AssistantGeminiTtsVoices = listOf(
    "Kore",
    "Puck",
    "Charon",
    "Fenrir",
    "Aoede",
    "Leda",
    "Orus",
    "Zephyr"
)

private val AssistantSaluteSpeechScopes = listOf(
    "SALUTE_SPEECH_PERS",
    "SALUTE_SPEECH_CORP"
)

private val AssistantSaluteSpeechVoices = listOf(
    "Nec_24000",
    "Bys_24000",
    "May_24000",
    "Tur_24000",
    "Ost_24000",
    "Pon_24000"
)

private data class AssistantActionHistoryItem(
    val command: String,
    val title: String,
    val description: String,
    val result: String,
    val canUndo: Boolean = false,
    val undoHint: String = "Откат недоступен для этого действия"
)

private data class AssistantPendingClarification(
    val originalText: String,
    val prompt: String,
    val type: AssistantClarificationType
)

@Composable
fun AssistantTabScreen(
    activeWorkplaceName: String,
    activeWorkplaceId: String,
    shiftOptions: List<AssistantShiftOption>,
    scheduledShifts: List<AssistantScheduledShift>,
    todaySummary: String,
    tomorrowSummary: String,
    nextAlarmSummary: String,
    upcomingPayments: List<UpcomingPaymentItem>,
    financeContext: AssistantFinanceContext,
    aiSettings: AssistantAiSettings,
    onAiSettingsChange: (AssistantAiSettings) -> Unit,
    onAssignShift: (LocalDate, AssistantShiftOption) -> Unit,
    onConfigureShiftAlarm: (AssistantShiftOption, Int?, Int?, Int?) -> Unit,
    onClearDay: (LocalDate) -> Unit,
    onCreateNote: (LocalDate, String, String) -> Unit,
    onOpenTab: (BottomTab) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var commandText by rememberSaveable { mutableStateOf("") }
    var draftAction by remember { mutableStateOf<AssistantDraftAction?>(null) }
    var isThinking by remember { mutableStateOf(false) }
    var apiTestStatus by rememberSaveable { mutableStateOf("") }
    var showAssistantSettings by rememberSaveable { mutableStateOf(false) }
    var pendingClarification by remember { mutableStateOf<AssistantPendingClarification?>(null) }
    var submitRecognizedSpeech by remember { mutableStateOf(false) }
    var recognizedSpeechToSubmit by remember { mutableStateOf<String?>(null) }
    var forceSpeakNextAssistantText by remember { mutableStateOf(false) }
    var voiceInputVisible by rememberSaveable { mutableStateOf(false) }
    var voiceInputAutoSubmit by rememberSaveable { mutableStateOf(false) }
    var voiceInputPartialText by rememberSaveable { mutableStateOf("") }
    var liveConversationActive by rememberSaveable { mutableStateOf(false) }
    var liveConversationWaitingForReply by rememberSaveable { mutableStateOf(false) }
    val chatMessages = remember { mutableStateListOf<AssistantChatMessage>() }
    val actionHistory = remember { mutableStateListOf<AssistantActionHistoryItem>() }
    val attachedImages = remember { mutableStateListOf<AssistantImageAttachment>() }
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            runCatching {
                readAssistantImageAttachment(
                    context = context,
                    uri = uri,
                    fallbackName = "Изображение ${attachedImages.size + 1}"
                )
            }.onSuccess { attachment ->
                attachedImages += attachment
                if (commandText.isBlank()) {
                    commandText = "Разбери изображение и предложи действие"
                }
                draftAction = null
            }.onFailure { error ->
                onShowMessage(error.message.orEmpty().ifBlank { "Не удалось прикрепить изображение" })
            }
        }
    }
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                .orEmpty()
            if (spoken.isNotBlank()) {
                commandText = spoken
                draftAction = null
                if (submitRecognizedSpeech) {
                    liveConversationActive = true
                    liveConversationWaitingForReply = true
                    forceSpeakNextAssistantText = true
                    recognizedSpeechToSubmit = spoken
                }
            }
        }
        submitRecognizedSpeech = false
    }
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                voiceInputPartialText = ""
                voiceInputVisible = true
            } else {
                submitRecognizedSpeech = voiceInputAutoSubmit
                try {
                    speechLauncher.launch(buildAssistantSpeechIntent(voiceInputAutoSubmit))
                } catch (_: ActivityNotFoundException) {
                    submitRecognizedSpeech = false
                    onShowMessage("На устройстве нет голосового ввода")
                }
            }
        } else {
            voiceInputAutoSubmit = false
            onShowMessage("Разреши доступ к микрофону для голосового ассистента")
        }
    }
    var ttsReady by remember { mutableStateOf(false) }
    var lastSpokenAssistantText by remember { mutableStateOf("") }
    var ttsEngines by remember { mutableStateOf<List<TextToSpeech.EngineInfo>>(emptyList()) }
    var neuralMediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isAssistantSpeaking by remember { mutableStateOf(false) }
    var speechStopToken by remember { mutableStateOf(0) }
    val selectedSystemTtsEngine = aiSettings.systemTtsEnginePackage.ifBlank { null }
    val textToSpeech = remember(context, selectedSystemTtsEngine) {
        ttsReady = false
        val listener = TextToSpeech.OnInitListener { status ->
            ttsReady = status == TextToSpeech.SUCCESS
        }
        if (selectedSystemTtsEngine == null) {
            TextToSpeech(context.applicationContext, listener)
        } else {
            TextToSpeech(context.applicationContext, listener, selectedSystemTtsEngine)
        }
    }
    fun finishAssistantSpeechPlayback(restartLive: Boolean = true) {
        isAssistantSpeaking = false
        liveConversationWaitingForReply = false
        if (restartLive && liveConversationActive && !voiceInputVisible) {
            voiceInputAutoSubmit = true
            voiceInputPartialText = ""
            voiceInputVisible = true
        }
    }

    fun stopAssistantSpeech(restartLive: Boolean = true) {
        speechStopToken += 1
        textToSpeech.stop()
        runCatching { neuralMediaPlayer?.stop() }
        neuralMediaPlayer?.release()
        neuralMediaPlayer = null
        finishAssistantSpeechPlayback(restartLive = restartLive)
    }

    fun endLiveConversation() {
        liveConversationActive = false
        liveConversationWaitingForReply = false
        voiceInputVisible = false
        voiceInputPartialText = ""
        voiceInputAutoSubmit = false
        stopAssistantSpeech(restartLive = false)
    }

    DisposableEffect(textToSpeech) {
        textToSpeech.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    coroutineScope.launch {
                        isAssistantSpeaking = true
                        liveConversationWaitingForReply = false
                    }
                }

                override fun onDone(utteranceId: String?) {
                    coroutineScope.launch { finishAssistantSpeechPlayback() }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    coroutineScope.launch { finishAssistantSpeechPlayback() }
                }
            }
        )
        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            neuralMediaPlayer?.release()
            neuralMediaPlayer = null
        }
    }
    LaunchedEffect(ttsReady, aiSettings.voiceRepliesEnabled, aiSettings.voiceReplyMode) {
        if (ttsReady) {
            val russian = Locale.forLanguageTag("ru-RU")
            if (textToSpeech.setLanguage(russian) < TextToSpeech.LANG_AVAILABLE) {
                textToSpeech.setLanguage(Locale.getDefault())
            }
            textToSpeech.setSpeechRate(1.0f)
            ttsEngines = textToSpeech.engines.orEmpty()
        }
        if (!aiSettings.voiceRepliesEnabled || aiSettings.voiceReplyMode != AssistantVoiceReplyMode.SYSTEM) {
            textToSpeech.stop()
        }
        if (!aiSettings.voiceRepliesEnabled) {
            runCatching { neuralMediaPlayer?.stop() }
        }
    }
    LaunchedEffect(chatMessages.size, aiSettings.voiceRepliesEnabled, aiSettings.voiceReplyMode, ttsReady) {
        val shouldSpeak = aiSettings.voiceRepliesEnabled || forceSpeakNextAssistantText
        if (!shouldSpeak) return@LaunchedEffect
        val message = chatMessages.lastOrNull { !it.fromUser } ?: return@LaunchedEffect
        val text = message.text.trim()
        val forcedVoiceReply = forceSpeakNextAssistantText
        if (text.isNotBlank() && (text != lastSpokenAssistantText || forcedVoiceReply)) {
            lastSpokenAssistantText = text
            forceSpeakNextAssistantText = false
            if (aiSettings.voiceReplyMode != AssistantVoiceReplyMode.SYSTEM) {
                val readinessError = aiSettings.neuralVoiceReadinessError()
                if (readinessError != null) {
                    onShowMessage(readinessError)
                    finishAssistantSpeechPlayback()
                    return@LaunchedEffect
                }
                textToSpeech.stop()
                val currentSpeechToken = speechStopToken
                isAssistantSpeaking = true
                liveConversationWaitingForReply = false
                runCatching {
                    playNeuralSpeech(
                        context = context,
                        settings = aiSettings,
                        text = text.take(900),
                        currentPlayer = neuralMediaPlayer,
                        shouldStartPlayback = { speechStopToken == currentSpeechToken },
                        onPlayerReady = { player -> neuralMediaPlayer = player },
                        onPlaybackFinished = {
                            coroutineScope.launch { finishAssistantSpeechPlayback() }
                        }
                    ).also { started ->
                        if (!started) {
                            finishAssistantSpeechPlayback(restartLive = false)
                        }
                    }
                }.onFailure { error ->
                    finishAssistantSpeechPlayback()
                    if (error !is CancellationException) {
                        onShowMessage(aiSettings.voiceReplyMode.errorPrefix(error.message))
                    }
                }
            } else if (ttsReady) {
                isAssistantSpeaking = true
                liveConversationWaitingForReply = false
                textToSpeech.speak(
                    text.take(900),
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "assistant_reply_${System.currentTimeMillis()}"
                )
            } else {
                finishAssistantSpeechPlayback()
            }
        }
    }

    fun speakTestPhrase() {
        val sample = "Голос ассистента настроен. Я могу читать ответы вслух."
        coroutineScope.launch {
            if (aiSettings.voiceReplyMode != AssistantVoiceReplyMode.SYSTEM) {
                val readinessError = aiSettings.neuralVoiceReadinessError()
                if (readinessError != null) {
                    onShowMessage(readinessError)
                    return@launch
                }
                textToSpeech.stop()
                val currentSpeechToken = speechStopToken
                isAssistantSpeaking = true
                runCatching {
                    playNeuralSpeech(
                        context = context,
                        settings = aiSettings,
                        text = sample,
                        currentPlayer = neuralMediaPlayer,
                        shouldStartPlayback = { speechStopToken == currentSpeechToken },
                        onPlayerReady = { player -> neuralMediaPlayer = player },
                        onPlaybackFinished = {
                            coroutineScope.launch { finishAssistantSpeechPlayback(restartLive = false) }
                        }
                    ).also { started ->
                        if (!started) {
                            finishAssistantSpeechPlayback(restartLive = false)
                        }
                    }
                }.onFailure { error ->
                    finishAssistantSpeechPlayback(restartLive = false)
                    if (error !is CancellationException) {
                        onShowMessage(aiSettings.voiceReplyMode.errorPrefix(error.message))
                    }
                }
            } else if (ttsReady) {
                isAssistantSpeaking = true
                textToSpeech.speak(sample, TextToSpeech.QUEUE_FLUSH, null, "assistant_test_voice")
            } else {
                onShowMessage("Системный TTS ещё не готов")
            }
        }
    }

    fun addActionHistory(
        draft: AssistantDraftAction,
        command: String,
        result: String,
        canUndo: Boolean = false,
        undoHint: String = "Откат недоступен для этого действия"
    ) {
        actionHistory.add(
            0,
            AssistantActionHistoryItem(
                command = command,
                title = draft.title,
                description = draft.description,
                result = result,
                canUndo = canUndo,
                undoHint = undoHint
            )
        )
        while (actionHistory.size > 12) {
            actionHistory.removeAt(actionHistory.lastIndex)
        }
    }

    fun executeDraftAction(draft: AssistantDraftAction) {
        val sourceCommand = commandText.ifBlank { draft.description }
        when (draft.kind) {
            AssistantActionKind.ASSIGN_SHIFT -> {
                val date = draft.date
                val shift = draft.shift
                if (date != null && shift != null) {
                    onAssignShift(date, shift)
                    val result = "${shift.displayCode} назначена на ${formatAssistantDate(date)}."
                    chatMessages += AssistantChatMessage(
                        text = "Готово: $result",
                        fromUser = false
                    )
                    addActionHistory(draft, sourceCommand, result)
                    commandText = ""
                    draftAction = null
                }
            }
            AssistantActionKind.ASSIGN_SHIFT_RANGE -> {
                val start = draft.rangeStart
                val end = draft.rangeEnd
                val sequence = draft.shiftSequence.ifEmpty { draft.shift?.let(::listOf).orEmpty() }
                if (start != null && end != null && sequence.isNotEmpty()) {
                    var date: LocalDate = start
                    var count = 0
                    val stepDays = draft.rangeStepDays.coerceAtLeast(1).toLong()
                    while (!date.isAfter(end)) {
                        if (draft.rangeDayFilter.matches(date)) {
                            onAssignShift(date, sequence[count % sequence.size])
                            count += 1
                        }
                        date = date.plusDays(stepDays)
                    }
                    val result = "${sequence.joinToString(" / ") { it.displayCode }} назначены на $count дн. (${formatAssistantDate(start)} - ${formatAssistantDate(end)})."
                    chatMessages += AssistantChatMessage(
                        text = "Готово: $result",
                        fromUser = false
                    )
                    addActionHistory(draft, sourceCommand, result)
                    commandText = ""
                    draftAction = null
                }
            }
            AssistantActionKind.ASSIGN_SHIFT_DATES -> {
                val assignments = draft.datedShiftAssignments
                if (assignments.isNotEmpty()) {
                    assignments.forEach { assignment ->
                        onAssignShift(assignment.date, assignment.shift)
                    }
                    val preview = assignments
                        .take(4)
                        .joinToString("; ") { "${formatAssistantDate(it.date)}: ${it.shift.displayCode}" }
                    val result = "Назначено ${assignments.size} смен${if (preview.isNotBlank()) " ($preview)" else ""}."
                    chatMessages += AssistantChatMessage(
                        text = "Готово: $result",
                        fromUser = false
                    )
                    addActionHistory(draft, sourceCommand, result)
                    commandText = ""
                    draftAction = null
                }
            }
            AssistantActionKind.CONFIGURE_SHIFT_ALARM -> {
                val shift = draft.shift
                if (shift != null) {
                    onConfigureShiftAlarm(
                        shift,
                        draft.alarmHour,
                        draft.alarmMinute,
                        draft.alarmMinutesBefore
                    )
                    val result = "Добавлен будильник для ${shift.displayCode}."
                    chatMessages += AssistantChatMessage(
                        text = result,
                        fromUser = false
                    )
                    addActionHistory(draft, sourceCommand, result)
                    commandText = ""
                    draftAction = null
                }
            }
            AssistantActionKind.CONFIGURE_SHIFT_ALARMS -> {
                val sequence = draft.shiftSequence
                if (sequence.isNotEmpty()) {
                    sequence.forEach { shift ->
                        onConfigureShiftAlarm(
                            shift,
                            draft.alarmHour,
                            draft.alarmMinute,
                            draft.alarmMinutesBefore
                        )
                    }
                    val result = "Добавлены будильники для ${sequence.size} смен."
                    chatMessages += AssistantChatMessage(
                        text = result,
                        fromUser = false
                    )
                    addActionHistory(draft, sourceCommand, result)
                    commandText = ""
                    draftAction = null
                }
            }
            AssistantActionKind.CLEAR_DAY -> {
                val date = draft.date ?: LocalDate.now()
                onClearDay(date)
                val result = "Очищен день ${formatAssistantDate(date)}."
                chatMessages += AssistantChatMessage(
                    text = result,
                    fromUser = false
                )
                addActionHistory(draft, sourceCommand, result)
                commandText = ""
                draftAction = null
            }
            AssistantActionKind.CREATE_NOTE -> {
                val date = draft.date ?: LocalDate.now()
                onCreateNote(date, draft.noteTitle, draft.noteBody)
                val result = "Создана заметка на ${formatAssistantDate(date)}."
                chatMessages += AssistantChatMessage(
                    text = result,
                    fromUser = false
                )
                addActionHistory(draft, sourceCommand, result)
                commandText = ""
                draftAction = null
            }
            AssistantActionKind.OPEN_TAB -> {
                draft.targetTab?.let(onOpenTab)
                val result = "Открыт раздел ${draft.targetTab?.label.orEmpty()}."
                chatMessages += AssistantChatMessage(
                    text = result,
                    fromUser = false
                )
                addActionHistory(draft, sourceCommand, result)
                commandText = ""
                draftAction = null
            }
            AssistantActionKind.UPDATE_MEMORY -> {
                val previousMemory = aiSettings.assistantMemory
                val updatedMemory = if (draft.clearMemory) {
                    ""
                } else {
                    appendAssistantMemory(previousMemory, draft.memoryText)
                }
                onAiSettingsChange(aiSettings.copy(assistantMemory = updatedMemory))
                val result = if (draft.clearMemory) "Память ассистента очищена." else "Запомнил: ${draft.memoryText}"
                chatMessages += AssistantChatMessage(
                    text = result,
                    fromUser = false
                )
                addActionHistory(
                    draft = draft,
                    command = sourceCommand,
                    result = result,
                    canUndo = true,
                    undoHint = previousMemory
                )
                commandText = ""
                draftAction = null
                pendingClarification = null
            }
            AssistantActionKind.ANSWER -> {
                val answer = draft.answerText.ifBlank { draft.description }
                chatMessages += AssistantChatMessage(
                    text = answer,
                    fromUser = false
                )
                addActionHistory(draft, sourceCommand, answer)
                commandText = ""
                draftAction = null
            }
            AssistantActionKind.UNKNOWN -> Unit
        }
    }

    fun handleParsedDraft(draft: AssistantDraftAction, sourceText: String) {
        when {
            !draft.canExecute || draft.kind == AssistantActionKind.UNKNOWN -> {
                draftAction = draft
                if (draft.clarificationPrompt.isNotBlank()) {
                    pendingClarification = AssistantPendingClarification(
                        originalText = sourceText,
                        prompt = draft.clarificationPrompt,
                        type = draft.clarificationType
                    )
                }
                chatMessages += AssistantChatMessage(
                    text = draft.description,
                    fromUser = false
                )
            }
            draft.requiresConfirmation(aiSettings) -> {
                draftAction = draft
                chatMessages += AssistantChatMessage(
                    text = "Понял так: ${draft.description}. Проверь и подтверди действие.",
                    fromUser = false
                )
            }
            else -> executeDraftAction(draft)
        }
    }

    fun submitCommand(forcedText: String? = null) {
        val userText = forcedText?.trim() ?: commandText.trim()
        if (userText.isBlank() || isThinking) return
        if (forcedText != null) {
            commandText = userText
        }
        chatMessages += AssistantChatMessage(text = userText, fromUser = true)
        if (attachedImages.isNotEmpty() && (!aiSettings.advancedModeEnabled || !aiSettings.hasApiKey)) {
            chatMessages += AssistantChatMessage(
                text = "Изображения можно разобрать только через API. Включи продвинутый режим и выбери GigaChat, Gemini или OpenAI.",
                fromUser = false
            )
            return
        }
        val lowerUserText = userText.lowercase(Locale.ROOT)
        val pendingDraft = draftAction
        if (pendingDraft != null && isAssistantConfirmCommand(lowerUserText)) {
            pendingClarification = null
            executeDraftAction(pendingDraft)
            return
        }
        if (pendingDraft != null && isAssistantCancelCommand(lowerUserText)) {
            draftAction = null
            pendingClarification = null
            commandText = ""
            chatMessages += AssistantChatMessage(
                text = "Отменил действие.",
                fromUser = false
            )
            return
        }
        if (pendingDraft != null && pendingDraft.kind != AssistantActionKind.UNKNOWN) {
            refineAssistantDraft(
                draft = pendingDraft,
                reply = userText,
                shifts = shiftOptions,
                activeWorkplaceId = activeWorkplaceId,
                assistantMemory = aiSettings.assistantMemory
            )?.let { refinedDraft ->
                handleParsedDraft(refinedDraft, userText)
                return
            }
        }
        val clarification = pendingClarification
        val effectiveUserText = if (clarification != null) {
            pendingClarification = null
            draftAction = null
            mergeAssistantClarification(clarification, userText)
        } else {
            userText
        }

        fun runLocalDraft() {
            val draft = buildAssistantDraftAction(
                rawText = effectiveUserText,
                shifts = shiftOptions,
                scheduledShifts = scheduledShifts,
                activeWorkplaceId = activeWorkplaceId,
                todaySummary = todaySummary,
                tomorrowSummary = tomorrowSummary,
                nextAlarmSummary = nextAlarmSummary,
            upcomingPayments = upcomingPayments,
            financeContext = financeContext,
            assistantMemory = aiSettings.assistantMemory
        )
            handleParsedDraft(draft, effectiveUserText)
        }

        if (aiSettings.advancedModeEnabled && aiSettings.hasApiKey) {
            isThinking = true
            coroutineScope.launch {
                val aiDraft = runCatching {
                    val actionJson = AiAssistantClient.requestActionJson(
                        settings = aiSettings,
                        command = effectiveUserText,
                        context = OpenAiAssistantRequestContext(
                            activeWorkplaceId = activeWorkplaceId,
                            activeWorkplaceName = activeWorkplaceName,
                            shifts = shiftOptions,
                            scheduledShifts = scheduledShifts,
                            upcomingPayments = upcomingPayments,
                            financeContext = financeContext,
                            todaySummary = todaySummary,
                            tomorrowSummary = tomorrowSummary,
                            nextAlarmSummary = nextAlarmSummary,
                            includeFinancialContext = aiSettings.sendFinancialContextToAi,
                            assistantMemory = aiSettings.assistantMemory,
                            recentMessages = if (aiSettings.sendChatHistoryToAi) {
                                chatMessages.takeLast(8).map { message ->
                                    "${if (message.fromUser) "Пользователь" else "Ассистент"}: ${message.text}"
                                }
                            } else {
                                emptyList()
                            }
                        ),
                        imageAttachments = attachedImages.toList()
                    )
                    buildAssistantDraftFromOpenAiJson(actionJson, shiftOptions)
                }
                isThinking = false
                aiDraft
                    .onSuccess { draft ->
                        if (draft != null) {
                            handleParsedDraft(draft, effectiveUserText)
                            attachedImages.clear()
                        } else {
                            val message = draft?.description ?: "ИИ вернул пустой ответ, попробую локальный режим."
                            chatMessages += AssistantChatMessage(message, fromUser = false)
                            runLocalDraft()
                        }
                    }
                    .onFailure { error ->
                        val canFallbackLocally = attachedImages.isEmpty()
                        chatMessages += AssistantChatMessage(
                            text = if (canFallbackLocally) {
                                "Продвинутый режим недоступен: ${error.message.orEmpty().ifBlank { "ошибка API" }}. Пробую локальный режим."
                            } else {
                                "Не удалось разобрать изображение через API: ${error.message.orEmpty().ifBlank { "ошибка API" }}. Картинка осталась прикреплённой, можно сменить провайдера или повторить."
                            },
                            fromUser = false
                        )
                        if (canFallbackLocally) {
                            runLocalDraft()
                        }
                    }
            }
        } else {
            runLocalDraft()
        }
    }

    LaunchedEffect(recognizedSpeechToSubmit) {
        val recognized = recognizedSpeechToSubmit ?: return@LaunchedEffect
        recognizedSpeechToSubmit = null
        submitCommand(recognized)
    }

    fun launchAssistantSpeechInput(autoSubmit: Boolean) {
        if (autoSubmit) {
            liveConversationActive = true
        } else {
            liveConversationActive = false
            liveConversationWaitingForReply = false
        }
        stopAssistantSpeech(restartLive = false)
        voiceInputAutoSubmit = autoSubmit
        voiceInputPartialText = ""
        val hasAudioPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasAudioPermission) {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            voiceInputVisible = true
        } else {
            submitRecognizedSpeech = autoSubmit
            try {
                speechLauncher.launch(buildAssistantSpeechIntent(autoSubmit))
            } catch (_: ActivityNotFoundException) {
                submitRecognizedSpeech = false
                onShowMessage("На устройстве нет голосового ввода")
            }
        }
    }

    fun buildAiContext(): OpenAiAssistantRequestContext {
        return OpenAiAssistantRequestContext(
            activeWorkplaceId = activeWorkplaceId,
            activeWorkplaceName = activeWorkplaceName,
            shifts = shiftOptions,
            scheduledShifts = scheduledShifts,
            upcomingPayments = upcomingPayments,
            financeContext = financeContext,
            todaySummary = todaySummary,
            tomorrowSummary = tomorrowSummary,
            nextAlarmSummary = nextAlarmSummary,
            includeFinancialContext = aiSettings.sendFinancialContextToAi,
            assistantMemory = aiSettings.assistantMemory,
            recentMessages = if (aiSettings.sendChatHistoryToAi) {
                chatMessages.takeLast(8).map { message ->
                    "${if (message.fromUser) "Пользователь" else "Ассистент"}: ${message.text}"
                }
            } else {
                emptyList()
            }
        )
    }

    fun testAiConnection() {
        if (!aiSettings.advancedModeEnabled) {
            apiTestStatus = "Включи продвинутый режим."
            return
        }
        if (!aiSettings.hasApiKey) {
            apiTestStatus = "Вставь API-ключ для ${aiSettings.provider.shortLabel}."
            return
        }
        isThinking = true
        apiTestStatus = "Проверяю ${aiSettings.provider.shortLabel}..."
        coroutineScope.launch {
            val result = runCatching {
                AiAssistantClient.requestActionJson(
                    settings = aiSettings,
                    command = "Проверка AI API. Верни JSON action answer с коротким answerText: подключение работает.",
                    context = buildAiContext()
                )
            }
            isThinking = false
            apiTestStatus = result.fold(
                onSuccess = { "${aiSettings.provider.shortLabel} API работает." },
                onFailure = { error -> error.message.orEmpty().ifBlank { "Ошибка API" } }
            )
        }
    }

    val assistantScreenBackground = assistantChatBackgroundColor(aiSettings)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(assistantScreenBackground)
            .padding(horizontal = appScreenPadding())
    ) {
        if (showAssistantSettings) {
            AssistantAiSettingsScreen(
                settings = aiSettings,
                onChange = onAiSettingsChange,
                isThinking = isThinking,
                apiTestStatus = apiTestStatus,
                onTestApi = ::testAiConnection,
                historyCount = chatMessages.size,
                onClearHistory = {
                    chatMessages.clear()
                    draftAction = null
                    pendingClarification = null
                },
                ttsEngines = ttsEngines,
                onOpenSystemTtsSettings = {
                    runCatching {
                        context.startActivity(Intent("com.android.settings.TTS_SETTINGS"))
                    }.onFailure {
                        runCatching {
                            context.startActivity(Intent(Settings.ACTION_SETTINGS))
                        }.onFailure {
                            onShowMessage("Не удалось открыть настройки TTS")
                        }
                    }
                },
                onTestVoice = ::speakTestPhrase,
                onBack = { showAssistantSettings = false },
                modifier = Modifier.fillMaxSize()
            )
            return@Box
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = appScreenPadding())
                    .padding(bottom = appBlockSpacing()),
                verticalArrangement = Arrangement.spacedBy(appBlockSpacing())
            ) {
                AssistantHeroCard()

                AssistantSmartSuggestions(
                    todaySummary = todaySummary,
                    tomorrowSummary = tomorrowSummary,
                    nextAlarmSummary = nextAlarmSummary,
                    upcomingPayments = upcomingPayments,
                    onPick = { sample ->
                        commandText = sample
                        draftAction = null
                        pendingClarification = null
                    }
                )

                if (chatMessages.isNotEmpty()) {
                    AssistantChatHistory(
                        messages = chatMessages,
                        settings = aiSettings,
                        onClear = {
                            chatMessages.clear()
                            draftAction = null
                            pendingClarification = null
                            textToSpeech.stop()
                        }
                    )
                }

                if (actionHistory.isNotEmpty()) {
                    AssistantActionHistoryCard(
                        items = actionHistory,
                        onRepeat = { item ->
                            commandText = item.command
                            draftAction = null
                            pendingClarification = null
                        },
                        onUndo = { item ->
                            if (item.canUndo) {
                                onAiSettingsChange(aiSettings.copy(assistantMemory = item.undoHint))
                                actionHistory.remove(item)
                                chatMessages += AssistantChatMessage("Откатил изменение памяти ассистента.", fromUser = false)
                            } else {
                                onShowMessage(item.undoHint)
                            }
                        }
                    )
                }

                draftAction?.let {
                    AssistantInlineFeedback(
                        draft = it,
                        onConfirm = if (it.requiresConfirmation(aiSettings)) {
                            {
                                executeDraftAction(it)
                            }
                        } else {
                            null
                        },
                        onDismiss = {
                            draftAction = null
                            pendingClarification = null
                        }
                    )
                }

                AssistantScheduleScanCard(
                    enabled = !isThinking,
                    providerLabel = aiSettings.provider.shortLabel,
                    isReady = aiSettings.advancedModeEnabled && aiSettings.hasApiKey,
                    onAttach = {
                        if (attachedImages.size >= 3) {
                            onShowMessage("Можно прикрепить до 3 изображений за раз")
                        } else {
                            commandText = "Разбери скриншот графика: распознай даты и коды смен, затем предложи импорт"
                            draftAction = null
                            pendingClarification = null
                            imagePicker.launch("image/*")
                        }
                    }
                )

                AssistantContextCard(
                    activeWorkplaceName = activeWorkplaceName,
                    todaySummary = todaySummary,
                    tomorrowSummary = tomorrowSummary,
                    nextAlarmSummary = nextAlarmSummary,
                    upcomingPayments = upcomingPayments
                )

                AssistantAiStatusCard(
                    settings = aiSettings,
                    apiTestStatus = apiTestStatus,
                    onOpenSettings = { showAssistantSettings = true }
                )

                AssistantQuickCommands(
                    onPick = { sample ->
                        commandText = sample
                        draftAction = null
                        pendingClarification = null
                    }
                )

                AssistantBackendNotice()
            }

            AssistantComposer(
                commandText = commandText,
                isThinking = isThinking,
                isAssistantSpeaking = isAssistantSpeaking,
                liveConversationActive = liveConversationActive,
                liveConversationWaitingForReply = liveConversationWaitingForReply,
                attachedImages = attachedImages,
                onCommandTextChange = {
                    commandText = it
                    draftAction = null
                },
                onSend = { submitCommand() },
                onAttachImage = {
                    if (attachedImages.size >= 3) {
                        onShowMessage("Можно прикрепить до 3 изображений за раз")
                    } else {
                        imagePicker.launch("image/*")
                    }
                },
                onRemoveImage = { attachment ->
                    attachedImages.remove(attachment)
                },
                onVoice = { launchAssistantSpeechInput(autoSubmit = false) },
                onVoiceConversation = { launchAssistantSpeechInput(autoSubmit = true) },
                onStopSpeaking = { stopAssistantSpeech(restartLive = true) },
                onEndLiveConversation = { endLiveConversation() },
                modifier = Modifier
                    .imePadding()
                    .padding(bottom = appScaledSpacing(10.dp))
            )
        }
        AssistantVoiceInputOverlay(
            visible = voiceInputVisible,
            autoSubmit = voiceInputAutoSubmit,
            partialText = voiceInputPartialText,
            onPartialText = { voiceInputPartialText = it },
            onResult = { spoken ->
                voiceInputVisible = false
                voiceInputPartialText = ""
                commandText = spoken
                draftAction = null
                if (voiceInputAutoSubmit) {
                    liveConversationActive = true
                    liveConversationWaitingForReply = true
                    forceSpeakNextAssistantText = true
                    recognizedSpeechToSubmit = spoken
                }
            },
            onDismiss = {
                voiceInputVisible = false
                voiceInputPartialText = ""
                if (voiceInputAutoSubmit) {
                    liveConversationActive = false
                    liveConversationWaitingForReply = false
                }
                voiceInputAutoSubmit = false
            },
            onError = { message ->
                voiceInputVisible = false
                voiceInputPartialText = ""
                if (voiceInputAutoSubmit && liveConversationActive) {
                    voiceInputVisible = true
                } else {
                    voiceInputAutoSubmit = false
                }
                onShowMessage(message)
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = appScaledSpacing(82.dp))
        )
    }
}

private fun buildAssistantSpeechIntent(autoSubmit: Boolean): Intent {
    return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(
            RecognizerIntent.EXTRA_PROMPT,
            if (autoSubmit) "Скажи команду ассистенту" else "Что сделать в планировщике?"
        )
    }
}

@Composable
private fun AssistantVoiceInputOverlay(
    visible: Boolean,
    autoSubmit: Boolean,
    partialText: String,
    onPartialText: (String) -> Unit,
    onResult: (String) -> Unit,
    onDismiss: () -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    val context = LocalContext.current
    DisposableEffect(autoSubmit) {
        var disposed = false
        var deliveredResult = false
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit

            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (text.isNotBlank()) {
                    onPartialText(text)
                }
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                    .trim()
                deliveredResult = true
                if (text.isNotBlank()) {
                    onResult(text)
                } else {
                    onDismiss()
                }
            }

            override fun onError(error: Int) {
                if (disposed || deliveredResult) return
                if (error == SpeechRecognizer.ERROR_CLIENT) {
                    onDismiss()
                    return
                }
                onError(assistantSpeechErrorMessage(error))
            }
        }
        recognizer.setRecognitionListener(listener)
        runCatching {
            recognizer.startListening(buildAssistantSpeechIntent(autoSubmit))
        }.onFailure {
            onError("Не удалось запустить голосовой ввод")
        }
        onDispose {
            disposed = true
            runCatching { recognizer.cancel() }
            runCatching { recognizer.destroy() }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = appScaledSpacing(4.dp)),
        shape = RoundedCornerShape(appCornerRadius(26.dp)),
        color = appPanelColor().copy(alpha = 0.98f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.34f)),
        shadowElevation = appScaledSpacing(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(appScaledSpacing(14.dp)),
            verticalArrangement = Arrangement.spacedBy(appScaledSpacing(10.dp))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(10.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.86f)
                ) {
                    Icon(
                        imageVector = if (autoSubmit) Icons.Rounded.AutoAwesome else Icons.Rounded.Mic,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(appScaledSpacing(9.dp))
                            .size(appScaledSpacing(20.dp)),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (autoSubmit) "Голосовой диалог" else "Диктовка",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (autoSubmit) "Слушаю. После ответа снова включу микрофон." else "Слушаю, текст появится в поле ввода",
                        style = MaterialTheme.typography.bodySmall,
                        color = appListSecondaryTextColor()
                    )
                }
                IconButton(onClick = appHapticAction(onAction = onDismiss)) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Отменить голосовой ввод"
                    )
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(appCornerRadius(18.dp)),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
            ) {
                Text(
                    text = partialText.ifBlank { "Говори, я слушаю..." },
                    modifier = Modifier.padding(appScaledSpacing(12.dp)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (partialText.isBlank()) appListSecondaryTextColor() else MaterialTheme.colorScheme.onSurface,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!autoSubmit && partialText.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    AssistantActionPill(
                        text = "Вставить",
                        accent = true,
                        onClick = { onResult(partialText) }
                    )
                }
            }
        }
    }
}

private fun assistantSpeechErrorMessage(error: Int): String {
    return when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "Не удалось получить звук с микрофона"
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Нет связи для распознавания речи"
        SpeechRecognizer.ERROR_NO_MATCH -> "Не расслышал команду"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Распознавание уже запущено"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Нет разрешения на микрофон"
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "Русский язык не поддержан движком распознавания"
        else -> "Ошибка голосового ввода: $error"
    }
}

@Composable
private fun AssistantHeroCard() {
    AppExpressiveSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = appScaledSpacing(4.dp),
                vertical = appScaledSpacing(4.dp)
            ),
        shape = RoundedCornerShape(appCornerRadius(24.dp)),
        tone = AppExpressiveSurfaceTone.ACCENT,
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Row(
            modifier = Modifier.padding(appScaledSpacing(12.dp)),
            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(12.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(appScaledSpacing(10.dp))
                        .size(appScaledSpacing(24.dp)),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(appScaledSpacing(2.dp))
            ) {
                Text(
                    text = "ИИ-помощник",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Скажи, напиши или приложи скриншот: смена, заметка, будильник, раздел.",
                    style = MaterialTheme.typography.bodySmall,
                    color = appListSecondaryTextColor(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AssistantSmartSuggestions(
    todaySummary: String,
    tomorrowSummary: String,
    nextAlarmSummary: String,
    upcomingPayments: List<UpcomingPaymentItem>,
    onPick: (String) -> Unit
) {
    val payment = upcomingPayments.firstOrNull()
    val suggestions = buildList {
        if (todaySummary.isBlank()) {
            add("Что можно быстро назначить на сегодня?")
        } else {
            add("Что сегодня?")
        }
        if (tomorrowSummary.isBlank()) {
            add("Поставь смену на завтра")
        } else {
            add("Какая смена завтра?")
        }
        if (nextAlarmSummary.isBlank()) {
            add("Создай будильники для всех смен за 90 минут")
        } else {
            add("Когда ближайший будильник?")
        }
        add(payment?.let { "Сколько до выплаты ${it.title.lowercase(Locale.ROOT)}?" } ?: "Когда ближайшая выплата?")
    }.distinct().take(4)

    AssistantCard(title = "Подсказки по текущему контексту") {
        suggestions.chunked(2).forEach { rowSuggestions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))
            ) {
                rowSuggestions.forEach { suggestion ->
                    AssistantCommandChip(
                        text = suggestion,
                        onClick = { onPick(suggestion) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowSuggestions.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AssistantScheduleScanCard(
    enabled: Boolean,
    providerLabel: String,
    isReady: Boolean,
    onAttach: () -> Unit
) {
    AssistantCard(title = "Разбор графика со скрина") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(12.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(appCornerRadius(18.dp)),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Image,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(appScaledSpacing(12.dp))
                        .size(appScaledSpacing(24.dp)),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isReady) "Готов разобрать скриншот" else "Нужен API-провайдер",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isReady) {
                        "Прикрепи изображение, я распознаю даты/коды смен и покажу таблицу перед импортом."
                    } else {
                        "Сейчас выбран $providerLabel. Для картинок включи API GigaChat, Gemini или OpenAI в настройках ИИ."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = appListSecondaryTextColor()
                )
            }
            AssistantActionPill(
                text = "Скрин",
                accent = true,
                onClick = onAttach,
                modifier = Modifier
            )
        }
        if (!enabled) {
            Text(
                text = "Подожди, ассистент ещё обрабатывает предыдущую команду.",
                style = MaterialTheme.typography.bodySmall,
                color = appListSecondaryTextColor()
            )
        }
    }
}

@Composable
private fun AssistantContextCard(
    activeWorkplaceName: String,
    todaySummary: String,
    tomorrowSummary: String,
    nextAlarmSummary: String,
    upcomingPayments: List<UpcomingPaymentItem>
) {
    AssistantCard(title = "Что ассистент уже знает") {
        val payment = upcomingPayments.firstOrNull()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(10.dp))
        ) {
            AssistantContextTile(
                icon = Icons.Rounded.Work,
                title = "Работа",
                value = activeWorkplaceName,
                modifier = Modifier.weight(1f),
                accent = true
            )
            AssistantContextTile(
                icon = Icons.Rounded.Paid,
                title = "Выплата",
                value = payment?.let { "${it.title} · ${formatAssistantDate(it.date)}" } ?: "нет выплат",
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(10.dp))
        ) {
            AssistantContextTile(
                icon = Icons.Rounded.CalendarMonth,
                title = "Сегодня",
                value = todaySummary.ifBlank { "смен нет" },
                modifier = Modifier.weight(1f)
            )
            AssistantContextTile(
                icon = Icons.Rounded.CalendarMonth,
                title = "Завтра",
                value = tomorrowSummary.ifBlank { "смен нет" },
                modifier = Modifier.weight(1f)
            )
        }
        AssistantContextTile(
            icon = Icons.Rounded.Alarm,
            title = "Ближайший будильник",
            value = nextAlarmSummary.ifBlank { "нет будущих срабатываний" },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AssistantContextTile(
    icon: ImageVector,
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false
) {
    AppExpressiveSurface(
        modifier = modifier,
        shape = RoundedCornerShape(appCornerRadius(18.dp)),
        tone = if (accent) AppExpressiveSurfaceTone.ACCENT else AppExpressiveSurfaceTone.GLASS,
        border = BorderStroke(
            1.dp,
            if (accent) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else appPanelBorderColor()
        )
    ) {
        Row(
            modifier = Modifier.padding(appScaledSpacing(12.dp)),
            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(10.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(appScaledSpacing(7.dp))
                        .size(appScaledSpacing(18.dp)),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = appListSecondaryTextColor(),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AssistantChatHistory(
    messages: List<AssistantChatMessage>,
    settings: AssistantAiSettings,
    onClear: () -> Unit
) {
    val userBubbleColor = assistantUserBubbleColor(settings)
    val assistantBubbleColor = assistantReplyBubbleColor(settings)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Диалог",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = appListSecondaryTextColor()
            )
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.44f),
                border = BorderStroke(1.dp, appPanelBorderColor()),
                modifier = Modifier.clickable(onClick = appHapticAction(onAction = onClear))
            ) {
                Text(
                    text = "Очистить",
                    modifier = Modifier.padding(
                        horizontal = appScaledSpacing(10.dp),
                        vertical = appScaledSpacing(5.dp)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = appListSecondaryTextColor()
                )
            }
        }
        messages.takeLast(8).forEach { message ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start
            ) {
                val bubbleColor = if (message.fromUser) userBubbleColor else assistantBubbleColor
                Surface(
                    shape = RoundedCornerShape(appCornerRadius(20.dp)),
                    color = bubbleColor,
                    border = BorderStroke(
                        1.dp,
                        if (message.fromUser) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                        } else {
                            appPanelBorderColor()
                        }
                    ),
                    modifier = Modifier.fillMaxWidth(0.88f)
                ) {
                    Text(
                        text = message.text,
                        modifier = Modifier.padding(
                            horizontal = appScaledSpacing(14.dp),
                            vertical = appScaledSpacing(10.dp)
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = assistantReadableTextColor(bubbleColor)
                    )
                }
            }
        }
    }
}

@Composable
private fun AssistantActionHistoryCard(
    items: List<AssistantActionHistoryItem>,
    onRepeat: (AssistantActionHistoryItem) -> Unit,
    onUndo: (AssistantActionHistoryItem) -> Unit
) {
    AssistantCard(title = "Журнал действий") {
        Text(
            text = "Последние изменения ассистента. Команду можно повторить, а откат доступен только для безопасных действий.",
            style = MaterialTheme.typography.bodySmall,
            color = appListSecondaryTextColor()
        )
        items.take(4).forEach { item ->
            AppExpressiveSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(appCornerRadius(16.dp)),
                tone = AppExpressiveSurfaceTone.GLASS,
                border = BorderStroke(1.dp, appPanelBorderColor())
            ) {
                Column(
                    modifier = Modifier.padding(appScaledSpacing(12.dp)),
                    verticalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.result,
                        style = MaterialTheme.typography.bodySmall,
                        color = appListSecondaryTextColor(),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))
                    ) {
                        AssistantHistoryActionButton(
                            text = "Повторить",
                            icon = Icons.Rounded.Replay,
                            onClick = { onRepeat(item) }
                        )
                        AssistantHistoryActionButton(
                            text = "Откат",
                            icon = Icons.AutoMirrored.Rounded.Undo,
                            enabled = item.canUndo,
                            onClick = { onUndo(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AssistantHistoryActionButton(
    text: String,
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (enabled) {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f)
        },
        border = BorderStroke(1.dp, appPanelBorderColor().copy(alpha = 0.62f)),
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = appScaledSpacing(9.dp),
                vertical = appScaledSpacing(5.dp)
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(4.dp))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = if (enabled) MaterialTheme.colorScheme.primary else appListSecondaryTextColor(),
                modifier = Modifier.size(appScaledSpacing(15.dp))
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) MaterialTheme.colorScheme.primary else appListSecondaryTextColor()
            )
        }
    }
}

@Composable
private fun AssistantComposer(
    commandText: String,
    isThinking: Boolean,
    isAssistantSpeaking: Boolean,
    liveConversationActive: Boolean,
    liveConversationWaitingForReply: Boolean,
    attachedImages: List<AssistantImageAttachment>,
    onCommandTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachImage: () -> Unit,
    onRemoveImage: (AssistantImageAttachment) -> Unit,
    onVoice: () -> Unit,
    onVoiceConversation: () -> Unit,
    onStopSpeaking: () -> Unit,
    onEndLiveConversation: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showTools by rememberSaveable { mutableStateOf(false) }
    val hasCommand = commandText.isNotBlank()

    AppExpressiveSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(appCornerRadius(30.dp)),
        tone = AppExpressiveSurfaceTone.FLOATING,
        border = BorderStroke(1.dp, appPanelBorderColor().copy(alpha = 0.62f)),
        shadowElevation = appScaledSpacing(6.dp)
    ) {
        Column(
            modifier = Modifier.padding(
                start = appScaledSpacing(12.dp),
                top = appScaledSpacing(8.dp),
                end = appScaledSpacing(8.dp),
                bottom = appScaledSpacing(8.dp)
            ),
            verticalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))
        ) {
            if (attachedImages.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))
                ) {
                    attachedImages.forEach { image ->
                        AssistantImageChip(
                            attachment = image,
                            onRemove = { onRemoveImage(image) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(3 - attachedImages.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            if (showTools) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistantComposerToolChip(
                        text = "Скриншот",
                        icon = Icons.Rounded.Image,
                        enabled = !isThinking,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            showTools = false
                            onAttachImage()
                        }
                    )
                    AssistantComposerToolChip(
                        text = "Диктовка",
                        icon = Icons.Rounded.Mic,
                        enabled = !isThinking,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            showTools = false
                            onVoice()
                        }
                    )
                    AssistantComposerToolChip(
                        text = "Диалог",
                        icon = Icons.Rounded.AutoAwesome,
                        enabled = !isThinking,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            showTools = false
                            onVoiceConversation()
                        }
                    )
                }
            }
            if (liveConversationActive) {
                AssistantLiveConversationBar(
                    isSpeaking = isAssistantSpeaking,
                    waitingForReply = liveConversationWaitingForReply,
                    onEnd = onEndLiveConversation
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(
                            min = appScaledSpacing(42.dp),
                            max = appScaledSpacing(138.dp)
                        ),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = commandText,
                        onValueChange = onCommandTextChange,
                        singleLine = false,
                        minLines = 1,
                        maxLines = 5,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = appScaledSpacing(4.dp))
                    )
                    if (commandText.isBlank()) {
                        Text(
                            text = "Напиши команду или приложи скриншот",
                            modifier = Modifier.padding(start = appScaledSpacing(4.dp)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = appListSecondaryTextColor(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (showTools) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (showTools) MaterialTheme.colorScheme.primary.copy(alpha = 0.28f) else appPanelBorderColor()
                    ),
                    modifier = Modifier
                        .size(appScaledSpacing(38.dp))
                        .clickable(
                            enabled = !isThinking,
                            onClick = appHapticAction { showTools = !showTools }
                        )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (showTools) Icons.Rounded.Close else Icons.Rounded.Add,
                            contentDescription = if (showTools) "Скрыть действия" else "Дополнительные действия",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(appScaledSpacing(21.dp))
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (hasCommand) {
                        MaterialTheme.colorScheme.primary
                    } else if (isAssistantSpeaking) {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.92f)
                    } else if (!isThinking) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                    },
                    modifier = Modifier
                        .size(appScaledSpacing(42.dp))
                        .clickable(
                            enabled = isAssistantSpeaking || !isThinking,
                            onClick = appHapticAction {
                                when {
                                    isAssistantSpeaking -> onStopSpeaking()
                                    hasCommand -> onSend()
                                    else -> onVoiceConversation()
                                }
                            }
                        )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isAssistantSpeaking) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Остановить озвучку",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(appScaledSpacing(20.dp))
                            )
                        } else if (hasCommand) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = if (isThinking) "ИИ думает" else "Отправить команду",
                                tint = if (!isThinking) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(appScaledSpacing(20.dp))
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Mic,
                                contentDescription = "Голосовой диалог",
                                tint = if (!isThinking) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(appScaledSpacing(20.dp))
                            )
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = if (!isThinking) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = appScaledSpacing(8.dp), end = appScaledSpacing(8.dp))
                                    .size(appScaledSpacing(10.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssistantLiveConversationBar(
    isSpeaking: Boolean,
    waitingForReply: Boolean,
    onEnd: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = appScaledSpacing(10.dp),
                vertical = appScaledSpacing(7.dp)
            ),
            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(appScaledSpacing(17.dp))
            )
            Text(
                text = when {
                    isSpeaking -> "Live: ассистент отвечает"
                    waitingForReply -> "Live: думаю над командой"
                    else -> "Live: можно говорить"
                },
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                border = BorderStroke(1.dp, appPanelBorderColor()),
                modifier = Modifier.clickable(onClick = appHapticAction(onAction = onEnd))
            ) {
                Text(
                    text = "Завершить",
                    modifier = Modifier.padding(
                        horizontal = appScaledSpacing(9.dp),
                        vertical = appScaledSpacing(4.dp)
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = appListSecondaryTextColor()
                )
            }
        }
    }
}

@Composable
private fun AssistantComposerToolChip(
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(
            enabled = enabled,
            onClick = appHapticAction(onAction = onClick)
        ),
        shape = RoundedCornerShape(999.dp),
        color = if (enabled) {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.68f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f)
        },
        border = BorderStroke(1.dp, appPanelBorderColor().copy(alpha = 0.62f))
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = appScaledSpacing(9.dp),
                vertical = appScaledSpacing(7.dp)
            ),
            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(5.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary else appListSecondaryTextColor(),
                modifier = Modifier.size(appScaledSpacing(16.dp))
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    appListSecondaryTextColor()
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AssistantImageChip(
    attachment: AssistantImageAttachment,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier.padding(
                start = appScaledSpacing(10.dp),
                top = appScaledSpacing(6.dp),
                end = appScaledSpacing(4.dp),
                bottom = appScaledSpacing(6.dp)
            ),
            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Image,
                contentDescription = null,
                modifier = Modifier.size(appScaledSpacing(16.dp)),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${attachment.displayName} · ${attachment.byteSize / 1024} KB",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(
                onClick = appHapticAction(onAction = onRemove),
                modifier = Modifier.size(appScaledSpacing(24.dp))
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Убрать изображение",
                    modifier = Modifier.size(appScaledSpacing(16.dp))
                )
            }
        }
    }
}

@Composable
private fun AssistantInlineFeedback(
    draft: AssistantDraftAction,
    onConfirm: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    val icon = when (draft.kind) {
        AssistantActionKind.ASSIGN_SHIFT -> Icons.Rounded.CalendarMonth
        AssistantActionKind.ASSIGN_SHIFT_RANGE -> Icons.Rounded.CalendarMonth
        AssistantActionKind.ASSIGN_SHIFT_DATES -> Icons.Rounded.CalendarMonth
        AssistantActionKind.CONFIGURE_SHIFT_ALARM -> Icons.Rounded.Alarm
        AssistantActionKind.CONFIGURE_SHIFT_ALARMS -> Icons.Rounded.Alarm
        AssistantActionKind.CLEAR_DAY -> Icons.Rounded.CalendarMonth
        AssistantActionKind.CREATE_NOTE -> Icons.AutoMirrored.Rounded.NoteAdd
        AssistantActionKind.OPEN_TAB -> Icons.Rounded.AutoAwesome
        AssistantActionKind.ANSWER -> Icons.Rounded.AutoAwesome
        AssistantActionKind.UPDATE_MEMORY -> Icons.Rounded.AutoAwesome
        AssistantActionKind.UNKNOWN -> Icons.Rounded.ErrorOutline
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(appCornerRadius(18.dp)),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = if (draft.canExecute) 0.0f else 0.42f),
        border = BorderStroke(
            1.dp,
            if (draft.canExecute) appPanelBorderColor() else MaterialTheme.colorScheme.error.copy(alpha = 0.28f)
        )
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = appScaledSpacing(12.dp),
                vertical = appScaledSpacing(10.dp)
            ),
            verticalArrangement = Arrangement.spacedBy(appScaledSpacing(10.dp))
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(10.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (draft.canExecute) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(appScaledSpacing(20.dp))
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (draft.canExecute) "Ассистент понял так" else draft.title,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = draft.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = appListSecondaryTextColor(),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(appScaledSpacing(32.dp))
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Закрыть")
                }
            }
            Text(
                text = draft.description,
                style = MaterialTheme.typography.bodySmall,
                color = appListSecondaryTextColor()
            )
            AssistantDraftDetails(draft = draft)
            if (onConfirm != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))
                ) {
                    AssistantActionPill(
                        text = "Отмена",
                        accent = false,
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss
                    )
                    AssistantActionPill(
                        text = "Выполнить",
                        accent = true,
                        modifier = Modifier.weight(1f),
                        onClick = onConfirm
                    )
                }
            }
        }
    }
}

@Composable
private fun AssistantDraftDetails(draft: AssistantDraftAction) {
    val details = remember(draft) { assistantDraftDetails(draft) }
    if (details.isEmpty()) return
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))
    ) {
        details.forEach { (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp)),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = label,
                    modifier = Modifier.weight(0.36f),
                    style = MaterialTheme.typography.labelSmall,
                    color = appListSecondaryTextColor(),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = value,
                    modifier = Modifier.weight(0.64f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun assistantDraftDetails(draft: AssistantDraftAction): List<Pair<String, String>> {
    return buildList {
        draft.date?.let { add("Дата" to formatAssistantDate(it)) }
        if (draft.rangeStart != null && draft.rangeEnd != null) {
            add("Период" to "${formatAssistantDate(draft.rangeStart)} - ${formatAssistantDate(draft.rangeEnd)}")
        }
        draft.shift?.let { add("Смена" to "${it.workplaceName}: ${it.displayCode} · ${it.title}") }
        if (draft.shiftSequence.isNotEmpty()) {
            add("Смены" to draft.shiftSequence.joinToString(" / ") { "${it.displayCode} (${it.workplaceName})" })
        }
        if (draft.datedShiftAssignments.isNotEmpty()) {
            val preview = draft.datedShiftAssignments
                .take(8)
                .joinToString("\n") { "${formatAssistantDate(it.date)} · ${it.shift.workplaceName}: ${it.shift.displayCode}" }
            add("Импорт" to "$preview${if (draft.datedShiftAssignments.size > 8) "\n+${draft.datedShiftAssignments.size - 8} ещё" else ""}")
        }
        if (draft.alarmHour != null || draft.alarmMinute != null || draft.alarmMinutesBefore != null) {
            add(
                "Будильник" to listOfNotNull(
                    if (draft.alarmHour != null && draft.alarmMinute != null) formatAssistantClock(draft.alarmHour, draft.alarmMinute) else null,
                    draft.alarmMinutesBefore?.let { "за $it мин." }
                ).joinToString(" · ").ifBlank { "по настройкам смены" }
            )
        }
        if (draft.noteTitle.isNotBlank()) add("Заголовок" to draft.noteTitle)
        if (draft.noteBody.isNotBlank()) add("Заметка" to draft.noteBody)
        draft.targetTab?.let { add("Раздел" to it.label) }
        if (draft.memoryText.isNotBlank()) add("Правило" to draft.memoryText)
    }
}

@Composable
private fun AssistantActionPill(
    text: String,
    accent: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = appHapticAction(onAction = onClick)),
        shape = RoundedCornerShape(999.dp),
        color = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
        border = BorderStroke(1.dp, if (accent) MaterialTheme.colorScheme.primary else appPanelBorderColor())
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = appScaledSpacing(12.dp),
                vertical = appScaledSpacing(8.dp)
            ),
            style = MaterialTheme.typography.bodySmall,
            color = if (accent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AssistantQuickCommands(
    onPick: (String) -> Unit
) {
    val groups = listOf(
        Triple(
            "Спросить",
            Icons.Rounded.AutoAwesome,
            listOf(
                "Что сегодня?",
                "Когда ближайшая выплата?",
                "Какая следующая смена?",
                "Объясни расчёт зарплаты",
                "Почему факт выплат отличается от расчёта?"
            )
        ),
        Triple(
            "Сделать",
            Icons.AutoMirrored.Rounded.NoteAdd,
            listOf(
                "Поставь смену Н завтра",
                "Добавь на следующую Д смену заметку: проверить переключения",
                "Будильник для Н за 90 минут",
                "Создай будильники для всех смен за 90 минут",
                "Очисти завтра"
            )
        ),
        Triple(
            "Пакеты",
            Icons.Rounded.CalendarMonth,
            listOf(
                "Поставь Д через день с 5 по 15 мая",
                "График Д Н с 1 по 8 мая",
                "Поставь Д по будням до конца месяца",
                "Разбери график:\n01.05 Д\n02.05 Н"
            )
        ),
        Triple(
            "Проверки",
            Icons.Rounded.ErrorOutline,
            listOf(
                "Проверь график на месяц",
                "Сводка недели по сменам",
                "Проверь качество данных",
                "Проверь зарплату и факт выплат",
                "Запомни: ночная смена обычно называется Н"
            )
        ),
        Triple(
            "Разбор",
            Icons.Rounded.Image,
            listOf(
                "Разбери текст графика:\n01.05 Д\n02.05 Н",
                "Я вставлю OCR со скриншота графика, преврати его в смены"
            )
        )
    )
    AssistantCard(title = "Быстрые сценарии") {
        Column(
            verticalArrangement = Arrangement.spacedBy(appScaledSpacing(12.dp))
        ) {
            groups.forEach { (title, icon, samples) ->
                AssistantCommandScenarioCard(
                    title = title,
                    icon = icon,
                    samples = samples,
                    onPick = onPick
                )
            }
        }
    }
}

@Composable
private fun AssistantCommandScenarioCard(
    title: String,
    icon: ImageVector,
    samples: List<String>,
    onPick: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(appCornerRadius(18.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Column(
            modifier = Modifier.padding(appScaledSpacing(12.dp)),
            verticalArrangement = Arrangement.spacedBy(appScaledSpacing(9.dp))
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(appScaledSpacing(6.dp))
                            .size(appScaledSpacing(17.dp)),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
            samples.chunked(2).forEach { rowSamples ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))
                ) {
                    rowSamples.forEach { sample ->
                        AssistantCommandChip(
                            text = sample.replace('\n', ' '),
                            onClick = { onPick(sample) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowSamples.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun AssistantCommandChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(appCornerRadius(14.dp)),
        color = appPanelColor().copy(alpha = 0.74f),
        border = BorderStroke(1.dp, appPanelBorderColor()),
        modifier = modifier.clickable(onClick = appHapticAction(onAction = onClick))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = appScaledSpacing(10.dp),
                vertical = appScaledSpacing(8.dp)
            ),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AssistantBackendNotice() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(appCornerRadius(18.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f),
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Column(
            modifier = Modifier.padding(appCardPadding()),
            verticalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))
        ) {
            Text(
                text = "Бесплатный локальный режим",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Команды разбираются на устройстве: приватно, без ключей и платных API.",
                style = MaterialTheme.typography.bodySmall,
                color = appListSecondaryTextColor()
            )
        }
    }
}

@Composable
private fun AssistantAdvancedModeCard(
    settings: AssistantAiSettings,
    onChange: (AssistantAiSettings) -> Unit,
    isThinking: Boolean,
    apiTestStatus: String,
    onTestApi: () -> Unit,
    historyCount: Int,
    onClearHistory: () -> Unit
) {
    AssistantCard(title = "Продвинутый режим") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(10.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (settings.advancedModeEnabled) {
                        "${settings.provider.shortLabel} API включён"
                    } else {
                        "Локальный режим"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (settings.advancedModeEnabled && settings.hasApiKey) {
                        "Команды сначала разбирает модель, затем приложение выполняет безопасное действие."
                    } else {
                        "Без ключа всё работает бесплатно и локально, просто менее гибко."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = appListSecondaryTextColor()
                )
            }
            AssistantTogglePill(
                checked = settings.advancedModeEnabled,
                enabled = !isThinking,
                onClick = {
                    onChange(settings.copy(advancedModeEnabled = !settings.advancedModeEnabled))
                }
            )
        }

        if (settings.advancedModeEnabled) {
            Text(
                text = "Провайдер",
                style = MaterialTheme.typography.bodySmall,
                color = appListSecondaryTextColor(),
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))
            ) {
                AssistantAiProvider.entries.forEach { provider ->
                    AssistantActionPill(
                        text = provider.shortLabel,
                        accent = settings.provider == provider,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onChange(settings.copy(provider = provider))
                        }
                    )
                }
            }
            Text(
                text = settings.provider.freeTierHint,
                style = MaterialTheme.typography.bodySmall,
                color = appListSecondaryTextColor()
            )
            AssistantSettingsTextField(
                label = "${settings.provider.shortLabel}: API-ключ",
                value = settings.selectedApiKey,
                placeholder = settings.provider.keyPlaceholder,
                obscure = true,
                onValueChange = { onChange(settings.withSelectedApiKey(it.trim())) }
            )
            AssistantModelPicker(
                provider = settings.provider,
                selectedModel = settings.selectedModel,
                onSelect = { model ->
                    onChange(settings.withSelectedModel(model.id))
                }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))
            ) {
                AssistantActionPill(
                    text = if (isThinking) "Проверяю..." else "Проверить API",
                    accent = true,
                    modifier = Modifier.weight(1f),
                    onClick = onTestApi
                )
                AssistantActionPill(
                    text = "Сбросить модель",
                    accent = false,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onChange(settings.withSelectedModel(settings.provider.defaultModel))
                    }
                )
            }
            if (apiTestStatus.isNotBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(appCornerRadius(16.dp)),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f),
                    border = BorderStroke(1.dp, appPanelBorderColor())
                ) {
                    Text(
                        text = apiTestStatus,
                        modifier = Modifier.padding(appScaledSpacing(12.dp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = appListSecondaryTextColor()
                    )
                }
            }
            AssistantSettingToggleRow(
                title = "Подтверждать действия",
                description = "Перед сменами, заметками, будильниками и очисткой дня показывать карточку проверки.",
                checked = settings.confirmBeforeActions,
                enabled = !isThinking,
                onClick = {
                    onChange(settings.copy(confirmBeforeActions = !settings.confirmBeforeActions))
                }
            )
            AssistantSettingToggleRow(
                title = "Подтверждать заметки",
                description = "Если выключено, команды создания заметок выполняются сразу.",
                checked = settings.confirmNoteActions,
                enabled = !isThinking && settings.confirmBeforeActions,
                onClick = {
                    onChange(settings.copy(confirmNoteActions = !settings.confirmNoteActions))
                }
            )
            AssistantSettingToggleRow(
                title = "Финансы в API",
                description = "Передавать ближайшие выплаты в ${settings.provider.shortLabel}. Выключено по умолчанию для приватности.",
                checked = settings.sendFinancialContextToAi,
                enabled = !isThinking,
                onClick = {
                    onChange(settings.copy(sendFinancialContextToAi = !settings.sendFinancialContextToAi))
                }
            )
            AssistantHistoryAndMemoryBlock(
                settings = settings,
                onChange = onChange,
                isThinking = isThinking,
                historyCount = historyCount,
                onClearHistory = onClearHistory
            )
            Text(
                text = "Ключ хранится локально в профиле приложения. Для бэкапа я его намеренно не добавляю, чтобы случайно не утаскивать секреты в JSON.",
                style = MaterialTheme.typography.bodySmall,
                color = appListSecondaryTextColor()
            )
        } else {
            AssistantSettingToggleRow(
                title = "Подтверждать действия",
                description = "Работает и в локальном режиме: сначала проверяем, потом выполняем.",
                checked = settings.confirmBeforeActions,
                enabled = !isThinking,
                onClick = {
                    onChange(settings.copy(confirmBeforeActions = !settings.confirmBeforeActions))
                }
            )
            AssistantSettingToggleRow(
                title = "Подтверждать заметки",
                description = "Можно оставить выключенным: заметки обычно безопаснее смен и очистки.",
                checked = settings.confirmNoteActions,
                enabled = !isThinking && settings.confirmBeforeActions,
                onClick = {
                    onChange(settings.copy(confirmNoteActions = !settings.confirmNoteActions))
                }
            )
            AssistantHistoryAndMemoryBlock(
                settings = settings,
                onChange = onChange,
                isThinking = isThinking,
                historyCount = historyCount,
                onClearHistory = onClearHistory
            )
        }
    }
}

@Composable
private fun AssistantVoiceAndAppearanceCard(
    settings: AssistantAiSettings,
    onChange: (AssistantAiSettings) -> Unit,
    isThinking: Boolean,
    ttsEngines: List<TextToSpeech.EngineInfo>,
    onOpenSystemTtsSettings: () -> Unit,
    onTestVoice: () -> Unit
) {
    AssistantCard(title = "Голос и оформление") {
        AssistantSettingToggleRow(
            title = "Озвучивать ответы",
            description = "Ассистент будет читать новые ответы выбранным голосом.",
            checked = settings.voiceRepliesEnabled,
            enabled = !isThinking,
            onClick = {
                onChange(settings.copy(voiceRepliesEnabled = !settings.voiceRepliesEnabled))
            }
        )
        AssistantVoiceReplyMode.entries.toList().chunked(2).forEach { rowModes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))
            ) {
                rowModes.forEach { mode ->
                    AssistantActionPill(
                        text = mode.label,
                        accent = settings.voiceReplyMode == mode,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onChange(
                                settings.copy(
                                    voiceReplyMode = mode,
                                    voiceRepliesEnabled = settings.voiceRepliesEnabled ||
                                        mode != AssistantVoiceReplyMode.SYSTEM
                                )
                            )
                        }
                    )
                }
                if (rowModes.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        Text(
            text = settings.voiceReplyMode.description,
            style = MaterialTheme.typography.bodySmall,
            color = appListSecondaryTextColor()
        )
        when (settings.voiceReplyMode) {
            AssistantVoiceReplyMode.SYSTEM -> {
                AssistantSystemTtsSettings(
                    settings = settings,
                    onChange = onChange,
                    ttsEngines = ttsEngines,
                    onOpenSystemTtsSettings = onOpenSystemTtsSettings
                )
            }
            AssistantVoiceReplyMode.OPENAI -> {
                AssistantOpenAiTtsSettings(
                    settings = settings,
                    onChange = onChange
                )
            }
            AssistantVoiceReplyMode.GEMINI -> {
                AssistantGeminiTtsSettings(
                    settings = settings,
                    onChange = onChange
                )
            }
            AssistantVoiceReplyMode.SALUTE_SPEECH -> {
                AssistantSaluteSpeechSettings(
                    settings = settings,
                    onChange = onChange
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))
        ) {
            AssistantActionPill(
                text = "Тест голоса",
                accent = true,
                modifier = Modifier.weight(1f),
                onClick = onTestVoice
            )
        }
        Text(
            text = "Цвета чата",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = appListSecondaryTextColor()
        )
        AssistantChatColorPickerRow(
            title = "Фон",
            selectedHex = settings.chatBackgroundColorHex,
            fallbackHex = "#F4F8F7",
            onSelect = { color ->
                onChange(settings.copy(chatBackgroundColorHex = color))
            }
        )
        AssistantChatColorPickerRow(
            title = "Мои сообщения",
            selectedHex = settings.userBubbleColorHex,
            fallbackHex = "#E5F4EF",
            onSelect = { color ->
                onChange(settings.copy(userBubbleColorHex = color))
            }
        )
        AssistantChatColorPickerRow(
            title = "Ответы ассистента",
            selectedHex = settings.assistantBubbleColorHex,
            fallbackHex = "#ECEFF5",
            onSelect = { color ->
                onChange(settings.copy(assistantBubbleColorHex = color))
            }
        )
    }
}

@Composable
private fun AssistantSystemTtsSettings(
    settings: AssistantAiSettings,
    onChange: (AssistantAiSettings) -> Unit,
    ttsEngines: List<TextToSpeech.EngineInfo>,
    onOpenSystemTtsSettings: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))
    ) {
        Text(
            text = "Локальный движок",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = appListSecondaryTextColor()
        )
        AssistantActionPill(
            text = "По умолчанию Android",
            accent = settings.systemTtsEnginePackage.isBlank(),
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                onChange(settings.copy(systemTtsEnginePackage = ""))
            }
        )
        ttsEngines.chunked(2).forEach { rowEngines ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))
            ) {
                rowEngines.forEach { engine ->
                    AssistantActionPill(
                        text = engine.label?.toString().orEmpty().ifBlank { engine.name },
                        accent = settings.systemTtsEnginePackage == engine.name,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onChange(settings.copy(systemTtsEnginePackage = engine.name))
                        }
                    )
                }
                if (rowEngines.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        AssistantActionPill(
            text = "Настройки TTS Android",
            accent = false,
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenSystemTtsSettings
        )
    }
}

@Composable
private fun AssistantOpenAiTtsSettings(
    settings: AssistantAiSettings,
    onChange: (AssistantAiSettings) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))
    ) {
        if (settings.openAiApiKey.isBlank()) {
            AssistantVoiceWarning("Для нейро-голоса нужен OpenAI API-ключ в блоке продвинутого режима.")
        }
        Text(
            text = "Модель голоса",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = appListSecondaryTextColor()
        )
        AssistantOpenAiTtsModels.chunked(2).forEach { rowModels ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))
            ) {
                rowModels.forEach { (model, label) ->
                    AssistantActionPill(
                        text = label,
                        accent = settings.openAiTtsModel == model,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onChange(settings.copy(openAiTtsModel = model))
                        }
                    )
                }
                if (rowModels.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        Text(
            text = "Голос",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = appListSecondaryTextColor()
        )
        AssistantOpenAiTtsVoices.chunked(3).forEach { rowVoices ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))
            ) {
                rowVoices.forEach { voice ->
                    AssistantActionPill(
                        text = voice.replaceFirstChar { it.uppercase() },
                        accent = settings.openAiTtsVoice == voice,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onChange(settings.copy(openAiTtsVoice = voice))
                        }
                    )
                }
                repeat(3 - rowVoices.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        AssistantSettingsTextField(
            label = "Стиль речи",
            value = settings.openAiTtsInstructions,
            placeholder = "Например: говори спокойно и коротко",
            singleLine = false,
            onValueChange = {
                onChange(settings.copy(openAiTtsInstructions = it.take(500)))
            }
        )
        Text(
            text = "Нейро-голос генерируется AI и может расходовать баланс OpenAI API.",
            style = MaterialTheme.typography.bodySmall,
            color = appListSecondaryTextColor()
        )
    }
}

@Composable
private fun AssistantGeminiTtsSettings(
    settings: AssistantAiSettings,
    onChange: (AssistantAiSettings) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))
    ) {
        if (settings.geminiApiKey.isBlank()) {
            AssistantVoiceWarning("Для Gemini TTS нужен Gemini API-ключ в блоке продвинутого режима.")
        }
        AssistantSegmentedChipRows(
            title = "Модель Gemini TTS",
            options = AssistantGeminiTtsModels,
            selected = settings.geminiTtsModel,
            columns = 2,
            onSelect = { model ->
                onChange(settings.copy(geminiTtsModel = model))
            }
        )
        AssistantSegmentedChipRows(
            title = "Голос Gemini",
            options = AssistantGeminiTtsVoices.map { it to it },
            selected = settings.geminiTtsVoice,
            columns = 3,
            onSelect = { voice ->
                onChange(settings.copy(geminiTtsVoice = voice))
            }
        )
        AssistantSettingsTextField(
            label = "Инструкция голосу",
            value = settings.geminiTtsInstructions,
            placeholder = "Say in Russian with a calm assistant voice:",
            singleLine = false,
            onValueChange = {
                onChange(settings.copy(geminiTtsInstructions = it.take(500)))
            }
        )
    }
}

@Composable
private fun AssistantSaluteSpeechSettings(
    settings: AssistantAiSettings,
    onChange: (AssistantAiSettings) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))
    ) {
        AssistantVoiceWarning("SaluteSpeech использует отдельный Authorization key. Ключ GigaChat может не подойти.")
        AssistantSettingsTextField(
            label = "SaluteSpeech: Authorization key",
            value = settings.saluteSpeechAuthorizationKey,
            placeholder = "Base64 ClientID:ClientSecret",
            obscure = true,
            onValueChange = {
                onChange(settings.copy(saluteSpeechAuthorizationKey = it.trim()))
            }
        )
        AssistantSegmentedChipRows(
            title = "Scope",
            options = AssistantSaluteSpeechScopes.map { it to it.removePrefix("SALUTE_SPEECH_").lowercase() },
            selected = settings.saluteSpeechScope,
            columns = 2,
            onSelect = { scope ->
                onChange(settings.copy(saluteSpeechScope = scope))
            }
        )
        AssistantSegmentedChipRows(
            title = "Голос SaluteSpeech",
            options = AssistantSaluteSpeechVoices.map { it to it.removeSuffix("_24000") },
            selected = settings.saluteSpeechVoice,
            columns = 3,
            onSelect = { voice ->
                onChange(settings.copy(saluteSpeechVoice = voice))
            }
        )
    }
}

@Composable
private fun AssistantVoiceWarning(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(appCornerRadius(16.dp)),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.34f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.24f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(appScaledSpacing(10.dp)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AssistantSegmentedChipRows(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    columns: Int,
    onSelect: (String) -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Bold,
        color = appListSecondaryTextColor()
    )
    options.chunked(columns).forEach { rowOptions ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))
        ) {
            rowOptions.forEach { (value, label) ->
                AssistantActionPill(
                    text = label,
                    accent = selected == value,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(value) }
                )
            }
            repeat(columns - rowOptions.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AssistantChatColorPickerRow(
    title: String,
    selectedHex: String,
    fallbackHex: String,
    onSelect: (String) -> Unit
) {
    var showPicker by rememberSaveable(title) { mutableStateOf(false) }
    val normalizedSelected = selectedHex.takeIf { it.isNotBlank() }?.let { normalizeHexColor(it) }.orEmpty()
    val pickerColor = normalizedSelected.ifBlank { normalizeHexColor(fallbackHex) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(appScaledSpacing(7.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistantAutoColorChip(
                    text = "Пикер",
                    selected = showPicker,
                    onClick = { showPicker = !showPicker }
                )
                AssistantAutoColorChip(
                    selected = selectedHex.isBlank(),
                    onClick = { onSelect("") }
                )
            }
        }
        AssistantChatColorPalette.chunked(6).forEach { rowColors ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowColors.forEach { colorHex ->
                    AssistantColorSwatch(
                        colorHex = colorHex,
                        selected = normalizedSelected.equals(normalizeHexColor(colorHex), ignoreCase = true),
                        onClick = { onSelect(normalizeHexColor(colorHex)) }
                    )
                }
            }
        }
        if (showPicker) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(appCornerRadius(18.dp)),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.56f),
                border = BorderStroke(1.dp, appPanelBorderColor())
            ) {
                Box(modifier = Modifier.padding(appScaledSpacing(12.dp))) {
                    FullColorPicker(
                        selectedColorHex = pickerColor,
                        onColorSelected = onSelect
                    )
                }
            }
        }
    }
}

@Composable
private fun AssistantAutoColorChip(
    text: String = "Авто",
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.62f)
        },
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.48f) else appPanelBorderColor()
        ),
        modifier = Modifier.clickable(onClick = appHapticAction(onAction = onClick))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = appScaledSpacing(10.dp),
                vertical = appScaledSpacing(5.dp)
            ),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (selected) MaterialTheme.colorScheme.primary else appListSecondaryTextColor()
        )
    }
}

@Composable
private fun AssistantColorSwatch(
    colorHex: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color = Color(parseColorHex(colorHex, MaterialTheme.colorScheme.primary.toArgb()))
    Surface(
        modifier = Modifier
            .size(appScaledSpacing(32.dp))
            .clickable(onClick = appHapticAction(onAction = onClick)),
        shape = RoundedCornerShape(999.dp),
        color = color,
        border = BorderStroke(
            width = if (selected) 3.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else appPanelBorderColor()
        )
    ) {}
}

@Composable
private fun AssistantModelPicker(
    provider: AssistantAiProvider,
    selectedModel: String,
    onSelect: (AssistantAiModelOption) -> Unit
) {
    val options = provider.modelOptions()
    val selectedFromList = options.any { it.id == selectedModel }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))
    ) {
        Text(
            text = "Модель",
            style = MaterialTheme.typography.bodySmall,
            color = appListSecondaryTextColor(),
            fontWeight = FontWeight.Bold
        )
        if (!selectedFromList && selectedModel.isNotBlank()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(appCornerRadius(16.dp)),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.24f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f))
            ) {
                Column(
                    modifier = Modifier.padding(appScaledSpacing(12.dp)),
                    verticalArrangement = Arrangement.spacedBy(appScaledSpacing(3.dp))
                ) {
                    Text(
                        text = "Текущая модель: $selectedModel",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Её можно оставить, но ниже есть проверенные варианты для ${provider.shortLabel}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = appListSecondaryTextColor()
                    )
                }
            }
        }
        options.forEach { option ->
            AssistantModelOptionRow(
                option = option,
                selected = selectedModel == option.id || (!selectedFromList && selectedModel.isBlank() && option.recommended),
                onClick = { onSelect(option) }
            )
        }
    }
}

@Composable
private fun AssistantModelOptionRow(
    option: AssistantAiModelOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = appHapticAction(onAction = onClick)),
        shape = RoundedCornerShape(appCornerRadius(16.dp)),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f)
        },
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.52f) else appPanelBorderColor()
        )
    ) {
        Row(
            modifier = Modifier.padding(appScaledSpacing(12.dp)),
            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(10.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.62f)
                }
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(appScaledSpacing(7.dp))
                        .size(appScaledSpacing(17.dp))
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = option.title,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (option.recommended) {
                        AssistantSmallTag(text = "рек.")
                    }
                    AssistantSmallTag(text = if (option.supportsImages) "vision" else "текст")
                }
                Text(
                    text = option.id,
                    style = MaterialTheme.typography.labelSmall,
                    color = appListSecondaryTextColor(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = option.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = appListSecondaryTextColor(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AssistantSmallTag(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f),
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = appScaledSpacing(6.dp),
                vertical = appScaledSpacing(2.dp)
            ),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun AssistantAiStatusCard(
    settings: AssistantAiSettings,
    apiTestStatus: String,
    onOpenSettings: () -> Unit
) {
    AssistantCard(title = "ИИ-режим") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(12.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(appScaledSpacing(30.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (settings.advancedModeEnabled && settings.hasApiKey) {
                        "${settings.provider.shortLabel} подключён"
                    } else {
                        "Локальный режим"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when {
                        settings.advancedModeEnabled && settings.hasApiKey -> {
                            "Команды разбирает модель, действия всё равно проходят через проверку приложения."
                        }
                        settings.advancedModeEnabled -> {
                            "Выбран ${settings.provider.shortLabel}, но ключ ещё не задан."
                        }
                        else -> {
                            "Бесплатно и приватно, но сложные команды лучше понимает API."
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = appListSecondaryTextColor()
                )
                if (apiTestStatus.isNotBlank()) {
                    Text(
                        text = apiTestStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = appListSecondaryTextColor(),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
                border = BorderStroke(1.dp, appPanelBorderColor()),
                modifier = Modifier.clickable(onClick = appHapticAction(onAction = onOpenSettings))
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = appScaledSpacing(12.dp),
                        vertical = appScaledSpacing(8.dp)
                    ),
                    horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(appScaledSpacing(18.dp))
                    )
                    Text(
                        text = "Настройки",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AssistantAiSettingsScreen(
    settings: AssistantAiSettings,
    onChange: (AssistantAiSettings) -> Unit,
    isThinking: Boolean,
    apiTestStatus: String,
    onTestApi: () -> Unit,
    historyCount: Int,
    onClearHistory: () -> Unit,
    ttsEngines: List<TextToSpeech.EngineInfo>,
    onOpenSystemTtsSettings: () -> Unit,
    onTestVoice: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(top = appScreenPadding())
            .padding(bottom = appScaledSpacing(32.dp)),
        verticalArrangement = Arrangement.spacedBy(appBlockSpacing())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(12.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(appCornerRadius(22.dp)),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
                border = BorderStroke(1.dp, appPanelBorderColor()),
                modifier = Modifier
                    .size(appScaledSpacing(52.dp))
                    .clickable(onClick = appHapticAction(onAction = onBack))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Назад",
                        modifier = Modifier.size(appScaledSpacing(28.dp))
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ИИ-ассистент",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Провайдеры, приватность, память и диагностика",
                    style = MaterialTheme.typography.bodySmall,
                    color = appListSecondaryTextColor()
                )
            }
        }

        AssistantProviderDiagnosticsCard(settings = settings, apiTestStatus = apiTestStatus)
        AssistantAdvancedModeCard(
            settings = settings,
            onChange = onChange,
            isThinking = isThinking,
            apiTestStatus = apiTestStatus,
            onTestApi = onTestApi,
            historyCount = historyCount,
            onClearHistory = onClearHistory
        )
        AssistantVoiceAndAppearanceCard(
            settings = settings,
            onChange = onChange,
            isThinking = isThinking,
            ttsEngines = ttsEngines,
            onOpenSystemTtsSettings = onOpenSystemTtsSettings,
            onTestVoice = onTestVoice
        )
        AssistantScenarioGuideCard()
    }
}

@Composable
private fun AssistantProviderDiagnosticsCard(
    settings: AssistantAiSettings,
    apiTestStatus: String
) {
    AssistantCard(title = "Диагностика") {
        AssistantDiagnosticRow(
            title = "Провайдер",
            value = settings.provider.label,
            state = if (settings.advancedModeEnabled) "активен" else "локально"
        )
        AssistantDiagnosticRow(
            title = "Ключ",
            value = if (settings.hasApiKey) "задан" else "не задан",
            state = if (settings.hasApiKey) "OK" else "Info"
        )
        AssistantDiagnosticRow(
            title = "Модель",
            value = settings.selectedModel,
            state = "model"
        )
        if (apiTestStatus.isNotBlank()) {
            AssistantDiagnosticRow(
                title = "Последняя проверка",
                value = apiTestStatus,
                state = when {
                    apiTestStatus.contains("работает", ignoreCase = true) -> "OK"
                    apiTestStatus.contains("провер", ignoreCase = true) -> "..."
                    else -> "!"
                }
            )
        }
    }
}

@Composable
private fun AssistantDiagnosticRow(
    title: String,
    value: String,
    state: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(10.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = appListSecondaryTextColor(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f)
        ) {
            Text(
                text = state,
                modifier = Modifier.padding(
                    horizontal = appScaledSpacing(10.dp),
                    vertical = appScaledSpacing(5.dp)
                ),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = if (state == "!" ) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun AssistantScenarioGuideCard() {
    val scenarios = listOf(
        "Поставь график Д Н с 1 по 8 мая",
        "Разбери график:\n01.05 Д\n02.05 Н",
        "Проверь график на месяц",
        "Проверь качество данных",
        "Почему факт выплат отличается от расчёта?",
        "Добавь на следующую ночную смену заметку: проверить оборудование",
        "Будильник для Н за 90 минут",
        "Создай будильники для всех смен за 90 минут",
        "Разбери OCR со скриншота графика и назначь смены",
        "Запомни: ГТП-2 = работа 1"
    )
    AssistantCard(title = "Что уже умеет") {
        scenarios.forEach { scenario ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp)),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = scenario,
                    style = MaterialTheme.typography.bodySmall,
                    color = appListSecondaryTextColor()
                )
            }
        }
    }
}

@Composable
private fun AssistantHistoryAndMemoryBlock(
    settings: AssistantAiSettings,
    onChange: (AssistantAiSettings) -> Unit,
    isThinking: Boolean,
    historyCount: Int,
    onClearHistory: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(appCornerRadius(18.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Column(
            modifier = Modifier.padding(appScaledSpacing(12.dp)),
            verticalArrangement = Arrangement.spacedBy(appScaledSpacing(10.dp))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(10.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "История и память",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Реплик в текущем диалоге: $historyCount",
                        style = MaterialTheme.typography.bodySmall,
                        color = appListSecondaryTextColor()
                    )
                }
                AssistantActionPill(
                    text = "Очистить",
                    accent = false,
                    onClick = onClearHistory
                )
            }
            AssistantSettingToggleRow(
                title = "История в API",
                description = "Передавать последние реплики, чтобы ассистент понимал уточнения вроде “да, на завтра”.",
                checked = settings.sendChatHistoryToAi,
                enabled = !isThinking,
                onClick = {
                    onChange(settings.copy(sendChatHistoryToAi = !settings.sendChatHistoryToAi))
                }
            )
            val memoryRules = remember(settings.assistantMemory) {
                parseAssistantMemoryRules(settings.assistantMemory)
            }
            var newRuleKey by rememberSaveable { mutableStateOf("") }
            var newRuleValue by rememberSaveable { mutableStateOf("") }
            if (memoryRules.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))
                ) {
                    memoryRules.forEach { rule ->
                        AssistantMemoryRuleCard(
                            rule = rule,
                            onRemove = {
                                onChange(
                                    settings.copy(
                                        assistantMemory = removeAssistantMemoryRule(
                                            settings.assistantMemory,
                                            rule.raw
                                        )
                                    )
                                )
                            }
                        )
                    }
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(appCornerRadius(16.dp)),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.42f),
                border = BorderStroke(1.dp, appPanelBorderColor())
            ) {
                Column(
                    modifier = Modifier.padding(appScaledSpacing(10.dp)),
                    verticalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))
                ) {
                    Text(
                        text = "Добавить правило",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))
                    ) {
                        AssistantSettingsTextField(
                            label = "Что",
                            value = newRuleKey,
                            placeholder = "Н",
                            modifier = Modifier.weight(0.38f),
                            onValueChange = { newRuleKey = it.take(40) }
                        )
                        AssistantSettingsTextField(
                            label = "Значит",
                            value = newRuleValue,
                            placeholder = "ночная смена",
                            modifier = Modifier.weight(0.62f),
                            onValueChange = { newRuleValue = it.take(120) }
                        )
                    }
                    AssistantActionPill(
                        text = "Добавить в память",
                        accent = true,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val rawRule = listOf(newRuleKey.trim(), newRuleValue.trim())
                                .filter { it.isNotBlank() }
                                .joinToString(" = ")
                            if (rawRule.isNotBlank()) {
                                onChange(settings.copy(assistantMemory = appendAssistantMemory(settings.assistantMemory, rawRule)))
                                newRuleKey = ""
                                newRuleValue = ""
                            }
                        }
                    )
                }
            }
            AssistantSettingsTextField(
                label = "Память одним текстом",
                value = settings.assistantMemory,
                placeholder = "Например: Д = дневная, Н = ночная; по умолчанию работа 1",
                singleLine = false,
                onValueChange = { onChange(settings.copy(assistantMemory = it.take(900))) }
            )
        }
    }
}

private data class AssistantMemoryRule(
    val key: String,
    val value: String,
    val raw: String
)

@Composable
private fun AssistantMemoryRuleCard(
    rule: AssistantMemoryRule,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(appCornerRadius(14.dp)),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f),
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Row(
            modifier = Modifier.padding(appScaledSpacing(10.dp)),
            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Text(
                    text = rule.key.ifBlank { "Правило" },
                    modifier = Modifier.padding(
                        horizontal = appScaledSpacing(10.dp),
                        vertical = appScaledSpacing(5.dp)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = rule.value.ifBlank { rule.raw },
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = appListSecondaryTextColor(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(onClick = appHapticAction(onAction = onRemove)) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Удалить правило",
                    modifier = Modifier.size(appScaledSpacing(18.dp))
                )
            }
        }
    }
}

@Composable
private fun AssistantSettingToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(10.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = appListSecondaryTextColor()
            )
        }
        AssistantTogglePill(
            checked = checked,
            enabled = enabled,
            onClick = onClick
        )
    }
}

@Composable
private fun AssistantTogglePill(
    checked: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (checked) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
        },
        border = BorderStroke(1.dp, appPanelBorderColor()),
        modifier = Modifier.clickable(enabled = enabled, onClick = appHapticAction(onAction = onClick))
    ) {
        Text(
            text = if (checked) "Вкл" else "Выкл",
            modifier = Modifier.padding(
                horizontal = appScaledSpacing(14.dp),
                vertical = appScaledSpacing(8.dp)
            ),
            style = MaterialTheme.typography.bodySmall,
            color = if (checked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AssistantSettingsTextField(
    label: String,
    value: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    obscure: Boolean = false,
    onValueChange: (String) -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(appCornerRadius(16.dp)),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.66f),
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = appScaledSpacing(12.dp),
                vertical = appScaledSpacing(9.dp)
            ),
            verticalArrangement = Arrangement.spacedBy(appScaledSpacing(4.dp))
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = appListSecondaryTextColor(),
                fontWeight = FontWeight.Bold
            )
            Box(contentAlignment = Alignment.CenterStart) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = singleLine,
                    visualTransformation = if (obscure) PasswordVisualTransformation() else VisualTransformation.None,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (singleLine) appScaledSpacing(24.dp) else appScaledSpacing(72.dp))
                )
                if (value.isBlank()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = appListSecondaryTextColor()
                    )
                }
            }
        }
    }
}

@Composable
private fun AssistantCard(
    title: String,
    content: @Composable () -> Unit
) {
    AppExpressiveSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(appCornerRadius(22.dp)),
        tone = AppExpressiveSurfaceTone.SOFT,
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Column(
            modifier = Modifier.padding(appCardPadding()),
            verticalArrangement = Arrangement.spacedBy(appScaledSpacing(10.dp))
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            content()
        }
    }
}

@Composable
private fun assistantChatBackgroundColor(settings: AssistantAiSettings): Color {
    val fallback = Color.Transparent
    return assistantColorFromHex(settings.chatBackgroundColorHex, fallback).let { color ->
        if (settings.chatBackgroundColorHex.isBlank()) color else color.copy(alpha = 0.38f)
    }
}

@Composable
private fun assistantUserBubbleColor(settings: AssistantAiSettings): Color {
    val fallback = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    return assistantColorFromHex(settings.userBubbleColorHex, fallback)
}

@Composable
private fun assistantReplyBubbleColor(settings: AssistantAiSettings): Color {
    val fallback = appBubbleBackgroundColor(defaultAlpha = 0.22f)
    return assistantColorFromHex(settings.assistantBubbleColorHex, fallback)
}

private fun assistantColorFromHex(value: String, fallback: Color): Color {
    if (value.isBlank()) return fallback
    return Color(parseColorHex(value, fallback.toArgb()))
}

private fun AssistantAiSettings.neuralVoiceReadinessError(): String? {
    return when (voiceReplyMode) {
        AssistantVoiceReplyMode.SYSTEM -> null
        AssistantVoiceReplyMode.OPENAI -> if (openAiApiKey.isBlank()) {
            "Для голоса OpenAI нужен OpenAI API-ключ."
        } else {
            null
        }
        AssistantVoiceReplyMode.GEMINI -> if (geminiApiKey.isBlank()) {
            "Для голоса Gemini нужен Gemini API-ключ."
        } else {
            null
        }
        AssistantVoiceReplyMode.SALUTE_SPEECH -> if (saluteSpeechAuthorizationKey.isBlank()) {
            "Для SaluteSpeech нужен отдельный Authorization key."
        } else {
            null
        }
    }
}

private fun AssistantVoiceReplyMode.errorPrefix(message: String?): String {
    val details = message.orEmpty().ifBlank { "не удалось озвучить ответ" }
    return "$label: $details"
}

private suspend fun playNeuralSpeech(
    context: Context,
    settings: AssistantAiSettings,
    text: String,
    currentPlayer: MediaPlayer?,
    shouldStartPlayback: () -> Boolean,
    onPlayerReady: (MediaPlayer) -> Unit,
    onPlaybackFinished: () -> Unit
): Boolean {
    val audioBytes = when (settings.voiceReplyMode) {
        AssistantVoiceReplyMode.OPENAI -> AiAssistantClient.requestOpenAiSpeech(settings, text)
        AssistantVoiceReplyMode.GEMINI -> AiAssistantClient.requestGeminiSpeech(settings, text)
        AssistantVoiceReplyMode.SALUTE_SPEECH -> AiAssistantClient.requestSaluteSpeech(settings, text)
        AssistantVoiceReplyMode.SYSTEM -> error("Выбран локальный TTS.")
    }
    val audioFile = withContext(Dispatchers.IO) {
        val extension = if (settings.voiceReplyMode == AssistantVoiceReplyMode.OPENAI) "mp3" else "wav"
        File(context.cacheDir, "assistant_neural_tts.$extension").apply {
            writeBytes(audioBytes)
        }
    }
    currentPlayer?.release()
    if (!shouldStartPlayback()) {
        return false
    }
    val player = MediaPlayer().apply {
        setDataSource(audioFile.absolutePath)
        setOnCompletionListener { completedPlayer ->
            completedPlayer.release()
            onPlaybackFinished()
        }
        setOnErrorListener { errorPlayer, _, _ ->
            errorPlayer.release()
            onPlaybackFinished()
            true
        }
        prepare()
        start()
    }
    onPlayerReady(player)
    return true
}

@Composable
private fun assistantReadableTextColor(background: Color): Color {
    return if (background.alpha > 0.75f && background.luminance() < 0.32f) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }
}

@Composable
private fun AssistantMiniRow(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(10.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(appScaledSpacing(7.dp))
                    .size(appScaledSpacing(18.dp)),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = appListSecondaryTextColor(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun buildAssistantDraftFromOpenAiJson(
    rawJson: String,
    shifts: List<AssistantShiftOption>
): AssistantDraftAction? {
    val root = runCatching { JSONObject(rawJson) }.getOrNull() ?: return null
    val action = root.optString("action").lowercase(Locale.ROOT)
    val message = root.optString("message").ifBlank { root.optString("answerText") }
    val shift = root.optString("shiftCode")
        .takeIf { it.isNotBlank() }
        ?.let { shiftCode -> findAssistantShiftByCode(shiftCode, shifts) }
    val shiftSequence = root.optJSONArray("shiftCodes")
        ?.let { array ->
            (0 until array.length()).mapNotNull { index ->
                array.optString(index).takeIf { it.isNotBlank() }?.let { findAssistantShiftByCode(it, shifts) }
            }
        }
        .orEmpty()

    fun jsonDate(field: String): LocalDate? {
        return root.optString(field)
            .takeIf { it.isNotBlank() }
            ?.let { value -> runCatching { LocalDate.parse(value) }.getOrNull() }
    }

    fun jsonInt(field: String): Int? {
        return if (root.has(field) && !root.isNull(field)) root.optInt(field) else null
    }

    return when (action) {
        "assign_shift" -> {
            val date = jsonDate("date")
            if (date == null || shift == null) {
                AssistantDraftAction(
                    kind = AssistantActionKind.UNKNOWN,
                    title = "Нужно уточнение",
                    description = message.ifBlank { "ИИ не указал дату или смену." },
                    canExecute = false
                )
            } else {
                AssistantDraftAction(
                    kind = AssistantActionKind.ASSIGN_SHIFT,
                    title = "Назначить смену",
                    description = message.ifBlank {
                        "${formatAssistantDate(date)} · ${shift.workplaceName}: ${shift.displayCode}"
                    },
                    date = date,
                    shift = shift
                )
            }
        }
        "assign_shift_range" -> {
            val start = jsonDate("rangeStart")
            val end = jsonDate("rangeEnd")
            val sequence = shiftSequence.ifEmpty { shift?.let(::listOf).orEmpty() }
            if (start == null || end == null || sequence.isEmpty()) {
                AssistantDraftAction(
                    kind = AssistantActionKind.UNKNOWN,
                    title = "Нужно уточнение",
                    description = message.ifBlank { "ИИ не указал диапазон или смену." },
                    clarificationPrompt = message.ifBlank { "Уточни диапазон или смену." },
                    clarificationType = AssistantClarificationType.GENERIC,
                    canExecute = false
                )
            } else {
                val stepDays = jsonInt("stepDays")?.coerceAtLeast(1) ?: 1
                val dayFilter = root.optString("dayFilter")
                    .takeIf { it.isNotBlank() }
                    ?.let { parseAssistantRangeDayFilter(it.lowercase(Locale.ROOT)) }
                    ?: AssistantRangeDayFilter.ALL
                AssistantDraftAction(
                    kind = AssistantActionKind.ASSIGN_SHIFT_RANGE,
                    title = if (sequence.size > 1) "Назначить последовательность" else "Назначить смену на диапазон",
                    description = message.ifBlank {
                        if (sequence.size > 1) {
                            buildAssistantSequenceRangeDescription(sequence, start, end, stepDays, dayFilter)
                        } else {
                            buildAssistantRangeDescription(sequence.first(), start, end, stepDays, dayFilter)
                        }
                    },
                    rangeStart = start,
                    rangeEnd = end,
                    rangeStepDays = stepDays,
                    rangeDayFilter = dayFilter,
                    shift = sequence.first(),
                    shiftSequence = sequence
                )
            }
        }
        "assign_shift_dates" -> {
            val assignments = root.optJSONArray("assignments")
                ?.let { array ->
                    (0 until array.length()).mapNotNull { index ->
                        val item = array.optJSONObject(index) ?: return@mapNotNull null
                        val date = item.optString("date")
                            .takeIf { it.isNotBlank() }
                            ?.let { value -> runCatching { LocalDate.parse(value) }.getOrNull() }
                            ?: return@mapNotNull null
                        val itemShift = item.optString("shiftCode")
                            .takeIf { it.isNotBlank() }
                            ?.let { findAssistantShiftByCode(it, shifts) }
                            ?: return@mapNotNull null
                        AssistantDatedShiftAssignment(date = date, shift = itemShift)
                    }
                }
                .orEmpty()
                .distinctBy { "${it.date}:${it.shift.code}:${it.shift.workplaceId}" }
                .sortedBy { it.date }
            if (assignments.isEmpty()) {
                AssistantDraftAction(
                    kind = AssistantActionKind.UNKNOWN,
                    title = "Нужно уточнение",
                    description = message.ifBlank { "ИИ не нашёл дат и кодов смен для импорта." },
                    canExecute = false
                )
            } else {
                val preview = assignments.take(6).joinToString("; ") {
                    "${formatAssistantDate(it.date)}: ${it.shift.displayCode}"
                }
                AssistantDraftAction(
                    kind = AssistantActionKind.ASSIGN_SHIFT_DATES,
                    title = "Импортировать график",
                    description = message.ifBlank { "Будет назначено ${assignments.size} смен. $preview" },
                    datedShiftAssignments = assignments
                )
            }
        }
        "create_note" -> {
            val date = jsonDate("date") ?: LocalDate.now()
            val title = root.optString("noteTitle").ifBlank { "Заметка" }
            val body = root.optString("noteBody").ifBlank { message }
            AssistantDraftAction(
                kind = AssistantActionKind.CREATE_NOTE,
                title = "Создать заметку",
                description = message.ifBlank { "${formatAssistantDate(date)} · $title" },
                date = date,
                noteTitle = title,
                noteBody = body
            )
        }
        "configure_alarm" -> {
            if (shift == null) {
                AssistantDraftAction(
                    kind = AssistantActionKind.UNKNOWN,
                    title = "Нужно уточнить смену",
                    description = message.ifBlank { "ИИ не указал смену для будильника." },
                    canExecute = false
                )
            } else {
                AssistantDraftAction(
                    kind = AssistantActionKind.CONFIGURE_SHIFT_ALARM,
                    title = "Добавить будильник смены",
                    description = message.ifBlank { "${shift.workplaceName}: ${shift.displayCode}" },
                    shift = shift,
                    alarmHour = jsonInt("hour")?.coerceIn(0, 23),
                    alarmMinute = jsonInt("minute")?.coerceIn(0, 59),
                    alarmMinutesBefore = jsonInt("minutesBefore")?.coerceIn(0, 24 * 60)
                )
            }
        }
        "configure_alarms" -> {
            val sequence = shiftSequence.ifEmpty {
                shifts.filter { option -> option.totalHours > 0.0 }.take(12)
            }
            if (sequence.isEmpty()) {
                AssistantDraftAction(
                    kind = AssistantActionKind.UNKNOWN,
                    title = "Нужно уточнить смены",
                    description = message.ifBlank { "ИИ не указал смены для будильников." },
                    canExecute = false
                )
            } else {
                AssistantDraftAction(
                    kind = AssistantActionKind.CONFIGURE_SHIFT_ALARMS,
                    title = "Добавить будильники смен",
                    description = message.ifBlank { "Будильники для ${sequence.size} шаблонов смен." },
                    shiftSequence = sequence,
                    alarmHour = jsonInt("hour")?.coerceIn(0, 23),
                    alarmMinute = jsonInt("minute")?.coerceIn(0, 59),
                    alarmMinutesBefore = jsonInt("minutesBefore")?.coerceIn(0, 24 * 60)
                )
            }
        }
        "clear_day" -> {
            val date = jsonDate("date") ?: LocalDate.now()
            AssistantDraftAction(
                kind = AssistantActionKind.CLEAR_DAY,
                title = "Очистить день",
                description = message.ifBlank { "Удалить смены и статусы: ${formatAssistantDate(date)}" },
                date = date
            )
        }
        "open_tab" -> {
            val tab = runCatching { BottomTab.valueOf(root.optString("tab").uppercase(Locale.ROOT)) }.getOrNull()
            AssistantDraftAction(
                kind = if (tab != null) AssistantActionKind.OPEN_TAB else AssistantActionKind.UNKNOWN,
                title = if (tab != null) "Открыть раздел" else "Не понял раздел",
                description = message.ifBlank { tab?.label ?: "Уточни, какой раздел открыть." },
                targetTab = tab,
                canExecute = tab != null
            )
        }
        "answer" -> {
            AssistantDraftAction(
                kind = AssistantActionKind.ANSWER,
                title = "Ответ",
                description = message.ifBlank { root.optString("answerText") },
                answerText = root.optString("answerText").ifBlank { message }
            )
        }
        "update_memory" -> {
            val memoryText = root.optString("memoryText").ifBlank { message }
            val clearMemory = root.optBoolean("clearMemory", false)
            AssistantDraftAction(
                kind = AssistantActionKind.UPDATE_MEMORY,
                title = if (clearMemory) "Очистить память" else "Обновить память",
                description = if (clearMemory) {
                    "Очистить память ассистента."
                } else {
                    "Запомнить: $memoryText"
                },
                memoryText = memoryText,
                clearMemory = clearMemory
            )
        }
        "unknown" -> {
            AssistantDraftAction(
                kind = AssistantActionKind.UNKNOWN,
                title = "Нужно уточнение",
                description = message.ifBlank { "Не понял команду." },
                clarificationPrompt = message.ifBlank { root.optString("question") },
                clarificationType = AssistantClarificationType.GENERIC,
                canExecute = false
            )
        }
        "needs_clarification" -> {
            val question = root.optString("question").ifBlank { message.ifBlank { "Нужно уточнить команду." } }
            AssistantDraftAction(
                kind = AssistantActionKind.UNKNOWN,
                title = "Нужно уточнение",
                description = question,
                clarificationPrompt = question,
                clarificationType = AssistantClarificationType.GENERIC,
                canExecute = false
            )
        }
        else -> null
    }
}

private fun findAssistantShiftByCode(
    shiftCode: String,
    shifts: List<AssistantShiftOption>
): AssistantShiftOption? {
    val normalized = shiftCode.trim().lowercase(Locale.ROOT)
    return shifts.firstOrNull { it.code.lowercase(Locale.ROOT) == normalized }
        ?: shifts.firstOrNull { it.displayCode.lowercase(Locale.ROOT) == normalized }
        ?: shifts.firstOrNull { it.title.lowercase(Locale.ROOT) == normalized }
}

private fun isAssistantConfirmCommand(lowerText: String): Boolean {
    val normalized = lowerText.trim(' ', '.', '!', '?')
    return normalized in setOf(
        "да",
        "ок",
        "окей",
        "ага",
        "подтверждаю",
        "выполни",
        "сделай",
        "применить",
        "применяй"
    )
}

private fun isAssistantCancelCommand(lowerText: String): Boolean {
    val normalized = lowerText.trim(' ', '.', '!', '?')
    return normalized in setOf(
        "нет",
        "не надо",
        "отмена",
        "отмени",
        "стоп",
        "закрой",
        "сбрось"
    )
}

private fun mergeAssistantClarification(
    clarification: AssistantPendingClarification,
    reply: String
): String {
    val cleanedReply = reply.trim()
    return when (clarification.type) {
        AssistantClarificationType.SHIFT -> "${clarification.originalText} $cleanedReply"
        AssistantClarificationType.DATE -> "${clarification.originalText} на $cleanedReply"
        AssistantClarificationType.NOTE_TEXT -> "${clarification.originalText}: $cleanedReply"
        AssistantClarificationType.GENERIC -> "${clarification.originalText}. Уточнение: $cleanedReply"
    }
}

private fun buildAssistantMemoryDraft(
    text: String,
    lower: String
): AssistantDraftAction? {
    if (
        (lower.contains("очист") || lower.contains("забуд")) &&
        (lower.contains("память") || lower.contains("запомн"))
    ) {
        return AssistantDraftAction(
            kind = AssistantActionKind.UPDATE_MEMORY,
            title = "Очистить память",
            description = "Очистить память ассистента.",
            clearMemory = true
        )
    }

    val memoryText = Regex("(?i)(?:запомни|помни|сохрани в память)\\s*[:：-]?\\s*(.+)$")
        .find(text)
        ?.groupValues
        ?.getOrNull(1)
        ?.trim()
        .orEmpty()

    if (memoryText.isNotBlank()) {
        return AssistantDraftAction(
            kind = AssistantActionKind.UPDATE_MEMORY,
            title = "Запомнить правило",
            description = "Запомнить: $memoryText",
            memoryText = memoryText
        )
    }

    return null
}

private fun refineAssistantDraft(
    draft: AssistantDraftAction,
    reply: String,
    shifts: List<AssistantShiftOption>,
    activeWorkplaceId: String,
    assistantMemory: String
): AssistantDraftAction? {
    val lower = reply.lowercase(Locale.ROOT)
    val date = parseAssistantDate(lower)
    val range = parseAssistantDateRange(lower, draft.rangeStart ?: LocalDate.now())
    val shift = findAssistantShift(lower, shifts, activeWorkplaceId, assistantMemory)
    val sequence = findAssistantShiftSequence(lower, shifts, activeWorkplaceId, assistantMemory)
    val time = parseAssistantTime(lower)
    val minutesBefore = parseAssistantMinutesBefore(lower)
    val hasChangeIntent = listOf(
        "измени",
        "поменяй",
        "лучше",
        "не",
        "друга",
        "другой",
        "на ",
        "с ",
        "по ",
        "за "
    ).any { lower.contains(it) }
    if (!hasChangeIntent && date == null && range == null && shift == null && time == null && minutesBefore == null) {
        return null
    }

    return when (draft.kind) {
        AssistantActionKind.ASSIGN_SHIFT -> {
            val updatedShift = shift ?: draft.shift
            val updatedDate = date ?: draft.date
            if (updatedShift == null || updatedDate == null) return null
            draft.copy(
                title = "Назначить смену",
                description = "${formatAssistantDate(updatedDate)} · ${updatedShift.workplaceName}: ${updatedShift.displayCode} · ${updatedShift.title}",
                date = updatedDate,
                shift = updatedShift
            )
        }
        AssistantActionKind.ASSIGN_SHIFT_RANGE -> {
            val updatedStart = range?.first ?: draft.rangeStart
            val updatedEnd = range?.second ?: draft.rangeEnd
            val updatedStep = if (lower.contains("кажд") || lower.contains("через")) {
                parseAssistantRangeStepDays(lower)
            } else {
                draft.rangeStepDays
            }
            val updatedFilter = if (lower.contains("будн") || lower.contains("выход") || lower.contains("рабоч")) {
                parseAssistantRangeDayFilter(lower)
            } else {
                draft.rangeDayFilter
            }
            val updatedSequence = when {
                sequence.size > 1 -> sequence
                shift != null -> listOf(shift)
                else -> draft.shiftSequence.ifEmpty { draft.shift?.let(::listOf).orEmpty() }
            }
            if (updatedStart == null || updatedEnd == null || updatedSequence.isEmpty()) return null
            draft.copy(
                title = if (updatedSequence.size > 1) "Назначить последовательность" else "Назначить смену на диапазон",
                description = if (updatedSequence.size > 1) {
                    buildAssistantSequenceRangeDescription(updatedSequence, updatedStart, updatedEnd, updatedStep, updatedFilter)
                } else {
                    buildAssistantRangeDescription(updatedSequence.first(), updatedStart, updatedEnd, updatedStep, updatedFilter)
                },
                rangeStart = updatedStart,
                rangeEnd = updatedEnd,
                rangeStepDays = updatedStep,
                rangeDayFilter = updatedFilter,
                shift = updatedSequence.first(),
                shiftSequence = updatedSequence
            )
        }
        AssistantActionKind.CONFIGURE_SHIFT_ALARM -> {
            val updatedShift = shift ?: draft.shift
            if (updatedShift == null) return null
            val timingText = when {
                time != null -> "на ${formatAssistantClock(time.first, time.second)}"
                minutesBefore != null -> "за $minutesBefore мин. до начала"
                draft.alarmHour != null && draft.alarmMinute != null -> "на ${formatAssistantClock(draft.alarmHour, draft.alarmMinute)}"
                draft.alarmMinutesBefore != null -> "за ${draft.alarmMinutesBefore} мин. до начала"
                else -> "по умолчанию"
            }
            draft.copy(
                description = "${updatedShift.workplaceName}: ${updatedShift.displayCode} · $timingText",
                shift = updatedShift,
                alarmHour = time?.first ?: draft.alarmHour,
                alarmMinute = time?.second ?: draft.alarmMinute,
                alarmMinutesBefore = minutesBefore ?: draft.alarmMinutesBefore
            )
        }
        AssistantActionKind.CREATE_NOTE -> {
            val body = extractAssistantNoteBody(reply).ifBlank {
                if (lower.contains("текст") || lower.contains("замет")) cleanAssistantNoteText(reply) else ""
            }
            val updatedDate = date ?: draft.date
            val updatedBody = body.ifBlank { draft.noteBody }
            if (updatedDate == null) return null
            draft.copy(
                description = "${formatAssistantDate(updatedDate)} · ${updatedBody.ifBlank { draft.noteTitle }}",
                date = updatedDate,
                noteBody = updatedBody,
                noteTitle = if (body.isNotBlank()) body.take(42) else draft.noteTitle
            )
        }
        AssistantActionKind.CLEAR_DAY -> {
            val updatedDate = date ?: draft.date ?: return null
            draft.copy(
                description = "Удалить смены и статусы: ${formatAssistantDate(updatedDate)}",
                date = updatedDate
            )
        }
        else -> null
    }
}

private fun appendAssistantMemory(
    currentMemory: String,
    newMemory: String
): String {
    val cleanedNewMemory = newMemory.trim()
    if (cleanedNewMemory.isBlank()) return currentMemory
    val lines = currentMemory
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .toMutableList()
    if (lines.none { it.equals(cleanedNewMemory, ignoreCase = true) }) {
        lines += cleanedNewMemory
    }
    return lines.takeLast(16).joinToString("\n")
}

private fun parseAssistantMemoryRules(memory: String): List<AssistantMemoryRule> {
    return memory
        .lineSequence()
        .flatMap { line -> line.split(';').asSequence() }
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .map { raw ->
            val parts = raw.split("=", ":", "—", limit = 2)
                .map { it.trim() }
            if (parts.size == 2 && parts[0].isNotBlank()) {
                AssistantMemoryRule(parts[0], parts[1], raw)
            } else {
                AssistantMemoryRule("Правило", raw, raw)
            }
        }
        .distinctBy { it.raw.lowercase(Locale.ROOT) }
        .take(12)
        .toList()
}

private fun removeAssistantMemoryRule(
    memory: String,
    rawRule: String
): String {
    return memory
        .lineSequence()
        .flatMap { line -> line.split(';').asSequence() }
        .map { it.trim() }
        .filter { it.isNotBlank() && !it.equals(rawRule, ignoreCase = true) }
        .joinToString("; ")
        .take(900)
}

private fun buildAssistantDraftAction(
    rawText: String,
    shifts: List<AssistantShiftOption>,
    scheduledShifts: List<AssistantScheduledShift>,
    activeWorkplaceId: String,
    todaySummary: String,
    tomorrowSummary: String,
    nextAlarmSummary: String,
    upcomingPayments: List<UpcomingPaymentItem>,
    financeContext: AssistantFinanceContext,
    assistantMemory: String = ""
): AssistantDraftAction {
    val text = rawText.trim()
    val lower = text.lowercase(Locale.ROOT)
    if (text.isBlank()) {
        return AssistantDraftAction(
            kind = AssistantActionKind.UNKNOWN,
            title = "Команда пустая",
            description = "Напиши или продиктуй, что нужно сделать.",
            canExecute = false
        )
    }

    val date = parseAssistantDate(lower) ?: LocalDate.now()

    buildAssistantAnswerDraft(
        lowerText = lower,
        activeWorkplaceId = activeWorkplaceId,
        shifts = shifts,
        todaySummary = todaySummary,
        tomorrowSummary = tomorrowSummary,
        nextAlarmSummary = nextAlarmSummary,
        upcomingPayments = upcomingPayments,
        financeContext = financeContext,
        scheduledShifts = scheduledShifts
    )?.let { return it }

    buildAssistantScheduleAnalysisDraft(
        lowerText = lower,
        activeWorkplaceId = activeWorkplaceId,
        scheduledShifts = scheduledShifts
    )?.let { return it }

    buildAssistantDataQualityDraft(
        lowerText = lower,
        activeWorkplaceId = activeWorkplaceId,
        shifts = shifts,
        scheduledShifts = scheduledShifts
    )?.let { return it }

    buildAssistantScheduleImportDraft(
        text = text,
        shifts = shifts,
        activeWorkplaceId = activeWorkplaceId,
        assistantMemory = assistantMemory
    )?.let { return it }

    buildAssistantMemoryDraft(text, lower)?.let { return it }

    if (
        listOf("очист", "сотри", "удали").any { lower.contains(it) } &&
        listOf("день", "дат", "сегодня", "завтра", "календар").any { lower.contains(it) }
    ) {
        return AssistantDraftAction(
            kind = AssistantActionKind.CLEAR_DAY,
            title = "Очистить день",
            description = "Удалить смены и статусы: ${formatAssistantDate(date)}",
            date = date
        )
    }

    if (
        lower.contains("будиль") ||
        (lower.contains("напомни") && lower.contains("смен") && listOf("перед", "до ", "за ").any { lower.contains(it) })
    ) {
        val wantsBulkAlarms = listOf("все смен", "всем смен", "кажд", "шаблон").any { lower.contains(it) } &&
            listOf("будиль", "напомни").any { lower.contains(it) }
        if (wantsBulkAlarms) {
            val activeShifts = shifts
                .filter { it.workplaceId == activeWorkplaceId }
                .ifEmpty { shifts }
                .filter { it.totalHours > 0.0 }
            if (activeShifts.isNotEmpty()) {
                val explicitTime = parseAssistantTime(lower)
                val minutesBefore = parseAssistantMinutesBefore(lower)
                val timingText = when {
                    explicitTime != null -> "на ${formatAssistantClock(explicitTime.first, explicitTime.second)}"
                    minutesBefore != null -> "за $minutesBefore мин. до начала"
                    else -> "по умолчанию"
                }
                return AssistantDraftAction(
                    kind = AssistantActionKind.CONFIGURE_SHIFT_ALARMS,
                    title = "Добавить будильники смен",
                    description = "Для ${activeShifts.size} шаблонов · $timingText",
                    shiftSequence = activeShifts,
                    alarmHour = explicitTime?.first,
                    alarmMinute = explicitTime?.second,
                    alarmMinutesBefore = minutesBefore
                )
            }
        }
        val nextShift = if (lower.contains("следующ") || lower.contains("ближайш")) {
            findNextAnyScheduledAssistantShift(scheduledShifts, activeWorkplaceId)
        } else {
            null
        }
        val shift = nextShift?.shift ?: findAssistantShift(lower, shifts, activeWorkplaceId, assistantMemory)
        if (shift == null) {
            return AssistantDraftAction(
                kind = AssistantActionKind.UNKNOWN,
                title = "Уточни смену",
                description = "Для какой смены поставить будильник?",
                clarificationPrompt = "Для какой смены поставить будильник?",
                clarificationType = AssistantClarificationType.SHIFT,
                canExecute = false
            )
        }
        val explicitTime = parseAssistantTime(lower)
        val minutesBefore = parseAssistantMinutesBefore(lower)
        val timingText = when {
            explicitTime != null -> "на ${formatAssistantClock(explicitTime.first, explicitTime.second)}"
            minutesBefore != null -> "за $minutesBefore мин. до начала"
            else -> "по умолчанию"
        }
        return AssistantDraftAction(
            kind = AssistantActionKind.CONFIGURE_SHIFT_ALARM,
            title = "Добавить будильник смены",
            description = "${shift.workplaceName}: ${shift.displayCode} · $timingText",
            shift = shift,
            alarmHour = explicitTime?.first,
            alarmMinute = explicitTime?.second,
            alarmMinutesBefore = minutesBefore
        )
    }

    if (listOf("замет", "запиши", "напомни", "задач").any { lower.contains(it) }) {
        val wantsNextShift = lower.contains("следующ") && lower.contains("смен") ||
            lower.contains("ближайш") && lower.contains("смен")
        val nextShift = findNextScheduledAssistantShift(
            lower,
            shifts,
            scheduledShifts,
            activeWorkplaceId,
            assistantMemory
        ) ?: if (wantsNextShift) findNextAnyScheduledAssistantShift(scheduledShifts, activeWorkplaceId) else null
        if (wantsNextShift && nextShift == null) {
            val available = shifts.take(6).joinToString(", ") { it.displayCode }
            return AssistantDraftAction(
                kind = AssistantActionKind.UNKNOWN,
                title = "Не нашёл следующую смену",
                description = if (available.isBlank()) {
                    "Для активной работы нет шаблонов смен."
                } else {
                    "Проверь код смены или график. Доступно: $available"
                },
                clarificationPrompt = "Уточни код смены или дату.",
                clarificationType = AssistantClarificationType.SHIFT,
                canExecute = false
            )
        }
        val noteDate = nextShift?.date ?: date
        val body = extractAssistantNoteBody(text).ifBlank { cleanAssistantNoteText(text) }
        if (body.isBlank()) {
            return AssistantDraftAction(
                kind = AssistantActionKind.UNKNOWN,
                title = "Что записать?",
                description = "Какой текст добавить в заметку?",
                clarificationPrompt = "Какой текст добавить в заметку?",
                clarificationType = AssistantClarificationType.NOTE_TEXT,
                canExecute = false
            )
        }
        val shiftContext = nextShift?.let { " · смена ${it.shift.displayCode}" }.orEmpty()
        val noteTitle = when {
            nextShift != null && body.isNotBlank() -> "${nextShift.shift.displayCode}: ${body.take(36)}"
            body.isNotBlank() -> body.take(42)
            else -> "Заметка"
        }
        return AssistantDraftAction(
            kind = AssistantActionKind.CREATE_NOTE,
            title = "Создать заметку",
            description = "${formatAssistantDate(noteDate)}$shiftContext · ${body.ifBlank { "без текста" }}",
            date = noteDate,
            noteTitle = noteTitle,
            noteBody = body.ifBlank { text }
        )
    }

    if (listOf("смен", "поставь", "назначь", "график").any { lower.contains(it) }) {
        val sequence = findAssistantShiftSequence(lower, shifts, activeWorkplaceId, assistantMemory)
        val shift = sequence.firstOrNull()
        if (shift == null) {
            val available = shifts.take(6).joinToString(", ") { it.displayCode }
            return AssistantDraftAction(
                kind = AssistantActionKind.UNKNOWN,
                title = "Не понял смену",
                description = if (available.isBlank()) {
                    "Для активной работы нет шаблонов смен."
                } else {
                    "Укажи код смены. Доступно: $available"
                },
                clarificationPrompt = if (available.isBlank()) "" else "Какую смену назначить?",
                clarificationType = AssistantClarificationType.SHIFT,
                canExecute = false
            )
        }
        val range = parseAssistantDateRange(lower, date)
        if (range != null) {
            val stepDays = parseAssistantRangeStepDays(lower)
            val dayFilter = parseAssistantRangeDayFilter(lower)
            val description = if (sequence.size > 1) {
                buildAssistantSequenceRangeDescription(sequence, range.first, range.second, stepDays, dayFilter)
            } else {
                buildAssistantRangeDescription(shift, range.first, range.second, stepDays, dayFilter)
            }
            return AssistantDraftAction(
                kind = AssistantActionKind.ASSIGN_SHIFT_RANGE,
                title = if (sequence.size > 1) "Назначить последовательность" else "Назначить смену на диапазон",
                description = description,
                rangeStart = range.first,
                rangeEnd = range.second,
                rangeStepDays = stepDays,
                rangeDayFilter = dayFilter,
                shift = shift,
                shiftSequence = sequence
            )
        }
        if (!hasAssistantExplicitDateOrRange(lower)) {
            return AssistantDraftAction(
                kind = AssistantActionKind.UNKNOWN,
                title = "Уточни дату",
                description = "На какую дату назначить ${shift.displayCode}?",
                clarificationPrompt = "На какую дату назначить ${shift.displayCode}?",
                clarificationType = AssistantClarificationType.DATE,
                canExecute = false
            )
        }
        return AssistantDraftAction(
            kind = AssistantActionKind.ASSIGN_SHIFT,
            title = "Назначить смену",
            description = "${formatAssistantDate(date)} · ${shift.workplaceName}: ${shift.displayCode} · ${shift.title}",
            date = date,
            shift = shift
        )
    }

    val targetTab = when {
        lower.contains("будиль") -> BottomTab.ALARMS
        lower.contains("финанс") || lower.contains("расч") || lower.contains("выплат") -> BottomTab.FINANCE
        lower.contains("календар") || lower.contains("график") -> BottomTab.CALENDAR
        lower.contains("замет") -> BottomTab.NOTES
        lower.contains("смен") -> BottomTab.SHIFTS
        lower.contains("настрой") -> BottomTab.SETTINGS
        else -> null
    }
    if (targetTab != null) {
        return AssistantDraftAction(
            kind = AssistantActionKind.OPEN_TAB,
            title = "Открыть раздел",
            description = targetTab.label,
            targetTab = targetTab
        )
    }

    return AssistantDraftAction(
        kind = AssistantActionKind.UNKNOWN,
        title = "Пока не умею это действие",
        description = "Сейчас я понимаю заметки, назначение смен, будильники смен, очистку дня и переходы по разделам.",
        canExecute = false
    )
}

private fun buildAssistantAnswerDraft(
    lowerText: String,
    activeWorkplaceId: String,
    shifts: List<AssistantShiftOption>,
    todaySummary: String,
    tomorrowSummary: String,
    nextAlarmSummary: String,
    upcomingPayments: List<UpcomingPaymentItem>,
    financeContext: AssistantFinanceContext,
    scheduledShifts: List<AssistantScheduledShift>
): AssistantDraftAction? {
    val asksQuestion = lowerText.contains("?") ||
        listOf("что", "когда", "сколько", "какие", "какая", "какой", "покажи", "расскажи").any {
            lowerText.contains(it)
        }
    if (!asksQuestion) return null

    if (lowerText.contains("сегодня")) {
        val today = LocalDate.now()
        val todayShifts = scheduledShifts
            .filter { it.date == today }
            .joinToString("; ") { "${it.shift.workplaceName}: ${it.shift.displayCode}" }
            .ifBlank { "смен не назначено" }
        return AssistantDraftAction(
            kind = AssistantActionKind.ANSWER,
            title = "Сегодня",
            description = todayShifts,
            answerText = "Сегодня: ${todaySummary.ifBlank { todayShifts }}"
        )
    }

    if (lowerText.contains("завтра")) {
        val tomorrow = LocalDate.now().plusDays(1)
        val tomorrowShifts = scheduledShifts
            .filter { it.date == tomorrow }
            .joinToString("; ") { "${it.shift.workplaceName}: ${it.shift.displayCode}" }
            .ifBlank { "смен не назначено" }
        return AssistantDraftAction(
            kind = AssistantActionKind.ANSWER,
            title = "Завтра",
            description = tomorrowShifts,
            answerText = "Завтра (${formatAssistantDate(tomorrow)}): ${tomorrowSummary.ifBlank { tomorrowShifts }}"
        )
    }

    if (lowerText.contains("будиль")) {
        val text = nextAlarmSummary.ifBlank { "Будущих будильников не нашёл." }
        return AssistantDraftAction(
            kind = AssistantActionKind.ANSWER,
            title = "Ближайший будильник",
            description = text,
            answerText = text
        )
    }

    if (listOf("объяс", "поясн", "как счит", "расч").any { lowerText.contains(it) } &&
        listOf("зарплат", "ндфл", "начисл", "на руки", "аванс").any { lowerText.contains(it) }
    ) {
        val answer = buildString {
            appendLine("Период: ${financeContext.periodLabel}")
            appendLine("Начислено до НДФЛ: ${formatMoney(financeContext.grossTotal)}.")
            appendLine("НДФЛ: ${formatMoney(financeContext.ndfl)}.")
            appendLine("На руки: ${formatMoney(financeContext.netTotal)}.")
            appendLine("Аванс к выплате: ${formatMoney(financeContext.netAdvance)}.")
            appendLine("К зарплате: ${formatMoney(financeContext.netSalary)}.")
            append("Подробная расшифровка зависит от режима оплаты, надбавок, удержаний, ночных и больничных. Если хочешь, спроси: “почему аванс отличается от факта”.")
        }.trim()
        return AssistantDraftAction(
            kind = AssistantActionKind.ANSWER,
            title = "Как посчитана зарплата",
            description = answer,
            answerText = answer
        )
    }

    if (lowerText.contains("выплат") || lowerText.contains("зарплат") || lowerText.contains("аванс")) {
        if (listOf("почему", "разниц", "сошл", "сравн", "факт", "пришл", "начисл", "объяс").any { lowerText.contains(it) }) {
            val expectedTotal = financeContext.netAdvance + financeContext.netSalary
            val actualTotal = financeContext.actualAdvance + financeContext.actualSalary
            val hasActual = financeContext.actualAdvance > 0.0 || financeContext.actualSalary > 0.0
            val delta = actualTotal - expectedTotal
            val tolerance = financeContext.paymentDifferenceToleranceRub.coerceAtLeast(0.0)
            val status = if (!hasActual) {
                "Фактические выплаты ещё не внесены."
            } else if (kotlin.math.abs(delta) <= tolerance) {
                "Расхождение в пределах допуска ${formatMoney(tolerance)}."
            } else {
                "Расхождение выше допуска ${formatMoney(tolerance)}."
            }
            val answer = buildString {
                appendLine("Период: ${financeContext.periodLabel}")
                appendLine("Расчёт: аванс ${formatMoney(financeContext.netAdvance)}, зарплата ${formatMoney(financeContext.netSalary)}, итого ${formatMoney(expectedTotal)} на руки.")
                if (hasActual) {
                    appendLine("Факт: аванс ${formatMoney(financeContext.actualAdvance)}, зарплата ${formatMoney(financeContext.actualSalary)}, итого ${formatMoney(actualTotal)}.")
                    appendLine("Разница: ${formatMoney(delta)}. $status")
                } else {
                    appendLine(status)
                }
                append("Если разница большая, чаще всего причина в удержаниях, премиях/доплатах, авансе по банковскому правилу или ручных корректировках бухгалтерии.")
            }.trim()
            return AssistantDraftAction(
                kind = AssistantActionKind.ANSWER,
                title = "План / факт",
                description = answer,
                answerText = answer
            )
        }

        val answer = upcomingPayments
            .take(3)
            .joinToString("\n") { payment ->
                "• ${payment.title}: ${formatAssistantDate(payment.date)}"
            }
            .ifBlank { "Ближайших выплат не нашёл." }
        return AssistantDraftAction(
            kind = AssistantActionKind.ANSWER,
            title = "Ближайшие выплаты",
            description = answer,
            answerText = answer
        )
    }

    if (lowerText.contains("следующ") && lowerText.contains("смен")) {
        val next = scheduledShifts
            .filter { scheduled -> scheduled.date.isAfter(LocalDate.now()) }
            .filter { scheduled -> scheduled.shift.workplaceId == activeWorkplaceId || lowerText.contains("все") }
            .minByOrNull { it.date }
        return AssistantDraftAction(
            kind = AssistantActionKind.ANSWER,
            title = "Следующая смена",
            description = next?.let { "${formatAssistantDate(it.date)} · ${it.shift.displayCode}" } ?: "Будущих смен не нашёл.",
            answerText = next?.let {
                "Следующая смена: ${formatAssistantDate(it.date)} · ${it.shift.workplaceName}: ${it.shift.displayCode} (${it.shift.title})."
            } ?: "Будущих смен не нашёл."
        )
    }

    if (lowerText.contains("шаблон") || lowerText.contains("смены") || lowerText.contains("доступ")) {
        val available = shifts
            .filter { it.workplaceId == activeWorkplaceId }
            .take(8)
            .joinToString("\n") { "• ${it.displayCode} — ${it.title}" }
            .ifBlank { "Для активной работы шаблонов нет." }
        return AssistantDraftAction(
            kind = AssistantActionKind.ANSWER,
            title = "Шаблоны смен",
            description = available,
            answerText = available
        )
    }

    return null
}

private fun buildAssistantScheduleAnalysisDraft(
    lowerText: String,
    activeWorkplaceId: String,
    scheduledShifts: List<AssistantScheduledShift>
): AssistantDraftAction? {
    val wantsAnalysis = listOf("проверь", "анализ", "сводка", "дыры", "пуст", "пересеч", "конфликт").any {
        lowerText.contains(it)
    }
    val mentionsSchedule = listOf("график", "календар", "смен", "месяц", "недел").any {
        lowerText.contains(it)
    }
    if (!wantsAnalysis || !mentionsSchedule) return null

    val today = LocalDate.now()
    val period = when {
        lowerText.contains("недел") -> today to today.plusDays(6)
        lowerText.contains("месяц") || lowerText.contains("календар") -> {
            val month = YearMonth.of(today.year, today.month)
            month.atDay(1) to month.atEndOfMonth()
        }
        else -> today to today.plusDays(13)
    }
    val scoped = scheduledShifts
        .filter { it.date in period.first..period.second }
        .filter { it.shift.workplaceId == activeWorkplaceId || lowerText.contains("все") }
        .sortedBy { it.date }
    val grouped = scoped.groupBy { it.date }
    val totalDays = generateSequence(period.first) { date ->
        date.plusDays(1).takeIf { !it.isAfter(period.second) }
    }.toList()
    val emptyDays = totalDays.filterNot { it in grouped.keys }
    val multiShiftDays = grouped.filterValues { it.size > 1 }
    val nightCount = scoped.count { isAssistantNightShift(it.shift) }
    val workNames = scoped.map { it.shift.workplaceName }.distinct()
    val topShifts = scoped
        .groupingBy { it.shift.displayCode }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .take(4)
        .joinToString(", ") { "${it.key}: ${it.value}" }
        .ifBlank { "нет" }

    val emptyPreview = emptyDays.take(5).joinToString(", ") { formatAssistantDate(it) }
    val multiPreview = multiShiftDays.entries.take(4).joinToString(", ") { (date, shifts) ->
        "${formatAssistantDate(date)} (${shifts.joinToString("+") { it.shift.displayCode }})"
    }
    val answer = buildString {
        appendLine("Период: ${formatAssistantDate(period.first)} - ${formatAssistantDate(period.second)}")
        appendLine("Смен: ${scoped.size}; ночных: $nightCount; работ: ${workNames.size.coerceAtLeast(if (scoped.isEmpty()) 0 else 1)}")
        appendLine("Чаще всего: $topShifts")
        appendLine("Пустых дней: ${emptyDays.size}${if (emptyPreview.isNotBlank()) " ($emptyPreview)" else ""}")
        appendLine("Дней с несколькими сменами: ${multiShiftDays.size}${if (multiPreview.isNotBlank()) " ($multiPreview)" else ""}")
        if (emptyDays.isEmpty() && multiShiftDays.isEmpty()) {
            append("Критичных дыр или пересечений в выбранном периоде не вижу.")
        } else {
            append("Стоит проверить пустые дни и дни с несколькими сменами.")
        }
    }.trim()

    return AssistantDraftAction(
        kind = AssistantActionKind.ANSWER,
        title = "Проверка графика",
        description = answer,
        answerText = answer
    )
}

private fun buildAssistantDataQualityDraft(
    lowerText: String,
    activeWorkplaceId: String,
    shifts: List<AssistantShiftOption>,
    scheduledShifts: List<AssistantScheduledShift>
): AssistantDraftAction? {
    val wantsDataCheck = listOf("качество", "данн", "диагност", "проверь данные", "ошибк").any {
        lowerText.contains(it)
    }
    if (!wantsDataCheck) return null

    val activeShifts = shifts.filter { it.workplaceId == activeWorkplaceId || lowerText.contains("все") }
    val zeroHourShifts = activeShifts.filter { it.totalHours <= 0.0 }
    val missingTimes = activeShifts.filter { it.startHour == null || it.endHour == null }
    val suspiciousNight = activeShifts.filter { isAssistantNightShift(it) && it.nightHours <= 0.0 }
    val upcoming = scheduledShifts.filter { !it.date.isBefore(LocalDate.now()) }
    val multiShiftDays = upcoming.groupBy { it.date }.filterValues { it.size > 1 }

    val issues = mutableListOf<String>()
    if (zeroHourShifts.isNotEmpty()) {
        issues += "Шаблоны без оплачиваемых часов: ${zeroHourShifts.take(5).joinToString(", ") { it.displayCode }}"
    }
    if (missingTimes.isNotEmpty()) {
        issues += "Шаблоны без начала/окончания: ${missingTimes.take(5).joinToString(", ") { it.displayCode }}"
    }
    if (suspiciousNight.isNotEmpty()) {
        issues += "Ночные шаблоны без ночных часов: ${suspiciousNight.take(5).joinToString(", ") { it.displayCode }}"
    }
    if (multiShiftDays.isNotEmpty()) {
        issues += "Дней с несколькими сменами впереди: ${multiShiftDays.size}"
    }

    val answer = if (issues.isEmpty()) {
        "Грубых проблем в шаблонах и ближайшем графике не вижу: часы, времена и будущие назначения выглядят нормально."
    } else {
        buildString {
            appendLine("Нашёл, что стоит проверить:")
            issues.forEach { appendLine("• $it") }
            append("Это не обязательно ошибка, но такие места чаще всего дают расхождения в расчёте и будильниках.")
        }.trim()
    }

    return AssistantDraftAction(
        kind = AssistantActionKind.ANSWER,
        title = "Проверка данных",
        description = answer,
        answerText = answer
    )
}

private fun buildAssistantScheduleImportDraft(
    text: String,
    shifts: List<AssistantShiftOption>,
    activeWorkplaceId: String,
    assistantMemory: String
): AssistantDraftAction? {
    val lower = text.lowercase(Locale.ROOT)
    val wantsImport = listOf("импорт", "расписание", "график списком", "вставь график", "разбери график").any {
        lower.contains(it)
    }
    if (!wantsImport && text.lines().count { parseAssistantImportLine(it, shifts, activeWorkplaceId, assistantMemory) != null } < 2) {
        return null
    }
    val assignments = text
        .lines()
        .mapNotNull { parseAssistantImportLine(it, shifts, activeWorkplaceId, assistantMemory) }
        .distinctBy { "${it.date}:${it.shift.code}:${it.shift.workplaceId}" }
        .sortedBy { it.date }

    if (assignments.isEmpty()) return null
    val preview = assignments.take(6).joinToString("; ") {
        "${formatAssistantDate(it.date)}: ${it.shift.displayCode}"
    }
    return AssistantDraftAction(
        kind = AssistantActionKind.ASSIGN_SHIFT_DATES,
        title = "Импортировать график",
        description = "Будет назначено ${assignments.size} смен. $preview",
        datedShiftAssignments = assignments
    )
}

private fun parseAssistantImportLine(
    line: String,
    shifts: List<AssistantShiftOption>,
    activeWorkplaceId: String,
    assistantMemory: String
): AssistantDatedShiftAssignment? {
    val lower = line.lowercase(Locale.ROOT)
    val date = parseAssistantDate(lower) ?: return null
    val shift = findAssistantShift(lower, shifts, activeWorkplaceId, assistantMemory) ?: return null
    return AssistantDatedShiftAssignment(date = date, shift = shift)
}

private fun extractAssistantNoteBody(text: String): String {
    return Regex("(?i)(заметку|заметка|заметке|запись|задачу)\\s*[:：-]\\s*(.+)$")
        .find(text)
        ?.groupValues
        ?.getOrNull(2)
        ?.trim()
        .orEmpty()
}

private fun cleanAssistantNoteText(text: String): String {
    return text
        .replace(Regex("(?i)^\\s*(добавь|создай|запиши|напомни)\\s+(заметку|заметка|задачу)?\\s*"), "")
        .replace(Regex("(?i)\\bна\\s+следующ\\S*\\s+[\\p{L}\\p{N}_]+\\s+смен\\S*\\s*"), " ")
        .replace(Regex("(?i)\\b(заметку|заметка|заметке|запись|задачу)\\b\\s*:?\\s*"), " ")
        .replace(Regex("(?i)\\s*(на сегодня|сегодня|на завтра|завтра)\\s*:?\\s*"), " ")
        .trim(' ', ':', '-', '—')
}

private fun findNextScheduledAssistantShift(
    lowerText: String,
    shifts: List<AssistantShiftOption>,
    scheduledShifts: List<AssistantScheduledShift>,
    activeWorkplaceId: String,
    assistantMemory: String = ""
): AssistantScheduledShift? {
    val requestedShift = findAssistantShift(lowerText, shifts, activeWorkplaceId, assistantMemory) ?: return null
    val today = LocalDate.now()
    return scheduledShifts
        .asSequence()
        .filter { scheduled ->
            scheduled.date.isAfter(today) &&
                scheduled.shift.code == requestedShift.code
        }
        .minByOrNull { it.date }
}

private fun findNextAnyScheduledAssistantShift(
    scheduledShifts: List<AssistantScheduledShift>,
    activeWorkplaceId: String
): AssistantScheduledShift? {
    val today = LocalDate.now()
    return scheduledShifts
        .asSequence()
        .filter { scheduled -> scheduled.date.isAfter(today) }
        .filter { scheduled -> scheduled.shift.workplaceId == activeWorkplaceId }
        .minByOrNull { it.date }
        ?: scheduledShifts
            .asSequence()
            .filter { scheduled -> scheduled.date.isAfter(today) }
            .minByOrNull { it.date }
}

private fun findAssistantShift(
    lowerText: String,
    shifts: List<AssistantShiftOption>,
    activeWorkplaceId: String,
    assistantMemory: String = ""
): AssistantShiftOption? {
    val tokens = Regex("[\\p{L}\\p{N}_]+").findAll(lowerText).map { it.value }.toSet()
    findAssistantShiftFromMemory(lowerText, shifts, activeWorkplaceId, assistantMemory)?.let { return it }
    val directMatches = shifts.filter { shift ->
        val code = shift.displayCode.lowercase(Locale.ROOT)
        val fullCode = shift.code.lowercase(Locale.ROOT)
        val title = shift.title.lowercase(Locale.ROOT)
        when {
            code.length <= 2 -> code in tokens
            else -> lowerText.contains(code)
        } || lowerText.contains(fullCode) || (title.length > 2 && lowerText.contains(title))
    }
    val directMatch = directMatches
        .maxByOrNull { shift -> assistantWorkplacePreferenceScore(shift, lowerText, activeWorkplaceId) }
    if (directMatch != null) return directMatch

    val wantsNight = listOf("ноч", "ночн", "ночная", "ночную", "нч").any { lowerText.contains(it) }
    val wantsDay = listOf("днев", "дневн", "дневная", "дневную", "день").any { lowerText.contains(it) }
    val requestedHours = parseRequestedShiftHours(lowerText)
    val scored = shifts
        .map { shift ->
            shift to assistantShiftSemanticScore(
                shift = shift,
                lowerText = lowerText,
                wantsDay = wantsDay,
                wantsNight = wantsNight,
                requestedHours = requestedHours,
                activeWorkplaceId = activeWorkplaceId
            )
        }
        .filter { (_, score) -> score > 0 }
        .maxByOrNull { (_, score) -> score }
    return scored?.first
}

private fun findAssistantShiftSequence(
    lowerText: String,
    shifts: List<AssistantShiftOption>,
    activeWorkplaceId: String,
    assistantMemory: String = ""
): List<AssistantShiftOption> {
    val tokens = Regex("[\\p{L}\\p{N}_]+")
        .findAll(lowerText)
        .map { it.value }
        .toList()
    val markerIndex = tokens.indexOfFirst { token ->
        token in setOf("цикл", "последовательность", "график", "чередование")
    }
    val candidateTokens = if (markerIndex >= 0) {
        tokens.drop(markerIndex + 1)
    } else {
        tokens
    }
    val stopWords = setOf(
        "с",
        "по",
        "на",
        "май",
        "мая",
        "июнь",
        "июня",
        "июль",
        "июля",
        "день",
        "дня",
        "дней",
        "каждые",
        "каждый",
        "через"
    )
    val sequence = candidateTokens
        .takeWhile { token ->
            token !in stopWords && token.toIntOrNull() == null
        }
        .mapNotNull { token -> findAssistantShift(token, shifts, activeWorkplaceId, assistantMemory) }
        .distinctBy { it.code }

    return if (sequence.size > 1) {
        sequence
    } else {
        findAssistantShift(lowerText, shifts, activeWorkplaceId, assistantMemory)?.let(::listOf).orEmpty()
    }
}

private fun findAssistantShiftFromMemory(
    lowerText: String,
    shifts: List<AssistantShiftOption>,
    activeWorkplaceId: String,
    assistantMemory: String
): AssistantShiftOption? {
    if (assistantMemory.isBlank()) return null
    val memoryPairs = assistantMemory
        .lineSequence()
        .flatMap { line ->
            line.split(';', ',')
                .asSequence()
                .mapNotNull { chunk ->
                    val parts = chunk.split("=", ":", "—", "-")
                        .map { it.trim().lowercase(Locale.ROOT) }
                        .filter { it.isNotBlank() }
                    if (parts.size >= 2) parts.first() to parts.drop(1).joinToString(" ") else null
                }
        }
        .toList()
    if (memoryPairs.isEmpty()) return null

    val sortedShifts = shifts.sortedByDescending { shift ->
        assistantWorkplacePreferenceScore(shift, lowerText, activeWorkplaceId)
    }
    for ((alias, target) in memoryPairs) {
        val commandMentionsAlias = assistantContainsPhrase(lowerText, alias)
        val commandMentionsTarget = assistantContainsPhrase(lowerText, target)
        for (shift in sortedShifts) {
            val aliasIsShift = assistantMemoryPartMatchesShift(alias, shift)
            val targetIsShift = assistantMemoryPartMatchesShift(target, shift)
            if ((commandMentionsAlias && targetIsShift) || (commandMentionsTarget && aliasIsShift)) {
                return shift
            }
        }
    }
    return null
}

private fun assistantMemoryPartMatchesShift(
    part: String,
    shift: AssistantShiftOption
): Boolean {
    val code = shift.displayCode.lowercase(Locale.ROOT)
    val fullCode = shift.code.lowercase(Locale.ROOT)
    val title = shift.title.lowercase(Locale.ROOT)
    return assistantContainsPhrase(part, code) ||
        assistantContainsPhrase(part, fullCode) ||
        (title.length > 2 && (assistantContainsPhrase(part, title) || assistantContainsPhrase(title, part)))
}

private fun assistantContainsPhrase(
    text: String,
    phrase: String
): Boolean {
    val normalizedPhrase = phrase.trim().lowercase(Locale.ROOT)
    if (normalizedPhrase.isBlank()) return false
    return if (normalizedPhrase.length <= 2) {
        Regex("[\\p{L}\\p{N}_]+")
            .findAll(text)
            .any { it.value == normalizedPhrase }
    } else {
        text.contains(normalizedPhrase)
    }
}

private fun assistantShiftSemanticScore(
    shift: AssistantShiftOption,
    lowerText: String,
    wantsDay: Boolean,
    wantsNight: Boolean,
    requestedHours: Double?,
    activeWorkplaceId: String
): Int {
    var score = 0
    val code = shift.displayCode.lowercase(Locale.ROOT)
    val title = shift.title.lowercase(Locale.ROOT)
    val isNight = isAssistantNightShift(shift)
    if (wantsNight) score += if (isNight) 70 else -40
    if (wantsDay) score += if (!isNight) 42 else -24
    if (requestedHours != null && kotlin.math.abs(shift.totalHours - requestedHours) <= 0.35) score += 35
    if (lowerText.contains("коротк") && shift.totalHours <= 8.5) score += 18
    if (lowerText.contains("длинн") && shift.totalHours >= 10.0) score += 18
    if (wantsNight && (title.contains("ноч") || code.contains("н"))) score += 16
    if (wantsDay && (title.contains("день") || title.contains("днев") || code.contains("д"))) score += 14
    if (requestedHours != null && shift.title.contains(requestedHours.toInt().toString())) score += 10
    score += assistantWorkplacePreferenceScore(shift, lowerText, activeWorkplaceId)
    return score
}

private fun assistantWorkplacePreferenceScore(
    shift: AssistantShiftOption,
    lowerText: String,
    activeWorkplaceId: String
): Int {
    val workplaceName = shift.workplaceName.lowercase(Locale.ROOT)
    val workplaceNumber = Regex("\\d+").find(workplaceName)?.value
    var score = if (shift.workplaceId == activeWorkplaceId) 10 else 0
    if (workplaceName.length > 2 && lowerText.contains(workplaceName)) score += 60
    if (
        workplaceNumber != null &&
        (lowerText.contains("работа $workplaceNumber") ||
            lowerText.contains("работе $workplaceNumber") ||
            lowerText.contains("${workplaceNumber} работа"))
    ) {
        score += 52
    }
    return score
}

private fun isAssistantNightShift(shift: AssistantShiftOption): Boolean {
    val startHour = shift.startHour
    val endHour = shift.endHour
    return shift.nightHours > 0.0 ||
        (startHour != null && endHour != null && endHour < startHour) ||
        shift.title.lowercase(Locale.ROOT).contains("ноч")
}

private fun parseRequestedShiftHours(lowerText: String): Double? {
    return Regex("\\b(\\d{1,2})(?:[,.](\\d{1,2}))?\\s*(?:час|ч)\\b")
        .find(lowerText)
        ?.let { match ->
            val whole = match.groupValues[1].toDoubleOrNull() ?: return@let null
            val fraction = match.groupValues.getOrNull(2)
                ?.takeIf { it.isNotBlank() }
                ?.let { "0.$it".toDoubleOrNull() }
                ?: 0.0
            whole + fraction
        }
}

private fun parseAssistantDateRange(
    lowerText: String,
    fallbackDate: LocalDate
): Pair<LocalDate, LocalDate>? {
    Regex("с\\s+(\\d{1,2})(?:[.\\-/](\\d{1,2}))?\\s+по\\s+(\\d{1,2})(?:[.\\-/](\\d{1,2}))?(?:\\s+([а-яё]+))?")
        .find(lowerText)
        ?.let { match ->
            val startDay = match.groupValues[1].toIntOrNull() ?: return@let null
            val startMonth = match.groupValues[2].toIntOrNull()
            val endDay = match.groupValues[3].toIntOrNull() ?: return@let null
            val endMonth = match.groupValues[4].toIntOrNull()
            val monthWord = match.groupValues.getOrNull(5).orEmpty()
            val resolvedMonth = assistantMonthNumber(monthWord)
            val start = runCatching {
                LocalDate.of(
                    fallbackDate.year,
                    startMonth ?: resolvedMonth ?: fallbackDate.monthValue,
                    startDay
                )
            }.getOrNull() ?: return@let null
            val end = runCatching {
                LocalDate.of(
                    fallbackDate.year,
                    endMonth ?: resolvedMonth ?: start.monthValue,
                    endDay
                )
            }.getOrNull() ?: return@let null
            return if (end.isBefore(start)) start to end.plusMonths(1) else start to end
        }

    Regex("\\b(\\d{1,2})\\s*[-–]\\s*(\\d{1,2})\\s+([а-яё]+)")
        .find(lowerText)
        ?.let { match ->
            val startDay = match.groupValues[1].toIntOrNull() ?: return@let null
            val endDay = match.groupValues[2].toIntOrNull() ?: return@let null
            val month = assistantMonthNumber(match.groupValues[3]) ?: return@let null
            val start = runCatching { LocalDate.of(fallbackDate.year, month, startDay) }.getOrNull()
                ?: return@let null
            val end = runCatching { LocalDate.of(fallbackDate.year, month, endDay) }.getOrNull()
                ?: return@let null
            return if (end.isBefore(start)) start to end.plusMonths(1) else start to end
        }

    if (lowerText.contains("до конца")) {
        val month = assistantMonthNumberFromText(lowerText) ?: fallbackDate.monthValue
        val start = parseAssistantRangeStartDate(lowerText, fallbackDate)
            ?: if (month == fallbackDate.monthValue) fallbackDate else LocalDate.of(fallbackDate.year, month, 1)
        val end = LocalDate.of(start.year, month, 1).withDayOfMonth(
            LocalDate.of(start.year, month, 1).lengthOfMonth()
        )
        return if (end.isBefore(start)) start to end.plusYears(1) else start to end
    }

    Regex("\\b(?:на|в)\\s+([а-яё]+)\\b")
        .find(lowerText)
        ?.let { match ->
            val month = assistantMonthNumber(match.groupValues[1]) ?: return@let null
            val start = LocalDate.of(fallbackDate.year, month, 1)
            val end = start.withDayOfMonth(start.lengthOfMonth())
            return start to end
        }

    return null
}

private fun hasAssistantExplicitDateOrRange(lowerText: String): Boolean {
    return parseAssistantDate(lowerText) != null ||
        parseAssistantDateRange(lowerText, LocalDate.now()) != null ||
        listOf("сегодня", "завтра", "послезавтра", "до конца").any { lowerText.contains(it) }
}

private fun parseAssistantRangeStepDays(lowerText: String): Int {
    if (lowerText.contains("через день") || lowerText.contains("через сутки")) return 2
    Regex("(?:кажд\\S*|через)\\s+(\\d{1,2})\\s*(?:дн|день|дня|дней|сут)")
        .find(lowerText)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()
        ?.let { return it.coerceIn(1, 31) }
    return 1
}

private fun parseAssistantRangeDayFilter(lowerText: String): AssistantRangeDayFilter {
    return when {
        lowerText.contains("weekdays") || lowerText.contains("будн") || lowerText.contains("рабоч") -> AssistantRangeDayFilter.WEEKDAYS
        lowerText.contains("weekends") || lowerText.contains("выходн") || lowerText.contains("суббот") || lowerText.contains("воскрес") -> AssistantRangeDayFilter.WEEKENDS
        else -> AssistantRangeDayFilter.ALL
    }
}

private fun AssistantRangeDayFilter.matches(date: LocalDate): Boolean {
    return when (this) {
        AssistantRangeDayFilter.ALL -> true
        AssistantRangeDayFilter.WEEKDAYS -> date.dayOfWeek !in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        AssistantRangeDayFilter.WEEKENDS -> date.dayOfWeek in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
    }
}

private fun AssistantRangeDayFilter.label(): String {
    return when (this) {
        AssistantRangeDayFilter.ALL -> ""
        AssistantRangeDayFilter.WEEKDAYS -> "по будням"
        AssistantRangeDayFilter.WEEKENDS -> "по выходным"
    }
}

private fun buildAssistantRangeDescription(
    shift: AssistantShiftOption,
    start: LocalDate,
    end: LocalDate,
    stepDays: Int,
    dayFilter: AssistantRangeDayFilter
): String {
    val stepText = when (stepDays) {
        1 -> "каждый день"
        2 -> "через день"
        else -> "каждые $stepDays дн."
    }
    return listOf(
        "${shift.displayCode}: ${formatAssistantDate(start)} - ${formatAssistantDate(end)}",
        stepText,
        dayFilter.label()
    ).filter { it.isNotBlank() }.joinToString(" · ")
}

private fun buildAssistantSequenceRangeDescription(
    sequence: List<AssistantShiftOption>,
    start: LocalDate,
    end: LocalDate,
    stepDays: Int,
    dayFilter: AssistantRangeDayFilter
): String {
    val stepText = when (stepDays) {
        1 -> "каждый день"
        2 -> "через день"
        else -> "каждые $stepDays дн."
    }
    return listOf(
        "${sequence.joinToString(" / ") { it.displayCode }}: ${formatAssistantDate(start)} - ${formatAssistantDate(end)}",
        stepText,
        dayFilter.label()
    ).filter { it.isNotBlank() }.joinToString(" · ")
}

private fun parseAssistantRangeStartDate(
    lowerText: String,
    fallbackDate: LocalDate
): LocalDate? {
    Regex("\\bс\\s+(\\d{1,2})(?:[.\\-/](\\d{1,2}))?(?:\\s+([а-яё]+))?")
        .find(lowerText)
        ?.let { match ->
            val day = match.groupValues[1].toIntOrNull() ?: return@let null
            val numericMonth = match.groupValues[2].toIntOrNull()
            val wordMonth = assistantMonthNumber(match.groupValues.getOrNull(3).orEmpty())
            val month = numericMonth ?: wordMonth ?: fallbackDate.monthValue
            return runCatching { LocalDate.of(fallbackDate.year, month, day) }.getOrNull()
        }
    return null
}

private fun assistantMonthNumberFromText(lowerText: String): Int? {
    return Regex("\\b(январ\\S*|феврал\\S*|март\\S*|апрел\\S*|ма[йяе]?|июн\\S*|июл\\S*|август\\S*|сентябр\\S*|октябр\\S*|ноябр\\S*|декабр\\S*)\\b")
        .find(lowerText)
        ?.groupValues
        ?.getOrNull(1)
        ?.let(::assistantMonthNumber)
}

private fun parseAssistantDate(lowerText: String): LocalDate? {
    val today = LocalDate.now()
    if (lowerText.contains("послезавтра")) return today.plusDays(2)
    if (lowerText.contains("завтра")) return today.plusDays(1)
    if (lowerText.contains("сегодня")) return today

    Regex("(\\d{1,2})[.\\-/](\\d{1,2})(?:[.\\-/](\\d{2,4}))?")
        .find(lowerText)
        ?.let { match ->
            val day = match.groupValues[1].toIntOrNull() ?: return@let null
            val month = match.groupValues[2].toIntOrNull() ?: return@let null
            val rawYear = match.groupValues.getOrNull(3).orEmpty()
            val year = when {
                rawYear.isBlank() -> today.year
                rawYear.length == 2 -> 2000 + rawYear.toInt()
                else -> rawYear.toIntOrNull() ?: today.year
            }
            return runCatching { LocalDate.of(year, month, day) }.getOrNull()
        }

    Regex("(\\d{1,2})\\s+([а-яё]+)(?:\\s+(\\d{4}))?")
        .find(lowerText)
        ?.let { match ->
            val day = match.groupValues[1].toIntOrNull() ?: return@let null
            val monthWord = match.groupValues[2]
            val month = assistantMonthNumber(monthWord) ?: return@let null
            val year = match.groupValues.getOrNull(3)?.toIntOrNull() ?: today.year
            return runCatching { LocalDate.of(year, month, day) }.getOrNull()
        }
    return null
}

private fun assistantMonthNumber(monthWord: String): Int? {
    val normalized = monthWord.lowercase(Locale.ROOT)
    val monthMap = mapOf(
        "январ" to 1,
        "феврал" to 2,
        "март" to 3,
        "апрел" to 4,
        "ма" to 5,
        "июн" to 6,
        "июл" to 7,
        "август" to 8,
        "сентябр" to 9,
        "октябр" to 10,
        "ноябр" to 11,
        "декабр" to 12
    )
    return monthMap.entries.firstOrNull { normalized.startsWith(it.key) }?.value
}

private fun parseAssistantTime(lowerText: String): Pair<Int, Int>? {
    val explicitWithPrefix = Regex("(?:\\bв|\\bна)\\s*([01]?\\d|2[0-3])\\s*[:.]\\s*([0-5]\\d)")
        .find(lowerText)
    val plainTime = explicitWithPrefix ?: Regex("\\b([01]?\\d|2[0-3])\\s*[:.]\\s*([0-5]\\d)\\b")
        .find(lowerText)
    return plainTime?.let { match ->
        val hour = match.groupValues[1].toIntOrNull() ?: return@let null
        val minute = match.groupValues[2].toIntOrNull() ?: return@let null
        hour to minute
    }
}

private fun parseAssistantMinutesBefore(lowerText: String): Int? {
    if (lowerText.contains("полтора час")) return 90
    if (lowerText.contains("за час")) return 60
    Regex("за\\s+(\\d{1,3})\\s*(мин|м\\b)")
        .find(lowerText)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()
        ?.let { return it.coerceIn(0, 24 * 60) }
    Regex("за\\s+(\\d{1,2})\\s*(час|ч\\b)")
        .find(lowerText)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()
        ?.let { return (it * 60).coerceIn(0, 24 * 60) }
    return null
}

private const val MAX_ASSISTANT_IMAGE_BYTES = 5 * 1024 * 1024

private fun readAssistantImageAttachment(
    context: Context,
    uri: Uri,
    fallbackName: String
): AssistantImageAttachment {
    val resolver = context.contentResolver
    val mimeType = resolver.getType(uri)
        ?.takeIf { it.startsWith("image/") }
        ?: "image/jpeg"
    if (!mimeType.startsWith("image/")) {
        error("Можно прикреплять только изображения")
    }

    val bytes = resolver.openInputStream(uri)?.use { stream ->
        stream.readBytes()
    } ?: error("Не удалось открыть изображение")
    if (bytes.isEmpty()) {
        error("Изображение пустое")
    }
    if (bytes.size > MAX_ASSISTANT_IMAGE_BYTES) {
        error("Изображение больше 5 МБ. Лучше отправить сжатый скриншот.")
    }

    return AssistantImageAttachment(
        mimeType = mimeType,
        base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP),
        displayName = fallbackName,
        byteSize = bytes.size
    )
}

private fun formatAssistantClock(hour: Int, minute: Int): String {
    return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}

private fun formatAssistantDate(date: LocalDate): String {
    return "${date.dayOfMonth.toString().padStart(2, '0')}." +
        "${date.monthValue.toString().padStart(2, '0')}.${date.year}"
}
