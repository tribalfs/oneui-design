package dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.stargazers.util

import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import dev.oneuiproject.oneui.widget.QRImageView

fun ImageView.loadImageFromUrl(imageUrl: String){
    Glide.with(context)
        .load(imageUrl)
        .error(dev.oneuiproject.oneui.R.drawable.ic_oui_error_2)
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .circleCrop()
        .into(this)
}

fun QRImageView.loadImageFromUrl(imageUrl: String){
    Glide.with(this)
        .load(imageUrl)
        .circleCrop()
        .into(object : CustomTarget<Drawable>() {
            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                setIcon(resource)
                invalidate()
            }
            override fun onLoadCleared(placeholder: Drawable?) {}
        })
}