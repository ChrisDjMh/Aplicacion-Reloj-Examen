package com.friction.shared

object FrictionEngine {
    fun process(
        budget: Budget,
        amount: Double,
        redRate: Double = KarmaSweep.DEFAULT_RED_RATE,
        now: Long = System.currentTimeMillis()
    ): PaymentResult {
        val risk = RuleEngine.evaluate(budget, amount)
        val sweep = KarmaSweep.calculate(amount, risk, redRate)
        val tx = Transaction(
            id = now,
            category = budget.category,
            amount = amount,
            risk = risk,
            sweep = sweep,
            timestamp = now
        )
        return PaymentResult(tx, budget.copy(spent = budget.spent + amount))
    }

    // Compra cancelada: no suma al gasto del presupuesto ni genera inversión.
    fun cancel(
        budget: Budget,
        amount: Double,
        now: Long = System.currentTimeMillis()
    ): Transaction {
        val risk = RuleEngine.evaluate(budget, amount)
        return Transaction(
            id = now,
            category = budget.category,
            amount = amount,
            risk = risk,
            sweep = 0.0,
            timestamp = now,
            cancelled = true
        )
    }
}