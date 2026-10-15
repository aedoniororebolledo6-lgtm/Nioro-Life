package com.nioro.life

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Navy0 = Color(0xFF0A1450)
val Navy1 = Color(0xFF0B2468)
val Navy2 = Color(0xFF0B3A7C)
val Navy3 = Color(0xFF0B5088)
val Cream = Color(0xFFFFF3C4)
val CreamGlow = Color(0x99FFF3BE)
val Muted = Color(0xB0E2EAFF)

val AppBackground: Brush
    get() = Brush.verticalGradient(listOf(Navy0, Navy1, Navy2, Navy3))

/** Tarjeta de vidrio líquido: degradado translúcido, borde con brillo y luz superior. */
@Composable
fun Glass(
    modifier: Modifier = Modifier,
    radius: Dp = 30.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(radius)
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(if (pressed && onClick != null) 0.96f else 1f, label = "press")
    Box(
        modifier
            .graphicsLayer {
                scaleX = sc
                scaleY = sc
            }
            .shadow(14.dp, shape, clip = false, ambientColor = Color(0xFF020828), spotColor = Color(0xFF020828))
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0x30FFFFFF), Color(0x0DFFFFFF), Color(0x1CFFFFFF))
                )
            )
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0x40FFFFFF), Color.Transparent),
                        center = Offset(size.width * 0.25f, 0f),
                        radius = size.width * 0.9f
                    )
                )
            }
            .border(
                1.dp,
                Brush.verticalGradient(listOf(Color(0x70FFFFFF), Color(0x22FFFFFF))),
                shape
            )
            .then(
                if (onClick != null)
                    Modifier.clickable(interactionSource = src, indication = null, onClick = onClick)
                else Modifier
            ),
        content = content
    )
}

@Composable
fun GlowText(
    text: String,
    size: TextUnit,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.ExtraBold,
    color: Color = Cream,
    glow: Boolean = true
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = size,
        fontWeight = weight,
        style = TextStyle(shadow = if (glow) Shadow(CreamGlow, Offset.Zero, 24f) else null)
    )
}

@Composable
fun IconBadge(content: @Composable BoxScope.() -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        Modifier
            .size(54.dp)
            .clip(shape)
            .background(
                Brush.radialGradient(
                    listOf(Color(0x59FFFFFF), Color(0x24FFF3C4), Color(0x0DFFFFFF)),
                    center = Offset(40f, 30f),
                    radius = 160f
                )
            )
            .border(1.dp, Color(0x47FFFFFF), shape),
        contentAlignment = Alignment.Center,
        content = content
    )
}

/** Luna creciente igual a la del icono, dentro de una caja 100x100 escalada. */
fun moonPath(s: Float, ox: Float, oy: Float): Path = Path().apply {
    moveTo(ox + 62 * s, oy + 8 * s)
    cubicTo(ox + 34 * s, oy + 12 * s, ox + 14 * s, oy + 33 * s, ox + 14 * s, oy + 58 * s)
    cubicTo(ox + 14 * s, oy + 83 * s, ox + 34 * s, oy + 100 * s, ox + 58 * s, oy + 100 * s)
    cubicTo(ox + 74 * s, oy + 100 * s, ox + 87 * s, oy + 92 * s, ox + 94 * s, oy + 79 * s)
    cubicTo(ox + 67 * s, oy + 85 * s, ox + 44 * s, oy + 70 * s, ox + 44 * s, oy + 46 * s)
    cubicTo(ox + 44 * s, oy + 32 * s, ox + 50 * s, oy + 20 * s, ox + 62 * s, oy + 8 * s)
    close()
}

@Composable
fun Stars() {
    val stars = remember {
        val r = java.util.Random(7)
        List(42) { Triple(r.nextFloat(), r.nextFloat() * 0.85f, r.nextFloat() * 1.6f + 0.8f) }
    }
    val tr = rememberInfiniteTransition(label = "stars")
    val a by tr.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(3200), RepeatMode.Reverse),
        label = "twinkle"
    )
    Canvas(Modifier.fillMaxSize()) {
        for ((x, y, r) in stars) {
            drawCircle(Color.White.copy(alpha = a * (0.4f + r / 3f)), r.dp.toPx(), Offset(x * size.width, y * size.height))
        }
    }
}

