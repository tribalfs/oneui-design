package dev.oneuiproject.oneuiexample.ui.main.fragment.apppicker

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import com.sec.sesl.tester.R
import com.sec.sesl.tester.databinding.FragmentApppickerBinding
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
import dev.oneuiproject.oneuiexample.data.AppsRepo
import dev.oneuiproject.oneuiexample.ui.main.MainActivity
import dev.oneuiproject.oneuiexample.ui.main.core.base.BaseFragment
import dev.oneuiproject.oneuiexample.ui.main.core.util.launchAndRepeatWithViewLifecycle
import dev.oneuiproject.oneuiexample.ui.main.core.util.toast
import dev.oneuiproject.oneuiexample.ui.main.fragment.apppicker.model.ListTypes
import kotlinx.coroutines.flow.collectLatest

class AppPickerFragment : BaseFragment(),
    ViewYTranslator by AppBarAwareYTranslator(),
    AppPickerOp by AppPickerDelegate() {


    private var _binding: FragmentApppickerBinding? = null
    private val binding get() = _binding!!
    private var toolbarLayout: ToolbarLayout? = null
    private lateinit var appsViewModel: AppsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View  = FragmentApppickerBinding.inflate(inflater, container, false).also {
        _binding = it
    }.root

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbarLayout = (requireActivity() as MainActivity).drawerLayout
        binding.nsvNoItem.translateYWithAppBar(toolbarLayout!!.appBarLayout, viewLifecycleOwner)

        val appsRepo = AppsRepo(requireContext())
        val viewModelFactory = AppsViewModelFactory(appsRepo)
        appsViewModel = ViewModelProvider(this, viewModelFactory)[AppsViewModel::class.java]

        binding.appPickerView.apply {
            itemAnimator = null
            seslSetSmoothScrollEnabled(true)
            configure(
                onGetCurrentList = { appsViewModel.appPickerScreenStateFlow.value.appList },
                onItemClicked = { _, _, appLabel ->
                    toast("$appLabel clicked!")
                },
                onItemCheckChanged = { _, _, appLabel, isChecked ->  },
                onSelectAllChanged = { _, isChecked -> },
                onItemActionClicked = { _, _, appLabel ->
                    toast("$appLabel action button clicked!")
                }
            )
        }

        showProgressBar(true)

        initSpinner()

        launchAndRepeatWithViewLifecycle(Lifecycle.State.CREATED){
            appsViewModel.appPickerScreenStateFlow
                .collectLatest {
                    showProgressBar(it.isLoading)
                    refreshAppList()
                    menuProvider.showSystemItemTitle =
                        getString(if (!it.showSystem) R.string.show_system_apps else R.string.hide_system_apps)
                }
        }
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
        private var mShowSystemItemBadge: Badge = Badge.NONE
        var showSystemItemTitle: String = ""

        override fun onPrepareMenu(menu: Menu) {
            menu.findItem(R.id.menu_apppicker_system)?.title = showSystemItemTitle
        }

        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_apppicker, menu)
            this.menu = menu
            menu.findItem(R.id.menu_apppicker_system)?.setBadge(mShowSystemItemBadge)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                R.id.menu_apppicker_system -> {
                    appsViewModel.toggleShowSystem()
                    menuItem.clearBadge()
                    true
                }
                R.id.menu_apppicker_search -> {
                    toolbarLayout!!.apply {
                        startSearchMode(
                            onStart = {
                                it.queryHint = "Search app"
                                if (!isSoftKeyboardShowing){
                                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                    imm.showSoftInput(it, 0)
                                }
                            },
                            onQuery = { query, _ ->
                                applyFilter(query)
                                true
                            },
                            onEnd = {

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

    override fun getLayoutResId(): Int = R.layout.fragment_apppicker

    override fun getIconResId(): Int = dev.oneuiproject.oneui.R.drawable.ic_oui_all_apps

    override fun getTitle(): CharSequence = "AppPickerView"

    companion object{
        private const val TAG = "AppPickerFragment"
    }

}