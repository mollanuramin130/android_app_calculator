package com.nuramin.calculator.adapter;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.nuramin.calculator.model.HistoryEntry;
import com.nuramin.sunsetcoralcalculator.R;

import java.util.List;

/**
 * Adapter for calculation history list. Shows expression, result, optional timestamp. Notifies when user taps "USE".
 */
public class HistoryAdapter extends ArrayAdapter<HistoryEntry> {

    public interface OnUseResultListener {
        void onUseResult(String resultValue);
    }

    private final OnUseResultListener listener;

    public HistoryAdapter(Context context, List<HistoryEntry> items, OnUseResultListener listener) {
        super(context, R.layout.basic_list_item_calc_history, items);
        this.listener = listener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView != null ? convertView : LayoutInflater.from(getContext()).inflate(R.layout.basic_list_item_calc_history, parent, false);
        HistoryEntry item = getItem(position);
        String exprPart = item != null ? item.getExpression() : "";
        String resultPart = item != null ? item.getResult() : "";
        Long timestamp = item != null ? item.getTimestamp() : null;

        TextView exprView = row.findViewById(R.id.history_expression);
        TextView resultView = row.findViewById(R.id.history_result);
        TextView timeView = row.findViewById(R.id.history_timestamp);
        Button useBtn = row.findViewById(R.id.history_btn_use);
        if (exprView != null) exprView.setText(exprPart);
        if (resultView != null) resultView.setText(resultPart);
        if (timeView != null) {
            if (timestamp != null && timestamp > 0) {
                timeView.setVisibility(View.VISIBLE);
                CharSequence formatted = DateFormat.format("MMM d, yyyy  h:mm a", timestamp);
                timeView.setText(formatted);
            } else {
                timeView.setVisibility(View.GONE);
            }
        }
        if (useBtn != null && listener != null) {
            final String resultToUse = resultPart.replace(",", "");
            useBtn.setOnClickListener(v -> listener.onUseResult(resultToUse));
        }
        return row;
    }
}
