package com.nuramin.calculator.basic;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Build;
import android.view.inputmethod.InputMethodManager;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.nuramin.sunsetcoralcalculator.R;
import com.nuramin.calculator.adapter.HistoryAdapter;
import com.nuramin.calculator.common.AppConstants;
import com.nuramin.calculator.model.HistoryEntry;
import com.nuramin.calculator.util.CalculatorUtils;
import com.nuramin.calculator.util.ExprParser;
import com.nuramin.calculator.util.HistoryStorage;

import java.util.ArrayList;
import java.util.List;

/**
 * Basic Calculator: smart expression engine with numbers, operators, %, (), AC, delete, =.
 * No XML or UI changes; only internal logic.
 */
public class BasicCalculatorScreen {

    private final AppCompatActivity activity;
    private EditText tvExpression;
    private TextView tvResult;
    private HorizontalScrollView displayScroll;
    private Button btnEquals;
    private ListView panelHistoryList;
    private View tabCalculateHistory;
    private View tabOldHistory;

    // ---- Core state ----
    private final StringBuilder expression = new StringBuilder();
    private boolean lastInputIsOperator = false;
    private boolean lastInputIsDecimal = false;
    private boolean isResultDisplayed = false;
    /** Expression as it was when user pressed =; used so one backspace restores expression minus last char. */
    private String expressionBeforeEquals = null;
    private int openParenthesisCount = 0;

    private boolean updatingFromCode = false;
    /** Current session only; cleared when app is terminated. */
    private final List<HistoryEntry> sessionHistory = new ArrayList<>();
    /** Loaded from storage; persisted until user clears history. */
    private final List<HistoryEntry> oldHistory = new ArrayList<>();
    private HistoryStorage historyStorage;
    private HistoryAdapter historyAdapter;
    /** 0 = Calculate History, 1 = Old History */
    private int currentHistoryTab = 0;

    /** Scientific mode: angle unit for sin/cos/tan (true = degrees, false = radians). */
    private boolean degMode = true;
    /** Scientific mode: when true, sin→asin, cos→acos, tan→atan. */
    private boolean invMode = false;

    /** Current result animator so we can cancel it when a new result is set. */
    private ValueAnimator resultCountUpAnimator;

    /** Last valid live-eval result; shown in live area when current expression is incomplete/invalid. */
    private String lastValidLiveResult = null;

    /** When set, updateDisplay() will set cursor to this position instead of end (for insert-at-cursor). */
    private Integer pendingSelectionAfterUpdate = null;

    private static final String OP_PLUS = "+";
    private static final String OP_MINUS = "−";
    private static final String OP_MUL = "×";
    private static final String OP_DIV = "÷";

    public BasicCalculatorScreen(AppCompatActivity activity) {
        this.activity = activity;
    }

