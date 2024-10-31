package dev.oneuiproject.oneuiexample.fragment.icons;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sec.sesl.tester.R;

import dev.oneuiproject.oneui.layout.DrawerLayout;
import dev.oneuiproject.oneui.layout.ToolbarLayout;
import dev.oneuiproject.oneui.widget.Toast;
import dev.oneuiproject.oneuiexample.activity.MainActivity;
import dev.oneuiproject.oneuiexample.base.BaseFragment;
import dev.oneuiproject.oneuiexample.data.IconsRepo;
import dev.oneuiproject.oneuiexample.fragment.icons.adapter.ImageAdapter;
import dev.oneuiproject.oneuiexample.ui.core.ItemDecoration;

public class IconsFragment extends BaseFragment {

    public IconsFragment() {
        super();

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView iconListView = (RecyclerView) getView();
        ImageAdapter adapter = new ImageAdapter(getContext());

        setupRecyclerView(iconListView, adapter);
        setupSelection(iconListView, adapter);
        setupAdapterClickListeners(iconListView, adapter);

        IconsRepo iconsRepo = new IconsRepo();
        adapter.submitList(iconsRepo.getIcons());
    }

    private void setupRecyclerView(RecyclerView iconListView, ImageAdapter adapter){
        iconListView.setItemAnimator(null);
        iconListView.setAdapter(adapter);
        iconListView.addItemDecoration(new ItemDecoration(requireContext()));
        iconListView.setLayoutManager(new LinearLayoutManager(mContext));
        iconListView.seslSetFillBottomEnabled(true);
        iconListView.seslSetLastRoundedCorner(true);
        iconListView.seslSetFastScrollerEnabled(true);
        iconListView.seslSetGoToTopEnabled(true);
        iconListView.seslSetSmoothScrollEnabled(true);
    }

    private void setupSelection(RecyclerView iconListView, ImageAdapter adapter){
        DrawerLayout drawerLayout = ((MainActivity)getActivity()).getDrawerLayout();
        adapter.configure(
                (RecyclerView) getView(),
                null,
                adapter::getItem,
                ass -> {drawerLayout.setActionModeAllSelector(ass.totalSelected, ass.isEnabled, ass.isChecked); return null;}
        );
    }

    private void setupAdapterClickListeners(RecyclerView iconListView, ImageAdapter adapter){
        adapter.onItemClickListener = new ImageAdapter.OnItemClickListener() {

            @Override
            public void onItemClick(Integer iconId, int position) {
                if (adapter.isActionMode()) {
                    adapter.onToggleItem(iconId, position);
                }else {
                    toast(getResources().getResourceEntryName(iconId) + " clicked!");
                }
            }

            @Override
            public void onItemLongClick(Integer iconId, int position) {
                if (!adapter.isActionMode()){
                    launchActionMode(adapter);
                }
                iconListView.seslStartLongPressMultiSelection();
            }
        };
    }

    private void launchActionMode(ImageAdapter adapter) {
        adapter.onToggleActionMode(true, null);

        DrawerLayout drawerLayout = ((MainActivity)getActivity()).getDrawerLayout();
        drawerLayout.startActionMode(new ToolbarLayout.ActionModeListener() {
            @Override
            public void onInflateActionMenu(@NonNull Menu menu) {
                requireActivity().getMenuInflater().inflate(R.menu.menu_action_mode_icons, menu);
            }

            @Override
            public void onEndActionMode() {
                adapter.onToggleActionMode(false, null);
            }

            @Override
            public boolean onMenuItemClicked(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.icons_am_menu1
                        || item.getItemId() == R.id.icons_am_menu2) {
                    toast(item.getTitle().toString());
                    return true;
                }
                return false;
            }

            @Override
            public void onSelectAll(boolean isChecked) {
                adapter.onToggleSelectAll(isChecked);
            }
        });
    }



    private void toast(String msg) {
        Toast.makeText(mContext,  msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public int getLayoutResId() {
        return R.layout.sample3_fragment_icons;
    }

    @Override
    public int getIconResId() {
        return dev.oneuiproject.oneui.R.drawable.ic_oui_emoji_2;
    }

    @Override
    public CharSequence getTitle() {
        return "Icons";
    }

}
