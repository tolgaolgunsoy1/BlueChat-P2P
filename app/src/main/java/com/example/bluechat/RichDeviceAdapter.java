package com.example.bluechat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Filter;
import android.widget.Filterable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RichDeviceAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<RichDeviceAdapter.ViewHolder> implements Filterable {
    private final List<DeviceItem> items = new ArrayList<>();
    private final List<DeviceItem> filteredItems = new ArrayList<>();
    private final LayoutInflater inflater;
    private final Context context;
    private OnDeviceClickListener onDeviceClickListener;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public interface OnDeviceClickListener {
        void onDeviceClick(DeviceItem device);
    }

    public void setOnDeviceClickListener(OnDeviceClickListener listener) {
        this.onDeviceClickListener = listener;
    }

    public RichDeviceAdapter(Context ctx) {
        this.inflater = LayoutInflater.from(ctx);
        this.context = ctx;
        this.filteredItems.addAll(items);
    }

    @Override
    public int getItemCount() {
        return filteredItems.size();
    }

    public void clear() {
        items.clear();
        filteredItems.clear();
        notifyDataSetChanged();
    }

    public boolean containsAddress(String address) {
        if (address == null) return false;
        for (DeviceItem it : items) {
            if (address.equals(it.getAddress())) return true;
        }
        return false;
    }

    public void addOrUpdate(DeviceItem item) {
        int idx = -1;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getAddress().equals(item.getAddress())) { idx = i; break; }
        }
        if (idx >= 0) {
            items.set(idx, item);
        } else {
            items.add(item);
        }
        getFilter().filter("");
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.row_device_rich, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        DeviceItem it = filteredItems.get(position);

        // Avatar initials and gradient
        String displayName = it.getNickname() != null && !it.getNickname().trim().isEmpty() ? it.getNickname() : it.getName();
        String initials = getInitials(displayName);
        holder.avatarInitials.setText(initials);
        int gradientRes = getGradientForName(displayName);
        holder.avatarBg.setBackgroundResource(gradientRes);

        // Online indicator for unpaired (newly discovered)
        holder.onlineIndicator.setVisibility(!it.isPaired() ? View.VISIBLE : View.GONE);

        holder.name.setText(displayName);
        holder.preview.setText(it.getPreview());
        holder.time.setText(it.getTime());

        // Badge for new devices or signal strength
        if (it.getBadgeCount() > 0) {
            holder.badge.setText(String.valueOf(it.getBadgeCount()));
            holder.badge.setVisibility(View.VISIBLE);
        } else {
            holder.badge.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (onDeviceClickListener != null) {
                onDeviceClickListener.onDeviceClick(it);
            }
        });
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
        } else {
            return name.substring(0, Math.min(2, name.length())).toUpperCase();
        }
    }

    private int getGradientForName(String name) {
        int hash = name.hashCode();
        int[] gradients = {
            R.drawable.avatar_gradient_blue,
            R.drawable.avatar_gradient_pink,
            R.drawable.avatar_gradient_purple,
            R.drawable.avatar_gradient_green,
            R.drawable.avatar_gradient_orange
        };
        return gradients[Math.abs(hash) % gradients.length];
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                String query = constraint.toString().toLowerCase();
                List<DeviceItem> filtered = new ArrayList<>();
                for (DeviceItem item : items) {
                    String displayName = item.getNickname() != null && !item.getNickname().trim().isEmpty() ? item.getNickname() : item.getName();
                    if (displayName.toLowerCase().contains(query) || item.getAddress().toLowerCase().contains(query)) {
                        filtered.add(item);
                    }
                }
                FilterResults results = new FilterResults();
                results.values = filtered;
                results.count = filtered.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredItems.clear();
                if (results.values != null) {
                    filteredItems.addAll((List<DeviceItem>) results.values);
                }
                notifyDataSetChanged();
            }
        };
    }

    public static class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        ImageView avatarBg;
        TextView avatarInitials;
        ImageView onlineIndicator;
        TextView name;
        TextView preview;
        TextView time;
        TextView badge;

        public ViewHolder(View itemView) {
            super(itemView);
            avatarBg = itemView.findViewById(R.id.avatar_bg);
            avatarInitials = itemView.findViewById(R.id.avatar_initials);
            onlineIndicator = itemView.findViewById(R.id.online_indicator);
            name = itemView.findViewById(R.id.name);
            preview = itemView.findViewById(R.id.preview);
            time = itemView.findViewById(R.id.time);
            badge = itemView.findViewById(R.id.badge);
        }
    }
}
