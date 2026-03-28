# App Functional Analysis Report

**Source:** Codebase scan (`AndroidManifest.xml`, `app/build.gradle`, Java sources under `app/src/main/java`, layouts under `app/src/main/res/layout`, string resources under `app/src/main/res/values`).  
**App ID:** `com.nuramin.sunsetcoralcalculator`  
**Version (from Gradle):** `v3.0.0` (versionCode `15`)

---

## 1. Detected App Purpose

This is a **multi-mode calculator and conversion app** with a **navigation drawer** switching between tools on a single main screen (`MainActivity`). It provides:

- A **basic calculator** with optional **scientific keys** (trigonometry, inverse trig, π, sqrt, log, ln, etc.), **live evaluation**, **percentage rules**, **parentheses**, and **tabbed calculation history** (session + persisted “old” history).
- Dedicated panels for **temperature** (°C / °F / K), **EMI / loan** (sliders, pie chart, large principal ranges including lakh/crore-style UX), **simple and compound interest**, **currency conversion** (USD, EUR, GBP, INR, BDT with flags; **live rates via Frankfurter API** when online, sample rates when not), **BMI** (metric, gender UI, category bands), and **discount / percentage** (percent or fixed amount, breakdown).
- A **date-related panel** wired in the main flow computes **age from date of birth to “today”** (with “use today” behavior); the separate `DateCalculatorActivity` still exists for **legacy / alternate entry** and supports **age vs. between-dates modes** in code, but the primary UX is the in-drawer panel (age-focused).
- **Three math mini-games** embedded in the main activity: **Memory Number Grid** (memorize sequence), **Number Target Puzzle** (combine numbers with operators to hit a target), and **Math Speed Game** (timed falling answers, high score in `SharedPreferences`).
- **AI Smart Calculator** (`AISmartActivity`): chat-style UI with **local intent routing** (EMI, age, discount, GST via keyword detection + `CalculationRouter`) and **optional cloud** (Gemini first, then DeepSeek fallback) when API keys and network allow; **API rate limiting** (`APILimiter`), **user-pasteable Gemini key** in-app, optional **BuildConfig** keys from `local.properties`. **Voice input** uses `SpeechRecognizer` with **runtime `RECORD_AUDIO`** request.

Other behaviors evidenced in code: **Material light/dark/system theme**, **overflow menu** (clear history, check updates, theme, privacy text, email feedback, help), **Google Play in-app updates** (`com.google.android.play:app-update`), **privacy policy dialog** stating local-only calculations and no analytics (per string resource).

**Note:** Many **string resources** in `strings_common.xml` describe extra tools (e.g. vision test, electricity bill, ear sound test, reaction time, color blindness, heart rate, lightning math, mental calculation). **No matching Java classes or drawer entries** were found for these names—the shipped navigation (`nav_drawer.xml`) does not expose them. They should **not** be marketed as current app features unless you wire them up.

---

## 2. Core Features Detected from Code

- **Basic + scientific calculator** with expression editing, `=` evaluation, history (calculate + old tabs), max **50** history items (`AppConstants.MAX_HISTORY_ITEMS`).
- **Scientific mode toggle** (shows extra rows: sin/cos/tan, inv, deg/rad, log/ln, sqrt, π, etc.).
- **Temperature converter** (Celsius, Fahrenheit, Kelvin) with formula display and quick swap.
- **EMI calculator** with principal/rate/tenure sliders and formatted output + pie chart.
- **Interest calculator** simple vs compound, time in years or months, breakdown.
- **Currency converter** for five currencies, network fetch to `api.frankfurter.app`, offline/sample fallback with user-facing warning.
- **BMI calculator** (kg/cm, categories, animated result).
- **Discount / percentage calculator** with steppers and detailed breakdown.
- **Age calculator** (DOB vs today) in main `DateCalculatorPanel`; legacy full-screen date activity for backward compatibility.
- **AI chat calculator** with hybrid local + cloud, chips for EMI/age/GST/discount, share intent with Play Store link (`strings_ai.xml`).
- **Voice input** in AI screen (microphone permission when needed).
- **Math games**: memory grid, number puzzle, math speed (scores persisted where implemented in respective controllers).
- **In-app updates** via Play Core; manual check from overflow menu.

