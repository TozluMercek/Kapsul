@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.example.kapsul.ui.attachments

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.kapsul.data.Attachment
import com.example.kapsul.data.AttachmentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

// ------------------------------------------------------------------ loading

/** Decodes an image no larger than [maxDim] px; honours EXIF rotation on API 28+. */
suspend fun loadBitmap(file: File, maxDim: Int): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
                val scale = max(info.size.width, info.size.height) / maxDim.toFloat()
                if (scale > 1f) {
                    decoder.setTargetSize((info.size.width / scale).toInt(), (info.size.height / scale).toInt())
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }.asImageBitmap()
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxDim) sample *= 2
            BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
                ?.asImageBitmap()
        }
    }.getOrNull()
}

fun pdfPageCount(file: File): Int = runCatching {
    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
        val renderer = PdfRenderer(pfd)
        try { renderer.pageCount } finally { renderer.close() }
    }
}.getOrDefault(0)

/** Renders one PDF page to a bitmap [widthPx] wide, on a white page. */
suspend fun renderPdfPage(file: File, index: Int, widthPx: Int): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            val renderer = PdfRenderer(pfd)
            try {
                val page = renderer.openPage(index)
                try {
                    val height = (widthPx.toFloat() * page.height / page.width).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(widthPx, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap.asImageBitmap()
                } finally {
                    page.close()
                }
            } finally {
                renderer.close()
            }
        }
    }.getOrNull()
}

// -------------------------------------------------------------------- cards

val StickyColors = listOf(
    0xFFFFF59D.toInt(), // sarı
    0xFFF8BBD0.toInt(), // pembe
    0xFFC5E1A5.toInt(), // yeşil
    0xFFB3E5FC.toInt(), // mavi
    0xFFFFCC80.toInt(), // turuncu
    0xFFD1C4E9.toInt(), // lila
)

/** Everything attached to a note, in the order it was added. */
@Composable
fun AttachmentList(
    attachments: List<Attachment>,
    fileOf: (String) -> File,
    onOpenImage: (Attachment) -> Unit,
    onOpenPdf: (Attachment) -> Unit,
    onEditSticky: (Attachment) -> Unit,
    onDelete: (Attachment) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        attachments.forEach { a ->
            val file = a.fileName?.let(fileOf)
            when (a.type) {
                AttachmentType.IMAGE, AttachmentType.DRAWING ->
                    if (file != null) ImageCard(file, a.type == AttachmentType.DRAWING, { onOpenImage(a) }, { onDelete(a) })
                AttachmentType.AUDIO -> if (file != null) AttachmentSurface(onDelete = { onDelete(a) }) {
                    AudioPlayer(file, a.label, a.durationMs, Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 48.dp))
                }
                AttachmentType.PDF -> if (file != null) PdfCard(file, a.label, { onOpenPdf(a) }, { onDelete(a) })
                AttachmentType.STICKY -> StickyCard(a, { onEditSticky(a) }, { onDelete(a) })
            }
        }
    }
}

@Composable
private fun AttachmentSurface(
    onDelete: () -> Unit,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Box {
            content()
            DeleteButton(onDelete, Modifier.align(Alignment.TopEnd))
        }
    }
}

@Composable
private fun DeleteButton(onClick: () -> Unit, modifier: Modifier = Modifier, onImage: Boolean = false) {
    IconButton(
        onClick = onClick,
        modifier = modifier.padding(4.dp),
        colors = if (onImage) IconButtonDefaults.iconButtonColors(
            containerColor = Color.Black.copy(alpha = 0.45f),
            contentColor = Color.White,
        ) else IconButtonDefaults.iconButtonColors(),
    ) {
        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Eki sil")
    }
}

