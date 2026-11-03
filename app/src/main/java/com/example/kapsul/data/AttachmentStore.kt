package com.example.kapsul.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** Attachment files in the app's private storage; nothing here is shared or uploaded. */
class AttachmentStore(private val context: Context) {
    private val dir = File(context.filesDir, "attachments").apply { mkdirs() }

    fun file(name: String) = File(dir, name)

    fun newFile(extension: String) = File(dir, "${UUID.randomUUID()}.$extension")

    /** Page background images from the removed note-style feature are no longer used. */
    suspend fun deleteLegacyPageImages() = withContext(Dispatchers.IO) {
        dir.listFiles { f -> f.name.startsWith("page_") }?.forEach { it.delete() }
    }

    /** content:// URI for handing a file to the camera or another app. */
    fun uriFor(file: File): Uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)

    data class Imported(val file: File, val displayName: String)

    /** Copies a picked document (PDF, image, audio…) into private storage. */
    suspend fun import(uri: Uri, fallbackExtension: String): Imported = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val displayName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
            ?: "Dosya"
        val extension = displayName.substringAfterLast('.', "").ifEmpty {
            resolver.getType(uri)?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) } ?: fallbackExtension
        }
        val target = newFile(extension.lowercase())
        resolver.openInputStream(uri)?.use { input -> target.outputStream().use { input.copyTo(it) } }
            ?: error("Dosya açılamadı")
        Imported(target, displayName)
    }

    fun delete(name: String?) {
        if (name != null) file(name).delete()
    }

    fun audioDurationMs(file: File): Long = runCatching {
        MediaMetadataRetriever().run {
            try {
                setDataSource(file.absolutePath)
                extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            } finally {
                release()
            }
        }
    }.getOrDefault(0L)
}
