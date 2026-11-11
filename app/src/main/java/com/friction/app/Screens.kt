package com.friction.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.friction.shared.Budget
import com.friction.shared.Category
import com.friction.shared.FrictionEngine
import com.friction.shared.FrictionScore
import com.friction.shared.RiskLevel
import com.friction.shared.Transaction
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.text.style.TextDecoration

private enum class AppView { WELCOME, SUMMARY, BUDGETS, PORTFOLIO, ACHIEVEMENTS }

private fun money(v: Double) = String.format(Locale.US, "$%.2f", v)

private fun weekStart() = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000

private fun riskColor(r: RiskLevel) = when (r) {
    RiskLevel.GREEN -> FColors.Low
    RiskLevel.YELLOW -> FColors.Medium
    RiskLevel.RED -> FColors.High
}

@Composable
private fun Label(text: String, color: Color, size: TextUnit = 9.sp) {
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
fun AppRoot() {
    val context = LocalContext.current
    val txs by TransactionRepository.transactions.collectAsState()
    val limits by BudgetRepository.limits.collectAsState()

    var view by remember { mutableStateOf(AppView.WELCOME) }
    var toast by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }

    LaunchedEffect(toast) {
        if (toast.isNotEmpty()) {
            delay(2200)
            toast = ""
        }
    }

    Box(Modifier.fillMaxSize().background(Color.White)) {
        if (view == AppView.WELCOME) {
            WelcomeScreen(
                onNewExpense = { view = AppView.SUMMARY; showAdd = true },
                onAchievements = { view = AppView.ACHIEVEMENTS }
                
            )
        } else {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                AppHeader(onHome = { view = AppView.WELCOME })
                Tabs(active = view, onChange = { view = it })
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    when (view) {
                        AppView.SUMMARY -> SummaryScreen(
                            txs = txs,
                            onClear = { TransactionRepository.clear(context) },
                            onAdd = { showAdd = true }
                        )
                        AppView.BUDGETS -> BudgetsScreen(
                            txs = txs,
                            limits = limits,
                            onSaved = { newLimits ->
                                BudgetRepository.save(context, newLimits)
                                toast = "Presupuesto guardado y enviado al reloj"
                            }
                        )
                        AppView.ACHIEVEMENTS -> AchievementsScreen(txs = txs)
                        else -> PortfolioScreen(txs = txs)
                    }
                }
            }
        }

        if (toast.isNotEmpty()) {
            Box(
                Modifier.fillMaxSize().navigationBarsPadding().padding(bottom = 22.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF123B26))
                        .padding(horizontal = 16.dp, vertical = 11.dp)
                ) {
                    Text(toast, color = Color.White, fontSize = 12.sp, fontFamily = AppFont)
                }
            }
        }

        if (showAdd) {
            AddExpenseDialog(
                limits = limits,
                txs = txs,
                onDismiss = { showAdd = false },
                onAdd = { tx ->
                    TransactionRepository.add(context, tx)
                    showAdd = false
                    toast = "Gasto añadido e inversión calculada"
                }
            )
        }
    }
}

@Composable
private fun WelcomeScreen(onNewExpense: () -> Unit, onAchievements: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF56C17A), Color(0xFF47B76D), Color(0xFF3BA762))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 26.dp, vertical = 28.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Friction",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = AppFont
            )
            Label("DE FRICCIÓN A RIQUEZA", Color.White.copy(alpha = 0.76f), 8.sp)
        }
        Spacer(Modifier.height(56.dp))
        Label("BIENVENIDO DE NUEVO", Color.White.copy(alpha = 0.7f), 10.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "Hola,\n$USER_NAME",
            color = Color.White,
            fontSize = 46.sp,
            lineHeight = 48.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1.5).sp,
            fontFamily = AppFont
        )
        Box(
            Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.ic_piggy),
                contentDescription = "Cochinito con una planta brotando",
                modifier = Modifier.size(232.dp),
                alpha = 0.92f
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(FColors.Deep)
                .clickable(onClick = onNewExpense),
            contentAlignment = Alignment.Center
        ) {
            Label("NUEVO GASTO", Color.White, 11.sp)
        }
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(15.dp))
                .border(1.5.dp, Color.White.copy(alpha = 0.86f), RoundedCornerShape(15.dp))
                .clickable(onClick = onAchievements),
            contentAlignment = Alignment.Center
        ) {
            Label("LOGROS", Color.White, 11.sp)
        }
    }
}

