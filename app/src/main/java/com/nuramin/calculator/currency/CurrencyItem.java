package com.nuramin.calculator.currency;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

/** ISO 4217 row: code, name, issuing region, optional ISO-3166 alpha-2 for drawable flags. */
public final class CurrencyItem {

    private final String code;
    private final String name;
    private final String country;
    @Nullable
    private final String flagCountryCode;

    public CurrencyItem(
            @NonNull String code,
            @NonNull String name,
            @NonNull String country,
            @Nullable String flagCountryCode) {
        this.code = code.toUpperCase(Locale.US);
        this.name = name.trim().isEmpty() ? this.code : name.trim();
        this.country = country.trim();
        this.flagCountryCode = flagCountryCode;
    }

    @NonNull
    public String getCode() {
        return code;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getCountry() {
        return country;
    }

    @Nullable
    public String getFlagCountryCode() {
        return flagCountryCode;
    }

    /** Regional-indicator emoji for legacy / compact UI (picker uses drawables). */
    @NonNull
    public String getFlagEmoji() {
        return FlagEmoji.forCurrency(code, flagCountryCode);
    }

    @NonNull
    public String getSpinnerLabel() {
        return getFlagEmoji() + "  " + code;
    }

    @NonNull
    public String getDropdownLabel() {
        return getFlagEmoji() + "  " + code + "  —  " + name;
    }

    /** Lowercase text for picker search (code, name, country). */
    @NonNull
    public String getSearchText() {
        return (code + " " + name + " " + country).toLowerCase(Locale.ROOT);
    }

    @NonNull
    public String getDisplaySubtitle() {
        if (!country.isEmpty()) {
            return name + " · " + country;
        }
        return name;
    }

    @NonNull
    @Override
    public String toString() {
        return code;
    }
}
