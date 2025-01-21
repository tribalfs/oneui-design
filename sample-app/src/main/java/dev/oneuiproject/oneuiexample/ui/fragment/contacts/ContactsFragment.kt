package dev.oneuiproject.oneuiexample.ui.fragment.contacts

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SeslProgressBar
import androidx.core.view.MenuProvider
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper.END
import androidx.recyclerview.widget.ItemTouchHelper.START
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.sec.sesl.tester.R
import dev.oneuiproject.oneui.delegates.AppBarAwareYTranslator
import dev.oneuiproject.oneui.delegates.ViewYTranslator
import dev.oneuiproject.oneui.ktx.clearBadge
import dev.oneuiproject.oneui.ktx.configureItemSwipeAnimator
import dev.oneuiproject.oneui.ktx.dpToPx
import dev.oneuiproject.oneui.ktx.enableCoreSeslFeatures
import dev.oneuiproject.oneui.ktx.setBadge
import dev.oneuiproject.oneui.layout.Badge
import dev.oneuiproject.oneui.layout.ToolbarLayout
import dev.oneuiproject.oneui.layout.ToolbarLayout.SearchModeOnBackBehavior
import dev.oneuiproject.oneui.layout.ToolbarLayout.SearchModeOnBackBehavior.CLEAR_DISMISS
import dev.oneuiproject.oneui.layout.startActionMode
import dev.oneuiproject.oneui.utils.ItemDecorRule
import dev.oneuiproject.oneui.utils.SemItemDecoration
import dev.oneuiproject.oneui.widget.AutoHideIndexScrollView
import dev.oneuiproject.oneui.widget.ScrollAwareFloatingActionButton
import dev.oneuiproject.oneui.widget.TipPopup
import dev.oneuiproject.oneui.widget.TipPopup.Direction
import dev.oneuiproject.oneui.widget.TipPopup.Mode
import dev.oneuiproject.oneuiexample.data.ActionModeSearch
import dev.oneuiproject.oneuiexample.data.ContactsRepo
import dev.oneuiproject.oneuiexample.ui.activity.AboutActivity
import dev.oneuiproject.oneuiexample.ui.activity.MainActivity
import dev.oneuiproject.oneuiexample.ui.core.base.BaseFragment
import dev.oneuiproject.oneuiexample.ui.core.ktx.launchAndRepeatWithViewLifecycle
import dev.oneuiproject.oneuiexample.ui.core.ktx.toast
import dev.oneuiproject.oneuiexample.ui.fragment.contacts.adapter.ContactsAdapter
import dev.oneuiproject.oneuiexample.ui.fragment.contacts.model.ContactsListItemUiModel
import dev.oneuiproject.oneuiexample.ui.fragment.contacts.util.toSearchOnActionMode
import dev.oneuiproject.oneuiexample.ui.fragment.contacts.util.updateIndexer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ContactsFragment : BaseFragment(), ViewYTranslator by AppBarAwareYTranslator() {
    private lateinit var mIndexScrollView: AutoHideIndexScrollView
    private lateinit var mContactsListRv: RecyclerView
    private lateinit var contactsAdapter: ContactsAdapter
    private lateinit var contactsViewModel: ContactsViewModel
    private lateinit var nsvNoItem: NestedScrollView
    private lateinit var tvNoItem: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var horizontalProgress: SeslProgressBar
    private lateinit var fab: ScrollAwareFloatingActionButton
    private var tipPopupShown = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        configureRecyclerView()
        configureSwipeRefresh()
        configureItemSwipeAnimator()
        observeUIState()
        // showTipPopup()
        if (!isHidden) {
            requireActivity().addMenuProvider(
                menuProvider,
                viewLifecycleOwner,
                Lifecycle.State.STARTED
            )
        }

        savedInstanceState?.apply {
            if (getBoolean(KEY_IS_ACTION_MODE)) {
                val selectedIds = savedInstanceState.getLongArray(KEY_ACTION_MODE_SELECTED_IDS)!!
                launchActionMode(selectedIds.toTypedArray())
            }
            if (getBoolean(KEY_IS_SEARCH_MODE)){
                (requireActivity() as MainActivity).drawerLayout
                    .launchSearchMode(CLEAR_DISMISS)
            }
        }
    }


    override fun onSaveInstanceState(outState: Bundle) {
        (requireActivity() as MainActivity).drawerLayout?.apply {
            if (isActionMode) {
                outState.putBoolean(KEY_IS_ACTION_MODE, true)
                outState.putLongArray(KEY_ACTION_MODE_SELECTED_IDS,
                    contactsAdapter.getSelectedIds().asSet().toLongArray())
            }
            if (isSearchMode) {
                outState.putBoolean(KEY_IS_SEARCH_MODE, true)
            }
        }
        super.onSaveInstanceState(outState)
    }

    private fun initViews(view: View){
        mIndexScrollView = view.findViewById(R.id.indexscroll_view)
        mContactsListRv = view.findViewById(R.id.contacts_list)
        nsvNoItem = view.findViewById(R.id.nsvNoItem)
        tvNoItem = view.findViewById(R.id.tvNoItem)
        swipeRefreshLayout = view.findViewById(R.id.swiperefresh_view)
        horizontalProgress = view.findViewById(R.id.horizontal_pb)
        fab = view.findViewById(R.id.fab)
    }


    private fun configureRecyclerView() {
        mContactsListRv.apply {
            setLayoutManager(LinearLayoutManager(mContext))
            setAdapter(ContactsAdapter(mContext).also {
                it.setupOnClickListeners()
                contactsAdapter = it
            })
            addItemDecoration(
                SemItemDecoration(
                    mContext,
                    dividerRule = ItemDecorRule.SELECTED{
                        it.itemViewType == ContactsListItemUiModel.ContactItem.VIEW_TYPE
                    },
                    subHeaderRule = ItemDecorRule.SELECTED{
                        it.itemViewType == ContactsListItemUiModel.SeparatorItem.VIEW_TYPE
                    }
                ).apply {
                    setDividerInsetStart(76.dpToPx(resources))
                }
            )
            setItemAnimator(null)
            enableCoreSeslFeatures(fastScrollerEnabled = false)
        }

        contactsAdapter.configure(
            mContactsListRv,
            ContactsAdapter.Payload.SELECTION_MODE,
            onAllSelectorStateChanged = { contactsViewModel.allSelectorStateFlow.value = it }
        )

        fab.hideOnScroll(mContactsListRv, mIndexScrollView /*optional*/)

        mIndexScrollView.apply {
            setIndexScrollMargin(0, 78.dpToPx(resources))
            attachToRecyclerView(mContactsListRv)
        }

        tvNoItem.translateYWithAppBar((requireActivity() as MainActivity).drawerLayout.appBarLayout, this)
    }

    private fun configureSwipeRefresh() {
        swipeRefreshLayout.apply {
            seslSetRefreshOnce(true)
            setProgressViewOffset(true, 130, 131)
            setOnRefreshListener {
                viewLifecycleOwner.lifecycleScope.launch {
                    delay(1_200)
                    isRefreshing = false
                    horizontalProgress.isVisible = true
                    delay(10_000)
                    horizontalProgress.isVisible = false
                }
            }
        }
    }


    private fun observeUIState(){
        val contactsRepo = ContactsRepo(requireContext())
        val viewModelFactory = ContactsViewModelFactory(contactsRepo)
        contactsViewModel = ViewModelProvider(this, viewModelFactory)[ContactsViewModel::class.java]

        launchAndRepeatWithViewLifecycle{
            launch {
                contactsViewModel.contactsListStateFlow
                    .collectLatest {
                        val itemsList = it.itemsList
                        if (itemsList.isNotEmpty()) {
                            updateRecyclerViewVisibility(true, it.noItemText)
                            mIndexScrollView.updateIndexer(itemsList)
                        }else{
                            updateRecyclerViewVisibility(false, it.noItemText)
                        }
                        contactsAdapter.submitList(itemsList)
                        contactsAdapter.highlightWord = it.query
                    }
            }

            launch {
                contactsViewModel.contactsSettingsStateFlow
                    .collectLatest {
                        mIndexScrollView.apply {
                            setIndexBarTextMode(it.isTextModeIndexScroll)
                            setAutoHide(it.autoHideIndexScroll)
                        }
                        menuProvider.apply {
                            autoHideMenuItemTitle = if (it.autoHideIndexScroll) {
                                "Always show indexscroll" } else "Autohide indexscroll"
                            showLettersMenuItemTitle = if (it.isTextModeIndexScroll) {
                                "Hide indexscroll letters" } else "Show indexscroll letters"
                            keepSearchMenuItemTitle = when (it.searchOnActionMode) {
                                ActionModeSearch.DISMISS -> "Resume search mode on action mode end"
                                ActionModeSearch.NO_DISMISS -> "Allow search mode on action mode"
                                ActionModeSearch.CONCURRENT -> "Dismiss search on action mode"
                            }
                            showCancelMenuItemTitle = if (it.actionModeShowCancel){
                                "No cancel button on action mode" } else "Show cancel button on action mode"
                        }
                    }
            }
        }
    }


    private fun updateRecyclerViewVisibility(visible: Boolean, noItemText: String){
        if (visible){
            nsvNoItem.isVisible = false
            mContactsListRv.isVisible = true
            mIndexScrollView.isVisible = true
        }else{
            tvNoItem.text = noItemText
            nsvNoItem.isVisible = true
            mContactsListRv.isVisible = false
            mIndexScrollView.isVisible = false
        }
    }


    private fun ContactsAdapter.setupOnClickListeners(){
        onClickItem = { contact, position ->
            if (isActionMode) {
                onToggleItem(contact.toStableId(), position)
            }else {
                when(contact){
                    is ContactsListItemUiModel.ContactItem -> {
                        toast("${contact.contact.name} clicked!")
                    }
                    is ContactsListItemUiModel.GroupItem -> {
                        toast("${contact.groupName} clicked!")
                    }
                    else -> Unit
                }
            }
        }

        onLongClickItem = {
            if (!isActionMode) launchActionMode()
            mContactsListRv.seslStartLongPressMultiSelection()
        }
    }

    private fun configureItemSwipeAnimator() {
        mContactsListRv.configureItemSwipeAnimator(
            leftToRightLabel = "Call",
            rightToLeftLabel = "Message",
            leftToRightColor = Color.parseColor("#11a85f"),
            rightToLeftColor = Color.parseColor("#31a5f3"),
            leftToRightDrawableRes = dev.oneuiproject.oneui.R.drawable.ic_oui_wifi_call,
            rightToLeftDrawableRes = dev.oneuiproject.oneui.R.drawable.ic_oui_message,
            isLeftSwipeEnabled = {viewHolder ->
                viewHolder.itemViewType == ContactsListItemUiModel.ContactItem.VIEW_TYPE
                        && !contactsAdapter.isActionMode
            },
            isRightSwipeEnabled = {viewHolder ->
                viewHolder.itemViewType == ContactsListItemUiModel.ContactItem.VIEW_TYPE
                        && !contactsAdapter.isActionMode
            },
            onSwiped = { position, swipeDirection, _ ->
                val contact = (contactsAdapter.getItemByPosition(position) as ContactsListItemUiModel.ContactItem).contact
                if (swipeDirection == START) {
                    toast("Messaging ${(contact.name)}... ")
                }
                if (swipeDirection == END) {
                    toast("Calling ${(contact.name)}... ")
                }
                true
            }
        )
    }


    private fun launchActionMode(initialSelected: Array<Long>? = null) {
        val drawerLayout = (requireActivity() as MainActivity).drawerLayout
        fab.isVisible = false

        val contactsSettings = contactsViewModel.contactsSettingsStateFlow.value


        drawerLayout.startActionMode(
            onInflateMenu = {menu, menuInflater ->
                contactsAdapter.onToggleActionMode(true, initialSelected)
                menuInflater.inflate( R.menu.menu_contacts_am, menu)
            },
            onEnd = {
                contactsAdapter.onToggleActionMode(false)
                fab.isVisible =! (requireActivity() as MainActivity).drawerLayout.isSearchMode
            },
            onSelectMenuItem = {
                requireActivity().toast(it.title.toString())
                drawerLayout.endActionMode()
                true
            },
            onSelectAll = { isChecked: Boolean -> contactsAdapter.onToggleSelectAll(isChecked) },
            allSelectorStateFlow = contactsViewModel.allSelectorStateFlow,
            searchOnActionMode = contactsSettings.searchOnActionMode.toSearchOnActionMode(searchModeListener),
            showCancel = contactsSettings.actionModeShowCancel
        )
    }

    private val menuProvider = object : MenuProvider {
        private lateinit var showLettersMenuItem: MenuItem
        private lateinit var autoHideMenuItem: MenuItem
        private lateinit var keepSearchMenuItem: MenuItem
        private lateinit var showCancelMenuItem: MenuItem
        var showLettersMenuItemTitle: String = ""
        var autoHideMenuItemTitle: String = ""
        var keepSearchMenuItemTitle: String = ""
        var showCancelMenuItemTitle: String = ""

        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_contacts_list, menu)
            showLettersMenuItem = menu.findItem(R.id.menu_contacts_indexscroll_show_letters)
            autoHideMenuItem = menu.findItem(R.id.menu_contacts_indexscroll_autohide)
            keepSearchMenuItem = menu.findItem(R.id.menu_contacts_keep_search)
            showCancelMenuItem = menu.findItem(R.id.menu_contacts_show_cancel)

            val menuItem = menu.findItem(R.id.menu_about_app)
            menuItem.setBadge(Badge.DOT)
        }

        override fun onPrepareMenu(menu: Menu) {
            showLettersMenuItem.title = showLettersMenuItemTitle
            autoHideMenuItem.title = autoHideMenuItemTitle
            keepSearchMenuItem.title = keepSearchMenuItemTitle
            showCancelMenuItem.title = showCancelMenuItemTitle
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                R.id.menu_contacts_indexscroll_show_letters -> {
                    contactsViewModel.toggleTextMode()
                    menuItem.clearBadge()
                    true
                }

                R.id.menu_contacts_indexscroll_autohide -> {
                    contactsViewModel.toggleAutoHide()
                    true
                }

                R.id.menu_contacts_keep_search -> {
                    contactsViewModel.toggleKeepSearch()
                    true
                }

                R.id.menu_contacts_show_cancel -> {
                    contactsViewModel.toggleShowCancel()
                    true
                }

                R.id.menu_about_app -> {
                    startActivity(Intent(requireContext(), AboutActivity::class.java))
                    menuItem.clearBadge()
                    true
                }

                R.id.menu_contacts_search -> {
                    (requireActivity() as MainActivity).drawerLayout
                        .launchSearchMode(CLEAR_DISMISS)
                    true
                }

                else -> return false
            }
        }
    }

    private val searchModeListener by lazy{
        object : ToolbarLayout.SearchModeListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                contactsViewModel.setQuery(query)
                return !query.isNullOrEmpty()
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                contactsViewModel.setQuery(newText)
                return true
            }

            override fun onSearchModeToggle(searchView: SearchView, visible: Boolean) {
                if (visible) {
                    searchView.queryHint = "Search contact"
                } else {
                    contactsViewModel.setQuery("")
                }
                fab.isVisible = !visible && !(requireActivity() as MainActivity).drawerLayout.isActionMode
            }
        }
    }

    private fun ToolbarLayout.launchSearchMode(onBackBehavior: SearchModeOnBackBehavior) {
        startSearchMode(searchModeListener, onBackBehavior)
    }

    private fun showTipPopup() {
        if (!tipPopupShown) {
            mContactsListRv.doOnLayout {
                mContactsListRv.postDelayed({
                    val anchor = mContactsListRv.layoutManager!!.findViewByPosition(2)
                    if (anchor != null) {
                        val tipPopup = TipPopup(anchor, Mode.TRANSLUCENT)
                        tipPopup.setMessage("Long-press item to trigger multi-selection.\nSwipe left to call, swipe right to message.")
                        tipPopup.setAction(
                            "Close"
                        ) { tipPopupShown = true }
                        tipPopup.setExpanded(true)
                        tipPopup.show(Direction.DEFAULT)
                    }
                }, 500)
            }
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) {
            requireActivity().addMenuProvider(
                menuProvider,
                viewLifecycleOwner,
                Lifecycle.State.STARTED
            )
        } else {
            requireActivity().removeMenuProvider(menuProvider)
        }
    }

    override fun getLayoutResId(): Int = R.layout.fragment_contacts

    override fun getIconResId(): Int = dev.oneuiproject.oneui.R.drawable.ic_oui_contact_outline

    override fun getTitle(): CharSequence = "Contacts"

    override fun getSubtitle(): CharSequence = "Pull down to refresh"

    override fun showBottomTab(): Boolean {
        return false
    }

    override fun isImmersiveScroll(): Boolean {
        return false
    }

    override fun isAppBarEnabled(): Boolean {
        return true
    }

    companion object {
        private const val KEY_IS_ACTION_MODE = "isActionMode"
        private const val KEY_ACTION_MODE_SELECTED_IDS = "selectedIds"
        private const val KEY_IS_SEARCH_MODE = "isSearchMode"
    }
}
