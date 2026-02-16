package com.example.jamiya;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.VH> {

    private List<ChatMessage> messages = new ArrayList<>();

    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ChatMessage msg = messages.get(position);
        holder.tvText.setText(msg.getText());

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.tvText.getLayoutParams();
        LinearLayout container = (LinearLayout) holder.itemView;

        if (msg.isUser()) {
            container.setGravity(Gravity.START); // Right in RTL
            holder.tvText.setBackgroundResource(R.drawable.bg_chat_bubble_user);
            holder.tvText.setTextColor(0xFF064E3B); // Dark Green
        } else {
            container.setGravity(Gravity.END); // Left in RTL
            holder.tvText.setBackgroundResource(R.drawable.bg_chat_bubble_ai);
            holder.tvText.setTextColor(0xFF1F2937); // Dark Gray
        }
        
        holder.tvText.setLayoutParams(params);
    }

    @Override
    public int getItemCount() { return messages.size(); }

    class VH extends RecyclerView.ViewHolder {
        TextView tvText;
        public VH(View itemView) {
            super(itemView);
            tvText = itemView.findViewById(R.id.tvMessageText);
        }
    }
}