---

## 3. Hidden/Underrated Features Found

- **Hybrid AI path:** Simple numeric expressions can stay **fully offline** (`AIHybridManager.isSimpleInput` + local `AIEngine`); complex text can use **Gemini then DeepSeek** without user choosing—good to mention “smart routing.”
- **Lakh/crore/k parsing** in AI EMI (`CalculationRouter`)—strong for **India-focused** ASO copy.
- **Currency panel refresh** on visibility (`CurrencyPanel.onPanelVisible`)—rates update when user opens the tool.
- **Back press:** From non-calculator panels, back returns to **basic calculator**; from calculator, app **moves to background** (expression preserved)—good retention/detail for “pick up where you left off.”
- **Legacy `DateCalculatorActivity`** still supports **date-between mode** if launched; main drawer panel is **age-only**—a niche differentiator if you expose the legacy flow or unify UI.

---

## 4. Best Play Store Category

**Primary:** **Tools** (or **Productivity** if Play Console groups calculators there in your locale).

**Reason:** The app is dominated by calculators, unit/currency conversion, and finance math (EMI, interest, discount, GST in AI). Secondary gaming content (math games) is supporting, not the core identity. **Finance** is a possible secondary tag/keyword play, but the all-in-one tool scope fits **Tools** best.

---

## 5. High Ranking Keywords (ASO)

Use as a mix of title, short description, full description, and hidden keywords (where Play allows). **50+ feature-aligned terms:**

calculator, scientific calculator, math calculator, EMI calculator, loan calculator, mortgage calculator, interest calculator, simple interest, compound interest, GST calculator, India GST, discount calculator, sale calculator, percentage calculator, BMI calculator, body mass index, age calculator, date calculator, birthday calculator, temperature converter, Celsius Fahrenheit, Kelvin, currency converter, exchange rate, USD EUR GBP INR BDT, money converter, finance calculator, productivity, offline calculator, AI calculator, smart calculator, voice calculator, math game, brain game, memory game, number puzzle, math speed, puzzle game, kids math, student calculator, homework, engineering calculator, trigonometry, sin cos tan, logarithm, dark mode, light theme, history, calculation history, free calculator, all in one calculator, unit converter, Frankfurter, loan EMI, interest rate, discount percent, multi calculator, calculator with games, play update, material design

---

## 6. Suggested App Title (30 chars)

**Smart Calc AI: EMI & Tools**  
(29 characters — adjust branding as needed.)

Alternative: **AI Calculator — Finance & Math** (32 chars — trim to fit store limit).

---

## 7. Suggested Short Description (80 chars)

**All-in-one AI calculator: EMI, GST, BMI, currency, interest, games. Dark mode.**

(79 characters.)

---

## 8. Suggested Full Description

**Smart Multi Calculator with AI** brings everyday math, finance, and quick conversions into one clean app—with an optional **AI assistant** for natural questions.

**Calculate**
- **Basic & scientific** modes: arithmetic, parentheses, percentages, trigonometry, log/ln, π, and more.
- **History** with recent calculations and saved history—reuse results anytime.

**Finance & life**
- **EMI / loan** payments with clear totals and a visual breakdown.
- **Interest** calculator for **simple or compound** growth.
- **Discount & GST** style calculations (also easy to ask the AI).
- **BMI** from height and weight with category feedback.
- **Age** from date of birth (relative to today).

**Converters**
- **Temperature:** Celsius, Fahrenheit, Kelvin with the formula shown.
- **Currency:** major currencies with **live rates when online** (fallback when offline).

**AI Smart mode**
- Ask in plain language: loans, age from a date, GST, discounts, and more.
- **Works offline** for many tasks; optional **cloud** when you add a **free Gemini API key** in settings (keys stay on your device per in-app messaging).

**Brain break**
- **Memory grid**, **number target puzzle**, and **math speed** mini-games to sharpen mental math.

