package com.nioro.life

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

data class Note(
    val id: Long,
    val title: String,
    val text: String,
    val remindAt: Long?,
    val done: Boolean
)

data class Settings(
    val bedMin: Int = 23 * 60,
    val wakeMin: Int = 7 * 60,
    val alarmOn: Boolean = true,
    val bedNotify: Boolean = false,
    val leadMin: Int = 30,
    val light: Boolean = false
)

/** kind: 0 = ingreso, 1 = gasto, 2 = deuda. */
data class Tx(
    val id: Long,
    val kind: Int,
    val amount: Long,
    val title: String,
    val reason: String,
    val date: Long,
    val paid: Boolean
)

data class SleepState(val sleeping: Boolean, val fromMs: Long, val targetMs: Long)

object Store {
    private fun prefs(c: Context) = c.getSharedPreferences("nioro", Context.MODE_PRIVATE)

    fun loadSettings(c: Context): Settings {
        val p = prefs(c)
        return Settings(
            bedMin = p.getInt("bed", 23 * 60),
            wakeMin = p.getInt("wake", 7 * 60),
            alarmOn = p.getBoolean("alarm", true),
            bedNotify = p.getBoolean("bedNotify", false),
            leadMin = p.getInt("lead", 30),
            light = p.getBoolean("light", false)
        )
    }

    fun saveSettings(c: Context, s: Settings) {
        prefs(c).edit()
            .putInt("bed", s.bedMin)
            .putInt("wake", s.wakeMin)
            .putBoolean("alarm", s.alarmOn)
            .putBoolean("bedNotify", s.bedNotify)
            .putInt("lead", s.leadMin)
            .putBoolean("light", s.light)
            .apply()
    }

    fun loadNotes(c: Context): List<Note> {
        val raw = prefs(c).getString("notes", "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Note(
                    id = o.getLong("id"),
                    title = o.optString("title", ""),
                    text = o.optString("text", ""),
                    remindAt = if (o.has("remindAt") && !o.isNull("remindAt")) o.getLong("remindAt") else null,
                    done = o.optBoolean("done", false)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveNotes(c: Context, list: List<Note>) {
        val arr = JSONArray()
        for (n in list) {
            val o = JSONObject()
            o.put("id", n.id)
            o.put("title", n.title)
            o.put("text", n.text)
            if (n.remindAt != null) o.put("remindAt", n.remindAt)
            o.put("done", n.done)
            arr.put(o)
        }
        prefs(c).edit().putString("notes", arr.toString()).apply()
    }

    fun loadTx(c: Context): List<Tx> {
        val raw = prefs(c).getString("tx", "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Tx(
                    id = o.getLong("id"),
                    kind = o.optInt("kind", 1),
                    amount = o.optLong("amount", 0L),
                    title = o.optString("title", ""),
                    reason = o.optString("reason", ""),
                    date = o.optLong("date", System.currentTimeMillis()),
                    paid = o.optBoolean("paid", false)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveTx(c: Context, list: List<Tx>) {
        val arr = JSONArray()
        for (t in list) {
            val o = JSONObject()
            o.put("id", t.id)
            o.put("kind", t.kind)
            o.put("amount", t.amount)
            o.put("title", t.title)
            o.put("reason", t.reason)
            o.put("date", t.date)
            o.put("paid", t.paid)
            arr.put(o)
        }
        prefs(c).edit().putString("tx", arr.toString()).apply()
    }
}

fun fmtTime(minOfDay: Int): String = "%02d:%02d".format(minOfDay / 60, minOfDay % 60)

fun fmtMoney(v: Long): String {
    val n = NumberFormat.getIntegerInstance(Locale("es", "CL")).format(Math.abs(v))
    return (if (v < 0) "-" else "") + "$" + n
}

fun sleepDurMin(s: Settings): Int {
    val d = ((s.wakeMin - s.bedMin) + 1440) % 1440
    return if (d == 0) 1440 else d
}

fun fmtDur(min: Int): String {
    val h = min / 60
    val m = min % 60
    return if (m == 0) "$h h" else "$h h $m min"
}

fun dayAt(base: Long, minOfDay: Int, dayOffset: Int): Long {
    val c = Calendar.getInstance()
    c.timeInMillis = base
    c.add(Calendar.DAY_OF_YEAR, dayOffset)
    c.set(Calendar.HOUR_OF_DAY, minOfDay / 60)
    c.set(Calendar.MINUTE, minOfDay % 60)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

fun nextOccurrence(minOfDay: Int, now: Long): Long {
    val t = dayAt(now, minOfDay, 0)
    return if (t <= now) dayAt(now, minOfDay, 1) else t
}

fun computeSleep(s: Settings, now: Long): SleepState {
    val dur = sleepDurMin(s) * 60_000L
    for (off in intArrayOf(-1, 0)) {
        val start = dayAt(now, s.bedMin, off)
        val end = start + dur
        if (now >= start && now < end) return SleepState(true, start, end)
    }
    val next = nextOccurrence(s.bedMin, now)
    val prevEnd = next - 86_400_000L + dur
    val from = if (prevEnd > now) next - 16 * 3_600_000L else prevEnd
    return SleepState(false, from, next)
}

// ---------------------------------------------------------------- calculadora

/** Evalúa cuentas como "450000+35000", "1.500.000-200000", "(2*35000)+1200". Devuelve null si no es válida. */
fun evalExpr(raw: String): Double? {
    var s = raw.replace(" ", "").replace("×", "*").replace("÷", "/").replace("−", "-")
    s = s.replace(Regex("(?<=\\d)\\.(?=\\d{3}(?!\\d))"), "")
    s = s.replace(',', '.')
    if (s.isEmpty()) return null
    return try {
        ExprParser(s).parse()
    } catch (e: Exception) {
        null
    }
}

private class ExprParser(val s: String) {
    var pos = 0

    fun parse(): Double? {
        val v = expr() ?: return null
        return if (pos == s.length && v.isFinite()) v else null
    }

    private fun expr(): Double? {
        var v = term() ?: return null
        while (pos < s.length && (s[pos] == '+' || s[pos] == '-')) {
            val op = s[pos++]
            val r = term() ?: return null
            v = if (op == '+') v + r else v - r
        }
        return v
    }

    private fun term(): Double? {
        var v = factor() ?: return null
        while (pos < s.length && (s[pos] == '*' || s[pos] == '/')) {
            val op = s[pos++]
            val r = factor() ?: return null
            if (op == '/' && r == 0.0) return null
            v = if (op == '*') v * r else v / r
        }
        return v
    }

    private fun factor(): Double? {
        if (pos >= s.length) return null
        if (s[pos] == '-') {
            pos++
            val f = factor() ?: return null
            return -f
        }
        if (s[pos] == '(') {
            pos++
            val v = expr() ?: return null
            if (pos >= s.length || s[pos] != ')') return null
            pos++
            return v
        }
        val start = pos
        while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) pos++
        if (start == pos) return null
        return s.substring(start, pos).toDoubleOrNull()
    }
}
