package com.friction.shared

object RuleEngine {
    private const val YELLOW_THRESHOLD = 0.75

    fun evaluate(budget: Budget, amount: Double): RiskLevel {
        if (budget.category == Category.CAPRICHO) return RiskLevel.RED
        val projected = budget.spent + amount
        val ratio = if (budget.weeklyLimit <= 0.0) 2.0 else projected / budget.weeklyLimit
        return when {
            ratio > 1.0 -> RiskLevel.RED
            ratio >= YELLOW_THRESHOLD -> RiskLevel.YELLOW
            else -> RiskLevel.GREEN
        }
    }
}