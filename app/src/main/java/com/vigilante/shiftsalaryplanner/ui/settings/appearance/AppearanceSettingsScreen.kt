package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.MoreHoriz
import com.vigilante.shiftsalaryplanner.ui.theme.AnimationSpeedMode
import com.vigilante.shiftsalaryplanner.ui.theme.AppColorSchemeMode
import com.vigilante.shiftsalaryplanner.ui.theme.AppFontMode
import com.vigilante.shiftsalaryplanner.ui.theme.AppearanceSettings
import com.vigilante.shiftsalaryplanner.ui.theme.CornerStyleMode
import com.vigilante.shiftsalaryplanner.ui.theme.CurrencySymbolMode
import com.vigilante.shiftsalaryplanner.ui.theme.ThemeMode
import com.vigilante.shiftsalaryplanner.ui.theme.UiContrastMode
import com.vigilante.shiftsalaryplanner.ui.theme.UiDensityMode
import com.vigilante.shiftsalaryplanner.ui.theme.sanitizeHexColor
import kotlin.math.roundToInt

private enum class CustomColorSlot {
    PRIMARY,
    SECONDARY,
    TERTIARY,
    BACKGROUND,
    BUBBLE
}

@Composable
fun AppearanceSettingsScreen(
    settings: AppearanceSettings,
    onBack: () -> Unit,
    onChange: (AppearanceSettings) -> Unit,
    onPickCustomFont: () -> Unit = {},
    onClearCustomFont: () -> Unit = {},
    customFontStatusMessage: String? = null
) {
    var pickerSlot by remember { mutableStateOf<CustomColorSlot?>(null) }
    var fontScaleDraft by remember(settings.fontScale) { mutableFloatStateOf(settings.fontScale) }

    fun update(mutator: (AppearanceSettings) -> AppearanceSettings) {
        onChange(mutator(settings))
    }

    fun colorForSlot(slot: CustomColorSlot): Color {
        return when (slot) {
            CustomColorSlot.PRIMARY -> parseHexColor(settings.customPrimaryHex, Color(0xFF0D665A))
            CustomColorSlot.SECONDARY -> parseHexColor(settings.customSecondaryHex, Color(0xFF3F6371))
            CustomColorSlot.TERTIARY -> parseHexColor(settings.customTertiaryHex, Color(0xFF5A5C7E))
            CustomColorSlot.BACKGROUND -> parseHexColor(settings.customBackgroundHex, Color(0xFFF4F8F7))
            CustomColorSlot.BUBBLE -> parseHexColor(
                settings.customBubbleHex,
                Color(0xFFE3E8EF)
            )
        }
    }

    fun saveSlotColor(slot: CustomColorSlot, color: Color) {
        val hex = colorToHex(color)
        update {
            when (slot) {
                CustomColorSlot.PRIMARY -> it.copy(
                    colorSchemeMode = AppColorSchemeMode.CUSTOM,
                    customPrimaryHex = hex
                )

                CustomColorSlot.SECONDARY -> it.copy(
                    colorSchemeMode = AppColorSchemeMode.CUSTOM,
                    customSecondaryHex = hex
                )

                CustomColorSlot.TERTIARY -> it.copy(
                    colorSchemeMode = AppColorSchemeMode.CUSTOM,
                    customTertiaryHex = hex
                )

                CustomColorSlot.BACKGROUND -> it.copy(
                    colorSchemeMode = AppColorSchemeMode.CUSTOM,
                    customBackgroundHex = hex
                )

                CustomColorSlot.BUBBLE -> it.copy(
                    customBubbleHex = hex
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AppScreenHeader(
            title = "Внешний вид",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(appScaledSpacing(14.dp))
        ) {
            AppearanceSectionTitle("Live-preview")
            Spacer(modifier = Modifier.height(appScaledSpacing(6.dp)))
            AppearanceLivePreview(settings = settings)

            Spacer(modifier = Modifier.height(appScaledSpacing(10.dp)))

            AppearanceSectionTitle("Тема")
            Spacer(modifier = Modifier.height(appScaledSpacing(6.dp)))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(appCornerRadius(18.dp)),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, appPanelBorderColor())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(appScaledSpacing(10.dp))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))
                    ) {
                        AppearanceModeCard(
                            title = "Светлая",
                            selected = settings.themeMode == ThemeMode.LIGHT,
                            onClick = { update { it.copy(themeMode = ThemeMode.LIGHT) } },
                            modifier = Modifier.weight(1f)
                        )
                        AppearanceModeCard(
                            title = "Тёмная",
                            selected = settings.themeMode == ThemeMode.DARK,
                            onClick = { update { it.copy(themeMode = ThemeMode.DARK) } },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(appScaledSpacing(6.dp)))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))
                    ) {
                        AppearanceModeCard(
                            title = "Авто",
                            selected = settings.themeMode == ThemeMode.AUTO,
                            onClick = { update { it.copy(themeMode = ThemeMode.AUTO) } },
                            modifier = Modifier.weight(1f)
                        )
                        AppearanceModeCard(
                            title = "По расписанию",
                            selected = settings.themeMode == ThemeMode.SCHEDULE,
                            onClick = { update { it.copy(themeMode = ThemeMode.SCHEDULE) } },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (settings.themeMode == ThemeMode.SCHEDULE) {
                        Spacer(modifier = Modifier.height(appScaledSpacing(10.dp)))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(10.dp)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CompactTimeEditor(
                                label = "Тёмная с",
                                hour = settings.scheduledDarkStartHour,
                                minute = settings.scheduledDarkStartMinute,
                                onHourChange = { hour -> update { it.copy(scheduledDarkStartHour = hour) } },
                                onMinuteChange = { minute -> update { it.copy(scheduledDarkStartMinute = minute) } },
                                modifier = Modifier.weight(1f)
                            )
                            CompactTimeEditor(
                                label = "Светлая с",
                                hour = settings.scheduledDarkEndHour,
                                minute = settings.scheduledDarkEndMinute,
                                onHourChange = { hour -> update { it.copy(scheduledDarkEndHour = hour) } },
                                onMinuteChange = { minute -> update { it.copy(scheduledDarkEndMinute = minute) } },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(appScaledSpacing(10.dp)))

            AppearanceSectionTitle("Цветовая схема")
            Spacer(modifier = Modifier.height(appScaledSpacing(6.dp)))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(appCornerRadius(18.dp)),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, appPanelBorderColor())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(appScaledSpacing(10.dp))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))
                    ) {
                        AppearanceModeCard(
                            title = "Mint",
                            selected = settings.colorSchemeMode == AppColorSchemeMode.MINT,
                            onClick = { update { it.copy(colorSchemeMode = AppColorSchemeMode.MINT) } },
                            modifier = Modifier.weight(1f)
                        )
                        AppearanceModeCard(
                            title = "Ocean",
                            selected = settings.colorSchemeMode == AppColorSchemeMode.OCEAN,
                            onClick = { update { it.copy(colorSchemeMode = AppColorSchemeMode.OCEAN) } },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(appScaledSpacing(6.dp)))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))
                    ) {
                        AppearanceModeCard(
                            title = "Sunset",
                            selected = settings.colorSchemeMode == AppColorSchemeMode.SUNSET,
                            onClick = { update { it.copy(colorSchemeMode = AppColorSchemeMode.SUNSET) } },
                            modifier = Modifier.weight(1f)
                        )
                        AppearanceModeCard(
                            title = "Graphite",
                            selected = settings.colorSchemeMode == AppColorSchemeMode.GRAPHITE,
                            onClick = { update { it.copy(colorSchemeMode = AppColorSchemeMode.GRAPHITE) } },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(appScaledSpacing(6.dp)))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))
                    ) {
                        AppearanceModeCard(
                            title = "Своя",
                            selected = settings.colorSchemeMode == AppColorSchemeMode.CUSTOM,
                            onClick = { update { it.copy(colorSchemeMode = AppColorSchemeMode.CUSTOM) } },
                            modifier = Modifier.weight(1f)
                        )
                        AppearanceModeCard(
                            title = "Material You",
                            selected = settings.colorSchemeMode == AppColorSchemeMode.DYNAMIC,
                            onClick = { update { it.copy(colorSchemeMode = AppColorSchemeMode.DYNAMIC) } },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(appScaledSpacing(8.dp)))
                    Text(
                        text = "Material You доступен на Android 12+",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(appScaledSpacing(10.dp)))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))
                    ) {
                        CustomColorPickerTile(
                            label = "Основной",
                            color = colorForSlot(CustomColorSlot.PRIMARY),
                            onPick = { pickerSlot = CustomColorSlot.PRIMARY },
                            modifier = Modifier.weight(1f)
                        )
                        CustomColorPickerTile(
                            label = "Дополнительный",
                            color = colorForSlot(CustomColorSlot.SECONDARY),
                            onPick = { pickerSlot = CustomColorSlot.SECONDARY },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(appScaledSpacing(6.dp)))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))
                    ) {
                        CustomColorPickerTile(
                            label = "Акцент",
                            color = colorForSlot(CustomColorSlot.TERTIARY),
                            onPick = { pickerSlot = CustomColorSlot.TERTIARY },
                            modifier = Modifier.weight(1f)
                        )
                        CustomColorPickerTile(
                            label = "Фон",
                            color = colorForSlot(CustomColorSlot.BACKGROUND),
                            onPick = { pickerSlot = CustomColorSlot.BACKGROUND },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(appScaledSpacing(6.dp)))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CustomColorPickerTile(
                            label = "Бабблы",
                            color = colorForSlot(CustomColorSlot.BUBBLE),
                            onPick = { pickerSlot = CustomColorSlot.BUBBLE },
                            modifier = Modifier.weight(1f)
                        )
                        AppearanceModeCard(
                            title = "Сбросить",
                            selected = false,
                            onClick = {
                                if (settings.customBubbleHex.isNotBlank()) {
                                    update { it.copy(customBubbleHex = "") }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(appScaledSpacing(10.dp)))

            AppearanceSectionTitle("Шрифт")
            Spacer(modifier = Modifier.height(appScaledSpacing(6.dp)))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(appCornerRadius(18.dp)),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, appPanelBorderColor())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(appScaledSpacing(10.dp))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))
                    ) {
                        AppearanceModeCard(
                            title = "Системный",
                            selected = settings.fontMode == AppFontMode.SYSTEM,
                            onClick = { update { it.copy(fontMode = AppFontMode.SYSTEM) } },
                            modifier = Modifier.weight(1f)
                        )
                        AppearanceModeCard(
                            title = "Sans",
                            selected = settings.fontMode == AppFontMode.SANS,
                            onClick = { update { it.copy(fontMode = AppFontMode.SANS) } },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(appScaledSpacing(6.dp)))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))
                    ) {
                        AppearanceModeCard(
                            title = "Serif",
                            selected = settings.fontMode == AppFontMode.SERIF,
                            onClick = { update { it.copy(fontMode = AppFontMode.SERIF) } },
                            modifier = Modifier.weight(1f)
                        )
                        AppearanceModeCard(
                            title = "Mono",
                            selected = settings.fontMode == AppFontMode.MONO,
                            onClick = { update { it.copy(fontMode = AppFontMode.MONO) } },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(appScaledSpacing(6.dp)))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))
                    ) {
                        AppearanceModeCard(
                            title = "Загрузить TTF/OTF",
                            selected = settings.fontMode == AppFontMode.EXTERNAL_CUSTOM,
                            onClick = {
                                if (settings.customFontUri.isBlank()) {
                                    onPickCustomFont()
                                } else {
                                    update { it.copy(fontMode = AppFontMode.EXTERNAL_CUSTOM) }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        AppearanceModeCard(
                            title = "Сбросить",
                            selected = false,
                            onClick = onClearCustomFont,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (settings.customFontDisplayName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(appScaledSpacing(6.dp)))
                        Text(
                            text = "Текущий файл: ${settings.customFontDisplayName}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!customFontStatusMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(appScaledSpacing(4.dp)))
                        AppFeedbackCard(
                            message = customFontStatusMessage,
                            state = inferAppFeedbackState(customFontStatusMessage)
                        )
                    }

                    Spacer(modifier = Modifier.height(appScaledSpacing(10.dp)))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Размер шрифта",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${(fontScaleDraft * 100f).roundToInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = appHapticAction(onAction = {
                                    fontScaleDraft = 1f
                                    if (kotlin.math.abs(settings.fontScale - 1f) > 0.001f) {
                                        update { it.copy(fontScale = 1f) }
                                    }
                                }),
                                enabled = kotlin.math.abs(fontScaleDraft - 1f) > 0.001f,
                                modifier = Modifier.height(appInputFieldHeight(32.dp)),
                                shape = RoundedCornerShape(appCornerRadius(10.dp))
                            ) {
                                Text(
                                    text = "100%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    DragAwareSlider(
                        value = fontScaleDraft,
                        onValueChange = { scale -> fontScaleDraft = scale },
                        onValueChangeFinished = {
                            if (kotlin.math.abs(settings.fontScale - fontScaleDraft) > 0.001f) {
                                update { it.copy(fontScale = fontScaleDraft) }
                            }
                        },
                        valueRange = 0.85f..1.3f
                    )
                }
            }
            Spacer(modifier = Modifier.height(appScaledSpacing(10.dp)))

            AppearanceSectionTitle("Валюта")
            Spacer(modifier = Modifier.height(appScaledSpacing(6.dp)))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(appCornerRadius(18.dp)),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, appPanelBorderColor())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(appScaledSpacing(10.dp))
                ) {
                    Text(
                        text = "Символ валюты",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(appScaledSpacing(4.dp)))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))
                    ) {
                        AppearanceModeCard(
                            title = "₽ Рубль",
                            selected = settings.currencySymbolMode == CurrencySymbolMode.RUB,
                            onClick = { update { it.copy(currencySymbolMode = CurrencySymbolMode.RUB) } },
                            modifier = Modifier.weight(1f)
                        )
                        AppearanceModeCard(
                            title = "\$ Доллар",
                            selected = settings.currencySymbolMode == CurrencySymbolMode.USD,
                            onClick = { update { it.copy(currencySymbolMode = CurrencySymbolMode.USD) } },
                            modifier = Modifier.weight(1f)
                        )
                        AppearanceModeCard(
                            title = "€ Евро",
                            selected = settings.currencySymbolMode == CurrencySymbolMode.EUR,
                            onClick = { update { it.copy(currencySymbolMode = CurrencySymbolMode.EUR) } },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(appScaledSpacing(6.dp)))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))
                    ) {
                        AppearanceModeCard(
                            title = "₸ Тенге",
                            selected = settings.currencySymbolMode == CurrencySymbolMode.KZT,
                            onClick = { update { it.copy(currencySymbolMode = CurrencySymbolMode.KZT) } },
                            modifier = Modifier.weight(1f)
                        )
                        AppearanceModeCard(
                            title = "Br BYN",
                            selected = settings.currencySymbolMode == CurrencySymbolMode.BYN,
                            onClick = { update { it.copy(currencySymbolMode = CurrencySymbolMode.BYN) } },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(appScaledSpacing(6.dp)))
                    Text(
                        text = "Будет применено к суммам на экранах расчёта и отчётов.",
                        style = MaterialTheme.typography.labelSmall,
                        color = appListSecondaryTextColor()
                    )
                }
            }

            Spacer(modifier = Modifier.height(appScaledSpacing(10.dp)))

            AppearanceSectionTitle("Интерфейс")
            Spacer(modifier = Modifier.height(appScaledSpacing(6.dp)))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(appCornerRadius(18.dp)),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, appPanelBorderColor())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(appScaledSpacing(10.dp))
                ) {
                    Text(
                        text = "Плотность",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(appScaledSpacing(4.dp)))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))
                    ) {
                        AppearanceModeCard(
                            title = "Комфортно",
                            selected = settings.uiDensityMode == UiDensityMode.COMFORTABLE,
                            onClick = { update { it.copy(uiDensityMode = UiDensityMode.COMFORTABLE) } },
                            modifier = Modifier.weight(1f)
                        )
                        AppearanceModeCard(
                            title = "Компактно",
                            selected = settings.uiDensityMode == UiDensityMode.COMPACT,
                            onClick = { update { it.copy(uiDensityMode = UiDensityMode.COMPACT) } },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(appScaledSpacing(10.dp)))
                    Text(
                        text = "Контраст",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(appScaledSpacing(4.dp)))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))
                    ) {
                        AppearanceModeCard(
                            title = "Обычный",
                            selected = settings.uiContrastMode == UiContrastMode.STANDARD,
                            onClick = { update { it.copy(uiContrastMode = UiContrastMode.STANDARD) } },
                            modifier = Modifier.weight(1f)
                        )
                        AppearanceModeCard(
                            title = "Повышенный",
                            selected = settings.uiContrastMode == UiContrastMode.HIGH,
                            onClick = { update { it.copy(uiContrastMode = UiContrastMode.HIGH) } },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(appScaledSpacing(10.dp)))
                    Text(
                        text = "Скорость анимаций",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(appScaledSpacing(4.dp)))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))
                    ) {
                        AppearanceModeCard(
                            title = "Обычная",
                            selected = settings.animationSpeedMode == AnimationSpeedMode.NORMAL,
                            onClick = { update { it.copy(animationSpeedMode = AnimationSpeedMode.NORMAL) } },
                            modifier = Modifier.weight(1f)
                        )
                        AppearanceModeCard(
                            title = "Медленно",
                            selected = settings.animationSpeedMode == AnimationSpeedMode.SLOW,
                            onClick = { update { it.copy(animationSpeedMode = AnimationSpeedMode.SLOW) } },
                            modifier = Modifier.weight(1f)
                        )
                        AppearanceModeCard(
                            title = "Выкл",
                            selected = settings.animationSpeedMode == AnimationSpeedMode.OFF,
                            onClick = { update { it.copy(animationSpeedMode = AnimationSpeedMode.OFF) } },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(appScaledSpacing(10.dp)))
                    Text(
                        text = "Скругления",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(appScaledSpacing(4.dp)))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))
                    ) {
                        AppearanceModeCard(
                            title = "Мягкие",
                            selected = settings.cornerStyleMode == CornerStyleMode.SOFT,
                            onClick = { update { it.copy(cornerStyleMode = CornerStyleMode.SOFT) } },
                            modifier = Modifier.weight(1f)
                        )
                        AppearanceModeCard(
                            title = "Стандарт",
                            selected = settings.cornerStyleMode == CornerStyleMode.STANDARD,
                            onClick = { update { it.copy(cornerStyleMode = CornerStyleMode.STANDARD) } },
                            modifier = Modifier.weight(1f)
                        )
                        AppearanceModeCard(
                            title = "Строгие",
                            selected = settings.cornerStyleMode == CornerStyleMode.SHARP,
                            onClick = { update { it.copy(cornerStyleMode = CornerStyleMode.SHARP) } },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(appScaledSpacing(16.dp)))
        }
    }

    pickerSlot?.let { slot ->
        val title = when (slot) {
            CustomColorSlot.PRIMARY -> "Основной"
            CustomColorSlot.SECONDARY -> "Дополнительный"
            CustomColorSlot.TERTIARY -> "Акцент"
            CustomColorSlot.BACKGROUND -> "Фон"
            CustomColorSlot.BUBBLE -> "Бабблы"
        }
        ColorPickerDialog(
            title = "Цвет: $title",
            initialColor = colorForSlot(slot),
            onDismiss = { pickerSlot = null },
            onConfirm = { selected ->
                saveSlotColor(slot, selected)
                pickerSlot = null
            }
        )
    }
}

