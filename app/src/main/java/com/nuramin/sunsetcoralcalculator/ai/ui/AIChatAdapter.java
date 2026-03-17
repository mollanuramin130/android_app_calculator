package com.nuramin.sunsetcoralcalculator.ai.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nuramin.sunsetcoralcalculator.R;
import com.nuramin.sunsetcoralcalculator.ai.core.AIResult;

import java.util.ArrayList;
import java.util.List;

/** RecyclerView adapter for AI chat: user bubbles and bot result cards. */
public final class AIChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_USER = 0;
    private static final int VIEW_TYPE_BOT = 1;

    private final List<ChatMessage> messages = new ArrayList<>();

    public void addUserMessage(@NonNull String text) {
        messages.add(ChatMessage.user(text));
        notifyItemInserted(messages.size() - 1);
    }

    public void addBotMessage(@NonNull AIResult result) {
        messages.add(ChatMessage.bot(result));
        notifyItemInserted(messages.size() - 1);
    }

    public void clear() {
        int size = messages.size();
        messages.clear();
        if (size > 0) notifyItemRangeRemoved(0, size);
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getType() == ChatMessage.TYPE_USER ? VIEW_TYPE_USER : VIEW_TYPE_BOT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_USER) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_user, parent, false);
            return new UserHolder(v);
        }
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_bot, parent, false);
        return new BotHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);
        if (holder instanceof UserHolder) {
            ((UserHolder) holder).bind(msg.getUserText());
        } else if (holder instanceof BotHolder) {
            ((BotHolder) holder).bind(msg.getBotResult());
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    private static final class UserHolder extends RecyclerView.ViewHolder {
        private final TextView text;

        UserHolder(View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.chat_user_text);
        }

        void bind(String s) {
            text.setText(s);
        }
    }

    private static final class BotHolder extends RecyclerView.ViewHolder {
        private final View card;

        BotHolder(View itemView) {
            super(itemView);
            card = itemView;
        }

        void bind(AIResult result) {
            AIResultAdapter.bindToCard(result, card);
        }
    }
}
