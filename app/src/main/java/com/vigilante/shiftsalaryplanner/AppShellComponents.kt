package com.vigilante.shiftsalaryplanner

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppScreenHeader(
    title: String,
    onBack: () -> Unit,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    actionEnabled: Boolean = true
) {
    BackHandler(onBack = onBack)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs + AppSpacing.xxs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(AppRadius.lg))
                    .background(appInnerSurfaceColor())
                    .border(1.dp, appPanelBorderColor(), RoundedCornerShape(AppRadius.lg))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "←",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(AppSpacing.sm + AppSpacing.xs))

            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (actionText != null && onAction != null) {
                TextButton(
                    onClick = onAction,
                    enabled = actionEnabled
                ) {
                    Text(actionText)
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
    onAction: () -> Unit,
    actionEnabled: Boolean = true
) {
    AppScreenHeader(
        title = title,
        onBack = onBack,
        actionText = actionText,
        onAction = onAction,
        actionEnabled = actionEnabled
    )
}

@Composable
fun AnimatedFullscreenOverlay(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            initialOffsetX = { it / 2 },
            animationSpec = tween(260)
        ) + fadeIn(animationSpec = tween(220)),
        exit = slideOutHorizontally(
            targetOffsetX = { it / 2 },
            animationSpec = tween(240)
        ) + fadeOut(animationSpec = tween(180))
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
    val configuration = LocalConfiguration.current
    val showLabels = configuration.fontScale <= AppTypographyScales.NAV_LABELS_HIDE_THRESHOLD
    val haptic = LocalHapticFeedback.current
    val denseLayout = BottomTab.entries.size >= 6

    NavigationBar(
        containerColor = appPanelColor()
    ) {
        BottomTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTabSelected(tab)
                },
                icon = {
                    TabIconWithHint(
                        tab = tab,
                        showHintOnLongPress = !showLabels,
                        modifier = Modifier.size(if (denseLayout) 22.dp else 24.dp)
                    )
                },
                label = if (showLabels) {
                    {
                        BottomNavLabel(
                            text = tab.label,
                            dense = denseLayout
                        )
                    }
                } else null,
                alwaysShowLabel = showLabels
            )
        }
    }
}

@Composable
fun AppNavigationRail(
    selectedTab: BottomTab,
    onTabSelected: (BottomTab) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    NavigationRail(
        containerColor = appPanelColor(),
        modifier = Modifier.fillMaxHeight()
    ) {
        Spacer(modifier = Modifier.height(AppSpacing.md))
        BottomTab.entries.forEach { tab ->
            NavigationRailItem(
                selected = selectedTab == tab,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTabSelected(tab)
                },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label
                    )
                },
                label = {
                    BottomNavLabel(
                        text = tab.label,
                        dense = false
                    )
                }
            )
            Spacer(modifier = Modifier.height(AppSpacing.xs + AppSpacing.xxs))
        }
    }
}

@Composable
fun BottomNavLabel(
    text: String,
    dense: Boolean
) {
    val fontSize = when {
        dense && text.length >= 10 -> 8.5.sp
        dense -> 9.5.sp
        text.length >= 10 -> 10.sp
        else -> 11.sp
    }

    Text(
        text = text,
        fontSize = fontSize,
        lineHeight = if (dense) 10.sp else 12.sp,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun TabIconWithHint(
    tab: BottomTab,
    showHintOnLongPress: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val pointerModifier = if (showHintOnLongPress) {
        Modifier.pointerInput(tab.label) {
            detectTapGestures(
                onLongPress = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    Toast.makeText(context, tab.label, Toast.LENGTH_SHORT).show()
                }
            )
        }
    } else {
        Modifier
    }

    Icon(
        imageVector = tab.icon,
        contentDescription = tab.label,
        modifier = modifier.then(pointerModifier)
    )
}

