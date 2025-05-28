package dev.oneuiproject.oneuiexample.ui.main.core.drawer;

import dev.oneuiproject.oneui.layout.ToolbarLayout.MainRoundedCorners;

public interface FragmentDrawerItem extends DrawerItem {
    int getLayoutResId();

    boolean isAppBarEnabled();

    CharSequence getSubtitle();

    boolean isImmersiveMode();

    boolean showBottomTab();

    boolean showSwitchBar();

    MainRoundedCorners roundedCorners();
}
