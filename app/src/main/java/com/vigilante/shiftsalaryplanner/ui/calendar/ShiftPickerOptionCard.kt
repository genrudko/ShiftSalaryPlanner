package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
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
import com.vigilante.shiftsalaryplanner.ui.theme.evolutionColorRoles

@Composable
fun ShiftPickerOptionCard(
    template: ShiftTemplateEntity,
    selected: Boolean,
    onClick: () -> Unit
) {
    val accentColor = Color(parseColorHex(template.colorHex, 0xFFE0E0E0.toInt()))
    val displayCode = stripWorkplaceScopeFromShiftCode(template.code)

    val roles = evolutionColorRoles()
    EvolutionSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        role = if (selected) EvolutionSurfaceRole.ACCENT else EvolutionSurfaceRole.SOFT,
        shape = RoundedCornerShape(16.dp),
        border = if (selected) BorderStroke(2.dp, roles.brandPrimary.copy(alpha = 0.72f)) else null,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(
            iconKey = template.iconKey,
            fallbackCode = displayCode,
            badgeColor = accentColor,
            size = 42.dp,
            shape = RoundedCornerShape(21.dp),
            selected = selected,
            unselectedBorderColor = roles.contentSecondary.copy(alpha = 0.35f)
        )

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
                text = "Код: $displayCode",
                style = MaterialTheme.typography.bodySmall,
                color = appListSecondaryTextColor()
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
                color = appListSecondaryTextColor(),
                maxLines = 2
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    }
}
