package com.nuramin.calculator.currency;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.drawerlayout.widget.DrawerLayout;

import com.nuramin.calculator.R;
import com.nuramin.calculator.util.CalculatorUtils;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Currency Converter: from/to spinners with country flags, amount, swap, result.
 * Fetches live rates from Frankfurter API; uses sample data and shows warning when offline or API fails.
 */
public final class CurrencyPanel {

    private static final String[] CODES = {"USD", "EUR", "GBP", "INR", "BDT"};
    /** Sample rates (1 unit = this many USD). BDT not in API so always sample. */
    private static final double[] SAMPLE_RATES_TO_USD = {1.0, 1.08, 1.27, 0.012, 0.0085};
    private static final int[] FLAG_IDS = {
            R.drawable.ic_flag_us,
            R.drawable.ic_flag_eur,
            R.drawable.ic_flag_gb,
            R.drawable.ic_flag_in,
            R.drawable.ic_flag_bd
    };
    private static final String API_URL = "https://api.frankfurter.app/latest?from=USD&to=EUR,GBP,INR";

    /** Current rates (1 unit = this many USD). Updated from API or sample. */
    private static double[] sRatesToUsd = SAMPLE_RATES_TO_USD.clone();
    private static boolean sUsingSampleData = true;
    private static View sPanel;
    private static Runnable sRefreshRunnable;

