package dev.oneuiproject.oneuiexample.ui.main.fragment;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.sec.sesl.tester.R;

import dev.oneuiproject.oneuiexample.ui.main.core.base.BaseFragment;

public class TabsFragment extends BaseFragment {
    private TabLayout mSubTabs;
    private BottomNavigationView mBottomNavView;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initSubTabs(view);
        initBNV(view);
    }

    @Override
    public int getLayoutResId() {
        return R.layout.sample3_fragment_tabs;
    }

    @Override
    public int getIconResId() {
        return dev.oneuiproject.oneui.R.drawable.ic_oui_prompt_from_menu;
    }

    @Override
    public CharSequence getTitle() {
        return "Navigation";
    }


    @Override
    public boolean showBottomTab() {
        return true;
    }

    private void initSubTabs(@NonNull View view) {
        mSubTabs = view.findViewById(R.id.tabs_subtab);
        mSubTabs.addTab(mSubTabs.newTab().setText("Subtab 4"));
        mSubTabs.addTab(mSubTabs.newTab().setText("Subtab 5"));
        mSubTabs.addTab(mSubTabs.newTab().setText("Subtab 6"));
        mSubTabs.addTab(mSubTabs.newTab().setText("Subtab 7"));
        mSubTabs.addTab(mSubTabs.newTab().setText("Subtab 8"));
    }

    private void initBNV(@NonNull View view) {
        mBottomNavView = view.findViewById(R.id.tabs_bottomnav);
        mBottomNavView.seslSetGroupDividerEnabled(true);
    }



}
