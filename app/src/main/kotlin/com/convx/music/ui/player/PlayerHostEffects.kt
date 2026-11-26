/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.ui.player

import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.convx.music.constants.PlayerBackgroundStyle
import com.convx.music.ui.component.BottomSheetState
import com.convx.music.designsystem.MotionTokens
import kotlinx.coroutines.launch

/** Status-bar icon colour and keep-screen-on while the sheet is expanded; skipped for the DIY preview copy. */
@Composable
fun PlayerWindowEffects(
    state: BottomSheetState,
    isPreviewOnly: Boolean,
    playerBackground: PlayerBackgroundStyle,
    useDarkTheme: Boolean,
    keepScreenOn: Boolean,
) {
    val context = LocalContext.current
    DisposableEffect(playerBackground, state.isExpanded, useDarkTheme, keepScreenOn, isPreviewOnly) {
        if (isPreviewOnly) return@DisposableEffect onDispose {}
        val window = (context as? android.app.Activity)?.window
        if (window != null && state.isExpanded) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)

            when (playerBackground) {
                PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT, PlayerBackgroundStyle.GLOW_ANIMATED, PlayerBackgroundStyle.APPLE_MUSIC, PlayerBackgroundStyle.LIVE_MESH, PlayerBackgroundStyle.FLUID_MESH, PlayerBackgroundStyle.STATIC, PlayerBackgroundStyle.CUSTOM_GRADIENT, PlayerBackgroundStyle.GLASS -> {
                    insetsController.isAppearanceLightStatusBars = false
                }
                PlayerBackgroundStyle.DEFAULT -> {
                    insetsController.isAppearanceLightStatusBars = !useDarkTheme
                }
            }

            if (keepScreenOn && state.isExpanded)
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        onDispose {
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = !useDarkTheme
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
}

/** Wires this sheet's drag and landing into the shared container morph; skipped for the DIY preview copy. */
@Composable
fun PlayerMorphHostEffects(
    state: BottomSheetState,
    isPreviewOnly: Boolean,
    styleKey: Any? = null,
) {
    val landingScope = rememberCoroutineScope()
    val landingDensity = LocalDensity.current

    // Reset transient landing/layer flags whenever player style/look switches so touch routing never stalls.
    LaunchedEffect(styleKey, isPreviewOnly) {
        if (!isPreviewOnly) {
            landingGeneration++
            PlayerMorph.landingActive = false
            clearMiniContentControlsRects()
        }
    }

    DisposableEffect(state, isPreviewOnly) {
        if (isPreviewOnly) return@DisposableEffect onDispose {}
        PlayerMorph.progressProvider = { state.progress }
        PlayerMorph.onDragStart = { fromMini -> state.onDragStart(fromMini) }
        PlayerMorph.onDragDelta = { state.dispatchRawDelta(it) }
        PlayerMorph.onDragEnd = { state.performFling(it, null) }
        PlayerMorph.onDragCancel = { state.performFling(0f, null) }
        state.onCollapseArmed = { armed ->
            if (armed && PlayerMorph.sharedLayersReady) {
                landingGeneration++
                PlayerMorph.landingActive = true
            } else if (!armed) {
                PlayerMorph.landingActive = false
            }
        }
        state.onCollapseLanded = { velocityDp ->
            landingScope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
                playPillLanding(velocityDp, state, landingDensity)
            }
        }
        onDispose {
            PlayerMorph.progressProvider = { 0f }
            PlayerMorph.onDragStart = {}
            PlayerMorph.onDragDelta = {}
            PlayerMorph.onDragEnd = {}
            PlayerMorph.onDragCancel = {}
            state.onCollapseLanded = null
            state.onCollapseArmed = null
            PlayerMorph.landingActive = false
        }
    }
}

internal suspend fun playPillLanding(
    sheetVelocityDp: Float,
    state: BottomSheetState,
    density: androidx.compose.ui.unit.Density,
) {
    val generation = landingGeneration
    val pill = miniPlayerContainerRect
    val rangeDp = (state.expandedBound - state.collapsedBound).value
    val progressVelocity = if (rangeDp > 0f) sheetVelocityDp / rangeDp else 0f
    if (pill == null || !PlayerMorph.landingActive || progressVelocity >= 0f) {
        if (landingGeneration == generation) PlayerMorph.landingActive = false
        return
    }
    val fullHeightPx = with(density) { state.expandedBound.toPx() }
    PlayerMorph.landingTravelPx = pill.center.y - fullHeightPx / 2f
    PlayerMorph.landingMaxDipPx = with(density) { LANDING_MAX_DIP.toPx() }
    try {
        PlayerMorph.landing.snapTo(0f)
        PlayerMorph.landing.animateTo(
            targetValue = 0f,
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = LANDING_DAMPING,
                stiffness = LANDING_STIFFNESS,
            ),
            initialVelocity = progressVelocity,
        )
    } finally {
        if (landingGeneration == generation) PlayerMorph.landingActive = false
    }
}

internal var landingGeneration = 0

private const val LANDING_DAMPING = MotionTokens.PlayerLandingDamping
private const val LANDING_STIFFNESS = MotionTokens.PlayerLandingStiffness
private val LANDING_MAX_DIP = MotionTokens.PlayerLandingMaxDipDp.dp