@Composable
private fun AppearanceLivePreview(settings: AppearanceSettings) {
    val hideBottomLabels = settings.fontScale > 1.15f

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(appCardRadius()),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(appCardPadding())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Мини-превью",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Rounded.ColorLens,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(appBlockSpacing()))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(appCornerRadius(12.dp)),
                color = appBubbleBackgroundColor(defaultAlpha = 0.28f),
                border = BorderStroke(1.dp, appPanelBorderColor())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = appScaledSpacing(10.dp), vertical = appScaledSpacing(7.dp)),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null,
                        tint = appListSecondaryTextColor()
                    )
                    Text(
                        text = "Расчёт",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.Rounded.MoreHoriz,
                        contentDescription = null,
                        tint = appListSecondaryTextColor()
                    )
                }
            }

            Spacer(modifier = Modifier.height(appBlockSpacing()))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(appBlockSpacing())
            ) {
                PreviewInfoTile(
                    title = "Карточка смены",
                    value = "12 ч",
                    subtitle = "Ночная",
                    modifier = Modifier.weight(1f)
                )
                PreviewInfoTile(
                    title = "К зарплате",
                    value = formatMoney(63420.0),
                    subtitle = "30 число",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(appBlockSpacing()))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(appCornerRadius(14.dp)),
                color = appBubbleBackgroundColor(defaultAlpha = 0.32f),
                border = BorderStroke(1.dp, appPanelBorderColor())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = appScaledSpacing(8.dp), vertical = appScaledSpacing(6.dp)),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomTab.entries.take(4).forEach { tab ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.contentDescription,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!hideBottomLabels) {
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            if (hideBottomLabels) {
                Spacer(modifier = Modifier.height(appScaledSpacing(6.dp)))
                Text(
                    text = "Подписи таббара скрыты для масштаба >115%",
                    style = MaterialTheme.typography.labelSmall,
                    color = appListSecondaryTextColor()
                )
            }
        }
    }
}

