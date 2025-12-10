package dev.oneuiproject.oneui.layout

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.withStyledAttributes
import dev.oneuiproject.oneui.design.R

/**
 * A custom splash screen layout, like in some Apps from Samsung. This can either have a static icon or an animation.
 *
 * - To use an animated icon:
 *      - set the `app:animated` attribute to `true`
 *      - set `app:foreground_image` to a reference of a drawable resource that will be used as the foreground image of the animated icon
 *      - set `app:background_image` to reference of a drawable resource that will be used as the background image of the animated icon
 *      - optionally, set `app:animation` to reference of a anim resource that will be used to animate the icon. If not provided,
 *      it will use the default animation provided in the library.
 *
 * Examples:
 * ```
 * <dev.oneuiproject.oneui.layout.SplashLayout
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent"
 *     app:animated="true"
 *     app:foreground_image="@drawable/your_foreground_drawable"
 *     app:background_image="@drawable/your_background_drawable"/>
 * ```
 * ```
 * <dev.oneuiproject.oneui.layout.SplashLayout
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent"
 *     app:animated="false"
 *     app:image="@drawable/your_drawable"/>
 * ```
 */
class SplashLayout(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private var animated = false
    private var imageForeground: Drawable? = null
    private var imageBackground: Drawable? = null
    private var title: String? = null
    private var splashAnim: Animation? = null
    private val textView: TextView
    private var imageview: ImageView? = null
    private var imageviewForeground: ImageView? = null
    private var imageviewBackground: ImageView? = null
    private var image: Drawable? = null

    companion object {
        private const val TAG = "SplashLayout"
    }

    init {
        context.withStyledAttributes(attrs, R.styleable.SplashLayout, 0, 0) {
            animated = getBoolean(R.styleable.SplashLayout_animated, true)
            title = getString(R.styleable.SplashLayout_title)
                ?: context.packageManager.getApplicationLabel(context.applicationInfo).toString()

            if (animated) {
                imageForeground = getDrawable(R.styleable.SplashLayout_foreground_image)
                imageBackground = getDrawable(R.styleable.SplashLayout_background_image)
                splashAnim = AnimationUtils.loadAnimation(
                    context,
                    getResourceId(
                        R.styleable.SplashLayout_animation,
                        R.anim.oui_des_splash_animation
                    )
                )
            } else {
                image = getDrawable(R.styleable.SplashLayout_image)
            }
        }


        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(
            if (animated) R.layout.oui_des_layout_splash_animated else R.layout.oui_des_layout_splash_simple,
            this,
            true
        )


        textView = findViewById<TextView>(R.id.oui_des_splash_text)
        textView.setText(title)

        if (animated) {
            imageviewForeground =
                findViewById<ImageView>(R.id.oui_des_splash_image_foreground).apply {
                    setImageDrawable(imageForeground)
                }
            imageviewBackground =
                findViewById<ImageView>(R.id.oui_des_splash_image_background).apply {
                    setImageDrawable(imageBackground)
                }
        } else {
            imageview = findViewById<ImageView>(R.id.oui_des_splash_image).apply {
                setImageDrawable(image)
            }
        }
    }

    /** Set the animation listener. This Only applies when `R.attr.animated` is set to `true`. */
    fun setSplashAnimationListener(listener: Animation.AnimationListener?) {
        if (animated) {
            splashAnim!!.setAnimationListener(listener)
        } else {
            Log.w(TAG, "Call to setSplashAnimationListener() `R.attr.animated` set to false will be ignored.")
        }
    }

    /** Start the animation. This Only applies when `R.attr.animated` is set to `true`. */
    fun startSplashAnimation() {
        if (animated) {
            imageviewForeground!!.startAnimation(splashAnim)
        } else {
            Log.w(TAG, "Call to startSplashAnimation() `R.attr.animated` set to false will be ignored.")
        }
    }

    /** Stop the animation.*/
    fun clearSplashAnimation() {
        if (animated) {
            imageviewForeground!!.clearAnimation()
        } else {
            Log.w(TAG, "Call to clearSplashAnimation() `R.attr.animated` set to false will be ignored.")
        }
    }

    /** The title text to show in the splash screen. The default will be your App's name. */
    var text: String?
        get() = title
        set(value) {
            this.title = value
            textView.setText(value)
        }

    /** Set the foreground and background layers for the animated splash screen. */
    fun setImage(foreground: Drawable, background: Drawable) {
        if (animated) {
            this.imageForeground = foreground
            this.imageBackground = background
            imageviewForeground!!.setImageDrawable(foreground)
            imageviewBackground!!.setImageDrawable(background)
        } else {
            Log.w(TAG, "Should call setImage(foreground: Drawable) when `R.attr.animated` is set to false")
        }
    }

    /** Set the image for the static splash screen. */
    fun setImage(image: Drawable) {
        if (!animated) {
            this.image = image
            imageview!!.setImageDrawable(image)
        } else {
            Log.w(
                TAG,
                "Should call setImage(foreground: Drawable, background: Drawable) when `R.attr.animated` is set to true"
            )
        }
    }
}
