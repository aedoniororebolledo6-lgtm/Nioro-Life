package com.nioro.life

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Sección Casa: ampolletas Wi-Fi con firmware libre, controladas desde Nioro Life por la red local. */
@Composable
fun CasaScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var devices by remember { mutableStateOf(SmartHome.load(ctx)) }
    val states = remember { mutableStateMapOf<Long, BulbState>() }
    var openId by remember { mutableStateOf<Long?>(null) }
    var adding by remember { mutableStateOf(false) }
    var guide by remember { mutableStateOf(false) }
    // tras tocar algo, se deja de preguntar el estado unos segundos para que no "salte" lo que mueves
    var lastTouch by remember { mutableLongStateOf(0L) }

    fun save(list: List<SmartDevice>) { devices = list; SmartHome.save(ctx, list) }

    // consulta el estado de todas las ampolletas cada 5 s mientras la pantalla está abierta
    LaunchedEffect(devices) {
        while (true) {
            if (System.currentTimeMillis() - lastTouch > 3000) {
                devices.forEach { d ->
                    launch { states[d.id] = SmartHome.refresh(d.ip, states[d.id] ?: BulbState()) }
                }
            }
            delay(5000)
        }
    }

    // envía un comando: cambia la pantalla al instante y luego confirma con lo que responde la ampolleta
    val jobs = remember { HashMap<String, Job>() }
    val control: (SmartDevice, String, String, BulbState, Boolean) -> Unit = { d, key, cmd, optimistic, adopt ->
        lastTouch = System.currentTimeMillis()
        states[d.id] = optimistic
        jobs["${d.id}$key"]?.cancel()
        jobs["${d.id}$key"] = scope.launch {
            delay(if (adopt) 0 else 120) // los deslizadores mandan solo el último valor
            val r = SmartHome.send(d.ip, cmd, optimistic)
            states[d.id] = if (adopt || !r.online) r else optimistic.copy(online = true)
        }
    }

    val open = devices.firstOrNull { it.id == openId }
    BackHandler(enabled = open != null) { openId = null }
    BackHandler(enabled = open == null) { onBack() }

    AnimatedContent(
        targetState = open,
        transitionSpec = {
            (scaleIn(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow), initialScale = 0.85f) + fadeIn(tween(220)))
                .togetherWith(scaleOut(tween(220), targetScale = 0.92f) + fadeOut(tween(180)))
        },
        label = "casa"
    ) { d ->
        if (d == null) {
            DeviceList(
                devices, states, onBack,
                onOpen = { openId = it.id },
                onToggle = { dev ->
                    val st = states[dev.id] ?: BulbState()
                    control(dev, "p", "Power Toggle", st.copy(on = !st.on), true)
                },
                onAdd = { adding = true },
                onGuide = { guide = true }
            )
        } else {
            BulbDetail(
                d, states[d.id] ?: BulbState(),
                onBack = { openId = null },
                onControl = { key, cmd, opt, adopt -> control(d, key, cmd, opt, adopt) },
                onRename = { n -> save(devices.map { if (it.id == d.id) it.copy(name = n) else it }) },
                onDelete = { openId = null; states.remove(d.id); save(devices.filter { it.id != d.id }) }
            )
        }
    }

    if (adding) {
        AddDeviceDialog(
            known = devices.map { it.ip }.toSet(),
            onDismiss = { adding = false },
            onGuide = { adding = false; guide = true }
        ) { name, ip ->
            save(devices + SmartDevice(System.currentTimeMillis(), name, ip))
            adding = false
        }
    }
    if (guide) FlashGuideDialog { guide = false }
}

// ---------------------------------------------------------------- lista

