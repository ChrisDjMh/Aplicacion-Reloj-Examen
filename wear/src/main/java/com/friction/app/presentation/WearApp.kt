package com.friction.app.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.friction.shared.Budget
import com.friction.shared.Category
import com.friction.shared.FrictionEngine
import com.friction.shared.KarmaSweep
import com.friction.shared.PaymentResult
import com.friction.shared.RiskLevel
import com.friction.shared.RuleEngine
import com.friction.shared.Transaction
import java.util.Locale

private object WColors {
    val Green = Color(0xFF4DB872)
    val Dark = Color(0xFF1F7A4D)
    val Deep = Color(0xFF14532D)
    val Night = Color(0xFF0E3A21)
    val Blue = Color(0xFF3B9AE1)
    val Soft = Color(0xFFF1F7F3)
    val Muted = Color(0xFF6B7C73)
}

private sealed interface Screen {
    data object Pay : Screen
    data class Confirm(val category: Category, val amount: Double, val risk: RiskLevel) : Screen
    data class Paid(val result: PaymentResult) : Screen
    data class Saved(val tx: Transaction) : Screen
}

private fun money(v: Double) = String.format(Locale.US, "$%.2f", v)

private fun riskBrush(risk: RiskLevel) = when (risk) {
    RiskLevel.GREEN -> Brush.verticalGradient(listOf(Color(0xFF56C17A), Color(0xFF3BA762)))
    RiskLevel.YELLOW -> Brush.verticalGradient(listOf(Color(0xFFF5A623), Color(0xFFB97A06)))
    RiskLevel.RED -> Brush.verticalGradient(listOf(Color(0xFFE04545), Color(0xFFB3261E)))
}

private fun riskText(risk: RiskLevel) = when (risk) {
    RiskLevel.GREEN -> "DENTRO DEL PRESUPUESTO"
    RiskLevel.YELLOW -> "CERCA DEL LÍMITE"
    RiskLevel.RED -> "LÍMITE SUPERADO"
}

@Composable
private fun SmallLabel(text: String, color: Color, size: TextUnit = 9.sp) {
    Text(
        text,
        color = color,
        fontSize = size,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.4.sp,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun Pill(text: String, bg: Color, fg: Color, outlined: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth(0.9f)
            .height(38.dp)
            .clip(CircleShape)
            .then(if (outlined) Modifier.border(1.5.dp, fg, CircleShape) else Modifier.background(bg))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
    }
}

@Composable
private fun RoundButton(symbol: String, onClick: () -> Unit) {
    Box(
        Modifier.size(36.dp).clip(CircleShape).background(WColors.Blue).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun WearApp() {
    val context = LocalContext.current
    remember { BudgetStore.init(context) }
    val limits by BudgetStore.limits.collectAsState()

    val budgets = remember {
        mutableStateMapOf<Category, Budget>().apply {
            BudgetStore.limits.value.forEach { (c, l) -> put(c, Budget(c, l)) }
        }
    }
    LaunchedEffect(limits) {
        limits.forEach { (c, l) ->
            budgets[c] = Budget(c, l, budgets[c]?.spent ?: 0.0)
        }
    }

    val history = remember { mutableStateListOf<Transaction>() }
    var category by remember { mutableStateOf(Category.CAFE) }
    var amount by remember { mutableStateOf(5.0) }
    var screen by remember { mutableStateOf<Screen>(Screen.Pay) }

    fun completePayment(cat: Category, amt: Double, playHaptic: Boolean) {
        val res = FrictionEngine.process(budgets.getValue(cat), amt)
        budgets[cat] = res.updatedBudget
        history.add(res.transaction)
        if (playHaptic) Haptics.play(context, res.transaction.risk)
        PhoneSync.sendTransaction(context, res.transaction)
        screen = Screen.Paid(res)
    }

    fun cancelPayment(cat: Category, amt: Double) {
        val tx = FrictionEngine.cancel(budgets.getValue(cat), amt)
        history.add(tx)
        Haptics.play(context, RiskLevel.GREEN)
        PhoneSync.sendTransaction(context, tx)
        screen = Screen.Saved(tx)
    }

    MaterialTheme {
        when (val s = screen) {
            Screen.Pay -> PayScreen(
                budget = budgets.getValue(category),
                amount = amount,
                onCategoryClick = {
                    val all = Category.values()
                    category = all[(category.ordinal + 1) % all.size]
                },
                onAmountChange = { delta ->
                    amount = (amount + delta).coerceIn(1.0, 500.0)
                },
                onPay = {
                    val risk = RuleEngine.evaluate(budgets.getValue(category), amount)
                    if (risk == RiskLevel.GREEN) {
                        completePayment(category, amount, true)
                    } else {
                        // La "resistencia" se siente antes de decidir
                        Haptics.play(context, risk)
                        screen = Screen.Confirm(category, amount, risk)
                    }
                }
            )

            is Screen.Confirm -> ConfirmScreen(
                c = s,
                onCancel = { cancelPayment(s.category, s.amount) },
                onPayAnyway = { completePayment(s.category, s.amount, false) }
            )

            is Screen.Paid -> ResultScreen(result = s.result, onDone = { screen = Screen.Pay })

            is Screen.Saved -> SavedScreen(tx = s.tx, onDone = { screen = Screen.Pay })
        }
    }
}

@Composable
private fun PayScreen(
    budget: Budget,
    amount: Double,
    onCategoryClick: () -> Unit,
    onAmountChange: (Double) -> Unit,
    onPay: () -> Unit
) {
    val remaining = (budget.weeklyLimit - budget.spent).coerceAtLeast(0.0)
    val used = if (budget.weeklyLimit > 0) (budget.spent / budget.weeklyLimit).toFloat().coerceIn(0f, 1f) else 1f

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(WColors.Dark, WColors.Night)))
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = rememberScalingLazyListState(),
            contentPadding = PaddingValues(top = 26.dp, bottom = 36.dp, start = 8.dp, end = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { SmallLabel("FRICTION", WColors.Green, 11.sp) }
            item {
                Column(
                    Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(WColors.Soft)
                        .clickable(onClick = onCategoryClick)
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Text(
                        budget.category.label,
                        color = WColors.Deep,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text("Disponible ${money(remaining)}", color = WColors.Muted, fontSize = 10.sp)
                    Spacer(Modifier.height(5.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD6E3DC))
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(used)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(WColors.Blue)
                        )
                    }
                }
            }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RoundButton("−") { onAmountChange(-1.0) }
                    Text(money(amount), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    RoundButton("+") { onAmountChange(1.0) }
                }
            }
            item { Pill("SIMULAR PAGO", WColors.Blue, Color.White, onClick = onPay) }
        }
    }
}

