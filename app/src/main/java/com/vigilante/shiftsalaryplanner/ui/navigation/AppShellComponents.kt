package com.vigilante.shiftsalaryplanner

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun AppScreenHeader(
    title: String,
    onBack: () -> Unit,
    actionText: String? = null,
    actionIcon: ImageVector? = null,
    actionContentDescription: String? = null,
    onAction: (() -> Unit)? = null,
    actionEnabled: Boolean = true
) {
    BackHandler(onBack = onBack)
    val iconCorner = appCornerRadius(12.dp)
    val horizontalPadding = appScaledSpacing(14.dp)
    val verticalPadding = appScaledSpacing(10.dp)
    val backButtonSize = if (appIsCompactMode()) 32.dp else 36.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(backButtonSize)
                .clip(RoundedCornerShape(iconCorner))
                .background(appInnerSurfaceColor())
                .border(1.dp, appPanelBorderColor(), RoundedCornerShape(iconCorner))
                .clickable(onClick = appHapticAction(onAction = onBack)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Назад",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.width(appScaledSpacing(12.dp)))

        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (actionText != null && onAction != null) {
            TextButton(
                onClick = appHapticAction(onAction = onAction),
                enabled = actionEnabled,
                modifier = Modifier.appLargeButtonSizing(base = 42.dp)
            ) {
                if (actionIcon != null) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = actionContentDescription ?: actionText
                    )
                } else {
                    Text(
                        text = actionText,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
fun FixedScreenHeader(
    title: String,
    onBack: () -> Unit
) {
    AppScreenHeader(
        title = title,
        onBack = onBack
    )
}

@Composable
fun FixedScreenHeaderAction(
    title: String,
    onBack: () -> Unit,
    actionText: String,
    actionIcon: ImageVector? = null,
    actionContentDescription: String? = null,
    onAction: () -> Unit,
    actionEnabled: Boolean = true
) {
    AppScreenHeader(
        title = title,
        onBack = onBack,
        actionText = actionText,
        actionIcon = actionIcon,
        actionContentDescription = actionContentDescription,
        onAction = onAction,
        actionEnabled = actionEnabled
    )
}

@Composable
fun AnimatedFullscreenOverlay(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    val enterSlide = appAnimationDurationMillis(260)
    val enterFade = appAnimationDurationMillis(220)
    val exitSlide = appAnimationDurationMillis(240)
    val exitFade = appAnimationDurationMillis(180)

    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            initialOffsetX = { it / 2 },
            animationSpec = tween(enterSlide)
        ) + fadeIn(animationSpec = tween(enterFade)),
        exit = slideOutHorizontally(
            targetOffsetX = { it / 2 },
            animationSpec = tween(exitSlide)
        ) + fadeOut(animationSpec = tween(exitFade))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

@Composable
fun AppBottomBar(
    selectedTab: BottomTab,
    onTabSelected: (BottomTab) -> Unit
) {
    val denseLayout = BottomTab.entries.size >= 6
    val showLabels = !appShouldHideBottomBarLabels()
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.onSurface,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = appScaledSpacing(10.dp), vertical = appScaledSpacing(6.dp))
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(appCornerRadius(24.dp)),
            color = appPanelColor().copy(alpha = 0.96f),
            border = BorderStroke(1.dp, appPanelBorderColor().copy(alpha = 0.52f)),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            NavigationBar(
                containerColor = androidx.compose.ui.graphics.Color.Transparent
            ) {
                BottomTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = appHapticAction { onTabSelected(tab) },
                        colors = itemColors,
                        alwaysShowLabel = showLabels,
                        icon = {
                            TabIcon(
                                tab = tab,
                                denseLayout = denseLayout,
                                showLabel = showLabels,
                                selected = selectedTab == tab
                            )
                        },
                        label = if (showLabels) {
                            {
                                BottomNavLabel(
                                    text = tab.label,
                                    dense = denseLayout
                                )
                            }
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AppNavigationRail(
    selectedTab: BottomTab,
    onTabSelected: (BottomTab) -> Unit
) {
    val itemColors = NavigationRailItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.onSurface,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer
    )

    NavigationRail(
        containerColor = appPanelColor(),
        modifier = Modifier.fillMaxHeight()
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        BottomTab.entries.forEach { tab ->
            NavigationRailItem(
                selected = selectedTab == tab,
                onClick = appHapticAction { onTabSelected(tab) },
                colors = itemColors,
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.contentDescription
                    )
                },
                label = {
                    BottomNavLabel(
                        text = tab.label,
                        dense = false
                    )
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabIcon(
    tab: BottomTab,
    denseLayout: Boolean,
    showLabel: Boolean,
    selected: Boolean
) {
    val iconSize = if (denseLayout) 20.dp else 22.dp
    val tooltipState = rememberTooltipState(isPersistent = false)
    val animatedScale = animateFloatAsState(
        targetValue = if (selected) 1.10f else 1f,
        animationSpec = tween(appAnimationDurationMillis(190)),
        label = "tab-icon-scale"
    ).value
    val glowAlpha = animateFloatAsState(
        targetValue = if (selected) 0.20f else 0f,
        animationSpec = tween(appAnimationDurationMillis(190)),
        label = "tab-icon-glow-alpha"
    ).value

    if (showLabel) {
        Box(
            modifier = Modifier.size(iconSize + appScaledSpacing(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha))
            )
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.contentDescription,
                modifier = Modifier
                    .size(iconSize)
                    .scale(animatedScale)
            )
        }
        return
    }

    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip {
                Text(tab.label)
            }
        },
        state = tooltipState
    ) {
        Box(
            modifier = Modifier.size(iconSize + appScaledSpacing(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha))
            )
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.contentDescription,
                modifier = Modifier
                    .size(iconSize)
                    .scale(animatedScale)
            )
        }
    }
}

@Composable
fun BottomNavLabel(
    text: String,
    dense: Boolean
) {
    val fontSize = when {
        dense && text.length >= 10 -> 9.sp
        dense -> 10.sp
        text.length >= 10 -> 10.5.sp
        else -> 11.sp
    }

    Text(
        text = text,
        fontSize = fontSize,
        lineHeight = if (dense) 11.sp else 13.sp,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
