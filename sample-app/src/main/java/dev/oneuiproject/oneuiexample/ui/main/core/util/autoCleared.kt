package dev.oneuiproject.oneuiexample.ui.main.core.util

import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Property delegate that automatically clears it's value
 * on the Fragment's onDestroyView as required in
 * https://developer.android.com/topic/libraries/view-binding#fragments
 */
fun <T> Fragment.autoCleared(initialize: () -> T): ReadOnlyProperty<Fragment, T> =
    object : ReadOnlyProperty<Fragment, T>, DefaultLifecycleObserver {
        private var _value: T? = null

        override fun onDestroy(owner: LifecycleOwner) {
            _value = null
        }

        override fun getValue(thisRef: Fragment, property: KProperty<*>): T =
            _value ?: initialize().also { _value = it; viewLifecycleOwner.lifecycle.addObserver(this) }
    }

