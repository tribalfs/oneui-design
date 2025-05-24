package dev.oneuiproject.oneuiexample.ui.main.core.base

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import com.google.android.material.transition.MaterialSharedAxis

abstract class AbsBaseFragment(@LayoutRes layoutResId: Int): Fragment(layoutResId) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupFragmentTransitions()
    }

    private fun setupFragmentTransitions() {
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, true)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.Y, true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, false)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Y, false)
    }

}