@Composable
private fun ConfirmScreen(c: Screen.Confirm, onCancel: () -> Unit, onPayAnyway: () -> Unit) {
    val sweep = KarmaSweep.calculate(c.amount, c.risk)
    val msg = when {
        c.category == Category.CAPRICHO -> "Es un gasto de capricho"
        c.risk == RiskLevel.RED -> "Superarías tu límite semanal"
        else -> "Estás cerca de tu límite semanal"
    }
    Box(
        Modifier.fillMaxSize().background(riskBrush(c.risk)),
        contentAlignment = Alignment.Center
    ) {
        ScalingLazyColumn(
            contentPadding = PaddingValues(top = 26.dp, bottom = 36.dp, start = 10.dp, end = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item { SmallLabel("¿SEGURO?", Color.White.copy(alpha = 0.85f), 11.sp) }
            item {
                Text(
                    money(c.amount),
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                Text(
                    msg,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
            item {
                Text(
                    "Si pagas, se invierten +${money(sweep)}",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
            item { Spacer(Modifier.height(4.dp)) }
            item { Pill("CANCELAR", Color.White, WColors.Deep, onClick = onCancel) }
            item {
                Pill("PAGAR IGUAL", Color.Transparent, Color.White, outlined = true, onClick = onPayAnyway)
            }
        }
    }
}

@Composable
private fun SavedScreen(tx: Transaction, onDone: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(riskBrush(RiskLevel.GREEN)),
        contentAlignment = Alignment.Center
    ) {
        ScalingLazyColumn(
            contentPadding = PaddingValues(top = 26.dp, bottom = 36.dp, start = 10.dp, end = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item { SmallLabel("¡BUEN TRABAJO!", Color.White.copy(alpha = 0.85f), 10.sp) }
            item { SmallLabel("AHORRASTE", Color.White.copy(alpha = 0.65f), 8.sp) }
            item {
                Text(
                    money(tx.amount),
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            item { Text("Compra cancelada", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp) }
            item {
                Text(
                    "Tu score de fricción sube",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
            item { Spacer(Modifier.height(4.dp)) }
            item { Pill("LISTO", Color.Transparent, Color.White, outlined = true, onClick = onDone) }
        }
    }
}

@Composable
private fun ResultScreen(result: PaymentResult, onDone: () -> Unit) {
    val tx = result.transaction
    Box(
        Modifier.fillMaxSize().background(riskBrush(tx.risk)),
        contentAlignment = Alignment.Center
    ) {
        ScalingLazyColumn(
            contentPadding = PaddingValues(top = 26.dp, bottom = 36.dp, start = 10.dp, end = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item { SmallLabel(riskText(tx.risk), Color.White.copy(alpha = 0.85f), 9.sp) }
            item { SmallLabel("FRICCIÓN", Color.White.copy(alpha = 0.65f), 8.sp) }
            item {
                Text(
                    "+${money(tx.sweep)}",
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            item { Text("Gasto: ${money(tx.amount)}", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp) }
            item {
                Text(
                    "Enviado a ETF S&P 500",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
            item { Spacer(Modifier.height(4.dp)) }
            item { Pill("LISTO", Color.Transparent, Color.White, outlined = true, onClick = onDone) }
        }
    }
}