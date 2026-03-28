package com.nuramin.calculator.currency;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nuramin.sunsetcoralcalculator.R;

import java.util.List;

public final class CurrencySpinnerAdapter extends ArrayAdapter<CurrencyItem> {

    public CurrencySpinnerAdapter(@NonNull Context context, @NonNull List<CurrencyItem> items) {
        super(context, R.layout.item_currency_spinner_row, items);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return bind(position, parent, false);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return bind(position, parent, true);
    }

    private View bind(int position, @NonNull ViewGroup parent, boolean dropdown) {
        CurrencyItem item = getItem(position);
        if (item == null) {
            return super.getView(position, null, parent);
        }
        int layout = dropdown ? R.layout.item_currency_spinner_dropdown : R.layout.item_currency_spinner_row;
        TextView tv = (TextView) LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        tv.setText(dropdown ? item.getDropdownLabel() : item.getSpinnerLabel());
        return tv;
    }
}
