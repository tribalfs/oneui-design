package dev.oneuiproject.oneui.preference;

import static androidx.appcompat.widget.SeslAbsSeekBar.NO_OVERLAP;
import static androidx.appcompat.widget.SeslProgressBar.MODE_LEVEL_BAR;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.util.SeslMisc;
import androidx.appcompat.widget.SeslSeekBar;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.content.res.TypedArrayUtils;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SeekBarPreference;
import androidx.reflect.view.SeslHapticFeedbackConstantsReflector;

import java.lang.ref.WeakReference;

import dev.oneuiproject.oneui.design.R;
import dev.oneuiproject.oneui.widget.SeekBarPlus;

public class SeekBarPreferencePro extends SeekBarPreference implements View.OnClickListener, OnLongClickListener, OnTouchListener {

    @SuppressLint("RestrictedApi")
    private final int HAPTIC_CONSTANT_CURSOR_MOVE = SeslHapticFeedbackConstantsReflector.semGetVibrationIndex(41);
    private final Handler longPressHandler = new LongPressHandler(this);

    private int mSeekbarMode = MODE_LEVEL_BAR;
    private boolean mSeamLess = false;
    private SeekBarPlus mSeekBar;
    private boolean centerBasedSeekBar = false;
    private CharSequence leftLabel;
    private CharSequence rightLabel;
    private CharSequence stateDescription;
    private boolean showTickMarks = false;
    private ColorStateList progressTintList;
    private ColorStateList tickMarkTintList;
    private int mOverlapPoint = NO_OVERLAP;
    private boolean mIsLongKeyProcessing = false;
    private TextView mSeekBarValueTextView;
    private String mUnits;
    private ImageView mAddButton;
    private ImageView mDeleteButton;
    private OnSeekBarPreferenceChangeListener mOnSeekBarPreferenceChangeListener;

    @SuppressLint("RestrictedApi")
    public SeekBarPreferencePro(Context context, AttributeSet attrs) {
        super(context, attrs, TypedArrayUtils.getAttr(
                context, R.attr.seekBarPreferenceProStyle,
                androidx.preference.R.attr.seekBarPreferenceStyle
        ), 0);
        init(context, attrs);
    }

