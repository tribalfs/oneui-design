package dev.oneuiproject.oneuiexample.ui.core.base;

public interface FragmentInfo {
    int getLayoutResId();

    int getIconResId();

    CharSequence getTitle();

    boolean isAppBarEnabled();

    CharSequence getSubtitle();

    boolean isImmersiveScroll();

    boolean showBottomTab();

    boolean showSwitchBar();
}
