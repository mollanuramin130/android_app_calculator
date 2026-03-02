package com.nuramin.calculator.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.nuramin.calculator.R;

import java.util.List;

/**
 * Adapter for calculation history list. Notifies listener when user taps "USE" on a result.
 */
public class HistoryAdapter extends ArrayAdapter<String> {

    public interface OnUseResultListener {
        void onUseResult(String resultValue);
    }

    private final OnUseResultListener listener;

    public HistoryAdapter(Context context, List<String> items, OnUseResultListener listener) {
        super(context, R.layout.basic_list_item_calc_history, items);
        this.listener = listener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView != null ? convertView : LayoutInflater.from(getContext()).inflate(R.layout.basic_list_item_calc_history, parent, false);
        String item = getItem(position);
        String exprPart = "";
        String resultPart = "";
        if (item != null) {
            int eq = item.indexOf("\n= ");
            if (eq >= 0) {
                exprPart = item.substring(0, eq);
                resultPart = item.substring(eq + 3);
            } else {
                exprPart = item;
            }
        }
        TextView exprView = row.findViewById(R.id.history_expression);
        TextView resultView = row.findViewById(R.id.history_result);
        TextView timeView = row.findViewById(R.id.history_timestamp);
        Button useBtn = row.findViewById(R.id.history_btn_use);
        if (exprView != null) exprView.setText(exprPart);
        if (resultView != null) resultView.setText(resultPart);
        if (timeView != null) timeView.setText("");
        if (useBtn != null && listener != null) {
            final String resultToUse = resultPart.replace(",", "");
            useBtn.setOnClickListener(v -> listener.onUseResult(resultToUse));
        }
        return row;
    }
}
