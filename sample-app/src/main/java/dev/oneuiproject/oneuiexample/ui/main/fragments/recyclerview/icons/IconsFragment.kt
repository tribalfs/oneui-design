package dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.icons

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.sec.sesl.tester.R
import com.sec.sesl.tester.databinding.FragmentIconsBinding
import dagger.hilt.android.AndroidEntryPoint
import dev.oneuiproject.oneui.delegates.AppBarAwareYTranslator
import dev.oneuiproject.oneui.delegates.ViewYTranslator
import dev.oneuiproject.oneui.ktx.clearBadge
import dev.oneuiproject.oneui.ktx.dpToPx
import dev.oneuiproject.oneui.ktx.hideSoftInput
import dev.oneuiproject.oneui.ktx.setBadge
import dev.oneuiproject.oneui.layout.Badge
import dev.oneuiproject.oneui.layout.ToolbarLayout
import dev.oneuiproject.oneui.layout.ToolbarLayout.ActionModeListener
import dev.oneuiproject.oneui.layout.ToolbarLayout.SearchModeListener
import dev.oneuiproject.oneui.layout.ToolbarLayout.SearchModeOnBackBehavior
import dev.oneuiproject.oneui.layout.ToolbarLayout.SearchOnActionMode.Concurrent
import dev.oneuiproject.oneui.recyclerview.ktx.enableCoreSeslFeatures
import dev.oneuiproject.oneui.recyclerview.ktx.hideSoftInputOnScroll
import dev.oneuiproject.oneui.recyclerview.ktx.seslSetFastScrollerAdditionalPadding
import dev.oneuiproject.oneui.utils.ItemDecorRule
import dev.oneuiproject.oneui.utils.SemItemDecoration
import dev.oneuiproject.oneui.widget.TipPopup
import dev.oneuiproject.oneuiexample.ui.main.MainActivity
import dev.oneuiproject.oneuiexample.ui.main.MainViewModel
import dev.oneuiproject.oneuiexample.ui.main.core.base.AbsBaseFragment
import dev.oneuiproject.oneuiexample.ui.main.core.util.autoCleared
import dev.oneuiproject.oneuiexample.ui.main.core.util.launchAndRepeatWithViewLifecycle
import dev.oneuiproject.oneuiexample.ui.main.core.util.showTipPopup
import dev.oneuiproject.oneuiexample.ui.main.core.util.suggestiveSnackBar
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.RvParentViewModel
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.icons.adapter.IconsAdapter
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class IconsFragment : AbsBaseFragment(R.layout.fragment_icons),
    ViewYTranslator by AppBarAwareYTranslator() {

    private val binding by autoCleared { FragmentIconsBinding.bind(requireView()) }
    private val viewModel by viewModels<IconsViewModel>()
    private val parentViewModel by viewModels<RvParentViewModel>( {requireParentFragment()} )
    private val mainViewModel by activityViewModels<MainViewModel>()
    private lateinit var toolbarLayout: ToolbarLayout
    private var rvTipView: TipPopup? = null

    private val iconsAdapter: IconsAdapter by lazy {
        IconsAdapter(
            onAllSelectorStateChanged = { ass ->
                toolbarLayout.updateAllSelector(ass.totalSelected, ass.isEnabled, ass.isChecked)
            },
            onBlockActionMode = { launchActionMode() },
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as MainActivity).let{
            toolbarLayout = it.drawerLayout
            it.addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)
        }

        setupRecyclerView()
        observeUIState()

        if (viewModel.isSearchMode) launchSearchMode()
        if (viewModel.isActionMode) {
            launchActionMode(viewModel.initialSelectedIds?.toSet())
            viewModel.initialSelectedIds = null //not anymore needed
        }

        showTipPopup(
            message = "Long-press item to trigger multi-selection.",
            delayMillis = 1_000,
            getAnchor = { binding.recyclerView.layoutManager!!.findViewByPosition(2) },
            onCreate = { rvTipView = this }
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
         rvTipView?.update()
    }

    private fun observeUIState() {
        launchAndRepeatWithViewLifecycle {
            launch {
                viewModel.iconsListStateFlow
                    .collectLatest {
                        if (it == null) {
                            binding.progressBar.isVisible = true
                        } else {
                            val itemsList = it.itemsList
                            updateRecyclerViewVisibility(itemsList.isNotEmpty(), it.noItemText)
                            iconsAdapter.submitList(itemsList)
                            iconsAdapter.highlightWord = it.query
                            binding.progressBar.isVisible = false
                        }
                    }
            }

            launch {
                mainViewModel.isCtrlKeyPressed
                    .collect {
                        binding.recyclerView.seslSetCtrlkeyPressed(it)
                        updateTabLayoutState()
                    }
            }
        }
    }

    private fun updateRecyclerViewVisibility(visible: Boolean, noItemText: String) {
        binding.tvNoItem.text = noItemText
        binding.nsvNoItem.isVisible = !visible
        binding.recyclerView.isVisible = visible
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        iconsAdapter.getSelectedIds().let {
            if (it.isNotEmpty()) {
                viewModel.initialSelectedIds = it.toTypedArray()
            }
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            itemAnimator = null
            setAdapter(iconsAdapter.apply { setupOnClickListeners() })
            addItemDecoration(
                SemItemDecoration(
                    requireContext(),
                    ItemDecorRule.ALL,
                    ItemDecorRule.NONE
                )
            )
            enableCoreSeslFeatures()
            hideSoftInputOnScroll()
            seslSetFastScrollerAdditionalPadding(8.dpToPx(resources))
        }

        iconsAdapter.configureWith(
            binding.recyclerView
        )

        binding.tvNoItem.translateYWithAppBar(
            this@IconsFragment.toolbarLayout.appBarLayout,
            this
        )
    }

    private fun IconsAdapter.setupOnClickListeners() {
        onClickItem = { icon, position ->
            if (isActionMode) {
                toggleItem(icon.id)
            } else {
                requireActivity().hideSoftInput()
                suggestiveSnackBar("${icon.name} clicked!", duration = Snackbar.LENGTH_SHORT)
            }
        }

        onLongClickItem = {
            if (!isActionMode) launchActionMode()
            binding.recyclerView.seslStartLongPressMultiSelection()
        }
    }

    private val searchModeListener: SearchModeListener = object : SearchModeListener {
        override fun onQueryTextSubmit(query: String?): Boolean {
            viewModel.setQuery(query)
            return true
        }

        override fun onQueryTextChange(newText: String?): Boolean {
            viewModel.setQuery(newText)
            return true
        }

        override fun onSearchModeToggle(searchView: SearchView, isActive: Boolean) {
            if (isActive) {
                searchView.queryHint = "Search icon"
            } else {
                viewModel.setQuery("")
            }

            //Ignore if it's action mode search
            if (viewModel.isActionMode) return
            viewModel.isSearchMode = isActive
            updateTabLayoutState()
        }
    }

    private fun launchActionMode(selectedIds: Set<Int>? = null) {
        viewModel.isActionMode = true
        updateTabLayoutState()
        iconsAdapter.toggleActionMode(true, selectedIds)

        toolbarLayout.startActionMode(
            object : ActionModeListener {
                override fun onInflateActionMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu_action_mode_icons, menu)
                }

                override fun onEndActionMode() {
                    viewModel.isActionMode = false
                    updateTabLayoutState()
                    iconsAdapter.toggleActionMode(false, null)
                }

                override fun onMenuItemClicked(item: MenuItem): Boolean {
                    if (item.itemId == R.id.icons_am_menu1
                        || item.itemId == R.id.icons_am_menu2
                    ) {
                        val selectedCount = iconsAdapter.getSelectedIds().count()
                        suggestiveSnackBar("$selectedCount icons selected for ${item.title}", duration = Snackbar.LENGTH_SHORT)
                        toolbarLayout.endActionMode()
                        return true
                    }
                    return false
                }

                override fun onSelectAll(isChecked: Boolean) {
                    iconsAdapter.onToggleSelectAll(isChecked)
                }
            },
            Concurrent(searchModeListener),
            null as? StateFlow<ToolbarLayout.AllSelectorState>?
        )
    }

    private fun launchSearchMode() {
        toolbarLayout.startSearchMode(searchModeListener, SearchModeOnBackBehavior.DISMISS)
    }

    private fun updateTabLayoutState() {
        parentViewModel.isTabLayoutEnabled = !(viewModel.isSearchMode || viewModel.isActionMode || mainViewModel.isCtrlKeyPressed.value)
    }

    private val menuProvider = object : MenuProvider {
        private lateinit var menu: Menu

        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_icons, menu)
            this.menu = menu
            val menuItemInfo = menu.findItem(R.id.menu_icons_info)
            menuItemInfo.setBadge(Badge.DOT)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            when(menuItem.itemId){
                R.id.menu_icons_info -> {
                    showInfoAlertDialog()
                    return true
                }
                R.id.menu_icons_search -> {
                    launchSearchMode()
                    return true
                }
            }
            return false
        }

        fun findItem(id: Int) = menu.findItem(id)
    }

    private fun showInfoAlertDialog(){
        AlertDialog.Builder(requireContext()).apply {
            setMessage("If you're wondering whether these icons are outdated, yes - they're from OneUI 4. " +
                    "Don't ask us why. But let us know if you think you can help update them.")
            setPositiveButton("OK") { _, _ ->
                menuProvider.findItem(R.id.menu_icons_info).clearBadge() }
            show()
        }
    }

    companion object {
        private const val KEY_IS_SEARCH_MODE = "is_search_mode"
        private const val KEY_IS_ACTION_MODE = "is_action_mode"
        private const val KEY_ACTION_MODE_SELECTED_IDS = "action_mode_selected_ids"
    }
}