@Composable
private fun IconBox(icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(FColors.Soft)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = FColors.Deep, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun AppHeader(onHome: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 17.dp, end = 17.dp, top = 12.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBox(Icons.Filled.Menu) {}
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Label("DE FRICCIÓN A RIQUEZA", FColors.Green, 8.sp)
            Text(
                "Friction",
                color = FColors.Deep,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = AppFont
            )
        }
        IconBox(Icons.Filled.Home, onHome)
    }
}

@Composable
private fun Tabs(active: AppView, onChange: (AppView) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 5.dp, bottom = 16.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(Color(0xFFF7FAF8))
            .border(1.dp, FColors.Border, RoundedCornerShape(17.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        listOf(
            AppView.SUMMARY to "Resumen",
            AppView.PORTFOLIO to "Portafolio",
            AppView.BUDGETS to "Presupuestos"
        ).forEach { (v, label) ->
            val sel = active == v
            Box(
                Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (sel) FColors.Green else Color.Transparent)
                    .clickable { onChange(v) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (sel) Color.White else FColors.Dark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    fontFamily = AppFont
                )
            }
        }
    }
}

@Composable
private fun ScoreRing(score: Int) {
    Box(Modifier.size(108.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 7.dp.toPx()
            val topLeft = Offset(stroke / 2, stroke / 2)
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = Color.White.copy(alpha = 0.24f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke)
            )
            drawArc(
                color = Color(0xFF68C2FF),
                startAngle = -90f,
                sweepAngle = 360f * score / 100f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$score%",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = AppFont
            )
            Label("FRICCIÓN", Color.White.copy(alpha = 0.65f), 8.sp)
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Column {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(label, color = Color.White.copy(alpha = 0.76f), fontSize = 11.sp, fontFamily = AppFont)
            Text(
                value,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = AppFont
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.18f)))
    }
}

@Composable
private fun SummaryScreen(txs: List<Transaction>, onClear: () -> Unit, onAdd: () -> Unit) {
    val week = txs.filter { it.timestamp >= weekStart() }
    val invested = txs.sumOf { it.sweep }
    val spent = week.filter { !it.cancelled }.sumOf { it.amount }
    val saved = week.filter { it.cancelled }.sumOf { it.amount }
    val score = FrictionScore.weekly(week)

    Column(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .padding(horizontal = 20.dp)
                    .padding(top = 3.dp, bottom = 20.dp)
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDFE7E3))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(score / 100f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(FColors.Blue)
                )
            }
            Label("INVERTIDO EN ETF S&P 500", FColors.Muted, 9.sp)
            Text(
                money(invested),
                color = FColors.Deep,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-2).sp,
                fontFamily = AppFont
            )
            Text("Tu dinero ya está trabajando", color = Color(0xFF8B9991), fontSize = 11.sp, fontFamily = AppFont)
        }

        Column(
            Modifier
                .padding(horizontal = 14.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF51BD76), Color(0xFF3FAB65))))
                .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 18.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Label("ESTA SEMANA", Color.White.copy(alpha = 0.65f), 9.sp)
                    Text(
                        "RESUMEN",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp,
                        fontFamily = AppFont
                    )
                }
                Icon(Icons.Filled.Star, null, tint = FColors.Deep, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(13.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    StatRow("Gastado", money(spent))
                    StatRow("Ahorrado", money(saved))
                    StatRow("Score de fricción", "$score / 100")
                    StatRow("Movimientos", "${week.size}")
                }
                Spacer(Modifier.width(12.dp))
                ScoreRing(score)
            }
        }

        Column(
            Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 24.dp, bottom = 28.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 0.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Label("HISTORIAL RECIENTE", FColors.Muted, 9.sp)
                    Text(
                        "MOVIMIENTOS",
                        color = FColors.Deep,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp,
                        fontFamily = AppFont
                    )
                }
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(FColors.Blue).clickable(onClick = onAdd),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", color = Color.White, fontSize = 22.sp, fontFamily = AppFont)
                }
            }
            Spacer(Modifier.height(9.dp))
            Box(Modifier.fillMaxWidth().height(2.dp).background(FColors.Green))

            if (txs.isEmpty()) {
                Text(
                    "Aún no hay movimientos. Simula un pago desde el reloj.",
                    color = Color(0xFF87948C),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    fontFamily = AppFont,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 34.dp)
                )
            } else {
                txs.take(20).forEach { MovementRow(it) }
            }

            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .align(Alignment.End)
                    .clip(CircleShape)
                    .background(FColors.Blue)
                    .clickable(onClick = onClear)
                    .padding(horizontal = 19.dp, vertical = 8.dp)
            ) {
                Text("Borrar", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppFont)
            }
        }
    }
}

