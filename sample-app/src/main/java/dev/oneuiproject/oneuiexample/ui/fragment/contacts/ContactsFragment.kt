package dev.oneuiproject.oneuiexample.ui.fragment.contacts

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sec.sesl.tester.R
import dev.oneuiproject.oneui.delegates.AppBarAwareYTranslator
import dev.oneuiproject.oneui.delegates.ViewYTranslator
import dev.oneuiproject.oneui.ktx.clearBadge
import dev.oneuiproject.oneui.ktx.enableCoreSeslFeatures
import dev.oneuiproject.oneui.ktx.setBadge
import dev.oneuiproject.oneui.layout.Badge
import dev.oneuiproject.oneui.layout.ToolbarLayout
import dev.oneuiproject.oneui.layout.ToolbarLayout.SearchModeOnBackBehavior
import dev.oneuiproject.oneui.layout.ToolbarLayout.SearchModeOnBackBehavior.CLEAR_DISMISS
import dev.oneuiproject.oneui.layout.startActionMode
import dev.oneuiproject.oneui.layout.startSearchMode
import dev.oneuiproject.oneui.widget.AutoHideIndexScrollView
import dev.oneuiproject.oneuiexample.data.ContactsRepo
import dev.oneuiproject.oneuiexample.ui.activity.AboutActivity
import dev.oneuiproject.oneuiexample.ui.activity.MainActivity
import dev.oneuiproject.oneuiexample.ui.core.base.BaseFragment
import dev.oneuiproject.oneuiexample.ui.core.ktx.launchAndRepeatWithViewLifecycle
import dev.oneuiproject.oneuiexample.ui.core.toast
import dev.oneuiproject.oneuiexample.ui.fragment.contacts.adapter.ContactsAdapter
import dev.oneuiproject.oneuiexample.ui.fragment.contacts.model.ContactsListItemUiModel
import dev.oneuiproject.oneuiexample.ui.fragment.contacts.util.ContactsListItemDecoration
import dev.oneuiproject.oneuiexample.ui.fragment.contacts.util.updateIndexer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ContactsFragment : BaseFragment(), ViewYTranslator by AppBarAwareYTranslator() {
    private lateinit var mIndexScrollView: AutoHideIndexScrollView
    private lateinit var mContactsListRv: RecyclerView
    private lateinit var contactsAdapter: ContactsAdapter
    private lateinit var contactsViewModel: ContactsViewModel
    private lateinit var nsvNoItem: NestedScrollView
    private lateinit var tvNoItem: TextView


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        initRecyclerView()
        observeUIState()
    }


    private fun initViews(view: View){
        mIndexScrollView = view.findViewById(R.id.indexscroll_view)
        mContactsListRv = view.findViewById(R.id.contacts_list)
        nsvNoItem = view.findViewById(R.id.nsvNoItem)
        tvNoItem = view.findViewById(R.id.tvNoItem)
    }


    private fun initRecyclerView() {
        mContactsListRv.apply {
            setLayoutManager(LinearLayoutManager(mContext))
            setAdapter(ContactsAdapter(mContext).also {
                it.setupOnClickListeners()
                contactsAdapter = it
            })
            addItemDecoration(ContactsListItemDecoration(mContext))
            setItemAnimator(null)
            enableCoreSeslFeatures(fastScrollerEnabled = false)
        }

        contactsAdapter.configure(
            mContactsListRv,
            ContactsAdapter.Payload.SELECTION_MODE,
            onAllSelectorStateChanged = { contactsViewModel.allSelectorStateFlow.value = it }
        )

        mIndexScrollView.attachToRecyclerView(mContactsListRv)

        tvNoItem.translateYWithAppBar((requireActivity() as MainActivity).drawerLayout.appBarLayout, this)
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
                                "Disable indexscroller autohide" } else "Enable indexscroller autohide"
                            showLettersMenuItemTitle = if (it.isTextModeIndexScroll) {
                                "Hide indexscroller letters" } else "Show indexscroller letters"
                            keepSearchMenuItemTitle = if (it.keepSearchOnActionMode) {
                                "Dismiss search on actionmode" } else "Keep search on actionmode"
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


    private fun launchActionMode(initialSelected: Array<Long>? = null) {
        val drawerLayout = (requireActivity() as MainActivity).drawerLayout
        drawerLayout.startActionMode(
            onInflateMenu = {menu ->
                contactsAdapter.onToggleActionMode(true, initialSelected)
                menu.apply {
                    requireActivity().menuInflater.inflate(R.menu.menu_contacts_am, this)
                }
            },
            onEnd = { contactsAdapter.onToggleActionMode(false) },
            onSelectMenuItem = {
                when (it.itemId) {
                    R.id.menu_contacts_am_share -> {
                        requireActivity().toast("Share!")
                        drawerLayout.endActionMode()
                        true
                    }
                    R.id.menu_contacts_am_delete -> {
                        requireActivity().toast("Delete!")
                        drawerLayout.endActionMode()
                        true
                    }
                    else -> false
                }
            },
            onSelectAll = { isChecked: Boolean -> contactsAdapter.onToggleSelectAll(isChecked) },
            allSelectorStateFlow = contactsViewModel.allSelectorStateFlow,
            keepSearchMode = contactsViewModel.contactsSettingsStateFlow.value.keepSearchOnActionMode
        )
    }

    private val menuProvider = object : MenuProvider {
        private lateinit var showLettersMenuItem: MenuItem
        private lateinit var autoHideMenuItem: MenuItem
        private lateinit var keepSearchMenuItem: MenuItem
        var showLettersMenuItemTitle: String = ""
        var autoHideMenuItemTitle: String = ""
        var keepSearchMenuItemTitle: String = ""

        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_contacts_list, menu)
            showLettersMenuItem = menu.findItem(R.id.menu_contacts_indexscroll_show_letters)
            autoHideMenuItem = menu.findItem(R.id.menu_contacts_indexscroll_autohide)
            keepSearchMenuItem = menu.findItem(R.id.menu_contacts_keep_search)

            val menuItem = menu.findItem(R.id.menu_about_app)
            menuItem.setBadge(Badge.DOT)
        }

        override fun onPrepareMenu(menu: Menu) {
            showLettersMenuItem.title = showLettersMenuItemTitle
            autoHideMenuItem.title = autoHideMenuItemTitle
            keepSearchMenuItem.title = keepSearchMenuItemTitle
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

    private fun ToolbarLayout.launchSearchMode(onBackBehavior: SearchModeOnBackBehavior) {
        startSearchMode(
            onBackBehavior = onBackBehavior,
            onQuery = { query, _ ->
                contactsViewModel.setQuery(query)
                true
            },
            onStart = {
                searchView.queryHint = "Search contact"
            },
            onEnd = {}
        )
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
}
