package dev.oneuiproject.oneui.utils.internal;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import static dev.oneuiproject.oneui.ktx.ActivityKt.isInMultiWindowModeCompat;

import android.app.Activity;
import android.content.res.Configuration;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.reflect.DeviceInfo;

import dev.oneuiproject.oneui.utils.DeviceLayoutUtil;

/**
 * @hide
 */
@RestrictTo(LIBRARY)
public class ToolbarLayoutUtils {
    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    public static void hideStatusBarForLandscape(@NonNull Activity activity,
                                                 int orientation) {
        if (DeviceLayoutUtil.isTabletBuildOrIsDeskTopMode(activity)) return;

        WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (isInMultiWindowModeCompat(activity) || DeviceLayoutUtil.isScreenWidthLarge(activity)) {
                lp.flags &= -(WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
            } else {
                lp.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
            }
            if (DeviceInfo.isOneUI()) {
                ReflectUtils.genericInvokeMethod(
                        WindowManager.LayoutParams.class,
                        lp,
                        "semAddExtensionFlags",
                        1 /* WindowManager.LayoutParams.SEM_EXTENSION_FLAG_RESIZE_FULLSCREEN_WINDOW_ON_SOFT_INPUT */);
            }
        } else {
            lp.flags &= -(WindowManager.LayoutParams.FLAG_FULLSCREEN
                    | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);

            if (DeviceInfo.isOneUI()) {
                ReflectUtils.genericInvokeMethod(
                        WindowManager.LayoutParams.class,
                        lp,
                        "semClearExtensionFlags",
                        1 /* WindowManager.LayoutParams.SEM_EXTENSION_FLAG_RESIZE_FULLSCREEN_WINDOW_ON_SOFT_INPUT */);
            }
        }
        activity.getWindow().setAttributes(lp);
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    public static void updateListBothSideMargin(@NonNull Activity activity,
                                                @NonNull ViewGroup layout) {
        if (!activity.isDestroyed() && !activity.isFinishing()) {
            activity.findViewById(android.R.id.content).post(() -> {
                SideMarginParams sideMarginParams = getSideMarginParams(activity);
                setSideMarginParams(layout, sideMarginParams, 0, 0);
                layout.requestLayout();
            });
        }
    }

    @RestrictTo(LIBRARY)
    public static SideMarginParams getSideMarginParams(@NonNull Activity activity) {
        final int width = activity.findViewById(android.R.id.content).getWidth();
        Configuration config = activity.getResources().getConfiguration();
        return new SideMarginParams(
                (int) (width * getMarginRatio(config.screenWidthDp, config.screenHeightDp)),
                activity.getResources().getConfiguration().screenWidthDp >= 589);
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    public static class SideMarginParams{
        public int sideMargin;
        public boolean matchParent;
        public SideMarginParams(
                int sideMargin,
                boolean matchParent
        ) {
            this.sideMargin = sideMargin;
            this.matchParent = matchParent;
        }
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    public static void setSideMarginParams(@NonNull View layout, SideMarginParams smp,
                                           Integer additionalLeft,
                                           Integer additionalRight) {
        ViewGroup.MarginLayoutParams lp
                = (ViewGroup.MarginLayoutParams) layout.getLayoutParams();
        if (lp != null) {
            lp.leftMargin = smp.sideMargin + additionalLeft;
            lp.rightMargin = smp.sideMargin + additionalRight;
        } else {
            layout.setPadding(smp.sideMargin + additionalLeft,
                    layout.getPaddingTop(),
                    smp.sideMargin + additionalRight,
                    layout.getPaddingBottom());
        }
        if (smp.matchParent){
            lp.width = MATCH_PARENT;
        }
        layout.setLayoutParams(lp);
    }

    /**
     * @hide
     */
    private static float getMarginRatio(int screenWidthDp, int screenHeightDp) {
        if (screenWidthDp < 589) {
            return 0.0f;
        }
        if (screenHeightDp > 411 && screenWidthDp <= 959) {
            return 0.05f;
        }
        if (screenWidthDp >= 960 && screenHeightDp <= 1919) {
            return 0.125f;
        }
        if (screenWidthDp >= 1920) {
            return 0.25f;
        }

        return 0.0f;
    }

}
