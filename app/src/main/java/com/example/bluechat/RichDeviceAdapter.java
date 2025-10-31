package com.example.bluechat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

public class RichDeviceAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<RichDeviceAdapter.ViewHolder> {
    private final List<DeviceItem> items = new ArrayList<>();
    private final LayoutInflater inflater;
    private final Context context;
    private OnDeviceClickListener onDeviceClickListener;

    public interface OnDeviceClickListener {
        void onDeviceClick(DeviceItem device);
    }

    public void setOnDeviceClickListener(OnDeviceClickListener listener) {
        this.onDeviceClickListener = listener;
    }

    public RichDeviceAdapter(Context ctx) {
        this.inflater = LayoutInflater.from(ctx);
        this.context = ctx;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void clear() {
        items.clear();
        notifyDataSetChanged();
    }

    public boolean containsAddress(String address) {
        if (address == null) return false;
        for (DeviceItem it : items) {
            if (address.equals(it.address)) return true;
        }
        return false;
    }

    public void addOrUpdate(DeviceItem item) {
        int idx = -1;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).address.equals(item.address)) { idx = i; break; }
        }
        if (idx >= 0) {
            items.set(idx, item);
            notifyItemChanged(idx);
        } else {
            items.add(item);
            notifyItemInserted(items.size() - 1);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.row_device_rich, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        DeviceItem it = items.get(position);
        // Map icon tag to vector drawable
        int iconRes = R.drawable.ic_phone;
        if ("💻".equals(it.icon)) iconRes = R.drawable.ic_laptop;
        else if ("🔊".equals(it.icon)) iconRes = R.drawable.ic_speaker;
        else iconRes = R.drawable.ic_phone;
        holder.icon.setImageResource(iconRes);
        String displayName = it.nickname != null && !it.nickname.trim().isEmpty() ? it.nickname : it.name;
        holder.name.setText(displayName);
        if (it.rssi != Short.MIN_VALUE) {
            holder.sub.setText(it.rssi + " dBm • " + it.address);
        } else {
            holder.sub.setText(it.address);
        }
        holder.badge.setVisibility(it.paired ? View.VISIBLE : View.GONE);

        // RSSI bars coloring
        int activeBars = 0;
        if (it.rssi != Short.MIN_VALUE) {
            if (it.rssi >= -55) activeBars = 5;
            else if (it.rssi >= -65) activeBars = 4;
            else if (it.rssi >= -75) activeBars = 3;
            else if (it.rssi >= -85) activeBars = 2;
            else activeBars = 1;
        }
        int onColor = context.getResources().getColor(R.color.colorSuccess);
        int offColor = context.getResources().getColor(R.color.colorBorder);
        for (int i = 0; i < holder.bars.length; i++) {
            holder.bars[i].setBackgroundColor(i < activeBars ? onColor : offColor);
        }

        // Row fade-in animation
        try {
            Animation fade = AnimationUtils.loadAnimation(context, R.anim.fade_in);
            if (fade != null) {
                holder.itemView.startAnimation(fade);
            }
        } catch (Exception ignored) {}

        holder.itemView.setOnClickListener(v -> {
            if (onDeviceClickListener != null) {
                onDeviceClickListener.onDeviceClick(it);
            }
        });
    }

    public static class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        ImageView icon;
        TextView name;
        TextView sub;
        TextView badge;
        View[] bars;

        public ViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.icon);
            name = itemView.findViewById(R.id.name);
            sub = itemView.findViewById(R.id.sub);
            badge = itemView.findViewById(R.id.paired_badge);
            bars = new View[] {
                itemView.findViewById(R.id.bar1),
                itemView.findViewById(R.id.bar2),
                itemView.findViewById(R.id.bar3),
                itemView.findViewById(R.id.bar4),
                itemView.findViewById(R.id.bar5)
            };
        }
    }
}
