package com.nuramin.calculator.emi;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.nuramin.calculator.util.LocaleFormatManager;
import com.nuramin.sunsetcoralcalculator.R;

import java.util.List;
import java.util.Locale;

/**
 * Amortization table rows: month, principal paid, interest, outstanding balance.
 */
public final class EmiScheduleAdapter extends RecyclerView.Adapter<EmiScheduleAdapter.Holder> {

    private final Context context;
    private final List<EmiScheduleRow> rows;

    public EmiScheduleAdapter(Context context, List<EmiScheduleRow> rows) {
        this.context = context.getApplicationContext();
        this.rows = rows;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_emi_schedule_row, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        EmiScheduleRow row = rows.get(position);
        h.month.setText(String.format(Locale.getDefault(), "%d", row.month));
        h.principal.setText(LocaleFormatManager.formatCurrency(context, row.principalPart));
        h.interest.setText(LocaleFormatManager.formatCurrency(context, row.interestPart));
        h.balance.setText(LocaleFormatManager.formatCurrency(context, row.closingBalance));
        h.itemView.setBackgroundColor(
                position % 2 == 0 ? Color.TRANSPARENT : ContextCompat.getColor(context, R.color.emi_schedule_row_alt));
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView month;
        final TextView principal;
        final TextView interest;
        final TextView balance;

        Holder(@NonNull View itemView) {
            super(itemView);
            month = itemView.findViewById(R.id.emi_sch_month);
            principal = itemView.findViewById(R.id.emi_sch_principal);
            interest = itemView.findViewById(R.id.emi_sch_interest);
            balance = itemView.findViewById(R.id.emi_sch_balance);
        }
    }
}
