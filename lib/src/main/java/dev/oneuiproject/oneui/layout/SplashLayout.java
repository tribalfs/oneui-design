package dev.oneuiproject.oneui.layout;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import dev.oneuiproject.oneui.design.R;

/**
 * A custom splash screen layout, like in some Apps from Samsung. This can either have a static icon or an animation.
 */
public class SplashLayout extends LinearLayout {

    private final boolean animated;
    private Drawable imageForeground;
    private Drawable imageBackground;
    private String title;
    private Animation splashAnim;
    private final TextView textView;
    private ImageView imageview;
    private ImageView imageviewForeground;
    private ImageView imageviewBackground;
    private Drawable image;

    public SplashLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray attr = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SplashLayout, 0, 0);

        try {
            animated = attr.getBoolean(R.styleable.SplashLayout_animated, true);
            title = attr.getString(R.styleable.SplashLayout_title);
            if (title == null) title = context.getString(R.string.app_name);

            if (animated) {
                imageForeground = attr.getDrawable(R.styleable.SplashLayout_foreground_image);
                imageBackground = attr.getDrawable(R.styleable.SplashLayout_background_image);
                splashAnim = AnimationUtils.loadAnimation(context, attr.getResourceId(R.styleable.SplashLayout_animation, R.anim.oui_des_splash_animation));
            } else {
                image = attr.getDrawable(R.styleable.SplashLayout_image);
            }

        } finally {
            attr.recycle();
        }

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(animated ? R.layout.oui_des_layout_splash_animated : R.layout.oui_des_layout_splash_simple, this, true);


        textView = findViewById(R.id.oui_des_splash_text);
        textView.setText(title);

        if (animated) {
            imageviewForeground = findViewById(R.id.oui_des_splash_image_foreground);
            imageviewBackground = findViewById(R.id.oui_des_splash_image_background);

            imageviewForeground.setImageDrawable(imageForeground);
            imageviewBackground.setImageDrawable(imageBackground);
        } else {
            imageview = findViewById(R.id.oui_des_splash_image);
            imageview.setImageDrawable(image);
        }


    }

    /**
     * Set the animation listener.
     */
    public void setSplashAnimationListener(Animation.AnimationListener listener) {
        if (animated) splashAnim.setAnimationListener(listener);
    }

    /**
     * Start the animation.
     */
    public void startSplashAnimation() {
        if (animated) imageviewForeground.startAnimation(splashAnim);
    }

    /**
     * Stop the animation.
     */
    public void clearSplashAnimation() {
        if (animated) imageviewForeground.clearAnimation();
    }

    public String getText() {
        return title;
    }

    /**
     * Set a custom text. The default will be your App's name.
     */
    public void setText(String mText) {
        this.title = mText;
        textView.setText(mText);
    }

    /**
     * Set the foreground and background layers for the animated splash screen.
     */
    public void setImage(Drawable foreground, Drawable background) {
        if (animated) {
            this.imageForeground = foreground;
            this.imageBackground = background;
            imageviewForeground.setImageDrawable(foreground);
            imageviewBackground.setImageDrawable(background);
        }
    }

    /**
     * Set the image for the static splash screen.
     */
    public void setImage(Drawable image) {
        if (!animated) {
            this.image = image;
            imageview.setImageDrawable(image);
        }
    }
}
