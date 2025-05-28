package dev.oneuiproject.oneuiexample.ui.main.core.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import dev.oneuiproject.oneui.layout.ToolbarLayout;
import dev.oneuiproject.oneuiexample.ui.main.core.drawer.FragmentDrawerItem;

public abstract class BaseFragment extends Fragment
        implements FragmentDrawerItem {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(getLayoutResId(), container, false);
    }

    public abstract int getLayoutResId();

    public abstract int getIconResId();

    public abstract CharSequence getTitle();

    @Override
    public boolean isAppBarEnabled() {
        return true;
    }

    @Override
    public  CharSequence getSubtitle() {
        return null;
    }

    @Override
    public  boolean isImmersiveMode() {
        return false;
    }

    @Override
    public  boolean showBottomTab() {
        return false;
    }

    @Override
    public  boolean showSwitchBar() {
        return false;
    }

    @Override
    public ToolbarLayout.MainRoundedCorners roundedCorners(){
        return ToolbarLayout.MainRoundedCorners.ALL;
    }
}