@Composable
private fun PreviewInfoTile(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(appCornerRadius(12.dp)),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = appScaledSpacing(8.dp), vertical = appScaledSpacing(7.dp))
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun AppearanceSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        color = appListSecondaryTextColor()
    )
}

@Composable
private fun AppearanceModeCard(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f)
    }

    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    } else {
        appPanelBorderColor()
    }

    Surface(
        modifier = modifier
            .height(appInputFieldHeight(36.dp))
            .clip(RoundedCornerShape(appCornerRadius(10.dp)))
            .clickable(onClick = appHapticAction(onAction = onClick)),
        shape = RoundedCornerShape(appCornerRadius(10.dp)),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = appScaledSpacing(10.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
@Composable
private fun CompactTimeEditor(
    label: String,
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(appScaledSpacing(2.dp)))
        Row(
            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(4.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactTimeField(
                value = hour,
                max = 23,
                onValueChange = onHourChange,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = ":",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            CompactTimeField(
                value = minute,
                max = 59,
                onValueChange = onMinuteChange,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CompactTimeField(
    value: Int,
    max: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember(value) { mutableStateOf(value.toString().padStart(2, '0')) }

    BasicTextField(
        value = text,
        onValueChange = { newValue ->
            val filtered = newValue.filter(Char::isDigit).take(2)
            text = filtered
            filtered.toIntOrNull()?.let { parsed ->
                onValueChange(parsed.coerceIn(0, max))
            }
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier
            .height(appInputFieldHeight(34.dp))
            .clip(RoundedCornerShape(appCornerRadius(10.dp)))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, appPanelBorderColor(), RoundedCornerShape(appCornerRadius(10.dp)))
            .padding(
                horizontal = appScaledSpacing(8.dp),
                vertical = if (appIsCompactMode()) appScaledSpacing(4.dp) else appScaledSpacing(7.dp)
            )
    )
}

@Composable
private fun CustomColorPickerTile(
    label: String,
    color: Color,
    onPick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(appInputFieldHeight(44.dp))
            .clip(RoundedCornerShape(appCornerRadius(10.dp)))
            .clickable(onClick = appHapticAction(onAction = onPick)),
        shape = RoundedCornerShape(appCornerRadius(10.dp)),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = appScaledSpacing(10.dp), vertical = appScaledSpacing(6.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp))
        ) {
            Surface(
                modifier = Modifier.size(appInputFieldHeight(24.dp)),
                shape = CircleShape,
                color = color,
                border = BorderStroke(1.dp, appPanelBorderColor())
            ) {}
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ColorPickerDialog(
    title: String,
    initialColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit
) {
    var selectedColorHex by remember(initialColor) { mutableStateOf(colorToHex(initialColor)) }
    UnifiedFullColorPickerDialog(
        title = title,
        selectedColorHex = selectedColorHex,
        onColorSelected = { selectedColorHex = it },
        onDismiss = onDismiss,
        onConfirm = {
            onConfirm(parseHexColor(selectedColorHex, initialColor))
        }
    )
}

@Composable
private fun DragAwareSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChangeFinished: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val start = valueRange.start
    val end = valueRange.endInclusive
    val rangeSize = (end - start).takeIf { it > 0f } ?: 1f

    Box(modifier = modifier.fillMaxWidth()) {
        Slider(
            modifier = Modifier.fillMaxWidth(),
            value = value.coerceIn(start, end),
            onValueChange = { changed -> onValueChange(changed.coerceIn(start, end)) },
            valueRange = valueRange
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(start, end) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        if (size.width <= 0) return@awaitEachGesture

                        fun updateValueByX(x: Float) {
                            val fraction = (x / size.width.toFloat()).coerceIn(0f, 1f)
                            onValueChange((start + rangeSize * fraction).coerceIn(start, end))
                        }

                        updateValueByX(down.position.x)
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull() ?: break
                            updateValueByX(change.position.x)
                            change.consume()
                        } while (change.pressed)
                        onValueChangeFinished?.invoke()
                    }
                }
        )
    }
}

private fun parseHexColor(value: String, fallback: Color): Color {
    return runCatching {
        Color(android.graphics.Color.parseColor(sanitizeHexColor(value, colorToHex(fallback))))
    }.getOrElse { fallback }
}

private fun colorToHex(color: Color): String {
    return String.format("#%06X", 0xFFFFFF and color.toArgb())
}
