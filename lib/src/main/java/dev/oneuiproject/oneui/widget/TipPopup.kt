@file:Suppress("unused", "NOTHING_TO_INLINE")

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
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import androidx.core.os.ConfigurationCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import dev.oneuiproject.oneui.design.R
import dev.oneuiproject.oneui.ktx.doOnEnd
import dev.oneuiproject.oneui.ktx.semSetButtonShapeEnabled
import dev.oneuiproject.oneui.ktx.setListener
import dev.oneuiproject.oneui.utils.DeviceLayoutUtil
import dev.oneuiproject.oneui.utils.DeviceLayoutUtil.isDeskTopMode
import dev.oneuiproject.oneui.utils.internal.CachedInterpolatorFactory
import dev.oneuiproject.oneui.utils.internal.CachedInterpolatorFactory.Type.ELASTIC_50
import dev.oneuiproject.oneui.utils.internal.CachedInterpolatorFactory.Type.ELASTIC_CUSTOM
import dev.oneuiproject.oneui.utils.internal.CachedInterpolatorFactory.Type.SINE_IN_OUT_33
import dev.oneuiproject.oneui.utils.internal.CachedInterpolatorFactory.Type.SINE_IN_OUT_70
import dev.oneuiproject.oneui.widget.TipPopup.Companion.TIMEOUT_DURATION_MS
import dev.oneuiproject.oneui.widget.TipPopup.Direction.BOTTOM_LEFT
import dev.oneuiproject.oneui.widget.TipPopup.Direction.BOTTOM_RIGHT
import dev.oneuiproject.oneui.widget.TipPopup.Direction.DEFAULT
import dev.oneuiproject.oneui.widget.TipPopup.Direction.TOP_LEFT
import dev.oneuiproject.oneui.widget.TipPopup.Direction.TOP_RIGHT
import dev.oneuiproject.oneui.widget.TipPopup.Mode.NORMAL
import dev.oneuiproject.oneui.widget.TipPopup.Mode.TRANSLUCENT
import kotlin.math.ceil
import kotlin.math.floor

class TipPopup(parentView: View, mode: Mode) {

    constructor(parentView: View): this(parentView, Mode.NORMAL)

    private val parentView: View = parentView
    private val context: Context = parentView.context
    private val resources: Resources = context.resources
    private val windowManager: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val actionView: Button
    private var actionClickListener: View.OnClickListener? = null
    private var actionText: CharSequence? = null
    private var actionTextColor: Int? = null

    private var arrowDirection: Direction = DEFAULT
    private var arrowPositionX: Int = -1
    private var arrowPositionY: Int = -1
    private val arrowHeight = resources.getDimensionPixelSize(R.dimen.oui_des_tip_popup_balloon_arrow_height)
    private val arrowWidth = resources.getDimensionPixelSize(R.dimen.oui_des_tip_popup_balloon_arrow_width)

    private val messageView: TextView

    private var backgroundColor: Int = Color.BLACK
    private var balloonBg1: ImageView? = null
    private var balloonBg2: ImageView? = null
    private var balloonBubble: FrameLayout? = null
    private var balloonBubbleHint: ImageView? = null
    private var balloonBubbleIcon: ImageView? = null
    private var balloonContent: FrameLayout? = null
    private var balloonHeight = 0
    private var balloonPanel: FrameLayout? = null
    private var balloonPopup: TipWindow? = null
    private var balloonPopupX = 0
    private var balloonPopupY = 0
    private val balloonView: View
    private var balloonWidth = 0
    private var balloonX: Int = -1
    private var balloonY = 0
    private var borderColor: Int? = null
    private var bubbleBackground: ImageView? = null
    private var bubbleHeight = 0
    private var bubbleIcon: ImageView? = null
    private var bubblePopup: TipWindow? = null
    private var bubblePopupX = 0
    private var bubblePopupY = 0
    private val bubbleView: View
    private var bubbleWidth = 0
    private var bubbleX = 0
    private var bubbleY = 0

    private var displayMetrics = resources.displayMetrics
    private var forceRealDisplay = false
    private var hintDescription: CharSequence? = null

    private var initialMessageViewWidth = 0
    private var isDefaultPosition = true
    private var isMessageViewMeasured = false
    private var messageText: CharSequence? = null
    private var messageTextColor: Int? = null

    private val mode: Mode = mode
    private var needToCallParentViewsOnClick = false
    private var onDismissListener: OnDismissListener? = null
    private var onStateChangeListener: OnStateChangeListener? = null

    private var scaleMargin = resources.getDimensionPixelSize(R.dimen.oui_des_tip_popup_scale_margin)
    private var sideMargin = resources.getDimensionPixelSize(R.dimen.oui_des_tip_popup_side_margin)

    private var state: State = State.HINT
    private var type: Type = Type.BALLOON_SIMPLE

