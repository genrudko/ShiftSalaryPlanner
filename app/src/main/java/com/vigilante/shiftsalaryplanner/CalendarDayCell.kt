package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import java.time.LocalDate

@Composable
fun DayCell(
    date: LocalDate,
    shiftCode: String?,
    template: ShiftTemplateEntity?,
    backgroundColor: Color,
    isSpecialDay: Boolean,
    isInPreviewRange: Boolean,
    isPreviewEdge: Boolean,
    isCurrentMonthCell: Boolean,
    compactMode: Boolean,
    onClick: () -> Unit
) {
    val isToday = date == LocalDate.now()
    val isDark = isSystemInDarkTheme()
    val holidayTint = if (isDark) Color(0xFF3A2B35) else Color(0xFFFFEFEF)
    val emptyBase = if (isDark) Color(0xFF1B2231) else backgroundColor
    val previewTint = MaterialTheme.colorScheme.primaryContainer

    val baseCellBackground = when {
        shiftCode == null && isSpecialDay -> if (isDark) Color(0xFF312631) else holidayTint
        shiftCode == null -> emptyBase
        isSpecialDay -> lerp(backgroundColor, holidayTint, if (isDark) 0.24f else 0.22f)
        else -> backgroundColor
    }

    val specialBackground = if (isCurrentMonthCell) {
        baseCellBackground
    } else {
        lerp(baseCellBackground, MaterialTheme.colorScheme.background, if (isDark) 0.46f else 0.52f)
    }

    val finalBackground = if (isInPreviewRange) {
        lerp(specialBackground, previewTint, if (isPreviewEdge) 0.60f else 0.36f)
    } else {
        specialBackground
    }

    val borderColor = when {
        isPreviewEdge -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primary
        !isCurrentMonthCell -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.70f else 0.35f)
        else -> if (isDark) Color.White.copy(alpha = 0.22f) else MaterialTheme.colorScheme.outlineVariant
    }

    val borderWidth = when {
        isPreviewEdge -> 2.dp
        isToday -> 2.dp
        else -> 1.dp
    }
    val glyph = when {
        template != null -> iconGlyph(template.iconKey, template.code)
        shiftCode != null -> shiftCode
        else -> ""
    }

    val glyphFontSize = (if (compactMode) shiftGlyphFontSize(glyph) - 2 else shiftGlyphFontSize(glyph)).coerceAtLeast(10).sp
    val mainTextColor = when {
        !isCurrentMonthCell && isSpecialDay -> Color(0xFFD32F2F).copy(alpha = 0.55f)
        !isCurrentMonthCell -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.36f)
        isSpecialDay -> Color(0xFFD32F2F)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = Modifier
            .height(if (compactMode) 58.dp else 72.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (compactMode) 10.dp else 12.dp))
            .background(finalBackground)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(if (compactMode) 10.dp else 12.dp)
            )
            .clickable(onClick = onClick)
            .padding(if (compactMode) 4.dp else 6.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = mainTextColor
        )

        Text(
            text = glyph,
            fontSize = glyphFontSize,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            color = if (isCurrentMonthCell) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f)
        )
    }
}
