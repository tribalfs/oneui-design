package dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout.INDICATOR_ANIMATION_MODE_LINEAR
import com.google.android.material.tabs.TabLayout.Tab
import com.sec.sesl.tester.R
import com.sec.sesl.tester.databinding.FragmentRecyclerBinding
import dev.oneuiproject.oneui.utils.TabPagerMediator
import dev.oneuiproject.oneuiexample.ui.main.core.base.AbsBaseFragment
import dev.oneuiproject.oneuiexample.ui.main.core.util.autoCleared
import dev.oneuiproject.oneuiexample.ui.main.core.util.launchAndRepeatWithViewLifecycle
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.apps.AppPickerFragment
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.contacts.ContactsFragment
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.icons.IconsFragment
import kotlinx.coroutines.flow.collectLatest

class RvParentFragment : AbsBaseFragment(R.layout.fragment_recycler) {
    private val binding by autoCleared { FragmentRecyclerBinding.bind(requireView()) }
    private val viewModel by viewModels<RvParentViewModel>()
    private lateinit var tabPagerMediator: TabPagerMediator

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initTabbedViewPager()
        launchAndRepeatWithViewLifecycle {
            viewModel.isTabLayoutEnabledStateFlow
                .collectLatest {
                    tabPagerMediator.setInteractionEnabled(it)
                }
        }
    }

    private fun initTabbedViewPager() {
        binding.vp2.apply {
            adapter = ViewPagerAdapter(this@RvParentFragment)
            //seslSetSuggestionPaging(true)
        }
        val titles = listOf("Icons", "Contacts", "Apps")
        tabPagerMediator = TabPagerMediator(this, binding.tabs, binding.vp2) { tab: Tab?, pos ->
            tab?.text = titles[pos]
        }
    }

    private class ViewPagerAdapter(fm: Fragment) : FragmentStateAdapter(fm) {
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> IconsFragment()
                1 -> ContactsFragment()
                2 -> AppPickerFragment()
                else -> throw IllegalArgumentException("Invalid position")
            }
        }

        override fun getItemCount() = 3
    }
}
