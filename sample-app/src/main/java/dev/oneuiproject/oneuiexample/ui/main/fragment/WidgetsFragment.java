package dev.oneuiproject.oneuiexample.ui.main.fragment;

import static android.view.View.VISIBLE;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.SeslSwitchBar;

import com.sec.sesl.tester.R;

import java.util.ArrayList;
import java.util.List;

import dev.oneuiproject.oneuiexample.ui.main.MainActivity;
import dev.oneuiproject.oneuiexample.ui.main.core.base.BaseFragment;

public class WidgetsFragment extends BaseFragment
        implements View.OnClickListener {

    SeslSwitchBar seslSwitchBar;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        seslSwitchBar = ((MainActivity)requireActivity()).getDrawerLayout().getSwitchBar();

        int[] Ids = {R.id.fragment_btn_1,
                R.id.fragment_btn_2,
                R.id.fragment_btn_3,
                R.id.fragment_btn_4,
                R.id.fragment_btn_5};
        for (int id : Ids) view.findViewById(id).setOnClickListener(this);

        AppCompatSpinner spinner = view.findViewById(R.id.fragment_spinner);
        List<String> items = new ArrayList<>();
        for (int i = 1; i < 5; i++)
            items.add("Spinner Item " + i);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(androidx.appcompat.R.layout.support_simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        SearchView searchView = view.findViewById(R.id.fragment_searchview);
        SearchManager manager = (SearchManager) requireContext().getSystemService(Context.SEARCH_SERVICE);
        searchView.setSearchableInfo(manager.getSearchableInfo(
                new ComponentName(requireContext(), MainActivity.class)));
        searchView.seslSetUpButtonVisibility(VISIBLE);
        searchView.seslSetOnUpButtonClickListener(this);

    }

    private SeslSwitchBar.OnSwitchChangeListener listener = (v, isChecked) ->{
        seslSwitchBar.setProgressBarVisible(true);
        seslSwitchBar.postDelayed(() -> seslSwitchBar.setProgressBarVisible(false), 3_000);
    };

    @Override
    public void onHiddenChanged(boolean hidden) {
        try {
            if (!hidden) {
                seslSwitchBar.addOnSwitchChangeListener(listener);
            } else {
                seslSwitchBar.removeOnSwitchChangeListener(listener);
            }
        }catch (Exception ignore){}
    }

    @Override
    public int getLayoutResId() {
        return R.layout.sample3_fragment_widgets;
    }

    @Override
    public int getIconResId() {
        return dev.oneuiproject.oneui.R.drawable.ic_oui_game_launcher;
    }

    @Override
    public CharSequence getTitle() {
        return "Widgets";
    }

    @Override
    public boolean showSwitchBar() {
        return true;
    }

    @Override
    public void onClick(View v) {
        // no-op
    }

}
