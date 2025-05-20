package dev.oneuiproject.oneuiexample.ui.main;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static androidx.picker3.widget.SeslColorPicker.isTablet;
import static dev.oneuiproject.oneui.ktx.ContextKt.startPopOverActivity;
import static dev.oneuiproject.oneui.widget.AdaptiveCoordinatorLayout.MARGIN_PROVIDER_ADP_DEFAULT;
import static dev.oneuiproject.oneui.widget.AdaptiveCoordinatorLayout.MARGIN_PROVIDER_ZERO;

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
import dev.oneuiproject.oneui.popover.PopOverOptions;
import dev.oneuiproject.oneui.popover.PopOverPosition;
import dev.oneuiproject.oneui.popover.PopOverPositions;
import dev.oneuiproject.oneui.popover.PopOverSize;
import dev.oneuiproject.oneuiexample.ui.customabout.CustomAboutActivity;
import dev.oneuiproject.oneuiexample.ui.preference.PreferenceActivity;
import dev.oneuiproject.oneuiexample.ui.main.core.drawer.FragmentDrawerItem;
import dev.oneuiproject.oneuiexample.ui.main.core.drawer.DrawerItem;
import dev.oneuiproject.oneuiexample.ui.main.fragment.apppicker.AppPickerFragment;
import dev.oneuiproject.oneuiexample.ui.main.fragment.contacts.ContactsFragment;
import dev.oneuiproject.oneuiexample.ui.main.fragment.PickersFragment;
import dev.oneuiproject.oneuiexample.ui.main.fragment.ProgressBarFragment;
import dev.oneuiproject.oneuiexample.ui.main.fragment.QRCodeFragment;
import dev.oneuiproject.oneuiexample.ui.main.fragment.SeekBarFragment;
import dev.oneuiproject.oneuiexample.ui.main.fragment.TabsFragment;
import dev.oneuiproject.oneuiexample.ui.main.fragment.WidgetsFragment;
import dev.oneuiproject.oneuiexample.ui.main.core.drawer.DrawerListAdapter;
import dev.oneuiproject.oneuiexample.ui.main.fragment.icons.IconsFragment;
import dev.oneuiproject.oneuiexample.ui.main.core.util.DarkModeUtils;

