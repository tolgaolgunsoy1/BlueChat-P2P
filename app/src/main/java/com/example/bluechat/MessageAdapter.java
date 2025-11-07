package com.example.bluechat;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.vanniktech.emoji.EmojiTextView;

import java.text.DateFormat;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private static final int VIEW_TYPE_RECEIVED = 0;
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_TYPING = 2;
    private static final int VIEW_TYPE_IMAGE = 3;
    private static final int VIEW_TYPE_FILE = 4;

    private final List<Message> messages;
    private final DateFormat timeFormat;
    private final DateFormat dateHeaderFormat;
    private boolean showTyping;
    private OnMessageReactionListener reactionListener;

    public interface OnMessageReactionListener {
        void onReactionClicked(Message message, int position);
    }

    public MessageAdapter(List<Message> messages) {
        this.messages = messages;
        this.timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault());
        this.dateHeaderFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
    }

    public void setOnMessageReactionListener(OnMessageReactionListener listener) {
        this.reactionListener = listener;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_SENT) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
        } else if (viewType == VIEW_TYPE_TYPING) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_typing_indicator, parent, false);
        } else if (viewType == VIEW_TYPE_IMAGE) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_image, parent, false);
        } else if (viewType == VIEW_TYPE_FILE) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_file, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
        }
        return new MessageViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_TYPING) {
            // nothing else to bind
            return;
        }
        Message message = messages.get(position);
        String text = message.getText();
        if (text == null) text = "";
        holder.messageText.setText(text);
        holder.timestampText.setText(timeFormat.format(message.getTimestamp()));

        // Date separator logic
        boolean showDate = false;
        if (position == 0) {
            showDate = true;
        } else {
            long prevTime = messages.get(position - 1).getTimestamp().getTime();
            long currTime = message.getTimestamp().getTime();
            long diff = currTime - prevTime;
            showDate = diff >= 24 * 60 * 60 * 1000; // 24 hours in milliseconds
        }
        if (holder.dateSeparator != null) {
            if (showDate) {
                holder.dateSeparator.setText(dateHeaderFormat.format(message.getTimestamp()));
                holder.dateSeparator.setVisibility(View.VISIBLE);
            } else {
                holder.dateSeparator.setVisibility(View.GONE);
            }
        }

        // Long press copy to clipboard
        holder.itemView.setOnLongClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("message", message.getText()));
                android.widget.Toast.makeText(v.getContext(), R.string.copied_to_clipboard, android.widget.Toast.LENGTH_SHORT).show();
            }
            return true;
        });

        // Group spacing when sender changes
        if (position > 0) {
            boolean prevSent = messages.get(position - 1).isSent();
            boolean currSent = message.isSent();
            int top = (prevSent == currSent) ? 4 : 12; // dp
            View bubble = holder.messageText;
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) bubble.getLayoutParams();
            int topPx = (int) (top * bubble.getResources().getDisplayMetrics().density);
            lp.topMargin = topPx;
            bubble.setLayoutParams(lp);
        }
        try {
            int anim = message.isSent() ? R.anim.slide_in_right : R.anim.slide_in_left;
            android.view.animation.Animation a = android.view.animation.AnimationUtils.loadAnimation(holder.itemView.getContext(), anim);
            holder.itemView.startAnimation(a);
        } catch (Exception e) {
            Log.e("MessageAdapter", "Animation loading failed", e);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size() + (showTyping ? 1 : 0);
    }

    @Override
    public int getItemViewType(int position) {
        if (showTyping && position == messages.size()) return VIEW_TYPE_TYPING;

        Message message = messages.get(position);
        if (message.getMessageType() != null) {
            switch (message.getMessageType()) {
                case "image":
                    return message.isSent() ? VIEW_TYPE_IMAGE : VIEW_TYPE_IMAGE;
                case "file":
                    return VIEW_TYPE_FILE;
            }
        }
        return message.isSent() ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
    }

    public void addMessage(Message message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    public void setTyping(boolean typing) {
        if (this.showTyping == typing) return;
        this.showTyping = typing;
        if (typing) {
            notifyItemInserted(messages.size());
        } else {
            notifyItemRemoved(messages.size());
        }
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView timestampText;
        TextView dateSeparator;
        ImageView messageImage;
        TextView fileNameText;
        TextView reactionsText;
        ImageView reactionButton;

        MessageViewHolder(View itemView, int viewType) {
            super(itemView);
            if (viewType != VIEW_TYPE_TYPING) {
                messageText = itemView.findViewById(R.id.message_text);
                timestampText = itemView.findViewById(R.id.timestamp_text);
                dateSeparator = itemView.findViewById(R.id.date_separator);

                // Optional views for different message types
                messageImage = itemView.findViewById(R.id.message_image);
                fileNameText = itemView.findViewById(R.id.file_name_text);
                reactionsText = itemView.findViewById(R.id.reactions_text);
                reactionButton = itemView.findViewById(R.id.reaction_button);
            }
        }
    }
}
