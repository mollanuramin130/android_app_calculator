package com.nuramin.calculator.adapter;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
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

    public interface OnUseExpressionListener {
        void onUseExpression(String expression);
    }

    public interface OnDismissListener {
        void onDismiss(int position);
    }

    /** 0 = Recent (USE + dismiss), 1 = Old (USE result + USE expression, no dismiss). */
    private int currentHistoryTab = 0;

    private final OnUseResultListener useListener;
    private final OnUseExpressionListener useExpressionListener;
    private final OnDismissListener dismissListener;

    public HistoryAdapter(Context context, List<HistoryEntry> items,
                          OnUseResultListener useListener,
                          OnUseExpressionListener useExpressionListener,
                          OnDismissListener dismissListener) {
        super(context, R.layout.basic_list_item_calc_history, items);
        this.useListener = useListener;
        this.useExpressionListener = useExpressionListener;
        this.dismissListener = dismissListener;
    }

    public void setCurrentHistoryTab(int tab) {
        if (currentHistoryTab != tab) {
            currentHistoryTab = tab;
            notifyDataSetChanged();
        }
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
        ImageButton useBtn = row.findViewById(R.id.history_btn_use);
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
        boolean isOldTab = (currentHistoryTab == 1);
        if (useBtn != null && useListener != null) {
            final String resultToUse = resultPart.replace(",", "");
            useBtn.setOnClickListener(v -> useListener.onUseResult(resultToUse));
        }
        ImageButton useExprBtn = row.findViewById(R.id.history_btn_use_expr);
        if (useExprBtn != null && useExpressionListener != null) {
            final String exprToUse = exprPart;
            useExprBtn.setOnClickListener(v -> useExpressionListener.onUseExpression(exprToUse));
            useExprBtn.setVisibility(isOldTab ? View.VISIBLE : View.GONE);
        }
        ImageButton dismissBtn = row.findViewById(R.id.history_btn_dismiss);
        if (dismissBtn != null && dismissListener != null) {
            final int pos = position;
            dismissBtn.setOnClickListener(v -> dismissListener.onDismiss(pos));
            dismissBtn.setVisibility(isOldTab ? View.GONE : View.VISIBLE);
        }
        return row;
    }
}
