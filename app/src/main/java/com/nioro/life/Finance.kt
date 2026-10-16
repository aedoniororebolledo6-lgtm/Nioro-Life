package com.nioro.life

import android.app.AlertDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class Summary(val income: Long, val expenses: Long, val pending: Long, val balance: Long)

fun sameMonth(a: Long, b: Long): Boolean {
    val x = Calendar.getInstance().apply { timeInMillis = a }
    val y = Calendar.getInstance().apply { timeInMillis = b }
    return x.get(Calendar.YEAR) == y.get(Calendar.YEAR) && x.get(Calendar.MONTH) == y.get(Calendar.MONTH)
}

/** Ingresos − gastos (incluye deudas ya pagadas) − deudas pendientes, del mes de `ms`. */
fun monthSummary(tx: List<Tx>, ms: Long): Summary {
    val l = tx.filter { sameMonth(it.date, ms) }
    val income = l.filter { it.kind == 0 }.sumOf { it.amount }
    val expenses = l.filter { it.kind == 1 || (it.kind == 2 && it.paid) }.sumOf { it.amount }
    val pending = l.filter { it.kind == 2 && !it.paid }.sumOf { it.amount }
    return Summary(income, expenses, pending, income - expenses - pending)
}

private val KIND_NAMES = listOf("Ingreso", "Gasto", "Deuda")

private fun kindColor(p: Palette, kind: Int): Color = when (kind) {
    0 -> p.income
    1 -> p.expense
    else -> p.debt
}

