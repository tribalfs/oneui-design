package dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.stargazers

import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.LayoutDirection
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.graphics.toColorInt
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper.END
import androidx.recyclerview.widget.ItemTouchHelper.START
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.sec.sesl.tester.R
import com.sec.sesl.tester.databinding.FragmentStargazersBinding
import com.sec.sesl.tester.databinding.ViewStargazersOptionsDialogBinding
import dagger.hilt.android.AndroidEntryPoint
import dev.oneuiproject.oneui.delegates.AppBarAwareYTranslator
import dev.oneuiproject.oneui.delegates.ViewYTranslator
import dev.oneuiproject.oneui.dialog.ProgressDialog
import dev.oneuiproject.oneui.dialog.ProgressDialog.ProgressStyle
import dev.oneuiproject.oneui.ktx.dpToPx
import dev.oneuiproject.oneui.ktx.setBadge
import dev.oneuiproject.oneui.layout.Badge
import dev.oneuiproject.oneui.layout.NavDrawerLayout
import dev.oneuiproject.oneui.layout.ToolbarLayout
import dev.oneuiproject.oneui.layout.ToolbarLayout.SearchModeOnBackBehavior
import dev.oneuiproject.oneui.layout.ToolbarLayout.SearchModeOnBackBehavior.CLEAR_DISMISS
import dev.oneuiproject.oneui.layout.startActionMode
import dev.oneuiproject.oneui.recyclerview.ktx.configureImmBottomPadding
import dev.oneuiproject.oneui.recyclerview.ktx.configureItemSwipeAnimator
import dev.oneuiproject.oneui.recyclerview.ktx.enableCoreSeslFeatures
import dev.oneuiproject.oneui.recyclerview.ktx.hideSoftInputOnScroll
import dev.oneuiproject.oneui.utils.ItemDecorRule
import dev.oneuiproject.oneui.utils.SemItemDecoration
import dev.oneuiproject.oneui.widget.TipPopup
import dev.oneuiproject.oneuiexample.data.stargazers.model.ActionModeSearch
import dev.oneuiproject.oneuiexample.data.stargazers.model.FetchState
import dev.oneuiproject.oneuiexample.ui.main.MainActivity
import dev.oneuiproject.oneuiexample.ui.main.MainViewModel
import dev.oneuiproject.oneuiexample.ui.main.core.base.AbsBaseFragment
import dev.oneuiproject.oneuiexample.ui.main.core.util.autoCleared
import dev.oneuiproject.oneuiexample.ui.main.core.util.launchAndRepeatWithViewLifecycle
import dev.oneuiproject.oneuiexample.ui.main.core.util.showTipPopup
import dev.oneuiproject.oneuiexample.ui.main.core.util.suggestiveSnackBar
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.RvParentViewModel
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.stargazers.StargazerProfileActivity.Companion.KEY_STARGAZER
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.stargazers.adapter.StargazersAdapter
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.stargazers.model.StargazersListItemUiModel
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.stargazers.model.StargazersListItemUiModel.StargazerItem
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.stargazers.util.toSearchOnActionMode
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.stargazers.util.updateIndexer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.getValue
import dev.oneuiproject.oneui.R as iconsLibR

