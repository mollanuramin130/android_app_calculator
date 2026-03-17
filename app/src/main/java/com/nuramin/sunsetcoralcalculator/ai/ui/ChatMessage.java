package com.nuramin.sunsetcoralcalculator.ai.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nuramin.sunsetcoralcalculator.ai.core.AIResult;

/** One entry in the AI chat: either user message or bot (AI) response. */
public final class ChatMessage {

    public static final int TYPE_USER = 0;
    public static final int TYPE_BOT = 1;

    private final int type;
    private final String userText;
    private final AIResult botResult;

    public ChatMessage(int type, @Nullable String userText, @Nullable AIResult botResult) {
        this.type = type;
        this.userText = userText != null ? userText : "";
        this.botResult = botResult;
    }

    public static ChatMessage user(@NonNull String text) {
        return new ChatMessage(TYPE_USER, text, null);
    }

    public static ChatMessage bot(@NonNull AIResult result) {
        return new ChatMessage(TYPE_BOT, null, result);
    }

    public int getType() { return type; }
    public String getUserText() { return userText; }
    public AIResult getBotResult() { return botResult; }
}
