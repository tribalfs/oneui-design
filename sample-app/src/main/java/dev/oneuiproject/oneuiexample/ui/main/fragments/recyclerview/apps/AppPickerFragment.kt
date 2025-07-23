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
import androidx.picker.di.AppPickerContext
import androidx.picker.model.AppInfo
import androidx.picker.model.AppInfoData
import androidx.picker.model.viewdata.AllAppsViewData
import androidx.picker.widget.AppPickerState.OnStateChangeListener
import androidx.picker.widget.SeslAppPickerGridView
import androidx.picker.widget.SeslAppPickerView
import androidx.picker.widget.SeslAppPickerView.Companion.ORDER_ASCENDING
import com.sec.sesl.tester.R
import com.sec.sesl.tester.databinding.FragmentApppickerBinding
import dagger.hilt.android.AndroidEntryPoint
import dev.oneuiproject.oneui.delegates.AppBarAwareYTranslator
import dev.oneuiproject.oneui.delegates.ViewYTranslator
import dev.oneuiproject.oneui.ktx.dpToPx
import dev.oneuiproject.oneui.ktx.isSoftKeyboardShowing
import dev.oneuiproject.oneui.ktx.seslSetFastScrollerAdditionalPadding
import dev.oneuiproject.oneui.ktx.setEntries
import dev.oneuiproject.oneui.layout.NavDrawerLayout
import dev.oneuiproject.oneui.layout.ToolbarLayout
import dev.oneuiproject.oneui.layout.startSearchMode
import dev.oneuiproject.oneuiexample.ui.main.MainActivity
import dev.oneuiproject.oneuiexample.ui.main.core.base.AbsBaseFragment
import dev.oneuiproject.oneuiexample.ui.main.core.util.autoCleared
import dev.oneuiproject.oneuiexample.ui.main.core.util.semToast
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.RvParentViewModel
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.apps.model.ListTypes
import dev.oneuiproject.oneuiexample.ui.main.fragments.recyclerview.apps.util.getAppList


