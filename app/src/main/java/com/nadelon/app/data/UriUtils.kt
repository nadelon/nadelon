package com.nadelon.app.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

fun Context.queryDisplayName(uri: Uri): String? = runCatching {
    contentResolver.query(uri, null, null, null, null)?.use { c ->
        val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
    } ?: uri.lastPathSegment
}.getOrNull()
