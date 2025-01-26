package dev.oneuiproject.oneuiexample.ui.main.core.drawer;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import static java.lang.Math.max;
import static dev.oneuiproject.oneui.ktx.FloatKt.dpToPx;

import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;

import com.sec.sesl.tester.R;

import dev.oneuiproject.oneui.utils.TypefaceUtilsKt;


public class DrawerListViewHolder extends RecyclerView.ViewHolder {
    private final boolean mIsSeparator;
    private Typeface mNormalTypeface;
    private Typeface mSelectedTypeface;

    private AppCompatImageView mIconView;
    private TextView mTitleView;

    public DrawerListViewHolder(@NonNull View itemView, boolean isSeparator) {
        super(itemView);
        mIsSeparator = isSeparator;
        if (!mIsSeparator) {
            mIconView = itemView.findViewById(R.id.drawer_item_icon);
            mTitleView = itemView.findViewById(R.id.drawer_item_title);
            mNormalTypeface = TypefaceUtilsKt.getRegularFont();
            mSelectedTypeface = TypefaceUtilsKt.getSemiBoldFont();
        }
    }

    public boolean isSeparator() {
        return mIsSeparator;
    }

    public void setIcon(@DrawableRes int resId) {
        if (!mIsSeparator) {
            mIconView.setImageResource(resId);
        }
    }

    public void setTitle(@Nullable CharSequence title) {
        if (!mIsSeparator) {
            mTitleView.setText(title);
        }
    }

    public void setSelected(boolean selected) {
        if (!mIsSeparator) {
            itemView.setSelected(selected);
            mTitleView.setTypeface(selected ? mSelectedTypeface : mNormalTypeface);
            mTitleView.setEllipsize(selected ?
                    TextUtils.TruncateAt.MARQUEE : TextUtils.TruncateAt.END);
        }
    }

    private float currentOffset = -1f;

    private final Runnable resetItemWidthRunnable = () -> {
        ViewGroup.LayoutParams lp = itemView.getLayoutParams();
        if (lp.width == MATCH_PARENT) return;
        lp.width = MATCH_PARENT;
        itemView.setLayoutParams(lp);
    };

    public void applyOffset(Float offset){
        if (currentOffset == offset) return;
        currentOffset = offset;

        if (mIsSeparator) {
            itemView.setAlpha(max(offset, 1/4f));
        }else{
            mTitleView.setAlpha(offset);
            itemView.getBackground().setAlpha(max((int) (255 * offset), 255/4));
        }

        if (offset == 0f) {
            if (mIsSeparator) {
                itemView.setAlpha(1f);
            }else{
                itemView.getBackground().setAlpha(255);
            }
            ViewGroup.LayoutParams lp = itemView.getLayoutParams();
            lp.width = dpToPx(!mIsSeparator ? 46f : 27f, itemView.getResources());
            itemView.setLayoutParams(lp);
        } else {
            itemView.post(resetItemWidthRunnable);
        }
    }
}
