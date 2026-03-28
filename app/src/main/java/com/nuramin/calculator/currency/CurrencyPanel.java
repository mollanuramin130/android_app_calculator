package com.nuramin.calculator.currency;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.button.MaterialButton;
import com.nuramin.sunsetcoralcalculator.R;
import com.nuramin.calculator.util.CalculatorUtils;
import com.nuramin.calculator.util.ConverterUiHelper;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * Currency converter: searchable picker, drawable flags, live rates from open.er-api.com,
 * persisted cache, last selection in {@link CurrencyPreferences}.
 */
public final class CurrencyPanel {

    private static volatile boolean sLastFetchWasLive;

    public static void setup(View panel, @Nullable DrawerLayout drawerLayout, @Nullable View.OnClickListener onOverflowClick) {
        if (panel == null) return;
        Context ctx = panel.getContext();
        CurrencyRatesRepository.loadCacheFromPrefs(ctx);
        CurrencyRatesRepository.notifyRegistryOfRateCodes(ctx);
        sLastFetchWasLive = false;

        List<CurrencyItem> catalog = CurrencyRegistry.getAllWithSvgFlags(ctx);

        MaterialButton fromBtn = panel.findViewById(R.id.currency_from);
        MaterialButton toBtn = panel.findViewById(R.id.currency_to);
        EditText amountEt = panel.findViewById(R.id.currency_amount);
        ImageButton swapBtn = panel.findViewById(R.id.currency_swap);
        TextView resultTv = panel.findViewById(R.id.currency_result);
        TextView indicativeTv = panel.findViewById(R.id.currency_indicative_text);
        TextView warningTv = panel.findViewById(R.id.currency_warning_text);
        TextView amountErrorTv = panel.findViewById(R.id.currency_amount_error);
        ImageView fromFlag = panel.findViewById(R.id.currency_from_flag);
        ImageView toFlag = panel.findViewById(R.id.currency_to_flag);

        final CurrencyItem[] fromSel = new CurrencyItem[1];
        final CurrencyItem[] toSel = new CurrencyItem[1];
        fromSel[0] = resolveItem(ctx, catalog, CurrencyPreferences.getFromCode(ctx, "USD"), "USD");
        toSel[0] = resolveItem(ctx, catalog, CurrencyPreferences.getToCode(ctx, "INR"), "INR");

        Runnable updateLabels = () -> {
            setCurrencyButton(fromBtn, fromSel[0]);
            setCurrencyButton(toBtn, toSel[0]);
            setFlagImage(ctx, fromFlag, fromSel[0]);
            setFlagImage(ctx, toFlag, toSel[0]);
        };

        Runnable updateResult = () -> {
            if (amountEt == null || resultTv == null || indicativeTv == null) return;
            if (fromSel[0] == null || toSel[0] == null) return;

            String raw = amountEt.getText() != null ? amountEt.getText().toString() : "";

            if (CurrencyConversion.isBlankAmount(raw)) {
                resultTv.setText("—");
                indicativeTv.setText("—");
                if (amountErrorTv != null) amountErrorTv.setVisibility(View.GONE);
                if (warningTv != null) {
                    warningTv.setVisibility(!sLastFetchWasLive ? View.VISIBLE : View.GONE);
                }
                return;
            }

            if (!CurrencyConversion.isValidAmount(raw)) {
                resultTv.setText("—");
                indicativeTv.setText("—");
                if (amountErrorTv != null) {
                    amountErrorTv.setText(R.string.currency_error_invalid_amount);
                    amountErrorTv.setVisibility(View.VISIBLE);
                }
                if (warningTv != null) warningTv.setVisibility(View.GONE);
                return;
            }

            if (amountErrorTv != null) amountErrorTv.setVisibility(View.GONE);

            try {
                double amount = CurrencyConversion.parseAmount(raw);
                String fromCode = fromSel[0].getCode();
                String toCode = toSel[0].getCode();

                double fromUsd = CurrencyRatesRepository.getUsdPerUnit(fromCode);
                double toUsd = CurrencyRatesRepository.getUsdPerUnit(toCode);
                boolean missingRate = !Double.isFinite(fromUsd) || fromUsd <= 0
                        || !Double.isFinite(toUsd) || toUsd <= 0;

                if (missingRate) {
                    resultTv.setText("—");
                    indicativeTv.setText(panel.getContext().getString(R.string.currency_rate_unavailable));
                    if (warningTv != null) warningTv.setVisibility(View.VISIBLE);
                    return;
                }

                double result = CurrencyConversion.convertViaUsd(amount, fromUsd, toUsd);
                if (!Double.isFinite(result)) {
                    resultTv.setText("—");
                    indicativeTv.setText("—");
                } else {
                    resultTv.setText(CalculatorUtils.formatNumber(result));
                    double cross = CurrencyConversion.unitsOfTargetPerOneSource(fromUsd, toUsd);
                    indicativeTv.setText(Double.isFinite(cross)
                            ? String.format("1 %s = %s %s", fromCode, CalculatorUtils.formatNumber(cross), toCode)
                            : "—");
                }
                if (warningTv != null) {
                    warningTv.setVisibility(!sLastFetchWasLive ? View.VISIBLE : View.GONE);
                }
            } catch (RuntimeException e) {
                resultTv.setText("—");
                indicativeTv.setText("—");
                if (warningTv != null) warningTv.setVisibility(View.VISIBLE);
            }
        };

        Runnable refreshAll = () -> {
            updateLabels.run();
            updateResult.run();
        };

        panel.setTag(R.id.currency_panel_refresh, refreshAll);

        if (fromBtn != null) {
            fromBtn.setOnClickListener(v -> CurrencyPickerDialog.show(
                    ctx,
                    catalog,
                    ctx.getString(R.string.currency_picker_from),
                    item -> {
                        fromSel[0] = item;
                        CurrencyPreferences.saveSelection(ctx, fromSel[0].getCode(), toSel[0] != null ? toSel[0].getCode() : "INR");
                        refreshAll.run();
                    }));
        }
        if (toBtn != null) {
            toBtn.setOnClickListener(v -> CurrencyPickerDialog.show(
                    ctx,
                    catalog,
                    ctx.getString(R.string.currency_picker_to),
                    item -> {
                        toSel[0] = item;
                        CurrencyPreferences.saveSelection(ctx, fromSel[0] != null ? fromSel[0].getCode() : "USD", toSel[0].getCode());
                        refreshAll.run();
                    }));
        }

        if (amountEt != null) {
            amountEt.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable editable) {
                    updateResult.run();
                }
            });
            amountEt.post(() -> {
                amountEt.requestFocus();
                amountEt.setSelection(amountEt.getText() != null ? amountEt.getText().length() : 0);
            });
        }

        if (swapBtn != null && fromSel[0] != null && toSel[0] != null) {
            swapBtn.setOnClickListener(v -> {
                ObjectAnimator.ofFloat(swapBtn, View.ROTATION, 0f, 180f).setDuration(240).start();
                CurrencyItem tmp = fromSel[0];
                fromSel[0] = toSel[0];
                toSel[0] = tmp;
                if (fromSel[0] != null && toSel[0] != null) {
                    CurrencyPreferences.saveSelection(ctx, fromSel[0].getCode(), toSel[0].getCode());
                }
                updateLabels.run();
                updateResult.run();
                ConverterUiHelper.hideSoftKeyboard(panel);
                if (resultTv != null) {
                    ConverterUiHelper.scrollToShowResult(panel, resultTv);
                }
            });
        }

        refreshAll.run();
    }

    @Nullable
    private static CurrencyItem resolveItem(Context ctx, List<CurrencyItem> catalog, String prefCode, String fallbackCode) {
        if (catalog.isEmpty()) return null;
        CurrencyItem pref = CurrencyRegistry.find(ctx, prefCode);
        if (pref != null && listContainsCode(catalog, pref.getCode())) {
            return pref;
        }
        CurrencyItem fb = CurrencyRegistry.find(ctx, fallbackCode);
        if (fb != null && listContainsCode(catalog, fb.getCode())) {
            return fb;
        }
        for (CurrencyItem x : catalog) {
            if (fallbackCode != null && fallbackCode.equalsIgnoreCase(x.getCode())) return x;
        }
        return catalog.get(0);
    }

    private static boolean listContainsCode(@NonNull List<CurrencyItem> catalog, @Nullable String code) {
        if (code == null) return false;
        String u = code.toUpperCase(Locale.US);
        for (CurrencyItem x : catalog) {
            if (u.equals(x.getCode())) return true;
        }
        return false;
    }

    private static void setCurrencyButton(@Nullable MaterialButton btn, @Nullable CurrencyItem item) {
        if (btn == null || item == null) return;
        btn.setText(item.getCode() + " — " + item.getName());
        btn.setSingleLine(true);
        btn.setEllipsize(TextUtils.TruncateAt.END);
    }

    private static void setFlagImage(Context ctx, @Nullable ImageView iv, @Nullable CurrencyItem item) {
        if (iv == null || item == null) return;
        CurrencySvgFlagLoader.loadInto(
                iv, item, R.dimen.currency_flag_render_width_main, R.dimen.currency_flag_render_height_main);
    }

    public static void onPanelVisible(Activity activity, View panel) {
        if (activity == null || panel == null) return;
        CurrencyRatesRepository.loadCacheFromPrefs(activity);
        CurrencyRatesRepository.notifyRegistryOfRateCodes(activity);
        boolean connected = isNetworkAvailable(activity);
        if (!connected) {
            sLastFetchWasLive = false;
            activity.runOnUiThread(() -> {
                showInternetOffDialog(activity);
                runRefresh(panel);
            });
            return;
        }
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            executor.execute(() -> {
                String json = CurrencyRatesRepository.fetchLatestJson();
                activity.runOnUiThread(() -> {
                    if (json != null) {
                        CurrencyRatesRepository.saveCacheToPrefs(activity, json);
                        sLastFetchWasLive = true;
                    } else {
                        sLastFetchWasLive = false;
                    }
                    CurrencyRatesRepository.notifyRegistryOfRateCodes(activity);
                    runRefresh(panel);
                });
            });
        } catch (RejectedExecutionException e) {
            activity.runOnUiThread(() -> {
                sLastFetchWasLive = false;
                runRefresh(panel);
            });
        } finally {
            executor.shutdown();
        }
    }

    private static void runRefresh(View panel) {
        Runnable r = (Runnable) panel.getTag(R.id.currency_panel_refresh);
        if (r != null) r.run();
    }

    private static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network network = cm.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private static void showInternetOffDialog(Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.currency_internet_off_title)
                .setMessage(R.string.currency_internet_off_message)
                .setPositiveButton(android.R.string.ok, (d, w) -> d.dismiss())
                .setCancelable(true)
                .show();
    }
}
