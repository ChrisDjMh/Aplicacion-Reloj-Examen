package com.friction.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import com.friction.shared.Transaction

private val TickerColors = mapOf(
    "VOO" to Color(0xFF4DB872),
    "QQQ" to Color(0xFF3B9AE1),
    "BND" to Color(0xFFF59E0B),
    "VXUS" to Color(0xFF8E6CD8)
)

private val TickerNames = mapOf(
    "VOO" to "S&P 500",
    "QQQ" to "Nasdaq 100",
    "BND" to "Bonos EE. UU.",
    "VXUS" to "Mercados internacionales"
)

private data class Position(
    val ticker: String,
    val shares: Double,
    val cost: Double,
    val value: Double
)

private fun usd(v: Double) = String.format(Locale.US, "$%,.2f", v)

private fun signed(v: Double) = (if (v >= 0) "+" else "-") + usd(abs(v))

private fun futureValue(weekly: Double, annualRate: Double, years: Int): Double {
    val rw = annualRate / 52
    val n = 52 * years
    return weekly * ((1 + rw).pow(n) - 1) / rw
}

@Composable
private fun PLabel(text: String, color: Color, size: TextUnit = 9.sp) {
    Text(
        text,
        color = color,
        fontSize = size,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.6.sp,
        fontFamily = AppFont
    )
}

@Composable
private fun Panel(title: String, subtitle: String? = null, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFF7FAF8))
            .border(1.dp, FColors.Border, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PLabel(title, FColors.Muted)
        if (subtitle != null) {
            Text(subtitle, color = Color(0xFF75827A), fontSize = 11.sp, fontFamily = AppFont)
        }
        content()
    }
}