    public void setup() {
        tvExpression = activity.findViewById(R.id.tv_expression);
        tvResult = activity.findViewById(R.id.tv_result);
        displayScroll = activity.findViewById(R.id.display_scroll);
        btnEquals = activity.findViewById(R.id.btn_equals);
        panelHistoryList = activity.findViewById(R.id.panel_history_list);

        if (tvExpression == null || tvResult == null) return;

        // Prevent soft keyboard in all scenarios (tap, select, copy, long-press)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tvExpression.setShowSoftInputOnFocus(false);
        }
        tvExpression.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) hideSoftKeyboard(); });

        // Number buttons 0–9 and dot: route to single handler
        setNumberButton(R.id.btn_0, "0");
        setNumberButton(R.id.btn_1, "1");
        setNumberButton(R.id.btn_2, "2");
        setNumberButton(R.id.btn_3, "3");
        setNumberButton(R.id.btn_4, "4");
        setNumberButton(R.id.btn_5, "5");
        setNumberButton(R.id.btn_6, "6");
        setNumberButton(R.id.btn_7, "7");
        setNumberButton(R.id.btn_8, "8");
        setNumberButton(R.id.btn_9, "9");
        setNumberButton(R.id.btn_dot, ".");

        activity.findViewById(R.id.btn_plus).setOnClickListener(v -> handleOperator(OP_PLUS));
        activity.findViewById(R.id.btn_minus).setOnClickListener(v -> handleOperator(OP_MINUS));
        activity.findViewById(R.id.btn_multiply).setOnClickListener(v -> handleOperator(OP_MUL));
        activity.findViewById(R.id.btn_divide).setOnClickListener(v -> handleOperator(OP_DIV));
        activity.findViewById(R.id.btn_lparen).setOnClickListener(v -> handleParenthesis());
        if (btnEquals != null) {
            btnEquals.setEnabled(true);
            btnEquals.setOnClickListener(v -> {
                calculateResult();
                btnEquals.post(() -> {
                    btnEquals.setEnabled(true);
                    btnEquals.refreshDrawableState();
                });
            });
        }
        activity.findViewById(R.id.btn_clear).setOnClickListener(v -> clearAll());
        activity.findViewById(R.id.btn_percent).setOnClickListener(v -> handlePercentage());

        View btnBackspace = activity.findViewById(R.id.btn_backspace);
        if (btnBackspace != null) {
            setupDeleteButtonPressAndHold(btnBackspace);
        }

        // Scientific buttons (may be in scientific_rows, same layout)
        setScientificButton(R.id.btn_sqrt, v -> handleSqrt());
        setScientificButton(R.id.btn_pi, v -> handlePi());
        setScientificButton(R.id.btn_power, v -> handlePower());
        setScientificButton(R.id.btn_factorial, v -> handleFactorial());
        setScientificButton(R.id.btn_deg, v -> handleDegToggle());
        setScientificButton(R.id.btn_sin, v -> handleSin());
        setScientificButton(R.id.btn_cos, v -> handleCos());
        setScientificButton(R.id.btn_tan, v -> handleTan());
        setScientificButton(R.id.btn_inv, v -> handleInvToggle());
        setScientificButton(R.id.btn_e, v -> handleE());
        setScientificButton(R.id.btn_ln, v -> handleLn());
        setScientificButton(R.id.btn_log, v -> handleLog());

        tvExpression.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable editable) {
                if (updatingFromCode) return;
                String raw = editable != null ? editable.toString() : "";
                String filtered = CalculatorUtils.filterExpressionChars(raw);
                if (!filtered.equals(expression.toString())) {
                    expression.setLength(0);
                    expression.append(filtered);
                    syncStateFromExpression();
                    if (!raw.equals(filtered)) {
                        updatingFromCode = true;
                        tvExpression.setText(filtered);
                        tvExpression.setSelection(filtered.length());
                        updatingFromCode = false;
                    }
                    updateDisplay();
                }
            }
        });

        historyStorage = new HistoryStorage(activity);
        oldHistory.addAll(historyStorage.loadOldHistory());
        historyAdapter = new HistoryAdapter(activity, new ArrayList<>(),
                this::applyResultFromHistory, this::onUseExpressionFromHistory, this::onHistoryEntryDismissed);
        if (panelHistoryList != null) {
            panelHistoryList.setAdapter(historyAdapter);
            panelHistoryList.setOnItemClickListener((parent, view, position, id) -> {});
        }

        tabCalculateHistory = activity.findViewById(R.id.tab_calculate_history);
        tabOldHistory = activity.findViewById(R.id.tab_old_history);
        if (tabCalculateHistory != null) {
            tabCalculateHistory.setOnClickListener(v -> setHistoryTab(0));
        }
        if (tabOldHistory != null) {
            tabOldHistory.setOnClickListener(v -> setHistoryTab(1));
        }
        refreshHistoryTabs();
        refreshHistoryList();

        updateDisplay();
    }

    private void setHistoryTab(int tab) {
        if (currentHistoryTab == tab) return;
        currentHistoryTab = tab;
        refreshHistoryTabs();
        refreshHistoryList();
    }

    private void refreshHistoryTabs() {
        if (tabCalculateHistory != null) {
            tabCalculateHistory.setBackgroundResource(currentHistoryTab == 0 ? R.drawable.bg_history_segment_selected : R.drawable.bg_history_segment_unselected);
        }
        if (tabOldHistory != null) {
            tabOldHistory.setBackgroundResource(currentHistoryTab == 1 ? R.drawable.bg_history_segment_selected : R.drawable.bg_history_segment_unselected);
        }
    }

    private List<HistoryEntry> getDisplayedHistoryList() {
        return currentHistoryTab == 0 ? sessionHistory : oldHistory;
    }

    private void refreshHistoryList() {
        if (historyAdapter == null) return;
        historyAdapter.setCurrentHistoryTab(currentHistoryTab);
        historyAdapter.clear();
        historyAdapter.addAll(getDisplayedHistoryList());
        historyAdapter.notifyDataSetChanged();
    }

    private void setNumberButton(int id, String value) {
        View btn = activity.findViewById(id);
        if (btn != null) {
            btn.setOnClickListener(v -> {
                if (value.equals(".")) handleDecimal();
                else handleNumber(value);
            });
        }
    }

    private void setScientificButton(int id, View.OnClickListener listener) {
        View v = activity.findViewById(id);
        if (v != null) v.setOnClickListener(listener);
    }

    /** Press: single delete. Press and hold: repeat delete after initial delay. */
    private void setupDeleteButtonPressAndHold(View btnBackspace) {
        final Handler handler = new Handler(Looper.getMainLooper());
        final long repeatDelayMs = 80;
        final long initialHoldMs = 450;
        final boolean[] repeatStarted = { false };
        final Runnable repeatRunnable = new Runnable() {
            @Override
            public void run() {
                repeatStarted[0] = true;
                deleteLast();
                handler.postDelayed(this, repeatDelayMs);
            }
        };
        btnBackspace.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    repeatStarted[0] = false;
                    v.setPressed(true);
                    handler.postDelayed(repeatRunnable, initialHoldMs);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.setPressed(false);
                    handler.removeCallbacks(repeatRunnable);
                    if (event.getAction() == MotionEvent.ACTION_UP && !repeatStarted[0]) {
                        deleteLast();
                    }
                    return true;
            }
            return false;
        });
    }

    // ---- Scientific button handlers ----
    private void handleSqrt() {
        if (isResultDisplayed) { expression.setLength(0); isResultDisplayed = false; }
        expression.append("sqrt(");
        openParenthesisCount++;
        lastInputIsOperator = false;
        lastInputIsDecimal = false;
        updateDisplay();
    }

    private void handlePi() {
        if (isResultDisplayed) { expression.setLength(0); isResultDisplayed = false; }
        expression.append('π');
        lastInputIsOperator = false;
        lastInputIsDecimal = false;
        updateDisplay();
    }

    private void handlePower() {
        handleOperator("^");
    }

    private void handleFactorial() {
        if (isResultDisplayed) { expression.setLength(0); isResultDisplayed = false; }
        expression.append('!');
        lastInputIsOperator = false;
        lastInputIsDecimal = false;
        updateDisplay();
    }

    private void handleDegToggle() {
        degMode = !degMode;
        updateDisplay();
    }

    private void handleSin() {
        if (isResultDisplayed) { expression.setLength(0); isResultDisplayed = false; }
        expression.append(invMode ? "asin(" : "sin(");
        openParenthesisCount++;
        lastInputIsOperator = false;
        lastInputIsDecimal = false;
        updateDisplay();
    }

    private void handleCos() {
        if (isResultDisplayed) { expression.setLength(0); isResultDisplayed = false; }
        expression.append(invMode ? "acos(" : "cos(");
        openParenthesisCount++;
        lastInputIsOperator = false;
        lastInputIsDecimal = false;
        updateDisplay();
    }

    private void handleTan() {
        if (isResultDisplayed) { expression.setLength(0); isResultDisplayed = false; }
        expression.append(invMode ? "atan(" : "tan(");
        openParenthesisCount++;
        lastInputIsOperator = false;
        lastInputIsDecimal = false;
        updateDisplay();
    }

    private void handleInvToggle() {
        invMode = !invMode;
        updateDisplay();
    }

    private void handleE() {
        if (isResultDisplayed) { expression.setLength(0); isResultDisplayed = false; }
        expression.append("e");
        lastInputIsOperator = false;
        lastInputIsDecimal = false;
        updateDisplay();
    }

    private void handleLn() {
        if (isResultDisplayed) { expression.setLength(0); isResultDisplayed = false; }
        expression.append("ln(");
        openParenthesisCount++;
        lastInputIsOperator = false;
        lastInputIsDecimal = false;
        updateDisplay();
    }

    private void handleLog() {
        if (isResultDisplayed) { expression.setLength(0); isResultDisplayed = false; }
        expression.append("log(");
        openParenthesisCount++;
        lastInputIsOperator = false;
        lastInputIsDecimal = false;
        updateDisplay();
    }

    // ---- Sync state from current expression (e.g. after external edit) ----
    private void syncStateFromExpression() {
        int len = expression.length();
        lastInputIsOperator = len > 0 && isOperatorChar(expression.charAt(len - 1));
        lastInputIsDecimal = len > 0 && expression.charAt(len - 1) == '.';
        openParenthesisCount = 0;
        for (int i = 0; i < len; i++) {
            char c = expression.charAt(i);
            if (c == '(') openParenthesisCount++;
            else if (c == ')') openParenthesisCount--;
        }
    }

    private static boolean isOperatorChar(char c) {
        return c == '+' || c == '−' || c == '×' || c == '÷' || c == '^';
    }

    /** Current cursor/insert position in expression (for insert-at-cursor). */
    private int getInsertPosition() {
        if (tvExpression == null) return expression.length();
        int sel = tvExpression.getSelectionStart();
        if (sel < 0) return expression.length();
        return Math.min(sel, expression.length());
    }

    /** Insert string at cursor position; keeps cursor after inserted text. */
    private void insertAtCursor(String s) {
        int pos = getInsertPosition();
        expression.insert(pos, s);
        syncStateFromExpression();
        pendingSelectionAfterUpdate = pos + s.length();
        updateDisplay();
    }

    private char lastChar() {
        return expression.length() == 0 ? '\0' : expression.charAt(expression.length() - 1);
    }

    // ---- 3. Number handling ----
    private void handleNumber(String value) {
        if (isResultDisplayed) {
            expression.setLength(0);
            isResultDisplayed = false;
        }
        int pos = getInsertPosition();
        if (pos < expression.length()) {
            expression.insert(pos, value);
            syncStateFromExpression();
            pendingSelectionAfterUpdate = pos + 1;
            updateDisplay();
            return;
        }
        // Cursor at end: prevent leading zero duplication (00, 000)
        if (value.equals("0")) {
            int start = lastNumberStart();
            String num = expression.substring(start);
            if (num.equals("0") || num.matches("0+")) return;
        }
        if (expression.length() == 1 && expression.charAt(0) == '0' && !value.equals("0")) expression.setLength(0);
        expression.append(value);
        lastInputIsOperator = false;
        lastInputIsDecimal = false;
        isResultDisplayed = false;
        updateDisplay();
    }

    /** Index of start of last number (digit/dot segment) in expression. */
    private int lastNumberStart() {
        int i = expression.length() - 1;
        while (i >= 0) {
            char c = expression.charAt(i);
            if (Character.isDigit(c) || c == '.') i--;
            else return i + 1;
        }
        return 0;
    }

    // ---- 4. Operator handling ----
    private void handleOperator(String op) {
        if (isResultDisplayed) isResultDisplayed = false;
        int pos = getInsertPosition();
        int len = expression.length();
        if (len == 0) {
            if (op.equals(OP_MINUS)) expression.append(op);
            lastInputIsOperator = true;
            lastInputIsDecimal = false;
            updateDisplay();
            return;
        }
        if (pos < len) {
            expression.insert(pos, op);
            syncStateFromExpression();
            pendingSelectionAfterUpdate = pos + 1;
            updateDisplay();
            return;
        }
        char last = lastChar();
        if (isOperatorChar(last)) {
            expression.setLength(len - 1);
            expression.append(op);
        } else if (last != '(') {
            expression.append(op);
        }
        lastInputIsOperator = true;
        lastInputIsDecimal = false;
        updateDisplay();
    }

    // ---- 5. Decimal handling ----
    private void handleDecimal() {
        if (isResultDisplayed) {
            expression.setLength(0);
            isResultDisplayed = false;
        }
        int pos = getInsertPosition();
        if (pos < expression.length()) {
            expression.insert(pos, ".");
            syncStateFromExpression();
            pendingSelectionAfterUpdate = pos + 1;
            updateDisplay();
            return;
        }
        if (hasDecimalInCurrentNumber()) return;
        if (expression.length() == 0 || lastInputIsOperator || lastChar() == '(') expression.append("0");
        expression.append(".");
        lastInputIsDecimal = true;
        lastInputIsOperator = false;
        isResultDisplayed = false;
        updateDisplay();
    }

    private boolean hasDecimalInCurrentNumber() {
        int i = expression.length() - 1;
        while (i >= 0) {
            char c = expression.charAt(i);
            if (c == '.') return true;
            if (isOperatorChar(c) || c == '(' || c == ')') return false;
            i--;
        }
        return false;
    }

    // ---- 6. Smart parenthesis ----
    private void handleParenthesis() {
        if (isResultDisplayed) {
            expression.setLength(0);
            isResultDisplayed = false;
        }
        int pos = getInsertPosition();
        if (pos < expression.length()) {
            char ch = '(';
            expression.insert(pos, ch);
            syncStateFromExpression();
            pendingSelectionAfterUpdate = pos + 1;
            updateDisplay();
            return;
        }
        int len = expression.length();
        boolean empty = len == 0;
        boolean lastIsOp = lastInputIsOperator;
        boolean lastIsOpen = lastChar() == '(';

        if (empty || lastIsOp || lastIsOpen) {
            expression.append('(');
            openParenthesisCount++;
        } else if (openParenthesisCount > 0 && !lastInputIsOperator && lastChar() != '(') {
            expression.append(')');
            openParenthesisCount--;
        } else {
            expression.append('(');
            openParenthesisCount++;
        }
        lastInputIsOperator = false;
        lastInputIsDecimal = false;
        updateDisplay();
    }

    /** Suffixes for scientific functions that end with '('; longest first for matching. */
    private static final String[] SCIENTIFIC_FUNCTION_SUFFIXES = {
        "atan(", "asin(", "acos(", "sqrt(", "sin(", "cos(", "tan(", "log(", "ln("
    };

    // ---- 7. Delete last ----
    private void deleteLast() {
        if (isResultDisplayed && expressionBeforeEquals != null) {
            if (expressionBeforeEquals.length() > 0) {
                String restored = expressionBeforeEquals.substring(0, expressionBeforeEquals.length() - 1);
                expression.setLength(0);
                expression.append(restored);
                syncStateFromExpression();
                isResultDisplayed = false;
                expressionBeforeEquals = null;
                if (tvResult != null) tvResult.setText("");
                updateDisplay();
            } else {
                expressionBeforeEquals = null;
                isResultDisplayed = false;
                if (tvResult != null) tvResult.setText("");
                updateDisplay();
            }
            return;
        }
        if (expression.length() == 0) return;
        if (isResultDisplayed) return;
        int pos = getInsertPosition();
        if (pos > 0 && pos < expression.length()) {
            char removed = expression.charAt(pos - 1);
            expression.deleteCharAt(pos - 1);
            if (removed == '(') openParenthesisCount--;
            else if (removed == ')') openParenthesisCount++;
            syncStateFromExpression();
            pendingSelectionAfterUpdate = pos - 1;
            updateDisplay();
            return;
        }
        if (pos < expression.length()) return;
        String expr = expression.toString();
        for (String suffix : SCIENTIFIC_FUNCTION_SUFFIXES) {
            if (expr.endsWith(suffix)) {
                expression.setLength(expression.length() - suffix.length());
                openParenthesisCount--;
                if (expression.length() > 0) {
                    char last = lastChar();
                    lastInputIsOperator = isOperatorChar(last);
                    lastInputIsDecimal = last == '.';
                } else {
                    lastInputIsOperator = false;
                    lastInputIsDecimal = false;
                }
                updateDisplay();
                return;
            }
        }
        char removed = expression.charAt(expression.length() - 1);
        expression.setLength(expression.length() - 1);
        if (removed == '(') openParenthesisCount--;
        else if (removed == ')') openParenthesisCount++;
        if (expression.length() > 0) {
            char last = lastChar();
            lastInputIsOperator = isOperatorChar(last);
            lastInputIsDecimal = last == '.';
        } else {
            lastInputIsOperator = false;
            lastInputIsDecimal = false;
        }
        updateDisplay();
    }

    // ---- 8. AC ----
    private void clearAll() {
        cancelResultAnimation();
        lastValidLiveResult = null;
        expression.setLength(0);
        updatingFromCode = true;
        tvResult.setText("0");
        if (tvExpression != null) {
            tvExpression.setText("");
            tvExpression.setVisibility(View.GONE);
        }
        updatingFromCode = false;
        lastInputIsOperator = false;
        lastInputIsDecimal = false;
        isResultDisplayed = false;
        openParenthesisCount = 0;
        scrollDisplayToEnd();
    }

    // ---- 9. Percentage: last number -> (number/100) ----
    private void handlePercentage() {
        if (isResultDisplayed) {
            expression.setLength(0);
            isResultDisplayed = false;
        }
        int len = expression.length();
        if (len == 0) return;
        int i = len - 1;
        while (i >= 0) {
            char c = expression.charAt(i);
            if (Character.isDigit(c) || c == '.') i--;
            else break;
        }
        int start = i + 1;
        if (start >= len) return;
        String numStr = expression.substring(start, len);
        double num;
        try {
            num = Double.parseDouble(numStr);
        } catch (NumberFormatException e) {
            return;
        }
        expression.setLength(start);
        expression.append("(").append(num).append("/100)");
        lastInputIsOperator = false;
        lastInputIsDecimal = false;
        updateDisplay();
    }

    // ---- 10. Calculation engine ----
    private void calculateResult() {
        expressionBeforeEquals = expression.toString();
        String exprStr = expression.toString().trim();
        if (exprStr.isEmpty()) return;

        // 1) Remove trailing operator
        while (exprStr.length() > 0 && isOperatorChar(exprStr.charAt(exprStr.length() - 1))) {
            exprStr = exprStr.substring(0, exprStr.length() - 1).trim();
        }
        if (exprStr.isEmpty()) return;

        // 2) Auto-close open parentheses
        int open = 0;
        for (int i = 0; i < exprStr.length(); i++) {
            if (exprStr.charAt(i) == '(') open++;
            else if (exprStr.charAt(i) == ')') open--;
        }
        while (open > 0) {
            exprStr += ")";
            open--;
        }

        // 3) Expand % to (x/100) for evaluation
        String toEval = expandPercentages(exprStr);
        toEval = toEval.replace('×', '*').replace('÷', '/').replace('−', '-').replaceAll("\\s+", "");

        Double result = evaluateExpression(toEval);
        if (result == null) {
            expressionBeforeEquals = null;
            cancelResultAnimation();
            if (tvResult != null) tvResult.setText("Error");
            if (tvExpression != null) tvExpression.setVisibility(View.VISIBLE);
            lastInputIsOperator = false;
            lastInputIsDecimal = false;
            isResultDisplayed = false;
            scrollDisplayToEnd();
            return;
        }

        String resultStr = formatResult(result);
        saveToHistory(exprStr, resultStr);

        updatingFromCode = true;
        if (tvExpression != null) {
            tvExpression.setVisibility(View.VISIBLE);
        }
        lastValidLiveResult = resultStr;
        if (tvExpression != null) {
            tvExpression.setText(resultStr);
            tvExpression.setSelection(resultStr.length());
        }
        if (tvResult != null) tvResult.setText("");
        expression.setLength(0);
        expression.append(resultStr.replace(",", ""));
        updatingFromCode = false;
        lastInputIsOperator = false;
        lastInputIsDecimal = false;
        isResultDisplayed = true;
        openParenthesisCount = 0;
        refreshHistoryList();
        scrollDisplayToEnd();
    }

    /** Replace each "number%" with "(number/100)" for evaluation. */
    private String expandPercentages(String expr) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < expr.length()) {
            if (expr.charAt(i) == '%') {
                int end = i;
                int start = i;
                while (start > 0) {
                    char c = expr.charAt(start - 1);
                    if (Character.isDigit(c) || c == '.') start--;
                    else break;
                }
                if (start < end) {
                    String numStr = expr.substring(start, end);
                    try {
                        Double.parseDouble(numStr);
                        out.setLength(out.length() - (end - start));
                        out.append("(").append(numStr).append("/100)");
                    } catch (NumberFormatException ignored) {
                        out.append('%');
                    }
                } else {
                    out.append('%');
                }
                i++;
            } else {
                out.append(expr.charAt(i));
                i++;
            }
        }
        return out.toString();
    }

    /**
     * Smart live evaluation: try to get a result from current input by normalizing then evaluating.
     * 1) Auto-close unclosed parentheses (e.g. sin(9 -> sin(9)).
     * 2) Try evaluate. If fail, strip trailing operators and try again (e.g. 9+6+ -> 9+6).
     * Returns null if no valid result; otherwise the computed value.
     */
    private Double evaluateLiveExpression(String toEval) {
        if (toEval == null || toEval.isEmpty()) return null;
        String normalized = closeUnclosedParenthesesForLiveEval(toEval);
        Double result = evaluateExpression(normalized);
        if (result != null) return result;
        String trimmed = stripTrailingOperatorsForLiveEval(normalized);
        if (!trimmed.equals(normalized)) result = evaluateExpression(trimmed);
        return result;
    }

    /**
     * For live evaluation only: append ')' for each unclosed '(' so that e.g. 9+6+sin(9
     * becomes 9+6+sin(9) and can be evaluated without waiting for the user to type ).
     */
    private String closeUnclosedParenthesesForLiveEval(String s) {
        if (s == null) return "";
        int open = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '(') open++;
            else if (s.charAt(i) == ')') open--;
        }
        StringBuilder out = new StringBuilder(s);
        while (open > 0) {
            out.append(')');
            open--;
        }
        return out.toString();
    }

    /**
     * For live evaluation only: remove trailing operator chars so e.g. 9+6+ or 9+6-
     * becomes 9+6 and can be evaluated to show the last partial result.
     */
    private String stripTrailingOperatorsForLiveEval(String s) {
        if (s == null) return "";
        String t = s.trim();
        while (t.length() > 0) {
            char c = t.charAt(t.length() - 1);
            if (c == '+' || c == '-' || c == '*' || c == '/' || c == '^') {
                t = t.substring(0, t.length() - 1).trim();
            } else {
                break;
            }
        }
        return t;
    }

    private Double evaluateExpression(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            double v = new ExprParser(s, degMode).parse();
            if (Double.isNaN(v) || Double.isInfinite(v)) return null;
            return v;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String formatResult(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return "Error";
        if (value == Math.rint(value)) return CalculatorUtils.formatNumber((long) value);
        String s = String.format(java.util.Locale.US, "%.6f", value);
        if (s.contains(".")) s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s;
    }

    /**
     * History integration placeholder. Called after successful calculation.
     * Do not implement database here.
     */
    private void saveToHistory(String expressionString, String resultString) {
        sessionHistory.add(0, new HistoryEntry(expressionString, resultString));
        if (sessionHistory.size() > AppConstants.MAX_HISTORY_ITEMS) {
            sessionHistory.remove(sessionHistory.size() - 1);
        }
        long now = System.currentTimeMillis();
        oldHistory.add(0, new HistoryEntry(expressionString, resultString, now));
        while (oldHistory.size() > 200) oldHistory.remove(oldHistory.size() - 1);
        if (historyStorage != null) {
            historyStorage.appendEntry(expressionString, resultString);
        }
        refreshHistoryList();
    }

    public void updateDisplay() {
        if (tvExpression == null || tvResult == null) return;
        String expr = expression.toString();
        if (expr.isEmpty()) {
            lastValidLiveResult = null;
            tvExpression.setVisibility(View.GONE);
            updatingFromCode = true;
            tvExpression.setText("");
            updatingFromCode = false;
            tvResult.setText("0");
        } else {
            tvExpression.setVisibility(View.VISIBLE);
            updatingFromCode = true;
            tvExpression.setText(expr);
            int sel = (pendingSelectionAfterUpdate != null) ? Math.max(0, Math.min(pendingSelectionAfterUpdate, expr.length())) : expr.length();
            tvExpression.setSelection(sel);
            pendingSelectionAfterUpdate = null;
            updatingFromCode = false;
            String toEval = expandPercentages(expr).replace('×', '*').replace('÷', '/').replace('−', '-').replaceAll("\\s+", "");
            Double live = evaluateLiveExpression(toEval);
            if (live != null) {
                lastValidLiveResult = formatResult(live);
                tvResult.setText(lastValidLiveResult);
            } else {
                tvResult.setText("");
            }
        }
        scrollDisplayToEnd();
    }

    private void scrollDisplayToEnd() {
        if (displayScroll != null) {
            displayScroll.post(() -> displayScroll.fullScroll(View.FOCUS_RIGHT));
        }
    }

    private void hideSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && tvExpression != null) {
            imm.hideSoftInputFromWindow(tvExpression.getWindowToken(), 0);
        }
    }

    public void clearHistory() {
        sessionHistory.clear();
        oldHistory.clear();
        if (historyStorage != null) historyStorage.clear();
        refreshHistoryList();
    }

    /** Remove a single history entry at the given position (Recent or Old tab). */
    private void onHistoryEntryDismissed(int position) {
        List<HistoryEntry> list = getDisplayedHistoryList();
        if (position < 0 || position >= list.size()) return;
        list.remove(position);
        if (currentHistoryTab == 1 && historyStorage != null) {
            historyStorage.saveAll(oldHistory);
        }
        refreshHistoryList();
    }

    /**
     * Called when user taps "use expression" (<> ) in Old history. If current expression is not
     * empty, shows a warning popup; otherwise applies the history expression directly.
     */
    private void onUseExpressionFromHistory(String exprStr) {
        if (exprStr == null) return;
        String trimmed = exprStr.trim();
        if (trimmed.isEmpty()) return;
        boolean hasCurrentExpression = expression.length() > 0;
        if (hasCurrentExpression) {
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.history_replace_title)
                    .setMessage(R.string.history_replace_message)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> applyExpressionFromHistory(exprStr))
                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                    .show();
        } else {
            applyExpressionFromHistory(exprStr);
        }
    }

    /**
     * Replace the current expression with a history entry's expression (Old tab: "Use expression").
     */
    private void applyExpressionFromHistory(String exprStr) {
        if (exprStr == null) return;
        String trimmed = exprStr.trim();
        if (trimmed.isEmpty()) return;
        expression.setLength(0);
        expression.append(trimmed);
        syncStateFromExpression();
        isResultDisplayed = false;
        if (tvExpression != null) tvExpression.setVisibility(View.VISIBLE);
        if (tvResult != null) tvResult.setText("");
        updateDisplay();
    }

    /**
     * Insert history result as digits into the current expression at cursor (or append).
     * Does not replace the expression: e.g. "12+" + USE "20" → "12+20".
     */
    private void applyResultFromHistory(String resultStr) {
        if (resultStr == null || resultStr.isEmpty()) return;
        String digits = resultStr.replace(",", "").trim();
        try {
            Double.parseDouble(digits);
        } catch (NumberFormatException e) {
            return;
        }
        int pos = getInsertPosition();
        expression.insert(pos, digits);
        syncStateFromExpression();
        lastInputIsOperator = false;
        lastInputIsDecimal = false;
        isResultDisplayed = false;
        pendingSelectionAfterUpdate = pos + digits.length();
        if (tvExpression != null) tvExpression.setVisibility(View.VISIBLE);
        if (tvResult != null) tvResult.setText("");
        updateDisplay();
    }

    private void cancelResultAnimation() {
        if (resultCountUpAnimator != null && resultCountUpAnimator.isRunning()) {
            resultCountUpAnimator.cancel();
            resultCountUpAnimator = null;
        }
    }

    /** Subtle scale-in for a view (expression or result). */
    private void playResultScaleIn(View view) {
        if (view == null) return;
        view.setScaleX(0.94f);
        view.setScaleY(0.94f);
        ObjectAnimator sx = ObjectAnimator.ofFloat(view, View.SCALE_X, 0.94f, 1f);
        ObjectAnimator sy = ObjectAnimator.ofFloat(view, View.SCALE_Y, 0.94f, 1f);
        sx.setDuration(220);
        sy.setDuration(220);
        sx.setInterpolator(new DecelerateInterpolator());
        sy.setInterpolator(new DecelerateInterpolator());
        sx.start();
        sy.start();
    }
}
