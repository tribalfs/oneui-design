package dev.oneuiproject.oneuiexample.ui.main.fragments.widgets

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.SeslSwitchBar
import androidx.appcompat.widget.SwitchCompat
import com.sec.sesl.tester.R
import com.sec.sesl.tester.databinding.FragmentWidgetsBinding
import dev.oneuiproject.oneui.ktx.seslSetFillHorizontalPaddingEnabled
import dev.oneuiproject.oneui.ktx.setEntries
import dev.oneuiproject.oneui.ktx.setSearchableInfoFrom
import dev.oneuiproject.oneuiexample.ui.main.MainActivity
import dev.oneuiproject.oneuiexample.ui.main.core.base.AbsBaseFragment
import dev.oneuiproject.oneuiexample.ui.main.core.util.autoCleared
import dev.oneuiproject.oneuiexample.ui.main.core.util.semToast

class WidgetsFragment : AbsBaseFragment(R.layout.fragment_widgets) {

    private val binding by autoCleared { FragmentWidgetsBinding.bind(requireView()) }
    private lateinit var seslSwitchBar: SeslSwitchBar

    override fun onDestroyView() {
        seslSwitchBar.removeOnSwitchChangeListener(listener)
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        seslSwitchBar = (requireActivity() as MainActivity).drawerLayout.switchBar
        seslSwitchBar.addOnSwitchChangeListener(listener)

        binding.fragmentSpinner.apply {
            val entries = listOf("Item 1", "Item 2", "Item 3","Item 4" )
            setEntries(
                entries = entries,
                onSelect = { position, _ ->
                    if (this@WidgetsFragment.isResumed) {
                        position?.let { semToast("${entries[it]} clicked!") }
                    }
                }
            )
        }

        binding.searchview.apply {
            setSearchableInfoFrom(requireActivity())
            seslSetUpButtonVisibility(View.VISIBLE)
            seslSetOnUpButtonClickListener{ semToast("SearchView Up button clicked!") }
        }

        binding.nsv.apply {
            seslSetFillHorizontalPaddingEnabled(true)
            seslSetGoToTopEnabled(true)
        }
    }

    private val listener =
        SeslSwitchBar.OnSwitchChangeListener { v: SwitchCompat?, isChecked: Boolean ->
            seslSwitchBar.setProgressBarVisible(true)
            seslSwitchBar.postDelayed({ seslSwitchBar.setProgressBarVisible(false) }, 3000)
        }

}