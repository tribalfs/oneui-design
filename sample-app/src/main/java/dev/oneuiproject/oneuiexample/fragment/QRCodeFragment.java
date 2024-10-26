package dev.oneuiproject.oneuiexample.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sec.sesl.tester.R;

import dev.oneuiproject.oneui.qr.QREncoder;
import dev.oneuiproject.oneuiexample.base.BaseFragment;

public class QRCodeFragment extends BaseFragment {


    @Override
    public int getLayoutResId() {
        return R.layout.sample3_fragment_qr_code;
    }

    @Override
    public int getIconResId() {
        return dev.oneuiproject.oneui.R.drawable.ic_oui_qr_code;
    }

    @Override
    public CharSequence getTitle() {
        return "QRCode";
    }

}
