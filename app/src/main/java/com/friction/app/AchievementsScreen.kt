package com.friction.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.friction.shared.FrictionScore
import com.friction.shared.RiskLevel
import com.friction.shared.Transaction
import java.util.Locale

private data class Achievement(
    val emoji: String,
    val title: String,
    val description: String,
    val current: Double,
    val target: Double,
    val isMoney: Boolean = false
) {
    val unlocked: Boolean get() = current >= target
    val progress: Float get() = (current / target).toFloat().coerceIn(0f, 1f)

    fun progressText(): String =
        if (isMoney) String.format(Locale.US, "$%.2f / $%.0f", current.coerceAtMost(target), target)
        else "${current.toInt().coerceAtMost(target.toInt())} / ${target.toInt()}"
}

private fun longestCleanStreak(txs: List<Transaction>): Int {
    var best = 0
    var cur = 0
    txs.sortedBy { it.timestamp }.forEach { tx ->
        if (tx.cancelled || tx.risk == RiskLevel.GREEN) {
            cur++
            if (cur > best) best = cur
        } else {
            cur = 0
        }
    }
    return best
}

private fun levelFor(unlocked: Int): String = when {
    unlocked >= 9 -> "Wall Street"
    unlocked >= 6 -> "Inversionista"
    unlocked >= 3 -> "Ahorrador"
    else -> "Principiante"
}

@Composable
private fun ALabel(text: String, color: Color, size: TextUnit = 9.sp) {
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
fun AchievementsScreen(txs: List<Transaction>) {
    val context = LocalContext.current
    remember { OrderRepository.init(context); true }
    val orders by OrderRepository.orders.collectAsState()

    val weekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
    val week = txs.filter { it.timestamp >= weekAgo }
    val paid = txs.filter { !it.cancelled }
    val cancelled = txs.filter { it.cancelled }
    val saved = cancelled.sumOf { it.amount }
    val investedTotal = orders.sumOf { it.amount }
    val executions = orders.map { it.timestamp }.distinct().size
    val tickers = orders.map { it.ticker }.distinct().size
    val perfectWeek = if (FrictionScore.weekly(week) == 100) week.size.toDouble() else 0.0

    val achievements = listOf(
        Achievement("🌱", "Primer paso", "Registra tu primer pago", paid.size.toDouble(), 1.0),
        Achievement("🛡️", "Resistencia", "Cancela tu primera compra riesgosa", cancelled.size.toDouble(), 1.0),
        Achievement("🔥", "Racha limpia", "3 movimientos seguidos en verde o cancelados", longestCleanStreak(txs).toDouble(), 3.0),
        Achievement("💸", "Ahorrador", "Ahorra $50 cancelando compras", saved, 50.0, isMoney = true),
        Achievement("🧱", "Disciplinado", "Cancela 5 compras", cancelled.size.toDouble(), 5.0),
        Achievement("📈", "Primera inversión", "Ejecuta tu primera orden de compra", executions.toDouble(), 1.0),
        Achievement("🧩", "Diversificado", "Invierte en 3 ETF distintos", tickers.toDouble(), 3.0),
        Achievement("💰", "Inversionista", "Invierte $10 en total", investedTotal, 10.0, isMoney = true),
        Achievement("🎯", "Semana perfecta", "Score de 100 con al menos 5 movimientos esta semana", perfectWeek, 5.0)
    )
    val unlockedCount = achievements.count { it.unlocked }

    Column(
        Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.padding(start = 3.dp, end = 3.dp, bottom = 5.dp)) {
            ALabel("TU PROGRESO", FColors.Muted)
            Text(
                "Logros",
                color = FColors.Deep,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp,
                fontFamily = AppFont
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(FColors.Dark, FColors.Deep)))
                .padding(20.dp)
        ) {
            ALabel("NIVEL ACTUAL", Color.White.copy(alpha = 0.6f))
            Text(
                levelFor(unlockedCount),
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp,
                fontFamily = AppFont
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "$unlockedCount de ${achievements.size} logros desbloqueados",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 12.sp,
                fontFamily = AppFont
            )
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(unlockedCount / achievements.size.toFloat())
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(Color(0xFF68C2FF))
                )
            }
        }

        achievements.forEach { AchievementCard(it) }
    }
}

@Composable
private fun AchievementCard(a: Achievement) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (a.unlocked) Color(0xFFEAF6EE) else Color(0xFFF7FAF8))
            .border(1.dp, if (a.unlocked) FColors.Green else FColors.Border, shape)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (a.unlocked) FColors.Green else Color(0xFFE3EBE6)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                a.emoji,
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(if (a.unlocked) 1f else 0.35f)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                a.title,
                color = if (a.unlocked) FColors.Deep else Color(0xFF4F6258),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = AppFont
            )
            Text(
                a.description,
                color = FColors.Muted,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontFamily = AppFont
            )
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD6E3DC))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(a.progress)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(if (a.unlocked) FColors.Green else FColors.Blue)
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        if (a.unlocked) {
            Text("✓", color = FColors.Green, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        } else {
            Text(
                a.progressText(),
                color = FColors.Muted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = AppFont
            )
        }
    }
}