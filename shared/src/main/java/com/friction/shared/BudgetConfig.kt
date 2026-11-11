package com.friction.shared

import org.json.JSONObject

object BudgetConfig {
    val defaults: Map<Category, Double> = mapOf(
        Category.NECESARIO to 100.0,
        Category.CAFE to 40.0,
        Category.ENTRETENIMIENTO to 60.0,
        Category.COMPRAS to 80.0,
        Category.CAPRICHO to 30.0
    )

    fun toJson(limits: Map<Category, Double>): String =
        JSONObject().apply { limits.forEach { (c, v) -> put(c.name, v) } }.toString()

    fun fromJson(json: String): Map<Category, Double> {
        val o = JSONObject(json)
        return Category.values().associateWith { c ->
            if (o.has(c.name)) o.getDouble(c.name) else defaults.getValue(c)
        }
    }
}