@AndroidEntryPoint
class AppPickerFragment : AbsBaseFragment(R.layout.fragment_apppicker),
    ViewYTranslator by AppBarAwareYTranslator() {

    private val binding by autoCleared { FragmentApppickerBinding.bind(requireView()) }
    private lateinit var navDrawerLayout: NavDrawerLayout
    private val parentViewModel by viewModels<RvParentViewModel>({ requireParentFragment() })
    private val packageManagerHelper by lazy { AppPickerContext(requireContext()).packageManagerHelper }
    private var currentPicker: SeslAppPickerView? = null
    private val viewModel by viewModels<AppPickerViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as MainActivity).let {
            navDrawerLayout = it.drawerLayout
            it.addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)
        }

        showAppPicker()
    }

    private fun showAppPicker() {
        viewModel.isSelectLayout.let {
            binding.appPickerSelectLayout.isVisible = it
            binding.simpleAppPickerFrame.isVisible = !it
            binding.simpleAppPickerSpinner.isVisible = !it

            if (it) {
                configureSelectLayout()
            } else {
                configureSpinner()
            }
        }
    }

    private fun setType(listType: ListTypes) {
        showProgressBar(true)
        currentPicker = when (listType) {
            ListTypes.TYPE_GRID,
            ListTypes.TYPE_GRID_CHECKBOX -> binding.appPickerGridView.also {
                it.isVisible = true
                binding.appPickerView.isVisible = false
            }
            else -> binding.appPickerView.also {
                it.isVisible = true
                binding.appPickerGridView.isVisible = false
            }
        }
        configureAppPicker(currentPicker!!)
        val packages = getAppList(requireContext(), listType)
        currentPicker!!.submitList(packages)
        updateAppPickerVisibility(packages.isNotEmpty())
        showProgressBar(false)
    }

    private fun configureAppPicker(appPicker: SeslAppPickerView) {
        if (appPicker.getTag(R.id.tag_app_picker_configured) == true) return
        appPicker.setTag(R.id.tag_app_picker_configured, true)

        appPicker.apply {
            appListOrder = ORDER_ASCENDING
            seslSetFillHorizontalPaddingEnabled(true)
            seslSetIndexTipEnabled(true)
            seslSetFastScrollerAdditionalPadding(10.dpToPx(resources))

            setOnItemClickEventListener { _, appInfo ->
                semToast("${packageManagerHelper.getAppLabel(appInfo)} clicked!")
                if (appPicker is SeslAppPickerGridView){
                    setState(appInfo, !getState(appInfo))
                    true
                } else {
                    false
                }
            }

            setOnItemActionClickEventListener { _, appInfo ->
                semToast("${packageManagerHelper.getAppLabel(appInfo)} action clicked!")
                true
            }

            setOnStateChangeListener(
                object : OnStateChangeListener {
                    override fun onStateAllChanged(isAllSelected: Boolean) {
                        setStateAll(isAllSelected)
                    }

                    override fun onStateChanged(
                        appInfo: AppInfo,
                        isSelected: Boolean
                    ) {
                        val allItemsSelected = appPicker.appDataList.count { !(it as AppInfoData).selected } == 0
                        (headerFooterAdapter.getItem(0) as? AllAppsViewData)?.selectableItem?.setValueSilence(allItemsSelected)
                    }
                }
            )
        }
    }

    private fun showProgressBar(show: Boolean) {
        binding.apppickerProgress.isVisible = show
    }

    private fun configureSpinner() {
        if (binding.simpleAppPickerSpinner.getTag(R.id.tag_app_picker_configured) == true) return
        binding.simpleAppPickerSpinner.setTag(R.id.tag_app_picker_configured, true)

        binding.simpleAppPickerSpinner.setEntries(
            ListTypes.entries.map { getString(it.description) }
        ){pos, _ ->
            pos?.let{ setType(ListTypes.entries[it]) }
        }
    }

    private fun applyFilter(query: String) {
        if (viewModel.isSelectLayout) {
            binding.appPickerSelectLayout.setSearchFilter(query)
        } else {
            currentPicker!!.setSearchFilter(query) {
                updateAppPickerVisibility(it > 0)
            }
        }
    }

    private fun updateAppPickerVisibility( visible: Boolean) {
        if (visible) {
            binding.nsvNoItem.isVisible = false
            currentPicker?.isVisible = true
        } else {
            binding.nsvNoItem.isVisible = true
            currentPicker?.isVisible = false
        }
    }

     private fun configureSelectLayout() {
        if (binding.appPickerSelectLayout.getTag(R.id.tag_app_picker_configured) == true) return
        binding.appPickerSelectLayout.setTag(R.id.tag_app_picker_configured, true)

        binding.appPickerSelectLayout.apply {
            appPickerStateView.appListOrder = ORDER_ASCENDING
            submitList(getAppList(requireContext(), ListTypes.TYPE_LIST_CHECKBOX))
            enableSelectedAppPickerView(true)
            setOnStateChangeListener(
                object : OnStateChangeListener {
                    override fun onStateAllChanged(isAllSelected: Boolean) {}

                    override fun onStateChanged(
                        appInfo: AppInfo,
                        isSelected: Boolean
                    ) {
                        if (isSelected) {
                            addSelectedItem(appInfo)
                        } else {
                            removeSelectedItem(appInfo)
                        }
                    }
                }
            )
        }
    }

    private val menuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_apppicker, menu)
        }

        override fun onPrepareMenu(menu: Menu) {
            super.onPrepareMenu(menu)
            if (viewModel.isSelectLayout) {
                menu.findItem(R.id.menu_apppicker_options).title = "Single picker mode"
            } else {
                menu.findItem(R.id.menu_apppicker_options).title = "Select layout mode"
            }
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {

                R.id.menu_apppicker_search -> {
                    navDrawerLayout.apply {
                        startSearchMode(
                            onStart = {
                                it.queryHint = "Search app"
                                if (!isSoftKeyboardShowing) {
                                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                    imm.showSoftInput(it, 0)
                                }
                                parentViewModel.isTabLayoutEnabled = false
                                binding.simpleAppPickerSpinner.isEnabled = false
                            },
                            onQuery = { query, _ ->
                                applyFilter(query)
                                true
                            },
                            onEnd = {
                                applyFilter("")
                                parentViewModel.isTabLayoutEnabled = true
                                binding.simpleAppPickerSpinner.isEnabled = true
                            },
                            onBackBehavior = ToolbarLayout.SearchModeOnBackBehavior.CLEAR_DISMISS
                        )
                    }
                    true
                }

                R.id.menu_apppicker_options -> {
                    viewModel.isSelectLayout = !viewModel.isSelectLayout
                    showAppPicker()
                    true
                }
                else -> false
            }
        }
    }

    companion object {
        private const val TAG = "AppPickerFragment"
    }

}