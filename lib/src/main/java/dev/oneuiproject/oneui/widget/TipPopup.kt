@file:Suppress("unused")

package dev.oneuiproject.oneui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.provider.Settings
import android.text.TextUtils
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_OUTSIDE
import android.view.Surface
import android.view.View
import android.view.View.MeasureSpec.UNSPECIFIED
import android.view.View.OnTouchListener
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.Interpolator
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.animation.SeslAnimationUtils
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.os.ConfigurationCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.reflect.content.res.SeslConfigurationReflector
import androidx.reflect.widget.SeslTextViewReflector
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.doOnEnd
import dev.oneuiproject.oneui.ktx.setListener
import dev.oneuiproject.oneui.utils.DeviceLayoutUtil
import dev.oneuiproject.oneui.utils.internal.PathInterpolator
import dev.oneuiproject.oneui.utils.internal.createSeslElasticInterpolator
import dev.oneuiproject.oneui.widget.TipPopup.Direction.BOTTOM_LEFT
import dev.oneuiproject.oneui.widget.TipPopup.Direction.BOTTOM_RIGHT
import dev.oneuiproject.oneui.widget.TipPopup.Direction.DEFAULT
import dev.oneuiproject.oneui.widget.TipPopup.Direction.TOP_LEFT
import dev.oneuiproject.oneui.widget.TipPopup.Direction.TOP_RIGHT
import kotlin.math.ceil
import kotlin.math.floor

class TipPopup @JvmOverloads constructor(parentView: View, mode: Mode = Mode.NORMAL) {

    @Deprecated("Use the more type-safe constructor TipPopup(parentView: View, mode: Mode)")
    constructor(parentView: View, mode: Int = MODE_NORMAL) : this(
        parentView,
        if (mode == MODE_NORMAL) Mode.NORMAL else Mode.TRANSLUCENT
    )

    private val mParentView: View = parentView
    private val mContext: Context = parentView.context
    private val mResources: Resources = mContext.resources
    private val mWindowManager: WindowManager = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val mActionView: Button
    private var mActionClickListener: View.OnClickListener? = null
    private var mActionText: CharSequence? = null
    private var mActionTextColor: Int? = null

    private var mArrowDirection: Direction = DEFAULT
    private var mArrowPositionX: Int = -1
    private var mArrowPositionY: Int = -1
    private val mArrowHeight = mResources.getDimensionPixelSize(R.dimen.sem_tip_popup_balloon_arrow_height)
    private val mArrowWidth = mResources.getDimensionPixelSize(R.dimen.sem_tip_popup_balloon_arrow_width)

    private val mMessageView: TextView

    private var mBackgroundColor: Int = Color.BLACK
    private var mBalloonBg1: ImageView? = null
    private var mBalloonBg2: ImageView? = null
    private var mBalloonBubble: FrameLayout? = null
    private var mBalloonBubbleHint: ImageView? = null
    private var mBalloonBubbleIcon: ImageView? = null
    private var mBalloonContent: FrameLayout? = null
    private var mBalloonHeight = 0
    private var mBalloonPanel: FrameLayout? = null
    private var mBalloonPopup: TipWindow? = null
    private var mBalloonPopupX = 0
    private var mBalloonPopupY = 0
    private val mBalloonView: View
    private var mBalloonWidth = 0
    private var mBalloonX: Int = -1
    private var mBalloonY = 0
    private var mBorderColor: Int? = null
    private var mBubbleBackground: ImageView? = null
    private var mBubbleHeight = 0
    private var mBubbleIcon: ImageView? = null
    private var mBubblePopup: TipWindow? = null
    private var mBubblePopupX = 0
    private var mBubblePopupY = 0
    private val mBubbleView: View
    private var mBubbleWidth = 0
    private var mBubbleX = 0
    private var mBubbleY = 0

    private var mDisplayMetrics = mResources.displayMetrics
    private var mForceRealDisplay = false
    private var mHintDescription: CharSequence? = null

    private var mInitialmMessageViewWidth = 0
    private var mIsDefaultPosition = true
    private var mIsMessageViewMeasured = false
    private var mMessageText: CharSequence? = null
    private var mMessageTextColor: Int? = null

    private val mMode: Mode = mode
    private var mNeedToCallParentViewsOnClick = false
    private var mOnDismissListener: OnDismissListener? = null
    private var mOnStateChangeListener: OnStateChangeListener? = null

    private var mScaleMargin = mResources.getDimensionPixelSize(R.dimen.sem_tip_popup_scale_margin)
    private var mSideMargin = mResources.getDimensionPixelSize(R.dimen.sem_tip_popup_side_margin)

    private var mState: State = State.HINT
    private var mType: Type = Type.BALLOON_SIMPLE

    private val mDisplayFrame: Rect = Rect()
    private val mHorizontalTextMargin = mResources.getDimensionPixelSize(R.dimen.sem_tip_popup_balloon_message_margin_horizontal)
    private val mVerticalTextMargin= mResources.getDimensionPixelSize(R.dimen.sem_tip_popup_balloon_message_margin_vertical)

    /**
     * Choose either [NORMAL] or [TRANSLUCENT].
     */
    enum class Mode{
        NORMAL,
        TRANSLUCENT
    }

    /**
     * Choose either [BOTTOM_LEFT], [BOTTOM_RIGHT], [DEFAULT], [TOP_LEFT] or [TOP_RIGHT].
     */
    enum class Direction{
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        DEFAULT,
        TOP_LEFT,
        TOP_RIGHT
    }

    private enum class Type{
        BALLOON_SIMPLE,
        BALLOON_ACTION,
        BALLOON_CUSTOM
    }

    enum class State{
        DISMISSED,
        EXPANDED,
        HINT
    }

    fun interface OnDismissListener {
        fun onDismiss()
    }

    fun interface OnStateChangeListener {
        fun onStateChanged(i: State)
    }

    fun setOnStateChangeListener(changeListener: OnStateChangeListener?) {
        mOnStateChangeListener = changeListener
    }

