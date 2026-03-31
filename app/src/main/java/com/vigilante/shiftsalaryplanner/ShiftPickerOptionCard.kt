package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity

@Composable
fun ShiftPickerOptionCard(
    template: ShiftTemplateEntity,
    selected: Boolean,
    onClick: () -> Unit
) {
    val accentColor = Color(parseColorHex(template.colorHex, 0xFFE0E0E0.toInt()))
    val glyph = iconGlyph(template.iconKey, template.code)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(21.dp))
                .background(accentColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = glyph,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = shiftGlyphFontSize(glyph).sp,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = template.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Text(
                text = "Код: ${template.code}",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = buildString {
                    append("Оплач. ")
                    append(formatDouble(template.paidHours()))
                    append(" ч")
                    if (template.breakHours > 0.0) {
                        append(" • Обед ")
                        append(formatDouble(template.breakHours))
                        append(" ч")
                    }
                    if (template.nightHours > 0.0) {
                        append(" • Ночь ")
                        append(formatDouble(template.nightHours))
                        append(" ч")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "›",
            style = MaterialTheme.typography.titleLarge
        )
    }
}
