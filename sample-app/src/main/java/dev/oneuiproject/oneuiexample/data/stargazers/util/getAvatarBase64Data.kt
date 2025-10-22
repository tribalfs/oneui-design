package dev.oneuiproject.oneuiexample.data.stargazers.util

import android.content.Context
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun getAvatarBase64Data(avatarUrl: String?, context: Context): String? = withContext(Dispatchers.IO) {
    try {
        if (!avatarUrl.isNullOrEmpty()) {
            val cacheFile = getCachedImageFile(context, avatarUrl) ?: return@withContext null
            Base64.encodeToString(cacheFile.readBytes(), Base64.DEFAULT)
        } else {
            null
        }
    } catch (e: Exception) {
        Log.e("Stargazer", "Error getting avatar image", e)
        null
    }
}