    private val displayFrame: Rect = Rect()
    private val horizontalTextMargin = resources.getDimensionPixelSize(R.dimen.oui_des_tip_popup_balloon_message_margin_horizontal)
    private val verticalTextMargin= resources.getDimensionPixelSize(R.dimen.oui_des_tip_popup_balloon_message_margin_vertical)

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
        onStateChangeListener = changeListener
    }

    init {
        debugLog("mDisplayMetrics = $displayMetrics")

        context.withStyledAttributes(null, R.styleable.TipPopup) {
            backgroundColor = getColor(R.styleable.TipPopup_tipPopupBackgroundColor, Color.BLACK)
        }

        initInterpolator()

        LayoutInflater.from(context).apply {
            bubbleView = inflate(R.layout.oui_des_tip_popup_bubble, null)
            balloonView = inflate(R.layout.oui_des_tip_popup_balloon, null).also {
                messageView = (it.findViewById<TextView>(R.id.oui_des_tip_popup_message)).apply { isVisible = false }
                actionView = (it.findViewById<Button>(R.id.oui_des_tip_popup_action)).apply { isVisible = false }
            }
        }

        initBubblePopup(mode)
        initBalloonPopup(mode)

        if (mode == Mode.TRANSLUCENT) {
            messageView.setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.oui_des_tip_popup_text_color_translucent
                )
            )
            actionView.setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.oui_des_tip_popup_text_color_translucent
                )
            )
        }

        bubblePopup!!.setOnDismissListener {
            if (state == State.HINT) {
                state = State.DISMISSED
                if (onStateChangeListener != null) {
                    onStateChangeListener!!.onStateChanged(state)
                    debugLog("mIsShowing : $isShowing")
                }
                if (mHandler != null) {
                    mHandler!!.removeCallbacksAndMessages(null)
                    mHandler = null
                }
                debugLog("onDismiss - BubblePopup")
            }
        }

        balloonPopup!!.setOnDismissListener {
            state = State.DISMISSED
            if (onStateChangeListener != null) {
                onStateChangeListener!!.onStateChanged(state)
                debugLog("mIsShowing : $isShowing")
            }
            debugLog("onDismiss - BalloonPopup")
            dismissBubble(false)
            if (mHandler != null) {
                mHandler!!.removeCallbacksAndMessages(null)
                mHandler = null
            }
        }

        balloonView.accessibilityDelegate = object : View.AccessibilityDelegate() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfo
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.addAction(AccessibilityAction(ACTION_CLICK, context.getString(R.string.oui_des_common_close)))
            }
        }
    }

    @SuppressLint("PrivateResource", "RestrictedApi")
    private fun initInterpolator() {
        if (INTERPOLATOR_SINE_IN_OUT_33 == null) {
            INTERPOLATOR_SINE_IN_OUT_33 = CachedInterpolatorFactory.getOrCreate(SINE_IN_OUT_33)
        }
        if (INTERPOLATOR_SINE_IN_OUT_70 == null) {
            INTERPOLATOR_SINE_IN_OUT_70 = CachedInterpolatorFactory.getOrCreate(SINE_IN_OUT_70)
        }
        if (INTERPOLATOR_ELASTIC_50 == null) {
            INTERPOLATOR_ELASTIC_50 = CachedInterpolatorFactory.getOrCreate(ELASTIC_50)
        }
        if (INTERPOLATOR_ELASTIC_CUSTOM == null) {
            INTERPOLATOR_ELASTIC_CUSTOM =  CachedInterpolatorFactory.getOrCreate(ELASTIC_CUSTOM)
        }
    }

    private fun initBubblePopup(mode: Mode) {
        bubbleBackground = bubbleView.findViewById(R.id.oui_des_tip_popup_bubble_bg)
        bubbleIcon = bubbleView.findViewById(R.id.oui_des_tip_popup_bubble_icon)

        if (mode == Mode.TRANSLUCENT) {
            bubbleBackground!!.setImageResource(R.drawable.oui_des_tip_popup_hint_bg_translucent)
            bubbleBackground!!.imageTintList = null
            if (isRTL && locale != "iw_IL") {
                bubbleIcon!!.setImageResource(R.drawable.oui_des_tip_popup_hint_icon_translucent_rtl)
            } else {
                bubbleIcon!!.setImageResource(R.drawable.oui_des_tip_popup_hint_icon_translucent)
            }
            bubbleIcon!!.imageTintList = null
            bubbleWidth = resources.getDimensionPixelSize(R.dimen.oui_des_tip_popup_bubble_width_translucent)
            bubbleHeight = resources.getDimensionPixelSize(R.dimen.oui_des_tip_popup_bubble_height_translucent)
        } else {
            bubbleWidth = resources.getDimensionPixelSize(R.dimen.oui_des_tip_popup_bubble_width)
            bubbleHeight = resources.getDimensionPixelSize(R.dimen.oui_des_tip_popup_bubble_height)
        }

        bubblePopup = TipWindowBubble(bubbleView, bubbleWidth, bubbleHeight, false).apply {
            isTouchable = true
            isOutsideTouchable = true
            if (Build.VERSION.SDK_INT >= 22) isAttachedInDecor = false
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initBalloonPopup(mode: Mode) {
        balloonBubble = balloonView.findViewById<FrameLayout>(R.id.oui_des_tip_popup_balloon_bubble).apply { isVisible = true }
        balloonBubbleHint = balloonView.findViewById(R.id.oui_des_tip_popup_balloon_bubble_hint)
        balloonBubbleIcon = balloonView.findViewById(R.id.oui_des_tip_popup_balloon_bubble_icon)
        balloonPanel = balloonView.findViewById<FrameLayout>(R.id.oui_des_tip_popup_balloon_panel).apply { isVisible = false }
        balloonContent = balloonView.findViewById(R.id.oui_des_tip_popup_balloon_content)
        balloonBg1 = balloonView.findViewById(R.id.oui_des_tip_popup_balloon_bg_01)
        balloonBg2 = balloonView.findViewById(R.id.oui_des_tip_popup_balloon_bg_02)
        if (mode == Mode.TRANSLUCENT) {
            balloonBg1!!.setBackgroundResource(R.drawable.oui_des_tip_popup_balloon_bg_left_translucent)
            balloonBg1!!.backgroundTintList = null
            balloonBg2!!.setBackgroundResource(R.drawable.oui_des_tip_popup_balloon_bg_right_translucent)
            balloonBg2!!.backgroundTintList = null
        }

        balloonPopup = TipWindowBalloon(balloonView, balloonWidth, balloonHeight, true).apply {
            isFocusable = true
            isTouchable = true
            isOutsideTouchable = true
            if (Build.VERSION.SDK_INT >= 22) isAttachedInDecor = false
            setTouchInterceptor { _, event ->
                if (needToCallParentViewsOnClick && parentView.hasOnClickListeners()
                    && (event.action == ACTION_DOWN || event.action == ACTION_OUTSIDE)) {
                    val parentViewBounds = Rect()
                    val outLocation = IntArray(2)
                    parentView.getLocationOnScreen(outLocation)
                    parentViewBounds[outLocation[0], outLocation[1], outLocation[0] + parentView.width] =
                        outLocation[1] + parentView.height
                    val isTouchContainedInParentView =
                        parentViewBounds.contains(event.rawX.toInt(), event.rawY.toInt())
                    if (isTouchContainedInParentView) {
                        debugLog("callOnClick for parent view")
                        parentView.callOnClick()
                    }
                }
                false
            }
        }
    }

    fun show(direction: Direction) {
        setInternal()
        if (arrowPositionX == -1 || arrowPositionY == -1) {
            calculateArrowPosition()
        }
        if (direction == DEFAULT) {
            calculateArrowDirection(arrowPositionX, arrowPositionY)
        } else {
            arrowDirection = direction
        }
        calculatePopupSize()
        calculatePopupPosition()
        setBubblePanel()
        setBalloonPanel()
        showInternal()
    }

    fun setMessage(message: CharSequence?) {
        messageText = message
    }

    fun setAction(actionText: CharSequence?, listener: View.OnClickListener?) {
        this@TipPopup.actionText = actionText
        actionClickListener = listener
    }

    /**
     * Sets whether a click on the expanded pop-up will be passed to the
     * parent (anchor) view. Default is false.
     *
     * @param needToCall
     */
    fun semCallParentViewsOnClick(needToCall: Boolean) {
        needToCallParentViewsOnClick = needToCall
    }

    /**
     * Returns whether the pop-up, either as hint or expanded, is currently showing.
     */
    val isShowing: Boolean
        get() = bubblePopup?.isShowing == true || balloonPopup?.isShowing == true

    fun dismiss(withAnimation: Boolean) {
        val tipWindow = bubblePopup
        if (tipWindow != null) {
            tipWindow.setUseDismissAnimation(withAnimation)
            debugLog("mBubblePopup.mIsDismissing = " + bubblePopup!!.mIsDismissing)
            bubblePopup!!.dismiss()
        }
        val tipWindow2 = balloonPopup
        if (tipWindow2 != null) {
            tipWindow2.setUseDismissAnimation(withAnimation)
            debugLog("mBalloonPopup.mIsDismissing = " + balloonPopup!!.mIsDismissing)
            balloonPopup!!.dismiss()
        }
        val onDismissListener = onDismissListener
        onDismissListener?.onDismiss()
        val handler = mHandler
        if (handler != null) {
            handler.removeCallbacksAndMessages(null)
            mHandler = null
        }
    }

    fun setExpanded(expanded: Boolean) {
        if (expanded) {
            state = State.EXPANDED
            scaleMargin = 0
            return
        }
        scaleMargin = resources.getDimensionPixelSize(R.dimen.oui_des_tip_popup_scale_margin)
    }

    fun setTargetPosition(x: Int, y: Int) {
        if (x < 0 || y < 0) {
            return
        }
        isDefaultPosition = false
        arrowPositionX = x
        arrowPositionY = y
    }

    fun setHintDescription(hintDescription: CharSequence?) {
        this@TipPopup.hintDescription = hintDescription
    }

    /**
     * Updates the pop-up's size, position and arrow position if currently showing.
     *
     * @param direction (Optional) The new [Direction] of the arrow.
     * @param dismissOnTimeout (Optional) Dismisses the hint after
     * a [timeout][TIMEOUT_DURATION_MS]. Defaults to false.
     */
    @JvmOverloads
    fun update(direction: Direction = arrowDirection, dismissOnTimeout: Boolean = false) {
        if (!isShowing/* || mParentView == null*/) {
            return
        }
        setInternal()
        balloonX = -1
        balloonY = -1
        if (isDefaultPosition) {
            debugLog("update - default position")
            calculateArrowPosition()
        }
        if (direction == DEFAULT) {
            calculateArrowDirection(arrowPositionX, arrowPositionY)
        } else {
            arrowDirection = direction
        }
        calculatePopupSize()
        calculatePopupPosition()
        setBubblePanel()
        setBalloonPanel()

        if (state == State.HINT) {
            bubblePopup!!.update(bubblePopupX, bubblePopupY, bubblePopup!!.width, bubblePopup!!.height)
            if (dismissOnTimeout) {
                debugLog("Timer Reset!")
                scheduleTimeout()
            }
        } else if (state == State.EXPANDED) {
            balloonPopup!!.update(
                balloonPopupX,
                balloonPopupY,
                balloonPopup!!.width,
                balloonPopup!!.height
            )
        }
    }

    /**
     * Set the text color of the message.
     *
     * @param color The color to set. Note that the alpha channel
     * is ignored, and the color will be applied as fully opaque.
     */
    fun setMessageTextColor(@ColorInt color: Int) {
        messageTextColor = Color.BLACK or color
    }

    /**
     * Set the text color of the action button.
     *
     * @param color The color to set. Note that the alpha channel
     * is ignored, and the color will be applied as fully opaque.
     */
    fun setActionTextColor(@ColorInt color: Int) {
        actionTextColor = Color.BLACK or color
    }

    /**
     * Set the background color when on [Mode.NORMAL].
     *
     * @param color The color to set. Note that the alpha channel
     * is ignored, and the color will be applied as fully opaque.
     *
     * @see setBackgroundColorWithAlpha
     */
    fun setBackgroundColor(@ColorInt color: Int) {
        backgroundColor = Color.BLACK or color
    }

    /**
     * Set the background color when on [Mode.NORMAL].
     *
     * @param color The color to set including the alpha channel.
     */
    fun setBackgroundColorWithAlpha(@ColorInt  color: Int) {
        backgroundColor = color
    }

    /**
     * **A MISNOMER**. This should be better called `setIconTint`.
     *
     * Set the tint color of the icon for both [State.HINT] and [State.EXPANDED]
     * when on [Mode.NORMAL].
     *
     * @param color The color to set. Note that the alpha channel
     * is ignored, and the color will be applied as fully opaque.
     */
    fun setBorderColor(@ColorInt  color: Int) {
        borderColor = Color.BLACK or color
    }

    fun setOutsideTouchEnabled(enabled: Boolean) {
        bubblePopup!!.isFocusable = enabled
        bubblePopup!!.isOutsideTouchable = enabled
        balloonPopup!!.isFocusable = enabled
        balloonPopup!!.isOutsideTouchable = enabled
        debugLog("outside enabled : $enabled")
    }

    /**
     * Allows the popup window to extend beyond the bounds of the screen.
     *
     * @param enabled Set to false if the pop-up window should be
     * allowed to extend outside of the screen
     */
    fun setPopupWindowClippingEnabled(enabled: Boolean) {
        bubblePopup!!.isClippingEnabled = enabled
        balloonPopup!!.isClippingEnabled = enabled
        forceRealDisplay = !enabled
        sideMargin =
            if (enabled) resources.getDimensionPixelSize(R.dimen.oui_des_tip_popup_side_margin) else 0
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

        val currentFontScale = resources.configuration.fontScale
        val messageTextSize = resources.getDimensionPixelOffset(R.dimen.oui_des_tip_popup_balloon_message_text_size)
        val actionTextSize = resources.getDimensionPixelOffset(R.dimen.oui_des_tip_popup_balloon_action_text_size)

        if (currentFontScale > 1.2f) {
            messageView.setTextSize(TypedValue.COMPLEX_UNIT_PX, floor(ceil(messageTextSize / currentFontScale) * 1.2f))
            actionView.setTextSize(TypedValue.COMPLEX_UNIT_PX, floor(ceil(actionTextSize / currentFontScale) * 1.2f))
        }

        messageView.text = messageText

        if (TextUtils.isEmpty(actionText) || actionClickListener == null) {
            actionView.visibility = View.GONE
            actionView.setOnClickListener(null)
            type = Type.BALLOON_SIMPLE
        } else {
            actionView.visibility = View.VISIBLE
            actionView.semSetButtonShapeEnabled(true, backgroundColor)
            actionView.text = actionText
            actionView.setOnClickListener { view ->
                actionClickListener?.onClick(view)
                dismiss(true)
            }
            type = Type.BALLOON_ACTION
        }

        bubbleIcon?.contentDescription = hintDescription

        if (mode == Mode.TRANSLUCENT
            || bubbleIcon == null
            || bubbleBackground == null
            || balloonBubble == null
            || balloonBg1 == null
            || balloonBg2 == null) {
            return
        }

        messageTextColor?.let {
            messageView.setTextColor(it)
        }

        actionTextColor?.let {
            actionView.setTextColor(it)
        }

        bubbleBackground!!.setColorFilter(backgroundColor)
        balloonBubbleHint!!.setColorFilter(backgroundColor)

        balloonBg1!!.backgroundTintList = ColorStateList.valueOf(backgroundColor)
        balloonBg2!!.backgroundTintList = ColorStateList.valueOf(backgroundColor)

        borderColor?.let {
            bubbleIcon!!.setColorFilter(it)
            balloonBubbleIcon!!.setColorFilter(it)
        }

    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showInternal() {
        if (state != State.EXPANDED) {
            state = State.HINT
            onStateChangeListener?.onStateChanged(State.HINT)?.also {
                debugLog("mIsShowing : $isShowing")
            }

            bubblePopup?.showAtLocation(parentView, 0, bubblePopupX, bubblePopupY)?.also {
                animateViewIn()
            }

            bubbleView.setOnTouchListener { _, _ ->
                state = State.EXPANDED
                onStateChangeListener?.onStateChanged(state)
                balloonPopup?.showAtLocation(
                    parentView,
                    0,
                    balloonPopupX,
                    balloonPopupY
                )
                mHandler?.apply {
                    removeMessages(0)
                    sendMessageDelayed(Message.obtain(mHandler, 1), 10L)
                    sendMessageDelayed(Message.obtain(mHandler, 2), 20L)
                }
                false
            }
        } else {
            balloonBubble!!.visibility = View.GONE
            balloonPanel!!.visibility = View.VISIBLE
            messageView.visibility = View.VISIBLE
            onStateChangeListener?.onStateChanged(state)
            balloonPopup?.showAtLocation(parentView, 0, balloonPopupX, balloonPopupY)
            animateBalloonScaleUp()
        }
        balloonView.setOnTouchListener(object : OnTouchListener {
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                if (type == Type.BALLOON_SIMPLE) {
                    dismiss(true)
                    return false
                }
                return false
            }
        })
    }

    private fun setBubblePanel() {
        if (bubblePopup == null) {
            return
        }
        val paramBubblePanel = bubbleBackground!!.layoutParams as FrameLayout.LayoutParams
        if (mode == Mode.TRANSLUCENT) {
            paramBubblePanel.width =
                resources.getDimensionPixelSize(R.dimen.oui_des_tip_popup_bubble_width_translucent)
            paramBubblePanel.height =
                resources.getDimensionPixelSize(R.dimen.oui_des_tip_popup_bubble_height_translucent)
        }
        when (arrowDirection) {
            TOP_LEFT -> {
                val tipWindow = bubblePopup
                tipWindow!!.setPivot(tipWindow.width.toFloat(), bubblePopup!!.height.toFloat())
                paramBubblePanel.gravity = 85
                val i = bubbleX
                val i2 = scaleMargin
                bubblePopupX = i - (i2 * 2)
                bubblePopupY = bubbleY - (i2 * 2)
                if (mode == Mode.NORMAL) {
                    bubbleBackground!!.setImageResource(R.drawable.oui_des_tip_popup_hint_bg03)
                    if (isRTL && locale != "iw_IL") {
                        bubbleIcon!!.setImageResource(R.drawable.oui_des_tip_popup_hint_icon_rtl)
                    } else {
                        bubbleIcon!!.setImageResource(R.drawable.oui_des_tip_popup_hint_icon)
                    }
                } else {
                    bubbleBackground!!.rotationX = 180.0f
                }
            }

            TOP_RIGHT -> {
                val tipWindow2 = bubblePopup
                tipWindow2!!.setPivot(0.0f, tipWindow2.height.toFloat())
                paramBubblePanel.gravity = 83
                bubblePopupX = bubbleX
                bubblePopupY = bubbleY - (scaleMargin * 2)
                if (mode == Mode.NORMAL) {
                    bubbleBackground!!.setImageResource(R.drawable.oui_des_tip_popup_hint_bg04)
                    if (isRTL && locale != "iw_IL") {
                        bubbleIcon!!.setImageResource(R.drawable.oui_des_tip_popup_hint_icon_rtl)
                    } else {
                        bubbleIcon!!.setImageResource(R.drawable.oui_des_tip_popup_hint_icon)
                    }
                } else {
                    bubbleBackground!!.rotation = 180.0f
                }
            }

            BOTTOM_LEFT -> {
                val tipWindow3 = bubblePopup
                tipWindow3!!.setPivot(tipWindow3.width.toFloat(), 0.0f)
                paramBubblePanel.gravity = 53
                bubblePopupX = bubbleX - (scaleMargin * 2)
                bubblePopupY = bubbleY
                if (mode == Mode.NORMAL) {
                    bubbleBackground!!.setImageResource(R.drawable.oui_des_tip_popup_hint_bg01)
                    if (isRTL && locale != "iw_IL") {
                        bubbleIcon!!.setImageResource(R.drawable.oui_des_tip_popup_hint_icon_rtl)
                    } else {
                        bubbleIcon!!.setImageResource(R.drawable.oui_des_tip_popup_hint_icon)
                    }
                }
            }

            BOTTOM_RIGHT -> {
                bubblePopup!!.setPivot(0.0f, 0.0f)
                paramBubblePanel.gravity = 51
                bubblePopupX = bubbleX
                bubblePopupY = bubbleY
                if (mode == Mode.NORMAL) {
                    bubbleBackground!!.setImageResource(R.drawable.oui_des_tip_popup_hint_bg02)
                    if (isRTL && locale != "iw_IL") {
                        bubbleIcon!!.setImageResource(R.drawable.oui_des_tip_popup_hint_icon_rtl)
                    } else {
                        bubbleIcon!!.setImageResource(R.drawable.oui_des_tip_popup_hint_icon)
                    }
                } else {
                    bubbleBackground!!.rotationY = 180.0f
                }
            }
            DEFAULT -> {}
        }
        bubbleBackground!!.layoutParams = paramBubblePanel
        bubbleIcon!!.layoutParams = paramBubblePanel
        bubblePopup!!.width = bubbleWidth + (scaleMargin * 2)
        bubblePopup!!.height = bubbleHeight + (scaleMargin * 2)
    }

    private fun setBalloonPanel() {
        val scaleFactor: Int
        val f: Float
        val paramBalloonContent: FrameLayout.LayoutParams
        val f2: Float
        if (balloonPopup != null) {
            debugLog("setBalloonPanel()")
            val i = bubbleX
            val i2 = balloonX
            val leftMargin = i - i2
            val rightMargin = (i2 + balloonWidth) - i
            val i3 = bubbleY
            val i4 = balloonY
            val topMargin = i3 - i4
            val bottomMargin = (i4 + balloonHeight) - (i3 + bubbleHeight)
            val realMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(realMetrics)
            val scaleFactor2 = ceil(realMetrics.density.toDouble()).toInt()
            val minBackgroundWidth =
                resources.getDimensionPixelSize(R.dimen.oui_des_tip_popup_balloon_background_minwidth)
            debugLog("leftMargin[$leftMargin]")
            debugLog("rightMargin[$rightMargin] mBalloonWidth[$balloonWidth]")
            val horizontalContentMargin = horizontalTextMargin - resources.getDimensionPixelSize(R.dimen.oui_des_tip_popup_button_padding_horizontal)
            val verticalButtonPadding = if (actionView.isVisible) resources.getDimensionPixelSize(
                R.dimen.oui_des_tip_popup_button_padding_vertical) else 0
            val paramBalloonBubble = balloonBubble!!.layoutParams as FrameLayout.LayoutParams
            val paramBalloonPanel = balloonPanel!!.layoutParams as FrameLayout.LayoutParams
            val paramBalloonContent2 = balloonContent!!.layoutParams as FrameLayout.LayoutParams
            val paramBalloonBg1 = balloonBg1!!.layoutParams as FrameLayout.LayoutParams
            val paramBalloonBg2 = balloonBg2!!.layoutParams as FrameLayout.LayoutParams
            if (mode == Mode.TRANSLUCENT) {
                balloonBubbleHint!!.setImageResource(R.drawable.oui_des_tip_popup_hint_bg_translucent)
                balloonBubbleHint!!.imageTintList = null
                if (isRTL && locale != "iw_IL") {
                    balloonBubbleIcon!!.setImageResource(R.drawable.oui_des_tip_popup_hint_icon_translucent_rtl)
                } else {
                    balloonBubbleIcon!!.setImageResource(R.drawable.oui_des_tip_popup_hint_icon_translucent)
                }
                balloonBubbleIcon!!.imageTintList = null
                paramBalloonBubble.width =
                    resources.getDimensionPixelSize(R.dimen.oui_des_tip_popup_bubble_width_translucent)
                paramBalloonBubble.height =
                    resources.getDimensionPixelSize(R.dimen.oui_des_tip_popup_bubble_height_translucent)
                scaleFactor = 0
            } else if (Color.alpha(backgroundColor) < 255) {
                debugLog("Updating scaleFactor to 0 because transparency is applied to background.")
                scaleFactor = 0
            } else {
                scaleFactor = scaleFactor2
            }
            when (arrowDirection) {
                TOP_LEFT -> {
                    val tipWindow = balloonPopup
                    val i5 = arrowPositionX - balloonX
                    val i6 = scaleMargin
                    tipWindow!!.setPivot((i5 + i6).toFloat(), (balloonHeight + i6).toFloat())
                    if (mode == Mode.NORMAL) {
                        balloonBubbleHint!!.setImageResource(R.drawable.oui_des_tip_popup_hint_bg03)
                        balloonBubbleIcon!!.setImageResource(R.drawable.oui_des_tip_popup_hint_icon)
                        f = 180.0f
                    } else {
                        f = 180.0f
                        balloonBubbleHint!!.rotationX = 180.0f
                    }
                    balloonBg1!!.rotationX = f
                    balloonBg2!!.rotationX = f
                    paramBalloonBg2.gravity = 85
                    paramBalloonBg1.gravity = 85
                    paramBalloonBubble.gravity = 85
                    val i7 = bubbleWidth
                    if (rightMargin - i7 < minBackgroundWidth) {
                        val scaledLeftMargin = balloonWidth - minBackgroundWidth
                        paramBalloonBg1.setMargins(0, 0, minBackgroundWidth, 0)
                        paramBalloonBg2.setMargins(scaledLeftMargin - scaleFactor, 0, 0, 0)
                        debugLog("Right Margin is less then minimum background width!")
                        debugLog("updated !! leftMargin[$scaledLeftMargin],  rightMargin[$minBackgroundWidth]")
                    } else {
                        paramBalloonBg1.setMargins(0, 0, rightMargin - i7, 0)
                        paramBalloonBg2.setMargins(
                            (bubbleWidth + leftMargin) - scaleFactor,
                            0,
                            0,
                            0
                        )
                    }
                    val i8 = verticalTextMargin
                    paramBalloonContent = paramBalloonContent2
                    paramBalloonContent.setMargins(
                        horizontalContentMargin,
                        i8,
                        horizontalContentMargin,
                        (arrowHeight + i8) - verticalButtonPadding
                    )
                }

                TOP_RIGHT -> {
                    val tipWindow2 = balloonPopup
                    val i9 = arrowPositionX - balloonX
                    val i10 = scaleMargin
                    tipWindow2!!.setPivot((i9 + i10).toFloat(), (balloonHeight + i10).toFloat())
                    if (mode == Mode.NORMAL) {
                        balloonBubbleHint!!.setImageResource(R.drawable.oui_des_tip_popup_hint_bg04)
                        balloonBubbleIcon!!.setImageResource(R.drawable.oui_des_tip_popup_hint_icon)
                        f2 = 180.0f
                    } else {
                        f2 = 180.0f
                        balloonBubbleHint!!.rotation = 180.0f
                    }
                    balloonBg1!!.rotation = f2
                    balloonBg2!!.rotation = f2
                    paramBalloonBg2.gravity = 83
                    paramBalloonBg1.gravity = 83
                    paramBalloonBubble.gravity = 83
                    if (leftMargin < minBackgroundWidth) {
                        val scaledRightMargin = balloonWidth - minBackgroundWidth
                        paramBalloonBg1.setMargins(minBackgroundWidth, 0, 0, 0)
                        paramBalloonBg2.setMargins(0, 0, scaledRightMargin - scaleFactor, 0)
                        debugLog("Left Margin is less then minimum background width!")
                        debugLog("updated !! leftMargin[$minBackgroundWidth],  rightMargin[]")
                    } else {
                        paramBalloonBg1.setMargins(leftMargin, 0, 0, 0)
                        paramBalloonBg2.setMargins(0, 0, rightMargin - scaleFactor, 0)
                    }
                    val i11 = verticalTextMargin
                    paramBalloonContent = paramBalloonContent2
                    paramBalloonContent.setMargins(
                        horizontalContentMargin,
                        i11,
                        horizontalContentMargin,
                        (arrowHeight + i11) - verticalButtonPadding
                    )
                }

                BOTTOM_LEFT -> {
                    val tipWindow3 = balloonPopup
                    val i12 = arrowPositionX - balloonX
                    val i13 = scaleMargin
                    tipWindow3!!.setPivot((i12 + i13).toFloat(), i13.toFloat())
                    if (mode == Mode.NORMAL) {
                        balloonBubbleHint!!.setImageResource(R.drawable.oui_des_tip_popup_hint_bg01)
                        balloonBubbleIcon!!.setImageResource(R.drawable.oui_des_tip_popup_hint_icon)
                    }
                    paramBalloonBg2.gravity = 53
                    paramBalloonBg1.gravity = 53
                    paramBalloonBubble.gravity = 53
                    paramBalloonBg1.setMargins(0, 0, rightMargin - bubbleWidth, 0)
                    paramBalloonBg2.setMargins((bubbleWidth + leftMargin) - scaleFactor, 0, 0, 0)
                    val i14 = arrowHeight
                    val i15 = verticalTextMargin
                    paramBalloonContent2.setMargins(
                        horizontalContentMargin,
                        i14 + i15,
                        horizontalContentMargin,
                        i15 - verticalButtonPadding
                    )
                    paramBalloonContent = paramBalloonContent2
                }

                BOTTOM_RIGHT -> {
                    val tipWindow4 = balloonPopup
                    val i16 = arrowPositionX - balloonX
                    val i17 = scaleMargin
                    tipWindow4!!.setPivot((i16 + i17).toFloat(), i17.toFloat())
                    if (mode == Mode.NORMAL) {
                        balloonBubbleHint!!.setImageResource(R.drawable.oui_des_tip_popup_hint_bg02)
                        balloonBubbleIcon!!.setImageResource(R.drawable.oui_des_tip_popup_hint_icon)
                    } else {
                        balloonBubbleHint!!.rotationY = 180.0f
                    }
                    balloonBg1!!.rotationY = 180.0f
                    balloonBg2!!.rotationY = 180.0f
                    paramBalloonBg2.gravity = 51
                    paramBalloonBg1.gravity = 51
                    paramBalloonBubble.gravity = 51
                    paramBalloonBg1.setMargins(leftMargin, 0, 0, 0)
                    paramBalloonBg2.setMargins(0, 0, rightMargin - scaleFactor, 0)
                    val i18 = arrowHeight
                    val i19 = verticalTextMargin
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
            val i20 = scaleMargin
            paramBalloonBubble.setMargins(
                leftMargin + i20,
                topMargin + i20,
                (rightMargin - bubbleWidth) + i20,
                bottomMargin + i20
            )
            val balloonPanelMargin = scaleMargin
            paramBalloonPanel.setMargins(
                balloonPanelMargin,
                balloonPanelMargin,
                balloonPanelMargin,
                balloonPanelMargin
            )
            val i21 = balloonX
            val i22 = scaleMargin
            balloonPopupX = i21 - i22
            balloonPopupY = balloonY - i22
            balloonBubble!!.layoutParams = paramBalloonBubble
            balloonPanel!!.layoutParams = paramBalloonPanel
            balloonBg1!!.layoutParams = paramBalloonBg1
            balloonBg2!!.layoutParams = paramBalloonBg2
            balloonContent!!.layoutParams = paramBalloonContent
            balloonPopup!!.width = balloonWidth + (scaleMargin * 2)
            balloonPopup!!.height = balloonHeight + (scaleMargin * 2)
        }
    }

    private fun calculateArrowDirection(arrowX: Int, arrowY: Int) {
        if (isDefaultPosition) {
            val location = IntArray(2)
            parentView.getLocationInWindow(location)
            val parentY = location[1] + (parentView.height / 2)

            arrowDirection = if (arrowX * 2 <= displayMetrics.widthPixels) {
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
        } else if (arrowX * 2 <= displayMetrics.widthPixels && arrowY * 2 <= displayMetrics.heightPixels) {
            arrowDirection = BOTTOM_RIGHT
        } else if (arrowX * 2 > displayMetrics.widthPixels && arrowY * 2 <= displayMetrics.heightPixels) {
            arrowDirection = TOP_LEFT
        } else if (arrowX * 2 <= displayMetrics.widthPixels && arrowY * 2 > displayMetrics.heightPixels) {
            arrowDirection = TOP_RIGHT
        } else if (arrowX * 2 > displayMetrics.widthPixels && arrowY * 2 > displayMetrics.heightPixels) {
            arrowDirection = TOP_LEFT
        }
        debugLog("calculateArrowDirection : arrow position ($arrowX, $arrowY) / mArrowDirection = $arrowDirection")
    }

    private fun calculateArrowPosition() {
        val location = IntArray(2)
        parentView.getLocationInWindow(location)
        debugLog("calculateArrowPosition anchor location : " + location[0] + ", " + location[1])

        arrowPositionX = location[0] + (parentView.width / 2)
        val y = location[1] + (parentView.height / 2)
        arrowPositionY = if (y * 2 <= displayMetrics.heightPixels) {
            (parentView.height / 2) + y
        } else {
            y - (parentView.height / 2)
        }
        debugLog("calculateArrowPosition mArrowPosition : $arrowPositionX, $arrowPositionY")
    }

    private fun calculatePopupSize() {
        displayMetrics = resources.displayMetrics

        val balloonMaxWidth = if (DeviceLayoutUtil.isDeskTopMode(resources)) {
            val windowWidthInDexMode = parentView.rootView.run {
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
            val screenWidthDp = resources.configuration.screenWidthDp
            val screenWidthPixels = displayMetrics.widthPixels

            debugLog("screen width DP $screenWidthDp")
            when {
                screenWidthDp <= 480 -> (screenWidthPixels * 0.83f).toInt()
                screenWidthDp <= 960 -> (screenWidthPixels * 0.6f).toInt()
                screenWidthDp <= 1280 -> (screenWidthPixels * 0.45f).toInt()
                else -> (screenWidthPixels * 0.25f).toInt()
            }
        }

        if (!isMessageViewMeasured) {
            initialMessageViewWidth = messageView.run{
                measure(0, 0)
                measuredWidth
            }
            isMessageViewMeasured = true
        }

        val balloonMinWidth = arrowWidth + (horizontalTextMargin * 2)

        balloonWidth = (initialMessageViewWidth + (horizontalTextMargin * 2))
            .coerceIn(balloonMinWidth, balloonMaxWidth)

        balloonHeight = messageView.run {
            width = balloonWidth - (horizontalTextMargin * 2)
            measure(0, 0)
            measuredHeight + (verticalTextMargin * 2) + arrowHeight
        }

        if (type == Type.BALLOON_ACTION) {
            actionView.apply {
                measure(UNSPECIFIED, UNSPECIFIED)
                balloonWidth = balloonWidth.coerceAtLeast(
                    measuredWidth+ (resources.getDimensionPixelSize(R.dimen.oui_des_tip_popup_button_padding_horizontal) * 2))
                balloonHeight += (measuredHeight - resources.getDimensionPixelSize(R.dimen.oui_des_tip_popup_button_padding_vertical))
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun calculatePopupPosition() {
        getDisplayFrame(displayFrame)

        if (balloonX < 0) {
            balloonX = if (arrowDirection == BOTTOM_RIGHT || arrowDirection == TOP_RIGHT) {
                (arrowPositionX + arrowWidth) - (balloonWidth / 2)
            } else {
                (arrowPositionX - arrowWidth) - (balloonWidth / 2)
            }
        }

        arrowPositionX = if (arrowDirection == BOTTOM_RIGHT || arrowDirection == TOP_RIGHT) {
            arrowPositionX.coerceIn(
                displayFrame.left + sideMargin + horizontalTextMargin,
                (displayFrame.right - sideMargin) - horizontalTextMargin - arrowWidth
            )
        } else {
            arrowPositionX.coerceIn(
                displayFrame.left + sideMargin + horizontalTextMargin + arrowWidth,
                (displayFrame.right - sideMargin) - horizontalTextMargin
            )
        }

        balloonX = if (isDeskTopMode(context.resources)) {
            val windowLocation = IntArray(2)
            val windowWidthInDexMode = parentView.rootView.run {
                getLocationOnScreen(windowLocation)
                if (windowLocation[0] < 0) {
                    measuredWidth + windowLocation[0]
                }else {
                    measuredWidth
                }
            }
            balloonX.coerceIn(
                displayFrame.left + sideMargin,
                (windowWidthInDexMode - sideMargin) - balloonWidth - minOf(windowLocation[0], 0)
            )
        } else {
            balloonX.coerceIn(
                displayFrame.left + sideMargin,
                (displayFrame.right - sideMargin) - balloonWidth
            )
        }

        when (arrowDirection) {
            TOP_LEFT -> {
                bubbleX = arrowPositionX - bubbleWidth
                bubbleY = arrowPositionY - bubbleHeight
                balloonY = arrowPositionY - balloonHeight
            }

            TOP_RIGHT -> {
                bubbleX = arrowPositionX
                bubbleY = arrowPositionY - bubbleHeight
                balloonY = arrowPositionY - balloonHeight
            }

            BOTTOM_LEFT -> {
                bubbleX = arrowPositionX - bubbleWidth
                bubbleY = arrowPositionY
                balloonY = arrowPositionY
            }

            BOTTOM_RIGHT -> {
                bubbleX = arrowPositionX
                bubbleY = arrowPositionY
                balloonY = arrowPositionY
            }

            DEFAULT -> Unit
        }

        debugLog("QuestionPopup : $bubbleX, $bubbleY, $bubbleWidth, $bubbleHeight")
        debugLog("BalloonPopup : $balloonX, $balloonY, $balloonWidth, $balloonHeight")
    }

    private fun dismissBubble(withAnimation: Boolean) {
        bubblePopup?.apply {
            setUseDismissAnimation(withAnimation)
            dismiss()
        }
        onDismissListener?.onDismiss()
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
        when (arrowDirection) {
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
        bubbleView.startAnimation(animScale)
    }

    private fun animateBounce() {
        val pivotX: Float
        val pivotY: Float
        when (arrowDirection) {
            TOP_LEFT -> {
                pivotX = bubblePopup!!.width.toFloat()
                pivotY = bubblePopup!!.height.toFloat()
            }

            TOP_RIGHT -> {
                pivotX = 0.0f
                pivotY = bubblePopup!!.height.toFloat()
            }

            BOTTOM_LEFT -> {
                pivotX = bubblePopup!!.width.toFloat()
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
                    bubbleView.startAnimation(animationSet)
                }
            )
        }

        animationSet.addAnimation(scaleAnimation1)
        animationSet.addAnimation(scaleAnimation2)
        animationSet.startOffset = ANIMATION_OFFSET_BOUNCE_SCALE
        bubbleView.startAnimation(animationSet)
    }

    private fun animateScaleUp() {
        val deltaHintY: Float
        val pivotHintX: Float
        val pivotHintY: Float
        when (arrowDirection) {
            TOP_LEFT -> {
                pivotHintX = balloonBubble!!.width.toFloat()
                pivotHintY = balloonBubble!!.height.toFloat()
                deltaHintY = 0.0f - (arrowHeight / 2.0f)
            }

            TOP_RIGHT -> {
                pivotHintX = 0.0f
                pivotHintY = balloonBubble!!.height.toFloat()
                deltaHintY = 0.0f - (arrowHeight / 2.0f)
            }

            BOTTOM_LEFT -> {
                pivotHintX = balloonBubble!!.width.toFloat()
                pivotHintY = 0.0f
                deltaHintY = arrowHeight / 2.0f
            }

            BOTTOM_RIGHT -> {
                pivotHintX = 0.0f
                pivotHintY = 0.0f
                deltaHintY = arrowHeight / 2.0f
            }
            DEFAULT -> {
                pivotHintX = 0.0f
                pivotHintY = 0.0f
                deltaHintY = arrowHeight / 2.0f
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
                AlphaAnimation(1.0f, 0.0f).apply {
                    duration = ANIMATION_DURATION_EXPAND_TEXT
                    interpolator = INTERPOLATOR_SINE_IN_OUT_70
                }
            )
            it.setListener(
                onStart = {balloonPanel!!.isVisible = true},
                onEnd = {balloonBubble!!.isVisible = false}
            )
        }

        balloonBubble!!.startAnimation(animationBubble)
        animateBalloonScaleUp()
    }

    private fun animateBalloonScaleUp() {
        val pivotPanelX: Float
        val pivotPanelY: Float
        val questionHeight = resources.getDimensionPixelSize(R.dimen.oui_des_tip_popup_bubble_height)
        val panelScale = (questionHeight / balloonHeight).toFloat()

        when (arrowDirection) {
            TOP_RIGHT -> {
                pivotPanelX = (arrowPositionX - balloonX).toFloat()
                pivotPanelY = balloonHeight.toFloat()
            }

            BOTTOM_LEFT -> {
                pivotPanelX = (arrowPositionX - balloonX).toFloat()
                pivotPanelY = 0.0f
            }

            BOTTOM_RIGHT -> {
                pivotPanelX = (bubbleX - balloonX).toFloat()
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
        balloonPanel!!.startAnimation(animationPanel)

        val animationText: Animation = AlphaAnimation(0.0f, 1.0f).apply {
            interpolator = INTERPOLATOR_SINE_IN_OUT_33
            startOffset = ANIMATION_OFFSET_EXPAND_TEXT
            duration = ANIMATION_DURATION_EXPAND_TEXT
            setListener(
                onStart = { messageView.isVisible = true},
                onEnd = { dismissBubble(false) }
            )
        }
        messageView.startAnimation(animationText)
        actionView.startAnimation(animationText)
    }

    private val isNavigationbarHide: Boolean
        get() {
            val context: Context = context
            return Settings.Global.getInt(
                context.contentResolver,
                "navigationbar_hide_bar_enabled",
                0
            ) == 1
        }

    private val navagationbarHeight: Int get() = DeviceLayoutUtil.getNavigationBarHeight(resources)

    private val isTablet: Boolean get() = DeviceLayoutUtil.isTabletLayout(resources)

    private fun getDisplayFrame(screenRect: Rect) {
        //var displayCutout: DisplayCutout
        val navigationbarHeight = navagationbarHeight
        val navigationbarHide = isNavigationbarHide

        @Suppress("DEPRECATION")
        val displayRotation = windowManager.defaultDisplay.rotation

        val realMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(realMetrics)

        debugLog("realMetrics = $realMetrics")
        debugLog("is tablet? = $isTablet")

        if (forceRealDisplay) {
            screenRect.left = 0
            screenRect.top = 0
            screenRect.right = realMetrics.widthPixels
            screenRect.bottom = realMetrics.heightPixels
            debugLog("Screen Rect = $screenRect mForceRealDisplay = $forceRealDisplay")
            return
        }
        screenRect.left = 0
        screenRect.top = 0
        screenRect.right = displayMetrics.widthPixels
        screenRect.bottom = displayMetrics.heightPixels

        val bounds = Rect()
        parentView.rootView!!.getWindowVisibleDisplayFrame(bounds)
        debugLog("Bounds = $bounds")
        if (isTablet) {
            debugLog("tablet")
            if ((realMetrics.widthPixels == displayMetrics.widthPixels)
                && (realMetrics.heightPixels - displayMetrics.heightPixels == navigationbarHeight)
                && navigationbarHide) {
                screenRect.bottom += navigationbarHeight
            }
        } else {
            debugLog("phone")
            when (displayRotation) {
                Surface.ROTATION_0 -> {
                    if (realMetrics.widthPixels == displayMetrics.widthPixels
                        && realMetrics.heightPixels - displayMetrics.heightPixels == navigationbarHeight
                        && navigationbarHide
                    ) {
                        screenRect.bottom += navigationbarHeight
                    }
                }

                Surface.ROTATION_90 -> {
                    if (realMetrics.heightPixels == displayMetrics.heightPixels
                        && realMetrics.widthPixels - displayMetrics.widthPixels == navigationbarHeight
                        && navigationbarHide
                    ) {
                        screenRect.right += navigationbarHeight
                    }
                    val windowInsets = ViewCompat.getRootWindowInsets(parentView)
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
                    if (realMetrics.widthPixels == displayMetrics.widthPixels
                        && realMetrics.heightPixels - displayMetrics.heightPixels == navigationbarHeight
                    ) {
                        if (navigationbarHide) {
                            screenRect.bottom += navigationbarHeight
                        } else {
                            screenRect.top += navigationbarHeight
                            screenRect.bottom += navigationbarHeight
                        }
                    } else if (realMetrics.widthPixels == displayMetrics.widthPixels && bounds.top == navigationbarHeight) {
                        debugLog("Top Docked")
                        screenRect.top += navigationbarHeight
                        screenRect.bottom += navigationbarHeight
                    }
                }

                Surface.ROTATION_270 -> {
                    if (realMetrics.heightPixels == displayMetrics.heightPixels
                        && realMetrics.widthPixels - displayMetrics.widthPixels == navigationbarHeight
                    ) {
                        if (navigationbarHide) {
                            screenRect.right += navigationbarHeight
                        } else {
                            screenRect.left += navigationbarHeight
                            screenRect.right += navigationbarHeight
                        }
                    } else if (realMetrics.heightPixels == displayMetrics.heightPixels && bounds.left == navigationbarHeight) {
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
        this@TipPopup.onDismissListener = onDismissListener
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
            if (Build.VERSION.SDK_INT > 22 && mIsUsingDismissAnimation && !mIsDismissing) {
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
            val messageView = contentView.findViewById<View>(R.id.oui_des_tip_popup_message)

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
        get() = context.resources.configuration.layoutDirection == 1

    private val locale: String
        get() = ConfigurationCompat.getLocales(context.resources.configuration).get(0).toString()

    private fun debugLog(msg: String) {
        Log.d(TAG, " #### $msg")
    }

    fun semGetBubblePopupWindow(): PopupWindow? {
        return bubblePopup
    }

    fun semGetBalloonPopupWindow(): PopupWindow? {
        return balloonPopup
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

    }
}

/**
* Set the tint color of the icon for both [TipPopup.State.HINT] and [TipPopup.State.EXPANDED]
* when on [TipPopup.Mode.NORMAL].
*
* @param color The color to set. Note that the alpha channel
* is ignored, and the color will be applied as fully opaque.
*/
inline fun TipPopup.setIconTint(@ColorInt color: Int){
    setBorderColor(color)
}
