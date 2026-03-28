package com.nuramin.calculator.currency;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nuramin.sunsetcoralcalculator.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Searchable list of currencies (code, name, country) with flag drawables.
 */
public final class CurrencyPickerAdapter extends RecyclerView.Adapter<CurrencyPickerAdapter.Holder>
        implements Filterable {

    public interface OnPickListener {
        void onPick(@NonNull CurrencyItem item);
    }

    private final Context context;
    private final List<CurrencyItem> all;
    private final List<CurrencyItem> visible = new ArrayList<>();
    private final OnPickListener listener;
    private Filter filter;

    public CurrencyPickerAdapter(
            @NonNull Context context,
            @NonNull List<CurrencyItem> allItems,
            @NonNull OnPickListener listener) {
        this.context = context.getApplicationContext();
        this.all = new ArrayList<>(allItems);
        this.visible.addAll(this.all);
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_currency_picker_row, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        CurrencyItem item = visible.get(position);
        h.code.setText(item.getCode());
        h.subtitle.setText(item.getDisplaySubtitle());
        CurrencySvgFlagLoader.loadInto(
                h.flag, item, R.dimen.currency_flag_render_width_picker, R.dimen.currency_flag_render_height_picker);
        h.itemView.setOnClickListener(v -> listener.onPick(item));
    }

    @Override
    public int getItemCount() {
        return visible.size();
    }

    @Override
    public Filter getFilter() {
        if (filter == null) {
            filter = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults fr = new FilterResults();
                    String q = constraint != null ? constraint.toString().trim().toLowerCase(Locale.ROOT) : "";
                    List<CurrencyItem> out = new ArrayList<>();
                    if (q.isEmpty()) {
                        out.addAll(all);
                    } else {
                        for (CurrencyItem c : all) {
                            if (c.getSearchText().contains(q)) {
                                out.add(c);
                            }
                        }
                    }
                    fr.values = out;
                    fr.count = out.size();
                    return fr;
                }

                @Override
                @SuppressWarnings("unchecked")
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    visible.clear();
                    if (results.values != null) {
                        visible.addAll((List<CurrencyItem>) results.values);
                    }
                    notifyDataSetChanged();
                }
            };
        }
        return filter;
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView flag;
        final TextView code;
        final TextView subtitle;

        Holder(@NonNull View itemView) {
            super(itemView);
            flag = itemView.findViewById(R.id.currency_picker_flag);
            code = itemView.findViewById(R.id.currency_picker_code);
            subtitle = itemView.findViewById(R.id.currency_picker_subtitle);
        }
    }
}