@Composable
private fun DeviceList(
    devices: List<SmartDevice>,
    states: Map<Long, BulbState>,
    onBack: () -> Unit,
    onOpen: (SmartDevice) -> Unit,
    onToggle: (SmartDevice) -> Unit,
    onAdd: () -> Unit,
    onGuide: () -> Unit
) {
    val p = LocalPalette.current
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 16.dp).padding(top = 14.dp)
    ) {
        TopBar("Casa", onBack)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            if (devices.isEmpty()) {
                item {
                    Glass(Modifier.fillMaxWidth().rise(0)) {
                        Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            BulbGlyph(BulbState(online = true, on = true, white = false, hue = 45, sat = 70), Modifier.size(110.dp))
                            Spacer(Modifier.height(8.dp))
                            GlowText("Tu casa, sin Smart Life", 19.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Controla tus ampolletas Wi-Fi directamente desde Nioro Life, por tu red y sin nubes. " +
                                    "Funcionan las que tienen firmware libre (OpenBeken o Tasmota).",
                                color = p.muted, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            itemsIndexed(devices, key = { _, d -> d.id }) { i, d ->
                DeviceCard(d, states[d.id], Modifier.rise(i).animateItem(), { onOpen(d) }) { onToggle(d) }
            }
            item {
                Spacer(Modifier.height(4.dp))
                DialogButton("+ Añadir ampolleta", true, Modifier.fillMaxWidth(), onAdd)
                Spacer(Modifier.height(10.dp))
                DialogButton("📘 Cómo liberar mi ampolleta de Tuya", false, Modifier.fillMaxWidth(), onGuide)
            }
        }
    }
}

@Composable
private fun DeviceCard(d: SmartDevice, st: BulbState?, modifier: Modifier, onOpen: () -> Unit, onToggle: () -> Unit) {
    val p = LocalPalette.current
    val s = st ?: BulbState()
    Glass(modifier.fillMaxWidth(), onClick = onOpen) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            BulbGlyph(s, Modifier.size(58.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(d.name, color = p.title, fontSize = 16.5.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    when {
                        st == null -> "Conectando…"
                        !s.online -> "Sin conexión · ${d.ip}"
                        !s.on -> "Apagada"
                        s.white -> "Encendida · ${s.dimmer}% · ${miredToKelvin(s.ct)} K"
                        else -> "Encendida · ${s.dimmer}% · color"
                    },
                    color = if (st != null && !s.online) p.expense else p.muted,
                    fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1
                )
            }
            PowerPill(s.on && s.online, enabled = s.online, onToggle)
        }
    }
}

/** Interruptor grande con resorte. */
@Composable
private fun PowerPill(on: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    val p = LocalPalette.current
    val haptic = LocalHapticFeedback.current
    val x by animateFloatAsState(if (on) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium), label = "pill")
    val track by animateColorAsState(if (on) p.accent else p.chipBg, tween(220), label = "pillC")
    Box(
        Modifier.width(62.dp).height(36.dp).clip(RoundedCornerShape(18.dp)).background(track)
            .border(1.dp, p.chipBorder, RoundedCornerShape(18.dp))
            .clickable(enabled = enabled) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onToggle() }
            .padding(4.dp)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.height / 2
            val cx = r + (size.width - 2 * r) * x
            drawCircle(if (on) p.onBtnMain else p.title.copy(alpha = if (enabled) 0.9f else 0.35f), r, Offset(cx, r))
        }
    }
}

// ---------------------------------------------------------------- dibujo de la ampolleta

