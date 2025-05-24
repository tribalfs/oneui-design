package dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.contacts

import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.graphics.toColorInt
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.indexscroll.widget.SeslIndexScrollView.OnIndexBarEventListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper.END
import androidx.recyclerview.widget.ItemTouchHelper.START
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.sec.sesl.tester.R
import com.sec.sesl.tester.databinding.FragmentContactsBinding
import com.sec.sesl.tester.databinding.ViewContactsOptionsDialogBinding
import dagger.hilt.android.AndroidEntryPoint
import dev.oneuiproject.oneui.delegates.AppBarAwareYTranslator
import dev.oneuiproject.oneui.delegates.ViewYTranslator
import dev.oneuiproject.oneui.ktx.clearBadge
import dev.oneuiproject.oneui.ktx.configureItemSwipeAnimator
import dev.oneuiproject.oneui.ktx.dpToPx
import dev.oneuiproject.oneui.ktx.enableCoreSeslFeatures
import dev.oneuiproject.oneui.ktx.hideSoftInput
import dev.oneuiproject.oneui.ktx.hideSoftInputOnScroll
import dev.oneuiproject.oneui.ktx.setBadge
import dev.oneuiproject.oneui.layout.Badge
import dev.oneuiproject.oneui.layout.ToolbarLayout
import dev.oneuiproject.oneui.layout.ToolbarLayout.SearchModeOnBackBehavior
import dev.oneuiproject.oneui.layout.ToolbarLayout.SearchModeOnBackBehavior.CLEAR_DISMISS
import dev.oneuiproject.oneui.layout.startActionMode
import dev.oneuiproject.oneui.utils.ItemDecorRule
import dev.oneuiproject.oneui.utils.SemItemDecoration
import dev.oneuiproject.oneui.widget.TipPopup
import dev.oneuiproject.oneuiexample.data.ActionModeSearch
import dev.oneuiproject.oneuiexample.ui.main.MainActivity
import dev.oneuiproject.oneuiexample.ui.main.core.base.AbsBaseFragment
import dev.oneuiproject.oneuiexample.ui.main.core.util.autoCleared
import dev.oneuiproject.oneuiexample.ui.main.core.util.launchAndRepeatWithViewLifecycle
import dev.oneuiproject.oneuiexample.ui.main.core.util.semToast
import dev.oneuiproject.oneuiexample.ui.main.core.util.showTipPopup
import dev.oneuiproject.oneuiexample.ui.main.core.util.suggestiveSnackBar
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.RvParentViewModel
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.contacts.adapter.ContactsAdapter
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.contacts.model.ContactsListItemUiModel
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.contacts.util.toSearchOnActionMode
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.contacts.util.updateIndexer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import dev.oneuiproject.oneui.R as iconsLibR

