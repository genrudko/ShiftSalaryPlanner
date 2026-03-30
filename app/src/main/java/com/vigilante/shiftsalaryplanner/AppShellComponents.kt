package com.vigilante.shiftsalaryplanner

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
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(appInnerSurfaceColor())
                    .border(1.dp, appPanelBorderColor(), RoundedCornerShape(16.dp))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "←",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

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
    val denseLayout = BottomTab.entries.size >= 6
    NavigationBar(
        containerColor = appPanelColor()
    ) {
        BottomTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                icon = {
                    Text(
                        text = tab.icon,
                        style = if (denseLayout) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium
                    )
                },
                label = {
                    BottomNavLabel(
                        text = tab.label,
                        dense = denseLayout
                    )
                }
            )
        }
    }
}

@Composable
fun AppNavigationRail(
    selectedTab: BottomTab,
    onTabSelected: (BottomTab) -> Unit
) {
    NavigationRail(
        containerColor = appPanelColor(),
        modifier = Modifier.fillMaxHeight()
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        BottomTab.entries.forEach { tab ->
            NavigationRailItem(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                icon = {
                    Text(
                        text = tab.icon,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                label = {
                    BottomNavLabel(
                        text = tab.label,
                        dense = false
                    )
                }
            )
            Spacer(modifier = Modifier.height(6.dp))
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

