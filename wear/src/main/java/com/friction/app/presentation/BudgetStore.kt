package com.friction.app.presentation

import android.content.Context
import com.friction.shared.BudgetConfig
import com.friction.shared.Category
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object BudgetStore {
    private const val PREFS = "friction_wear_prefs"
    private const val KEY = "budgets"

    private val _limits = MutableStateFlow(BudgetConfig.defaults)
    val limits: StateFlow<Map<Category, Double>> = _limits

    private var loaded = false

    @Synchronized
    fun init(context: Context) {
        if (loaded) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null)
            ?.let { _limits.value = BudgetConfig.fromJson(it) }
        loaded = true
    }

    @Synchronized
    fun update(context: Context, newLimits: Map<Category, Double>) {
        init(context)
        _limits.value = newLimits
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, BudgetConfig.toJson(newLimits)).apply()
    }
}