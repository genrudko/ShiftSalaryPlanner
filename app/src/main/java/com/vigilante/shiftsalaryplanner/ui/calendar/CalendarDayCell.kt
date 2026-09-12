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
import com.vigilante.shiftsalaryplanner.ui.theme.evolutionColorRoles
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
    val roles = evolutionColorRoles()
    val isToday = date == today
    val isDark = roles.appBackground.luminance() < 0.5f
    val semanticSegmentColors = if (assignmentBackgroundColors.isNotEmpty()) {
        assignmentBackgroundColors
    } else {
        listOf(backgroundColor)
    }

    fun baseCellSurface(shiftColor: Color): Color {
        val neutral = roles.surfacePrimary
        return when {
            shiftCode == null && isSpecialDay -> lerp(neutral, roles.dangerAccent, if (isDark) 0.15f else 0.09f)
            shiftCode == null -> lerp(neutral, backgroundColor, if (isDark) 0.10f else 0.08f)
            isSpecialDay -> lerp(lerp(neutral, shiftColor, if (isDark) 0.22f else 0.15f), roles.dangerAccent, 0.08f)
            else -> lerp(neutral, shiftColor, if (isDark) 0.22f else 0.16f)
        }
    }

    fun applyCellTransforms(baseColor: Color): Color {
        val monthAdjusted = if (isCurrentMonthCell) {
            baseCellSurface(baseColor)
        } else {
            lerp(baseCellSurface(baseColor), roles.appBackground, 0.54f)
        }
        val rangeAdjusted = if (isInPreviewRange) {
            lerp(monthAdjusted, roles.brandPrimary, if (isPreviewEdge) 0.20f else 0.11f)
        } else {
            monthAdjusted
        }
        val isSelectedTint = isSelected && isCurrentMonthCell
        return if (isSelectedTint) {
            lerp(rangeAdjusted, roles.brandPrimary, if (isDark) 0.10f else 0.065f)
        } else {
            rangeAdjusted
        }
    }

    val cellSurfaceColors = semanticSegmentColors.map(::applyCellTransforms)
    val contentReferenceBackground = cellSurfaceColors.firstOrNull() ?: roles.surfacePrimary
    val baseAlpha = if (isCurrentMonthCell) 1f else 0.48f
    val mainTextColor = when {
        isSpecialDay -> roles.dangerAccent.copy(alpha = baseAlpha)
        else -> roles.contentPrimary.copy(alpha = baseAlpha)
    }

    val selectionBorder: Pair<Dp, Color>? = when {
        isPreviewEdge -> 2.dp to roles.brandSecondary
        else -> null
    }
    val cellShape = RoundedCornerShape(if (compactMode) 11.dp else 13.dp)
    val displayCode = shiftCode?.let(::stripWorkplaceScopeFromShiftCode)
    val glyph = when {
        template != null -> iconGlyph(template.iconKey, displayCode ?: stripWorkplaceScopeFromShiftCode(template.code))
        displayCode != null -> displayCode
        else -> ""
    }
    val iconVector = template?.let { materialShiftIcon(it.iconKey) }
    val glyphFontSize = (if (compactMode) shiftGlyphFontSize(glyph) - 2 else shiftGlyphFontSize(glyph)).coerceAtLeast(10).sp
    val assignmentCount = assignmentShiftCodes.size
    val visibleAssignments = assignmentShiftCodes.take(2)
    val hiddenAssignmentCount = (assignmentCount - visibleAssignments.size).coerceAtLeast(0)
    val showAssignmentsIcons = assignmentCount > 1
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
            .clip(cellShape)
            .then(
                if (selectionBorder != null) {
                    Modifier.border(selectionBorder.first, selectionBorder.second, cellShape)
                } else {
                    Modifier
                }
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        if (cellSurfaceColors.size <= 1) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(contentReferenceBackground)
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                cellSurfaceColors.forEach { segmentColor ->
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
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(
                        start = if (compactMode) 3.dp else 4.dp,
                        end = if (compactMode) 3.dp else 4.dp,
                        bottom = if (compactMode) 3.dp else 4.dp,
                        top = if (compactMode) 25.dp else 29.dp
                    )
            ) {
                visibleAssignments.forEachIndexed { index, code ->
                    val codeFallback = stripWorkplaceScopeFromShiftCode(code)
                    val iconKey = assignmentIconKeys.getOrNull(index).orEmpty()
                    val workplaceBadge = workplaceBadgeLabel(assignmentWorkplaceIds.getOrNull(index))
                    val icon = materialShiftIcon(iconKey)
                    val slotColor = semanticSegmentColors.getOrElse(index) { backgroundColor }
                    val slotTint = if (slotColor.luminance() > 0.52f) Color(0xFF151A21) else Color(0xFFF7F8FF)
                    val isOverflowRow = index == visibleAssignments.lastIndex && hiddenAssignmentCount > 0
                    val overflowEndPadding = if (isOverflowRow) {
                        if (compactMode) 11.dp else 13.dp
                    } else {
                        0.dp
                    }
                    val segmentIdentityBackground = if (isCurrentMonthCell) slotColor else lerp(slotColor, roles.surfacePrimary, 0.48f)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(if (compactMode) 5.dp else 6.dp))
                            .background(segmentIdentityBackground)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.Center)
                                .padding(
                                    start = if (compactMode) 3.dp else 4.dp,
                                    end = (if (compactMode) 3.dp else 4.dp) + overflowEndPadding,
                                    top = 1.dp,
                                    bottom = 1.dp
                                ),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!workplaceBadge.isNullOrBlank()) {
                                Text(
                                    text = workplaceBadge,
                                    fontSize = (if (compactMode) 7 else 8).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = slotTint.copy(alpha = if (isCurrentMonthCell) 0.82f else 0.48f)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                            }
                            if (icon != null) {
                                Icon(icon, null, tint = slotTint, modifier = Modifier.size(iconSize))
                            } else {
                                Text(
                                    text = iconGlyph(iconKey, codeFallback),
                                    fontSize = (if (compactMode) 8 else 9).sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    color = slotTint.copy(alpha = if (isCurrentMonthCell) 1f else 0.55f)
                                )
                            }
                        }
                        if (isOverflowRow) {
                            Text(
                                text = "+$hiddenAssignmentCount",
                                fontSize = (if (compactMode) 6 else 7).sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                color = slotTint.copy(alpha = if (isCurrentMonthCell) 0.82f else 0.48f),
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 2.dp)
                            )
                        }
                    }
                }
            }
        } else if (shiftCode != null) {
            val shiftColor = semanticSegmentColors.firstOrNull() ?: backgroundColor
            val shiftTint = if (shiftColor.luminance() > 0.52f) Color(0xFF151A21) else Color(0xFFF7F8FF)
            SingleShiftIdentityBadge(
                glyph = glyph,
                iconVector = iconVector,
                workplaceBadge = workplaceBadgeLabel(assignmentWorkplaceIds.firstOrNull()),
                tint = shiftTint.copy(alpha = if (isCurrentMonthCell) 1f else 0.55f),
                background = shiftColor,
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
    val roles = evolutionColorRoles()
    val badgeBackground = when {
        isSelected -> roles.brandPrimary
        isToday -> lerp(roles.surfacePrimary, roles.brandSecondary, 0.18f)
        else -> Color.Transparent
    }
    val foreground = when {
        isSelected -> Color.White
        isToday -> roles.brandSecondary
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
    EvolutionShiftIdentityBadge(
        glyph = glyph,
        iconVector = iconVector,
        workplaceBadge = workplaceBadge,
        tint = tint,
        shiftColor = background,
        glyphFontSize = glyphFontSize,
        iconHeight = iconHeight,
        compactMode = compactMode,
        modifier = modifier
    )
}

@Composable
private fun EvolutionShiftIdentityBadge(
    glyph: String,
    iconVector: ImageVector?,
    workplaceBadge: String?,
    tint: Color,
    shiftColor: Color,
    glyphFontSize: TextUnit,
    iconHeight: Dp,
    compactMode: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (compactMode) 6.dp else 7.dp))
            .background(shiftColor)
            .padding(horizontal = if (compactMode) 4.dp else 6.dp, vertical = if (compactMode) 2.dp else 3.dp),
        horizontalArrangement = Arrangement.Center,
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
            Icon(iconVector, null, tint = tint, modifier = Modifier.height(iconHeight))
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
    val roles = evolutionColorRoles()
    val metadataClusterSize = if (compactMode) 11.dp else 12.dp
    Box(
        modifier = modifier.size(metadataClusterSize),
        contentAlignment = Alignment.Center
    ) {
        if (hasShiftOverride) {
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = "Индивидуальная правка дня",
                tint = roles.brandSecondary.copy(alpha = 0.88f),
                modifier = Modifier.size(if (compactMode) 9.dp else 10.dp)
            )
        }
        if (hasNote) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(if (compactMode) 4.dp else 5.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(roles.warningAccent)
                    .border(
                        0.75.dp,
                        if (isDark) roles.appBackground else Color.White,
                        RoundedCornerShape(999.dp)
                    )
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
