package dev.oneuiproject.oneui.qr.app.internal

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

fun <T: Any> LiveData<T>.observeOnce(owner: LifecycleOwner, action: (T) -> Boolean) {
    val observer = object: Observer<T> {
        override fun onChanged(value: T) {
            if (action(value)) {
                removeObserver(this)
            }
        }
    }
    observe(owner, observer)
}