package com.example.bluechat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    private List<DeviceItem> devices = new ArrayList<>();
    private OnDeviceClickListener listener;

    public interface OnDeviceClickListener {
        void onDeviceClick(DeviceItem device);
    }

    public DeviceAdapter(OnDeviceClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device_search, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        DeviceItem device = devices.get(position);
        holder.bind(device);
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public void setDevices(List<DeviceItem> devices) {
        this.devices = devices;
        notifyDataSetChanged();
    }

    public void filter(String query) {
        // Implement filtering logic if needed
        notifyDataSetChanged();
    }

    class DeviceViewHolder extends RecyclerView.ViewHolder {
        private TextView deviceIcon, deviceName, deviceStatus, deviceSignal, deviceDistance;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceIcon = itemView.findViewById(R.id.device_icon);
            deviceName = itemView.findViewById(R.id.device_name);
            deviceStatus = itemView.findViewById(R.id.device_status);
            deviceSignal = itemView.findViewById(R.id.device_signal);
            deviceDistance = itemView.findViewById(R.id.device_distance);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onDeviceClick(devices.get(position));
                }
            });
        }

        public void bind(DeviceItem device) {
            deviceIcon.setText(device.getIcon());
            deviceName.setText(device.getName());
            deviceStatus.setText(device.isActive() ? "● " + itemView.getContext().getString(R.string.device_active) :
                    "○ " + itemView.getContext().getString(R.string.device_offline));
            deviceSignal.setText("📶");
            deviceDistance.setText(device.getDistance() + "m");

            // Set status color
            int statusColor = device.isActive() ? R.color.online_green : R.color.slate_400;
            deviceStatus.setTextColor(itemView.getContext().getColor(statusColor));

            // Set alpha for offline devices
            float alpha = device.isActive() ? 1.0f : 0.3f;
            deviceSignal.setAlpha(alpha);
        }
    }
}
