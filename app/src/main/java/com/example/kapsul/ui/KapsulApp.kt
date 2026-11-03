package com.example.kapsul.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.kapsul.ui.ask.AskScreen
import com.example.kapsul.ui.capture.QuickCaptureSheet
import com.example.kapsul.ui.editor.EditorScreen
import com.example.kapsul.ui.home.HomeScreen
import com.example.kapsul.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

sealed interface Screen {
    data object Home : Screen
    data object Ask : Screen
    data object Settings : Screen
    data class Editor(val id: Long) : Screen

    fun encode(): String = when (this) {
        Home -> "home"
        Ask -> "ask"
        Settings -> "settings"
        is Editor -> "editor:$id"
    }

    companion object {
        fun decode(value: String): Screen = when {
            value == "ask" -> Ask
            value == "settings" -> Settings
            value.startsWith("editor:") -> Editor(value.removePrefix("editor:").toLong())
            else -> Home
        }
    }
}

/** Shortcut / widget entry points. */
sealed interface LaunchAction {
    data object QuickNote : LaunchAction
    data object VoiceNote : LaunchAction
    data object Ask : LaunchAction
    /** From a reminder notification or the alarm screen. */
    data class OpenNote(val id: Long) : LaunchAction
}

private val BackStackSaver = listSaver<SnapshotStateList<Screen>, String>(
    save = { stack -> stack.map { it.encode() } },
    restore = { saved -> saved.map(Screen::decode).toMutableStateList() },
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun KapsulApp(vm: KapsulViewModel, launchAction: LaunchAction?, onLaunchActionHandled: () -> Unit) {
    val backStack = rememberSaveable(saver = BackStackSaver) { mutableListOf<Screen>(Screen.Home).toMutableStateList() }
    var capture by rememberSaveable { mutableStateOf<String?>(null) } // null | "text" | "voice"
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun push(screen: Screen) {
        backStack.add(screen)
    }

    fun pop() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    fun showMessage(text: String) {
        scope.launch { snackbar.showSnackbar(text, withDismissAction = true) }
    }

    LaunchedEffect(launchAction) {
        when (launchAction) {
            LaunchAction.QuickNote -> capture = "text"
            LaunchAction.VoiceNote -> capture = "voice"
            LaunchAction.Ask -> if (backStack.lastOrNull() != Screen.Ask) push(Screen.Ask)
            is LaunchAction.OpenNote -> {
                val target = Screen.Editor(launchAction.id)
                if (backStack.lastOrNull() != target) push(target)
            }
            null -> return@LaunchedEffect
        }
        onLaunchActionHandled()
    }

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is UiEvent.Message -> showMessage(event.text)
                is UiEvent.Captured -> scope.launch {
                    val suggestion = event.suggestion
                    val result = snackbar.showSnackbar(
                        message = if (suggestion != null) {
                            "“${event.note.title}” kaydedildi. “${suggestion.title}” ile ilgili görünüyor."
                        } else {
                            "“${event.note.title}” kaydedildi"
                        },
                        actionLabel = if (suggestion != null) "Bağla" else "Aç",
                        withDismissAction = true,
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        if (suggestion != null) {
                            vm.link(event.note.id, suggestion.id)
                            showMessage("Notlar bağlandı")
                        } else {
                            push(Screen.Editor(event.note.id))
                        }
                    }
                }
            }
        }
    }

    BackHandler(enabled = backStack.size > 1) { pop() }

    val motion = MaterialTheme.motionScheme
    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = backStack.toList(),
            contentKey = { it.last() },
            transitionSpec = {
                val forward = targetState.size >= initialState.size
                val spatial = motion.defaultSpatialSpec<IntOffset>()
                val effects = motion.defaultEffectsSpec<Float>()
                (slideInHorizontally(spatial) { w -> if (forward) w / 4 else -w / 4 } + fadeIn(effects))
                    .togetherWith(slideOutHorizontally(spatial) { w -> if (forward) -w / 4 else w / 4 } + fadeOut(effects))
            },
            label = "screens",
        ) { stack ->
            when (val screen = stack.last()) {
                Screen.Home -> HomeScreen(
                    vm = vm,
                    onOpenNote = { push(Screen.Editor(it)) },
                    onNewNote = { vm.newNote { id -> push(Screen.Editor(id)) } },
                    onQuickCapture = { voice -> capture = if (voice) "voice" else "text" },
                    onAsk = { push(Screen.Ask) },
                    onSettings = { push(Screen.Settings) },
                )
                Screen.Settings -> SettingsScreen(
                    settingsRepository = vm.settings,
                    onBack = ::pop,
                )
                Screen.Ask -> AskScreen(
                    vm = vm,
                    onBack = ::pop,
                    onOpenNote = { push(Screen.Editor(it)) },
                    onVoiceUnavailable = { showMessage("Bu cihazda konuşma tanıma bulunamadı") },
                )
                is Screen.Editor -> EditorScreen(
                    noteId = screen.id,
                    vm = vm,
                    onBack = ::pop,
                    onOpenNote = { push(Screen.Editor(it)) },
                )
            }
        }

        SnackbarHost(
            snackbar,
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 88.dp),
        )
    }

    capture?.let { mode ->
        QuickCaptureSheet(
            startWithVoice = mode == "voice",
            intelligence = vm.ai,
            newAudioFile = { vm.store.newFile("m4a") },
            onDismiss = { capture = null },
            onSave = { text, source, recording ->
                vm.quickCapture(text, source, recording)
                capture = null
            },
            onMessage = ::showMessage,
        )
    }
}
