package com.friction.shared

import kotlin.math.ceil
import kotlin.math.round

object KarmaSweep {
    const val DEFAULT_RED_RATE = 0.15 // configurable entre 0.15 y 0.20
    private const val YELLOW_RATE = 0.10

    fun calculate(
        amount: Double,
        risk: RiskLevel,
        redRate: Double = DEFAULT_RED_RATE
    ): Double {
        val raw = when (risk) {
            RiskLevel.GREEN -> ceil(amount) - amount
            RiskLevel.YELLOW -> amount * YELLOW_RATE
            RiskLevel.RED -> amount * redRate.coerceIn(0.15, 0.20)
        }
        return round2(raw)
    }

    private fun round2(v: Double) = round(v * 100.0) / 100.0
}