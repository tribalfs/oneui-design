package dev.oneuiproject.oneuiexample.ui.main.core.drawer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.sec.sesl.tester.R;

import java.util.List;

public class DrawerListAdapter extends RecyclerView.Adapter<DrawerListViewHolder> {
    private Context mContext;
    private List<DrawerItem> mFragmentResources;
    private DrawerListener mListener;
    private int mSelectedPos = -1;
    private float offsetApplied = 1f;

    public interface DrawerListener {
        boolean onDrawerItemSelected(DrawerItem drawerItem);
    }

    public DrawerListAdapter(@NonNull Context context, List<DrawerItem> fragmentResources,
                             @Nullable DrawerListener listener) {
        mContext = context;
        mFragmentResources = fragmentResources;
        mListener = listener;
    }

    public void setOffset(float offset){
        if (offsetApplied == offset)return;
        offsetApplied = offset;
        notifyItemRangeChanged(0, getItemCount());
    }

    @NonNull
    @Override
    public DrawerListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);

        final boolean isSeparator = viewType == 0;
        View view;
        if (isSeparator) {
            view = inflater.inflate(
                    R.layout.sample3_view_drawer_list_separator, parent, false);
        } else {
            view = inflater.inflate(
                    R.layout.sample3_view_drawer_list_item, parent, false);
        }

        return new DrawerListViewHolder(view, isSeparator);
    }

    @Override
    public void onBindViewHolder(@NonNull DrawerListViewHolder holder, int position) {
        holder.applyOffset(offsetApplied);
        if (!holder.isSeparator()) {
            DrawerItem drawerItem = mFragmentResources.get(position);
            holder.setIcon(drawerItem.getIconResId());
            holder.setTitle(drawerItem.getTitle());
            holder.setSelected(position == mSelectedPos);

            holder.itemView.setOnClickListener(v -> {
                final int itemPos = holder.getBindingAdapterPosition();
                setSelectedItem(itemPos);
            });
        }
    }

    @Override
    public int getItemCount() {
        return mFragmentResources.size();
    }

    @Override
    public int getItemViewType(int position) {
        return (mFragmentResources.get(position) == null) ? 0 : 1;
    }

    public void setSelectedItem(int position) {
        if (position == mSelectedPos) return;
        DrawerItem drawerItem = mFragmentResources.get(position);
        boolean result = false;
        if (drawerItem != null) {
            if (mListener != null) {
                result = mListener.onDrawerItemSelected(drawerItem);
            }
            if (result) {
                mSelectedPos = position;
                notifyItemRangeChanged(0, getItemCount());
            }
        }
    }

    public int getSelectedPosition(){
        return mSelectedPos;
    }
}