    @SuppressLint("RestrictedApi")
    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SeekBarPreferencePro);
            centerBasedSeekBar = a.getBoolean(R.styleable.SeekBarPreferencePro_centerBasedSeekBar, false);
            leftLabel = a.getString(R.styleable.SeekBarPreferencePro_leftLabelName);
            mOverlapPoint = a.getInt(R.styleable.SeekBarPreferencePro_overlapPoint, NO_OVERLAP);
            rightLabel = a.getString(R.styleable.SeekBarPreferencePro_rightLabelName);
            mSeamLess = a.getBoolean(R.styleable.SeekBarPreferencePro_seamlessSeekBar, false);
            mSeekbarMode = a.getInt(R.styleable.SeekBarPreferencePro_seekBarMode, MODE_LEVEL_BAR);
            showTickMarks = a.getBoolean(R.styleable.SeekBarPreferencePro_showTickMark, true);
            mUnits = a.getString(R.styleable.SeekBarPreferencePro_units);
            a.recycle();
        }
        setOnSeekBarPreferenceChangeListener(null);
    }

    @Override
    public void setOnSeekBarPreferenceChangeListener(SeekBarPreference.OnSeekBarPreferenceChangeListener onSeekBarPreferenceChangeListener) {
        mOnSeekBarPreferenceChangeListener = new SeekBarPreference.OnSeekBarPreferenceChangeListener() {

            @Override
            public void onProgressChanged(SeslSeekBar seekBar, int progress, boolean fromUser) {
                if (onSeekBarPreferenceChangeListener != null) {
                    onSeekBarPreferenceChangeListener.onProgressChanged(seekBar, progress, fromUser);
                }
                if (getShowSeekBarValue()) updateValueLabel(progress);
            }

            @Override
            public void onStartTrackingTouch(SeslSeekBar seekBar) {
                if (onSeekBarPreferenceChangeListener != null) {
                    onSeekBarPreferenceChangeListener.onStartTrackingTouch(seekBar);
                }
            }

            @Override
            public void onStopTrackingTouch(SeslSeekBar seekBar) {
                if (onSeekBarPreferenceChangeListener != null) {
                    onSeekBarPreferenceChangeListener.onStopTrackingTouch(seekBar);
                }
                if (getShowSeekBarValue()) updateValueLabel(seekBar.getProgress());

            }
        };
        super.setOnSeekBarPreferenceChangeListener(mOnSeekBarPreferenceChangeListener);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        mSeekBar = (SeekBarPlus) holder.findViewById(R.id.seekbar);
        if (mSeekBar != null) {
            mSeekBar.setSeamless(mSeamLess);
            if (!centerBasedSeekBar) {
                mSeekBar.setCenterBasedBar(false);
                mSeekBar.setMode(mSeekbarMode);
                if (mOverlapPoint != NO_OVERLAP) {
                    if (mOverlapPoint > getMin() && mOverlapPoint < getMax()) {
                        mSeekBar.setOverlapPointForDualColor(mOverlapPoint - getMin());
                        setShowOverlapPreview(true);
                    } else {
                        Log.e(TAG, "overlapPoint must be at least 1 and less than max");
                    }
                }
                setShowTickMarks(showTickMarks);
            }
        }

        mSeekBarValueTextView = (TextView) holder.findViewById(R.id.seekbar_value);
        if (mSeekBarValueTextView != null) {
            if (getShowSeekBarValue()) {
                mSeekBarValueTextView.setVisibility(View.VISIBLE);
                updateValueLabel(mSeekBar.getProgress());
            }
        }

        if (leftLabel != null || rightLabel != null) {
            LinearLayout seekbarLabelArea = (LinearLayout) holder.findViewById(R.id.seekbar_label_area);
            if (seekbarLabelArea != null) {
                seekbarLabelArea.setVisibility(View.VISIBLE);
            }
            TextView leftLabelTextView = (TextView) holder.findViewById(R.id.left_label);
            if (leftLabelTextView != null) {
                leftLabelTextView.setText(leftLabel);
            }
            TextView rightLabelTextView = (TextView) holder.findViewById(R.id.right_label);
            if (rightLabelTextView != null) {
                rightLabelTextView.setText(rightLabel);
            }
        }

        mAddButton = (ImageView) holder.findViewById(R.id.add_button);
        if (mAddButton != null) {
            if (isAdjustable()) {
                mAddButton.setVisibility(View.VISIBLE);
                mAddButton.setOnClickListener(this);
                mAddButton.setOnLongClickListener(this);
                mAddButton.setOnTouchListener(this);
                mAddButton.setEnabled(isEnabled());
                mAddButton.setAlpha(isEnabled() ? 1.0f : 0.4f);
            } else {
                mAddButton.setVisibility(View.GONE);
            }
        }

        mDeleteButton = (ImageView) holder.findViewById(R.id.delete_button);
        if (mDeleteButton != null) {
            if (isAdjustable()) {
                mDeleteButton.setVisibility(View.VISIBLE);
                mDeleteButton.setOnClickListener(this);
                mDeleteButton.setOnLongClickListener(this);
                mDeleteButton.setOnTouchListener(this);
                mDeleteButton.setEnabled(isEnabled());
                mDeleteButton.setAlpha(isEnabled() ? 1.0f : 0.4f);
            } else {
                mDeleteButton.setVisibility(View.GONE);
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private void setShowOverlapPreview(Boolean show) {

        int mOverlapNormalProgressColor;
        if ( SeslMisc.isLightTheme(getContext())) {
            mOverlapNormalProgressColor = androidx.appcompat.R.color.sesl_seekbar_overlap_color_default_light;
        } else {
            mOverlapNormalProgressColor = androidx.appcompat.R.color.sesl_seekbar_overlap_color_default_dark;
        }

        int overlapInactiveColor;
        if (show){
            overlapInactiveColor = R.color.oui_seekbar_legacy_overlap_color_default;
        } else{
            overlapInactiveColor = mOverlapNormalProgressColor;
        }
        int bgColor = ContextCompat.getColor(getContext(), overlapInactiveColor);


        int mOverlapActivatedProgressColorActivated;
       if ( SeslMisc.isLightTheme(getContext())) {
           mOverlapActivatedProgressColorActivated = androidx.appcompat.R.color.sesl_seekbar_overlap_color_activated_dark;
        } else {
           mOverlapActivatedProgressColorActivated = androidx.appcompat.R.color.sesl_seekbar_overlap_color_activated_light;
       }
        int fgColor = ContextCompat.getColor(getContext(), mOverlapActivatedProgressColorActivated);

        mSeekBar.setDualModeOverlapColor(bgColor, fgColor);
    }

    private void setShowTickMarks(Boolean show) {
        if (show) {
            mSeekBar.setTickMark(ResourcesCompat.getDrawable(getContext().getResources(),
                  androidx.appcompat.R.drawable.sesl_level_seekbar_tick_mark, getContext().getTheme()));
        }else mSeekBar.setTickMark(null);
    }

    private void updateValueLabel(int progress) {
        if (mSeekBarValueTextView != null) {
            int value = progress + getMin();
            String valueStr = value + (mUnits != null ? mUnits : "");
            mSeekBarValueTextView.setText(valueStr);
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.delete_button) {
            onDeleteButtonClicked();
        } else if (id == R.id.add_button) {
            onAddButtonClicked();
        }
        view.announceForAccessibility(stateDescription);
    }

    @Override
    public boolean onLongClick(View view) {
        mIsLongKeyProcessing = true;
        int id = view.getId();
        if (id == R.id.delete_button || id == R.id.add_button) {
            new Thread(() -> {
                while (mIsLongKeyProcessing) {
                    longPressHandler.sendEmptyMessage(id == R.id.delete_button ? MSG_DELETE : MSG_ADD);
                    try {
                        Thread.sleep(300L);
                    } catch (InterruptedException e) {
                        Log.w(TAG, "InterruptedException!", e);
                    }
                }
            }).start();
            return false;
        }
        return false;
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            mIsLongKeyProcessing = false;
            longPressHandler.removeMessages(MSG_DELETE);
            longPressHandler.removeMessages(MSG_ADD);
        }
        return false;
    }

    private void onDeleteButtonClicked() {
        int value = getValue() - getSeekBarIncrement();
        if (value < getMin() || !callChangeListener(value)) {
            return;
        }
        mSeekBar.performHapticFeedback(HAPTIC_CONSTANT_CURSOR_MOVE);
        setValue(value);
    }

    private void onAddButtonClicked() {
        int value = getValue() + getSeekBarIncrement();
        if (value > getMax() || !callChangeListener(value)) {
            return;
        }
        mSeekBar.performHapticFeedback(HAPTIC_CONSTANT_CURSOR_MOVE);
        setValue(value);
    }

    private static class LongPressHandler extends Handler {
        private final WeakReference<SeekBarPreferencePro> weakReference;

        public LongPressHandler(SeekBarPreferencePro seekBarPref) {
            super(Looper.getMainLooper());
            weakReference = new WeakReference<>(seekBarPref);
        }

        @Override
        public void handleMessage(Message message) {
            SeekBarPreferencePro seekBarPreference = weakReference.get();
            if (seekBarPreference != null) {
                switch (message.what) {
                    case MSG_DELETE:
                        seekBarPreference.onDeleteButtonClicked();
                        break;
                    case MSG_ADD:
                        seekBarPreference.onAddButtonClicked();
                        break;
                }
            }
        }
    }

    private static final String TAG = "SeekBarPreferencePro";
    private static final int MSG_DELETE = 1;
    private static final int MSG_ADD = 2;
}