**Quality of life**
- **Light, dark, or system** theme.
- **In-app updates** from Google Play when available.
- **Privacy:** calculations and history stay on your device; see the in-app privacy policy for details.

Download **Smart Multi Calculator with AI** for one app that covers homework, shopping, travel money, and loans—plus a little fun.

---

## 9. Permission Justification

| Permission | Why it exists (from code) |
|------------|---------------------------|
| **INTERNET** | Currency rates (`HttpURLConnection` to Frankfurter), Gemini/DeepSeek APIs in `AIHybridManager`, Play Core update checks, and speech recognition may use network on some devices. |
| **ACCESS_NETWORK_STATE** | Used to detect connectivity before cloud AI and to drive offline fallbacks (`NetworkUtil` / currency / hybrid logic). |
| **RECORD_AUDIO** | Requested at runtime in `AISmartActivity` when the user taps the **microphone** for **voice input** (`VoiceInputHelper` + `SpeechRecognizer`). |

For Play Console: describe **microphone** narrowly as “optional voice input in AI calculator mode,” and **network** as “currency rates, optional AI answers, and app updates.”

---

## 10. Screenshot Text Suggestions

1. **Hero:** “All-in-one calculator + AI” / “EMI • GST • BMI • Currency”
2. **Basic + Scientific:** “Scientific mode — trig, log, π”
3. **History:** “Calculation history — tap to reuse”
4. **EMI:** “Loan EMI — see payment & breakdown”
5. **Currency:** “Live rates — USD EUR GBP INR BDT”
6. **AI mode:** “Ask naturally — voice or type”
7. **Games:** “Math games — memory, puzzle, speed”
8. **Theme:** “Light & dark — your choice”

---

## 11. Feature Tags for Play Console

Tools, Calculator, Finance, Currency, Unit Converter, Education, Productivity, Personalization (themes), Offline use (partial), Mathematics, Loan, Interest, BMI, Discount, GST (India-relevant)

---

## 12. Similar Top Apps in Same Niche

*(Market positioning reference—not extracted from your repository.)*

Apps users often compare: **Google Calculator**, **Microsoft Math / Edge tools**, **ClevCalc / Calculator Plus**, **Financial calculators** (EMI-focused), **Currency Converter** apps, and **AI assistant** calculators. Your differentiators in code are the **combined drawer UX**, **hybrid AI with local routing**, **specific currency set + Frankfurter**, **GST/discount/EMI in AI**, and **embedded games**.

---

## 13. What Makes This App Unique

- **Single-host design:** Many tools in one `MainActivity` with consistent toolbar and drawer—no separate APKs per tool.
- **Hybrid AI:** Offline keyword/intent pipeline plus optional Gemini/DeepSeek with rate limiting and user-supplied Gemini key.
- **India-friendly finance language** in AI EMI parsing (lakh/crore/k).
- **Games + utilities** in one package—unusual vs. pure calculator or pure game apps.
- **Play in-app updates** integrated for smoother distribution.

---

## 14. Missing Features That Could Increase Downloads

*(Actionable; aligned with gaps between resources and code or common store expectations.)*

1. **Remove or implement orphan strings:** Either **ship** the vision/electricity/hearing/etc. features or **delete unused strings** to avoid misleading store text if someone copies strings.xml into listings.
2. **Expose date-between mode** in the main drawer if product intent matches `DateCalculatorActivity` (currently age-only in `DateCalculatorPanel`).
3. **Runtime permission rationale:** Add an in-app rationale dialog before `RECORD_AUDIO` to improve trust and approval rates.
4. **More currencies or crypto toggle** only if you extend `CurrencyPanel` (currently five codes hardcoded).
5. **Widget / shortcut** for “open EMI” or “open AI”—not present in manifest; would help discovery and retention.
6. **Screenshot and short description** should avoid claiming **analytics** either way—your privacy string says no analytics; keep listing consistent.
7. **Tablet / large-screen** screenshots if layouts support them—improves conversion in some regions.

---

*Document generated from project source analysis. Update this file when features or permissions change.*
