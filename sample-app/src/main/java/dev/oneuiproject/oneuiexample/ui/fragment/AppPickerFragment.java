package dev.oneuiproject.oneuiexample.ui.fragment

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import androidx.appcompat.view.menu.SeslMenuItem
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.SeslProgressBar
import androidx.apppickerview.widget.AppPickerView
import androidx.apppickerview.widget.AppPickerView.OnBindListener
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import com.sec.sesl.tester.R
import dev.oneuiproject.oneui.widget.Toast
import dev.oneuiproject.oneuiexample.ui.core.base.BaseFragment
import java.util.Collections

class AppPickerFragment : BaseFragment(), OnBindListener, AdapterView.OnItemSelectedListener {
    private var mListInitialized = false
    private var mListType = AppPickerView.TYPE_LIST
    private var mShowSystemApps = false

    private val mItems: MutableList<Boolean> = ArrayList()
    private var mIsAllAppsSelected = false
    private var mCheckedPosition = 0

    private var mAppPickerView: AppPickerView? = null
    private var mProgress: SeslProgressBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mProgress = view.findViewById(R.id.apppicker_progress)
        mAppPickerView = view.findViewById(R.id.apppicker_list)
        mAppPickerView.setItemAnimator(null)
        mAppPickerView.seslSetSmoothScrollEnabled(true)
        initSpinner(view)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && !mListInitialized) {
            fillListView()
            mListInitialized = true
        }
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


    override fun getLayoutResId(): Int {
        return R.layout.sample3_fragment_apppicker
    }

    override fun getIconResId(): Int {
        return dev.oneuiproject.oneui.R.drawable.ic_oui_all_apps
    }

    override fun getTitle(): CharSequence {
        return "AppPickerView"
    }

    private fun initSpinner(view: View) {
        val spinner = view.findViewById<AppCompatSpinner>(R.id.apppicker_spinner)

        val categories: MutableList<String> = ArrayList()
        categories.add("List")
        categories.add("List, Action Button")
        categories.add("List, CheckBox")
        categories.add("List, CheckBox, All apps")
        categories.add("List, RadioButton")
        categories.add("List, Switch")
        categories.add("List, Switch, All apps")
        categories.add("Grid")
        categories.add("Grid, CheckBox")

        val adapter = ArrayAdapter(mContext, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(androidx.appcompat.R.layout.support_simple_spinner_dropdown_item)

        spinner.adapter = adapter
        spinner.onItemSelectedListener = this
    }


    private val menuProvider: MenuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_apppicker, menu)

            val menuItem = menu.findItem(R.id.menu_apppicker_system)
            (menuItem as SeslMenuItem).badgeText = ""
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            if (menuItem.itemId == R.id.menu_apppicker_system) {
                mShowSystemApps = !mShowSystemApps
                if (mShowSystemApps) {
                    menuItem.setTitle("Hide system apps")
                } else {
                    menuItem.setTitle("Show system apps")
                }

                refreshListView()
                return true
            }
            return false
        }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
        mListType = position
        fillListView()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    private fun fillListView() {
        mIsAllAppsSelected = false
        showProgressCircle(true)
        object : Thread() {
            override fun run() {
                if (!mListInitialized) {
                    try {
                        sleep(1000)
                    } catch (ignored: InterruptedException) {
                    }
                }
                requireActivity().runOnUiThread {
                    val installedAppSet
                            : ArrayList<String> =
                        ArrayList<String>(this.installedPackageNameUnmodifiableSet)
                    if (mAppPickerView!!.itemDecorationCount > 0) {
                        for (i in 0 until mAppPickerView!!.itemDecorationCount) {
                            mAppPickerView!!.removeItemDecorationAt(i)
                        }
                    }

                    mAppPickerView!!.setAppPickerView(
                        mListType,
                        installedAppSet, AppPickerView.ORDER_ASCENDING_IGNORE_CASE
                    )
                    mAppPickerView!!.setOnBindListener(this@AppPickerFragment)

                    mItems.clear()
                    if (mListType == AppPickerView.TYPE_LIST_CHECKBOX_WITH_ALL_APPS
                        || mListType == AppPickerView.TYPE_LIST_SWITCH_WITH_ALL_APPS
                    ) {
                        mItems.add(java.lang.Boolean.FALSE)
                    }
                    for (app in installedAppSet) {
                        mItems.add(java.lang.Boolean.FALSE)
                    }
                    showProgressCircle(false)
                }
            }
        }.start()
    }

    private fun refreshListView() {
        showProgressCircle(true)
        object : Thread() {
            override fun run() {
                requireActivity().runOnUiThread {
                    val installedAppSet
                            : ArrayList<String> =
                        ArrayList<String>(this.installedPackageNameUnmodifiableSet)
                    mAppPickerView!!.resetPackages(installedAppSet)

                    mItems.clear()
                    if (mListType == AppPickerView.TYPE_LIST_CHECKBOX_WITH_ALL_APPS
                        || mListType == AppPickerView.TYPE_LIST_SWITCH_WITH_ALL_APPS
                    ) {
                        mItems.add(java.lang.Boolean.FALSE)
                    }
                    for (app in installedAppSet) {
                        mItems.add(java.lang.Boolean.FALSE)
                    }
                    showProgressCircle(false)
                }
            }
        }.start()
    }

    override fun onBindViewHolder(
        holder: AppPickerView.ViewHolder,
        position: Int, packageName: String
    ) {
        when (mListType) {
            AppPickerView.TYPE_LIST -> {
                holder.item.setOnClickListener { view: View? -> }
            }

            AppPickerView.TYPE_LIST_ACTION_BUTTON -> {
                holder.actionButton!!
                    .setOnClickListener { view: View? ->
                        Toast.makeText(
                            mContext,
                            "onClick",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }

            AppPickerView.TYPE_LIST_CHECKBOX -> {
                val checkBox = holder.checkBox
                checkBox!!.isChecked = mItems[position]
                checkBox.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
                    mItems[position] =
                        isChecked
                }
            }

            AppPickerView.TYPE_LIST_CHECKBOX_WITH_ALL_APPS -> {
                val checkBox = holder.checkBox
                if (position == 0) {
                    holder.appLabel.text = "All apps"
                    checkBox!!.isChecked = mIsAllAppsSelected
                    checkBox.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
                        if (mIsAllAppsSelected != isChecked) {
                            mIsAllAppsSelected = isChecked
                            var i = 0
                            while (i < mItems.size) {
                                mItems[i] = mIsAllAppsSelected
                                i++
                            }
                            mAppPickerView!!.refreshUI()
                        }
                    }
                } else {
                    checkBox!!.isChecked = mItems[position]
                    checkBox.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
                        mItems[position] =
                            isChecked
                        checkAllAppsToggle()
                    }
                }
            }

            AppPickerView.TYPE_LIST_RADIOBUTTON -> {
                val radioButton = holder.radioButton
                radioButton!!.isChecked = mItems[position]
                holder.radioButton!!
                    .setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
                        if (isChecked) {
                            if (mCheckedPosition != position) {
                                mItems[mCheckedPosition] = false
                                mAppPickerView!!.refreshUI(mCheckedPosition)
                            }

                            mItems[position] = true
                            mCheckedPosition = position
                        }
                    }
            }

            AppPickerView.TYPE_LIST_SWITCH -> {
                val switchWidget = holder.switch
                switchWidget!!.isChecked = mItems[position]
                switchWidget.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
                    mItems[position] =
                        isChecked
                }
            }

            AppPickerView.TYPE_LIST_SWITCH_WITH_ALL_APPS -> {
                val switchWidget = holder.switch
                if (position == 0) {
                    holder.appLabel.text = "All apps"
                    switchWidget!!.isChecked = mIsAllAppsSelected
                    switchWidget.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
                        if (mIsAllAppsSelected != isChecked) {
                            mIsAllAppsSelected = isChecked
                            var i = 0
                            while (i < mItems.size) {
                                mItems[i] = mIsAllAppsSelected
                                i++
                            }
                            mAppPickerView!!.refreshUI()
                        }
                    }
                } else {
                    switchWidget!!.isChecked = mItems[position]
                    switchWidget.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
                        mItems[position] =
                            isChecked
                        checkAllAppsToggle()
                    }
                }
            }

            AppPickerView.TYPE_GRID -> {
                holder.item.setOnClickListener { view: View? -> }
            }

            AppPickerView.TYPE_GRID_CHECKBOX -> {
                val checkBox = holder.checkBox
                checkBox!!.isChecked = mItems[position]
                checkBox.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
                    mItems[position] =
                        isChecked
                }
                holder.item.setOnClickListener { view: View? ->
                    checkBox.isChecked =
                        !checkBox.isChecked
                }
            }
        }
    }

    private fun checkAllAppsToggle() {
        mIsAllAppsSelected = true
        for (selected in mItems) {
            if (!selected) {
                mIsAllAppsSelected = false
                break
            }
        }
        mAppPickerView!!.refreshUI(0)
    }

    private fun showProgressCircle(show: Boolean) {
        mProgress!!.visibility = if (show) View.VISIBLE else View.GONE
        mAppPickerView!!.visibility =
            if (show) View.GONE else View.VISIBLE
    }

    private val installedPackageNameUnmodifiableSet: Set<String>
        get() {
            val set = HashSet<String>()
            for (appInfo in installedAppList) {
                set.add(appInfo.packageName)
            }
            return Collections.unmodifiableSet(set)
        }

    private val installedAppList: List<ApplicationInfo>
        get() {
            val list = ArrayList<ApplicationInfo>()
            val apps = mContext.packageManager
                .getInstalledApplications(PackageManager.GET_META_DATA)
            for (appInfo in apps) {
                if ((appInfo.flags and (ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
                            or ApplicationInfo.FLAG_SYSTEM)) > 0 && !mShowSystemApps
                ) {
                    continue
                }
                list.add(appInfo)
            }
            return list
        }
}
