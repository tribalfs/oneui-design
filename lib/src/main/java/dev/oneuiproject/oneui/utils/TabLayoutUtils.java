package dev.oneuiproject.oneui.utils;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.tabs.TabLayout;

import dev.oneuiproject.oneui.ktx.TabLayoutKt;

@Deprecated
public class TabLayoutUtils {

    public interface TabButtonClickListener {
        void onClick(View v);
    }

    /**
     * @deprecated Check out {@link dev.oneuiproject.oneui.ktx.TabLayoutKt#addCustomTab(TabLayout, Integer, Integer, View.OnClickListener) TabLayoutKt.addCustomTab}.
     */
    @Deprecated()
    public static void addCustomButton(@NonNull TabLayout tabLayout, @DrawableRes int resId,
                                       @Nullable TabButtonClickListener listener) {
        TabLayoutKt.addCustomTab(tabLayout, null, resId, v -> listener.onClick(v));
    }


    /**
     * @deprecated Check out {@link dev.oneuiproject.oneui.ktx.TabLayoutKt#addCustomTab(TabLayout, CharSequence, Drawable, View.OnClickListener)} TabLayoutKt.addCustomTab}.
     */
    @Deprecated()
    public static void addCustomButton(@NonNull TabLayout tabLayout, @Nullable Drawable icon,
                                @Nullable TabButtonClickListener listener) {
        TabLayoutKt.addCustomTab(tabLayout, null, icon, v -> listener.onClick(v));
    }


    /**
     * @deprecated Check out {@link dev.oneuiproject.oneui.ktx.TabLayoutKt#getTabViewGroup(TabLayout) TabLayoutKt.getTabView}.
     */
    @Nullable
    private static ViewGroup getTabViewGroup(@NonNull TabLayout tabLayout) {
       return TabLayoutKt.getTabViewGroup(tabLayout);
    }

    /**
     * @deprecated Check out {@link dev.oneuiproject.oneui.ktx.TabLayoutKt#getTabView(TabLayout, int) TabLayoutKt.getTabView}.
     */
    @Nullable
    private static View getTabView(@NonNull TabLayout tabLayout, int position) {
        return TabLayoutKt.getTabView(tabLayout, position);
    }

}
