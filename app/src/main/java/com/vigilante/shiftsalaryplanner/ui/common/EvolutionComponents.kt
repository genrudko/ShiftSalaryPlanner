package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.ui.theme.AppVisualStyleMode
import com.vigilante.shiftsalaryplanner.ui.theme.LocalAppAppearanceSettings
import com.vigilante.shiftsalaryplanner.ui.theme.UiContrastMode
import com.vigilante.shiftsalaryplanner.ui.theme.evolutionColorRoles

enum class EvolutionSurfaceRole {
    PRIMARY,
    SOFT,
    ACCENT,
    FLOATING,
    HERO
}

enum class EvolutionIconTone {
    BRAND,
    SECONDARY,
    FINANCE,
    WARNING,
    DANGER,
    NEUTRAL
}

@Composable
fun EvolutionSurface(
    modifier: Modifier = Modifier,
    role: EvolutionSurfaceRole = EvolutionSurfaceRole.PRIMARY,
    shape: Shape = RoundedCornerShape(appCornerRadius(22.dp)),
    border: BorderStroke? = null,
    shadowElevation: Dp = 2.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val roles = evolutionColorRoles()
    val appearance = LocalAppAppearanceSettings.current
    val highContrast = appearance.uiContrastMode == UiContrastMode.HIGH
    val base = when (role) {
        EvolutionSurfaceRole.PRIMARY -> roles.surfacePrimary
        EvolutionSurfaceRole.SOFT -> roles.surfaceSoft
        EvolutionSurfaceRole.ACCENT -> roles.surfaceAccent
        EvolutionSurfaceRole.FLOATING -> roles.surfaceFloating
        EvolutionSurfaceRole.HERO -> roles.surfaceAccent
    }
    val resolvedBorder = border ?: if (highContrast) {
        BorderStroke(1.dp, roles.contentSecondary.copy(alpha = 0.70f))
    } else {
        null
    }
    val elevation = when (role) {
        EvolutionSurfaceRole.FLOATING -> shadowElevation.coerceAtLeast(5.dp)
        EvolutionSurfaceRole.HERO -> shadowElevation.coerceAtLeast(3.dp)
        EvolutionSurfaceRole.PRIMARY,
        EvolutionSurfaceRole.SOFT,
        EvolutionSurfaceRole.ACCENT -> shadowElevation.coerceAtMost(2.dp)
    }

    Surface(
        modifier = modifier,
        shape = shape,
        color = base,
        contentColor = roles.contentPrimary,
        border = resolvedBorder,
        shadowElevation = elevation,
        tonalElevation = 0.dp
    ) {
        Box {
            if (role == EvolutionSurfaceRole.HERO || appearance.visualStyleMode == AppVisualStyleMode.EXPRESSIVE_GLASS) {
                val first = if (role == EvolutionSurfaceRole.HERO) roles.brandPrimary.copy(alpha = 0.15f)
                    else Color.White.copy(alpha = 0.10f)
                val second = if (role == EvolutionSurfaceRole.HERO) roles.brandSecondary.copy(alpha = 0.11f)
                    else roles.brandPrimary.copy(alpha = 0.07f)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(first, Color.Transparent, second)
                            ),
                            shape
                        )
                )
            }
            content()
        }
    }
}

@Composable
fun EvolutionIconTile(
    icon: ImageVector? = null,
    glyph: String? = null,
    contentDescription: String?,
    tone: EvolutionIconTone,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val roles = evolutionColorRoles()
    val strong = evolutionToneColor(tone)
    val dark = MaterialTheme.colorScheme.background.red + MaterialTheme.colorScheme.background.green +
        MaterialTheme.colorScheme.background.blue < 1.5f
    val background = if (dark) lerp(roles.surfacePrimary, strong, 0.24f) else lerp(Color.White, strong, 0.17f)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(appCornerRadius(14.dp)))
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        when {
            icon != null -> Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = strong,
                modifier = Modifier.size(size * 0.52f)
            )
            !glyph.isNullOrBlank() -> Text(
                text = glyph,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = strong,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun evolutionToneColor(tone: EvolutionIconTone): Color {
    val roles = evolutionColorRoles()
    return when (tone) {
        EvolutionIconTone.BRAND -> roles.brandPrimary
        EvolutionIconTone.SECONDARY -> roles.brandSecondary
        EvolutionIconTone.FINANCE -> roles.financePositive
        EvolutionIconTone.WARNING -> roles.warningAccent
        EvolutionIconTone.DANGER -> roles.dangerAccent
        EvolutionIconTone.NEUTRAL -> roles.contentSecondary
    }
}

@Composable
fun EvolutionHeroCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconDescription: String,
    semanticTone: EvolutionIconTone,
    supportingText: String?,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable BoxScope.() -> Unit)? = null
) {
    val roles = evolutionColorRoles()
    val semanticColor = evolutionToneColor(semanticTone)
    EvolutionSurface(
        modifier = modifier,
        role = EvolutionSurfaceRole.HERO,
        shape = RoundedCornerShape(appCornerRadius(24.dp)),
        shadowElevation = 3.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(124.dp)
                    .align(Alignment.TopEnd)
                    .background(
                        Brush.radialGradient(
                            listOf(semanticColor.copy(alpha = 0.16f), Color.Transparent)
                        ),
                        CircleShape
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(appScaledSpacing(18.dp)),
                verticalArrangement = Arrangement.spacedBy(appScaledSpacing(10.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(12.dp))
                ) {
                    EvolutionIconTile(
                        icon = icon,
                        contentDescription = iconDescription,
                        tone = semanticTone,
                        size = 46.dp
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = roles.contentSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = semanticColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!supportingText.isNullOrBlank()) {
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodySmall,
                        color = roles.contentSecondary
                    )
                }
            }
            trailingContent?.invoke(this)
        }
    }
}

