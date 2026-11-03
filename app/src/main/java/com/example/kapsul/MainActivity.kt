package com.example.kapsul

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kapsul.data.ThemeMode
import com.example.kapsul.ui.KapsulApp
import com.example.kapsul.ui.KapsulViewModel
import com.example.kapsul.ui.LaunchAction
import com.example.kapsul.ui.onboarding.OnboardingScreen
import com.example.kapsul.ui.theme.KapsulTheme

// FragmentActivity (a ComponentActivity) because BiometricPrompt needs it for note locks.
class MainActivity : FragmentActivity() {
    private val viewModel: KapsulViewModel by viewModels()
    private var launchAction by mutableStateOf<LaunchAction?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) launchAction = intent.toLaunchAction()
        val settingsRepository = (application as KapsulApplication).settings
        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle()
            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            // Keep status/navigation bar icons readable when the app overrides the system theme.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(LightScrim, DarkScrim) { darkTheme },
                )
                onDispose {}
            }

            KapsulTheme(
                darkTheme = darkTheme,
                dynamicColor = settings.dynamicColor && settingsRepository.dynamicColorSupported,
                seedColor = settings.seedColor,
                paletteStyle = settings.paletteStyle,
                amoled = settings.amoled,
            ) {
                if (!settings.onboarded) {
                    OnboardingScreen(onFinish = settingsRepository::setOnboarded)
                } else {
                    KapsulApp(
                        vm = viewModel,
                        launchAction = launchAction,
                        onLaunchActionHandled = { launchAction = null },
                    )
                }
            }
        }
    }

    private var stoppedAt = 0L

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) stoppedAt = System.currentTimeMillis()
    }

    /**
     * Locked notes need unlocking again after the app has been away for a minute. Not at once:
     * the camera, file pickers and the scanner also stop this activity while they're open.
     */
    override fun onStart() {
        super.onStart()
        if (stoppedAt != 0L && System.currentTimeMillis() - stoppedAt > RELOCK_AFTER_MS) viewModel.relockNotes()
        stoppedAt = 0L
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        launchAction = intent.toLaunchAction()
    }

    private fun Intent.toLaunchAction(): LaunchAction? = when (action) {
        ACTION_QUICK_NOTE -> LaunchAction.QuickNote
        ACTION_VOICE_NOTE -> LaunchAction.VoiceNote
        ACTION_ASK -> LaunchAction.Ask
        ACTION_OPEN_NOTE -> getLongExtra(EXTRA_NOTE_ID, -1).takeIf { it >= 0 }?.let(LaunchAction::OpenNote)
        else -> null
    }

    companion object {
        const val ACTION_QUICK_NOTE = "com.example.kapsul.action.QUICK_NOTE"
        const val ACTION_VOICE_NOTE = "com.example.kapsul.action.VOICE_NOTE"
        const val ACTION_ASK = "com.example.kapsul.action.ASK"
        private const val ACTION_OPEN_NOTE = "com.example.kapsul.action.OPEN_NOTE"
        private const val EXTRA_NOTE_ID = "noteId"
        private const val RELOCK_AFTER_MS = 60_000L

        /** Opens a note, e.g. from a reminder notification. */
        fun openNoteIntent(context: Context, noteId: Long): Intent =
            Intent(context, MainActivity::class.java)
                .setAction(ACTION_OPEN_NOTE)
                .putExtra(EXTRA_NOTE_ID, noteId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        // Same scrims androidx.activity uses by default for 3-button navigation.
        private val LightScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        private val DarkScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
