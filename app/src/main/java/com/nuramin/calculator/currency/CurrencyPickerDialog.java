package com.nuramin.calculator.currency;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nuramin.sunsetcoralcalculator.R;

import java.util.List;

/** Full-screen friendly dialog with search + RecyclerView currency list. */
public final class CurrencyPickerDialog {

    private CurrencyPickerDialog() {}

    public static void show(
            @NonNull Context context,
            @NonNull List<CurrencyItem> items,
            @Nullable String title,
            @NonNull CurrencyPickerAdapter.OnPickListener onPick) {
        Context themed = context;
        View root = LayoutInflater.from(themed).inflate(R.layout.dialog_currency_picker, null, false);
        EditText search = root.findViewById(R.id.currency_picker_search);
        RecyclerView list = root.findViewById(R.id.currency_picker_list);

        final AlertDialog[] dialogRef = new AlertDialog[1];
        CurrencyPickerAdapter adapter = new CurrencyPickerAdapter(themed, items, item -> {
            onPick.onPick(item);
            if (dialogRef[0] != null) {
                dialogRef[0].dismiss();
            }
        });
        list.setLayoutManager(new LinearLayoutManager(themed));
        list.setAdapter(adapter);
        list.setHasFixedSize(true);

        AlertDialog dialog = new AlertDialog.Builder(themed)
                .setTitle(title != null ? title : themed.getString(R.string.currency_picker_title))
                .setView(root)
                .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
                .create();
        dialogRef[0] = dialog;

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                adapter.getFilter().filter(s != null ? s.toString() : "");
            }
        });

        dialog.setOnShowListener(d -> {
            if (search.requestFocus()) {
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            }
        });

        dialog.show();
    }
}
