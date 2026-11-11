package com.friction.app

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

const val MIN_ORDER = 1.0

data class Order(
    val id: Long,
    val timestamp: Long,
    val ticker: String,
    val shares: Double,
    val price: Double,
    val amount: Double
)

enum class RiskProfile(val label: String, val weights: Map<String, Double>) {
    CONSERVADOR("Conservador", linkedMapOf("BND" to 0.60, "VOO" to 0.30, "VXUS" to 0.10)),
    MODERADO("Moderado", linkedMapOf("VOO" to 0.45, "BND" to 0.25, "QQQ" to 0.15, "VXUS" to 0.15)),
    AGRESIVO("Agresivo / Tech", linkedMapOf("QQQ" to 0.50, "VOO" to 0.35, "VXUS" to 0.15))
}

object MarketSim {
    private const val PREFS = "friction_market"
    private const val KEY = "prices"

    val basePrices: Map<String, Double> = mapOf(
        "VOO" to 520.0,
        "QQQ" to 480.0,
        "BND" to 73.0,
        "VXUS" to 63.0
    )
    private val tickVol = mapOf(
        "VOO" to 0.0010,
        "QQQ" to 0.0016,
        "BND" to 0.0003,
        "VXUS" to 0.0011
    )

    private val _prices = MutableStateFlow(basePrices)
    val prices: StateFlow<Map<String, Double>> = _prices

    private val _vooHistory = MutableStateFlow<List<Double>>(emptyList())
    val vooHistory: StateFlow<List<Double>> = _vooHistory

    private var loaded = false

    @Synchronized
    fun init(context: Context) {
        if (loaded) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null)
            ?.let { raw ->
                val o = JSONObject(raw)
                _prices.value = basePrices.mapValues { (k, v) ->
                    if (o.has(k)) o.getDouble(k) else v
                }
            }
        var p = _prices.value.getValue("VOO")
        val list = ArrayList<Double>()
        list.add(p)
        repeat(59) {
            p /= (1 + Random.nextDouble(-1.0, 1.0) * tickVol.getValue("VOO"))
            list.add(0, p)
        }
        _vooHistory.value = list
        loaded = true
    }

    fun tick(context: Context) {
        val next = _prices.value.mapValues { (k, p) ->
            val v = tickVol.getValue(k)
            (p * (1 + Random.nextDouble(-1.0, 1.0) * v + 0.00002)).coerceAtLeast(1.0)
        }
        _prices.value = next
        _vooHistory.value = (_vooHistory.value + next.getValue("VOO")).takeLast(60)
        val o = JSONObject()
        next.forEach { (k, v) -> o.put(k, v) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, o.toString()).apply()
    }
}

object OrderRepository {
    private const val PREFS = "friction_orders"
    private const val KEY_ORDERS = "orders"
    private const val KEY_PROFILE = "profile"

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders

    private val _profile = MutableStateFlow(RiskProfile.MODERADO)
    val profile: StateFlow<RiskProfile> = _profile

    private var loaded = false

    @Synchronized
    fun init(context: Context) {
        if (loaded) return
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        p.getString(KEY_ORDERS, null)?.let { raw ->
            val arr = JSONArray(raw)
            _orders.value = (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Order(
                    id = o.getLong("id"),
                    timestamp = o.getLong("ts"),
                    ticker = o.getString("ticker"),
                    shares = o.getDouble("shares"),
                    price = o.getDouble("price"),
                    amount = o.getDouble("amount")
                )
            }
        }
        p.getString(KEY_PROFILE, null)?.let { name ->
            _profile.value = RiskProfile.values().firstOrNull { it.name == name }
                ?: RiskProfile.MODERADO
        }
        loaded = true
    }

    fun setProfile(context: Context, profile: RiskProfile) {
        _profile.value = profile
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_PROFILE, profile.name).apply()
    }

    @Synchronized
    fun execute(context: Context, pending: Double, prices: Map<String, Double>) {
        if (pending < MIN_ORDER) return
        val now = System.currentTimeMillis()
        val created = _profile.value.weights.entries.mapIndexed { i, (ticker, w) ->
            val amount = Math.round(pending * w * 100) / 100.0
            val price = prices.getValue(ticker)
            Order(now + i, now, ticker, amount / price, price, amount)
        }
        _orders.value = created + _orders.value
        save(context)
    }

    @Synchronized
    fun clear(context: Context) {
        _orders.value = emptyList()
        save(context)
    }

    private fun save(context: Context) {
        val arr = JSONArray()
        _orders.value.forEach {
            arr.put(
                JSONObject().apply {
                    put("id", it.id)
                    put("ts", it.timestamp)
                    put("ticker", it.ticker)
                    put("shares", it.shares)
                    put("price", it.price)
                    put("amount", it.amount)
                }
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_ORDERS, arr.toString()).apply()
    }
}