package com.friction.app

import android.content.Context
import com.friction.shared.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray

object TransactionRepository {
    private const val PREFS = "friction_prefs"
    private const val KEY = "transactions"

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions

    private var loaded = false

    @Synchronized
    fun init(context: Context) {
        if (loaded) return
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null)
        if (raw != null) {
            val arr = JSONArray(raw)
            _transactions.value = (0 until arr.length())
                .map { Transaction.fromJson(arr.getString(it)) }
        }
        loaded = true
    }

    @Synchronized
    fun add(context: Context, tx: Transaction) {
        init(context)
        if (_transactions.value.any { it.id == tx.id }) return
        val updated = (_transactions.value + tx).sortedByDescending { it.timestamp }
        _transactions.value = updated
        val arr = JSONArray()
        updated.forEach { arr.put(it.toJson()) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }

    @Synchronized
    fun clear(context: Context) {
        _transactions.value = emptyList()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY).apply()
    }
}