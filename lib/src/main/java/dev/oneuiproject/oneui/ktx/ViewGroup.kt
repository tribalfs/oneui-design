package dev.oneuiproject.oneui.ktx

import android.view.ViewGroup


/**
 * Recursively sets the enabled state of all child views within a ViewGroup.
 *
 * This function iterates through all children of the ViewGroup. For each child, it sets its
 * `isEnabled` property to the provided `enable` value. If a child is itself a ViewGroup, this
 * function is recursively called on that child to ensure all nested views are also updated.
 *
 * @param enable `true` to enable all child views, `false` to disable them.
 * @see android.view.View.setEnabled
 */
fun ViewGroup.setEnableRecursive(enable: Boolean) {
    isEnabled = enable
    for (i in 0 until childCount) {
        getChildAt(i).let {
            if (it is ViewGroup) {
                it.setEnableRecursive(enable)
            }else{
                it.isEnabled = enable
            }
        }
    }
}
