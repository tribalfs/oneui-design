package dev.oneuiproject.oneuiexample.ui.activity;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.AppBarLayout;
import com.sec.sesl.tester.BuildConfig;
import com.sec.sesl.tester.R;
import com.sec.sesl.tester.databinding.ActivityAboutBinding;
import com.sec.sesl.tester.databinding.ActivityAboutContentBinding;

import dev.oneuiproject.oneui.utils.DeviceLayoutUtil;
import dev.oneuiproject.oneui.utils.ViewUtils;
import dev.oneuiproject.oneui.utils.internal.ToolbarLayoutUtils;
import dev.oneuiproject.oneui.widget.Toast;

public class AboutActivity extends AppCompatActivity
        implements View.OnClickListener {
    private boolean mEnableBackToHeader;
    private long mLastClickTime;

    private ActivityAboutBinding mBinding;
    private ActivityAboutContentBinding mBottomContent;

    private AboutAppBarListener mAppBarListener = new AboutAppBarListener();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityAboutBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        if (Build.VERSION.SDK_INT >= 30 && !getWindow().getDecorView().getFitsSystemWindows()) {
            mBinding.getRoot().setOnApplyWindowInsetsListener((v, insets) -> {
                Insets systemBarsInsets = WindowInsetsCompat.toWindowInsetsCompat(insets)
                        .getInsets(WindowInsetsCompat.Type.systemBars());
                mBinding.getRoot().setPadding(systemBarsInsets.left, systemBarsInsets.top,
                        systemBarsInsets.right, systemBarsInsets.bottom);
                return insets;
            });
        }

        mBottomContent = mBinding.aboutBottomContent;

        setSupportActionBar(mBinding.aboutToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        mBinding.aboutToolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        resetAppBar(getResources().getConfiguration());
        initContent();
        initOnBackPressed();
    }


    private OnBackPressedCallback mBackPressedCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            mBinding.aboutAppBar.setExpanded(true);
        }
    };

    private void initOnBackPressed() {
        getOnBackPressedDispatcher().addCallback(this, mBackPressedCallback);
        mBackPressedCallback.setEnabled(mBinding.aboutAppBar.seslIsCollapsed()
                && getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE
                && !isInMultiWindowMode());
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        resetAppBar(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.sample3_menu_about, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_app_info) {
            Intent intent = new Intent(
                    "android.settings.APPLICATION_DETAILS_SETTINGS",
                    Uri.fromParts("package", BuildConfig.APPLICATION_ID, null));
            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return true;
        }
        return false;
    }

    @Override
    public boolean isInMultiWindowMode() {
        return Build.VERSION.SDK_INT >= 24 && super.isInMultiWindowMode();
    }

    @SuppressLint("RestrictedApi")
    private void resetAppBar(Configuration config) {
        ToolbarLayoutUtils.hideStatusBarForLandscape(this, config.orientation);
        ToolbarLayoutUtils.updateListBothSideMargin(this,
                mBinding.aboutBottomContainer);

        if (config.orientation != Configuration.ORIENTATION_LANDSCAPE
                && !isInMultiWindowMode() || DeviceLayoutUtil.INSTANCE.isTabletLayoutOrDesktop(this)) {
            mBinding.aboutAppBar.seslSetCustomHeightProportion(true, 0.5f);
            mBinding.aboutAppBar.addOnOffsetChangedListener(mAppBarListener);
            mBinding.aboutAppBar.setExpanded(true, false);
            mBackPressedCallback.setEnabled(true);
            mBinding.aboutSwipeUpContainer.setVisibility(View.VISIBLE);
            ViewGroup.LayoutParams lp = mBinding.aboutSwipeUpContainer.getLayoutParams();
            lp.height = getResources().getDisplayMetrics().heightPixels / 2;
        } else {
            mBinding.aboutAppBar.setExpanded(false, false);
            mBackPressedCallback.setEnabled(false);
            mBinding.aboutAppBar.seslSetCustomHeightProportion(true, 0);
            mBinding.aboutAppBar.removeOnOffsetChangedListener(mAppBarListener);
            mBinding.aboutBottomContainer.setAlpha(1f);
            setBottomContentEnabled(true);
            mBinding.aboutSwipeUpContainer.setVisibility(View.GONE);
        }
    }

    private void initContent() {
        ViewUtils.semSetRoundedCorners(
                mBinding.aboutBottomContent.getRoot(),
                ViewUtils.SEM_ROUNDED_CORNER_TOP_LEFT | ViewUtils.SEM_ROUNDED_CORNER_TOP_RIGHT);
        ViewUtils.semSetRoundedCornerColor(mBinding.aboutBottomContent.getRoot(),
                ViewUtils.SEM_ROUNDED_CORNER_TOP_LEFT | ViewUtils.SEM_ROUNDED_CORNER_TOP_RIGHT,
                getColor(dev.oneuiproject.oneui.design.R.color.oui_round_and_bgcolor));

        Drawable appIcon = getDrawable(R.mipmap.ic_launcher);
        mBinding.aboutHeaderAppIcon.setImageDrawable(appIcon);
        mBinding.aboutBottomAppIcon.setImageDrawable(appIcon);

        mBinding.aboutHeaderAppVersion.setText("Version " + BuildConfig.VERSION_NAME);
        mBinding.aboutBottomAppVersion.setText("Version " + BuildConfig.VERSION_NAME);

        mBinding.aboutHeaderGithub.setOnClickListener(this);
        TooltipCompat.setTooltipText(mBinding.aboutHeaderGithub, "GitHub");
        mBinding.aboutHeaderTelegram.setOnClickListener(this);
        TooltipCompat.setTooltipText(mBinding.aboutHeaderTelegram, "Telegram");

        mBottomContent.aboutBottomDevYann.setOnClickListener(this);
        mBottomContent.aboutBottomDevMesa.setOnClickListener(this);
        mBottomContent.aboutBottomDevTribalfs.setOnClickListener(this);

        mBottomContent.aboutBottomOssApache.setOnClickListener(this);
        mBottomContent.aboutBottomOssMit.setOnClickListener(this);

        mBottomContent.aboutBottomRelativeJetpack.setOnClickListener(this);
        mBottomContent.aboutBottomRelativeMaterial.setOnClickListener(this);
        mBottomContent.aboutBottomRelativeSeslAndroidx.setOnClickListener(this);
        mBottomContent.aboutBottomRelativeSeslMaterial.setOnClickListener(this);
        mBottomContent.aboutBottomRelativeDesign6.setOnClickListener(this);
    }

    private void setBottomContentEnabled(boolean enabled) {
        mBinding.aboutHeaderGithub.setEnabled(!enabled);
        mBinding.aboutHeaderTelegram.setEnabled(!enabled);
        mBottomContent.aboutBottomDevYann.setEnabled(enabled);
        mBottomContent.aboutBottomDevMesa.setEnabled(enabled);
        mBottomContent.aboutBottomOssApache.setEnabled(enabled);
        mBottomContent.aboutBottomOssMit.setEnabled(enabled);
        mBottomContent.aboutBottomRelativeJetpack.setEnabled(enabled);
        mBottomContent.aboutBottomRelativeMaterial.setEnabled(enabled);
        mBottomContent.aboutBottomRelativeSeslAndroidx.setEnabled(enabled);
        mBottomContent.aboutBottomRelativeSeslMaterial.setEnabled(enabled);
        mBottomContent.aboutBottomDevTribalfs.setEnabled(enabled);
        mBottomContent.aboutBottomRelativeDesign6.setEnabled(enabled);

    }

    @Override
    public void onClick(View v) {
        long uptimeMillis = SystemClock.uptimeMillis();
        if (uptimeMillis - mLastClickTime > 600L) {
            String url = null;
            if (v.getId() == mBinding.aboutHeaderGithub.getId()) {
                url = "https://github.com/tribalfs/oneui-design";
            } else if (v.getId() == mBinding.aboutHeaderTelegram.getId()) {
                url = "https://t.me/oneuiproject";
            } else if (v.getId() == mBottomContent.aboutBottomDevYann.getId()) {
                url = "https://github.com/Yanndroid";
            } else if (v.getId() == mBottomContent.aboutBottomDevMesa.getId()) {
                url = "https://github.com/salvogiangri";
            } else if (v.getId() == mBottomContent.aboutBottomOssApache.getId()) {
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt";
            } else if (v.getId() == mBottomContent.aboutBottomOssMit.getId()) {
                url = "https://raw.githubusercontent.com/tribalfs/oneui-design/refs/heads/oneui6/LICENSE";
            } else if (v.getId() == mBottomContent.aboutBottomRelativeJetpack.getId()) {
                url = "https://developer.android.com/jetpack";
            } else if (v.getId() == mBottomContent.aboutBottomRelativeMaterial.getId()) {
                url = "https://material.io/develop/android";
            } else if (v.getId() == mBottomContent.aboutBottomRelativeSeslAndroidx.getId()) {
                url = "https://github.com/tribalfs/sesl-androidx";
            } else if (v.getId() == mBottomContent.aboutBottomRelativeSeslMaterial.getId()) {
                url = "https://github.com/tribalfs/sesl-material-components-android";
            }else if (v.getId() == mBottomContent.aboutBottomRelativeDesign6.getId()) {
                url = "https://github.com/tribalfs/oneui-design";
            }else if (v.getId() == mBottomContent.aboutBottomDevTribalfs.getId()) {
                url = "https://github.com/tribalfs";
            }

            if (url != null) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(
                            this, "No suitable activity found", Toast.LENGTH_SHORT).show();
                }
            }
        }
        mLastClickTime = uptimeMillis;
    }

    private class AboutAppBarListener implements AppBarLayout.OnOffsetChangedListener {
        @Override
        public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
            // Handle the SwipeUp anim view
            final int totalScrollRange = appBarLayout.getTotalScrollRange();
            final int abs = Math.abs(verticalOffset);

            if (abs >= totalScrollRange / 2) {
                mBinding.aboutSwipeUpContainer.setAlpha(0f);
                setBottomContentEnabled(true);
            } else if (abs == 0) {
                mBinding.aboutSwipeUpContainer.setAlpha(1f);
                setBottomContentEnabled(false);
            } else {
                float offsetAlpha = (appBarLayout.getY() / totalScrollRange);
                float arrowAlpha = 1 - (offsetAlpha * -3);
                if (arrowAlpha < 0) {
                    arrowAlpha = 0;
                } else if (arrowAlpha > 1) {
                    arrowAlpha = 1;
                }
                mBinding.aboutSwipeUpContainer.setAlpha(arrowAlpha);
            }

            // Handle the bottom part of the UI
            final float alphaRange = mBinding.aboutCtl.getHeight() * 0.143f;
            final float layoutPosition = Math.abs(appBarLayout.getTop());
            float bottomAlpha = (150.0f / alphaRange)
                    * (layoutPosition - (mBinding.aboutCtl.getHeight() * 0.35f));

            if (bottomAlpha < 0) {
                bottomAlpha = 0;
            } else if (bottomAlpha >= 255) {
                bottomAlpha = 255;
            }

            mBinding.aboutBottomContainer.setAlpha(bottomAlpha / 255);

            mBackPressedCallback.setEnabled(appBarLayout.getTotalScrollRange() + verticalOffset == 0);
        }
    }
}