/** Ampolleta dibujada: brilla con su color y su brillo; apagada se ve como vidrio. */
@Composable
fun BulbGlyph(s: BulbState, modifier: Modifier) {
    val p = LocalPalette.current
    val target = if (s.on && s.online) Color(bulbArgb(s)) else p.chipBg
    val col by animateColorAsState(target, tween(450), label = "bulbC")
    val level by animateFloatAsState(if (s.on && s.online) 0.35f + 0.65f * s.dimmer / 100f else 0f, tween(450), label = "bulbL")
    val breathe by rememberInfiniteTransition(label = "bulbB").animateFloat(0.92f, 1.06f, infiniteRepeatable(tween(2600), RepeatMode.Reverse), label = "b")
    Canvas(modifier) {
        val w = size.minDimension
        val c = Offset(size.width / 2, size.height * 0.42f)
        val r = w * 0.3f
        if (level > 0f) {
            drawCircle(Brush.radialGradient(listOf(col.copy(alpha = 0.55f * level), Color.Transparent), c, w * 0.62f * breathe), w * 0.62f * breathe, c)
        }
        // vidrio
        drawCircle(if (level > 0f) col else p.chipBg, r, c)
        drawCircle(Color.White.copy(alpha = 0.18f + 0.3f * level), r * 0.55f, Offset(c.x - r * 0.25f, c.y - r * 0.28f))
        drawCircle(p.borderTop, r, c, style = Stroke(w * 0.025f))
        // rosca
        val bw = r * 0.95f
        val top = c.y + r * 0.86f
        for (i in 0..2) {
            drawRoundRect(
                p.muted.copy(alpha = 0.85f), Offset(c.x - bw / 2, top + i * w * 0.06f), Size(bw, w * 0.045f), CornerRadius(w * 0.02f)
            )
        }
    }
}

// ---------------------------------------------------------------- detalle

