package dev.oneuiproject.oneuiexample.ui.main.fragments.widgets

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SeslSwitchBar
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.isVisible
import com.google.android.material.appbar.model.ButtonModel
import com.google.android.material.appbar.model.SuggestAppBarModel
import com.google.android.material.appbar.model.view.SuggestAppBarView
import com.sec.sesl.tester.R
import com.sec.sesl.tester.databinding.FragmentWidgetsBinding
import dev.oneuiproject.oneui.ktx.seslSetFillHorizontalPaddingEnabled
import dev.oneuiproject.oneui.ktx.setEntries
import dev.oneuiproject.oneui.ktx.setSearchableInfoFrom
import dev.oneuiproject.oneui.layout.DrawerLayout
import dev.oneuiproject.oneuiexample.ui.main.MainActivity
import dev.oneuiproject.oneuiexample.ui.main.core.base.AbsBaseFragment
import dev.oneuiproject.oneuiexample.ui.main.core.util.autoCleared
import dev.oneuiproject.oneuiexample.ui.main.core.util.semToast

class WidgetsFragment : AbsBaseFragment(R.layout.fragment_widgets) {

    private val binding by autoCleared { FragmentWidgetsBinding.bind(requireView()) }
    private var drawerLayout: DrawerLayout? = null

    private val listener by lazy {
        SeslSwitchBar.OnSwitchChangeListener { v: SwitchCompat?, isChecked: Boolean ->
            drawerLayout?.switchBar?.apply {
                setProgressBarVisible(true)
                postDelayed({ setProgressBarVisible(false) }, 3000)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        drawerLayout = (requireActivity() as MainActivity).drawerLayout

        binding.fragmentSpinner.apply {
            val entries = listOf("Item 1", "Item 2", "Item 3", "Item 4")
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
            seslSetOnUpButtonClickListener { semToast("SearchView Up button clicked!") }
        }

        binding.nsv.apply {
            seslSetFillHorizontalPaddingEnabled(true)
            seslSetGoToTopEnabled(true)
        }

        binding.bottomTip.setOnLinkClickListener {
            semToast("More details clicked!")
        }
    }

    override fun onStart() {
        super.onStart()
        drawerLayout?.apply {
            switchBar.apply { isVisible = true; addOnSwitchChangeListener(listener) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setAppBarSuggestView(createSuggestAppBarModel())
            }
        }
    }

    override fun onStop() {
        super.onStop()
        drawerLayout?.apply {
            switchBar.apply { isVisible = false; removeOnSwitchChangeListener(listener) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setAppBarSuggestView(null)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun createSuggestAppBarModel(): SuggestAppBarModel<SuggestAppBarView> =
        SuggestAppBarModel.Builder(requireContext()).apply {
            setTitle("This is an a suggestion view")
            setCloseClickListener {_, _ -> drawerLayout?.setAppBarSuggestView(null) }
            setButtons(
                arrayListOf(
                    ButtonModel(
                        text = "Action Button",
                        clickListener = { _, _ ->
                            semToast("Action button clicked!")
                        }
                    )
                ))
        }.build()

}