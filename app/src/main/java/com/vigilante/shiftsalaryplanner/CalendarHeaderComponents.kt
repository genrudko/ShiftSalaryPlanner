package com.vigilante.shiftsalaryplanner

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.YearMonth
import java.util.Locale
import java.time.format.DateTimeFormatter

@Composable
fun MonthHeader(
    currentMonth: YearMonth,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onPickMonth: (YearMonth) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val ruLocale = remember { Locale.forLanguageTag("ru-RU") }

    val formatter = remember {
        DateTimeFormatter.ofPattern("LLLL yyyy", ruLocale)
    }

    val monthTitle = currentMonth.atDay(1).format(formatter).replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(ruLocale) else it.toString()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(AppRadius.sm))
                .background(appPanelColor())
                .border(1.dp, appPanelBorderColor(), RoundedCornerShape(AppRadius.sm))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onPrevMonth()
                },
            contentAlignment = Alignment.Center
        ) {
            Text("←", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        Text(
            text = monthTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = AppSpacing.md)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val initialDate = currentMonth.atDay(1)

                    DatePickerDialog(
                        context,
                        { _, year, month, _ ->
                            onPickMonth(YearMonth.of(year, month + 1))
                        },
                        initialDate.year,
                        initialDate.monthValue - 1,
                        initialDate.dayOfMonth
                    ).show()
                }
        )

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(AppRadius.sm))
                .background(appPanelColor())
                .border(1.dp, appPanelBorderColor(), RoundedCornerShape(AppRadius.sm))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onNextMonth()
                },
            contentAlignment = Alignment.Center
        ) {
            Text("→", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}