@Composable
private fun BulbDetail(
    d: SmartDevice,
    s: BulbState,
    onBack: () -> Unit,
    onControl: (key: String, cmd: String, optimistic: BulbState, adopt: Boolean) -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    val p = LocalPalette.current
    var renaming by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 16.dp).padding(top = 14.dp)
    ) {
        TopBar(d.name, onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Glass(Modifier.fillMaxWidth().rise(0)) {
                Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(190.dp).clip(CircleShape).clickable(enabled = s.online) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onControl("p", "Power Toggle", s.copy(on = !s.on), true)
                        },
                        contentAlignment = Alignment.Center
                    ) { BulbGlyph(s, Modifier.fillMaxSize()) }
                    Text(
                        if (!s.online) "Sin conexión con ${d.ip}" else if (s.on) "Toca la ampolleta para apagarla" else "Toca la ampolleta para encenderla",
                        color = if (!s.online) p.expense else p.muted, fontSize = 13.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(14.dp))
                    LabeledSlider("☀️ Brillo", "${s.dimmer}%", s.dimmer.toFloat(), 1f..100f, s.online) { v ->
                        val n = v.toInt()
                        onControl("d", "Dimmer $n", s.copy(dimmer = n, on = true), false)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Glass(Modifier.fillMaxWidth().rise(1)) {
                Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("🎨 Color", !s.white) {
                            onControl("m", "HSBColor ${s.hue},${maxOf(s.sat, 60)},${s.dimmer}", s.copy(white = false, sat = maxOf(s.sat, 60), on = true), false)
                        }
                        Chip("💡 Blanco", s.white) { onControl("m", "CT ${s.ct}", s.copy(white = true, on = true), false) }
                    }
                    Spacer(Modifier.height(14.dp))
                    AnimatedContent(targetState = s.white, label = "modo") { white ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (!white) {
                                HueRing(s.hue, s.sat, Modifier.size(230.dp), s.online) { h ->
                                    onControl("c", "HSBColor $h,${s.sat},${s.dimmer}", s.copy(hue = h, white = false, on = true), false)
                                }
                                Spacer(Modifier.height(10.dp))
                                LabeledSlider("Intensidad del color", "${s.sat}%", s.sat.toFloat(), 5f..100f, s.online) { v ->
                                    onControl("c", "HSBColor ${s.hue},${v.toInt()},${s.dimmer}", s.copy(sat = v.toInt(), white = false, on = true), false)
                                }
                            } else {
                                // deslizador al revés: a la izquierda cálido, a la derecha frío
                                GradientSlider(
                                    "Temperatura", "${miredToKelvin(s.ct)} K",
                                    (653 - s.ct).toFloat(), 153f..500f, listOf(Color(whiteArgb(500)), Color(whiteArgb(153))), s.online
                                ) { v ->
                                    val ct = 653 - v.toInt()
                                    onControl("t", "CT $ct", s.copy(ct = ct, white = true, on = true), false)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Glass(Modifier.fillMaxWidth().rise(2)) {
                Column(Modifier.padding(16.dp)) {
                    GlowText("Escenas", 16.sp, glow = false)
                    Spacer(Modifier.height(10.dp))
                    LIGHT_SCENES.chunked(4).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                            row.forEach { sc -> SceneButton(sc, Modifier.weight(1f), s.online) { applyScene(sc, s, onControl) } }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DialogButton("✏️ Nombre", false, Modifier.weight(1f)) { renaming = true }
                DialogButton("Quitar", false, Modifier.weight(1f)) { confirmDelete = true }
            }
            Spacer(Modifier.height(8.dp))
            Text("IP ${d.ip} · OpenBeken / Tasmota", color = p.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
    }
    if (renaming) {
        NameDialog(d.name, onDismiss = { renaming = false }) { onRename(it); renaming = false }
    }
    if (confirmDelete) {
        ConfirmDialog("¿Quitar \"${d.name}\"?", "Se quita de Nioro Life. La ampolleta no se borra ni se resetea.", "Quitar", onNo = { confirmDelete = false }) {
            confirmDelete = false
            onDelete()
        }
    }
}

private fun applyScene(sc: LightScene, s: BulbState, onControl: (String, String, BulbState, Boolean) -> Unit) {
    if (sc.white) {
        onControl("s", "Backlog CT ${sc.ct}; Dimmer ${sc.dimmer}", s.copy(white = true, ct = sc.ct, dimmer = sc.dimmer, on = true), false)
    } else {
        onControl("s", "HSBColor ${sc.hue},${sc.sat},${sc.dimmer}", s.copy(white = false, hue = sc.hue, sat = sc.sat, dimmer = sc.dimmer, on = true), false)
    }
}

@Composable
private fun SceneButton(sc: LightScene, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    val c = Color(if (sc.white) whiteArgb(sc.ct) else hsvToArgb(sc.hue.toFloat(), sc.sat / 100f, 1f))
    Column(
        modifier.clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(c.copy(alpha = 0.32f), p.chipBg)))
            .border(1.dp, c.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(sc.emoji, fontSize = 20.sp)
        Text(sc.name, color = p.title, fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}

@Composable
private fun LabeledSlider(label: String, value: String, v: Float, range: ClosedFloatingPointRange<Float>, enabled: Boolean, onChange: (Float) -> Unit) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth()) {
        Row {
            Text(label, color = p.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(value, color = p.accent, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
        }
        Slider(
            value = v.coerceIn(range), onValueChange = onChange, valueRange = range, enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = p.title, activeTrackColor = p.accent, inactiveTrackColor = p.track,
                disabledThumbColor = p.muted, disabledActiveTrackColor = p.track
            )
        )
    }
}

/** Deslizador con el riel pintado en degradado (para la temperatura del blanco). */
@Composable
private fun GradientSlider(
    label: String, value: String, v: Float, range: ClosedFloatingPointRange<Float>, colors: List<Color>, enabled: Boolean, onChange: (Float) -> Unit
) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth()) {
        Row {
            Text(label, color = p.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(value, color = p.accent, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
        }
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 10.dp).height(12.dp).clip(RoundedCornerShape(6.dp)).background(Brush.horizontalGradient(colors)))
            Slider(
                value = v.coerceIn(range), onValueChange = onChange, valueRange = range, enabled = enabled,
                colors = SliderDefaults.colors(
                    thumbColor = p.title, activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent,
                    disabledActiveTrackColor = Color.Transparent, disabledInactiveTrackColor = Color.Transparent
                )
            )
        }
        Row {
            Text("🔥 Cálido", color = p.muted, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("Frío ❄️", color = p.muted, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** Rueda de colores: arrastra o toca para elegir el tono. */
@Composable
private fun HueRing(hue: Int, sat: Int, modifier: Modifier, enabled: Boolean, onHue: (Int) -> Unit) {
    val p = LocalPalette.current
    val haptic = LocalHapticFeedback.current
    var local by remember { mutableIntStateOf(hue) }
    LaunchedEffect(hue) { local = hue }
    fun pick(o: Offset, w: Float, h: Float) {
        val deg = Math.toDegrees(atan2((o.y - h / 2).toDouble(), (o.x - w / 2).toDouble())).toFloat()
        val nh = ((deg + 360f) % 360f).toInt()
        if (nh / 30 != local / 30) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        local = nh
        onHue(nh)
    }
    val hues = remember { List(13) { Color(hsvToArgb(it * 30f, 1f, 1f)) } }
    Canvas(
        modifier.aspectRatio(1f)
            .pointerInput(enabled) { if (enabled) detectTapGestures { pick(it, size.width.toFloat(), size.height.toFloat()) } }
            .pointerInput(enabled) {
                if (enabled) detectDragGestures(onDragStart = { pick(it, size.width.toFloat(), size.height.toFloat()) }) { ch, _ ->
                    pick(ch.position, size.width.toFloat(), size.height.toFloat())
                }
            }
    ) {
        val ring = size.minDimension * 0.13f
        val r = size.minDimension / 2 - ring / 2 - 8f
        val c = center
        drawCircle(Brush.sweepGradient(hues, c), r, c, style = Stroke(ring))
        // vista previa en el centro
        val col = Color(hsvToArgb(local.toFloat(), sat / 100f, 1f))
        drawCircle(Brush.radialGradient(listOf(col.copy(alpha = 0.5f), Color.Transparent), c, r * 0.85f), r * 0.85f, c)
        drawCircle(col, r * 0.42f, c)
        drawCircle(p.borderTop, r * 0.42f, c, style = Stroke(3f))
        // perilla
        val a = local / 180f * PI.toFloat()
        val k = Offset(c.x + r * cos(a), c.y + r * sin(a))
        drawCircle(Color.Black.copy(alpha = 0.25f), ring * 0.62f, k + Offset(0f, 3f))
        drawCircle(Color.White, ring * 0.6f, k)
        drawCircle(Color(hsvToArgb(local.toFloat(), 1f, 1f)), ring * 0.45f, k)
        if (!enabled) drawCircle(p.bg.first().copy(alpha = 0.5f), size.minDimension / 2, c)
    }
}

@Composable
private fun NameDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    GlassDialog(onDismiss) {
        GlowText("Nombre", 19.sp)
        Spacer(Modifier.height(10.dp))
        Field(name, "Ej: Ampolleta del living") { name = it }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DialogButton("Cancelar", false, Modifier.weight(1f), onDismiss)
            DialogButton("Guardar", true, Modifier.weight(1f)) { if (name.isNotBlank()) onSave(name.trim()) }
        }
    }
}

@Composable
private fun Field(value: String, hint: String, password: Boolean = false, number: Boolean = false, onChange: (String) -> Unit) {
    val p = LocalPalette.current
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(p.chipBg)
            .border(1.dp, p.chipBorder, RoundedCornerShape(14.dp))
            .semantics { contentDescription = hint }
            .padding(12.dp)
    ) {
        if (value.isEmpty()) Text(hint, color = p.placeholder, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        BasicTextField(
            value, onChange, singleLine = true,
            visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = when {
                password -> KeyboardType.Password
                number -> KeyboardType.Decimal
                else -> KeyboardType.Text
            }),
            textStyle = TextStyle(color = p.title, fontSize = 15.sp, fontWeight = FontWeight.Bold),
            cursorBrush = SolidColor(p.accent), modifier = Modifier.fillMaxWidth()
        )
    }
}

// ---------------------------------------------------------------- añadir

@Composable
private fun AddDeviceDialog(known: Set<String>, onDismiss: () -> Unit, onGuide: () -> Unit, onAdd: (String, String) -> Unit) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var step by remember { mutableIntStateOf(0) } // 0 buscar, 1 manual, 2 wifi de ampolleta nueva
    var scanning by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var found by remember { mutableStateOf<List<SmartHome.Found>?>(null) }
    var ip by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var ssid by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }

    GlassDialog(onDismiss) {
        GlowText("Añadir ampolleta", 19.sp)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Chip("🔍 Buscar", step == 0) { step = 0 }
            Chip("⌨️ Por IP", step == 1) { step = 1 }
            Chip("📶 Nueva", step == 2) { step = 2 }
        }
        Spacer(Modifier.height(12.dp))
        when (step) {
            0 -> {
                Text(
                    "Busca en tu Wi-Fi las ampolletas con OpenBeken o Tasmota. El teléfono debe estar en el mismo Wi-Fi.",
                    color = p.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(10.dp))
                if (scanning) {
                    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(p.track)) {
                        Box(Modifier.fillMaxWidth(progress / 100f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(p.accent))
                    }
                    Text("Buscando… $progress%", color = p.muted, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                } else {
                    DialogButton(if (found == null) "Buscar en mi red" else "Buscar otra vez", true, Modifier.fillMaxWidth()) {
                        if (SmartHome.phoneIp(ctx) == null) {
                            Toast.makeText(ctx, "Conéctate al Wi-Fi de tu casa primero", Toast.LENGTH_LONG).show()
                        } else {
                            scanning = true; progress = 0
                            scope.launch {
                                found = SmartHome.scan(ctx) { progress = it }
                                scanning = false
                            }
                        }
                    }
                }
                val f = found
                if (f != null && !scanning) {
                    Spacer(Modifier.height(10.dp))
                    if (f.isEmpty()) {
                        Text("No encontré ampolletas. Revisa que tengan firmware libre y estén en tu Wi-Fi, o añádela por IP.",
                            color = p.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    f.forEach { x ->
                        val already = x.ip in known
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(14.dp)).background(p.chipBg)
                                .clickable(enabled = !already) { onAdd(x.name, x.ip) }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("💡", fontSize = 20.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(x.name, color = p.title, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                                Text(x.ip, color = p.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Text(if (already) "Ya está" else "Añadir", color = if (already) p.muted else p.accent, fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
            1 -> {
                Text("Escribe la IP que muestra la ampolleta en su página o en tu router.", color = p.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Field(ip, "IP, ej: 192.168.1.50", number = true) { ip = it.trim() }
                Spacer(Modifier.height(8.dp))
                Field(name, "Nombre, ej: Living") { name = it }
                Spacer(Modifier.height(12.dp))
                DialogButton("Añadir", true, Modifier.fillMaxWidth()) {
                    when {
                        !isIpv4(ip) -> Toast.makeText(ctx, "Esa IP no es válida", Toast.LENGTH_SHORT).show()
                        ip in known -> Toast.makeText(ctx, "Esa ampolleta ya está añadida", Toast.LENGTH_SHORT).show()
                        else -> onAdd(name.trim().ifBlank { "Ampolleta ${ip.substringAfterLast('.')}" }, ip)
                    }
                }
            }
            else -> {
                Text(
                    "Para una ampolleta recién liberada (todavía sin tu Wi-Fi):\n" +
                        "1. Enciéndela. Crea una red llamada OpenBeken_… o tasmota-…\n" +
                        "2. En los ajustes de Wi-Fi del teléfono, conéctate a esa red. Si Android avisa \"sin internet\", elige mantener la conexión.\n" +
                        "3. Vuelve aquí, escribe los datos de tu Wi-Fi y pulsa Enviar.\n" +
                        "4. Vuelve a conectar el teléfono a tu Wi-Fi y usa \"Buscar\".",
                    color = p.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                SmallButton("Abrir ajustes de Wi-Fi", Modifier.fillMaxWidth()) {
                    ctx.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                Spacer(Modifier.height(8.dp))
                Field(ssid, "Nombre de tu Wi-Fi (2,4 GHz)") { ssid = it }
                Spacer(Modifier.height(8.dp))
                Field(pass, "Contraseña del Wi-Fi", password = true) { pass = it }
                Spacer(Modifier.height(12.dp))
                DialogButton(if (sending) "Enviando…" else "Enviar a la ampolleta", true, Modifier.fillMaxWidth()) {
                    if (sending) return@DialogButton
                    if (ssid.isBlank()) { Toast.makeText(ctx, "Escribe el nombre de tu Wi-Fi", Toast.LENGTH_SHORT).show(); return@DialogButton }
                    sending = true
                    scope.launch {
                        val ok = SmartHome.sendWifi(ctx, ssid.trim(), pass)
                        sending = false
                        Toast.makeText(
                            ctx,
                            if (ok) "¡Listo! La ampolleta se reinicia. Vuelve a tu Wi-Fi y pulsa Buscar."
                            else "No respondió. ¿Estás conectado a la red de la ampolleta?",
                            Toast.LENGTH_LONG
                        ).show()
                        if (ok) step = 0
                    }
                }
                Spacer(Modifier.height(8.dp))
                SmallButton("O abrir su página (192.168.4.1)", Modifier.fillMaxWidth()) {
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://192.168.4.1")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DialogButton("Cerrar", false, Modifier.weight(1f), onDismiss)
            DialogButton("📘 Guía", false, Modifier.weight(1f), onGuide)
        }
    }
}

// ---------------------------------------------------------------- guía

@Composable
private fun FlashGuideDialog(onDismiss: () -> Unit) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    fun open(url: String) = ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    GlassDialog(onDismiss) {
        GlowText("Liberar tu ampolleta de Tuya", 19.sp)
        Spacer(Modifier.height(8.dp))
        Column(Modifier.verticalScroll(rememberScrollState()).weight(1f, fill = false)) {
            GuideStep("1", "Revisa el chip",
                "Las ampolletas Smart Life traen un chip Wi-Fi. Si es Beken (BK7231T o BK7231N, módulos tipo WB2L, CB2L, WB3S…) se puede liberar. " +
                    "Si es BL602 o de otra marca, normalmente no. Busca tu modelo en la lista de equipos de Elektroda/OpenBeken.")
            GuideStep("2", "Sin soldar: tuya-cloudcutter",
                "Desde un computador con Linux (o una Raspberry Pi) y Wi-Fi, tuya-cloudcutter instala OpenBeken sin abrir la ampolleta. " +
                    "Usa el archivo \"UG\" de OpenBeken para tu chip (ej. OpenBK7231N_UG_…bin) y el perfil de tu modelo.")
            GuideStep("3", "Si no funciona: por cable",
                "Si Tuya ya actualizó tu ampolleta, cloudcutter puede fallar. Entonces hay que abrirla y programarla por cable (UART) con el flasher de OpenBeken.")
            GuideStep("4", "Configura los pines",
                "En la página de la ampolleta (192.168.4.1) aplica la plantilla de tu modelo para que el color y el blanco funcionen bien.")
            GuideStep("5", "Conéctala a Nioro Life",
                "En \"Añadir ampolleta → Nueva\" le envías tu Wi-Fi. Luego \"Buscar\" la encuentra y queda en Casa.")
            Spacer(Modifier.height(6.dp))
            Text(
                "⚠️ Cambiar el firmware tiene riesgo: si el chip no es el correcto, la ampolleta puede quedar inutilizable. " +
                    "Hazlo con calma y con una ampolleta que puedas arriesgar. Desconéctala de la corriente antes de abrirla.",
                color = p.debt, fontSize = 12.5.sp, fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallButton("OpenBeken", Modifier.weight(1f)) { open("https://github.com/openshwprojects/OpenBK7231T_App") }
            SmallButton("Cloudcutter", Modifier.weight(1f)) { open("https://github.com/tuya-cloudcutter/tuya-cloudcutter") }
        }
        Spacer(Modifier.height(8.dp))
        DialogButton("Entendido", true, Modifier.fillMaxWidth(), onDismiss)
    }
}

@Composable
private fun GuideStep(n: String, title: String, body: String) {
    val p = LocalPalette.current
    Row(Modifier.padding(vertical = 6.dp)) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(p.accentSoft), contentAlignment = Alignment.Center) {
            Text(n, color = p.title, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, color = p.title, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Text(body, color = p.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
