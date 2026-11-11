package com.friction.shared

import org.json.JSONObject

enum class Category(val label: String) {
    NECESARIO("Necesarios"),
    CAFE("Café/Restaurantes"),
    ENTRETENIMIENTO("Entretenimiento"),
    COMPRAS("Compras"),
    CAPRICHO("Capricho")
}

enum class RiskLevel { GREEN, YELLOW, RED }

data class Budget(
    val category: Category,
    val weeklyLimit: Double,
    val spent: Double = 0.0
) {
    val usedRatio: Double
        get() = if (weeklyLimit <= 0.0) 1.0 else spent / weeklyLimit
}

data class Transaction(
    val id: Long,
    val category: Category,
    val amount: Double,
    val risk: RiskLevel,
    val sweep: Double,
    val timestamp: Long,
    val cancelled: Boolean = false
) {
    fun toJson(): String = JSONObject().apply {
        put("id", id)
        put("category", category.name)
        put("amount", amount)
        put("risk", risk.name)
        put("sweep", sweep)
        put("timestamp", timestamp)
        put("cancelled", cancelled)
    }.toString()

    companion object {
        fun fromJson(json: String): Transaction {
            val o = JSONObject(json)
            return Transaction(
                id = o.getLong("id"),
                category = Category.valueOf(o.getString("category")),
                amount = o.getDouble("amount"),
                risk = RiskLevel.valueOf(o.getString("risk")),
                sweep = o.getDouble("sweep"),
                timestamp = o.getLong("timestamp"),
                cancelled = o.optBoolean("cancelled", false)
            )
        }
    }
}

data class PaymentResult(
    val transaction: Transaction,
    val updatedBudget: Budget
)