public class MainActivity extends AppCompatActivity
        implements DrawerListAdapter.DrawerListener {
    private ActivityMainBinding mBinding;
    DrawerListAdapter adapter;
    Boolean mIsDrawerMode = false;

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
        outState.putBoolean(KEY_BOTTOM_TAB_SHOWN, mBinding.bottomTab.getVisibility() == VISIBLE);
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
            applyOffset();
            mBinding.drawerLayout.postDelayed(this, 10);
        }
    };

    private void applyOffset(){
        float offset = mBinding.drawerLayout.getDrawerOffset();
        adapter.setOffset(offset);
    }

    private void initDrawer(Bundle savedInstanceState) {
        List<DrawerItem> drawerItems = new ArrayList<>();
        List<Integer> dividerPositions = Arrays.asList(2, 8);
        for (Fragment fragment: getSupportFragmentManager().getFragments()){
            drawerItems.add((FragmentDrawerItem)fragment);
            if (dividerPositions.contains(drawerItems.size() - 1)){
                drawerItems.add(null/*for divider*/);
            }
        }
        drawerItems.add(null/*for divider*/);
        drawerItems.add(new DrawerItem() {
            @Override public int getIconResId() {return R.drawable.ic_about_sample;}
            @Override public CharSequence getTitle() {return "Custom about";}
        });

        adapter = new DrawerListAdapter(this, drawerItems, this);

        if (savedInstanceState != null) {
            adapter.setSelectedItem(savedInstanceState.getInt(KEY_SELECTED_POSITION, 0));
            int bottomTabVisibility = savedInstanceState.getBoolean(KEY_BOTTOM_TAB_SHOWN) ? VISIBLE : GONE;
            mBinding.bottomTab.setVisibility(bottomTabVisibility);
        }else{
            adapter.setSelectedItem(0);
        }

        mBinding.drawerLayout.setHeaderButtonIcon(
                AppCompatResources.getDrawable(this, dev.oneuiproject.oneui.R.drawable.ic_oui_settings_outline));
        mBinding.drawerLayout.setHeaderButtonTooltip("Preferences");
        mBinding.drawerLayout.setHeaderButtonOnClickListener(v -> {
            startPopOverActivity(this,
                    new Intent(MainActivity.this, PreferenceActivity.class),
                    new PopOverOptions(
                            new PopOverSize(731, 360, (isTablet(this)) ? 731 : 574, 360),
                            new PopOverPositions(PopOverPosition.TOP_LEFT, PopOverPosition.TOP_LEFT)
                    ), null);
        });

        mBinding.drawerListView.setLayoutManager(new LinearLayoutManager(this));
        mBinding.drawerListView.setAdapter(adapter);
        mBinding.drawerListView.setItemAnimator(null);
        mBinding.drawerListView.setHasFixedSize(true);
        mBinding.drawerListView.seslSetLastRoundedCorner(false);

        mBinding.drawerLayout.setHeaderButtonBadge(Badge.DOT.INSTANCE);
        mBinding.drawerLayout.setNavRailContentMinSideMargin(10);
        mBinding.drawerLayout.setDrawerStateListener((state) -> {
            if (!mBinding.drawerLayout.isLargeScreenMode()) return;
            mBinding.drawerLayout.removeCallbacks(offsetRunner);
            switch (state) {
                case OPEN:
                case CLOSE:
                    applyOffset();
                    break;
                case CLOSING, OPENING:
                    mBinding.drawerLayout.post(offsetRunner);
                    break;
            }
        });
        float initialOffset = mBinding.drawerLayout.isLargeScreenMode()
                ? mBinding.drawerLayout.getDrawerOffset() : 1;
        adapter.setOffset(initialOffset);

    }

    private void initFragments() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        transaction.add(R.id.main_content, new ContactsFragment());
        transaction.add(R.id.main_content, new IconsFragment());
        transaction.add(R.id.main_content, new AppPickerFragment());
        transaction.add(R.id.main_content, new WidgetsFragment());
        transaction.add(R.id.main_content, new ProgressBarFragment());
        transaction.add(R.id.main_content, new SeekBarFragment());
        transaction.add(R.id.main_content, new PickersFragment());
        transaction.add(R.id.main_content, new QRCodeFragment());
        transaction.add(R.id.main_content, new TabsFragment());

        transaction.commitNow();
    }

    @Override
    public boolean onDrawerItemSelected(DrawerItem drawerItem) {
        if (!(drawerItem instanceof FragmentDrawerItem fragmentItem)) {
            startActivity(new Intent(this, CustomAboutActivity.class));
            return false;
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        String selectedClassName = drawerItem.getClass().getName();

        for (Fragment fragment : fragmentManager.getFragments()) {
            if (fragment.getClass().getName().equals(selectedClassName)) {
                if (fragment.isHidden()) {
                    transaction.show(fragment);
                }
            } else {
                transaction.hide(fragment);
            }
        }
        transaction.commit();

        if (!fragmentItem.isAppBarEnabled()) {
            mBinding.drawerLayout.setExpanded(false, false);
            mBinding.drawerLayout.setExpandable(false);
        } else {
            mBinding.drawerLayout.setExpandable(true);
            mBinding.drawerLayout.setExpanded(false, false);
        }

        mBinding.drawerLayout.setMainRoundedCorners(fragmentItem.roundedCorners());

        configureImmersiveMode(fragmentItem.isImmersiveMode());

        mBinding.drawerLayout.setTitle(fragmentItem.getTitle());
        mBinding.drawerLayout.setSubtitle(fragmentItem.getSubtitle());


        if (fragmentItem.showSwitchBar()) {
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

        if (fragmentItem.showBottomTab()) {
            mBinding.bottomTab.show(true);
        } else {
            mBinding.bottomTab.hide(false);
        }

        boolean isHomeSelected = selectedClassName.equals(ContactsFragment.class.getName());
        mBinding.drawerLayout.setCloseNavRailOnBack(isHomeSelected);
        mBackPressedCallback.setEnabled(!isHomeSelected);

        return true;
    }



    private void configureImmersiveMode(boolean immersiveMode){
        mBinding.drawerLayout.setImmersiveScroll(immersiveMode);
        boolean immersiveApplied = mBinding.drawerLayout.isImmersiveScroll();
        mBinding.drawerLayout.setAdaptiveMarginProvider(immersiveApplied
                ? MARGIN_PROVIDER_ZERO : MARGIN_PROVIDER_ADP_DEFAULT);
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
    private static final String KEY_BOTTOM_TAB_SHOWN = "bottom_tab_shown";

}
