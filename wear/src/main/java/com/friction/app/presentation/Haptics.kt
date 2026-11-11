package com.friction.app.presentation

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import com.friction.shared.RiskLevel

object Haptics {

    @Suppress("DEPRECATION")
    fun play(context: Context, risk: RiskLevel) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (!vibrator.hasVibrator()) return

        val (timings, amplitudes) = when (risk) {
            // Click sutil
            RiskLevel.GREEN -> Pair(
                longArrayOf(0, 40),
                intArrayOf(0, 120)
            )
            // "Viscosidad": pulsos cortos y densos
            RiskLevel.YELLOW -> Pair(
                longArrayOf(0, 70, 40, 70, 40, 70, 40, 70),
                intArrayOf(0, 200, 0, 200, 0, 200, 0, 200)
            )
            // "Freno de mano": vibración larga y creciente
            RiskLevel.RED -> Pair(
                longArrayOf(0, 300, 40, 400, 40, 900),
                intArrayOf(0, 140, 0, 200, 0, 255)
            )
        }

        val effect = if (vibrator.hasAmplitudeControl()) {
            VibrationEffect.createWaveform(timings, amplitudes, -1)
        } else {
            VibrationEffect.createWaveform(timings, -1)
        }
        vibrator.vibrate(effect)
    }
}