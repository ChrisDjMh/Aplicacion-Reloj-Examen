package com.friction.shared

object FrictionScore {
    // 0..100. Cancelar una compra riesgosa da puntaje completo;
    // pagar en amarillo da la mitad y pagar en rojo no da nada.
    fun weekly(transactions: List<Transaction>): Int {
        if (transactions.isEmpty()) return 100
        val points = transactions.sumOf { tx ->
            when {
                tx.cancelled -> 1.0
                tx.risk == RiskLevel.GREEN -> 1.0
                tx.risk == RiskLevel.YELLOW -> 0.5
                else -> 0.0
            }
        }
        return ((points / transactions.size) * 100).toInt().coerceIn(0, 100)
    }
}