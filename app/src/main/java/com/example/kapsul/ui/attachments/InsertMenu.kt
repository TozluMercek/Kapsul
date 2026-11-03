package com.example.kapsul.ui.attachments

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File

enum class InsertAction(val label: String, val icon: ImageVector) {
    Pdf("PDF", Icons.Outlined.PictureAsPdf),
    Recording("Ses kaydı", Icons.Outlined.Mic),
    Image("Görüntü", Icons.Outlined.Image),
    Camera("Kamera", Icons.Outlined.PhotoCamera),
    Scan("Belge tarama", Icons.Outlined.DocumentScanner),
    AudioFile("Ses dosyası", Icons.Outlined.AudioFile),
    Drawing("Çizim", Icons.Outlined.Draw),
    Sticky("Yapışkan not", Icons.AutoMirrored.Outlined.StickyNote2),
    Table("Tablo", Icons.Outlined.TableChart),
}

/** The "add to note" menu; a dashed rule separates documents/recording from the rest. */
@Composable
fun InsertMenu(expanded: Boolean, onDismiss: () -> Unit, onAction: (InsertAction) -> Unit) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        InsertAction.entries.forEach { action ->
            DropdownMenuItem(
                text = { Text(action.label, style = MaterialTheme.typography.titleMedium) },
                leadingIcon = { Icon(action.icon, contentDescription = null) },
                onClick = {
                    onDismiss()
                    onAction(action)
                },
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            if (action == InsertAction.Recording) DashedDivider()
        }
    }
}

@Composable
private fun DashedDivider() {
    val color = MaterialTheme.colorScheme.outlineVariant
    Canvas(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .height(1.dp)
    ) {
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = size.height,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
        )
    }
}

/** Callbacks for everything the system pickers return. */
class InsertCallbacks(
    val onPdf: (Uri) -> Unit,
    val onImage: (Uri) -> Unit,
    val onAudioFile: (Uri) -> Unit,
    val onPhoto: (File) -> Unit,
    val onScan: (pdf: Uri, pageCount: Int) -> Unit,
    val onMessage: (String) -> Unit,
)

/**
 * Registers the system pickers (documents, photos, camera, ML Kit scanner) and returns
 * a launcher per action. In-app actions (recording, drawing, sticky, table) are handled by the caller.
 */
@Composable
fun rememberSystemInsertLaunchers(
    newPhotoFile: () -> File,
    uriFor: (File) -> Uri,
    callbacks: InsertCallbacks,
): (InsertAction) -> Boolean {
    val cb by rememberUpdatedState(callbacks)
    val activity: Activity? = LocalActivity.current
    var pendingPhoto by rememberSaveable { mutableStateOf<String?>(null) }

    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { cb.onPdf(it) }
    }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { cb.onAudioFile(it) }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { cb.onImage(it) }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val file = pendingPhoto?.let(::File)
        pendingPhoto = null
        if (file == null) return@rememberLauncherForActivityResult
        if (saved && file.length() > 0) cb.onPhoto(file) else file.delete()
    }
    val scanner = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val scan = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
        val pdf = scan?.pdf
        if (pdf != null) cb.onScan(pdf.uri, pdf.pageCount) else cb.onMessage("Tarama sonucu alınamadı")
    }

    return remember(activity) {
        fun launch(action: InsertAction): Boolean {
            when (action) {
                InsertAction.Pdf -> pdfPicker.launch(arrayOf("application/pdf"))
                InsertAction.AudioFile -> audioPicker.launch(arrayOf("audio/*"))
                InsertAction.Image -> imagePicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
                InsertAction.Camera -> {
                    val file = newPhotoFile()
                    pendingPhoto = file.absolutePath
                    try {
                        camera.launch(uriFor(file))
                    } catch (_: Exception) {
                        file.delete()
                        pendingPhoto = null
                        cb.onMessage("Kamera uygulaması bulunamadı")
                    }
                }
                InsertAction.Scan -> {
                    val options = GmsDocumentScannerOptions.Builder()
                        .setGalleryImportAllowed(true)
                        .setPageLimit(30)
                        .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
                        .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                        .build()
                    if (activity == null) {
                        cb.onMessage("Belge tarayıcı açılamadı")
                    } else {
                        GmsDocumentScanning.getClient(options)
                            .getStartScanIntent(activity)
                            .addOnSuccessListener { sender ->
                                scanner.launch(IntentSenderRequest.Builder(sender).build())
                            }
                            .addOnFailureListener {
                                cb.onMessage("Belge tarayıcı bu cihazda kullanılamıyor (Google Play hizmetleri gerekli)")
                            }
                    }
                }
                else -> return false
            }
            return true
        }
        ::launch
    }
}