@Composable
private fun MovementRow(tx: Transaction) {
    val time = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(tx.timestamp))
    Column {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 3.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    tx.category.label,
                    color = Color(0xFF193E2A),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = AppFont
                )
                Text(time, color = Color(0xFF87948C), fontSize = 10.sp, fontFamily = AppFont)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    money(tx.amount),
                    color = if (tx.cancelled) Color(0xFF87948C) else Color(0xFF193E2A),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (tx.cancelled) TextDecoration.LineThrough else null,
                    fontFamily = AppFont
                )
                Text(
                    if (tx.cancelled) "Cancelado · ahorraste ${money(tx.amount)}"
                    else "+${money(tx.sweep)} invertido",
                    color = if (tx.cancelled) FColors.Blue else riskColor(tx.risk),
                    fontSize = 10.sp,
                    fontFamily = AppFont
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE7EDE9)))
    }
}

@Composable
private fun BudgetsScreen(
    txs: List<Transaction>,
    limits: Map<Category, Double>,
    onSaved: (Map<Category, Double>) -> Unit
) {
    val fields = remember {
        mutableStateMapOf<Category, String>().apply {
            limits.forEach { (c, v) -> put(c, String.format(Locale.US, "%.2f", v)) }
        }
    }
    val errors = remember { mutableStateMapOf<Category, Boolean>() }
    val week = txs.filter { it.timestamp >= weekStart() }

    Column(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 3.dp, bottom = 24.dp)) {
        Column(Modifier.padding(start = 3.dp, end = 3.dp, bottom = 17.dp)) {
            Label("PLAN DE GASTOS", FColors.Muted, 9.sp)
            Text(
                "Presupuesto semanal",
                color = FColors.Deep,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp,
                fontFamily = AppFont
            )
            Text(
                "Se sincroniza con el reloj al guardar.",
                color = Color(0xFF75827A),
                fontSize = 12.sp,
                fontFamily = AppFont
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(FColors.Dark, FColors.Deep)))
                .padding(start = 18.dp, end = 18.dp, top = 21.dp, bottom = 18.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 15.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Label("CONFIGURA TUS LÍMITES", Color.White.copy(alpha = 0.58f), 9.sp)
                    Text(
                        "NUEVO PRESUPUESTO",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp,
                        fontFamily = AppFont
                    )
                }
                Box(
                    Modifier
                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Label("7 DÍAS", Color.White.copy(alpha = 0.74f), 8.sp)
                }
            }

            Category.values().forEach { c ->
                val limit = limits[c] ?: 1.0
                val spent = week.filter { it.category == c && !it.cancelled }.sumOf { it.amount }
                val pct = if (limit > 0) (spent / limit * 100).toInt() else 0
                val hasError = errors[c] == true

                Row(
                    Modifier.fillMaxWidth().padding(start = 3.dp, end = 3.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(c.label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = AppFont)
                    Text("$pct% usado", color = Color.White.copy(alpha = 0.55f), fontSize = 10.sp, fontFamily = AppFont)
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(Color(0xFFF3F7F4))
                        .then(
                            if (hasError) Modifier.border(1.5.dp, FColors.High, RoundedCornerShape(11.dp))
                            else Modifier
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("$", color = Color(0xFF789084), fontSize = 13.sp, modifier = Modifier.padding(start = 13.dp, end = 4.dp), fontFamily = AppFont)
                    BasicTextField(
                        value = fields[c] ?: "",
                        onValueChange = {
                            fields[c] = sanitizeAmount(it)
                            errors[c] = false
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = TextStyle(
                            color = FColors.Deep,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = AppFont
                        ),
                        cursorBrush = SolidColor(FColors.Deep),
                        modifier = Modifier.weight(1f).padding(end = 10.dp)
                    )
                }
                if (hasError) {
                    Text(
                        "Ingresa un monto entre \$0.01 y \$100,000",
                        color = Color(0xFFFFB4AB),
                        fontSize = 10.sp,
                        fontFamily = AppFont,
                        modifier = Modifier.padding(start = 3.dp, top = 3.dp)
                    )
                }
                Box(
                    Modifier
                        .padding(horizontal = 9.dp)
                        .padding(top = 3.dp)
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD6E3DC))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth((pct / 100f).coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(FColors.Blue)
                    )
                }
                Spacer(Modifier.height(10.dp))
            }

            Box(
                Modifier
                    .padding(top = 7.dp)
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(FColors.Blue)
                    .clickable {
                        val parsed = Category.values().associateWith {
                            parseAmount(fields[it] ?: "", MAX_BUDGET)
                        }
                        Category.values().forEach { errors[it] = parsed[it] == null }
                        if (parsed.values.all { it != null }) {
                            val newLimits = parsed.mapValues { it.value!! }
                            newLimits.forEach { (c, v) ->
                                fields[c] = String.format(Locale.US, "%.2f", v)
                            }
                            onSaved(newLimits)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Guardar y enviar al reloj",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = AppFont
                )
            }
        }
    }
}

