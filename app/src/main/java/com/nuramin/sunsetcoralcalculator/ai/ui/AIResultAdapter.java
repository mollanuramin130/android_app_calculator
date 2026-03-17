package com.nuramin.sunsetcoralcalculator.ai.ui;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nuramin.sunsetcoralcalculator.R;
import com.nuramin.sunsetcoralcalculator.ai.core.AIResult;

/**
 * Binds an AIResult to the result card (title, result value, explanation).
 */
public final class AIResultAdapter {

    /**
     * Bind result to the card container. Card must contain ai_result_title, ai_result_value, ai_result_explanation.
     */
    public static void bindToCard(@Nullable AIResult result, @NonNull View cardRoot) {
        if (result == null) {
            cardRoot.setVisibility(View.GONE);
            return;
        }
        TextView title = cardRoot.findViewById(R.id.ai_result_title);
        TextView value = cardRoot.findViewById(R.id.ai_result_value);
        TextView explanation = cardRoot.findViewById(R.id.ai_result_explanation);
        String titleStr = result.getTitle() != null ? result.getTitle() : "";
        String valueStr = result.getResultText() != null ? result.getResultText() : "";
        String explanationStr = result.getExplanation() != null ? result.getExplanation() : "";
        if (title != null) {
            title.setText(titleStr);
            title.setVisibility(titleStr.isEmpty() ? View.GONE : View.VISIBLE);
        }
        if (value != null) {
            value.setText(valueStr);
            value.setVisibility(valueStr.isEmpty() ? View.GONE : View.VISIBLE);
        }
        if (explanation != null) {
            explanation.setText(explanationStr);
            explanation.setVisibility(explanationStr.isEmpty() ? View.GONE : View.VISIBLE);
        }
        cardRoot.setVisibility(View.VISIBLE);
    }
}
