package com.nuramin.calculator.currency;

import android.content.Context;

import androidx.annotation.NonNull;

import com.nuramin.sunsetcoralcalculator.R;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Loads ISO 4217 → ISO 3166-1 alpha-2 (or EU/UN) from {@code res/raw/currency_regions.csv}. */
public final class CurrencyRegionsLoader {

    private CurrencyRegionsLoader() {}

    @NonNull
    public static Map<String, String> load(@NonNull Context context) {
        Map<String, String> map = new HashMap<>();
        try (InputStream is = context.getResources().openRawResource(R.raw.currency_regions);
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int comma = line.indexOf(',');
                if (comma <= 0) continue;
                String code = line.substring(0, comma).trim().toUpperCase();
                String region = line.substring(comma + 1).trim().toUpperCase();
                if (code.length() == 3 && region.length() >= 2) {
                    map.put(code, region);
                }
            }
        } catch (Exception ignored) {
        }
        return map;
    }
}
