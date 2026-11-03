package com.example.kapsul.ui.components

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/**
 * Speech-to-text via the system recognizer, asking it to stay on-device where possible.
 * Returns a function that starts listening.
 */
@Composable
fun rememberVoiceInput(onResult: (String) -> Unit, onUnavailable: () -> Unit): () -> Unit {
    val currentOnResult by rememberUpdatedState(onResult)
    val currentOnUnavailable by rememberUpdatedState(onUnavailable)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.let(currentOnResult)
        }
    }
    return remember(launcher) {
        {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
                .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                .putExtra(RecognizerIntent.EXTRA_PROMPT, "Dinliyorum…")
            try {
                launcher.launch(intent)
            } catch (_: ActivityNotFoundException) {
                currentOnUnavailable()
            }
        }
    }
}
