package dev.oneuiproject.oneuiexample.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.sec.sesl.tester.R;
import com.sec.sesl.tester.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import dev.oneuiproject.oneui.layout.Badge;
import dev.oneuiproject.oneui.layout.DrawerLayout;
import dev.oneuiproject.oneui.utils.ActivityUtils;
import dev.oneuiproject.oneuiexample.ui.core.base.FragmentInfo;
import dev.oneuiproject.oneuiexample.ui.fragment.apppicker.AppPickerFragment;
import dev.oneuiproject.oneuiexample.ui.fragment.contacts.ContactsFragment;
import dev.oneuiproject.oneuiexample.ui.fragment.PickersFragment;
import dev.oneuiproject.oneuiexample.ui.fragment.ProgressBarFragment;
import dev.oneuiproject.oneuiexample.ui.fragment.QRCodeFragment;
import dev.oneuiproject.oneuiexample.ui.fragment.SeekBarFragment;
import dev.oneuiproject.oneuiexample.ui.fragment.TabsFragment;
import dev.oneuiproject.oneuiexample.ui.fragment.WidgetsFragment;
import dev.oneuiproject.oneuiexample.ui.core.drawer.DrawerListAdapter;
import dev.oneuiproject.oneuiexample.ui.fragment.icons.IconsFragment;
import dev.oneuiproject.oneuiexample.ui.core.DarkModeUtils;

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
        fragments.add(new ContactsFragment());
        fragments.add(null);
        fragments.add(new WidgetsFragment());
        fragments.add(new ProgressBarFragment());
        fragments.add(new SeekBarFragment());
        fragments.add(new TabsFragment());
        fragments.add(null);
        fragments.add(new PickersFragment());
        fragments.add(new QRCodeFragment());
        fragments.add(new IconsFragment());
        fragments.add(null);
        fragments.add(new AppPickerFragment());

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
        mBinding.drawerLayout.setHeaderButtonIcon(AppCompatResources.getDrawable(this, dev.oneuiproject.oneui.R.drawable.ic_oui_settings_outline));
        mBinding.drawerLayout.setHeaderButtonTooltip("Preferences");
        mBinding.drawerLayout.setHeaderButtonOnClickListener(v -> {
                    ActivityUtils.startPopOverActivity(this,
                            new Intent(MainActivity.this, PreferenceActivity.class),
                            null,
                            ActivityUtils.POP_OVER_POSITION_TOP | ActivityUtils.POP_OVER_POSITION_CENTER_HORIZONTAL);
                    mBinding.drawerLayout.setHeaderButtonBadge(Badge.NONE.INSTANCE);
                });

        mBinding.drawerListView.setLayoutManager(new LinearLayoutManager(this));
        mBinding.drawerListView.setAdapter(new DrawerListAdapter(this, fragments, this));
        mBinding.drawerListView.setItemAnimator(null);
        mBinding.drawerListView.setHasFixedSize(true);
        mBinding.drawerListView.seslSetLastRoundedCorner(false);
        mBinding.drawerLayout.setButtonBadges(Badge.DOT.INSTANCE, Badge.DOT.INSTANCE);
        mBinding.drawerLayout.setDrawerStateListener((state) -> {
            if (state == DrawerLayout.DrawerState.OPEN) {
                mBinding.drawerLayout.setNavigationButtonBadge(Badge.NONE.INSTANCE);
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
            mBinding.drawerLayout.setTitle(((FragmentInfo) newFragment).getTitle());
            if (newFragment instanceof ContactsFragment) {
                mBinding.drawerLayout.setExpandedSubtitle("Pull down to refresh");
            }
        }
        mBinding.drawerLayout.setDrawerOpen(false, true);

        mBackPressedCallback.setEnabled(position != 0);

        return true;
    }

    public DrawerLayout getDrawerLayout() {
        return mBinding.drawerLayout;
    }

    @Override
    public void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        if (Objects.equals(intent.getAction(), Intent.ACTION_SEARCH)) {
            mBinding.drawerLayout.setSearchQueryFromIntent(intent);
        }
    }
}
