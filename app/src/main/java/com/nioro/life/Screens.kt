package com.nioro.life

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// ---------------------------------------------------------------- INICIO

@Composable
fun HomeScreen(settings: Settings, notes: List<Note>, now: Long, onGo: (String) -> Unit) {
    val st = computeSleep(settings, now)
    val totalSec = maxOf(0L, (st.targetMs - now) / 1000)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    val frac = ((now - st.fromMs).toFloat() / (st.targetMs - st.fromMs).toFloat()).coerceIn(0f, 1f)
    val hour = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)
    val greet = when {
        hour < 6 -> "Buenas noches"
        hour < 13 -> "Buenos días"
        hour < 20 -> "Buenas tardes"
        else -> "Buenas noches"
    }
    val nextRem = notes
        .filter { !it.done && it.remindAt != null && (it.remindAt ?: 0L) > now }
        .minByOrNull { it.remindAt ?: Long.MAX_VALUE }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(Modifier.padding(start = 6.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.mipmap.ic_launcher),
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.width(10.dp))
            Column {
                GlowText("Nioro Life", 22.sp)
                Text(greet, color = Muted, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Glass(Modifier.fillMaxWidth(), onClick = { onGo("clock") }) {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 22.dp, horizontal = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (st.sleeping) "FALTAN PARA DESPERTAR" else "TE QUEDAN PARA DORMIR",
                    color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp
                )
                Box(Modifier.padding(top = 12.dp).size(236.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize().padding(8.dp)) {
                        val stroke = 9.dp.toPx()
                        val tl = Offset(stroke / 2, stroke / 2)
                        val sz = Size(size.width - stroke, size.height - stroke)
                        // luna
                        val ms = size.width * 0.66f / 100f
                        val ox = (size.width - 100 * ms) / 2
                        val oy = (size.height - 100 * ms) / 2
                        val mp = moonPath(ms, ox, oy)
                        drawPath(mp, Cream.copy(alpha = 0.10f), style = Stroke(width = 28f))
                        drawPath(mp, Cream.copy(alpha = 0.14f), style = Stroke(width = 14f))
                        drawPath(
                            mp,
                            Brush.radialGradient(
                                listOf(Color(0xFFEEF2FB), Color(0xFFD9E2F3), Color(0xFFF4EFC6)),
                                center = Offset(size.width * 0.4f, size.height * 0.35f),
                                radius = size.width * 0.6f
                            ),
                            alpha = 0.45f
                        )
                        // anillo
                        drawArc(Color(0x1FFFFFFF), 0f, 360f, false, tl, sz, style = Stroke(stroke))
                        if (frac > 0f) {
                            drawArc(Cream.copy(alpha = 0.22f), -90f, 360f * frac, false, tl, sz, style = Stroke(stroke + 12f, cap = StrokeCap.Round))
                            drawArc(Color(0xFFFFE28A), -90f, 360f * frac, false, tl, sz, style = Stroke(stroke, cap = StrokeCap.Round))
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val big = buildAnnotatedString {
                            append("$h")
                            withStyle(SpanStyle(fontSize = 20.sp)) { append("h ") }
                            append("%02d".format(m))
                            withStyle(SpanStyle(fontSize = 20.sp)) { append("m") }
                        }
                        Text(
                            big, color = Cream, fontSize = 52.sp, fontWeight = FontWeight.ExtraBold,
                            style = TextStyle(shadow = androidx.compose.ui.graphics.Shadow(CreamGlow, Offset.Zero, 28f))
                        )
                        Text("%02d s".format(s), color = Muted, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Te duermes a las ${fmtTime(settings.bedMin)} · despiertas a las ${fmtTime(settings.wakeMin)}",
                    color = Muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Tile(Modifier.weight(1f), "Reloj", "${fmtTime(settings.bedMin)} → ${fmtTime(settings.wakeMin)}", { ClockIcon() }) { onGo("clock") }
            Tile(
                Modifier.weight(1f), "Notas",
                if (notes.isEmpty()) "Recordatorios" else "${notes.size} ${if (notes.size == 1) "nota" else "notas"}",
                { NoteIcon() }
            ) { onGo("notes") }
        }

        if (nextRem != null) {
            Spacer(Modifier.height(14.dp))
            Glass(Modifier.fillMaxWidth(), radius = 24.dp) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(Cream))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Próximo recordatorio", color = Muted, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        val t = if (nextRem.title.isBlank()) nextRem.text.take(40) else nextRem.title
                        Text(t, color = Cream, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(fmtDate(nextRem.remindAt ?: 0L), color = Muted, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun Tile(
    modifier: Modifier,
    title: String,
    sub: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Glass(modifier.height(150.dp), onClick = onClick) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            IconBadge { icon() }
            Column {
                GlowText(title, 19.sp)
                Text(sub, color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

// ---------------------------------------------------------------- RELOJ

@Composable
private fun TopBar(title: String, onBack: () -> Unit) {
    Row(Modifier.padding(bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Glass(Modifier.size(46.dp), radius = 16.dp, onClick = onBack) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { BackIcon() }
        }
        Spacer(Modifier.width(12.dp))
        GlowText(title, 25.sp)
    }
}

@Composable
fun ClockScreen(settings: Settings, onChange: (Settings) -> Unit, onBack: () -> Unit) {
    val now = System.currentTimeMillis()
    val dur = sleepDurMin(settings)
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 16.dp)
            .padding(top = 14.dp)
    ) {
        TopBar("Reloj", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Glass(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    GlowText("Mi horario de sueño", 16.sp, glow = false)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TimeBox("Dormir", settings.bedMin, Modifier.weight(1f)) { onChange(settings.copy(bedMin = it)) }
                        TimeBox("Despertar", settings.wakeMin, Modifier.weight(1f)) { onChange(settings.copy(wakeMin = it)) }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Text("Duermes ", color = Muted, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        GlowText(fmtDur(dur), 22.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (dur < 360) "Dormirás menos de 6 h: intenta acercarte a 7–9 h."
                        else if (dur <= 540) "Buena duración de sueño para un adulto."
                        else "Es un sueño largo, ajusta si lo necesitas.",
                        color = Muted, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Glass(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    GlowText("Avisos", 16.sp, glow = false)
                    SwitchRow("Alarma de despertar", "Suena a tu hora, aunque la app esté cerrada", settings.alarmOn) {
                        onChange(settings.copy(alarmOn = it))
                    }
                    SwitchRow("Aviso para ir a dormir", "Te recuerda antes de tu hora", settings.bedNotify) {
                        onChange(settings.copy(bedNotify = it))
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(15 to "15 min antes", 30 to "30 min antes", 60 to "1 h antes").forEach { (v, label) ->
                            Chip(label, settings.leadMin == v) { onChange(settings.copy(leadMin = v)) }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Glass(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    GlowText("Si me duermo ahora, despierto a las…", 16.sp, glow = false)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(4, 5, 6).forEach { n ->
                            val c = Calendar.getInstance().apply { timeInMillis = now + (15 + 90 * n) * 60_000L }
                            val t = fmtTime(c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE))
                            Column(
                                Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(Color(0x0FFFFFFF))
                                    .border(1.dp, Color(0x24FFFFFF), RoundedCornerShape(20.dp)).padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                GlowText(t, 22.sp)
                                Text("$n ciclos", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Ciclos de 90 min + 15 min para dormirte. Despertar al final de un ciclo ayuda a sentirte más descansado.",
                        color = Muted, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TimeBox(label: String, minutes: Int, modifier: Modifier, onPick: (Int) -> Unit) {
    val ctx = LocalContext.current
    Glass(
        modifier, radius = 22.dp,
        onClick = {
            TimePickerDialog(ctx, { _, hh, mm -> onPick(hh * 60 + mm) }, minutes / 60, minutes % 60, true).show()
        }
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label.uppercase(), color = Muted, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(6.dp))
            GlowText(fmtTime(minutes), 34.sp)
        }
    }
}

@Composable
private fun SwitchRow(title: String, sub: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(sub, color = Muted, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        }
        Switch(
            checked = on, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0x99FFECA0),
                checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0x24FFFFFF),
                uncheckedBorderColor = Color(0x38FFFFFF)
            )
        )
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier.clip(shape)
            .background(if (selected) Color(0x47FFECA0) else Color(0x14FFFFFF))
            .border(1.dp, Color(0x38FFFFFF), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Text(label, color = Cream, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

// ---------------------------------------------------------------- NOTAS

@Composable
fun NotesScreen(
    notes: List<Note>,
    onSave: (Note) -> Unit,
    onDelete: (Note) -> Unit,
    onBack: () -> Unit
) {
    var editing by remember { mutableStateOf<Note?>(null) }
    var creating by remember { mutableStateOf(false) }
    val sorted = notes.sortedByDescending { it.id }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 16.dp)
                .padding(top = 14.dp)
        ) {
            TopBar("Notas", onBack)
            if (sorted.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().padding(top = 60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NoteIcon(64.dp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Aún no tienes notas.\nToca + para crear la primera.",
                        color = Muted, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 110.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(sorted, key = { it.id }) { n ->
                        Glass(Modifier.fillMaxWidth(), radius = 26.dp, onClick = { editing = n }) {
                            Column(Modifier.padding(16.dp)) {
                                if (n.title.isNotBlank()) GlowText(n.title, 17.sp, glow = false)
                                if (n.text.isNotBlank()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        n.text, color = Color(0xE6F4F6FF), fontSize = 14.5.sp,
                                        fontWeight = FontWeight.SemiBold, maxLines = 6, overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(
                                    Modifier.fillMaxWidth().padding(top = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val r = n.remindAt
                                    Text(
                                        if (r != null) "🔔 ${fmtDate(r)}" else "",
                                        color = if (n.done) Muted else Cream,
                                        fontSize = 12.5.sp, fontWeight = FontWeight.Bold
                                    )
                                    Box(
                                        Modifier.size(34.dp).clip(RoundedCornerShape(12.dp))
                                            .background(Color(0x12FFFFFF))
                                            .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(12.dp))
                                            .clickable { onDelete(n) },
                                        contentAlignment = Alignment.Center
                                    ) { CloseIcon() }
                                }
                            }
                        }
                    }
                }
            }
        }

        Box(
            Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(20.dp)
                .size(68.dp).clip(RoundedCornerShape(26.dp))
                .background(Brush.linearGradient(listOf(Color(0x6BFFF3BE), Color(0x29FFF3BE))))
                .border(1.dp, Color(0x59FFFFFF), RoundedCornerShape(26.dp))
                .clickable { creating = true },
            contentAlignment = Alignment.Center
        ) { PlusIcon() }
    }

    if (creating) {
        NoteDialog(
            initial = null,
            onDismiss = { creating = false },
            onSave = { title, text, remind ->
                onSave(Note(System.currentTimeMillis(), title, text, remind, false))
                creating = false
            }
        )
    }
    val ed = editing
    if (ed != null) {
        NoteDialog(
            initial = ed,
            onDismiss = { editing = null },
            onSave = { title, text, remind ->
                onSave(ed.copy(title = title, text = text, remindAt = remind, done = if (remind != ed.remindAt) false else ed.done))
                editing = null
            }
        )
    }
}

@Composable
private fun NoteDialog(initial: Note?, onDismiss: () -> Unit, onSave: (String, String, Long?) -> Unit) {
    val ctx = LocalContext.current
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var text by remember { mutableStateOf(initial?.text ?: "") }
    var remind by remember { mutableStateOf(initial?.remindAt) }
    val shape = RoundedCornerShape(30.dp)
    val fieldColors = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        focusedIndicatorColor = Color(0x66FFF3C4),
        unfocusedIndicatorColor = Color(0x24FFFFFF),
        cursorColor = Cream,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedPlaceholderColor = Color(0x66E2EAFF),
        unfocusedPlaceholderColor = Color(0x66E2EAFF)
    )

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp).clip(shape)
                .background(Brush.verticalGradient(listOf(Color(0xF2183282), Color(0xF20B2864))))
                .border(1.dp, Color(0x40FFFFFF), shape)
                .padding(18.dp)
        ) {
            TextField(
                value = title, onValueChange = { title = it },
                placeholder = { Text("Título (opcional)") },
                singleLine = true,
                textStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold),
                colors = fieldColors, modifier = Modifier.fillMaxWidth()
            )
            TextField(
                value = text, onValueChange = { text = it },
                placeholder = { Text("Escribe aquí lo que quieras recordar…") },
                minLines = 4, maxLines = 8,
                textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                colors = fieldColors, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0x12FFFFFF))
                    .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(20.dp))
                    .clickable { pickDateTime(ctx, remind) { remind = it } }
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔔", fontSize = 18.sp)
                Spacer(Modifier.width(10.dp))
                val r = remind
                Text(
                    if (r != null) fmtDate(r) else "Añadir recordatorio (fecha y hora)",
                    color = if (r != null) Cream else Muted, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (r != null) {
                    Box(Modifier.size(30.dp).clickable { remind = null }, contentAlignment = Alignment.Center) { CloseIcon() }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DialogButton("Cancelar", false, Modifier.weight(1f), onDismiss)
                DialogButton("Guardar", true, Modifier.weight(1f)) {
                    if (title.isBlank() && text.isBlank()) {
                        Toast.makeText(ctx, "Escribe algo primero", Toast.LENGTH_SHORT).show()
                    } else {
                        onSave(title.trim(), text.trim(), remind)
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogButton(label: String, main: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier.height(52.dp).clip(shape)
            .background(
                if (main) Brush.verticalGradient(listOf(Color(0xFFFFF7D2), Color(0xFFFFE99A)))
                else Brush.verticalGradient(listOf(Color(0x17FFFFFF), Color(0x17FFFFFF)))
            )
            .border(1.dp, if (main) Color(0xFFFFF3C4) else Color(0x38FFFFFF), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (main) Color(0xFF10215F) else Cream, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
    }
}

// ---------------------------------------------------------------- utilidades

fun fmtDate(ms: Long): String =
    SimpleDateFormat("EEE d MMM · HH:mm", Locale("es", "CL")).format(ms)

fun pickDateTime(ctx: Context, current: Long?, onPicked: (Long) -> Unit) {
    val c = Calendar.getInstance()
    c.timeInMillis = current ?: (System.currentTimeMillis() + 3_600_000L)
    DatePickerDialog(
        ctx,
        { _, y, mo, d ->
            TimePickerDialog(
                ctx,
                { _, hh, mi ->
                    val r = Calendar.getInstance()
                    r.set(y, mo, d, hh, mi, 0)
                    r.set(Calendar.MILLISECOND, 0)
                    onPicked(r.timeInMillis)
                },
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true
            ).show()
        },
        c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
    ).show()
}
