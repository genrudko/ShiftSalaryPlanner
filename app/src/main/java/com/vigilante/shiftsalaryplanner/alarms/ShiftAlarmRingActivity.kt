package com.vigilante.shiftsalaryplanner

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlarmOff
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowRight
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vigilante.shiftsalaryplanner.settings.ShiftAlarmStore
import com.vigilante.shiftsalaryplanner.ui.theme.AppearanceSettings
import com.vigilante.shiftsalaryplanner.ui.theme.ShiftSalaryPlannerTheme
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

private val alarmTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val russianLocale: Locale = Locale.forLanguageTag("ru")
private val alarmDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", russianLocale)

class ShiftAlarmRingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
            }
        }

        val alarmKey = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_ALARM_KEY)
            .orEmpty()
            .ifBlank { "shift_alarm" }
        val title = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_TITLE) ?: "Скоро смена"
        val text = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_TEXT) ?: "Проверь календарь смен"
        val volumePercent = intent.getIntExtra(ShiftAlarmScheduler.EXTRA_VOLUME_PERCENT, 100).coerceIn(0, 100)
        val soundUri = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_SOUND_URI)
        val soundLabel = intent.getStringExtra(ShiftAlarmScheduler.EXTRA_SOUND_LABEL).orEmpty()

        setContent {
            val alarmStore = remember { ShiftAlarmStore(this@ShiftAlarmRingActivity) }
            val alarmSettings by alarmStore.settingsFlow.collectAsState(initial = ShiftAlarmSettings())
            ShiftSalaryPlannerTheme(appearanceSettings = AppearanceSettings()) {
                ShiftAlarmRingScreen(
                    title = title,
                    text = text,
                    volumePercent = volumePercent,
                    soundLabel = soundLabel,
                    ringUi = alarmSettings.ringUi,
                    onSnooze = {
                        ShiftAlarmPlaybackService.snooze(
                            context = this,
                            alarmKey = alarmKey,
                            title = title,
                            text = text,
                            volumePercent = volumePercent,
                            soundUri = soundUri,
                            soundLabel = soundLabel
                        )
                        finish()
                    },
                    onDismiss = {
                        ShiftAlarmPlaybackService.stop(this, alarmKey)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
private fun ShiftAlarmRingScreen(
    title: String,
    text: String,
    volumePercent: Int,
    soundLabel: String,
    ringUi: ShiftAlarmRingUiSettings,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit
) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1_000L)
        }
    }

    val animationMode = ringUi.animationMode
    val animationsEnabled = animationMode != ShiftAlarmRingAnimationMode.OFF
    val vivid = animationMode == ShiftAlarmRingAnimationMode.VIVID
    val pulseEnabled = animationsEnabled && ringUi.pulseAccent && ringUi.visualStyle == ShiftAlarmRingVisualStyle.MODERN
    val gradientEnabled = animationsEnabled && ringUi.animatedGradient
    val accent = MaterialTheme.colorScheme.primary
    val isMinimal = ringUi.visualStyle == ShiftAlarmRingVisualStyle.MINIMAL

    val transition = rememberInfiniteTransition(label = "alarmTransition")
    val pulseScale by transition.animateFloat(
        initialValue = if (pulseEnabled) 0.92f else 1f,
        targetValue = if (pulseEnabled) (if (vivid) 1.14f else 1.08f) else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (vivid) 1250 else 1700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by transition.animateFloat(
        initialValue = if (pulseEnabled) 0.12f else 0.10f,
        targetValue = if (pulseEnabled) (if (vivid) 0.34f else 0.24f) else 0.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (vivid) 1250 else 1700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val gradientShift by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (gradientEnabled) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (vivid) 3600 else 5600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientShift"
    )
    val orbitAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (animationsEnabled) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (vivid) 4500 else 7200),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitAngle"
    )
    val waveShift by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (animationsEnabled) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (vivid) 1800 else 2600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveShift"
    )

    val topColor = lerp(Color(0xFF101521), accent.copy(alpha = if (vivid) 0.40f else 0.24f), gradientShift * 0.30f)
    val middleColor = lerp(Color(0xFF090C14), accent.copy(alpha = if (vivid) 0.22f else 0.12f), gradientShift * 0.20f)
    val bottomColor = lerp(Color(0xFF05060A), Color(0xFF000000), if (isMinimal) 0.52f else 0.20f)
    val dateLabel = remember(now) {
        now.format(alarmDateFormatter).replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(russianLocale) else char.toString()
        }
    }
    val resolvedSoundLabel = if (soundLabel.isNotBlank()) soundLabel else "Системная"

    val topWeight = if (ringUi.clockAlignment == ShiftAlarmRingClockAlignment.CENTER) 0.38f else 0f
    val bottomWeight = if (ringUi.clockAlignment == ShiftAlarmRingClockAlignment.CENTER) 0.62f else 1f
    val clockStyle = MaterialTheme.typography.displayLarge.copy(
        fontSize = (84f * ringUi.clockScale.coerceIn(0.8f, 1.4f)).sp,
        lineHeight = (88f * ringUi.clockScale.coerceIn(0.8f, 1.4f)).sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-1.5f).sp,
        fontFamily = if (ringUi.useMonospaceClock) FontFamily.Monospace else FontFamily.Default
    )
    val titleFontScale = ringUi.textScale.coerceIn(0.85f, 1.35f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(topColor, middleColor, bottomColor)
                )
            )
    ) {
        RenderAlarmBackgroundDecor(
            style = ringUi.animationStyle,
            accent = accent,
            pulseScale = pulseScale,
            pulseAlpha = pulseAlpha,
            orbitAngle = orbitAngle,
            waveShift = waveShift,
            animationsEnabled = animationsEnabled
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Будильник смены",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.78f),
                letterSpacing = 0.5.sp
            )

            if (topWeight > 0f) {
                Spacer(modifier = Modifier.weight(topWeight))
            } else {
                Spacer(modifier = Modifier.height(14.dp))
            }

            if (ringUi.showCurrentClock || ringUi.showDate) {
                AlarmClockBlock(
                    now = now,
                    showCurrentClock = ringUi.showCurrentClock,
                    showDate = ringUi.showDate,
                    dateLabel = dateLabel,
                    clockStyle = clockStyle
                )
            }

            Spacer(modifier = Modifier.weight(bottomWeight))

            if (ringUi.showMetaInfo) {
                AlarmMetaInfo(
                    showVolume = ringUi.showVolumeInfo,
                    showSound = ringUi.showSoundLabel,
                    showTimezone = ringUi.showTimezoneInfo,
                    volumePercent = volumePercent,
                    soundLabel = resolvedSoundLabel
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(if (isMinimal) 24.dp else 30.dp),
                color = Color.White.copy(alpha = if (isMinimal) 0.08f else 0.10f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = if (isMinimal) 0.10f else 0.16f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = (30f * titleFontScale).sp,
                            lineHeight = (34f * titleFontScale).sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = (18f * titleFontScale.coerceIn(0.85f, 1.2f)).sp,
                            lineHeight = (24f * titleFontScale.coerceIn(0.85f, 1.2f)).sp
                        ),
                        color = Color.White.copy(alpha = 0.82f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AlarmActionsSection(
                ringUi = ringUi,
                minimal = isMinimal,
                onSnooze = onSnooze,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
private fun AlarmClockBlock(
    now: LocalDateTime,
    showCurrentClock: Boolean,
    showDate: Boolean,
    dateLabel: String,
    clockStyle: androidx.compose.ui.text.TextStyle
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (showCurrentClock) {
            Text(
                text = now.format(alarmTimeFormatter),
                style = clockStyle,
                color = Color.White
            )
        }
        if (showDate) {
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.84f)
            )
        }
    }
}

@Composable
private fun AlarmMetaInfo(
    showVolume: Boolean,
    showSound: Boolean,
    showTimezone: Boolean,
    volumePercent: Int,
    soundLabel: String
) {
    val items = remember(showVolume, showSound, showTimezone, volumePercent, soundLabel) {
        buildList {
            if (showVolume) add("Громкость $volumePercent%")
            if (showSound) add("Мелодия: $soundLabel")
            if (showTimezone) add("Часовой пояс: ${ZoneId.systemDefault().id}")
        }
    }
    if (items.isEmpty()) return

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items.forEach { item ->
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = Color.White.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
            ) {
                Text(
                    text = item,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.86f)
                )
            }
        }
    }
}

