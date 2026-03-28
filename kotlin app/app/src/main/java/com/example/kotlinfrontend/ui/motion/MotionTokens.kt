package com.example.kotlinfrontend.ui.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object MotionTokens {
    const val Quick = 180
    const val Standard = 240
    const val Relaxed = 320

    val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val GentleEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

    fun <T> quickTween(): FiniteAnimationSpec<T> = tween(
        durationMillis = Quick,
        easing = GentleEasing
    )

    fun <T> standardTween(): FiniteAnimationSpec<T> = tween(
        durationMillis = Standard,
        easing = EmphasizedEasing
    )

    fun <T> relaxedTween(): FiniteAnimationSpec<T> = tween(
        durationMillis = Relaxed,
        easing = GentleEasing
    )

    fun <T> bouncySpring(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
}
