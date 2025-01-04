package dev.oneuiproject.oneuiexample.ui.fragment.icons;

import static dev.oneuiproject.oneui.ktx.RecyclerViewKt.enableCoreSeslFeatures;
import static dev.oneuiproject.oneui.layout.ToolbarLayout.SearchModeOnBackBehavior.DISMISS;
import static dev.oneuiproject.oneuiexample.ui.core.ktx.ToastKt.toast;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

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
import dev.oneuiproject.oneui.layout.Badge;
import dev.oneuiproject.oneui.layout.DrawerLayout;
import dev.oneuiproject.oneui.layout.ToolbarLayout;
import dev.oneuiproject.oneui.layout.ToolbarLayout.SearchOnActionMode;
import dev.oneuiproject.oneui.utils.ItemDecorRule;
import dev.oneuiproject.oneui.utils.SemItemDecoration;
import dev.oneuiproject.oneui.widget.TipPopup;
import dev.oneuiproject.oneuiexample.ui.activity.MainActivity;
import dev.oneuiproject.oneuiexample.ui.core.base.BaseFragment;
import dev.oneuiproject.oneuiexample.data.IconsRepo;
import dev.oneuiproject.oneuiexample.ui.fragment.icons.adapter.IconsAdapter;

public class IconsFragment extends BaseFragment {

    private DrawerLayout drawerLayout;
    private IconsAdapter adapter;

    private AdapterDataObserver observer = new AdapterDataObserver() {
        @Override
        public void onChanged() {
            RecyclerView iconListView = getView().findViewById(R.id.recyclerView);
            NestedScrollView notItemView = getView().findViewById(R.id.nsvNoItem);
            if(adapter.getItemCount() > 0){
                iconListView.setVisibility(View.VISIBLE);
                notItemView.setVisibility(View.GONE);
            }else{
                iconListView.setVisibility(View.GONE);
                notItemView.setVisibility(View.VISIBLE);
            }
        }
    };

    private boolean tipPopupShown = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        drawerLayout = ((MainActivity)getActivity()).getDrawerLayout();
        adapter = new IconsAdapter(getContext());
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (!hidden) {
            requireActivity().addMenuProvider(menuProvider, getViewLifecycleOwner(), Lifecycle.State.STARTED);
            showTipPopup();
        }else{
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
        adapter.registerAdapterDataObserver(observer);
    }

    @Override
    public void onDestroyView() {
        adapter.unregisterAdapterDataObserver(observer);
        super.onDestroyView();
    }

    private void setupRecyclerView(RecyclerView iconListView, IconsAdapter adapter){
        SemItemDecoration itemDecoration = new SemItemDecoration(
                requireContext(),
                ItemDecorRule.ALL.INSTANCE,
                ItemDecorRule.NONE.INSTANCE
        );

        iconListView.setItemAnimator(null);
        iconListView.setAdapter(adapter);
        iconListView.addItemDecoration(itemDecoration);
        iconListView.setLayoutManager(new LinearLayoutManager(mContext));
        enableCoreSeslFeatures(iconListView);
    }

    private void setupSelection(RecyclerView iconListView, IconsAdapter adapter){
        adapter.configure(
                iconListView,
                null,
                adapter::getItem,
                ass -> {drawerLayout.updateAllSelector(ass.totalSelected, ass.isEnabled, ass.isChecked); return null;}
        );
    }

    private void setupAdapterClickListeners(RecyclerView iconListView, IconsAdapter adapter){
        adapter.onItemClickListener = new IconsAdapter.OnItemClickListener() {

            @Override
            public void onItemClick(Integer iconId, int position) {
                if (adapter.isActionMode()) {
                    adapter.onToggleItem(iconId, position);
                }else {
                    toast(IconsFragment.this, getResources().getResourceEntryName(iconId) + " clicked!");
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

    private ToolbarLayout.SearchModeListener searchModeListener = new ToolbarLayout.SearchModeListener() {
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
            if (visible) {
                searchView.setQueryHint( "Search icons");
            }else{
                adapter.filter("");
            }
        }
    };

    private void launchActionMode(IconsAdapter adapter) {

        drawerLayout.startActionMode(
                new ToolbarLayout.ActionModeListener() {
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
                            toast(IconsFragment.this, item.getTitle().toString());
                            return true;
                        }
                        return false;
                    }

                    @Override
                    public void onSelectAll(boolean isChecked) {
                        adapter.onToggleSelectAll(isChecked);
                    }
                },
                new SearchOnActionMode.Concurrent(searchModeListener)
        );

        adapter.onToggleActionMode(true, null);
    }

    private void launchSearchMode(){
        NestedScrollView notItemView = getView().findViewById(R.id.nsvNoItem);

        final ViewYTranslator translatorDelegate = new AppBarAwareYTranslator();
        translatorDelegate.translateYWithAppBar(notItemView, drawerLayout.getAppBarLayout(), requireActivity());

        drawerLayout.startSearchMode(searchModeListener, DISMISS);
    }


    private void showTipPopup(){
        if (!tipPopupShown) {
            RecyclerView iconListView = getView().findViewById(R.id.recyclerView);
            iconListView.postDelayed(() -> {
                View anchor = iconListView.getLayoutManager().findViewByPosition(0);
                if (anchor != null) {
                    TipPopup tipPopup = new TipPopup(anchor, TipPopup.Mode.TRANSLUCENT);
                    tipPopup.setMessage("Long-press item to trigger multi-selection.");
                    tipPopup.setAction("Close", view -> tipPopupShown = true);
                    tipPopup.setExpanded(true);
                    tipPopup.show(TipPopup.Direction.DEFAULT);
                }
            }, 500);
        }
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
            drawerLayout.setMenuItemBadge((SeslMenuItem) searchItem, Badge.DOT.INSTANCE);
        }

        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
            if (menuItem.getItemId() == R.id.menu_icons_search) {
                launchSearchMode();
                drawerLayout.setMenuItemBadge((SeslMenuItem) menuItem, Badge.NONE.INSTANCE);
                return true;
            }
            return false;
        }
    };

}