@AndroidEntryPoint
class ContactsFragment : AbsBaseFragment(R.layout.fragment_contacts),
    ViewYTranslator by AppBarAwareYTranslator() {

    private val binding by autoCleared { FragmentContactsBinding.bind(requireView()) }
    private val viewModel: ContactsViewModel by viewModels()
    private val parentViewModel by viewModels<RvParentViewModel>({ requireParentFragment() })
    private lateinit var toolbarLayout: ToolbarLayout

    @Inject
    lateinit var contactsAdapter: ContactsAdapter
    private var rvTipView: TipPopup? = null
    private var fabTipPopup: TipPopup? = null
    private val indexBarEventListener = object:OnIndexBarEventListener{
        override fun onIndexChanged(sectionIndex: Int) {}
        override fun onPressed(v: Float) {
            parentViewModel.isTabLayoutEnabled = false
        }
        override fun onReleased(v: Float) {
            parentViewModel.isTabLayoutEnabled = !(viewModel.isActionMode || viewModel.isSearchMode)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as MainActivity).let {
            toolbarLayout = it.drawerLayout
            it.addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)
        }

        configureRecyclerView()
        configureSwipeRefresh()
        configureItemSwipeAnimator()
        observeUIState()

        if (viewModel.isSearchMode) toolbarLayout.launchSearchMode(CLEAR_DISMISS)
        if (viewModel.isActionMode) {
            launchActionMode(viewModel.initialSelectedIds)
            viewModel.initialSelectedIds = null //not anymore needed
        }

        showTipPopup(
            message = "Swipe left to call, swipe right to message.",
            delayMillis = 1_000,
            getAnchor = { binding.contactsList.layoutManager!!.findViewByPosition(2) },
            onCreate = {
                rvTipView = this
                setAction("Close") { }
            }
        )

        binding.fab.setOnClickListener {
            showTipPopup(
                "I am FAB!",
                getAnchor = { it },
                expanded = true,
                mode = TipPopup.Mode.NORMAL
            ) {
                fabTipPopup = this
                setBackgroundColorWithAlpha("#9AB65205".toColorInt())
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        rvTipView?.dismiss(false)
        fabTipPopup?.dismiss(false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        contactsAdapter.getSelectedIds().let {
            if (it.isNotEmpty()) {
                viewModel.initialSelectedIds = it.asSet().toTypedArray()
            }
        }
        binding.indexscrollView.removeOnIndexEventListener(indexBarEventListener)
    }

    private fun configureRecyclerView() {
        binding.contactsList.apply {
            setLayoutManager(LinearLayoutManager(requireContext()))
            setAdapter(contactsAdapter.apply { setupOnClickListeners() })
            addItemDecoration(
                SemItemDecoration(
                    requireContext(),
                    dividerRule = ItemDecorRule.SELECTED {
                        it.itemViewType == ContactsListItemUiModel.ContactItem.VIEW_TYPE
                    },
                    subHeaderRule = ItemDecorRule.SELECTED {
                        it.itemViewType == ContactsListItemUiModel.SeparatorItem.VIEW_TYPE
                    }
                ).apply {
                    setDividerInsetStart(76.dpToPx(resources))
                }
            )
            setItemAnimator(null)
            enableCoreSeslFeatures(fastScrollerEnabled = false)
            hideSoftInputOnScroll()
        }

        contactsAdapter.configure(
            binding.contactsList,
            ContactsAdapter.Payload.SELECTION_MODE,
            onAllSelectorStateChanged = { viewModel.allSelectorStateFlow.value = it }
        )

        binding.fab.hideOnScroll(binding.contactsList, binding.indexscrollView /*optional*/)

        binding.indexscrollView.apply {
            setIndexScrollMargin(0, 78.dpToPx(resources))
            attachToRecyclerView(binding.contactsList)
            addOnIndexEventListener(indexBarEventListener)
        }

        binding.tvNoItem.translateYWithAppBar(
            this@ContactsFragment.toolbarLayout.appBarLayout,
            this
        )

    }

    private fun configureSwipeRefresh() {
        binding.swiperefreshView.apply {
            seslSetRefreshOnce(true)
            setProgressViewOffset(true, 130, 131)
            setOnRefreshListener {
                viewLifecycleOwner.lifecycleScope.launch {
                    delay(1_200)
                    isRefreshing = false
                    binding.horizontalPb.isVisible = true
                    delay(10_000)
                    binding.horizontalPb.isVisible = false
                }
            }
        }
    }


    private fun observeUIState() {
        launchAndRepeatWithViewLifecycle {
            launch {
                viewModel.contactsListStateFlow
                    .collectLatest {
                        val itemsList = it.itemsList
                        if (itemsList.isNotEmpty()) {
                            updateRecyclerViewVisibility(true, it.noItemText)
                            binding.indexscrollView.updateIndexer(itemsList)
                        } else {
                            updateRecyclerViewVisibility(false, it.noItemText)
                        }
                        contactsAdapter.submitList(itemsList)
                        contactsAdapter.highlightWord = it.query
                    }
            }

            launch {
                viewModel.showFabStateFlow
                    .collectLatest { binding.fab.isVisible = it }
            }

            launch {
                viewModel.contactsSettingsStateFlow
                    .collectLatest {
                        binding.indexscrollView.apply {
                            setIndexBarTextMode(it.isTextModeIndexScroll)
                            setAutoHide(it.autoHideIndexScroll)
                        }
                    }
            }
        }
    }


    private fun updateRecyclerViewVisibility(visible: Boolean, noItemText: String) {
        binding.tvNoItem.text = noItemText
        binding.nsvNoItem.isVisible = !visible
        binding.contactsList.isVisible = visible
        binding.indexscrollView.isVisible = visible
    }


    private fun ContactsAdapter.setupOnClickListeners() {
        onClickItem = { contact, position ->
            if (isActionMode) {
                onToggleItem(contact.toStableId(), position)
            } else {
                requireActivity().hideSoftInput()

                when (contact) {
                    is ContactsListItemUiModel.ContactItem -> {
                        suggestiveSnackBar(
                            "${contact.contact.name} clicked!",
                            duration = Snackbar.LENGTH_LONG
                        ).apply {
                            setAction("Dismiss") { dismiss() }
                        }
                    }

                    is ContactsListItemUiModel.GroupItem -> {
                        suggestiveSnackBar(
                            "${contact.groupName} clicked!",
                            duration = Snackbar.LENGTH_LONG
                        ).apply {
                            setAction("Dismiss") { dismiss() }
                        }
                    }

                    else -> Unit
                }
            }
        }

        onLongClickItem = {
            if (!isActionMode) launchActionMode()
            binding.contactsList.seslStartLongPressMultiSelection()
        }
    }

    private fun configureItemSwipeAnimator() {
        binding.contactsList.configureItemSwipeAnimator(
            leftToRightLabel = "Call",
            rightToLeftLabel = "Message",
            leftToRightColor = "#11a85f".toColorInt(),
            rightToLeftColor = "#31a5f3".toColorInt(),
            leftToRightDrawableRes = iconsLibR.drawable.ic_oui_wifi_call,
            rightToLeftDrawableRes = iconsLibR.drawable.ic_oui_message,
            isLeftSwipeEnabled = { viewHolder ->
                viewHolder.itemViewType == ContactsListItemUiModel.ContactItem.VIEW_TYPE
                        && !contactsAdapter.isActionMode
            },
            isRightSwipeEnabled = { viewHolder ->
                viewHolder.itemViewType == ContactsListItemUiModel.ContactItem.VIEW_TYPE
                        && !contactsAdapter.isActionMode
            },
            onSwiped = { position, swipeDirection, _ ->
                val contact =
                    (contactsAdapter.getItemByPosition(position) as ContactsListItemUiModel.ContactItem).contact
                if (swipeDirection == START) {
                    semToast("Messaging ${(contact.name)}... ")
                }
                if (swipeDirection == END) {
                    semToast("Calling ${(contact.name)}... ")
                }
                true
            }
        )
    }


    private fun launchActionMode(selectedIds: Array<Long>? = null) {
        viewModel.isActionMode = true
        parentViewModel.isTabLayoutEnabled = false

        contactsAdapter.onToggleActionMode(true, selectedIds)

        val contactsSettings = viewModel.contactsSettingsStateFlow.value
        toolbarLayout.startActionMode(
            onInflateMenu = { menu, menuInflater ->
                menuInflater.inflate(R.menu.menu_contacts_am, menu)
            },
            onEnd = {
                viewModel.isActionMode = false
                parentViewModel.isTabLayoutEnabled = !viewModel.isSearchMode
                contactsAdapter.onToggleActionMode(false)
            },
            onSelectMenuItem = {
                val selectedCount = contactsAdapter.getSelectedIds().count()
                suggestiveSnackBar(
                    "$selectedCount contacts selected for ${it.title}",
                    duration = Snackbar.LENGTH_SHORT
                )
                this@ContactsFragment.toolbarLayout.endActionMode()
                true
            },
            onSelectAll = { isChecked -> contactsAdapter.onToggleSelectAll(isChecked) },
            allSelectorStateFlow = viewModel.allSelectorStateFlow,
            searchOnActionMode = contactsSettings.searchOnActionMode.toSearchOnActionMode(
                searchModeListener
            ),
            showCancel = contactsSettings.actionModeShowCancel
        )
    }

    private val menuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_contacts_list, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                R.id.menu_contacts_options -> {
                    showOptionsDialog()
                    true
                }

                R.id.menu_contacts_search -> {
                    toolbarLayout.launchSearchMode(CLEAR_DISMISS)
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
        val contactSettings = viewModel.contactsSettingsStateFlow.value
        val dialogBinding = ViewContactsOptionsDialogBinding.inflate(layoutInflater).apply {
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
            setTitle("Contacts options")
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

}
