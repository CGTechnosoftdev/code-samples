package com.composebasics.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val MAX_DIMENSION = 1080

/** Displays an image from a file path or content Uri, decoded off the main thread and downsampled. */
@Composable
fun LocalImage(
    source: Any,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, source) {
        value = withContext(Dispatchers.IO) { runCatching { decode(context, source) }.getOrNull() }
    }
    val loaded = bitmap
    if (loaded != null) {
        Image(loaded.asImageBitmap(), contentDescription, modifier, contentScale = contentScale)
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant))
    }
}

private fun decode(context: Context, source: Any): Bitmap? {
    fun open() = when (source) {
        is File -> source.inputStream()
        is Uri -> context.contentResolver.openInputStream(source)
        else -> null
    }

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    open()?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= MAX_DIMENSION || bounds.outHeight / (sample * 2) >= MAX_DIMENSION) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    return open()?.use { BitmapFactory.decodeStream(it, null, options) }
}
