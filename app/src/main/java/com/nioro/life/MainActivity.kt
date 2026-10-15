package com.nioro.life

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private var resumeTick by mutableIntStateOf(0)

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        Scheduler.ensureChannel(this)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        Scheduler.scheduleAll(this)
        setContent { NioroApp(resumeTick) }
    }

    override fun onResume() {
        super.onResume()
        resumeTick++
    }
}

@Composable
fun NioroApp(resumeTick: Int) {
    val ctx = LocalContext.current
    var settings by remember { mutableStateOf(Store.loadSettings(ctx)) }
    var notes by remember { mutableStateOf(Store.loadNotes(ctx)) }
    var screen by remember { mutableStateOf("home") }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(resumeTick) {
        settings = Store.loadSettings(ctx)
        notes = Store.loadNotes(ctx)
    }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    BackHandler(enabled = screen != "home") { screen = "home" }

    val updateSettings: (Settings) -> Unit = {
        settings = it
        Store.saveSettings(ctx, it)
        Scheduler.scheduleAll(ctx)
    }
    val saveNote: (Note) -> Unit = { n ->
        val list = notes.filter { it.id != n.id } + n
        notes = list
        Store.saveNotes(ctx, list)
        Scheduler.scheduleAll(ctx)
    }
    val deleteNote: (Note) -> Unit = { n ->
        Scheduler.cancelNote(ctx, n.id)
        val list = notes.filter { it.id != n.id }
        notes = list
        Store.saveNotes(ctx, list)
        Scheduler.scheduleAll(ctx)
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Cream, background = Navy0, surface = Navy1)) {
        Box(Modifier.fillMaxSize().background(AppBackground)) {
            Stars()
            Crossfade(targetState = screen, label = "nav") { s ->
                when (s) {
                    "clock" -> ClockScreen(settings, updateSettings) { screen = "home" }
                    "notes" -> NotesScreen(notes, saveNote, deleteNote) { screen = "home" }
                    else -> HomeScreen(settings, notes, now) { screen = it }
                }
            }
        }
    }
}
