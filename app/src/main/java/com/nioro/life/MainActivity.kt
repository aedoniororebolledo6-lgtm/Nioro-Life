package com.nioro.life

import android.Manifest
import android.app.Activity
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
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
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
    var tx by remember { mutableStateOf(Store.loadTx(ctx)) }
    var cobros by remember { mutableStateOf(Store.loadCobros(ctx)) }
    var lists by remember { mutableStateOf(Store.loadLists(ctx)) }
    var screen by remember { mutableStateOf("home") }
    var showSettings by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(resumeTick) {
        settings = Store.loadSettings(ctx)
        notes = Store.loadNotes(ctx)
        tx = Store.loadTx(ctx)
        cobros = Store.loadCobros(ctx)
        lists = Store.loadLists(ctx)
    }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    BackHandler(enabled = screen != "home") { screen = "home" }

    val palette = if (settings.light) LightPalette else DarkPalette

    // Íconos de la barra de estado oscuros en el tema claro y claros en el oscuro.
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            val c = WindowCompat.getInsetsController(window, view)
            c.isAppearanceLightStatusBars = palette.light
            c.isAppearanceLightNavigationBars = palette.light
        }
    }

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
    val saveTx: (Tx) -> Unit = { t ->
        val list = tx.filter { it.id != t.id } + t
        tx = list
        Store.saveTx(ctx, list)
    }
    val saveCobro: (Cobro) -> Unit = { c ->
        val list = cobros.filter { it.id != c.id } + c
        cobros = list
        Store.saveCobros(ctx, list)
    }
    val deleteCobro: (Cobro) -> Unit = { c ->
        val list = cobros.filter { it.id != c.id }
        cobros = list
        Store.saveCobros(ctx, list)
    }
    val saveList: (CheckList) -> Unit = { l ->
        val list = lists.filter { it.id != l.id } + l
        lists = list
        Store.saveLists(ctx, list)
    }
    val deleteList: (CheckList) -> Unit = { l ->
        val list = lists.filter { it.id != l.id }
        lists = list
        Store.saveLists(ctx, list)
    }
    val deleteTx: (Tx) -> Unit = { t ->
        val list = tx.filter { it.id != t.id }
        tx = list
        Store.saveTx(ctx, list)
    }

    CompositionLocalProvider(LocalPalette provides palette) {
        val scheme = if (palette.light) {
            lightColorScheme(primary = palette.accent, background = palette.bg[0], surface = palette.bg[1])
        } else {
            darkColorScheme(primary = palette.accent, background = palette.bg[0], surface = palette.bg[1])
        }
        MaterialTheme(colorScheme = scheme) {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(palette.bg))) {
                Stars()
                Crossfade(targetState = screen, label = "nav") { s ->
                    when (s) {
                        "clock" -> ClockScreen(settings, updateSettings) { screen = "home" }
                        "notes" -> NotesScreen(notes, saveNote, deleteNote) { screen = "home" }
                        "finance" -> FinanceScreen(tx, cobros, saveTx, deleteTx, saveCobro, deleteCobro) { screen = "home" }
                        "lists" -> ListsScreen(lists, saveList, deleteList) { screen = "home" }
                        else -> HomeScreen(
                            settings, notes, tx, cobros, lists, now,
                            onGo = { screen = it },
                            onMenu = { showSettings = true }
                        )
                    }
                }
                if (showSettings) {
                    SettingsDialog(
                        settings = settings,
                        onTheme = { light -> updateSettings(settings.copy(light = light)) },
                        onDismiss = { showSettings = false }
                    )
                }
            }
        }
    }
}
