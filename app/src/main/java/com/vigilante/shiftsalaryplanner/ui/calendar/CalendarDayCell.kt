package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.vigilante.shiftsalaryplanner.data.ShiftTemplateEntity
import com.vigilante.shiftsalaryplanner.settings.WORKPLACE_MAIN_ID
import com.vigilante.shiftsalaryplanner.settings.WORKPLACE_SECOND_ID
import com.vigilante.shiftsalaryplanner.settings.WORKPLACE_THIRD_ID
import java.time.LocalDate

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DayCell(
    date: LocalDate,
    shiftCode: String?,
    template: ShiftTemplateEntity?,
    assignmentWorkplaceIds: List<String>,
    assignmentShiftCodes: List<String>,
    assignmentIconKeys: List<String?>,
    assignmentBackgroundColors: List<Color>,
    backgroundColor: Color,
    today: LocalDate,
    isSpecialDay: Boolean,
    isSelected: Boolean,
    isInPreviewRange: Boolean,
    isPreviewEdge: Boolean,
    isCurrentMonthCell: Boolean,
    hasNote: Boolean,
    hasShiftOverride: Boolean,
    compactMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isToday = date == today
    val surfaceBackground = MaterialTheme.colorScheme.background
    val isDark = surfaceBackground.luminance() < 0.5f
    val holidayTint = if (isDark) Color(0xFF3A2B35) else Color(0xFFFFEFEF)
    val emptyBase = if (isDark) Color(0xFF1B2231) else backgroundColor
    val previewTint = MaterialTheme.colorScheme.primaryContainer

    fun adjustedBackground(baseColor: Color): Color = when {
        shiftCode == null && isSpecialDay -> if (isDark) Color(0xFF312631) else holidayTint
        shiftCode == null -> emptyBase
        isSpecialDay -> lerp(baseColor, holidayTint, if (isDark) 0.24f else 0.22f)
        else -> baseColor
    }
    fun applyCellTransforms(baseColor: Color): Color {
        val adjusted = adjustedBackground(baseColor)
        val monthAdjusted = if (isCurrentMonthCell) {
            adjusted
        } else {
            lerp(adjusted, surfaceBackground, if (isDark) 0.46f else 0.52f)
        }
        return if (isInPreviewRange) {
            lerp(monthAdjusted, previewTint, if (isPreviewEdge) 0.60f else 0.36f)
        } else {
            monthAdjusted
        }
    }

    val segmentBaseColors = if (assignmentBackgroundColors.isNotEmpty()) {
        assignmentBackgroundColors
    } else {
        listOf(backgroundColor)
    }

    val finalBackgrounds = segmentBaseColors.map(::applyCellTransforms)
    val contentReferenceBackground = applyCellTransforms(backgroundColor)
    val contentBaseColor = if (contentReferenceBackground.luminance() > 0.52f) {
        Color(0xFF151A21)
    } else {
        Color(0xFFF3F7FF)
    }

    val borderColor = when {
        isPreviewEdge -> MaterialTheme.colorScheme.primary
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.tertiary
        !isCurrentMonthCell -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.70f else 0.35f)
        else -> if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.48f) else MaterialTheme.colorScheme.outlineVariant
    }

    val borderWidth = when {
        isPreviewEdge -> 2.dp
        isSelected -> 2.dp
        isToday -> 1.5.dp
        else -> 1.dp
    }
    val displayCode = shiftCode?.let(::stripWorkplaceScopeFromShiftCode)
    val glyph = when {
        template != null -> iconGlyph(template.iconKey, displayCode ?: stripWorkplaceScopeFromShiftCode(template.code))
        displayCode != null -> displayCode
        else -> ""
    }
    val iconVector = template?.let { materialShiftIcon(it.iconKey) }

    val glyphFontSize = (if (compactMode) shiftGlyphFontSize(glyph) - 2 else shiftGlyphFontSize(glyph)).coerceAtLeast(10).sp
    val baseAlpha = if (isCurrentMonthCell) 1f else 0.50f
    val mainTextColor = when {
        isSpecialDay && contentReferenceBackground.luminance() > 0.52f -> Color(0xFFB3261E).copy(alpha = baseAlpha)
        isSpecialDay -> Color(0xFFFFB4AB).copy(alpha = baseAlpha)
        else -> contentBaseColor.copy(alpha = baseAlpha)
    }
    val iconTintColor = contentBaseColor.copy(alpha = if (isCurrentMonthCell) 0.92f else 0.46f)
    val assignmentCount = assignmentShiftCodes.size
    val showAssignmentsIcons = assignmentShiftCodes.size > 1
    val iconHeight = when {
        compactMode && assignmentCount > 1 -> 14.dp
        compactMode -> 16.dp
        assignmentCount > 1 -> 16.dp
        else -> 18.dp
    }

    Box(
        modifier = Modifier
            .height(if (compactMode) 58.dp else 72.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (compactMode) 10.dp else 12.dp))
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(if (compactMode) 10.dp else 12.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        if (finalBackgrounds.size <= 1) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(contentReferenceBackground)
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                finalBackgrounds.forEach { segmentColor ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(segmentColor)
                    )
                }
            }
        }

        DayDateBadge(
            day = date.dayOfMonth,
            isSelected = isSelected,
            isToday = isToday,
            textColor = mainTextColor,
            compactMode = compactMode,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = if (compactMode) 3.dp else 4.dp, top = if (compactMode) 3.dp else 4.dp)
        )

        if (showAssignmentsIcons) {
            val iconSize = when {
                assignmentCount >= 4 -> if (compactMode) 10.dp else 11.dp
                assignmentCount == 3 -> if (compactMode) 11.dp else 12.dp
                else -> if (compactMode) 12.dp else 13.dp
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        end = if (compactMode) 3.dp else 4.dp
                    )
            ) {
                assignmentShiftCodes.forEachIndexed { index, code ->
                    val codeFallback = stripWorkplaceScopeFromShiftCode(code)
                    val iconKey = assignmentIconKeys.getOrNull(index).orEmpty()
                    val workplaceBadge = workplaceBadgeLabel(assignmentWorkplaceIds.getOrNull(index))
                    val icon = materialShiftIcon(iconKey)
                    val slotColor = finalBackgrounds.getOrElse(index) { contentReferenceBackground }
                    val slotTint = if (slotColor.luminance() > 0.52f) {
                        Color(0xFF151A21)
                    } else {
                        Color(0xFFF3F7FF)
                    }.copy(alpha = if (isCurrentMonthCell) 0.92f else 0.46f)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        val segmentIdentityBackground = if (slotColor.luminance() > 0.52f) {
                            Color.Black.copy(alpha = 0.08f)
                        } else {
                            Color.White.copy(alpha = 0.14f)
                        }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(segmentIdentityBackground)
                                .padding(horizontal = if (compactMode) 3.dp else 4.dp, vertical = 1.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!workplaceBadge.isNullOrBlank()) {
                                Text(
                                    text = workplaceBadge,
                                    fontSize = (if (compactMode) 7 else 8).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = slotTint.copy(alpha = if (isCurrentMonthCell) 0.85f else 0.48f)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                            }
                            if (icon != null) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = slotTint,
                                    modifier = Modifier.size(iconSize)
                                )
                            } else {
                                Text(
                                    text = iconGlyph(iconKey, codeFallback),
                                    fontSize = (if (compactMode) 8 else 9).sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    color = slotTint
                                )
                            }
                        }
                    }
                }
            }
        } else if (shiftCode != null) {
            SingleShiftIdentityBadge(
                glyph = glyph,
                iconVector = iconVector,
                workplaceBadge = workplaceBadgeLabel(assignmentWorkplaceIds.firstOrNull()),
                tint = iconTintColor,
                background = contentReferenceBackground,
                glyphFontSize = glyphFontSize,
                iconHeight = iconHeight,
                compactMode = compactMode,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(if (compactMode) 4.dp else 6.dp)
            )
        }

        DayMetadataCluster(
            hasNote = hasNote,
            hasShiftOverride = hasShiftOverride,
            isDark = isDark,
            compactMode = compactMode,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = if (compactMode) 4.dp else 5.dp, end = if (compactMode) 4.dp else 5.dp)
        )
    }
}


