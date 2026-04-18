package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AlarmHeaderSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val triggerHaptic = appHapticAction(onAction = {})
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = { next ->
                triggerHaptic()
                onCheckedChange(next)
            },
            modifier = Modifier.scale(0.78f)
        )
    }
}

@Composable
fun AlarmCompactSection(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(appCornerRadius(14.dp)),
        color = appBubbleBackgroundColor(defaultAlpha = 0.24f),
        border = BorderStroke(1.dp, appPanelBorderColor())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = appScaledSpacing(10.dp), vertical = appScaledSpacing(8.dp))
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun AlarmDaysMiniField(
    value: String,
    onValueChange: (String) -> Unit,
    width: Dp,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = { next -> onValueChange(next.filter { it.isDigit() }) },
        modifier = modifier.width(width),
        textStyle = MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        decorationBox = { innerTextField ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, appPanelBorderColor())
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 6.dp, vertical = 5.dp)
                        .fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (value.isBlank()) {
                            Text(
                                text = "90",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        innerTextField()
                    }
                }
            }
        }
    )
}

@Composable
fun AlarmInfoPill(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = appBubbleBackgroundColor(defaultAlpha = 0.46f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AlarmQuickAction(
    text: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    icon: ImageVector? = null,
    emphasized: Boolean = false,
    compact: Boolean = false,
    hapticKind: AppHapticKind = AppHapticKind.SOFT,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(appCornerRadius(if (compact) 10.dp else 12.dp))
    val containerColor = if (emphasized) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    } else {
        appBubbleBackgroundColor(defaultAlpha = 0.32f)
    }
    val borderColor = if (emphasized) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)
    } else {
        appPanelBorderColor()
    }
    val iconContainer = if (emphasized) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.62f)
    }
    val titleColor = if (emphasized) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val subtitleColor = if (emphasized) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.82f)
    } else {
        appListSecondaryTextColor()
    }

    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = appHapticAction(kind = hapticKind, onAction = onClick))
                .padding(
                    horizontal = appScaledSpacing(if (compact) 8.dp else 10.dp),
                    vertical = appScaledSpacing(if (compact) 6.dp else 9.dp)
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)
        ) {
            if (icon != null) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = iconContainer
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = titleColor,
                        modifier = Modifier.padding(if (compact) 5.dp else 6.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Text(
                    text = text,
                    style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor,
                    textAlign = TextAlign.Start
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = subtitleColor,
                        textAlign = TextAlign.Start
                    )
                }
            }
        }
    }
}
