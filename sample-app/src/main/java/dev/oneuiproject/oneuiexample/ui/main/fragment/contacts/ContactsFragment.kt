package dev.oneuiproject.oneuiexample.ui.main.fragment.contacts

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
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
import dev.oneuiproject.oneui.ktx.hideSoftInput
import dev.oneuiproject.oneui.ktx.hideSoftInputOnScroll
import dev.oneuiproject.oneui.layout.DrawerLayout
import dev.oneuiproject.oneui.layout.ToolbarLayout
import dev.oneuiproject.oneui.layout.ToolbarLayout.MainRoundedCorners
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
import dev.oneuiproject.oneuiexample.ui.main.MainActivity
import dev.oneuiproject.oneuiexample.ui.main.core.base.BaseFragment
import dev.oneuiproject.oneuiexample.ui.main.core.util.launchAndRepeatWithViewLifecycle
import dev.oneuiproject.oneuiexample.ui.main.core.util.suggestiveSnackBar
import dev.oneuiproject.oneuiexample.ui.main.core.util.toast
import dev.oneuiproject.oneuiexample.ui.main.fragment.contacts.adapter.ContactsAdapter
import dev.oneuiproject.oneuiexample.ui.main.fragment.contacts.model.ContactsListItemUiModel
import dev.oneuiproject.oneuiexample.ui.main.fragment.contacts.util.toSearchOnActionMode
import dev.oneuiproject.oneuiexample.ui.main.fragment.contacts.util.updateIndexer
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
    private var tipPopup: TipPopup? = null
    private var tipPopupShown = false
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) =  super.onCreateView(inflater, container, savedInstanceState).also {
        //Cast to directly to MainActivity since we are not reusing
        //this fragment elsewhere
        drawerLayout = (requireActivity() as MainActivity).drawerLayout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        configureRecyclerView()
        configureSwipeRefresh()
        configureItemSwipeAnimator()
        observeUIState()
        if (!isHidden) {
            requireActivity().addMenuProvider(
                menuProvider,
                viewLifecycleOwner,
                Lifecycle.State.STARTED
            )
        }

        if (contactsViewModel.isSearchMode){
            drawerLayout.launchSearchMode(CLEAR_DISMISS)
        }

        if (contactsViewModel.isActionMode){
            launchActionMode()
        }

        if (savedInstanceState == null){
            showTipPopup()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        contactsAdapter.getSelectedIds().let {
            if (it.isNotEmpty()) {
                contactsViewModel.initialSelectedIds = it.asSet().toTypedArray()
            }
        }
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
            setLayoutManager(LinearLayoutManager(requireContext()))
            setAdapter(ContactsAdapter(requireContext()).also {
                it.setupOnClickListeners()
                contactsAdapter = it
            })
            addItemDecoration(
                SemItemDecoration(
                    requireContext(),
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
            hideSoftInputOnScroll()
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

        tvNoItem.translateYWithAppBar(drawerLayout.appBarLayout, this)

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
                contactsViewModel.showFabStateFlow
                    .collectLatest { fab.isVisible = it }
            }

            launch {
                contactsViewModel.isActionModeStateFlow
                    .collectLatest {
                        if (it){
                            contactsAdapter.onToggleActionMode(true, contactsViewModel.initialSelectedIds)
                            contactsViewModel.initialSelectedIds = null //not anymore needed
                        }else{
                            contactsAdapter.onToggleActionMode(false)
                        }
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
                                "Hide index letters" } else "Show index letters"
                            keepSearchMenuItemTitle = when (it.searchOnActionMode) {
                                ActionModeSearch.DISMISS -> "ActionModeSearch.DISMISS"
                                ActionModeSearch.NO_DISMISS -> "ActionModeSearch.NO_DISMISS"
                                ActionModeSearch.CONCURRENT -> "ActionModeSearch.CONCURRENT"
                            }
                            showCancelMenuItemTitle = if (it.actionModeShowCancel){
                                "No cancel button" } else "Show cancel button"
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
                requireActivity().hideSoftInput()

                when(contact){
                    is ContactsListItemUiModel.ContactItem -> {
                        suggestiveSnackBar("${contact.contact.name} clicked!").apply {
                            setAction("Dismiss") { dismiss() }
                        }
                    }
                    is ContactsListItemUiModel.GroupItem -> {
                        suggestiveSnackBar("${contact.groupName} clicked!").apply {
                            setAction("Dismiss") { dismiss() }
                        }
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


    private fun launchActionMode() {
        val contactsSettings = contactsViewModel.contactsSettingsStateFlow.value
        contactsViewModel.isActionMode = true

        drawerLayout.startActionMode(
            onInflateMenu = {menu, menuInflater ->
                menuInflater.inflate( R.menu.menu_contacts_am, menu)
            },
            onEnd = { contactsViewModel.isActionMode = false },
            onSelectMenuItem = {
                requireActivity().toast(it.title.toString())
                drawerLayout.endActionMode()
                true
            },
            onSelectAll = { isChecked -> contactsAdapter.onToggleSelectAll(isChecked) },
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

                R.id.menu_contacts_search -> {
                    drawerLayout.launchSearchMode(CLEAR_DISMISS)
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

            override fun onSearchModeToggle(searchView: SearchView, isActive: Boolean) {
                if (isActive) {
                    searchView.queryHint = "Search contact"
                }else{
                    contactsViewModel.setQuery("")
                }
                //Ignore if it's action mode search
                if (contactsViewModel.isActionMode) return
                contactsViewModel.isSearchMode = isActive
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
                    if (anchor != null && tipPopup?.isShowing != true) {
                        tipPopup = TipPopup(anchor, Mode.TRANSLUCENT).apply {
                            setMessage("Long-press item to trigger multi-selection.\nSwipe left to call, swipe right to message.")
                            setAction("Close") { tipPopupShown = true }
                            setExpanded(false)
                            show(Direction.DEFAULT)
                        }
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

    override fun getLayoutResId() = R.layout.fragment_contacts

    override fun getIconResId() = dev.oneuiproject.oneui.R.drawable.ic_oui_contact_outline

    override fun getTitle() = "Contacts"

    override fun getSubtitle() = "Pull down to refresh"

    override fun roundedCorners() = MainRoundedCorners.BOTTOM

}
