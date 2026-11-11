package com.friction.app

import android.content.Context
import com.friction.shared.BudgetConfig
import com.friction.shared.Category
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object BudgetRepository {
    private const val PREFS = "friction_prefs"
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

    fun save(context: Context, newLimits: Map<Category, Double>) {
        _limits.value = newLimits
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, BudgetConfig.toJson(newLimits)).apply()
        push(context)
    }

    fun push(context: Context) {
        val request = PutDataMapRequest.create("/budgets").apply {
            dataMap.putString("json", BudgetConfig.toJson(_limits.value))
            dataMap.putLong("ts", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        Wearable.getDataClient(context).putDataItem(request)
    }
}