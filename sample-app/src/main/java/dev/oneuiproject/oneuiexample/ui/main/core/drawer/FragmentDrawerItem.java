package dev.oneuiproject.oneuiexample.ui.main.core.drawer;

import dev.oneuiproject.oneui.layout.ToolbarLayout.MainRoundedCorners;

public interface FragmentDrawerItem extends DrawerItem {
    int getLayoutResId();

    boolean isAppBarEnabled();

    CharSequence getSubtitle();

    boolean isImmersiveMode();

    boolean showSwitchBar();

    boolean showBottomTab();

    MainRoundedCorners roundedCorners();
}
