@file:Suppress("NOTHING_TO_INLINE")

package dev.oneuiproject.oneui.delegates

import android.annotation.SuppressLint
import android.content.Context
import android.widget.CompoundButton
import android.widget.ImageButton
import androidx.apppickerview.widget.AbsAdapter
import androidx.apppickerview.widget.AppPickerView
import androidx.collection.mutableScatterSetOf
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dev.oneuiproject.oneui.design.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

/**
 * Delegate for managing [AppPickerView] operations
 *
 * - Call [AppPickerView.configure] in your Activity or Fragment to set up the delegate.
 * - Use [setListType] to change the picker type.
 * - Use [refreshAppList] to load or reload the app list.
 *
 * Sample usage:
 * ```
 * class AppPickerFragment : Fragment(),
 *                       AppPickerOp by AppPickerDelegate() {
 *
 *    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
 *        binding.appPickerView.configure(
 *                 this@AppPickerFragment,
 *                 onGetCurrentList = { appsViewModel.appPickerUiStateFlow.value.appList },
 *                 onItemClicked = { _, _, appLabel ->  toast("$appLabel clicked!") },
 *                 onItemCheckChanged = { _, _, appLabel, isChecked ->  },
 *                 onSelectAllChanged = { _, isChecked -> },
 *                 onItemActionClicked = { _, _, appLabel ->
 *                     toast("$appLabel action button clicked!")
 *                 }
 *             )
 *    }
 * ```
 */
class AppPickerDelegate : AppPickerOp, AppPickerView.OnBindListener, CoroutineScope {

    private var listType: Int? = null
    private var getCurrentList: (() -> ArrayList<String>)? = null
    private val selectedItems = mutableScatterSetOf<CharSequence>()
    private var isInitialized = AtomicBoolean(false)
    private var onItemClicked: ((pos: Int, packageName: CharSequence, appLabel:CharSequence) -> Unit)? = null
    private var onItemCheckChange: ((pos: Int, packageName: CharSequence, appLabel:CharSequence, isChecked: Boolean) -> Unit)? = null
    private var onItemActionClicked: ((pos: Int, packageName: CharSequence, appLabel:CharSequence) -> Unit)? = null

    private var onSelectAllChange: ((selectAllButton: CompoundButton, isChecked: Boolean) -> Unit)? = null
    private var onLongClicked: ((pos: Int, packageName: CharSequence, appLabel:CharSequence) -> Unit)? = null
    private lateinit var context: Context
    private lateinit var appPickerView: AppPickerView

    private val masterJob = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = masterJob + Dispatchers.Main
    private lateinit var lifecycleOwner: LifecycleOwner

    override fun AppPickerView.configure(
        lifecycleOwner: LifecycleOwner,
        onGetCurrentList: (() -> ArrayList<String>)?,
        onItemClicked: ((position: Int, packageName: CharSequence, appLabel: CharSequence) -> Unit)?,
        onItemCheckChanged: ((position: Int, packageName: CharSequence, appLabel: CharSequence, isChecked: Boolean) -> Unit)?,
        onSelectAllChanged: ((selectAllButton: CompoundButton, isChecked: Boolean) -> Unit)?,
        onItemActionClicked: ((pos: Int, packageName: CharSequence, appLabel: CharSequence) -> Unit)?,
        onLongClicked: ((position: Int, packageName: CharSequence, appLabel: CharSequence) -> Unit)?
    ) {
        appPickerView = this@configure
        this@AppPickerDelegate.context = appPickerView.context
        this@AppPickerDelegate.onItemClicked = onItemClicked
        onItemCheckChange = onItemCheckChanged
        this@AppPickerDelegate.onItemActionClicked = onItemActionClicked
        this@AppPickerDelegate.onLongClicked = onLongClicked
        onSelectAllChange = onSelectAllChanged
        getCurrentList = onGetCurrentList

        lifecycleOwner.lifecycle.addObserver(
            object: DefaultLifecycleObserver{
                override fun onDestroy(owner: LifecycleOwner) {
                    super.onDestroy(owner)
                    masterJob.cancel()
                }
            }
        )
    }