@Composable
private fun DayDateBadge(
    day: Int,
    isSelected: Boolean,
    isToday: Boolean,
    textColor: Color,
    compactMode: Boolean,
    modifier: Modifier = Modifier
) {
    val badgeBackground = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.tertiaryContainer
        else -> Color.Transparent
    }
    val foreground = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> textColor
    }
    val dateBadgeSize = if (compactMode) 22.dp else 24.dp
    Box(
        modifier = modifier
            .size(dateBadgeSize)
            .clip(RoundedCornerShape(999.dp))
            .background(badgeBackground),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = foreground
        )
    }
}

@Composable
private fun SingleShiftIdentityBadge(
    glyph: String,
    iconVector: ImageVector?,
    workplaceBadge: String?,
    tint: Color,
    background: Color,
    glyphFontSize: TextUnit,
    iconHeight: Dp,
    compactMode: Boolean,
    modifier: Modifier = Modifier
) {
    val identityBackground = if (background.luminance() > 0.52f) {
        Color.Black.copy(alpha = 0.08f)
    } else {
        Color.White.copy(alpha = 0.14f)
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(identityBackground)
            .padding(horizontal = if (compactMode) 4.dp else 5.dp, vertical = if (compactMode) 1.dp else 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!workplaceBadge.isNullOrBlank()) {
            Text(
                text = workplaceBadge,
                fontSize = (if (compactMode) 7 else 8).sp,
                fontWeight = FontWeight.Bold,
                color = tint.copy(alpha = 0.82f)
            )
            Spacer(modifier = Modifier.width(3.dp))
        }
        if (iconVector != null) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.height(iconHeight)
            )
        } else {
            Text(text = glyph, fontSize = glyphFontSize, fontWeight = FontWeight.Bold, maxLines = 1, color = tint)
        }
    }
}

@Composable
private fun DayMetadataCluster(
    hasNote: Boolean,
    hasShiftOverride: Boolean,
    isDark: Boolean,
    compactMode: Boolean,
    modifier: Modifier = Modifier
) {
    val metadataClusterSize = if (compactMode) 15.dp else 17.dp
    Box(
        modifier = modifier.size(metadataClusterSize),
        contentAlignment = Alignment.Center
    ) {
        if (hasShiftOverride) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.94f))
                    .border(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f), RoundedCornerShape(999.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = "Индивидуальная правка дня",
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(if (compactMode) 10.dp else 11.dp)
                )
            }
        }
        if (hasNote) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(if (compactMode) 6.dp else 7.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.tertiary)
                    .border(1.dp, if (isDark) Color(0xFF0C1118) else Color.White, RoundedCornerShape(999.dp))
            )
        }
    }
}

private fun workplaceBadgeLabel(workplaceId: String?): String? = when (workplaceId) {
    WORKPLACE_MAIN_ID -> "1"
    WORKPLACE_SECOND_ID -> "2"
    WORKPLACE_THIRD_ID -> "3"
    else -> null
}
