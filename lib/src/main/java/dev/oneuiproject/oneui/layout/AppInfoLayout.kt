@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package dev.oneuiproject.oneui.layout

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RestrictTo
import androidx.appcompat.widget.SeslProgressBar
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.view.children
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import dev.oneuiproject.oneui.delegates.AllSelectorState
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.dpToPx
import dev.oneuiproject.oneui.ktx.ifNegative
import dev.oneuiproject.oneui.ktx.onMultiClick
import dev.oneuiproject.oneui.layout.internal.util.getAppVersion
import dev.oneuiproject.oneui.layout.internal.util.getApplicationName
import dev.oneuiproject.oneui.utils.DeviceLayoutUtil
import dev.oneuiproject.oneui.widget.CardItemView
import kotlinx.coroutines.flow.StateFlow


/**
 * Custom App Info Layout like in any App from Samsung showing the App name, app version, and other related app info.
 */
class AppInfoLayout(context: Context, attrs: AttributeSet?) : ToolbarLayout(context, attrs) {

    sealed interface Status {
        /**
         * The app is checking for updates. A [ProgressBar][SeslProgressBar] will be shown.
         */
        data object Loading : Status

        /**
         * There is a update available and the update button will be visible.
         *
         * @see [setMainButtonClickListener]
         */
        data object UpdateAvailable : Status

        /**
         * There is a update available and downloaded and the update button will be visible.
         *
         * @see [setMainButtonClickListener]
         */
        data object UpdateDownloaded : Status

        /**
         * There are no updates available.
         */
        data object NoUpdate : Status

        /**
         * Updates aren't possible in this app. Buttons and status text won't be shown.
         */
        data object NotUpdatable : Status

        /**
         * The device has no internet connection. Show a retry button.
         *
         * @see [setMainButtonClickListener]
         */
        data object NoConnection : Status

        /**
         * Update check failed.  Show a retry button. Custom message will be shown when set.
         */
        data class Failed(@JvmField val message: CharSequence? = null) : Status
    }

    /**
     * Listener for the update and retry button.
     */
    interface OnClickListener {
        fun onUpdateClicked(v: View)
        fun onRetryClicked(v: View)
    }

    private var mainButtonClickListener: OnClickListener? = null

    private val mAILContainer: LinearLayout?
    private val mAppNameTextView: TextView
    private val mVersionTextView: TextView
    private val mUpdateNotice: TextView
    private val mUpdateButton: Button
    private var mProgressCircle: SeslProgressBar

    private var mInfoTextColor: Int = 0
    private val optionalTextParent: LinearLayout

    /**
     * Get the App Info's current update state.
     *
     * @see Status
     */
    var updateStatus: Status = Status.NoUpdate
        /**
         * Set the App Info's update state.
         *
         * @param status
         */
        set(status) {
            field = status
            when (status) {
                Status.NotUpdatable -> {
                    mProgressCircle.isGone = true
                    mUpdateNotice.isGone = true
                    mUpdateButton.isGone = true
                }

                Status.Loading -> {
                    mProgressCircle.isVisible = true
                    mUpdateNotice.isGone = true
                    mUpdateButton.isGone = true
                }

                Status.UpdateAvailable,
                Status.UpdateDownloaded -> {
                    mProgressCircle.isGone = true
                    mUpdateNotice.isGone = false
                    mUpdateButton.isGone = false
                    mUpdateNotice.text = context.getText(R.string.new_version_is_available)
                    mUpdateButton.text = context.getText(R.string.update)
                }

                Status.NoUpdate -> {
                    mProgressCircle.isGone = true
                    mUpdateNotice.isGone = false
                    mUpdateButton.isGone = true
                    mUpdateNotice.text = context.getText(R.string.latest_version)
                }

                Status.NoConnection -> {
                    mProgressCircle.isGone = true
                    mUpdateNotice.isGone = false
                    mUpdateButton.isGone = false
                    mUpdateNotice.text = context.getText(R.string.network_connect_is_not_stable)
                    mUpdateButton.text = context.getText(R.string.retry)
                }

                is Status.Failed -> {
                    mProgressCircle.isGone = true
                    mUpdateNotice.isGone = false
                    mUpdateButton.isGone = false
                    mUpdateNotice.text = status.message
                    mUpdateButton.text = context.getText(R.string.retry)
                }
            }
        }

    init {
        isExpanded = false

        var titleTextColor = 0

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.AppInfoLayout,
            R.attr.appInfoLayoutStyle, R.style.OneUI_AppInfoStyle
        ).use { ta ->
            titleTextColor = ta.getColor(
                R.styleable.AppInfoLayout_appTitleTextColor,
                ContextCompat.getColor(
                    context,
                    R.color.oui_appinfolayout_app_label_text_color
                )
            )
            mInfoTextColor = ta.getColor(
                R.styleable.AppInfoLayout_appInfoTextColor,
                ContextCompat.getColor(
                    context,
                    R.color.oui_appinfolayout_sub_text_color
                )
            )

        }

        showNavigationButtonAsBack = true
        activity?.setSupportActionBar(null)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        LayoutInflater.from(context).inflate(R.layout.oui_layout_app_info, mainContainer, true)
        mAILContainer = findViewById(R.id.app_info_lower_layout)
        mAppNameTextView = findViewById(R.id.app_info_name)
        mVersionTextView = findViewById(R.id.app_info_version)
        mUpdateNotice = findViewById(R.id.app_info_update_notice)
        mUpdateButton = findViewById(R.id.app_info_update)
        mProgressCircle = findViewById(R.id.app_info_progress)

        optionalTextParent = findViewById(R.id.app_info_upper_layout)

        setLayoutMargins()
        with(context) {
            setTitle(context.getApplicationName())
            mVersionTextView.text = getString(R.string.version_info, getAppVersion())
        }

