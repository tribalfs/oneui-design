package dev.oneuiproject.oneuiexample.ui.main.fragments.navi

import android.os.Bundle
import android.view.View
import com.sec.sesl.tester.R
import com.sec.sesl.tester.databinding.FragmentNaviBinding
import dev.oneuiproject.oneui.ktx.addTab
import dev.oneuiproject.oneuiexample.ui.main.MainActivity
import dev.oneuiproject.oneuiexample.ui.main.core.base.AbsBaseFragment
import dev.oneuiproject.oneuiexample.ui.main.core.util.autoCleared
import dev.oneuiproject.oneuiexample.ui.main.core.util.semToast

class NaviFragment : AbsBaseFragment(R.layout.fragment_navi) {

     private val binding by autoCleared { FragmentNaviBinding.bind(requireView()) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initSubTabs(view)
        initBNV(view)
    }

    override fun onStart() {
        super.onStart()
        (requireActivity() as MainActivity).apply{
            bottomTab.let {
                it.show(true)
                it.setOnMenuItemClickListener { menuItem ->
                    requireContext().semToast("${menuItem.title} selected!")
                    true
                }
            }
            drawerLayout.isImmersiveScroll = true
        }
    }

    override fun onStop() {
        super.onStop()
        (requireActivity() as MainActivity).apply{
            bottomTab.let {
                it.hide(false)
                it.setOnMenuItemClickListener(null)
            }
            drawerLayout.isImmersiveScroll = false
        }
    }

    private fun initSubTabs(view: View) {
        binding.tabsSubtab.apply {
            addTab("Subtab 4")
            addTab("Subtab 5")
            addTab("Subtab 6")
            addTab("Subtab 7")
            addTab("Subtab 8")
        }
    }

    private fun initBNV(view: View) {
        binding.tabsBottomnav.seslSetGroupDividerEnabled(true)
    }

}