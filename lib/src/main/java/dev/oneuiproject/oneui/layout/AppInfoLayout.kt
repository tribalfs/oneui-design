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
import androidx.core.content.withStyledAttributes
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
 *
 * **Important:**
 * - This view must be hosted within an [AppCompatActivity][androidx.appcompat.app.AppCompatActivity]
 * as it relies on AppCompat-specific features and theming. Otherwise, it will result to runtime exceptions.
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
         * The device connection is unstable. Show a retry button.
         *
         * @see [setMainButtonClickListener]
         */
        data object UnstableConnection : Status

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

        data object Unset : Status
    }

    private var mainButtonClickListener: OnClickListener? = null

    private val ailContainer: LinearLayout?
    private val appNameTextView: TextView
    private val versionTextView: TextView
    private val updateNotice: TextView
    private val updateButton: Button
    private var progressCircle: SeslProgressBar

    private var infoTextColor: Int = 0
    private val optionalTextParent: LinearLayout

    /**
     * Get the App Info's current update state.
     *
     * @see Status
     */
    var updateStatus: Status = Status.Unset
        /**
         * Set the App Info's update state.
         *
         * @param status
         */
        set(status) {
            field = status
            when (status) {
                Status.NotUpdatable -> {
                    progressCircle.isGone = true
                    updateNotice.isGone = true
                    updateButton.isGone = true
                }

                Status.Loading -> {
                    progressCircle.isVisible = true
                    updateNotice.isGone = true
                    updateButton.isGone = true
                }

                Status.UpdateAvailable,
                Status.UpdateDownloaded -> {
                    progressCircle.isGone = true
                    updateNotice.isGone = false
                    updateButton.isGone = false
                    updateNotice.text = context.getText(R.string.oui_des_new_version_is_available)
                    updateButton.text = context.getText(R.string.oui_des_update)
                }

                Status.NoUpdate -> {
                    progressCircle.isGone = true
                    updateNotice.isGone = false
                    updateButton.isGone = true
                    updateNotice.text = context.getText(R.string.oui_des_latest_version)
                }

                Status.UnstableConnection -> {
                    progressCircle.isGone = true
                    updateNotice.isGone = false
                    updateButton.isGone = false
                    updateNotice.text = context.getText(R.string.oui_des_network_connect_is_not_stable)
                    updateButton.text = context.getText(R.string.oui_des_retry)
                }

                Status.NoConnection -> {
                    progressCircle.isGone = true
                    updateNotice.isGone = false
                    updateButton.isGone = false
                    updateNotice.text = context.getText(R.string.oui_des_network_error)
                    updateButton.text = context.getText(R.string.oui_des_retry)
                }

                is Status.Failed -> {
                    progressCircle.isGone = true
                    updateNotice.isGone = false
                    updateButton.isGone = false
                    updateNotice.text = status.message
                    updateButton.text = context.getText(R.string.oui_des_retry)
                }

                Status.Unset -> {
                    progressCircle.isGone = true
                    updateNotice.isGone = true
                    updateButton.isGone = true
                }
            }
        }

    init {
        isExpanded = false

        var titleTextColor = 0

        context.withStyledAttributes(
            attrs, R.styleable.AppInfoLayout, R.attr.appInfoLayoutStyle, R.style.OneUI_AppInfoStyle
        ) {
            titleTextColor = getColor(
                R.styleable.AppInfoLayout_appTitleTextColor,
                ContextCompat.getColor(
                    context,
                    R.color.oui_des_appinfolayout_app_label_text_color
                )
            )
            infoTextColor = getColor(
                R.styleable.AppInfoLayout_appInfoTextColor,
                ContextCompat.getColor(
                    context,
                    R.color.oui_des_appinfolayout_sub_text_color
                )
            )

        }

        showNavigationButtonAsBack = true
        activity?.setSupportActionBar(null)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        LayoutInflater.from(context).inflate(R.layout.oui_des_layout_app_info, mainContainer, true)
        ailContainer = findViewById(R.id.app_info_lower_layout)
        appNameTextView = findViewById(R.id.app_info_name)
        versionTextView = findViewById(R.id.app_info_version)
        updateNotice = findViewById(R.id.app_info_update_notice)
        updateButton = findViewById(R.id.app_info_update)
        progressCircle = findViewById(R.id.app_info_progress)

        optionalTextParent = findViewById(R.id.app_info_upper_layout)

        setLayoutMargins()
        with(context) {
            setTitle(context.getApplicationName())
            versionTextView.text = getString(R.string.oui_des_version_info, getAppVersion())
        }

        appNameTextView.setTextColor(titleTextColor)
        versionTextView.setTextColor(infoTextColor)
        updateNotice.setTextColor(infoTextColor)

        toolbar.apply {
            post { setupMenu() }
        }
    }

    private fun Toolbar.setupMenu() {
        inflateMenu(R.menu.oui_des_app_info)
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
        appNameTextView.apply {
            if (text == title) return
            text = title
        }
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        if (ailContainer == null) {
            super.addView(child, index, params)
        } else {
            if ((params as ToolbarLayoutParams).layoutLocation == MAIN_CONTENT/*default*/) {
                when (child) {
                    is Button -> {
                        ailContainer.addView(child, params)
                        if (ailContainer.indexOfChild(child) > 0) {
                            child.updateLayoutParams<LayoutParams> {
                                topMargin = 15.dpToPx(resources)
                            }
                        }
                    }
                    is CardItemView -> {
                        optionalTextParent.addView(
                            child,
                            optionalTextParent.indexOfChild(ailContainer).ifNegative { optionalTextParent.childCount },
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
        updateButton.setOnClickListener(mainButtonClickListener)
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
            setTextColor(infoTextColor)
            textAlignment = TEXT_ALIGNMENT_CENTER
            layoutParams = updateNotice.layoutParams
            optionalTextParent.addView(this, optionalTextParent.indexOfChild(updateNotice))
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        ailContainer!!.post {
            updateButtonsWidth()
        }
    }

    private fun updateButtonsWidth(){
        if (activity == null || activity!!.isDestroyed) return

        var widthButtons = 0
        ailContainer!!.children.forEach {
            it.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
            widthButtons = maxOf(widthButtons, it.measuredWidth)
        }

        val containerWidth = ailContainer.width

        updateButton.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
        widthButtons = maxOf(widthButtons, updateButton.measuredWidth)
            .coerceIn((containerWidth * 0.6f).toInt(), (containerWidth * 0.9f).toInt())

        updateButton.width = widthButtons
        ailContainer.children.forEach {
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
        appNameTextView.onMultiClick(clickCount) { listener.onClick(appNameTextView) }

    /**
     * This does nothing in AppInfoLayout.
     */
    @Deprecated("Unsupported operation.", level = DeprecationLevel.ERROR)
    override fun startActionMode(
        listener: ActionModeListener,
        searchOnActionMode: SearchOnActionMode,
        allSelectorStateFlow: StateFlow<AllSelectorState>?,
        showCancel: Boolean,
        maxActionItems: Int
    ) {
        //no op
    }

    /**
     * This does nothing in AppInfoLayout.
     */
    @Deprecated("Unsupported operation.", level = DeprecationLevel.ERROR)
    override fun startSearchMode(
        listener: SearchModeListener,
        searchModeOnBackBehavior: SearchModeOnBackBehavior,
        predictiveBackEnabled: Boolean
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