    public static void setup(View panel, @Nullable DrawerLayout drawerLayout, @Nullable View.OnClickListener onOverflowClick) {
        if (panel == null) return;
        sPanel = panel;
        ImageButton navMenu = panel.findViewById(R.id.currency_nav_menu);
        ImageButton menuDots = panel.findViewById(R.id.currency_menu_dots);
        final DrawerLayout layout = drawerLayout;
        if (navMenu != null && layout != null) {
            navMenu.setOnClickListener(v -> layout.openDrawer(Gravity.START));
        }
        if (menuDots != null && onOverflowClick != null) {
            menuDots.setOnClickListener(onOverflowClick);
        }

        Spinner fromSpinner = panel.findViewById(R.id.currency_from);
        Spinner toSpinner = panel.findViewById(R.id.currency_to);
        EditText amountEt = panel.findViewById(R.id.currency_amount);
        ImageButton swapBtn = panel.findViewById(R.id.currency_swap);
        TextView resultTv = panel.findViewById(R.id.currency_result);
        TextView indicativeTv = panel.findViewById(R.id.currency_indicative_text);
        TextView warningTv = panel.findViewById(R.id.currency_warning_text);
        ImageView fromFlag = panel.findViewById(R.id.currency_from_flag);
        ImageView toFlag = panel.findViewById(R.id.currency_to_flag);

        if (fromSpinner != null && toSpinner != null) {
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(panel.getContext(),
                    R.array.currency_codes, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            fromSpinner.setAdapter(adapter);
            toSpinner.setAdapter(adapter);
            fromSpinner.setSelection(0);
            toSpinner.setSelection(3);
        }

        Runnable updateFlags = () -> {
            if (fromFlag != null && fromSpinner != null) {
                int pos = fromSpinner.getSelectedItemPosition();
                if (pos >= 0 && pos < FLAG_IDS.length) fromFlag.setImageResource(FLAG_IDS[pos]);
            }
            if (toFlag != null && toSpinner != null) {
                int pos = toSpinner.getSelectedItemPosition();
                if (pos >= 0 && pos < FLAG_IDS.length) toFlag.setImageResource(FLAG_IDS[pos]);
            }
        };

        Runnable updateResult = () -> {
            if (amountEt == null || resultTv == null || indicativeTv == null || fromSpinner == null || toSpinner == null) return;
            double amount;
            try {
                String s = amountEt.getText() != null ? amountEt.getText().toString().replace(",", "").trim() : "";
                amount = s.isEmpty() ? 0 : Double.parseDouble(s);
            } catch (NumberFormatException e) {
                resultTv.setText("—");
                indicativeTv.setText("—");
                if (warningTv != null) warningTv.setVisibility(sUsingSampleData ? View.VISIBLE : View.GONE);
                return;
            }
            int fromIdx = fromSpinner.getSelectedItemPosition();
            int toIdx = toSpinner.getSelectedItemPosition();
            if (fromIdx < 0) fromIdx = 0;
            if (toIdx < 0) toIdx = 0;
            double[] rates = sRatesToUsd != null ? sRatesToUsd : SAMPLE_RATES_TO_USD;
            if (fromIdx >= rates.length) fromIdx = 0;
            if (toIdx >= rates.length) toIdx = 0;
            double fromRate = rates[fromIdx];
            double toRate = rates[toIdx];
            double usdValue = amount * fromRate;
            double result = toRate > 0 ? usdValue / toRate : 0;
            resultTv.setText(CalculatorUtils.formatNumber(result));
            indicativeTv.setText(String.format("1 %s = %s %s", CODES[fromIdx], CalculatorUtils.formatNumber(fromRate / toRate), CODES[toIdx]));
            if (warningTv != null) {
                warningTv.setVisibility(sUsingSampleData ? View.VISIBLE : View.GONE);
            }
        };

        sRefreshRunnable = () -> {
            updateResult.run();
            updateFlags.run();
        };

        if (fromSpinner != null) {
            fromSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    updateFlags.run();
                    updateResult.run();
                }
                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
        }
        if (toSpinner != null) {
            toSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    updateFlags.run();
                    updateResult.run();
                }
                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
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
        }
        if (swapBtn != null && fromSpinner != null && toSpinner != null) {
            swapBtn.setOnClickListener(v -> {
                int from = fromSpinner.getSelectedItemPosition();
                int to = toSpinner.getSelectedItemPosition();
                fromSpinner.setSelection(to);
                toSpinner.setSelection(from);
                updateFlags.run();
                updateResult.run();
            });
        }
        updateFlags.run();
        updateResult.run();
    }

    /**
     * Call when the currency converter panel becomes visible. Checks connectivity, shows
     * "turn on internet" dialog if off (repeats every time user is on this screen with no internet),
     * fetches live rates or uses sample data and shows warning.
     */
    public static void onPanelVisible(Activity activity, View panel) {
        if (activity == null || panel == null) return;
        boolean connected = isNetworkAvailable(activity);
        if (!connected) {
            sUsingSampleData = true;
            sRatesToUsd = SAMPLE_RATES_TO_USD.clone();
            runOnUiThread(activity, () -> {
                showInternetOffDialog(activity);
                refreshUi(panel);
            });
            return;
        }
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            double[] fetched = fetchRatesFromApi();
            activity.runOnUiThread(() -> {
                if (fetched != null) {
                    sRatesToUsd = fetched;
                    sUsingSampleData = false;
                } else {
                    sRatesToUsd = SAMPLE_RATES_TO_USD.clone();
                    sUsingSampleData = true;
                }
                refreshUi(panel);
            });
        });
    }

    private static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo net = cm.getActiveNetworkInfo();
        return net != null && net.isConnected();
    }

    private static void showInternetOffDialog(Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.currency_internet_off_title)
                .setMessage(R.string.currency_internet_off_message)
                .setPositiveButton(android.R.string.ok, (d, w) -> d.dismiss())
                .setCancelable(true)
                .show();
    }

    /** @return new rates array (1 unit = X USD), or null on failure */
    private static double[] fetchRatesFromApi() {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(API_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            int code = conn.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) return null;
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            JSONObject root = new JSONObject(sb.toString());
            JSONObject rates = root.optJSONObject("rates");
            if (rates == null) return null;
            double[] result = SAMPLE_RATES_TO_USD.clone();
            result[0] = 1.0; // USD
            double eur = rates.optDouble("EUR", 0);
            if (eur > 0) result[1] = 1.0 / eur;
            double gbp = rates.optDouble("GBP", 0);
            if (gbp > 0) result[2] = 1.0 / gbp;
            double inr = rates.optDouble("INR", 0);
            if (inr > 0) result[3] = 1.0 / inr;
            // result[4] BDT stays sample
            return result;
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static void runOnUiThread(Activity activity, Runnable r) {
        if (activity != null) activity.runOnUiThread(r);
    }

    private static void refreshUi(View panel) {
        if (sRefreshRunnable != null) sRefreshRunnable.run();
    }
}
