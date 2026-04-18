package com.vigilante.shiftsalaryplanner

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun SettingsSectionCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    val cardShape = RoundedCornerShape(appCornerRadius(16.dp))
    val contentPadding = appScaledSpacing(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(appPanelColor())
            .border(1.dp, appPanelBorderColor(), cardShape)
            .padding(contentPadding)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall
        )
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

@Composable
fun CollapsibleSettingsSectionCard(
    title: String,
    subtitle: String,
    summary: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    val cardShape = RoundedCornerShape(appCornerRadius(16.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(appPanelColor())
            .border(1.dp, appPanelBorderColor(), cardShape)
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(
                    horizontal = appScaledSpacing(12.dp),
                    vertical = appScaledSpacing(8.dp)
                ),
            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(10.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = if (expanded) subtitle else summary.ifBlank { subtitle },
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Text(
                text = if (expanded) "Свернуть" else "Открыть",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (expanded) {
            HorizontalDivider()
            Column(modifier = Modifier.padding(appScaledSpacing(10.dp))) {
                content()
            }
        }
    }
}

@Composable
fun CompactSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val triggerHaptic = appHapticAction(onAction = {})
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = appScaledSpacing(2.dp)),
        horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(12.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 3
        )

        Switch(
            checked = checked,
            onCheckedChange = { next ->
                triggerHaptic()
                onCheckedChange(next)
            },
            modifier = Modifier.scale(0.82f)
        )
    }
}
@Composable
fun CompactIntField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    CompactInputField(
        value = value,
        onValueChange = { newValue ->
            onValueChange(newValue.filter { it.isDigit() })
        },
        label = label,
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        keyboardType = KeyboardType.Number
    )
}
@Composable
fun CompactDecimalField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    CompactInputField(
        value = value,
        onValueChange = { newValue ->
            onValueChange(
                newValue
                    .replace(',', '.')
                    .filter { it.isDigit() || it == '.' }
            )
        },
        label = label,
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        keyboardType = KeyboardType.Decimal
    )
}

@Composable
fun CompactTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    CompactInputField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        keyboardType = KeyboardType.Text
    )
}

@Composable
private fun CompactInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier,
    keyboardOptions: KeyboardOptions,
    keyboardType: KeyboardType
) {
    val fieldHeight = appInputFieldHeight(36.dp)
    val corner = appCornerRadius(10.dp)
    val verticalPadding = if (appIsCompactMode()) appScaledSpacing(4.dp) else appScaledSpacing(7.dp)
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )
        Spacer(modifier = Modifier.height(2.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            keyboardOptions = keyboardOptions.copy(keyboardType = keyboardType),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(fieldHeight)
                .clip(RoundedCornerShape(corner))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(corner)
                )
                .padding(horizontal = appScaledSpacing(10.dp), vertical = verticalPadding)
        )
    }
}
@Composable
fun PayModeChoiceCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    showSubtitle: Boolean = true
) {
    val tileShape = RoundedCornerShape(appCornerRadius(12.dp))
    val tilePadding = appScaledSpacing(10.dp)
    Column(
        modifier = modifier
            .clip(tileShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(tilePadding)
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodySmall
        )
        if (showSubtitle) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
@Composable
fun NormModeChoiceCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    showSubtitle: Boolean = true
) {
    val tileShape = RoundedCornerShape(appCornerRadius(12.dp))
    val tilePadding = appScaledSpacing(10.dp)
    Column(
        modifier = modifier
            .clip(tileShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(tilePadding)
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodySmall
        )
        if (showSubtitle) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
@Composable
fun AnnualNormSourceChoiceCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    showSubtitle: Boolean = true
) {
    val tileShape = RoundedCornerShape(appCornerRadius(12.dp))
    val tilePadding = appScaledSpacing(10.dp)
    Column(
        modifier = modifier
            .clip(tileShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(tilePadding)
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodySmall
        )
        if (showSubtitle) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
@Composable
fun AdvanceModeChoiceCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    showSubtitle: Boolean = true
) {
    val tileShape = RoundedCornerShape(appCornerRadius(12.dp))
    val tilePadding = appScaledSpacing(10.dp)
    Column(
        modifier = modifier
            .clip(tileShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(tilePadding)
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodySmall
        )
        if (showSubtitle) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
@Composable
fun ExtraSalaryModeChoiceCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    showSubtitle: Boolean = true
) {
    val tileShape = RoundedCornerShape(appCornerRadius(12.dp))
    val tilePadding = appScaledSpacing(10.dp)
    Column(
        modifier = modifier
            .clip(tileShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(tilePadding)
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodySmall
        )
        if (showSubtitle) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