        mAppNameTextView.setTextColor(titleTextColor)
        mVersionTextView.setTextColor(mInfoTextColor)
        mUpdateNotice.setTextColor(mInfoTextColor)

        toolbar.apply {
            post { setupMenu() }
        }
    }

    private fun Toolbar.setupMenu() {
        inflateMenu(R.menu.app_info_menu)
        setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_app_info -> {
                    openSettingsAppInfo()
                    true
                }

                else -> false
            }
        }
    }

    private fun openSettingsAppInfo() {
        val intent = Intent(
            "android.settings.APPLICATION_DETAILS_SETTINGS",
            Uri.fromParts("package", context.packageName, null)
        )
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
    }


    /**
     * Set a custom App Info title. The default will be your App's name.
     * @param title the title to replace the app's name
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    override fun setTitle(title: CharSequence?) {
        mAppNameTextView.apply {
            if (text == title) return
            text = title
        }
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        if (mAILContainer == null) {
            super.addView(child, index, params)
        } else {
            if ((params as ToolbarLayoutParams).layoutLocation == MAIN_CONTENT/*default*/) {
                when (child) {
                    is Button -> {
                        mAILContainer.addView(child, params)
                        if (mAILContainer.indexOfChild(child) > 0) {
                            child.updateLayoutParams<LayoutParams> {
                                topMargin = 15.dpToPx(resources)
                            }
                        }
                    }
                    is CardItemView -> {
                        optionalTextParent.addView(
                            child,
                            optionalTextParent.indexOfChild(mAILContainer).ifNegative { optionalTextParent.childCount },
                            params)
                    }
                }
            }
        }
    }

    /**
     * Sets the listener for the update and retry button clicks.
     */
    fun setMainButtonClickListener(listener: OnClickListener?) {
        if (mainButtonClickListener == listener) return
        mainButtonClickListener = listener
        mUpdateButton.setOnClickListener { v: View ->
            when (updateStatus) {
                is Status.Failed,
                Status.NoConnection -> {
                    mainButtonClickListener?.onRetryClicked(v as Button)
                }

                Status.UpdateAvailable,
                Status.UpdateDownloaded -> {
                    mainButtonClickListener?.onUpdateClicked(v as Button)
                }
                else -> Unit //Not expected
            }
        }
    }


    /**
     * Add another TextView below the version text.
     *
     * @param text the text for the TextView
     * @return The TextView of the optional text created.
     */
    fun addOptionalText(text: CharSequence?): TextView {
        return TextView(context).apply {
            setText(text)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(mInfoTextColor)
            textAlignment = TEXT_ALIGNMENT_CENTER
            layoutParams = mUpdateNotice.layoutParams
            optionalTextParent.addView(this, optionalTextParent.indexOfChild(mUpdateNotice))
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        mAILContainer!!.post {
            updateButtonsWidth()
        }
    }

    private fun updateButtonsWidth(){
        if (activity == null || activity!!.isDestroyed) return

        var widthButtons = 0
        mAILContainer!!.children.forEach {
            it.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
            widthButtons = maxOf(widthButtons, it.measuredWidth)
        }

        val containerWidth = mAILContainer.width

        mUpdateButton.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
        widthButtons = maxOf(widthButtons, mUpdateButton.measuredWidth)
            .coerceIn((containerWidth * 0.6f).toInt(), (containerWidth * 0.9f).toInt())

        mUpdateButton.width = widthButtons
        mAILContainer.children.forEach {
            (it as Button).width = widthButtons
        }
    }


    private fun setLayoutMargins() {
        val mEmptyTop = findViewById<View>(R.id.app_info_empty_view_top)
        val mEmptyBottom = findViewById<View>(R.id.app_info_empty_view_bottom)
        if (mEmptyTop != null && mEmptyBottom != null && DeviceLayoutUtil.isPortrait(resources.configuration)) {
            val h = resources.displayMetrics.heightPixels
            mEmptyTop.layoutParams.height = (h * 0.10).toInt()
            mEmptyBottom.layoutParams.height = (h * 0.10).toInt()
        }
    }

    @JvmOverloads
    fun setTitleMultiClickListener(clickCount: Int = 7, listener: View.OnClickListener) =
        mAppNameTextView.onMultiClick(clickCount) { listener.onClick(mAppNameTextView) }

    /**
     * This does nothing in AppInfoLayout.
     */
    @Deprecated("Unsupported operation.", level = DeprecationLevel.ERROR)
    override fun startActionMode(
        listener: ActionModeListener,
        searchOnActionMode: SearchOnActionMode,
        allSelectorStateFlow: StateFlow<AllSelectorState>?,
        showCancel: Boolean
    ) {
        //no op
    }

    /**
     * This does nothing in AppInfoLayout.
     */
    @Deprecated("Unsupported operation.", level = DeprecationLevel.ERROR)
    override fun startSearchMode(
        listener: SearchModeListener,
        searchModeOnBackBehavior: SearchModeOnBackBehavior
    ) {
        //no op
    }

    /**
     * This does nothing in AppInfoLayout.
     */
    @Deprecated("Unsupported operation.", level = DeprecationLevel.ERROR)
    override fun endActionMode() {
        //no op
    }

    /**
     * This does nothing in AppInfoLayout.
     */
    @Deprecated("Unsupported operation.", level = DeprecationLevel.ERROR)
    override fun endSearchMode() {
        //no op
    }

    @Deprecated("Unsupported operation.", level = DeprecationLevel.ERROR)
    override val switchBar get() =
        throw UnsupportedOperationException("AppInfoLayout has no switchbar.")

    companion object{
        private const val TAG = "AppInfoLayout"
    }

}