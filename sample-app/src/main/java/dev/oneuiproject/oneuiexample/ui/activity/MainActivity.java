package dev.oneuiproject.oneuiexample.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.SeslMenuItem;
import androidx.core.view.MenuCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.sec.sesl.tester.R;
import com.sec.sesl.tester.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;

import dev.oneuiproject.oneui.layout.DrawerLayout;
import dev.oneuiproject.oneui.layout.ToolbarLayout.Badge;
import dev.oneuiproject.oneui.utils.ActivityUtils;
import dev.oneuiproject.oneui.widget.TipPopup;
import dev.oneuiproject.oneuiexample.ui.core.base.FragmentInfo;
import dev.oneuiproject.oneuiexample.ui.fragment.AppPickerFragment;
import dev.oneuiproject.oneuiexample.ui.fragment.icons.IconsFragment;
import dev.oneuiproject.oneuiexample.ui.fragment.IndexScrollFragment;
import dev.oneuiproject.oneuiexample.ui.fragment.PickersFragment;
import dev.oneuiproject.oneuiexample.ui.fragment.PreferencesFragment;
import dev.oneuiproject.oneuiexample.ui.fragment.ProgressBarFragment;
import dev.oneuiproject.oneuiexample.ui.fragment.QRCodeFragment;
import dev.oneuiproject.oneuiexample.ui.fragment.SeekBarFragment;
import dev.oneuiproject.oneuiexample.ui.fragment.SwipeRefreshFragment;
import dev.oneuiproject.oneuiexample.ui.fragment.TabsFragment;
import dev.oneuiproject.oneuiexample.ui.fragment.WidgetsFragment;
import dev.oneuiproject.oneuiexample.ui.core.drawer.DrawerListAdapter;
import dev.oneuiproject.oneuiexample.utils.DarkModeUtils;

public class MainActivity extends AppCompatActivity
        implements DrawerListAdapter.DrawerListener {
    private ActivityMainBinding mBinding;
    private FragmentManager mFragmentManager;
    private final List<Fragment> fragments = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        initFragmentList();
        initDrawer();
        initFragments();
        initOnBackPressed();
    }

    @Override
    public void attachBaseContext(Context context) {
        // pre-OneUI
        if (Build.VERSION.SDK_INT <= 28) {
            super.attachBaseContext(DarkModeUtils.createDarkModeContextWrapper(context));
        } else {
            super.attachBaseContext(context);
        }
    }

    private void initFragmentList() {
        fragments.add(new WidgetsFragment());
        fragments.add(new ProgressBarFragment());
        fragments.add(new SeekBarFragment());
        fragments.add(new SwipeRefreshFragment());
        fragments.add(new PreferencesFragment());
        fragments.add(null);
        fragments.add(new TabsFragment());
        fragments.add(null);
        fragments.add(new AppPickerFragment());
        fragments.add(new IndexScrollFragment());
        fragments.add(new PickersFragment());
        fragments.add(null);
        fragments.add(new QRCodeFragment());
        fragments.add(new IconsFragment());
    }

    private OnBackPressedCallback mBackPressedCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            onDrawerItemSelected(0);
            ((DrawerListAdapter)mBinding.drawerListView.getAdapter()).setSelectedItem(0);
        }
    };

    private void initOnBackPressed() {
        getOnBackPressedDispatcher().addCallback(this, mBackPressedCallback);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // pre-OneUI
        if (Build.VERSION.SDK_INT <= 28) {
            final Resources res = getResources();
            res.getConfiguration().setTo(DarkModeUtils.createDarkModeConfig(this, newConfig));
        }
    }



    private void initDrawer() {
        mBinding.drawerLayout.setDrawerButtonIcon(getDrawable(dev.oneuiproject.oneui.R.drawable.ic_oui_info_outline));
        mBinding.drawerLayout.setDrawerButtonTooltip("About page");
        mBinding.drawerLayout.setDrawerButtonOnClickListener(v -> {
                    ActivityUtils.startPopOverActivity(this,
                            new Intent(MainActivity.this, SampleAboutActivity.class),
                            null,
                            ActivityUtils.POP_OVER_POSITION_TOP | ActivityUtils.POP_OVER_POSITION_CENTER_HORIZONTAL);
                    mBinding.drawerLayout.setDrawerButtonBadge(new Badge.None());
                });

        mBinding.drawerListView.setLayoutManager(new LinearLayoutManager(this));
        mBinding.drawerListView.setAdapter(new DrawerListAdapter(this, fragments, this));
        mBinding.drawerListView.setItemAnimator(null);
        mBinding.drawerListView.setHasFixedSize(true);
        mBinding.drawerListView.seslSetLastRoundedCorner(false);
        mBinding.drawerLayout.setButtonBadges(new Badge.Dot(), new Badge.Dot());
        mBinding.drawerLayout.setDrawerStateListener((state) -> {
            if (state == DrawerLayout.DrawerState.OPEN) {
                mBinding.drawerLayout.setNavigationButtonBadge(new Badge.None());
            }
            return null;
        });
    }

    private void initFragments() {
        mFragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        for (Fragment fragment : fragments) {
            if (fragment != null) transaction.add(R.id.main_content, fragment);
        }
        transaction.commit();
        mFragmentManager.executePendingTransactions();

        onDrawerItemSelected(0);
    }

    @Override
    public boolean onDrawerItemSelected(int position) {
        Fragment newFragment = fragments.get(position);
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        for (Fragment fragment : mFragmentManager.getFragments()) {
            transaction.hide(fragment);
        }
        transaction.show(newFragment).commit();

        if (newFragment instanceof FragmentInfo) {
            if (!((FragmentInfo) newFragment).isAppBarEnabled()) {
                mBinding.drawerLayout.setExpanded(false, false);
                mBinding.drawerLayout.setExpandable(false);
            } else {
                mBinding.drawerLayout.setExpandable(true);
                mBinding.drawerLayout.setExpanded(false, false);
            }
            mBinding.drawerLayout.setTitle(getString(R.string.app_name), ((FragmentInfo) newFragment).getTitle());
            mBinding.drawerLayout.setExpandedSubtitle(((FragmentInfo) newFragment).getTitle());
        }
        mBinding.drawerLayout.setDrawerOpen(false, true);

        mBackPressedCallback.setEnabled(position != 0);

        return true;
    }

    public DrawerLayout getDrawerLayout() {
        return mBinding.drawerLayout;
    }
}
