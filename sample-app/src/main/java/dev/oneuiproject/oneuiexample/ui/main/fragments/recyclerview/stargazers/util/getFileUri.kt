package dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.stargazers.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

fun File.getFileUri(context: Context): Uri =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", this)


