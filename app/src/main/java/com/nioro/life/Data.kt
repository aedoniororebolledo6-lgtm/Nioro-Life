package com.nioro.life

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

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
    val leadMin: Int = 30
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
            leadMin = p.getInt("lead", 30)
        )
    }

    fun saveSettings(c: Context, s: Settings) {
        prefs(c).edit()
            .putInt("bed", s.bedMin)
            .putInt("wake", s.wakeMin)
            .putBoolean("alarm", s.alarmOn)
            .putBoolean("bedNotify", s.bedNotify)
            .putInt("lead", s.leadMin)
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
}

fun fmtTime(minOfDay: Int): String = "%02d:%02d".format(minOfDay / 60, minOfDay % 60)

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
