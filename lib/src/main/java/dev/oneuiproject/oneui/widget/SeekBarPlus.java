package dev.oneuiproject.oneui.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import androidx.appcompat.widget.SeslSeekBar;

import dev.oneuiproject.oneui.design.R;

public class SeekBarPlus extends SeslSeekBar {

    /**
     * If true, this sets [SeslSeekBar] to only put tick marks at the start, middle and end
     * regardless of the min and max value.
     * This will ignore [setMode] value and will use [MODE_LEVEL_BAR][SeslSeekBar.MODE_LEVEL_BAR] mode.
     */
    private boolean centerBasedBar = true;

    public SeekBarPlus(Context context) {
        super(context);
        init(context, null);
    }

    public SeekBarPlus(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SeekBarPlus(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            // Retrieve attributes
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SeekBarPlus);
            setSeamless(a.getBoolean(R.styleable.SeekBarPlus_seamless, true));
            centerBasedBar = a.getBoolean(R.styleable.SeekBarPlus_centerBasedBar, true);
            if (centerBasedBar) {
                setMode(MODE_LEVEL_BAR);
            }
            a.recycle();
        }
        setProgressTintMode(PorterDuff.Mode.SRC);
    }

    public void setCenterBasedBar(boolean centerBasedBar) {
        if (this.centerBasedBar != centerBasedBar) {
            this.centerBasedBar = centerBasedBar;
            if (centerBasedBar) {
                setMode(MODE_LEVEL_BAR);
            }
            invalidate();
        }
    }

    public boolean isCenterBasedBar() {
        return centerBasedBar;
    }

    @SuppressLint("RestrictedApi")
    @Override
    public boolean performHapticFeedback(int feedbackConstant) {
        int progress = getProgress();
        if (centerBasedBar) {
            if ((float) progress != (getMin() + getMax()) / 2f && progress != getMin() && progress != getMax()) {
                return false;
            }
        }
        return super.performHapticFeedback(feedbackConstant);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void drawTickMarks(Canvas canvas) {
        if (centerBasedBar) {
            Drawable tickMark = getTickMark();
            if (tickMark != null) {
                int tickWidth = tickMark.getIntrinsicWidth();
                int tickHeight = tickMark.getIntrinsicHeight();
                int right = tickWidth >= 0 ? tickWidth / 2 : 1;
                int bottom = tickHeight >= 0 ? tickHeight / 2 : 1;
                tickMark.setBounds(-right, -bottom, right, bottom);
                float width = (getWidth() - getPaddingLeft() - getPaddingRight() - tickWidth * 2) / 2.0f;
                int save = canvas.save();
                canvas.translate((getPaddingLeft() + tickWidth), getHeight() / 2.0f);
                for (int i = 0; i <= 2; i++) {
                    tickMark.draw(canvas);
                    canvas.translate(width, 0.0f);
                }
                canvas.restoreToCount(save);
            }
        } else {
            super.drawTickMarks(canvas);
        }
    }

    @SuppressLint("RestrictedApi")
    public int getCurrentMode() {
        return mCurrentMode;
    }
}