@Composable
private fun RenderAlarmBackgroundDecor(
    style: ShiftAlarmRingAnimationStyle,
    accent: Color,
    pulseScale: Float,
    pulseAlpha: Float,
    orbitAngle: Float,
    waveShift: Float,
    animationsEnabled: Boolean
) {
    when (style) {
        ShiftAlarmRingAnimationStyle.AURORA -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                    }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                accent.copy(alpha = pulseAlpha),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        ShiftAlarmRingAnimationStyle.ORBIT -> {
            val radius = 120.dp
            val offsetX = (kotlin.math.cos(Math.toRadians(orbitAngle.toDouble())) * 120f).toFloat()
            val offsetY = (kotlin.math.sin(Math.toRadians(orbitAngle.toDouble())) * 120f).toFloat()
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(radius)
                        .graphicsLayer {
                            translationX = offsetX
                            translationY = offsetY
                        }
                        .clip(CircleShape)
                        .background(accent.copy(alpha = if (animationsEnabled) 0.20f else 0.08f))
                )
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .graphicsLayer {
                            translationX = -offsetX * 0.75f
                            translationY = -offsetY * 0.75f
                        }
                        .clip(CircleShape)
                        .background(accent.copy(alpha = if (animationsEnabled) 0.16f else 0.06f))
                )
            }
        }

        ShiftAlarmRingAnimationStyle.WAVE -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 90.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                repeat(3) { index ->
                    val localAlpha = (0.10f + index * 0.05f + waveShift * 0.08f).coerceIn(0.08f, 0.34f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.82f - index * 0.10f)
                            .height((12 + index * 4).dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(accent.copy(alpha = if (animationsEnabled) localAlpha else 0.08f))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun AlarmActionsSection(
    ringUi: ShiftAlarmRingUiSettings,
    minimal: Boolean,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit
) {
    when (ringUi.actionStyle) {
        ShiftAlarmRingActionStyle.BUTTONS -> {
            if (ringUi.buttonsLayout == ShiftAlarmRingButtonsLayout.HORIZONTAL) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AlarmActionButton(
                        title = "Отложить",
                        subtitle = "10 минут",
                        primary = false,
                        minimal = minimal,
                        onClick = onSnooze,
                        modifier = Modifier.weight(1f)
                    )
                    AlarmActionButton(
                        title = "Выключить",
                        subtitle = "Остановить",
                        primary = true,
                        minimal = minimal,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AlarmActionButton(
                        title = "Выключить",
                        subtitle = "Остановить",
                        primary = true,
                        minimal = minimal,
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    )
                    AlarmActionButton(
                        title = "Отложить",
                        subtitle = "10 минут",
                        primary = false,
                        minimal = minimal,
                        onClick = onSnooze,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        ShiftAlarmRingActionStyle.SLIDER -> {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AlarmSlideAction(
                    title = "Свайпни вправо, чтобы выключить",
                    onComplete = onDismiss
                )
                AlarmActionChip(
                    title = "Отложить на 10 минут",
                    icon = Icons.Rounded.Snooze,
                    onClick = onSnooze,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        ShiftAlarmRingActionStyle.CHIPS -> {
            if (ringUi.buttonsLayout == ShiftAlarmRingButtonsLayout.HORIZONTAL) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AlarmActionChip(
                        title = "Отложить",
                        icon = Icons.Rounded.Schedule,
                        onClick = onSnooze,
                        modifier = Modifier.weight(1f)
                    )
                    AlarmActionChip(
                        title = "Выключить",
                        icon = Icons.Rounded.AlarmOff,
                        emphasized = true,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AlarmActionChip(
                        title = "Выключить",
                        icon = Icons.Rounded.AlarmOff,
                        emphasized = true,
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    )
                    AlarmActionChip(
                        title = "Отложить",
                        icon = Icons.Rounded.Schedule,
                        onClick = onSnooze,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun AlarmSlideAction(
    title: String,
    onComplete: () -> Unit
) {
    var offsetPx by remember { mutableFloatStateOf(0f) }
    var maxOffsetPx by remember { mutableFloatStateOf(0f) }
    var fired by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.11f))
            .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(22.dp))
            .padding(4.dp)
    ) {
        val knobSize = 54.dp
        maxOffsetPx = with(density) { (maxWidth - knobSize - 8.dp).toPx().coerceAtLeast(1f) }
        val triggerPx = maxOffsetPx * 0.74f
        val clampedOffset = offsetPx.coerceIn(0f, maxOffsetPx)
        if (offsetPx != clampedOffset) {
            offsetPx = clampedOffset
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.88f)
            )
        }

        Surface(
            modifier = Modifier
                .offset { IntOffset(clampedOffset.roundToInt(), 0) }
                .size(knobSize)
                .draggable(
                    state = rememberDraggableState { delta ->
                        if (!fired) {
                            offsetPx = (offsetPx + delta).coerceIn(0f, maxOffsetPx)
                        }
                    },
                    orientation = Orientation.Horizontal,
                    onDragStopped = {
                        if (!fired && offsetPx >= triggerPx) {
                            fired = true
                            onComplete()
                        } else {
                            offsetPx = 0f
                        }
                    }
                ),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardDoubleArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun AlarmActionChip(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    emphasized: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = if (emphasized) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.94f)
    } else {
        Color.White.copy(alpha = 0.10f)
    }
    val border = if (emphasized) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.96f)
    } else {
        Color.White.copy(alpha = 0.20f)
    }
    val color = if (emphasized) MaterialTheme.colorScheme.onPrimary else Color.White

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = background,
        border = BorderStroke(1.dp, border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = appHapticAction(onAction = onClick))
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun AlarmActionButton(
    title: String,
    subtitle: String,
    primary: Boolean,
    minimal: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(if (minimal) 16.dp else 22.dp)
    val container = if (primary) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.95f)
    } else {
        Color.White.copy(alpha = 0.10f)
    }
    val borderColor = if (primary) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.92f)
    } else {
        Color.White.copy(alpha = if (minimal) 0.14f else 0.22f)
    }
    val titleColor = if (primary) MaterialTheme.colorScheme.onPrimary else Color.White
    val subtitleColor = if (primary) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.86f)
    } else {
        Color.White.copy(alpha = 0.76f)
    }

    Column(
        modifier = modifier
            .clip(shape)
            .background(container)
            .border(1.dp, borderColor, shape)
            .clickable(
                onClick = appHapticAction(
                    kind = if (primary) AppHapticKind.CONFIRM else AppHapticKind.SOFT,
                    onAction = onClick
                )
            )
            .padding(horizontal = 10.dp, vertical = 12.dp)
            .appLargeButtonSizing(base = 58.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = titleColor
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = subtitleColor
        )
    }
}