@Composable
fun EvolutionActionRow(
    title: String,
    subtitle: String? = null,
    trailingValue: String? = null,
    icon: ImageVector? = null,
    glyph: String? = null,
    iconDescription: String? = title,
    tone: EvolutionIconTone = EvolutionIconTone.BRAND,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val roles = evolutionColorRoles()
    EvolutionSurface(
        modifier = modifier.fillMaxWidth(),
        role = EvolutionSurfaceRole.PRIMARY,
        shape = RoundedCornerShape(appCornerRadius(18.dp)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = appHapticAction(onAction = onClick)) else Modifier)
                .padding(horizontal = appScaledSpacing(12.dp), vertical = appScaledSpacing(10.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(12.dp))
        ) {
            EvolutionIconTile(
                icon = icon,
                glyph = glyph,
                contentDescription = iconDescription,
                tone = tone,
                size = 42.dp
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(appScaledSpacing(2.dp))
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = roles.contentPrimary
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = roles.contentSecondary
                    )
                }
            }
            if (!trailingValue.isNullOrBlank()) {
                Text(
                    text = trailingValue,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = evolutionToneColor(tone),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

data class EvolutionTabItem<T>(
    val value: T,
    val label: String
)

@Composable
fun <T> EvolutionTextTabs(
    items: List<EvolutionTabItem<T>>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val roles = evolutionColorRoles()
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        items.forEach { item ->
            val active = item.value == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = appHapticAction { onSelected(item.value) })
                    .padding(top = appScaledSpacing(8.dp)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    color = if (active) roles.brandPrimary else roles.contentSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(appScaledSpacing(7.dp)))
                Box(
                    modifier = Modifier
                        .height(if (active) 3.dp else 1.dp)
                        .fillMaxWidth(0.62f)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (active) roles.brandPrimary else Color.Transparent)
                )
            }
        }
    }
}

data class EvolutionOptionItem<T>(
    val value: T,
    val label: String
)

@Composable
fun <T> EvolutionOptionControl(
    items: List<EvolutionOptionItem<T>>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val roles = evolutionColorRoles()
    EvolutionSurface(
        modifier = modifier,
        role = EvolutionSurfaceRole.SOFT,
        shape = RoundedCornerShape(appCornerRadius(16.dp)),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(appScaledSpacing(4.dp)),
            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(4.dp))
        ) {
            items.forEach { item ->
                val active = item.value == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(appCornerRadius(12.dp)))
                        .background(if (active) roles.surfacePrimary else Color.Transparent)
                        .clickable(onClick = appHapticAction { onSelected(item.value) })
                        .padding(horizontal = appScaledSpacing(8.dp), vertical = appScaledSpacing(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (active) roles.brandPrimary else roles.contentSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

data class EvolutionNavItem<T>(
    val value: T,
    val label: String,
    val icon: ImageVector
)

@Composable
fun <T> EvolutionBottomNavigation(
    items: List<EvolutionNavItem<T>>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    require(items.size == 3) { "Variant A primary navigation requires exactly three destinations" }
    val roles = evolutionColorRoles()
    EvolutionSurface(
        modifier = modifier.fillMaxWidth(),
        role = EvolutionSurfaceRole.FLOATING,
        shape = RoundedCornerShape(appCornerRadius(24.dp)),
        shadowElevation = 7.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = appScaledSpacing(8.dp), vertical = appScaledSpacing(6.dp)),
            horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(4.dp))
        ) {
            items.forEach { item ->
                val active = item.value == selected
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(appCornerRadius(16.dp)))
                        .background(if (active) roles.surfaceAccent else Color.Transparent)
                        .clickable(onClick = appHapticAction { onSelected(item.value) })
                        .padding(vertical = appScaledSpacing(7.dp)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(appScaledSpacing(3.dp))
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (active) roles.brandPrimary else roles.contentSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (active) roles.brandPrimary else roles.contentSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