private fun Modifier.offset3(): Modifier = this.padding(top = 3.dp)

@Composable
private fun AddExpenseDialog(
    limits: Map<Category, Double>,
    txs: List<Transaction>,
    onDismiss: () -> Unit,
    onAdd: (Transaction) -> Unit
) {
    var cat by remember { mutableStateOf(Category.CAFE) }
    var amountText by remember { mutableStateOf("10") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo gasto", fontFamily = AppFont) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Category.values().forEach { c ->
                        val sel = c == cat
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .background(if (sel) FColors.Green else FColors.Soft)
                                .clickable { cat = c }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                c.label,
                                color = if (sel) Color.White else FColors.Deep,
                                fontSize = 12.sp,
                                fontFamily = AppFont
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = sanitizeAmount(it)
                        error = false
                    },
                    label = { Text("Monto") },
                    singleLine = true,
                    isError = error,
                    supportingText = {
                        if (error) Text("Ingresa un monto entre \$0.01 y \$100,000")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = parseAmount(amountText, MAX_AMOUNT)
                if (amount == null) {
                    error = true
                } else {
                    val spent = txs
                        .filter { it.category == cat && it.timestamp >= weekStart() && !it.cancelled }
                        .sumOf { it.amount }
                    val res = FrictionEngine.process(Budget(cat, limits[cat] ?: 1.0, spent), amount)
                    onAdd(res.transaction)
                }
            }) { Text("Añadir") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}