@AndroidEntryPoint
class StargazersFragment : AbsBaseFragment(R.layout.fragment_stargazers),
    ViewYTranslator by AppBarAwareYTranslator() {

    private val binding by autoCleared { FragmentStargazersBinding.bind(requireView()) }
    private val viewModel: StargazersViewModel by viewModels()
    private val parentViewModel by viewModels<RvParentViewModel>({ requireParentFragment() })
    private lateinit var navDrawerLayout: NavDrawerLayout
    private val mainViewModel by activityViewModels<MainViewModel>()

    private val stargazersAdapter: StargazersAdapter by lazy {
        StargazersAdapter(
            onAllSelectorStateChanged = { viewModel.allSelectorStateFlow.value = it },
            onBlockActionMode = ::launchActionMode
        )
    }
    private var rvTipView: TipPopup? = null
    private var fabTipPopup: TipPopup? = null
    private var lastStateReceived: FetchState? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as MainActivity).let {
            navDrawerLayout = it.drawerLayout
            it.addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)
        }

        configureRecyclerView()
        configureSwipeRefresh()
        configureItemSwipeAnimator()
        observeUIState()

        if (viewModel.isSearchMode) navDrawerLayout.launchSearchMode(CLEAR_DISMISS)
        if (viewModel.isActionMode) {
            launchActionMode(viewModel.initialSelectedIds)
            viewModel.initialSelectedIds = null //not anymore needed
        }

        showTipPopup(
            message = "Swipe left to call, swipe right to message.",
            delayMillis = 1_000,
            getAnchor = { binding.recyclerView.layoutManager!!.findViewByPosition(2) },
            onCreate = {
                rvTipView = this
                setAction("Close") { }
            }
        )

        binding.fab.setOnClickListener {
            showTipPopup(
                message = "Star these github repositories: " +
                        "\n• OneUI Design lib" +
                        "\n• sesl-androidx " +
                        "\n• sesl-material.",
                getAnchor = { it },
                expanded = true,
                mode = TipPopup.Mode.NORMAL,
                dismissOnPaused = false
            ) {
                fabTipPopup = this
                setBackgroundColorWithAlpha("#EAB65205".toColorInt())
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        rvTipView?.update()
        fabTipPopup?.update()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stargazersAdapter.getSelectedIds().let {
            if (it.isNotEmpty()) {
                viewModel.initialSelectedIds = it
            }
        }
    }

    private fun configureRecyclerView() {
        binding.recyclerView.apply rv@{
            setLayoutManager(LinearLayoutManager(requireContext()))
            setAdapter(stargazersAdapter.also { it.setupOnClickListeners() })
            addItemDecoration(
                SemItemDecoration(requireContext(),
                    dividerRule = ItemDecorRule.SELECTED{
                        it.itemViewType == StargazerItem.VIEW_TYPE
                    },
                    subHeaderRule = ItemDecorRule.SELECTED{
                        it.itemViewType == StargazersListItemUiModel.SeparatorItem.VIEW_TYPE
                    }
                ).apply { setDividerInsetStart(78.dpToPx(resources)) }
            )
            setItemAnimator(null)
            enableCoreSeslFeatures(fastScrollerEnabled = false)
            stargazersAdapter.configureWith(this)
            binding.fab.hideOnScroll(this@rv/*, binding.indexscrollView*/)
            binding.indexscrollView.attachToRecyclerView(this@rv)
            hideSoftInputOnScroll()
            if (Build.VERSION.SDK_INT >= 30){
                configureImmBottomPadding(navDrawerLayout)
            }
        }

        translateYWithAppBar(
            setOf(binding.nsvNoItem, binding.loadingPb),
            navDrawerLayout.appBarLayout,
            this@StargazersFragment
        )
    }


    private fun configureSwipeRefresh() {
        /*binding.swiperefreshView.apply {
            setOnRefreshListener {
                viewModel.refreshStargazers()
                viewLifecycleOwner.lifecycleScope.launch {
                    delay(SWITCH_TO_HPB_DELAY)
                    isRefreshing = false
                    //We are switching to less intrusive horizontal progress bar
                    //if refreshing is not yet completed at this moment
                    if (viewModel.stargazersListScreenStateFlow.value.fetchStatus == FetchState.REFRESHING) {
                        binding.horizontalPb.isVisible = true
                    }
                }
            }
        }*/

        binding.retryBtn.apply {
            setOnClickListener {
                viewModel.refreshStargazers()
            }
        }
    }

    private fun observeUIState() {
        launchAndRepeatWithViewLifecycle {
            launch {
                viewModel.stargazersListScreenStateFlow
                    .collectLatest {
                        val itemsList = it.itemsList

                        updateLoadingStateViews(it.fetchStatus, itemsList.isEmpty())
                        stargazersAdapter.submitList(itemsList)

                        if (itemsList.isNotEmpty()) {
                            updateRecyclerViewVisibility(true, it.noItemText)
                            binding.indexscrollView.updateIndexer(itemsList)
                        } else {
                            updateRecyclerViewVisibility(false, it.noItemText)
                        }

                        stargazersAdapter.highlightWord = it.query
                    }
            }

            launch {
                viewModel.showFabStateFlow
                    .collectLatest { binding.fab.isVisible = it }
            }

            launch {
                viewModel.stargazersSettingsStateFlow
                    .collectLatest {
                        binding.indexscrollView.apply {
                            setAutoHide(it.autoHideIndexScroll)
                            setIndexBarTextMode(it.isTextModeIndexScroll)
                        }

                        val shouldAutoRefresh = it.lastRefresh != 0L &&
                                (System.currentTimeMillis() - it.lastRefresh) > 1000*60*60*15

                        if (shouldAutoRefresh){
                            //Just do it silently
                            viewModel.refreshStargazers(false)
                        }
                    }
            }

            launch {
                viewModel.userMessage
                    .collect{
                        if (it != null){
                            suggestiveSnackBar(it, duration = Snackbar.LENGTH_SHORT)
                            viewModel.setUserMessageShown()
                        }
                    }
            }

            launch {
                mainViewModel.isCtrlKeyPressed
                    .collect {
                        Log.d("XXX", "observeUIState: isCtrlKeyPressed $it")
                        binding.recyclerView.seslSetCtrlkeyPressed(it)
                        parentViewModel.isTabLayoutEnabled = !it && with(viewModel) {!isActionMode && !isSearchMode}
                    }
            }
        }
    }

    private fun updateLoadingStateViews(loadState: FetchState, isEmpty: Boolean) {
        if (lastStateReceived == loadState) return
        lastStateReceived = loadState

        when (loadState) {
            FetchState.NOT_INIT -> Unit
            FetchState.INITING -> {
                binding.loadingPb.isVisible = true
            }
            FetchState.INIT_ERROR,
            FetchState.REFRESH_ERROR -> {
                binding.loadingPb.isVisible = false
                binding.horizontalPb.isVisible = false
                binding.retryBtn.isVisible = isEmpty
            }
            FetchState.INITED,
            FetchState.REFRESHED -> {
                binding.loadingPb.isVisible = false
                binding.horizontalPb.isVisible = false
                binding.retryBtn.isVisible = false
            }
            FetchState.REFRESHING -> {
                // binding.loadingPb.isVisible = false
                // binding.horizontalPb.isVisible = true
                binding.retryBtn.isVisible = false

            }
        }
    }

    private fun updateRecyclerViewVisibility(visible: Boolean, noItemText: String) {
        binding.nsvNoItem.isVisible = !visible
        binding.recyclerView.isVisible = visible
        binding.indexscrollView.isVisible = visible
        binding.tvNoItem.text = noItemText
    }


    private fun StargazersAdapter.setupOnClickListeners() {
        onClickItem = { stargazer, position, vh ->
            if (isActionMode) {
               toggleItem(stargazer.toStableId(), position)
            } else {
                when (stargazer) {
                    is StargazerItem -> {
                        openProfileActivity(vh, stargazer)
                    }
                    else -> Unit
                }
            }
        }

        onLongClickItem = {
            if (!isActionMode) launchActionMode()
            binding.recyclerView.seslStartLongPressMultiSelection()
        }
    }

    private fun openProfileActivity(
        vh: StargazersAdapter.ViewHolder,
        stargazer: StargazerItem
    ) {
        requireActivity().startActivity(
            Intent(
                requireActivity(),
                StargazerProfileActivity::class.java
            ).apply {
                putExtra(KEY_STARGAZER, stargazer.stargazer)
            }
        )
    }

    private fun configureItemSwipeAnimator() {
        binding.recyclerView.configureItemSwipeAnimator(
            leftToRightLabel = "Call",
            rightToLeftLabel = "Message",
            leftToRightColor = "#11a85f".toColorInt(),
            rightToLeftColor = "#31a5f3".toColorInt(),
            leftToRightDrawableRes = iconsLibR.drawable.ic_oui_wifi_call,
            rightToLeftDrawableRes = iconsLibR.drawable.ic_oui_message,
            isLeftSwipeEnabled = { viewHolder ->
                viewHolder.itemViewType == StargazerItem.VIEW_TYPE
                        && !stargazersAdapter.isActionMode
            },
            isRightSwipeEnabled = { viewHolder ->
                viewHolder.itemViewType == StargazerItem.VIEW_TYPE
                        && !stargazersAdapter.isActionMode
            },
            onSwiped = { position, swipeDirection, _ ->
                val stargazer =
                    (stargazersAdapter.getItemByPosition(position) as StargazerItem).stargazer
                if (navDrawerLayout.layoutDirection == LayoutDirection.LTR){
                    if (swipeDirection == START ) showMessagingProgressDialog(stargazer.name ?: stargazer.login)
                    if (swipeDirection == END) showCallingProgressDialog(stargazer.name ?: stargazer.login)
                } else {
                    if (swipeDirection == END ) showMessagingProgressDialog(stargazer.name ?: stargazer.login)
                    if (swipeDirection == START) showCallingProgressDialog(stargazer.name ?: stargazer.login)
                }
                true
            }
        )
    }

    private fun showCallingProgressDialog(name: String){
        val dialog = ProgressDialog(requireContext()).apply {
            setMessage("Calling $name...")
            isIndeterminate = true
            setProgressStyle(ProgressStyle.HORIZONTAL)
            show()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            delay(4_000)
            dialog.dismiss()
        }
    }

    private fun showMessagingProgressDialog(name: String){
        val dialog = ProgressDialog(requireContext()).apply {
            setMessage("Sending message to $name...")
            isIndeterminate = false
            setProgressStyle(ProgressStyle.HORIZONTAL)
            show()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            dialog.apply {
                for (p in 0..100) { progress = p; delay(50) }
                dismiss()
            }
        }
    }


    private fun launchActionMode(selectedIds: Set<Long>? = null) {
        viewModel.isActionMode = true
        parentViewModel.isTabLayoutEnabled = false

        stargazersAdapter.toggleActionMode(true, selectedIds)

        val contactsSettings = viewModel.stargazersSettingsStateFlow.value
        navDrawerLayout.startActionMode(
            onInflateMenu = { menu, menuInflater ->
                menuInflater.inflate(R.menu.menu_stargazers_am, menu)
            },
            onEnd = {
                viewModel.isActionMode = false
                parentViewModel.isTabLayoutEnabled = !viewModel.isSearchMode
                stargazersAdapter.toggleActionMode(false)
            },
            onSelectMenuItem = {
                val selectedCount = stargazersAdapter.getSelectedIds().count()
                suggestiveSnackBar(
                    "$selectedCount contacts selected for ${it.title}",
                    duration = Snackbar.LENGTH_SHORT
                )
                this@StargazersFragment.navDrawerLayout.endActionMode()
                true
            },
            onSelectAll = { isChecked -> stargazersAdapter.onToggleSelectAll(isChecked) },
            allSelectorStateFlow = viewModel.allSelectorStateFlow,
            searchOnActionMode = contactsSettings.searchOnActionMode.toSearchOnActionMode(
                searchModeListener
            ),
            showCancel = contactsSettings.actionModeShowCancel
        )
    }

    private val menuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_stargazers_list, menu)
            menu.findItem(R.id.menu_sg_options).setBadge(Badge.DOT)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                R.id.menu_sg_options -> {
                    showOptionsDialog()
                    true
                }

                R.id.menu_sg_search -> {
                    navDrawerLayout.launchSearchMode(CLEAR_DISMISS)
                    true
                }

                else -> return false
            }
        }
    }

    private val searchModeListener by lazy {
        object : ToolbarLayout.SearchModeListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.setQuery(query)
                return !query.isNullOrEmpty()
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setQuery(newText)
                return true
            }

            override fun onSearchModeToggle(searchView: SearchView, isActive: Boolean) {
                if (isActive) {
                    searchView.queryHint = "Search contact"
                } else {
                    viewModel.setQuery("")
                }
                parentViewModel.isTabLayoutEnabled = !(isActive || viewModel.isActionMode)

                //Ignore if it's action mode search
                if (viewModel.isActionMode) return
                viewModel.isSearchMode = isActive
            }
        }
    }

    private fun ToolbarLayout.launchSearchMode(onBackBehavior: SearchModeOnBackBehavior) {
        startSearchMode(searchModeListener, onBackBehavior)
    }

    private fun showOptionsDialog() {
        val contactSettings = viewModel.stargazersSettingsStateFlow.value
        val dialogBinding = ViewStargazersOptionsDialogBinding.inflate(layoutInflater).apply {
            swShowLetters.isChecked = contactSettings.isTextModeIndexScroll
            swAutohide.isChecked = contactSettings.autoHideIndexScroll
            swShowCancel.isChecked = contactSettings.actionModeShowCancel
            amsOptionsRg.check(
                when (contactSettings.searchOnActionMode) {
                    ActionModeSearch.DISMISS -> R.id.amsDismiss
                    ActionModeSearch.NO_DISMISS -> R.id.amsNoDismiss
                    ActionModeSearch.CONCURRENT -> R.id.amsConcurrent
                }
            )
        }
        AlertDialog.Builder(requireContext()).apply {
            setTitle("Stargazers options")
            setView(dialogBinding.root)
            setNegativeButton("Cancel") { d, w -> d.dismiss() }
            setPositiveButton("Apply") { d, w ->
                viewModel.setTextMode(dialogBinding.swShowLetters.isChecked)
                viewModel.setAutoHide(dialogBinding.swAutohide.isChecked)
                viewModel.setShowCancel(dialogBinding.swShowCancel.isChecked)
                viewModel.setActionModeSearchMode(
                    when (dialogBinding.amsOptionsRg.checkedRadioButtonId){
                        R.id.amsNoDismiss -> ActionModeSearch.NO_DISMISS
                        R.id.amsConcurrent -> ActionModeSearch.CONCURRENT
                        else/*ActionModeSearch.DISMISS*/ -> ActionModeSearch.DISMISS
                    }
                )
            }
            show()
        }
    }

    companion object {
        const val SWITCH_TO_HPB_DELAY = 1_500L
    }
}
