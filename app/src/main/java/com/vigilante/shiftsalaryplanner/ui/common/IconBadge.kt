package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun IconBadge(
    iconKey: String,
    fallbackCode: String,
    badgeColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
    shape: Shape = RoundedCornerShape(12.dp),
    selected: Boolean = false,
    unselectedBorderColor: Color = Color.Transparent,
    selectedGlowColor: Color? = null
) {
    val glowColor = selectedGlowColor ?: MaterialTheme.colorScheme.primary
    val outerSize = if (selected) size + 4.dp else size
    val borderColor = if (selected) glowColor.copy(alpha = 0.74f) else unselectedBorderColor
    val iconVector = materialShiftIcon(iconKey)

    Box(
        modifier = modifier.size(outerSize),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(glowColor.copy(alpha = 0.16f))
            )
        }
        Box(
            modifier = Modifier
                .size(size)
                .clip(shape)
                .background(badgeColor)
                .border(1.dp, borderColor, shape),
            contentAlignment = Alignment.Center
        ) {
            if (iconVector != null) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = readableContentColor(badgeColor)
                )
            } else {
                val glyph = iconGlyph(iconKey, fallbackCode)
                val glyphSizeSp = remember(glyph, size) {
                    val baseSp = shiftGlyphFontSize(glyph).toFloat()
                    val targetBySizeSp = size.value * 0.52f
                    val lengthScale = when {
                        glyph.length >= 7 -> 0.52f
                        glyph.length == 6 -> 0.58f
                        glyph.length == 5 -> 0.66f
                        glyph.length == 4 -> 0.76f
                        glyph.length == 3 -> 0.88f
                        else -> 1f
                    }
                    (targetBySizeSp * lengthScale)
                        .coerceAtMost(baseSp)
                        .coerceAtLeast(7f)
                }
                Text(
                    text = glyph,
                    color = readableContentColor(badgeColor),
                    fontWeight = FontWeight.Bold,
                    fontSize = glyphSizeSp.sp,
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