@Composable
fun ClockIcon(dp: Dp = 30.dp, color: Color = Cream) {
    Canvas(Modifier.size(dp)) {
        val w = size.minDimension
        val sw = w * 0.075f
        val c = Offset(w / 2, w * 0.56f)
        drawCircle(color, radius = w * 0.34f, center = c, style = Stroke(sw, cap = StrokeCap.Round))
        drawLine(color, c, Offset(c.x, c.y - w * 0.2f), sw, StrokeCap.Round)
        drawLine(color, c, Offset(c.x + w * 0.13f, c.y + w * 0.09f), sw, StrokeCap.Round)
        drawLine(color, Offset(w * 0.12f, w * 0.22f), Offset(w * 0.26f, w * 0.1f), sw, StrokeCap.Round)
        drawLine(color, Offset(w * 0.88f, w * 0.22f), Offset(w * 0.74f, w * 0.1f), sw, StrokeCap.Round)
    }
}

@Composable
fun NoteIcon(dp: Dp = 30.dp, color: Color = Cream) {
    Canvas(Modifier.size(dp)) {
        val w = size.minDimension
        val sw = w * 0.075f
        val p = Path().apply {
            moveTo(w * 0.22f, w * 0.1f)
            lineTo(w * 0.62f, w * 0.1f)
            lineTo(w * 0.8f, w * 0.28f)
            lineTo(w * 0.8f, w * 0.9f)
            lineTo(w * 0.22f, w * 0.9f)
            close()
        }
        drawPath(p, color, style = Stroke(sw, cap = StrokeCap.Round))
        drawLine(color, Offset(w * 0.62f, w * 0.1f), Offset(w * 0.62f, w * 0.28f), sw, StrokeCap.Round)
        drawLine(color, Offset(w * 0.62f, w * 0.28f), Offset(w * 0.8f, w * 0.28f), sw, StrokeCap.Round)
        drawLine(color, Offset(w * 0.34f, w * 0.52f), Offset(w * 0.68f, w * 0.52f), sw, StrokeCap.Round)
        drawLine(color, Offset(w * 0.34f, w * 0.7f), Offset(w * 0.56f, w * 0.7f), sw, StrokeCap.Round)
    }
}

@Composable
fun BackIcon(dp: Dp = 22.dp, color: Color = Cream) {
    Canvas(Modifier.size(dp)) {
        val w = size.minDimension
        val sw = w * 0.1f
        drawLine(color, Offset(w * 0.65f, w * 0.18f), Offset(w * 0.3f, w * 0.5f), sw, StrokeCap.Round)
        drawLine(color, Offset(w * 0.3f, w * 0.5f), Offset(w * 0.65f, w * 0.82f), sw, StrokeCap.Round)
    }
}

@Composable
fun PlusIcon(dp: Dp = 32.dp, color: Color = Color(0xFFFFFBE6)) {
    Canvas(Modifier.size(dp)) {
        val w = size.minDimension
        val sw = w * 0.1f
        drawLine(color, Offset(w * 0.5f, w * 0.2f), Offset(w * 0.5f, w * 0.8f), sw, StrokeCap.Round)
        drawLine(color, Offset(w * 0.2f, w * 0.5f), Offset(w * 0.8f, w * 0.5f), sw, StrokeCap.Round)
    }
}

@Composable
fun CloseIcon(dp: Dp = 14.dp, color: Color = Muted) {
    Canvas(Modifier.size(dp)) {
        val w = size.minDimension
        val sw = w * 0.14f
        drawLine(color, Offset(w * 0.15f, w * 0.15f), Offset(w * 0.85f, w * 0.85f), sw, StrokeCap.Round)
        drawLine(color, Offset(w * 0.85f, w * 0.15f), Offset(w * 0.15f, w * 0.85f), sw, StrokeCap.Round)
    }
}
