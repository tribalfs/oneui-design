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
import java.util.Arrays;
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
    DrawerListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        if (savedInstanceState == null) {
            initFragments();
        }
        initDrawer(savedInstanceState);
        getOnBackPressedDispatcher().addCallback(this, mBackPressedCallback);
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

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_POSITION, adapter.getSelectedPosition());
    }

    private OnBackPressedCallback mBackPressedCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            adapter.setSelectedItem(0);
        }
    };


    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // pre-OneUI
        if (Build.VERSION.SDK_INT <= 28) {
            final Resources res = getResources();
            res.getConfiguration().setTo(DarkModeUtils.createDarkModeConfig(this, newConfig));
        }
    }


    private final Runnable offsetRunner = new Runnable() {
        @Override
        public void run() {
            adapter.setOffset(mBinding.drawerLayout.getDrawerOffset());
            mBinding.drawerLayout.postDelayed(this, 10);
        }
    };

    private void initDrawer(Bundle savedInstanceState) {
        List<FragmentInfo> drawerItems = new ArrayList<>();
        List<Integer> dividerPositions = Arrays.asList(1, 5, 8);
        for (Fragment fragment: getSupportFragmentManager().getFragments()){
            drawerItems.add((FragmentInfo)fragment);
            if (dividerPositions.contains(drawerItems.size() - 1)){
                drawerItems.add(null/*for divider*/);
            }
        }

        adapter = new DrawerListAdapter(this, drawerItems, this);

        if (savedInstanceState != null) {
            adapter.setSelectedItem(savedInstanceState.getInt(KEY_SELECTED_POSITION, 0));
        }else{
            adapter.setSelectedItem(0);
        }

        mBinding.drawerLayout.setHeaderButtonIcon(
                AppCompatResources.getDrawable(this, dev.oneuiproject.oneui.R.drawable.ic_oui_settings_outline));
        mBinding.drawerLayout.setHeaderButtonTooltip("Preferences");
        mBinding.drawerLayout.setHeaderButtonOnClickListener(v -> {
            ActivityUtils.startPopOverActivity(this,
                    new Intent(MainActivity.this, PreferenceActivity.class),
                    null,
                    ActivityUtils.POP_OVER_POSITION_TOP | ActivityUtils.POP_OVER_POSITION_CENTER_HORIZONTAL);
        });

        mBinding.drawerListView.setLayoutManager(new LinearLayoutManager(this));
        mBinding.drawerListView.setAdapter(adapter);
        mBinding.drawerListView.setItemAnimator(null);
        mBinding.drawerListView.setHasFixedSize(true);
        mBinding.drawerListView.seslSetLastRoundedCorner(false);

        mBinding.drawerLayout.setHeaderButtonBadge(Badge.DOT.INSTANCE);
        mBinding.drawerLayout.setNavRailContentMinSideMargin(10);

        mBinding.drawerLayout.setDrawerStateListener((state) -> {
            if (!mBinding.drawerLayout.isLargeScreenMode()) return null;
            mBinding.drawerLayout.removeCallbacks(offsetRunner);
            switch (state) {
                case OPEN:
                    adapter.setOffset(1f);
                    break;
                case CLOSE:
                    adapter.setOffset(0f);
                    break;
                case CLOSING, OPENING:
                    mBinding.drawerLayout.post(offsetRunner);
                    break;
            }
            return null;
        });
        adapter.setOffset(mBinding.drawerLayout.isLargeScreenMode()
                ? mBinding.drawerLayout.getDrawerOffset() : 1);
    }

    private void initFragments() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        transaction.add(R.id.main_content, new ContactsFragment());
        transaction.add(R.id.main_content, new WidgetsFragment());
        transaction.add(R.id.main_content, new ProgressBarFragment());
        transaction.add(R.id.main_content, new SeekBarFragment());
        transaction.add(R.id.main_content, new TabsFragment());
        transaction.add(R.id.main_content, new PickersFragment());
        transaction.add(R.id.main_content, new QRCodeFragment());
        transaction.add(R.id.main_content, new IconsFragment());
        transaction.add(R.id.main_content, new AppPickerFragment());

        transaction.commitNow();
    }

    @Override
    public boolean onFragmentItemSelected(FragmentInfo fragmentInfo) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        String selectedClassName = fragmentInfo.getClass().getName();
        for (Fragment fragment : fragmentManager.getFragments()) {
            if (fragment.getClass().getName().equals(selectedClassName)){
                if (fragment.isHidden()) {
                    transaction.show(fragment);
                }
            }else {
                transaction.hide(fragment);
            }
        }
        transaction.commit();

        if (!fragmentInfo.isAppBarEnabled()) {
            mBinding.drawerLayout.setExpanded(false, false);
            mBinding.drawerLayout.setExpandable(false);
        } else {
            mBinding.drawerLayout.setExpandable(true);
            mBinding.drawerLayout.setExpanded(false, false);
        }
        mBinding.drawerLayout.setTitle(fragmentInfo.getTitle());
        mBinding.drawerLayout.setExpandedSubtitle(fragmentInfo.getSubtitle());

        if (fragmentInfo.showBottomTab()) {
            mBinding.bottomTab.show();
        }else{
            mBinding.bottomTab.hide(false);
        }

        boolean isImmersive = fragmentInfo.isImmersiveScroll();
        mBinding.drawerLayout.setImmersiveScroll(isImmersive);

        if (fragmentInfo.showSwitchBar()) {
            mBinding.drawerLayout.getSwitchBar().show();
        }else{
            mBinding.drawerLayout.getSwitchBar().hide();
        }

        if (mBinding.drawerLayout.isLargeScreenMode()) {
            if (mBinding.drawerLayout.isActionMode()) {
                mBinding.drawerLayout.endActionMode();
            }
            if (mBinding.drawerLayout.isSearchMode()) {
                mBinding.drawerLayout.endSearchMode();
            }
        }else{
            mBinding.drawerLayout.setDrawerOpen(false, true);
        }

        mBinding.drawerLayout.setCloseNavRailOnBack(true);
        mBackPressedCallback.setEnabled(!selectedClassName.equals(ContactsFragment.class.getSimpleName()));

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

    private static final String KEY_SELECTED_POSITION = "selected_position";
}
