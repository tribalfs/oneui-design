package dev.oneuiproject.oneuiexample.ui.fragment.icons.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ScatterSet;
import androidx.recyclerview.widget.RecyclerView;

import com.sec.sesl.tester.R;

import java.util.ArrayList;
import java.util.List;

import dev.oneuiproject.oneui.delegates.AllSelectorState;
import dev.oneuiproject.oneui.delegates.MultiSelector;
import dev.oneuiproject.oneui.delegates.MultiSelectorDelegate;
import dev.oneuiproject.oneui.delegates.SectionIndexerDelegate;
import dev.oneuiproject.oneui.utils.SearchHighlighter;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class IconsAdapter extends RecyclerView.Adapter<IconsAdapter.ViewHolder>
        implements SectionIndexer, MultiSelector<Integer> {

    private final List<Integer> mIconsId = new ArrayList<>();
    private final List<Integer> filteredIconsId = new ArrayList<>();  // For filtered data
    private final Context mContext;

    private final MultiSelectorDelegate<Integer> mSelectionDelegate = new MultiSelectorDelegate<>();
    private final SectionIndexerDelegate<Integer> indexerDelegate;

    private final SearchHighlighter highlighter = new SearchHighlighter();
    private String query = "";

    public interface OnItemClickListener {
        void onItemClick(Integer iconId, int position);
        void onItemLongClick(Integer iconId, int position);
    }

    public OnItemClickListener onItemClickListener = null;

    public IconsAdapter(Context context) {
        mContext = context;
        indexerDelegate = new SectionIndexerDelegate<>(context, this::getIndexerLabel);
    }

    private String getIndexerLabel(Integer iconId) {
        char firstChar = mContext.getResources().getResourceEntryName(iconId)
                .replace("ic_oui_", "").charAt(0);
        if (Character.isDigit(firstChar)) {
            return "#";
        }
        return Character.toString(firstChar).toUpperCase();
    }

    public void submitList(List<Integer> list) {
        mIconsId.clear();
        mIconsId.addAll(list);
        filteredIconsId.clear();
        filteredIconsId.addAll(list);  // Initialize filtered list with all items
        notifyDataSetChanged();
        indexerDelegate.updateSections(list, false);
        updateSelectableIds(filteredIconsId);
    }

    // Filtering method
    public void filter(String query) {
        if (this.query.equals(query)) return;
        this.query = query;

        filteredIconsId.clear();
        if (query == null || query.isEmpty()) {
            filteredIconsId.addAll(mIconsId);  // No filter applied, show all items
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (Integer iconId : mIconsId) {
                String iconName = mContext.getResources().getResourceEntryName(iconId).toLowerCase();
                if (iconName.contains(lowerCaseQuery)) {
                    filteredIconsId.add(iconId);  // Add matching items to filtered list
                }
            }
        }
        notifyDataSetChanged();
        indexerDelegate.updateSections(filteredIconsId, false);
    }

    public int getItem(int position) {
        return filteredIconsId.get(position);
    }

    @Override
    public int getItemCount() {
        return filteredIconsId.size();
    }

    @NonNull
    @Override
    public IconsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.sample3_view_icon_listview_item, parent, false);
        ViewHolder vh = new ViewHolder(view);
        vh.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(filteredIconsId.get(vh.getBindingAdapterPosition()),
                        vh.getBindingAdapterPosition());
            }
        });
        vh.itemView.setOnLongClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemLongClick(filteredIconsId.get(vh.getBindingAdapterPosition()),
                        vh.getBindingAdapterPosition());
            }
            return true;
        });
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Integer iconId = filteredIconsId.get(position);
        holder.imageView.setImageResource(iconId);
        holder.textView.setText(highlighter.invoke(mContext.getResources().getResourceEntryName(iconId), query));
        holder.checkBox.setVisibility(isActionMode() ? View.VISIBLE : View.GONE);
        holder.checkBox.setChecked(isSelected(iconId));
    }

    // Implement SectionIndexer methods
    @Override
    public Object[] getSections() {
        return indexerDelegate.getSections();
    }

    @Override
    public int getPositionForSection(int sectionIndex) {
        return indexerDelegate.getPositionForSection(sectionIndex);
    }

    @Override
    public int getSectionForPosition(int position) {
        return indexerDelegate.getSectionForPosition(position);
    }

    // MultiSelector interface methods (delegate implementation)
    @Override
    public void configure(@NonNull RecyclerView recyclerView,
                          @Nullable Object selectionChangePayload,
                          @Nullable Function1<? super Integer, ? extends Integer> selectionId,
                          @NonNull Function1<? super AllSelectorState, Unit> allSelectorState) {
        mSelectionDelegate.configure(recyclerView, selectionChangePayload, selectionId, allSelectorState);
    }

    @Override
    public void updateSelectableIds(@NonNull List<? extends Integer> selectableIds) {
        mSelectionDelegate.updateSelectableIds(selectableIds);
    }

    @Override
    public void onToggleActionMode(boolean isActionMode, @Nullable Integer[] initialSelectedIds) {
        mSelectionDelegate.onToggleActionMode(isActionMode, initialSelectedIds);
    }

    @Override
    public void onToggleItem(Integer selectionId, int position) {
        mSelectionDelegate.onToggleItem(selectionId, position);
    }

    @Override
    public void onToggleSelectAll(boolean isSelectAll) {
        mSelectionDelegate.onToggleSelectAll(isSelectAll);
    }

    @Override
    public boolean isSelected(Integer selectionId) {
        return mSelectionDelegate.isSelected(selectionId);
    }

    @NonNull
    @Override
    public ScatterSet<Integer> getSelectedIds() {
        return mSelectionDelegate.getSelectedIds();
    }

    @Override
    public boolean isActionMode() {
        return mSelectionDelegate.isActionMode();
    }

    // ViewHolder class
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView textView;
        CheckBox checkBox;

        ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.icon_list_item_icon);
            textView = itemView.findViewById(R.id.icon_list_item_text);
            checkBox = itemView.findViewById(R.id.icon_list_item_checkbox);
        }
    }
}