@Composable
private fun LineChart(values: List<Double>, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val min = values.min()
        val max = values.max()
        val range = if (max - min > 1e-9) max - min else 1.0
        val h = size.height.toDouble()
        val stepX = size.width / (values.size - 1)
        val line = Path()
        val fill = Path()
        values.forEachIndexed { i, v ->
            val x = i * stepX
            val y = (h - ((v - min) / range * h * 0.85 + h * 0.075)).toFloat()
            if (i == 0) {
                line.moveTo(x, y)
                fill.moveTo(x, size.height)
                fill.lineTo(x, y)
            } else {
                line.lineTo(x, y)
                fill.lineTo(x, y)
            }
        }
        fill.lineTo(size.width, size.height)
        fill.close()
        drawPath(
            path = fill,
            brush = Brush.verticalGradient(listOf(color.copy(alpha = 0.25f), Color.Transparent))
        )
        drawPath(
            path = line,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
private fun Donut(weights: Map<String, Double>, modifier: Modifier) {
    Canvas(modifier) {
        val stroke = 16.dp.toPx()
        val topLeft = Offset(stroke / 2, stroke / 2)
        val arcSize = Size(size.width - stroke, size.height - stroke)
        var start = -90f
        weights.forEach { (t, w) ->
            val sweep = (w * 360).toFloat()
            drawArc(
                color = TickerColors[t] ?: Color.Gray,
                startAngle = start,
                sweepAngle = sweep - 2f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke)
            )
            start += sweep
        }
    }
}

@Composable
fun PortfolioScreen(txs: List<Transaction>) {
    val context = LocalContext.current
    remember {
        MarketSim.init(context)
        OrderRepository.init(context)
        true
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1500)
            MarketSim.tick(context)
        }
    }

    val prices by MarketSim.prices.collectAsState()
    val history by MarketSim.vooHistory.collectAsState()
    val orders by OrderRepository.orders.collectAsState()
    val profile by OrderRepository.profile.collectAsState()

    val totalSweep = txs.sumOf { it.sweep }
    val executed = orders.sumOf { it.amount }
    val pending = (totalSweep - executed).coerceAtLeast(0.0)
    val canExecute = pending >= MIN_ORDER

    val positions = orders.groupBy { it.ticker }.map { (t, os) ->
        Position(
            ticker = t,
            shares = os.sumOf { it.shares },
            cost = os.sumOf { it.amount },
            value = os.sumOf { it.shares } * (prices[t] ?: 0.0)
        )
    }.sortedByDescending { it.value }
    val value = positions.sumOf { it.value }
    val cost = positions.sumOf { it.cost }
    val pl = value - cost
    val plPct = if (cost > 0) pl / cost * 100 else 0.0

    val first = history.firstOrNull() ?: 0.0
    val last = history.lastOrNull() ?: 0.0
    val chg = if (first > 0) (last / first - 1) * 100 else 0.0
    val up = chg >= 0

    val weekSweep = txs
        .filter { it.timestamp >= System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000 }
        .sumOf { it.sweep }
    var weekly by remember { mutableStateOf(weekSweep.coerceIn(1.0, 100.0).toFloat()) }
    var years by remember { mutableStateOf(10f) }

    Column(
        Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Valor del portafolio
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(FColors.Dark, FColors.Deep)))
                .padding(20.dp)
        ) {
            PLabel("VALOR DEL PORTAFOLIO", Color.White.copy(alpha = 0.6f))
            Text(
                usd(value),
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1.5).sp,
                fontFamily = AppFont
            )
            Text(
                "${if (pl >= 0) "▲" else "▼"} ${signed(pl)} (${String.format(Locale.US, "%+.2f", plPct)}%)",
                color = if (pl >= 0) Color(0xFF7CF0A3) else Color(0xFFFF8A80),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = AppFont
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF7CF0A3)))
                Spacer(Modifier.width(6.dp))
                PLabel("MERCADO SIMULADO · EN VIVO", Color.White.copy(alpha = 0.7f), 8.sp)
            }
        }

        // Gráfica S&P 500
        Panel("S&P 500 · VOO", "Precio simulado, se actualiza cada 1.5 s") {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    usd(last),
                    color = FColors.Deep,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = AppFont
                )
                Text(
                    String.format(Locale.US, "%+.2f%%", chg),
                    color = if (up) FColors.Low else FColors.High,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = AppFont
                )
            }
            LineChart(
                values = history,
                color = if (up) FColors.Low else FColors.High,
                modifier = Modifier.fillMaxWidth().height(120.dp)
            )
        }

        // Smart Vault
        Panel(
            "SMART VAULT",
            "Los redondeos se acumulan y se invierten al llegar a ${usd(MIN_ORDER)}"
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    usd(pending),
                    color = FColors.Deep,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = AppFont
                )
                PLabel("PENDIENTE", FColors.Muted, 8.sp)
            }
            PLabel("PERFIL DE RIESGO", FColors.Muted, 8.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RiskProfile.values().forEach { p ->
                    val sel = p == profile
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(if (sel) FColors.Green else Color.White)
                            .border(1.dp, if (sel) FColors.Green else FColors.Border, CircleShape)
                            .clickable { OrderRepository.setProfile(context, p) }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            p.label,
                            color = if (sel) Color.White else FColors.Dark,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                            fontFamily = AppFont
                        )
                    }
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (canExecute) FColors.Blue else Color(0xFFB8C4BD))
                    .clickable(enabled = canExecute) {
                        OrderRepository.execute(context, pending, prices)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "EJECUTAR ORDEN DE COMPRA",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    fontFamily = AppFont
                )
            }
            if (!canExecute) {
                Text(
                    "Aún no llegas al mínimo para ejecutar una orden.",
                    color = Color(0xFF75827A),
                    fontSize = 11.sp,
                    fontFamily = AppFont
                )
            }
        }

        // Asignación objetivo
        Panel("ASIGNACIÓN OBJETIVO", profile.label) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Donut(profile.weights, Modifier.size(110.dp))
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    profile.weights.forEach { (t, w) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(TickerColors[t] ?: Color.Gray)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    t,
                                    color = FColors.Deep,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = AppFont
                                )
                                Text(
                                    TickerNames[t] ?: "",
                                    color = FColors.Muted,
                                    fontSize = 9.sp,
                                    fontFamily = AppFont
                                )
                            }
                            Text(
                                "${(w * 100).toInt()}%",
                                color = FColors.Deep,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = AppFont
                            )
                        }
                    }
                }
            }
        }

        // Posiciones
        Panel("POSICIONES", "Acciones fraccionadas compradas con tu fricción") {
            if (positions.isEmpty()) {
                Text(
                    "Aún no hay posiciones. Ejecuta tu primera orden.",
                    color = Color(0xFF87948C),
                    fontSize = 12.sp,
                    fontFamily = AppFont
                )
            } else {
                positions.forEach { p ->
                    val ppl = p.value - p.cost
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                p.ticker,
                                color = FColors.Deep,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = AppFont
                            )
                            Text(
                                String.format(Locale.US, "%.6f acciones", p.shares),
                                color = FColors.Muted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                usd(p.value),
                                color = FColors.Deep,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = AppFont
                            )
                            Text(
                                signed(ppl),
                                color = if (ppl >= 0) FColors.Low else FColors.High,
                                fontSize = 10.sp,
                                fontFamily = AppFont
                            )
                        }
                    }
                }
            }
        }

        // Bitácora de órdenes
        Panel("BITÁCORA DE ÓRDENES") {
            if (orders.isEmpty()) {
                Text(
                    "Sin órdenes todavía.",
                    color = Color(0xFF87948C),
                    fontSize = 12.sp,
                    fontFamily = AppFont
                )
            } else {
                orders.take(8).forEach { o ->
                    val time = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                        .format(Date(o.timestamp))
                    Column {
                        Text(
                            "COMPRA ${o.ticker} ${String.format(Locale.US, "%.6f", o.shares)} @ ${usd(o.price)}",
                            color = FColors.Ink,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "$time · ${usd(o.amount)}",
                            color = FColors.Muted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Proyección
        Panel("PROYECCIÓN", "Qué pasaría si inviertes tu fricción de forma constante") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Aporte semanal", color = FColors.Ink, fontSize = 12.sp, fontFamily = AppFont)
                Text(
                    usd(weekly.toDouble()),
                    color = FColors.Deep,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = AppFont
                )
            }
            Slider(
                value = weekly,
                onValueChange = { weekly = it },
                valueRange = 1f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = FColors.Green,
                    activeTrackColor = FColors.Green,
                    inactiveTrackColor = FColors.Border
                )
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Horizonte", color = FColors.Ink, fontSize = 12.sp, fontFamily = AppFont)
                Text(
                    "${years.toInt()} años",
                    color = FColors.Deep,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = AppFont
                )
            }
            Slider(
                value = years,
                onValueChange = { years = it },
                valueRange = 1f..30f,
                colors = SliderDefaults.colors(
                    thumbColor = FColors.Green,
                    activeTrackColor = FColors.Green,
                    inactiveTrackColor = FColors.Border
                )
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total aportado", color = FColors.Muted, fontSize = 12.sp, fontFamily = AppFont)
                Text(
                    usd(weekly.toDouble() * 52 * years.toInt()),
                    color = FColors.Ink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = AppFont
                )
            }
            listOf(0.04, 0.07, 0.10).forEach { r ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "Escenario ${(r * 100).toInt()}% anual",
                        color = FColors.Dark,
                        fontSize = 12.sp,
                        fontFamily = AppFont
                    )
                    Text(
                        usd(futureValue(weekly.toDouble(), r, years.toInt())),
                        color = FColors.Deep,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = AppFont
                    )
                }
            }
        }

        Box(
            Modifier
                .align(Alignment.End)
                .clip(CircleShape)
                .border(1.dp, FColors.Border, CircleShape)
                .clickable { OrderRepository.clear(context) }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("Reiniciar portafolio", color = FColors.Dark, fontSize = 11.sp, fontFamily = AppFont)
        }

        Text(
            "Simulación educativa con precios ficticios. Los escenarios son ilustrativos: " +
                "los rendimientos pasados no garantizan rendimientos futuros.",
            color = Color(0xFF8B9991),
            fontSize = 10.sp,
            lineHeight = 14.sp,
            fontFamily = AppFont
        )
    }
}