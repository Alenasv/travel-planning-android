package com.example.travel_planning.utils

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

object ScreenTransitions {
    val listItemEnter: EnterTransition = fadeIn(
        animationSpec = tween(durationMillis = 300, delayMillis = 100)
    ) + slideInVertically(
        animationSpec = tween(durationMillis = 400),
        initialOffsetY = { it / 2 }
    )

    val listItemExit: ExitTransition = fadeOut(
        animationSpec = tween(durationMillis = 250)
    ) + slideOutVertically(
        animationSpec = tween(durationMillis = 350),
        targetOffsetY = { it / 2 }
    )

    val fabEnter: EnterTransition = slideInVertically(
        animationSpec = tween(durationMillis = 400),
        initialOffsetY = { it }
    )

    val fabExit: ExitTransition = slideOutVertically(
        animationSpec = tween(durationMillis = 300),
        targetOffsetY = { it }
    )

}

@Composable
fun AnimatedFAB(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = ScreenTransitions.fabEnter,
        exit = ScreenTransitions.fabExit,
        modifier = modifier,
        content = content
    )
}

@Composable
fun AnimatedListItem(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = ScreenTransitions.listItemEnter,
        exit = ScreenTransitions.listItemExit,
        modifier = modifier,
        content = content
    )
}