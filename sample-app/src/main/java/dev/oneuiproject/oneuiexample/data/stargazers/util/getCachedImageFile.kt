package dev.oneuiproject.oneuiexample.data.stargazers.util

import android.content.Context
import android.util.Log
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

suspend fun getCachedImageFile(context: Context, imageUrl: String): File? = withContext(Dispatchers.IO) {
    try {
        val futureTarget = Glide.with(context)
            .asFile()
            .load(imageUrl)
            .submit()

        val cacheFile = futureTarget.get()
        Glide.with(context).clear(futureTarget)
        cacheFile
    } catch (e: Exception) {
        Log.e("Stargazer", "Error retrieving cached image", e)
        null
    }
}