    init {
        debugLog("mDisplayMetrics = $mDisplayMetrics")

        mContext.obtainStyledAttributes(null as AttributeSet?, R.styleable.TipPopup).use {
            mBackgroundColor = it.getColor(R.styleable.TipPopup_tipPopupBackgroundColor, Color.BLACK)
        }

        initInterpolator()

        LayoutInflater.from(mContext).apply {
            mBubbleView = inflate(R.layout.sem_tip_popup_bubble, null)
            mBalloonView = inflate(R.layout.sem_tip_popup_balloon, null).also {
                mMessageView = (it.findViewById<TextView>(R.id.sem_tip_popup_message)).apply { isVisible = false }
                mActionView = (it.findViewById<Button>(R.id.sem_tip_popup_action)).apply { isVisible = false }
            }
        }

        initBubblePopup(mode)
        initBalloonPopup(mode)

        if (mode == Mode.TRANSLUCENT) {
            mMessageView.setTextColor(
                ContextCompat.getColor(
                    mContext,
                    R.color.sem_tip_popup_text_color_translucent
                )
            )
            mActionView.setTextColor(
                ContextCompat.getColor(
                    mContext,
                    R.color.sem_tip_popup_text_color_translucent
                )
            )
        }

        mBubblePopup!!.setOnDismissListener {
            if (mState == State.HINT) {
                mState = State.DISMISSED
                if (mOnStateChangeListener != null) {
                    mOnStateChangeListener!!.onStateChanged(mState)
                    debugLog("mIsShowing : $isShowing")
                }
                if (mHandler != null) {
                    mHandler!!.removeCallbacksAndMessages(null)
                    mHandler = null
                }
                debugLog("onDismiss - BubblePopup")
            }
        }

        mBalloonPopup!!.setOnDismissListener {
            mState = State.DISMISSED
            if (mOnStateChangeListener != null) {
                mOnStateChangeListener!!.onStateChanged(mState)
                debugLog("mIsShowing : $isShowing")
            }
            debugLog("onDismiss - BalloonPopup")
            dismissBubble(false)
            if (mHandler != null) {
                mHandler!!.removeCallbacksAndMessages(null)
                mHandler = null
            }
        }

        mBalloonView.accessibilityDelegate = object : View.AccessibilityDelegate() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfo
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.addAction(AccessibilityAction(ACTION_CLICK, mContext.getString(R.string.oui_common_close)))
            }
        }
    }

    @SuppressLint("PrivateResource", "RestrictedApi")
    private fun initInterpolator() {
        if (INTERPOLATOR_SINE_IN_OUT_33 == null) {
            INTERPOLATOR_SINE_IN_OUT_33 = PathInterpolator.create(PathInterpolator.Type.TYPE_SINE_IN_OUT_33)
        }
        if (INTERPOLATOR_SINE_IN_OUT_70 == null) {
            INTERPOLATOR_SINE_IN_OUT_70 = SeslAnimationUtils.SINE_IN_OUT_70
        }
        if (INTERPOLATOR_ELASTIC_50 == null) {
            INTERPOLATOR_ELASTIC_50 = SeslAnimationUtils.ELASTIC_50
        }
        if (INTERPOLATOR_ELASTIC_CUSTOM == null) {
            INTERPOLATOR_ELASTIC_CUSTOM = createSeslElasticInterpolator(1.0f, 1.3f)
        }
    }

    private fun initBubblePopup(mode: Mode) {
        mBubbleBackground = mBubbleView.findViewById(R.id.sem_tip_popup_bubble_bg)
        mBubbleIcon = mBubbleView.findViewById(R.id.sem_tip_popup_bubble_icon)

        if (mode == Mode.TRANSLUCENT) {
            mBubbleBackground!!.setImageResource(R.drawable.sem_tip_popup_hint_background_translucent)
            mBubbleBackground!!.imageTintList = null
            if (isRTL && locale != "iw_IL") {
                mBubbleIcon!!.setImageResource(R.drawable.sem_tip_popup_hint_icon_translucent_rtl)
            } else {
                mBubbleIcon!!.setImageResource(R.drawable.sem_tip_popup_hint_icon_translucent)
            }
            mBubbleIcon!!.imageTintList = null
            mBubbleWidth = mResources.getDimensionPixelSize(R.dimen.sem_tip_popup_bubble_width_translucent)
            mBubbleHeight = mResources.getDimensionPixelSize(R.dimen.sem_tip_popup_bubble_height_translucent)
        } else {
            mBubbleWidth = mResources.getDimensionPixelSize(R.dimen.sem_tip_popup_bubble_width)
            mBubbleHeight = mResources.getDimensionPixelSize(R.dimen.sem_tip_popup_bubble_height)
        }

        mBubblePopup = TipWindowBubble(mBubbleView, mBubbleWidth, mBubbleHeight, false).apply {
            isTouchable = true
            isOutsideTouchable = true
            isAttachedInDecor = false
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initBalloonPopup(mode: Mode) {
        mBalloonBubble = mBalloonView.findViewById<FrameLayout>(R.id.sem_tip_popup_balloon_bubble).apply { isVisible = true }
        mBalloonBubbleHint = mBalloonView.findViewById(R.id.sem_tip_popup_balloon_bubble_hint)
        mBalloonBubbleIcon = mBalloonView.findViewById(R.id.sem_tip_popup_balloon_bubble_icon)
        mBalloonPanel = mBalloonView.findViewById<FrameLayout>(R.id.sem_tip_popup_balloon_panel).apply { isVisible = false }
        mBalloonContent = mBalloonView.findViewById(R.id.sem_tip_popup_balloon_content)
        mBalloonBg1 = mBalloonView.findViewById(R.id.sem_tip_popup_balloon_bg_01)
        mBalloonBg2 = mBalloonView.findViewById(R.id.sem_tip_popup_balloon_bg_02)
        if (mode == Mode.TRANSLUCENT) {
            mBalloonBg1!!.setBackgroundResource(R.drawable.sem_tip_popup_balloon_background_left_translucent)
            mBalloonBg1!!.backgroundTintList = null
            mBalloonBg2!!.setBackgroundResource(R.drawable.sem_tip_popup_balloon_background_right_translucent)
            mBalloonBg2!!.backgroundTintList = null
        }

        mBalloonPopup = TipWindowBalloon(mBalloonView, mBalloonWidth, mBalloonHeight, true).apply {
            isFocusable = true
            isTouchable = true
            isOutsideTouchable = true
            isAttachedInDecor = false
            setTouchInterceptor { _, event ->
                if (mNeedToCallParentViewsOnClick && mParentView.hasOnClickListeners()
                    && (event.action == ACTION_DOWN || event.action == ACTION_OUTSIDE)) {
                    val parentViewBounds = Rect()
                    val outLocation = IntArray(2)
                    mParentView.getLocationOnScreen(outLocation)
                    parentViewBounds[outLocation[0], outLocation[1], outLocation[0] + mParentView.width] =
                        outLocation[1] + mParentView.height
                    val isTouchContainedInParentView =
                        parentViewBounds.contains(event.rawX.toInt(), event.rawY.toInt())
                    if (isTouchContainedInParentView) {
                        debugLog("callOnClick for parent view")
                        mParentView.callOnClick()
                    }
                }
                false
            }
        }
    }

    @Deprecated("Use the type-safe show(direction: Direction) instead.")
    fun show(direction: Int) = show(
        when(direction){
            DIRECTION_TOP_LEFT -> TOP_LEFT
            DIRECTION_BOTTOM_LEFT -> BOTTOM_LEFT
            DIRECTION_BOTTOM_RIGHT -> BOTTOM_RIGHT
            DIRECTION_TOP_RIGHT -> TOP_RIGHT
            DIRECTION_DEFAULT -> DEFAULT
            else -> throw IllegalArgumentException("Invalid direction value")
        }
    )

    fun show(direction: Direction) {
        setInternal()
        if (mArrowPositionX == -1 || mArrowPositionY == -1) {
            calculateArrowPosition()
        }
        if (direction == DEFAULT) {
            calculateArrowDirection(mArrowPositionX, mArrowPositionY)
        } else {
            mArrowDirection = direction
        }
        calculatePopupSize()
        calculatePopupPosition()
        setBubblePanel()
        setBalloonPanel()
        showInternal()
    }

    fun setMessage(message: CharSequence?) {
        mMessageText = message
    }

    fun setAction(actionText: CharSequence?, listener: View.OnClickListener?) {
        mActionText = actionText
        mActionClickListener = listener
    }

    fun semCallParentViewsOnClick(needToCall: Boolean) {
        mNeedToCallParentViewsOnClick = needToCall
    }

    val isShowing: Boolean
        get() = mBubblePopup?.isShowing == true || mBalloonPopup?.isShowing == true

    fun dismiss(withAnimation: Boolean) {
        val tipWindow = mBubblePopup
        if (tipWindow != null) {
            tipWindow.setUseDismissAnimation(withAnimation)
            debugLog("mBubblePopup.mIsDismissing = " + mBubblePopup!!.mIsDismissing)
            mBubblePopup!!.dismiss()
        }
        val tipWindow2 = mBalloonPopup
        if (tipWindow2 != null) {
            tipWindow2.setUseDismissAnimation(withAnimation)
            debugLog("mBalloonPopup.mIsDismissing = " + mBalloonPopup!!.mIsDismissing)
            mBalloonPopup!!.dismiss()
        }
        val onDismissListener = mOnDismissListener
        onDismissListener?.onDismiss()
        val handler = mHandler
        if (handler != null) {
            handler.removeCallbacksAndMessages(null)
            mHandler = null
        }
    }

    fun setExpanded(expanded: Boolean) {
        if (expanded) {
            mState = State.EXPANDED
            mScaleMargin = 0
            return
        }
        mScaleMargin = mResources.getDimensionPixelSize(R.dimen.sem_tip_popup_scale_margin)
    }

    fun setTargetPosition(x: Int, y: Int) {
        if (x < 0 || y < 0) {
            return
        }
        mIsDefaultPosition = false
        mArrowPositionX = x
        mArrowPositionY = y
    }

    fun setHintDescription(hintDescription: CharSequence?) {
        mHintDescription = hintDescription
    }

    @JvmOverloads
    fun update(direction: Direction = mArrowDirection, resetHintTimer: Boolean = false) {
        if (!isShowing/* || mParentView == null*/) {
            return
        }
        setInternal()
        mBalloonX = -1
        mBalloonY = -1
        if (mIsDefaultPosition) {
            debugLog("update - default position")
            calculateArrowPosition()
        }
        if (direction == DEFAULT) {
            calculateArrowDirection(mArrowPositionX, mArrowPositionY)
        } else {
            mArrowDirection = direction
        }
        calculatePopupSize()
        calculatePopupPosition()
        setBubblePanel()
        setBalloonPanel()

        if (mState == State.HINT) {
            mBubblePopup!!.update(mBubblePopupX, mBubblePopupY, mBubblePopup!!.width, mBubblePopup!!.height)
            if (resetHintTimer) {
                debugLog("Timer Reset!")
                scheduleTimeout()
            }
        } else if (mState == State.EXPANDED) {
            mBalloonPopup!!.update(
                mBalloonPopupX,
                mBalloonPopupY,
                mBalloonPopup!!.width,
                mBalloonPopup!!.height
            )
        }
    }

    fun setMessageTextColor(color: Int) {
        mMessageTextColor = Integer.valueOf((-16777216) or color)
    }

    fun setActionTextColor(color: Int) {
        mActionTextColor = Integer.valueOf((-16777216) or color)
    }

    fun setBackgroundColor(color: Int) {
        mBackgroundColor = (-16777216) or color
    }

    fun setBackgroundColorWithAlpha(color: Int) {
        mBackgroundColor = color
    }

    fun setBorderColor(color: Int) {
        mBorderColor = Integer.valueOf((-16777216) or color)
    }

    fun setOutsideTouchEnabled(enabled: Boolean) {
        mBubblePopup!!.isFocusable = enabled
        mBubblePopup!!.isOutsideTouchable = enabled
        mBalloonPopup!!.isFocusable = enabled
        mBalloonPopup!!.isOutsideTouchable = enabled
        debugLog("outside enabled : $enabled")
    }

    fun setPopupWindowClippingEnabled(enabled: Boolean) {
        mBubblePopup!!.isClippingEnabled = enabled
        mBalloonPopup!!.isClippingEnabled = enabled
        mForceRealDisplay = !enabled
        mSideMargin =
            if (enabled) mResources.getDimensionPixelSize(R.dimen.sem_tip_popup_side_margin) else 0
        debugLog("clipping enabled : $enabled")
    }

    private fun setInternal() {
        if (mHandler == null) {
            mHandler = object : Handler(Looper.getMainLooper()) {
                override fun handleMessage(message: Message) {
                    when (message.what) {
                        MSG_TIMEOUT -> {
                            dismissBubble(true)
                            return
                        }

                        MSG_DISMISS -> {
                            dismissBubble(false)
                            return
                        }

                        MSG_SCALE_UP -> {
                            animateScaleUp()
                            return
                        }

                        else -> return
                    }
                }
            }
        }

        val currentFontScale = mResources.configuration.fontScale
        val messageTextSize = mResources.getDimensionPixelOffset(R.dimen.sem_tip_popup_balloon_message_text_size)
        val actionTextSize = mResources.getDimensionPixelOffset(R.dimen.sem_tip_popup_balloon_action_text_size)

        if (currentFontScale > 1.2f) {
            mMessageView.setTextSize(TypedValue.COMPLEX_UNIT_PX, floor(ceil(messageTextSize / currentFontScale) * 1.2f))
            mActionView.setTextSize(TypedValue.COMPLEX_UNIT_PX, floor(ceil(actionTextSize / currentFontScale) * 1.2f))
        }

        mMessageView.text = mMessageText

        if (TextUtils.isEmpty(mActionText) || mActionClickListener == null) {
            mActionView.visibility = View.GONE
            mActionView.setOnClickListener(null)
            mType = Type.BALLOON_SIMPLE
        } else {
            mActionView.visibility = View.VISIBLE
            SeslTextViewReflector.semSetButtonShapeEnabled(mActionView, true, mBackgroundColor)
            mActionView.text = mActionText
            mActionView.setOnClickListener { view ->
                mActionClickListener?.onClick(view)
                dismiss(true)
            }
            mType = Type.BALLOON_ACTION
        }

        mBubbleIcon?.contentDescription = mHintDescription

        if (mMode == Mode.TRANSLUCENT
            || mBubbleIcon == null
            || mBubbleBackground == null
            || mBalloonBubble == null
            || mBalloonBg1 == null
            || mBalloonBg2 == null) {
            return
        }

        mMessageTextColor?.let {
            mMessageView.setTextColor(it)
        }

        mActionTextColor?.let {
            mActionView.setTextColor(it)
        }

        mBubbleBackground!!.setColorFilter(mBackgroundColor)
        mBalloonBubbleHint!!.setColorFilter(mBackgroundColor)

        mBalloonBg1!!.backgroundTintList = ColorStateList.valueOf(mBackgroundColor)
        mBalloonBg2!!.backgroundTintList = ColorStateList.valueOf(mBackgroundColor)

        mBorderColor?.let {
            mBubbleBackground!!.setColorFilter(it)
            mBalloonBubbleHint!!.setColorFilter(it)
        }

    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showInternal() {
        if (mState != State.EXPANDED) {
            mState = State.HINT
            mOnStateChangeListener?.onStateChanged(State.HINT)?.also {
                debugLog("mIsShowing : $isShowing")
            }

            mBubblePopup?.showAtLocation(mParentView, 0, mBubblePopupX, mBubblePopupY)?.also {
                animateViewIn()
            }

            mBubbleView.setOnTouchListener { _, _ ->
                mState = State.EXPANDED
                mOnStateChangeListener?.onStateChanged(mState)
                mBalloonPopup?.showAtLocation(
                    mParentView,
                    0,
                    mBalloonPopupX,
                    mBalloonPopupY
                )
                mHandler?.apply {
                    removeMessages(0)
                    sendMessageDelayed(Message.obtain(mHandler, 1), 10L)
                    sendMessageDelayed(Message.obtain(mHandler, 2), 20L)
                }
                false
            }
        } else {
            mBalloonBubble!!.visibility = View.GONE
            mBalloonPanel!!.visibility = View.VISIBLE
            mMessageView.visibility = View.VISIBLE
            mOnStateChangeListener?.onStateChanged(mState)
            mBalloonPopup?.showAtLocation(mParentView, 0, mBalloonPopupX, mBalloonPopupY)
            animateBalloonScaleUp()
        }
        mBalloonView.setOnTouchListener(object : OnTouchListener {
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                if (mType == Type.BALLOON_SIMPLE) {
                    dismiss(true)
                    return false
                }
                return false
            }
        })
    }

    private fun setBubblePanel() {
        if (mBubblePopup == null) {
            return
        }
        val paramBubblePanel = mBubbleBackground!!.layoutParams as FrameLayout.LayoutParams
        if (mMode == Mode.TRANSLUCENT) {
            paramBubblePanel.width =
                mResources.getDimensionPixelSize(R.dimen.sem_tip_popup_bubble_width_translucent)
            paramBubblePanel.height =
                mResources.getDimensionPixelSize(R.dimen.sem_tip_popup_bubble_height_translucent)
        }
        when (mArrowDirection) {
            TOP_LEFT -> {
                val tipWindow = mBubblePopup
                tipWindow!!.setPivot(tipWindow.width.toFloat(), mBubblePopup!!.height.toFloat())
                paramBubblePanel.gravity = 85
                val i = mBubbleX
                val i2 = mScaleMargin
                mBubblePopupX = i - (i2 * 2)
                mBubblePopupY = mBubbleY - (i2 * 2)
                if (mMode == Mode.NORMAL) {
                    mBubbleBackground!!.setImageResource(R.drawable.sem_tip_popup_hint_background_03)
                    if (isRTL && locale != "iw_IL") {
                        mBubbleIcon!!.setImageResource(R.drawable.sem_tip_popup_hint_icon_rtl)
                    } else {
                        mBubbleIcon!!.setImageResource(R.drawable.sem_tip_popup_hint_icon)
                    }
                } else {
                    mBubbleBackground!!.rotationX = 180.0f
                }
            }

            TOP_RIGHT -> {
                val tipWindow2 = mBubblePopup
                tipWindow2!!.setPivot(0.0f, tipWindow2.height.toFloat())
                paramBubblePanel.gravity = 83
                mBubblePopupX = mBubbleX
                mBubblePopupY = mBubbleY - (mScaleMargin * 2)
                if (mMode == Mode.NORMAL) {
                    mBubbleBackground!!.setImageResource(R.drawable.sem_tip_popup_hint_background_04)
                    if (isRTL && locale != "iw_IL") {
                        mBubbleIcon!!.setImageResource(R.drawable.sem_tip_popup_hint_icon_rtl)
                    } else {
                        mBubbleIcon!!.setImageResource(R.drawable.sem_tip_popup_hint_icon)
                    }
                } else {
                    mBubbleBackground!!.rotation = 180.0f
                }
            }

            BOTTOM_LEFT -> {
                val tipWindow3 = mBubblePopup
                tipWindow3!!.setPivot(tipWindow3.width.toFloat(), 0.0f)
                paramBubblePanel.gravity = 53
                mBubblePopupX = mBubbleX - (mScaleMargin * 2)
                mBubblePopupY = mBubbleY
                if (mMode == Mode.NORMAL) {
                    mBubbleBackground!!.setImageResource(R.drawable.sem_tip_popup_hint_background_01)
                    if (isRTL && locale != "iw_IL") {
                        mBubbleIcon!!.setImageResource(R.drawable.sem_tip_popup_hint_icon_rtl)
                    } else {
                        mBubbleIcon!!.setImageResource(R.drawable.sem_tip_popup_hint_icon)
                    }
                }
            }

            BOTTOM_RIGHT -> {
                mBubblePopup!!.setPivot(0.0f, 0.0f)
                paramBubblePanel.gravity = 51
                mBubblePopupX = mBubbleX
                mBubblePopupY = mBubbleY
                if (mMode == Mode.NORMAL) {
                    mBubbleBackground!!.setImageResource(R.drawable.sem_tip_popup_hint_background_02)
                    if (isRTL && locale != "iw_IL") {
                        mBubbleIcon!!.setImageResource(R.drawable.sem_tip_popup_hint_icon_rtl)
                    } else {
                        mBubbleIcon!!.setImageResource(R.drawable.sem_tip_popup_hint_icon)
                    }
                } else {
                    mBubbleBackground!!.rotationY = 180.0f
                }
            }
            DEFAULT -> {}
        }
        mBubbleBackground!!.layoutParams = paramBubblePanel
        mBubbleIcon!!.layoutParams = paramBubblePanel
        mBubblePopup!!.width = mBubbleWidth + (mScaleMargin * 2)
        mBubblePopup!!.height = mBubbleHeight + (mScaleMargin * 2)
    }

    private fun setBalloonPanel() {
        val scaleFactor: Int
        val f: Float
        val paramBalloonContent: FrameLayout.LayoutParams
        val f2: Float
        if (mBalloonPopup != null) {
            debugLog("setBalloonPanel()")
            val i = mBubbleX
            val i2 = mBalloonX
            val leftMargin = i - i2
            val rightMargin = (i2 + mBalloonWidth) - i
            val i3 = mBubbleY
            val i4 = mBalloonY
            val topMargin = i3 - i4
            val bottomMargin = (i4 + mBalloonHeight) - (i3 + mBubbleHeight)
            val realMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            mWindowManager.defaultDisplay.getRealMetrics(realMetrics)
            val scaleFactor2 = ceil(realMetrics.density.toDouble()).toInt()
            val minBackgroundWidth =
                mResources.getDimensionPixelSize(R.dimen.sem_tip_popup_balloon_background_minwidth)
            debugLog("leftMargin[$leftMargin]")
            debugLog("rightMargin[$rightMargin] mBalloonWidth[$mBalloonWidth]")
            val horizontalContentMargin = mHorizontalTextMargin - mResources.getDimensionPixelSize(R.dimen.sem_tip_popup_button_padding_horizontal)
            val verticalButtonPadding = if (mActionView.visibility == 0) mResources.getDimensionPixelSize(
                R.dimen.sem_tip_popup_button_padding_vertical) else 0
            val paramBalloonBubble = mBalloonBubble!!.layoutParams as FrameLayout.LayoutParams
            val paramBalloonPanel = mBalloonPanel!!.layoutParams as FrameLayout.LayoutParams
            val paramBalloonContent2 = mBalloonContent!!.layoutParams as FrameLayout.LayoutParams
            val paramBalloonBg1 = mBalloonBg1!!.layoutParams as FrameLayout.LayoutParams
            val paramBalloonBg2 = mBalloonBg2!!.layoutParams as FrameLayout.LayoutParams
            if (mMode == Mode.TRANSLUCENT) {
                mBalloonBubbleHint!!.setImageResource(R.drawable.sem_tip_popup_hint_background_translucent)
                mBalloonBubbleHint!!.imageTintList = null
                if (isRTL && locale != "iw_IL") {
                    mBalloonBubbleIcon!!.setImageResource(R.drawable.sem_tip_popup_hint_icon_translucent_rtl)
                } else {
                    mBalloonBubbleIcon!!.setImageResource(R.drawable.sem_tip_popup_hint_icon_translucent)
                }
                mBalloonBubbleIcon!!.imageTintList = null
                paramBalloonBubble.width =
                    mResources.getDimensionPixelSize(R.dimen.sem_tip_popup_bubble_width_translucent)
                paramBalloonBubble.height =
                    mResources.getDimensionPixelSize(R.dimen.sem_tip_popup_bubble_height_translucent)
                scaleFactor = 0
            } else if (Color.alpha(mBackgroundColor) < 255) {
                debugLog("Updating scaleFactor to 0 because transparency is applied to background.")
                scaleFactor = 0
            } else {
                scaleFactor = scaleFactor2
            }
            when (mArrowDirection) {
                TOP_LEFT -> {
                    val tipWindow = mBalloonPopup
                    val i5 = mArrowPositionX - mBalloonX
                    val i6 = mScaleMargin
                    tipWindow!!.setPivot((i5 + i6).toFloat(), (mBalloonHeight + i6).toFloat())
                    if (mMode == Mode.NORMAL) {
                        mBalloonBubbleHint!!.setImageResource(R.drawable.sem_tip_popup_hint_background_03)
                        mBalloonBubbleIcon!!.setImageResource(R.drawable.sem_tip_popup_hint_icon)
                        f = 180.0f
                    } else {
                        f = 180.0f
                        mBalloonBubbleHint!!.rotationX = 180.0f
                    }
                    mBalloonBg1!!.rotationX = f
                    mBalloonBg2!!.rotationX = f
                    paramBalloonBg2.gravity = 85
                    paramBalloonBg1.gravity = 85
                    paramBalloonBubble.gravity = 85
                    val i7 = mBubbleWidth
                    if (rightMargin - i7 < minBackgroundWidth) {
                        val scaledLeftMargin = mBalloonWidth - minBackgroundWidth
                        paramBalloonBg1.setMargins(0, 0, minBackgroundWidth, 0)
                        paramBalloonBg2.setMargins(scaledLeftMargin - scaleFactor, 0, 0, 0)
                        debugLog("Right Margin is less then minimum background width!")
                        debugLog("updated !! leftMargin[$scaledLeftMargin],  rightMargin[$minBackgroundWidth]")
                    } else {
                        paramBalloonBg1.setMargins(0, 0, rightMargin - i7, 0)
                        paramBalloonBg2.setMargins(
                            (mBubbleWidth + leftMargin) - scaleFactor,
                            0,
                            0,
                            0
                        )
                    }
                    val i8 = mVerticalTextMargin
                    paramBalloonContent = paramBalloonContent2
                    paramBalloonContent.setMargins(
                        horizontalContentMargin,
                        i8,
                        horizontalContentMargin,
                        (mArrowHeight + i8) - verticalButtonPadding
                    )
                }

                TOP_RIGHT -> {
                    val tipWindow2 = mBalloonPopup
                    val i9 = mArrowPositionX - mBalloonX
                    val i10 = mScaleMargin
                    tipWindow2!!.setPivot((i9 + i10).toFloat(), (mBalloonHeight + i10).toFloat())
                    if (mMode == Mode.NORMAL) {
                        mBalloonBubbleHint!!.setImageResource(R.drawable.sem_tip_popup_hint_background_04)
                        mBalloonBubbleIcon!!.setImageResource(R.drawable.sem_tip_popup_hint_icon)
                        f2 = 180.0f
                    } else {
                        f2 = 180.0f
                        mBalloonBubbleHint!!.rotation = 180.0f
                    }
                    mBalloonBg1!!.rotation = f2
                    mBalloonBg2!!.rotation = f2
                    paramBalloonBg2.gravity = 83
                    paramBalloonBg1.gravity = 83
                    paramBalloonBubble.gravity = 83
                    if (leftMargin < minBackgroundWidth) {
                        val scaledRightMargin = mBalloonWidth - minBackgroundWidth
                        paramBalloonBg1.setMargins(minBackgroundWidth, 0, 0, 0)
                        paramBalloonBg2.setMargins(0, 0, scaledRightMargin - scaleFactor, 0)
                        debugLog("Left Margin is less then minimum background width!")
                        debugLog("updated !! leftMargin[$minBackgroundWidth],  rightMargin[]")
                    } else {
                        paramBalloonBg1.setMargins(leftMargin, 0, 0, 0)
                        paramBalloonBg2.setMargins(0, 0, rightMargin - scaleFactor, 0)
                    }
                    val i11 = mVerticalTextMargin
                    paramBalloonContent = paramBalloonContent2
                    paramBalloonContent.setMargins(
                        horizontalContentMargin,
                        i11,
                        horizontalContentMargin,
                        (mArrowHeight + i11) - verticalButtonPadding
                    )
                }

                BOTTOM_LEFT -> {
                    val tipWindow3 = mBalloonPopup
                    val i12 = mArrowPositionX - mBalloonX
                    val i13 = mScaleMargin
                    tipWindow3!!.setPivot((i12 + i13).toFloat(), i13.toFloat())
                    if (mMode == Mode.NORMAL) {
                        mBalloonBubbleHint!!.setImageResource(R.drawable.sem_tip_popup_hint_background_01)
                        mBalloonBubbleIcon!!.setImageResource(R.drawable.sem_tip_popup_hint_icon)
                    }
                    paramBalloonBg2.gravity = 53
                    paramBalloonBg1.gravity = 53
                    paramBalloonBubble.gravity = 53
                    paramBalloonBg1.setMargins(0, 0, rightMargin - mBubbleWidth, 0)
                    paramBalloonBg2.setMargins((mBubbleWidth + leftMargin) - scaleFactor, 0, 0, 0)
                    val i14 = mArrowHeight
                    val i15 = mVerticalTextMargin
                    paramBalloonContent2.setMargins(
                        horizontalContentMargin,
                        i14 + i15,
                        horizontalContentMargin,
                        i15 - verticalButtonPadding
                    )
                    paramBalloonContent = paramBalloonContent2
                }

                BOTTOM_RIGHT -> {
                    val tipWindow4 = mBalloonPopup
                    val i16 = mArrowPositionX - mBalloonX
                    val i17 = mScaleMargin
                    tipWindow4!!.setPivot((i16 + i17).toFloat(), i17.toFloat())
                    if (mMode == Mode.NORMAL) {
                        mBalloonBubbleHint!!.setImageResource(R.drawable.sem_tip_popup_hint_background_02)
                        mBalloonBubbleIcon!!.setImageResource(R.drawable.sem_tip_popup_hint_icon)
                    } else {
                        mBalloonBubbleHint!!.rotationY = 180.0f
                    }
                    mBalloonBg1!!.rotationY = 180.0f
                    mBalloonBg2!!.rotationY = 180.0f
                    paramBalloonBg2.gravity = 51
                    paramBalloonBg1.gravity = 51
                    paramBalloonBubble.gravity = 51
                    paramBalloonBg1.setMargins(leftMargin, 0, 0, 0)
                    paramBalloonBg2.setMargins(0, 0, rightMargin - scaleFactor, 0)
                    val i18 = mArrowHeight
                    val i19 = mVerticalTextMargin
                    paramBalloonContent2.setMargins(
                        horizontalContentMargin,
                        i18 + i19,
                        horizontalContentMargin,
                        i19 - verticalButtonPadding
                    )
                    paramBalloonContent = paramBalloonContent2
                }

                else -> paramBalloonContent = paramBalloonContent2
            }
            val i20 = mScaleMargin
            paramBalloonBubble.setMargins(
                leftMargin + i20,
                topMargin + i20,
                (rightMargin - mBubbleWidth) + i20,
                bottomMargin + i20
            )
            val balloonPanelMargin = mScaleMargin
            paramBalloonPanel.setMargins(
                balloonPanelMargin,
                balloonPanelMargin,
                balloonPanelMargin,
                balloonPanelMargin
            )
            val i21 = mBalloonX
            val i22 = mScaleMargin
            mBalloonPopupX = i21 - i22
            mBalloonPopupY = mBalloonY - i22
            mBalloonBubble!!.layoutParams = paramBalloonBubble
            mBalloonPanel!!.layoutParams = paramBalloonPanel
            mBalloonBg1!!.layoutParams = paramBalloonBg1
            mBalloonBg2!!.layoutParams = paramBalloonBg2
            mBalloonContent!!.layoutParams = paramBalloonContent
            mBalloonPopup!!.width = mBalloonWidth + (mScaleMargin * 2)
            mBalloonPopup!!.height = mBalloonHeight + (mScaleMargin * 2)
        }
    }

    private fun calculateArrowDirection(arrowX: Int, arrowY: Int) {
        if (mIsDefaultPosition) {
            val location = IntArray(2)
            mParentView.getLocationInWindow(location)
            val parentY = location[1] + (mParentView.height / 2)

            mArrowDirection = if (arrowX * 2 <= mDisplayMetrics.widthPixels) {
                if (arrowY <= parentY) {
                    TOP_RIGHT
                } else {
                    BOTTOM_RIGHT
                }
            } else if (arrowY <= parentY) {
                TOP_LEFT
            } else {
                BOTTOM_LEFT
            }
        } else if (arrowX * 2 <= mDisplayMetrics.widthPixels && arrowY * 2 <= mDisplayMetrics.heightPixels) {
            mArrowDirection = BOTTOM_RIGHT
        } else if (arrowX * 2 > mDisplayMetrics.widthPixels && arrowY * 2 <= mDisplayMetrics.heightPixels) {
            mArrowDirection = TOP_LEFT
        } else if (arrowX * 2 <= mDisplayMetrics.widthPixels && arrowY * 2 > mDisplayMetrics.heightPixels) {
            mArrowDirection = TOP_RIGHT
        } else if (arrowX * 2 > mDisplayMetrics.widthPixels && arrowY * 2 > mDisplayMetrics.heightPixels) {
            mArrowDirection = TOP_LEFT
        }
        debugLog("calculateArrowDirection : arrow position ($arrowX, $arrowY) / mArrowDirection = $mArrowDirection")
    }

    private fun calculateArrowPosition() {
        val location = IntArray(2)
        mParentView.getLocationInWindow(location)
        debugLog("calculateArrowPosition anchor location : " + location[0] + ", " + location[1])

        mArrowPositionX = location[0] + (mParentView.width / 2)
        val y = location[1] + (mParentView.height / 2)
        mArrowPositionY = if (y * 2 <= mDisplayMetrics.heightPixels) {
            (mParentView.height / 2) + y
        } else {
            y - (mParentView.height / 2)
        }
        debugLog("calculateArrowPosition mArrowPosition : $mArrowPositionX, $mArrowPositionY")
    }

    private fun calculatePopupSize() {
        mDisplayMetrics = mResources.displayMetrics

        val balloonMaxWidth = if (DeviceLayoutUtil.isDeskTopMode(mResources)) {
            val windowWidthInDexMode = mParentView.rootView.run {
                val windowLocation = IntArray(2)
                getLocationOnScreen(windowLocation)
                measuredWidth + minOf(windowLocation[0], 0)
            }
            debugLog("Window width in DexMode $windowWidthInDexMode")
            when {
                windowWidthInDexMode <= 480 -> (windowWidthInDexMode * 0.83f).toInt()
                windowWidthInDexMode <= 960 -> (windowWidthInDexMode * 0.6f).toInt()
                windowWidthInDexMode <= 1280 -> (windowWidthInDexMode * 0.45f).toInt()
                else -> (windowWidthInDexMode * 0.25f).toInt()
            }
        } else {
            val screenWidthDp = mResources.configuration.screenWidthDp
            val screenWidthPixels = mDisplayMetrics.widthPixels

            debugLog("screen width DP $screenWidthDp")
            when {
                screenWidthDp <= 480 -> (screenWidthPixels * 0.83f).toInt()
                screenWidthDp <= 960 -> (screenWidthPixels * 0.6f).toInt()
                screenWidthDp <= 1280 -> (screenWidthPixels * 0.45f).toInt()
                else -> (screenWidthPixels * 0.25f).toInt()
            }
        }

        if (!mIsMessageViewMeasured) {
            mInitialmMessageViewWidth = mMessageView.run{
                measure(0, 0)
                measuredWidth
            }
            mIsMessageViewMeasured = true
        }

        val balloonMinWidth = mArrowWidth + (mHorizontalTextMargin * 2)

        mBalloonWidth = (mInitialmMessageViewWidth + (mHorizontalTextMargin * 2))
            .coerceIn(balloonMinWidth, balloonMaxWidth)

        mBalloonHeight = mMessageView.run {
            width = mBalloonWidth - (mHorizontalTextMargin * 2)
            measure(0, 0)
            measuredHeight + (mVerticalTextMargin * 2) + mArrowHeight
        }

        if (mType == Type.BALLOON_ACTION) {
            mActionView.apply {
                measure(UNSPECIFIED, UNSPECIFIED)
                mBalloonWidth = mBalloonWidth.coerceAtLeast(
                    measuredWidth+ (mResources.getDimensionPixelSize(R.dimen.sem_tip_popup_button_padding_horizontal) * 2))
                mBalloonHeight += (measuredHeight - mResources.getDimensionPixelSize(R.dimen.sem_tip_popup_button_padding_vertical))
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun calculatePopupPosition() {
        getDisplayFrame(mDisplayFrame)

        if (mBalloonX < 0) {
            mBalloonX = if (mArrowDirection == BOTTOM_RIGHT || mArrowDirection == TOP_RIGHT) {
                (mArrowPositionX + mArrowWidth) - (mBalloonWidth / 2)
            } else {
                (mArrowPositionX - mArrowWidth) - (mBalloonWidth / 2)
            }
        }

        mArrowPositionX = if (mArrowDirection == BOTTOM_RIGHT || mArrowDirection == TOP_RIGHT) {
            mArrowPositionX.coerceIn(
                mDisplayFrame.left + mSideMargin + mHorizontalTextMargin,
                (mDisplayFrame.right - mSideMargin) - mHorizontalTextMargin - mArrowWidth
            )
        } else {
            mArrowPositionX.coerceIn(
                mDisplayFrame.left + mSideMargin + mHorizontalTextMargin + mArrowWidth,
                (mDisplayFrame.right - mSideMargin) - mHorizontalTextMargin
            )
        }

        mBalloonX = if (SeslConfigurationReflector.isDexEnabled(mContext.resources.configuration)) {
            val windowLocation = IntArray(2)
            val windowWidthInDexMode = mParentView.rootView.run {
                getLocationOnScreen(windowLocation)
                if (windowLocation[0] < 0) {
                    measuredWidth + windowLocation[0]
                }else {
                    measuredWidth
                }
            }
            mBalloonX.coerceIn(
                mDisplayFrame.left + mSideMargin,
                (windowWidthInDexMode - mSideMargin) - mBalloonWidth - minOf(windowLocation[0], 0)
            )
        } else {
            mBalloonX.coerceIn(
                mDisplayFrame.left + mSideMargin,
                (mDisplayFrame.right - mSideMargin) - mBalloonWidth
            )
        }

        when (mArrowDirection) {
            TOP_LEFT -> {
                mBubbleX = mArrowPositionX - mBubbleWidth
                mBubbleY = mArrowPositionY - mBubbleHeight
                mBalloonY = mArrowPositionY - mBalloonHeight
            }

            TOP_RIGHT -> {
                mBubbleX = mArrowPositionX
                mBubbleY = mArrowPositionY - mBubbleHeight
                mBalloonY = mArrowPositionY - mBalloonHeight
            }

            BOTTOM_LEFT -> {
                mBubbleX = mArrowPositionX - mBubbleWidth
                mBubbleY = mArrowPositionY
                mBalloonY = mArrowPositionY
            }

            BOTTOM_RIGHT -> {
                mBubbleX = mArrowPositionX
                mBubbleY = mArrowPositionY
                mBalloonY = mArrowPositionY
            }

            DEFAULT -> Unit
        }

        debugLog("QuestionPopup : $mBubbleX, $mBubbleY, $mBubbleWidth, $mBubbleHeight")
        debugLog("BalloonPopup : $mBalloonX, $mBalloonY, $mBalloonWidth, $mBalloonHeight")
    }

    private fun dismissBubble(withAnimation: Boolean) {
        mBubblePopup?.apply {
            setUseDismissAnimation(withAnimation)
            dismiss()
        }
        mOnDismissListener?.onDismiss()
    }

    private fun scheduleTimeout() {
        mHandler?.apply {
            removeMessages(0)
            sendMessageDelayed(Message.obtain(this, 0), TIMEOUT_DURATION_MS)
        }
    }

    private fun animateViewIn() {
        val pivotX: Float
        val pivotY: Float
        when (mArrowDirection) {
            TOP_LEFT -> {
                pivotX = 1.0f
                pivotY = 1.0f
            }

            TOP_RIGHT -> {
                pivotX = 0.0f
                pivotY = 1.0f
            }

            BOTTOM_LEFT -> {
                pivotX = 1.0f
                pivotY = 0.0f
            }

            BOTTOM_RIGHT,
            DEFAULT -> {
                pivotX = 0.0f
                pivotY = 0.0f
            }
        }

        val animScale = ScaleAnimation(0.0f, 1.0f, 0.0f, 1.0f, 1, pivotX, 1, pivotY).apply {
            interpolator = INTERPOLATOR_ELASTIC_50
            duration = 500L
            doOnEnd {
                scheduleTimeout()
                animateBounce()
            }
        }
        mBubbleView.startAnimation(animScale)
    }

    private fun animateBounce() {
        val pivotX: Float
        val pivotY: Float
        when (mArrowDirection) {
            TOP_LEFT -> {
                pivotX = mBubblePopup!!.width.toFloat()
                pivotY = mBubblePopup!!.height.toFloat()
            }

            TOP_RIGHT -> {
                pivotX = 0.0f
                pivotY = mBubblePopup!!.height.toFloat()
            }

            BOTTOM_LEFT -> {
                pivotX = mBubblePopup!!.width.toFloat()
                pivotY = 0.0f
            }
            BOTTOM_RIGHT,
            DEFAULT -> {
                pivotX = 0.0f
                pivotY = 0.0f
            }
        }

        val animationSet = AnimationSet(false)

        val scaleAnimation1 = ScaleAnimation(1.0f, 1.2f, 1.0f, 1.2f, 0, pivotX, 0, pivotY).apply {
            duration = ANIMATION_DURATION_BOUNCE_SCALE1
            interpolator = INTERPOLATOR_SINE_IN_OUT_70
        }

        val scaleAnimation2 = ScaleAnimation(1.0f, 0.833f, 1.0f, 0.833f, 0, pivotX, 0, pivotY).apply {
            startOffset = ANIMATION_DURATION_BOUNCE_SCALE1
            duration = ANIMATION_DURATION_BOUNCE_SCALE2
            interpolator = INTERPOLATOR_SINE_IN_OUT_33
            var count = 0
            setListener(
                onStart = { count++ },
                onEnd = {
                    debugLog("repeat count $count")
                    mBubbleView.startAnimation(animationSet)
                }
            )
        }

        animationSet.addAnimation(scaleAnimation1)
        animationSet.addAnimation(scaleAnimation2)
        animationSet.startOffset = ANIMATION_OFFSET_BOUNCE_SCALE
        mBubbleView.startAnimation(animationSet)
    }

    private fun animateScaleUp() {
        val deltaHintY: Float
        val pivotHintX: Float
        val pivotHintY: Float
        when (mArrowDirection) {
            TOP_LEFT -> {
                pivotHintX = mBalloonBubble!!.width.toFloat()
                pivotHintY = mBalloonBubble!!.height.toFloat()
                deltaHintY = 0.0f - (mArrowHeight / 2.0f)
            }

            TOP_RIGHT -> {
                pivotHintX = 0.0f
                pivotHintY = mBalloonBubble!!.height.toFloat()
                deltaHintY = 0.0f - (mArrowHeight / 2.0f)
            }

            BOTTOM_LEFT -> {
                pivotHintX = mBalloonBubble!!.width.toFloat()
                pivotHintY = 0.0f
                deltaHintY = mArrowHeight / 2.0f
            }

            BOTTOM_RIGHT -> {
                pivotHintX = 0.0f
                pivotHintY = 0.0f
                deltaHintY = mArrowHeight / 2.0f
            }
            DEFAULT -> {
                pivotHintX = 0.0f
                pivotHintY = 0.0f
                deltaHintY = mArrowHeight / 2.0f
            }
        }

        val animationBubble = AnimationSet(false).also {
            it.addAnimation(
                TranslateAnimation(0, 0.0f, 0, 0.0f, 0, 0.0f, 0, deltaHintY).apply {
                    duration = ANIMATION_DURATION_EXPAND_SCALE
                    interpolator = INTERPOLATOR_ELASTIC_CUSTOM })
            it.addAnimation(
                ScaleAnimation(1.0f, 1.5f, 1.0f, 1.5f, 0, pivotHintX, 0, pivotHintY).apply {
                    duration = ANIMATION_DURATION_EXPAND_SCALE
                    interpolator = INTERPOLATOR_ELASTIC_50
                }
            )
            it.addAnimation(
                AlphaAnimation(0.0f, 1.0f).apply {
                    duration = ANIMATION_DURATION_EXPAND_TEXT
                    interpolator = INTERPOLATOR_SINE_IN_OUT_70
                }
            )
            it.setListener(
                onStart = {mBalloonPanel!!.isVisible = true},
                onEnd = {mBalloonBubble!!.isVisible = false}
            )
        }

        mBalloonBubble!!.startAnimation(animationBubble)
        animateBalloonScaleUp()
    }

    private fun animateBalloonScaleUp() {
        val pivotPanelX: Float
        val pivotPanelY: Float
        val questionHeight = mResources.getDimensionPixelSize(R.dimen.sem_tip_popup_bubble_height)
        val panelScale = (questionHeight / mBalloonHeight).toFloat()

        when (mArrowDirection) {
            TOP_RIGHT -> {
                pivotPanelX = (mArrowPositionX - mBalloonX).toFloat()
                pivotPanelY = mBalloonHeight.toFloat()
            }

            BOTTOM_LEFT -> {
                pivotPanelX = (mArrowPositionX - mBalloonX).toFloat()
                pivotPanelY = 0.0f
            }

            BOTTOM_RIGHT -> {
                pivotPanelX = (mBubbleX - mBalloonX).toFloat()
                pivotPanelY = 0.0f
            }

            else -> {
                pivotPanelX = 0.0f
                pivotPanelY = 0.0f
            }
        }

        val animationPanel = AnimationSet(false).apply {
            addAnimation(
                ScaleAnimation(0.32f, 1.0f, panelScale, 1.0f, 0, pivotPanelX, 0, pivotPanelY).apply{
                    interpolator = INTERPOLATOR_ELASTIC_CUSTOM
                    duration = ANIMATION_DURATION_SHOW_SCALE
                }
            )
            addAnimation(
                AlphaAnimation(0.0f, 1.0f).apply {
                    interpolator = INTERPOLATOR_SINE_IN_OUT_70
                    duration = ANIMATION_DURATION_EXPAND_ALPHA
                }
            )
        }
        mBalloonPanel!!.startAnimation(animationPanel)

        val animationText: Animation = AlphaAnimation(0.0f, 1.0f).apply {
            interpolator = INTERPOLATOR_SINE_IN_OUT_33
            startOffset = ANIMATION_OFFSET_EXPAND_TEXT
            duration = ANIMATION_DURATION_EXPAND_TEXT
            setListener(
                onStart = { mMessageView.isVisible = true},
                onEnd = { dismissBubble(false) }
            )
        }
        mMessageView.startAnimation(animationText)
        mActionView.startAnimation(animationText)
    }

    private val isNavigationbarHide: Boolean
        get() {
            val context: Context = mContext
            return Settings.Global.getInt(
                context.contentResolver,
                "navigationbar_hide_bar_enabled",
                0
            ) == 1
        }

    private val navagationbarHeight: Int get() = DeviceLayoutUtil.getNavigationBarHeight(mResources)

    private val isTablet: Boolean get() = DeviceLayoutUtil.isTabletLayout(mResources)

    private fun getDisplayFrame(screenRect: Rect) {
        //var displayCutout: DisplayCutout
        val navigationbarHeight = navagationbarHeight
        val navigationbarHide = isNavigationbarHide

        @Suppress("DEPRECATION")
        val displayRotation = mWindowManager.defaultDisplay.rotation

        val realMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        mWindowManager.defaultDisplay.getRealMetrics(realMetrics)

        debugLog("realMetrics = $realMetrics")
        debugLog("is tablet? = $isTablet")

        if (mForceRealDisplay) {
            screenRect.left = 0
            screenRect.top = 0
            screenRect.right = realMetrics.widthPixels
            screenRect.bottom = realMetrics.heightPixels
            debugLog("Screen Rect = $screenRect mForceRealDisplay = $mForceRealDisplay")
            return
        }
        screenRect.left = 0
        screenRect.top = 0
        screenRect.right = mDisplayMetrics.widthPixels
        screenRect.bottom = mDisplayMetrics.heightPixels

        val bounds = Rect()
        mParentView.rootView!!.getWindowVisibleDisplayFrame(bounds)
        debugLog("Bounds = $bounds")
        if (isTablet) {
            debugLog("tablet")
            if ((realMetrics.widthPixels == mDisplayMetrics.widthPixels)
                && (realMetrics.heightPixels - mDisplayMetrics.heightPixels == navigationbarHeight)
                && navigationbarHide) {
                screenRect.bottom += navigationbarHeight
            }
        } else {
            debugLog("phone")
            when (displayRotation) {
                Surface.ROTATION_0 -> {
                    if (realMetrics.widthPixels == mDisplayMetrics.widthPixels
                        && realMetrics.heightPixels - mDisplayMetrics.heightPixels == navigationbarHeight
                        && navigationbarHide
                    ) {
                        screenRect.bottom += navigationbarHeight
                    }
                }

                Surface.ROTATION_90 -> {
                    if (realMetrics.heightPixels == mDisplayMetrics.heightPixels
                        && realMetrics.widthPixels - mDisplayMetrics.widthPixels == navigationbarHeight
                        && navigationbarHide
                    ) {
                        screenRect.right += navigationbarHeight
                    }
                    val windowInsets = ViewCompat.getRootWindowInsets(mParentView)
                    if (windowInsets != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                        && windowInsets.displayCutout != null
                    ) {
                        with(windowInsets.displayCutout!!) {
                            screenRect.left += safeInsetLeft
                            screenRect.right += safeInsetLeft
                            debugLog("displayCutout.getSafeInsetLeft() :  $safeInsetLeft")
                        }
                    }
                }

                Surface.ROTATION_180 -> {
                    if (realMetrics.widthPixels == mDisplayMetrics.widthPixels
                        && realMetrics.heightPixels - mDisplayMetrics.heightPixels == navigationbarHeight
                    ) {
                        if (navigationbarHide) {
                            screenRect.bottom += navigationbarHeight
                        } else {
                            screenRect.top += navigationbarHeight
                            screenRect.bottom += navigationbarHeight
                        }
                    } else if (realMetrics.widthPixels == mDisplayMetrics.widthPixels && bounds.top == navigationbarHeight) {
                        debugLog("Top Docked")
                        screenRect.top += navigationbarHeight
                        screenRect.bottom += navigationbarHeight
                    }
                }

                Surface.ROTATION_270 -> {
                    if (realMetrics.heightPixels == mDisplayMetrics.heightPixels
                        && realMetrics.widthPixels - mDisplayMetrics.widthPixels == navigationbarHeight
                    ) {
                        if (navigationbarHide) {
                            screenRect.right += navigationbarHeight
                        } else {
                            screenRect.left += navigationbarHeight
                            screenRect.right += navigationbarHeight
                        }
                    } else if (realMetrics.heightPixels == mDisplayMetrics.heightPixels && bounds.left == navigationbarHeight) {
                        debugLog("Left Docked")
                        screenRect.left += navigationbarHeight
                        screenRect.right += navigationbarHeight
                    }
                }
            }
        }
        debugLog("Screen Rect = $screenRect")
    }

    fun setOnDismissListener(onDismissListener: OnDismissListener?) {
        mOnDismissListener = onDismissListener
    }

    open class TipWindow(
        contentView: View,
        width: Int,
        height: Int,
        focusable: Boolean
    ) : PopupWindow(contentView, width, height, focusable) {
        var mIsDismissing = false
        private var mIsUsingDismissAnimation = true
        protected var mPivotX = 0.0f
        protected var mPivotY = 0.0f

        fun setUseDismissAnimation(useAnimation: Boolean) {
            mIsUsingDismissAnimation = useAnimation
        }

        fun setPivot(pivotX: Float, pivotY: Float) {
            mPivotX = pivotX
            mPivotY = pivotY
        }

        override fun dismiss() {
            if (mIsUsingDismissAnimation && !mIsDismissing) {
                animateViewOut()
            } else {
                super.dismiss()
            }
        }

        fun dismissFinal() = super.dismiss()

        open fun animateViewOut() {}
    }

    private class TipWindowBubble(contentView: View, width: Int, height: Int, focusable: Boolean) :
        TipWindow(contentView, width, height, focusable) {

        override fun animateViewOut() {
            val animationSet = AnimationSet(true).apply {
                addAnimation(
                    ScaleAnimation(1.0f, 0.81f, 1.0f, 0.81f, 0, mPivotX, 0, mPivotY).apply {
                        interpolator = INTERPOLATOR_ELASTIC_CUSTOM
                        duration = ANIMATION_DURATION_DISMISS_SCALE
                    })
                addAnimation(
                    AlphaAnimation(1.0f, 0.0f).apply {
                        interpolator = INTERPOLATOR_SINE_IN_OUT_33
                        duration = ANIMATION_DURATION_DISMISS_ALPHA
                    })
                setListener(
                    onStart = { mIsDismissing = true },
                    onEnd = { dismissFinal() }
                )
            }
            contentView.startAnimation(animationSet)
        }
    }


    private class TipWindowBalloon(contentView: View, width: Int, height: Int, focusable: Boolean) :
        TipWindow(contentView, width, height, focusable) {

        override fun animateViewOut() {
            val messageView = contentView.findViewById<View>(R.id.sem_tip_popup_message)

            val animAlpha = AlphaAnimation(1.0f, 0.0f).apply {
                duration = ANIMATION_DURATION_EXPAND_SCALE
            }

            val animationSet = AnimationSet(true).apply {
                addAnimation(
                    ScaleAnimation(1.0f, 0.32f, 1.0f, 0.32f, 0, mPivotX, 0, mPivotY).apply {
                        duration = ANIMATION_DURATION_DISMISS_SCALE
                        interpolator = INTERPOLATOR_ELASTIC_CUSTOM
                    }
                )
                addAnimation(animAlpha)
                setListener(
                    onStart = { mIsDismissing = true },
                    onEnd = { dismissFinal() }
                )
            }
            contentView.startAnimation(animationSet)
            messageView.startAnimation(animAlpha)
        }
    }

    private val isRTL: Boolean
        get() = mContext.resources.configuration.layoutDirection == 1

    private val locale: String
        get() = ConfigurationCompat.getLocales(mContext.resources.configuration).get(0).toString()

    private fun debugLog(msg: String) {
        Log.d(TAG, " #### $msg")
    }

    fun semGetBubblePopupWindow(): PopupWindow? {
        return mBubblePopup
    }

    fun semGetBalloonPopupWindow(): PopupWindow? {
        return mBalloonPopup
    }

    companion object {
        private const val ANIMATION_DURATION_BOUNCE_SCALE1 = 167L
        private const val ANIMATION_DURATION_BOUNCE_SCALE2 = 250L
        private const val ANIMATION_DURATION_DISMISS_ALPHA = 167L
        private const val ANIMATION_DURATION_DISMISS_SCALE = 167L
        private const val ANIMATION_DURATION_EXPAND_ALPHA = 83L
        private const val ANIMATION_DURATION_EXPAND_SCALE = 500L
        private const val ANIMATION_DURATION_EXPAND_TEXT = 167L
        private const val ANIMATION_DURATION_SHOW_SCALE = 500L
        private const val ANIMATION_OFFSET_BOUNCE_SCALE = 3000L
        private const val ANIMATION_OFFSET_EXPAND_TEXT = 333L

        private const val MSG_DISMISS = 1
        private const val MSG_SCALE_UP = 2
        private const val MSG_TIMEOUT = 0
        private const val TAG = "SemTipPopup"
        private const val TIMEOUT_DURATION_MS = 7100L
        private var mHandler: Handler? = null
        private var INTERPOLATOR_SINE_IN_OUT_33: Interpolator? = null
        private var INTERPOLATOR_SINE_IN_OUT_70: Interpolator? = null
        private var INTERPOLATOR_ELASTIC_50: Interpolator? = null
        private var INTERPOLATOR_ELASTIC_CUSTOM: Interpolator? = null


        @Deprecated("Use Direction.BOTTOM_LEFT instead.")
        const val DIRECTION_BOTTOM_LEFT: Int = 2
        @Deprecated("Use Direction.BOTTOM_RIGHT instead.")
        const val DIRECTION_BOTTOM_RIGHT: Int = 3
        @Deprecated("Use Direction.DEFAULT instead.")
        const val DIRECTION_DEFAULT: Int = -1
        @Deprecated("Use Direction.TOP_LEFT instead.")
        const val DIRECTION_TOP_LEFT: Int = 0
        @Deprecated("Use Direction.TOP_RIGHT instead.")
        const val DIRECTION_TOP_RIGHT: Int = 1
        @Deprecated("Use Mode.NORMAL instead.")
        const val MODE_NORMAL: Int = 0
        @Deprecated("Use Mode.TRANSLUCENT instead.")
        const val MODE_TRANSLUCENT: Int = 1
    }
}