    @SuppressLint("RestrictedApi")
    override fun refreshAppList() {
        launch(Dispatchers.IO) {
            getCurrentList().let {
                if (!isInitialized.getAndSet(true) || appPickerView.adapter == null) {
                    withContext(Dispatchers.Main) {
                        with(appPickerView) {
                            clearItemDecorations()
                            setAppPickerView(
                                listType ?: AppPickerView.TYPE_LIST,
                                it,
                                AppPickerView.ORDER_ASCENDING_IGNORE_CASE
                            )
                            setOnBindListener(this@AppPickerDelegate)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) { appPickerView.resetPackages(it) }
                }
            }
        }
    }


    override fun setListType(@AppPickerView.AppPickerType listType: Int) {
        if (this@AppPickerDelegate.listType == listType) return
        this@AppPickerDelegate.listType = listType
        isInitialized.set(false)
        refreshAppList()
    }

    override fun clearSelectedItems() {
        selectedItems.clear()
    }

    /**
     * Called internally by AppPickerView adapter
     */
    override fun onBindViewHolder(
        holder: AppPickerView.ViewHolder,
        position: Int, packageName: String
    ) {
        when (listType) {
            AppPickerView.TYPE_LIST -> doOnBind( position, packageName, holder, null)
            AppPickerView.TYPE_LIST_ACTION_BUTTON -> doOnBind( position, packageName, holder, holder.actionButton)
            AppPickerView.TYPE_GRID -> doOnBind( position, packageName, holder, null)
            AppPickerView.TYPE_LIST_CHECKBOX -> doOnBindCheckable( holder.checkBox!!, position, packageName, holder)
            AppPickerView.TYPE_LIST_CHECKBOX_WITH_ALL_APPS -> doOnBindCheckableWithAll(holder.checkBox!!, position, packageName, holder)
            AppPickerView.TYPE_LIST_RADIOBUTTON -> doOnBindCheckable( holder.radioButton!!, position, packageName, holder)
            AppPickerView.TYPE_LIST_SWITCH -> doOnBindCheckable( holder.switch!!, position, packageName, holder)
            AppPickerView.TYPE_LIST_SWITCH_WITH_ALL_APPS -> doOnBindCheckableWithAll(holder.switch!!, position, packageName, holder)
            AppPickerView.TYPE_GRID_CHECKBOX -> doOnBindCheckOnClick( holder.checkBox!!, position, packageName, holder)
        }
    }

    private inline fun AppPickerView.clearItemDecorations(){
        if (itemDecorationCount > 0) {
            for (i in 0 until itemDecorationCount) {
                removeItemDecorationAt(i)
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun getAbsAdapter(): AbsAdapter{
        return (appPickerView.adapter!! as AbsAdapter)
    }


    private fun doOnBindCheckableWithAll(switchWidget: CompoundButton, position: Int,
                                                packageName: CharSequence, holder: AppPickerView.ViewHolder) {
        if (position == 0) {
            if (AppPickerView.ALL_APPS_STRING != packageName) return
            holder.appLabel.text = context.getString(R.string.oui_des_action_mode_all_checkbox)
            switchWidget.apply {
                isChecked = selectedItems.size == appListCache.size
                holder.itemView.setOnClickListener {
                    isChecked = !isChecked
                    if (isChecked) {
                        selectedItems.addAll(appListCache)
                    } else {
                        selectedItems.clear()
                    }
                    appPickerView.refreshUI()
                    onSelectAllChange?.invoke(switchWidget, isChecked)
                }
            }
        } else {
            switchWidget.apply {
                isChecked = selectedItems.contains(packageName)
                setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
                    onItemCheckChange?.invoke(
                        position,
                        packageName,
                        holder.appLabel.text.toString(),
                        isChecked
                    )
                }
                holder.itemView.setOnClickListener {
                    isChecked = !isChecked
                    if (isChecked) {
                        if (selectedItems.add(packageName)) {
                            if (selectedItems.size == appListCache.size) {
                                appPickerView.refreshUI(0)
                            }
                        }
                    } else {
                        val refreshAll = selectedItems.size == appListCache.size
                        if (selectedItems.remove(packageName)){
                            if (refreshAll) appPickerView.refreshUI(0)
                        }
                    }
                }
            }

        }
    }

    private fun doOnBindCheckable(switchWidget: CompoundButton, position: Int,
                                         packageName: CharSequence, holder: AppPickerView.ViewHolder) {
        with(switchWidget) {
            isChecked = selectedItems.contains(packageName)
            setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
                if (isChecked) {
                    selectedItems.add(packageName)
                } else {
                    selectedItems.remove(packageName)
                }
                appPickerView.refreshUI(position)
                onItemCheckChange?.invoke(
                    position,
                    packageName,
                    holder.appLabel.text.toString(),
                    isChecked
                )
            }
        }
    }

    private inline fun doOnBind(position: Int, packageName: CharSequence, holder: AppPickerView.ViewHolder,
                                actionButton: ImageButton?) {
        holder.itemView.setOnClickListener {
            onItemClicked?.invoke(position, packageName, holder.appLabel.text.toString())
        }
        actionButton?.setOnClickListener {
            onItemActionClicked?.invoke(position, packageName, holder.appLabel.text.toString())
        }
    }

    private fun doOnBindCheckOnClick(switchWidget: CompoundButton, position: Int,
                                            packageName: CharSequence, holder: AppPickerView.ViewHolder) {
        with(switchWidget) {
            isChecked = selectedItems.contains(packageName)
            setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
                if (isChecked) {
                    selectedItems.add(packageName)
                } else {
                    selectedItems.remove(packageName)
                }
                appPickerView.refreshUI(position)
                onItemCheckChange?.invoke(
                    position,
                    packageName,
                    holder.appLabel.text.toString(),
                    isChecked
                )
            }
            holder.itemView.setOnClickListener {
                isChecked = !isChecked
            }
        }
    }
}


interface AppPickerOp{
    /**
     * Should be called on [Activity.onCreate] or [Fragment.onViewCreated]
     *
     * @param onGetCurrentList Lambda function to provide own app list.
     * If not set or set to `null`, it will use the [default implementation][AppPickerView.getInstalledPackages].
     * @param onItemClicked Lambda function to handle item click.
     * @param onItemCheckChanged Lambda function to handle item check change.
     * @param onItemActionClicked Lambda function to handle item action click.
     * @param onLongClicked Lambda function to handle item long click.
     */
    fun AppPickerView.configure(
        lifecycleOwner: LifecycleOwner,
        /**
         * Set lambda to provide own app list. If not set or set to `null`, it will use the [default implementation]
         * [AppPickerView.getInstalledPackages]
         */
        onGetCurrentList: (() -> ArrayList<String>)? = null,
        onItemClicked: ((position: Int, packageName: CharSequence, appLabel:CharSequence) -> Unit)? = null,
        onItemCheckChanged: ((position: Int, packageName: CharSequence, appLabel:CharSequence, isChecked: Boolean) -> Unit)? = null,
        onSelectAllChanged: ((selectAllButton: CompoundButton, isChecked: Boolean) -> Unit)? = null,
        onItemActionClicked: ((pos: Int, packageName: CharSequence, appLabel:CharSequence) -> Unit)? = null,
        onLongClicked: ((position: Int, packageName: CharSequence, appLabel: CharSequence) -> Unit)? = null
    )

    /**
     * Updates the apps list. This is also called internally when [AppPickerView.AppPickerType] is updated by [setListType].
     */
    fun refreshAppList()
    /**
     * Sets the [AppPickerView.AppPickerType] to use.
     */
    fun setListType(listType: Int)

    fun clearSelectedItems()
}