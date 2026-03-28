# Global Readiness Refactor Report

## 1) Hardcoded / Country-Specific Problems Found

- Hardcoded UI text in Java and layouts (AI chip examples, API key hint, timeout title, BMI content descriptions, scientific button labels).
- Country-specific terms and symbols:
  - `GST` in AI labels/examples/suggestions.
  - `₹` hardcoded in EMI/Interest layouts and AI formatting helpers.
  - `Date of Birth` wording in date/age strings.
- Locale assumptions:
  - `Locale.US` formatting in `AmountFormatter`, `DateCalculatorPanel`, `AgeUtil`.
  - Direct `Double.parseDouble(...replace(",", ""))` in finance panels.
- Currency assumptions:
  - India scale labels (`L`, `Cr`) and rupee labels in EMI strings/UI.

## 2) Files Changed

- Java:
  - `app/src/main/java/com/nuramin/calculator/util/LocaleFormatManager.java` (new)
  - `app/src/main/java/com/nuramin/calculator/util/AmountFormatter.java`
  - `app/src/main/java/com/nuramin/calculator/emi/EmiCalculatorPanel.java`
  - `app/src/main/java/com/nuramin/calculator/interest/InterestCalculatorPanel.java`
  - `app/src/main/java/com/nuramin/calculator/discount/DiscountCalculatorPanel.java`
  - `app/src/main/java/com/nuramin/calculator/date/DateCalculatorPanel.java`
  - `app/src/main/java/com/nuramin/sunsetcoralcalculator/ai/ui/AISmartActivity.java`
  - `app/src/main/java/com/nuramin/sunsetcoralcalculator/ai/voice/VoiceInputHelper.java`
  - `app/src/main/java/com/nuramin/sunsetcoralcalculator/ai/calculator/EMIUtil.java`
  - `app/src/main/java/com/nuramin/sunsetcoralcalculator/ai/calculator/DiscountUtil.java`
  - `app/src/main/java/com/nuramin/sunsetcoralcalculator/ai/calculator/CalculationRouter.java`
  - `app/src/main/java/com/nuramin/sunsetcoralcalculator/ai/calculator/AgeUtil.java`
  - `app/src/main/java/com/nuramin/sunsetcoralcalculator/ai/core/AIEngine.java`
  - `app/src/main/java/com/nuramin/sunsetcoralcalculator/ai/utils/SuggestionEngine.java`

- Layouts:
  - `app/src/main/res/layout/activity_main.xml`
  - `app/src/main/res/layout/bmi_content.xml`
  - `app/src/main/res/layout/emi_content.xml`
  - `app/src/main/res/layout/emi/content.xml`
  - `app/src/main/res/layout/interest_content.xml`
  - `app/src/main/res/layout/interest/content.xml`

- Strings/resources:
  - `app/src/main/res/values/strings.xml` (new base file)
  - `app/src/main/res/values/strings_common.xml`
  - `app/src/main/res/values/strings_ai.xml`
  - `app/src/main/res/values/strings_emi.xml`
  - `app/src/main/res/values/strings_interest.xml`
  - `app/src/main/res/values/strings_bmi.xml`
  - `app/src/main/res/values/strings_age.xml`
  - `app/src/main/res/values/strings_date_calculator.xml`
  - `app/src/main/res/values-hi/strings.xml` (new)
  - `app/src/main/res/values-bn/strings.xml` (new)
  - `app/src/main/res/values-ar/strings.xml` (new)

## 3) Internationalization / Localization Refactor

- Moved remaining hardcoded user-facing text to resources for touched screens.
- Added localized string packs for:
  - Hindi (`values-hi`)
  - Bengali (`values-bn`)
  - Arabic (`values-ar`)
- Kept app support for RTL (`android:supportsRtl="true"` already enabled in manifest).

## 4) Tax Terminology Globalization

- Updated user-facing AI tax terminology from `GST` to global wording:
  - `Tax`
  - `Tax Calculator (GST/VAT/Sales Tax)` (AI result title)
- Updated suggestion and prompt text accordingly.

## 5) Currency Globalization

- Replaced hardcoded rupee output with locale currency formatting:
  - `NumberFormat.getCurrencyInstance(Locale.getDefault())`
  - `Currency.getInstance(Locale.getDefault())` fallback safe handling
- Added manual currency override picker:
  - `LocaleFormatManager.showCurrencyPickerDialog(...)`
  - Stored in shared prefs (`currency_override`)
- Applied to EMI, Interest, Discount outputs and AI helper currency outputs.

## 6) Number / Date Locale Fixes

- Replaced US-specific number formatting usages with locale-aware formatters.
- Added locale-aware numeric parsing helper to handle regional separators.
- Updated date detail number formatting in date calculator with locale-aware grouping.
- Updated AI age explanation to locale date formatting (medium date style).

## 7) Remaining Follow-Ups (Optional for full parity)

- More AI engine/core fallback text still uses inline English constants in non-Android-core model classes.
- Some non-critical `tools:text` preview literals remain in layouts (editor-only, not runtime).
- `gradlew` wrapper is absent in repo root, so full build verification in this environment could not be executed.
