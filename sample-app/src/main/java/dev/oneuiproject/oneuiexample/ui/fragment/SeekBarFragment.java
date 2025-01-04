package dev.oneuiproject.oneuiexample.ui.fragment;

import static dev.oneuiproject.oneui.ktx.SeslSeekBarKt.updateDualColorRange;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SeslSeekBar;

import com.sec.sesl.tester.R;

import dev.oneuiproject.oneui.utils.SeekBarUtils;
import dev.oneuiproject.oneuiexample.ui.core.base.BaseFragment;

public class SeekBarFragment extends BaseFragment {

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SeslSeekBar seekbarSeamless = view.findViewById(R.id.seekbar_level_seamless);
        seekbarSeamless.setSeamless(true);

        SeslSeekBar seekBarOverlap = view.findViewById(R.id.fragment_seekbar_overlap);
        updateDualColorRange(seekBarOverlap, 70);
    }

    @Override
    public int getLayoutResId() {
        return R.layout.sample3_fragment_seek_bar;
    }

    @Override
    public int getIconResId() {
        return R.drawable.drawer_page_icon_seekbar;
    }

    @Override
    public CharSequence getTitle() {
        return "SeekBar";
    }

    @Override
    public boolean isAppBarEnabled() {
        return false;
    }

}
