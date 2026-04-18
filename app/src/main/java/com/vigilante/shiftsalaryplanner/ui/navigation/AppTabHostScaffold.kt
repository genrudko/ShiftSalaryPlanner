package com.vigilante.shiftsalaryplanner

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun AppTabHostScaffold(
    isLandscape: Boolean,
    selectedTab: BottomTab,
    onTabSelected: (BottomTab) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHost: @Composable () -> Unit = {},
    tabContent: @Composable (BottomTab) -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = snackbarHost,
        bottomBar = {
            if (!isLandscape) {
                AppBottomBar(
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                if (isLandscape) {
                    AppNavigationRail(
                        selectedTab = selectedTab,
                        onTabSelected = onTabSelected
                    )
                }

                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        val forward = targetState.ordinal > initialState.ordinal
                        if (forward) {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(260)
                            ) + fadeIn(animationSpec = tween(240)) + scaleIn(
                                initialScale = 0.98f,
                                animationSpec = tween(220)
                            ) togetherWith
                                slideOutHorizontally(
                                    targetOffsetX = { -it },
                                    animationSpec = tween(240)
                                ) + fadeOut(animationSpec = tween(180)) + scaleOut(
                                targetScale = 0.99f,
                                animationSpec = tween(180)
                            )
                        } else {
                            slideInHorizontally(
                                initialOffsetX = { -it },
                                animationSpec = tween(260)
                            ) + fadeIn(animationSpec = tween(240)) + scaleIn(
                                initialScale = 0.98f,
                                animationSpec = tween(220)
                            ) togetherWith
                                slideOutHorizontally(
                                    targetOffsetX = { it },
                                    animationSpec = tween(240)
                                ) + fadeOut(animationSpec = tween(180)) + scaleOut(
                                targetScale = 0.99f,
                                animationSpec = tween(180)
                            )
                        }
                    },
                    label = "tab_content",
                    modifier = Modifier.weight(1f)
                ) { tab ->
                    tabContent(tab)
                }
            }
        }
    }
}