@Composable
private fun ImageCard(file: File, isDrawing: Boolean, onOpen: () -> Unit, onDelete: () -> Unit) {
    val bitmap by produceState<ImageBitmap?>(null, file) { value = loadBitmap(file, 1600) }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(if (isDrawing) Color.White else MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(onClick = onOpen)
    ) {
        val bmp = bitmap
        if (bmp == null) {
            Box(Modifier.fillMaxWidth().aspectRatio(4f / 3f), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
        } else {
            Image(
                bmp,
                contentDescription = if (isDrawing) "Çizim" else "Görüntü",
                contentScale = if (isDrawing) ContentScale.Fit else ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio((bmp.width.toFloat() / bmp.height).coerceIn(0.6f, 2f)),
            )
        }
        DeleteButton(onDelete, Modifier.align(Alignment.TopEnd), onImage = true)
    }
}

@Composable
private fun PdfCard(file: File, label: String, onOpen: () -> Unit, onDelete: () -> Unit) {
    val density = LocalDensity.current
    val thumbPx = with(density) { 84.dp.roundToPx() }
    val thumb by produceState<ImageBitmap?>(null, file) { value = renderPdfPage(file, 0, thumbPx) }
    val pages by produceState(0, file) { value = withContext(Dispatchers.IO) { pdfPageCount(file) } }

    AttachmentSurface(onDelete = onDelete, onClick = onOpen) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(width = 84.dp, height = 108.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                val t = thumb
                if (t != null) Image(t, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, tint = Color.Gray)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f).padding(end = 40.dp)) {
                Text(label, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    if (pages > 0) "PDF · $pages sayfa" else "PDF",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StickyCard(sticky: Attachment, onEdit: () -> Unit, onDelete: () -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Surface(
            onClick = onEdit,
            color = Color(sticky.color),
            contentColor = Color(0xFF2B2B2B),
            shape = MaterialTheme.shapes.small,
            shadowElevation = 4.dp,
            modifier = Modifier
                .padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
                .fillMaxWidth(0.78f)
                .heightIn(min = 140.dp)
                .rotate(-1.5f),
        ) {
            Box {
                Text(
                    sticky.text.ifBlank { "Boş yapışkan not" },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 18.dp, top = 18.dp, bottom = 18.dp, end = 44.dp),
                )
                IconButton(onClick = onDelete, modifier = Modifier.align(Alignment.TopEnd)) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Yapışkan notu sil")
                }
            }
        }
    }
}

// ------------------------------------------------------------------ viewers

/** Full-screen photo/drawing viewer with pinch-to-zoom. */
@Composable
fun ImageViewerDialog(file: File, onDismiss: () -> Unit) {
    val bitmap by produceState<ImageBitmap?>(null, file) { value = loadBitmap(file, 3000) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transform = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, 6f)
        offset = if (scale == 1f) Offset.Zero else offset + pan
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .transformable(transform),
            contentAlignment = Alignment.Center,
        ) {
            bitmap?.let {
                Image(
                    it,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale; scaleY = scale
                            translationX = offset.x; translationY = offset.y
                        },
                )
            } ?: LoadingIndicator()
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape),
            ) { Icon(Icons.Filled.Close, contentDescription = "Kapat", tint = Color.White) }
        }
    }
}

/** In-app PDF reader: every page rendered on-device with [PdfRenderer]. */
@Composable
fun PdfViewerDialog(file: File, title: String, onDismiss: () -> Unit) {
    val pages by produceState(-1, file) { value = withContext(Dispatchers.IO) { pdfPageCount(file) } }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (pages > 0) {
                                Text("$pages sayfa", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                        }
                    },
                )
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ) { padding ->
            when {
                pages < 0 -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
                pages == 0 -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("PDF açılamadı") }
                else -> BoxWithConstraints(Modifier.padding(padding).fillMaxSize()) {
                    val widthPx = with(LocalDensity.current) { (maxWidth - 32.dp).roundToPx() }
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(pages) { index ->
                            val page by produceState<ImageBitmap?>(null, file, index, widthPx) {
                                value = renderPdfPage(file, index, widthPx)
                            }
                            Surface(shape = MaterialTheme.shapes.small, shadowElevation = 2.dp, color = Color.White) {
                                val p = page
                                if (p != null) {
                                    Image(p, contentDescription = "Sayfa ${index + 1}", modifier = Modifier.fillMaxWidth().aspectRatio(p.width.toFloat() / p.height))
                                } else {
                                    Box(Modifier.fillMaxWidth().aspectRatio(0.707f), contentAlignment = Alignment.Center) {
                                        LoadingIndicator()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
