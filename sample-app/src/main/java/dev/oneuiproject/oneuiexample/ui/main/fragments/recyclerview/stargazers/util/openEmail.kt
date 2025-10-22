package dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.stargazers.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

fun Context.openEmail(email: String, subject: String? = null, body: String? = null,
                      attachmentFileUri: Uri? = null,
                      onException: ((Exception) -> Unit)? = null ){
    try {
        val browserSelectorIntent = Intent().apply {
            setAction(Intent.ACTION_VIEW)
            addCategory(Intent.CATEGORY_BROWSABLE)
            setData(Uri.parse("mailto:"))
        }

        val targetIntent = Intent().apply {
            setAction(Intent.ACTION_VIEW)
            addCategory(Intent.CATEGORY_BROWSABLE)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            setData(Uri.parse(email))
            if (attachmentFileUri != null) {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_STREAM, attachmentFileUri)
            }
        }


        targetIntent.apply {
            if (getPackage() == null && selector != browserSelectorIntent) {
                selector = browserSelectorIntent
            }
        }

        if (this !is Activity) {
            targetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(targetIntent)
    }
    catch(e: Exception){
        Log.e("openUrl", e.message.toString())
        onException?.invoke(e)
    }
}