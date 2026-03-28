package com.nuramin.calculator.emi;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nuramin.calculator.util.CalculatorUtils;
import com.nuramin.calculator.util.LocaleFormatManager;
import com.nuramin.sunsetcoralcalculator.R;

import java.util.List;

/**
 * Full EMI amortization schedule in a scrollable dialog.
 */
public final class EmiScheduleDialogHelper {

    private EmiScheduleDialogHelper() {}

    public static void show(Context context, @Nullable EmiLoanSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        List<EmiScheduleRow> rows = EmiCalculatorMath.buildSchedule(
                snapshot.principal, snapshot.annualRatePct, snapshot.months);
        if (rows.isEmpty()) {
            return;
        }
        Context app = context.getApplicationContext();
        View root = LayoutInflater.from(context).inflate(R.layout.dialog_emi_schedule, null);
        TextView summary = root.findViewById(R.id.emi_schedule_summary);
        TextView emiLine = root.findViewById(R.id.emi_schedule_emi_line);
        RecyclerView recycler = root.findViewById(R.id.emi_schedule_recycler);

        String loan = LocaleFormatManager.formatCurrency(app, snapshot.principal);
        String rateStr = CalculatorUtils.formatNumber(snapshot.annualRatePct) + "%";
        String tenureStr = context.getString(R.string.emi_schedule_tenure_months, snapshot.months);
        summary.setText(context.getString(R.string.emi_schedule_summary_line, loan, rateStr, tenureStr));

        double emi = EmiCalculatorMath.monthlyEmi(snapshot.principal, snapshot.annualRatePct, snapshot.months);
        String emiFormatted = LocaleFormatManager.formatCurrency(app, emi);
        emiLine.setText(context.getString(R.string.emi_schedule_emi_line, emiFormatted));

        recycler.setLayoutManager(new LinearLayoutManager(context));
        recycler.setAdapter(new EmiScheduleAdapter(context, rows));
        recycler.setNestedScrollingEnabled(true);

        ImageView overflow = root.findViewById(R.id.emi_schedule_overflow);
        if (overflow != null) {
            overflow.setOnClickListener(
                    v -> EmiScheduleExporter.showMenu(context, v, snapshot, rows));
        }

        new AlertDialog.Builder(context)
                .setView(root)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
