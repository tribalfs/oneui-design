package dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.apps

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import com.sec.sesl.tester.R
import com.sec.sesl.tester.databinding.FragmentApppickerBinding
import dagger.hilt.android.AndroidEntryPoint
import dev.oneuiproject.oneui.delegates.AppBarAwareYTranslator
import dev.oneuiproject.oneui.delegates.AppPickerDelegate
import dev.oneuiproject.oneui.delegates.AppPickerOp
import dev.oneuiproject.oneui.delegates.ViewYTranslator
import dev.oneuiproject.oneui.ktx.clearBadge
import dev.oneuiproject.oneui.ktx.isSoftKeyboardShowing
import dev.oneuiproject.oneui.ktx.setBadge
import dev.oneuiproject.oneui.ktx.setEntries
import dev.oneuiproject.oneui.layout.Badge
import dev.oneuiproject.oneui.layout.ToolbarLayout
import dev.oneuiproject.oneui.layout.startSearchMode
import dev.oneuiproject.oneuiexample.ui.main.MainActivity
import dev.oneuiproject.oneuiexample.ui.main.core.base.AbsBaseFragment
import dev.oneuiproject.oneuiexample.ui.main.core.util.autoCleared
import dev.oneuiproject.oneuiexample.ui.main.core.util.launchAndRepeatWithViewLifecycle
import dev.oneuiproject.oneuiexample.ui.main.core.util.semToast
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.RvParentViewModel
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.apps.model.ListTypes
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class AppPickerFragment : AbsBaseFragment(R.layout.fragment_apppicker),
    ViewYTranslator by AppBarAwareYTranslator(),
    AppPickerOp by AppPickerDelegate() {

    private val binding by autoCleared { FragmentApppickerBinding.bind(requireView()) }
    private lateinit var toolbarLayout: ToolbarLayout
    private val viewModel: AppsViewModel by viewModels()
    private val parentViewModel by viewModels<RvParentViewModel>( {requireParentFragment()} )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as MainActivity).let{
            toolbarLayout = it.drawerLayout
            it.addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)
        }

        configureAppPickerView()
        showProgressBar(true)
        initSpinner()

        launchAndRepeatWithViewLifecycle(Lifecycle.State.CREATED){
            viewModel.appPickerUiStateFlow
                .collectLatest {
                    showProgressBar(it.isLoading)
                    refreshAppList()
                    menuProvider.showSystemItemTitle =
                        getString(if (!it.showSystem) R.string.show_system_apps else R.string.hide_system_apps)
                }
        }
    }

    private fun configureAppPickerView(){
        binding.appPickerView.apply {
            itemAnimator = null
            seslSetSmoothScrollEnabled(true)
            configure(
                this@AppPickerFragment,
                onGetCurrentList = { viewModel.appPickerUiStateFlow.value.appList },
                onItemClicked = { _, _, appLabel ->
                    semToast("$appLabel clicked!")
                },
                onItemCheckChanged = { _, _, appLabel, isChecked ->  },
                onSelectAllChanged = { _, isChecked -> },
                onItemActionClicked = { _, _, appLabel ->
                    semToast("$appLabel action button clicked!")
                }
            )
        }
        binding.nsvNoItem.translateYWithAppBar(toolbarLayout!!.appBarLayout, viewLifecycleOwner)
    }

    private fun showProgressBar(show: Boolean){
        binding.apppickerProgress.isVisible = show
    }

    private fun initSpinner() {
        binding.apppickerSpinner.setEntries(
            ListTypes.entries.map { getString(it.description) }
        ){pos, _ ->
            pos?.let{ setListType(ListTypes.entries[it].type)}
        }
    }


    private fun applyFilter(query: String?){
        binding.appPickerView.setSearchFilter(query){ itemCount ->
            updateRecyclerViewVisibility(itemCount > 0)
        }
    }

    private fun updateRecyclerViewVisibility(visible: Boolean){
        if (visible){
            binding.nsvNoItem.isVisible = false
            binding.appPickerView.isVisible = true
        }else{
            binding.nsvNoItem.isVisible = true
            binding.appPickerView.isVisible = false
        }
    }


    private val menuProvider = object : MenuProvider {
        private var menu: Menu? = null
        private var showSystemItemBadge: Badge = Badge.NONE
        var showSystemItemTitle: String = ""

        override fun onPrepareMenu(menu: Menu) {
            menu.findItem(R.id.menu_apppicker_system)?.title = showSystemItemTitle
        }

        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_apppicker, menu)
            this.menu = menu
            menu.findItem(R.id.menu_apppicker_system)?.setBadge(showSystemItemBadge)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                R.id.menu_apppicker_system -> {
                    viewModel.toggleShowSystem()
                    menuItem.clearBadge()
                    true
                }
                R.id.menu_apppicker_search -> {
                    toolbarLayout.apply {
                        startSearchMode(
                            onStart = {
                                it.queryHint = "Search app"
                                if (!isSoftKeyboardShowing){
                                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                    imm.showSoftInput(it, 0)
                                }
                                parentViewModel.isTabLayoutEnabled = false
                            },
                            onQuery = { query, _ ->
                                applyFilter(query)
                                true
                            },
                            onEnd = {
                                applyFilter("")
                                parentViewModel.isTabLayoutEnabled = true
                            },
                            onBackBehavior = ToolbarLayout.SearchModeOnBackBehavior.CLEAR_DISMISS
                        )
                    }
                    true
                }
                else -> false
            }
        }
    }

    companion object{
        private const val TAG = "AppPickerFragment"
    }

}