@Composable
fun FinanceScreen(
    tx: List<Tx>,
    onSave: (Tx) -> Unit,
    onDelete: (Tx) -> Unit,
    onBack: () -> Unit
) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    var month by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var newKind by remember { mutableStateOf<Int?>(null) }
    var editing by remember { mutableStateOf<Tx?>(null) }

    val list = tx.filter { sameMonth(it.date, month) }.sortedByDescending { it.date }
    val sum = monthSummary(tx, month)
    val monthLabel = SimpleDateFormat("LLLL yyyy", Locale("es", "CL")).format(month)
        .replaceFirstChar { it.uppercase() }

    fun shiftMonth(delta: Int) {
        val c = Calendar.getInstance()
        c.timeInMillis = month
        c.set(Calendar.DAY_OF_MONTH, 1)
        c.add(Calendar.MONTH, delta)
        month = c.timeInMillis
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 16.dp)
            .padding(top = 14.dp)
    ) {
        TopBar("Finanzas", onBack)
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Glass(Modifier.size(44.dp), radius = 15.dp, onClick = { shiftMonth(-1) }) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { BackIcon() }
                    }
                    GlowText(monthLabel, 19.sp)
                    Glass(Modifier.size(44.dp), radius = 15.dp, onClick = { shiftMonth(1) }) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { ForwardIcon() }
                    }
                }
            }

            item {
                Glass(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SALDO DEL MES", color = p.muted, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        Spacer(Modifier.height(4.dp))
                        GlowText(fmtMoney(sum.balance), 38.sp, color = if (sum.balance < 0) p.expense else p.title)
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Stat("Ingresos", fmtMoney(sum.income), p.income, Modifier.weight(1f))
                            Stat("Gastos", fmtMoney(sum.expenses), p.expense, Modifier.weight(1f))
                            Stat("Deudas", fmtMoney(sum.pending), p.debt, Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Saldo = ingresos − gastos − deudas pendientes",
                            color = p.muted, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center
                        )
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (k in 0..2) {
                        Glass(Modifier.weight(1f), radius = 20.dp, onClick = { newKind = k }) {
                            Box(Modifier.fillMaxWidth().padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                                Text("+ ${KIND_NAMES[k]}", color = kindColor(p, k), fontSize = 14.5.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }

            if (list.isEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(top = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        CoinIcon(60.dp)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Sin movimientos este mes.\nAgrega tu sueldo, tus gastos o tus deudas.",
                            color = p.muted, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(list, key = { it.id }) { t ->
                    TxRow(
                        t,
                        onClick = { editing = t },
                        onTogglePaid = { onSave(t.copy(paid = !t.paid)) },
                        onDelete = {
                            AlertDialog.Builder(ctx)
                                .setMessage("¿Eliminar \"${t.title.ifBlank { KIND_NAMES[t.kind] }}\" por ${fmtMoney(t.amount)}?")
                                .setPositiveButton("Eliminar") { _, _ -> onDelete(t) }
                                .setNegativeButton("Cancelar", null)
                                .show()
                        }
                    )
                }
            }
        }
    }

    val nk = newKind
    if (nk != null) {
        TxDialog(kindInit = nk, initial = null, onDismiss = { newKind = null }) {
            onSave(it)
            newKind = null
        }
    }
    val ed = editing
    if (ed != null) {
        TxDialog(kindInit = ed.kind, initial = ed, onDismiss = { editing = null }) {
            onSave(it)
            editing = null
        }
    }
}

@Composable
private fun Stat(label: String, value: String, color: Color, modifier: Modifier) {
    val p = LocalPalette.current
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(p.chipBg)
            .border(1.dp, p.chipBorder, RoundedCornerShape(18.dp)).padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = p.muted, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}

@Composable
private fun TxRow(t: Tx, onClick: () -> Unit, onTogglePaid: () -> Unit, onDelete: () -> Unit) {
    val p = LocalPalette.current
    val color = kindColor(p, t.kind)
    val amountText = when (t.kind) {
        0 -> "+${fmtMoney(t.amount)}"
        1 -> "-${fmtMoney(t.amount)}"
        else -> fmtMoney(t.amount)
    }
    Glass(Modifier.fillMaxWidth(), radius = 24.dp, onClick = onClick) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.padding(top = 6.dp).size(10.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    GlowText(t.title.ifBlank { KIND_NAMES[t.kind] }, 16.sp, glow = false)
                    if (t.reason.isNotBlank()) {
                        Text(
                            "Motivo: ${t.reason}", color = p.body, fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold, maxLines = 3, overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        "${KIND_NAMES[t.kind]} · ${fmtDay(t.date)}",
                        color = p.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        amountText, color = color, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
                        textDecoration = if (t.kind == 2 && t.paid) TextDecoration.LineThrough else null
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier.size(32.dp).clip(RoundedCornerShape(11.dp)).background(p.chipBg)
                            .border(1.dp, p.chipBorder, RoundedCornerShape(11.dp)).clickable(onClick = onDelete),
                        contentAlignment = Alignment.Center
                    ) { CloseIcon(13.dp) }
                }
            }
            if (t.kind == 2) {
                Spacer(Modifier.height(10.dp))
                Row {
                    Chip(if (t.paid) "Pagada ✓ (deshacer)" else "Marcar como pagada", t.paid, onTogglePaid)
                }
            }
        }
    }
}

@Composable
private fun TxDialog(kindInit: Int, initial: Tx?, onDismiss: () -> Unit, onSave: (Tx) -> Unit) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    var kind by remember { mutableStateOf(kindInit) }
    var amountText by remember { mutableStateOf(initial?.amount?.toString() ?: "") }
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var reason by remember { mutableStateOf(initial?.reason ?: "") }
    var date by remember { mutableLongStateOf(initial?.date ?: System.currentTimeMillis()) }
    var paid by remember { mutableStateOf(initial?.paid ?: false) }
    val shape = RoundedCornerShape(30.dp)
    val value = evalExpr(amountText)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp).clip(shape)
                .background(Brush.verticalGradient(p.dialog))
                .border(1.dp, p.dialogBorder, shape)
                .verticalScroll(rememberScrollState())
                .padding(18.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (k in 0..2) Chip(KIND_NAMES[k], kind == k) { kind = k }
            }
            Spacer(Modifier.height(8.dp))

            TextField(
                value = amountText, onValueChange = { amountText = it },
                placeholder = { Text("Monto o cuenta (ej: 450000+35000)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold),
                colors = fieldColors(), modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (sym in listOf("+", "−", "×", "÷", "(", ")")) {
                    CalcKey(sym, Modifier.weight(1f)) { amountText += sym }
                }
                CalcKey("⌫", Modifier.weight(1f)) { if (amountText.isNotEmpty()) amountText = amountText.dropLast(1) }
            }
            Text(
                text = when {
                    amountText.isBlank() -> "Puedes escribir una cuenta: se calcula sola."
                    value == null -> "Esa cuenta no es válida"
                    else -> "= ${fmtMoney(Math.round(value))}"
                },
                color = if (amountText.isNotBlank() && value == null) p.expense else p.accent,
                fontSize = 17.sp, fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(6.dp))

            TextField(
                value = title, onValueChange = { title = it },
                placeholder = {
                    Text(
                        when (kind) {
                            0 -> "Concepto (ej: Sueldo)"
                            1 -> "Concepto (ej: Arriendo)"
                            else -> "¿A quién o qué debes?"
                        }
                    )
                },
                singleLine = true,
                textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                colors = fieldColors(), modifier = Modifier.fillMaxWidth()
            )
            TextField(
                value = reason, onValueChange = { reason = it },
                placeholder = { Text("¿Por qué? (motivo)") },
                minLines = 2, maxLines = 5,
                textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                colors = fieldColors(), modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(p.chipBg)
                    .border(1.dp, p.chipBorder, RoundedCornerShape(20.dp))
                    .clickable { pickDate(ctx, date) { date = it } }
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📅", fontSize = 18.sp)
                Spacer(Modifier.width(10.dp))
                Text(fmtDay(date), color = p.title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            if (kind == 2) {
                SwitchRow("Ya está pagada", "Si no, queda como deuda pendiente", paid) { paid = it }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DialogButton("Cancelar", false, Modifier.weight(1f), onDismiss)
                DialogButton("Guardar", true, Modifier.weight(1f)) {
                    val v = value
                    if (v == null || Math.round(v) <= 0L) {
                        Toast.makeText(ctx, "Escribe un monto válido", Toast.LENGTH_SHORT).show()
                    } else {
                        onSave(
                            Tx(
                                id = initial?.id ?: System.currentTimeMillis(),
                                kind = kind,
                                amount = Math.round(v),
                                title = title.trim(),
                                reason = reason.trim(),
                                date = date,
                                paid = if (kind == 2) paid else false
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalcKey(label: String, modifier: Modifier, onClick: () -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier.height(40.dp).clip(shape).background(p.chipBg).border(1.dp, p.chipBorder, shape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = p.title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
    }
}
