package dev.oneuiproject.oneuiexample.ui.fragment.icons;

import static dev.oneuiproject.oneui.layout.ToolbarLayout.SearchModeOnBackBehavior.CLEAR_DISMISS;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.SeslMenuItem;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuProvider;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver;

import com.sec.sesl.tester.R;

import dev.oneuiproject.oneui.delegates.AppBarAwareYTranslator;
import dev.oneuiproject.oneui.delegates.ViewYTranslator;
import dev.oneuiproject.oneui.layout.DrawerLayout;
import dev.oneuiproject.oneui.layout.ToolbarLayout;
import dev.oneuiproject.oneui.widget.Toast;
import dev.oneuiproject.oneuiexample.ui.activity.MainActivity;
import dev.oneuiproject.oneuiexample.ui.core.base.BaseFragment;
import dev.oneuiproject.oneuiexample.data.IconsRepo;
import dev.oneuiproject.oneuiexample.ui.fragment.icons.adapter.IconsAdapter;
import dev.oneuiproject.oneuiexample.ui.core.ItemDecoration;

public class IconsFragment extends BaseFragment {

    private DrawerLayout drawerLayout;
    private IconsAdapter adapter;
    AdapterDataObserver observer;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        drawerLayout = ((MainActivity)getActivity()).getDrawerLayout();
        adapter = new IconsAdapter(getContext());
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (!hidden) {
            adapter.registerAdapterDataObserver(observer);
            requireActivity().addMenuProvider(menuProvider, getViewLifecycleOwner(), Lifecycle.State.STARTED);
        }else{
            adapter.unregisterAdapterDataObserver(observer);
            requireActivity().removeMenuProvider(menuProvider);
        }
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView iconListView = getView().findViewById(R.id.recyclerView);

        setupRecyclerView(iconListView, adapter);
        setupSelection(iconListView, adapter);
        setupAdapterClickListeners(iconListView, adapter);

        IconsRepo iconsRepo = new IconsRepo();
        adapter.submitList(iconsRepo.getIcons());
    }

    private void setupRecyclerView(RecyclerView iconListView, IconsAdapter adapter){
        iconListView.setItemAnimator(null);
        iconListView.setAdapter(adapter);
        iconListView.addItemDecoration(new ItemDecoration(requireContext()));
        iconListView.setLayoutManager(new LinearLayoutManager(mContext));
        iconListView.seslSetFillBottomEnabled(true);
        iconListView.seslSetLastRoundedCorner(true);
        iconListView.seslSetFastScrollerEnabled(true);
        iconListView.seslSetGoToTopEnabled(true);
        iconListView.seslSetSmoothScrollEnabled(true);
        iconListView.seslSetIndexTipEnabled(true);

        observer = new AdapterDataObserver() {
            RecyclerView iconListView = getView().findViewById(R.id.recyclerView);
            NestedScrollView notItemView = getView().findViewById(R.id.nsvNoItem);
            @Override
            public void onChanged() {
                if(adapter.getItemCount() > 0){
                    iconListView.setVisibility(View.VISIBLE);
                    notItemView.setVisibility(View.GONE);
                }else{
                    iconListView.setVisibility(View.GONE);
                    notItemView.setVisibility(View.VISIBLE);
                }
            }
        };
    }

    private void setupSelection(RecyclerView iconListView, IconsAdapter adapter){
        adapter.configure(
                iconListView,
                null,
                adapter::getItem,
                ass -> {drawerLayout.setActionModeAllSelector(ass.totalSelected, ass.isEnabled, ass.isChecked); return null;}
        );
    }

    private void setupAdapterClickListeners(RecyclerView iconListView, IconsAdapter adapter){
        adapter.onItemClickListener = new IconsAdapter.OnItemClickListener() {

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

    private void launchActionMode(IconsAdapter adapter) {
        adapter.onToggleActionMode(true, null);

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

    private void launchSearchMode(){
        RecyclerView iconListView = getView().findViewById(R.id.recyclerView);
        NestedScrollView notItemView = getView().findViewById(R.id.nsvNoItem);
        IconsAdapter adapter = (IconsAdapter) iconListView.getAdapter();

        final ViewYTranslator translatorDelegate = new AppBarAwareYTranslator();
        translatorDelegate.translateYWithAppBar(notItemView, drawerLayout.getAppBarLayout(), requireActivity());

        drawerLayout.startSearchMode(new ToolbarLayout.SearchModeListener() {
            @Override
            public boolean onQueryTextSubmit(@Nullable String query) {
                adapter.filter(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(@Nullable String newText) {
                adapter.filter(newText);
                return true;
            }

            @Override
            public void onSearchModeToggle(@NonNull SearchView searchView, boolean visible) {

            }
        }, CLEAR_DISMISS);
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

    private MenuProvider menuProvider = new MenuProvider() {
        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            menuInflater.inflate(R.menu.menu_icons, menu);

            MenuItem searchItem = menu.findItem(R.id.menu_icons_search);
            drawerLayout.setMenuItemBadge((SeslMenuItem) searchItem, new ToolbarLayout.Badge.Dot());
        }

        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
            if (menuItem.getItemId() == R.id.menu_icons_search) {
                launchSearchMode();
                return true;
            }
            return false;
        }
    };

}
