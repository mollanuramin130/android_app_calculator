package com.nuramin.calculator.basic;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.nuramin.calculator.R;
import com.nuramin.calculator.adapter.HistoryAdapter;
import com.nuramin.calculator.common.AppConstants;
import com.nuramin.calculator.util.CalculatorUtils;
import com.nuramin.calculator.util.ExprParser;

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

    // ---- Core state ----
    private final StringBuilder expression = new StringBuilder();
    private boolean lastInputIsOperator = false;
    private boolean lastInputIsDecimal = false;
    private boolean isResultDisplayed = false;
    private int openParenthesisCount = 0;

    private boolean updatingFromCode = false;
    private final List<String> historyList = new ArrayList<>();
    private HistoryAdapter historyAdapter;

    /** Scientific mode: angle unit for sin/cos/tan (true = degrees, false = radians). */
    private boolean degMode = true;
    /** Scientific mode: when true, sin→asin, cos→acos, tan→atan. */
    private boolean invMode = false;

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
            btnBackspace.setOnClickListener(v -> deleteLast());
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

        historyAdapter = new HistoryAdapter(activity, historyList, this::applyResultFromHistory);
        if (panelHistoryList != null) {
            panelHistoryList.setAdapter(historyAdapter);
            panelHistoryList.setOnItemClickListener((parent, view, position, id) -> {});
        }

        updateDisplay();
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

    private char lastChar() {
        return expression.length() == 0 ? '\0' : expression.charAt(expression.length() - 1);
    }

    // ---- 3. Number handling ----
    private void handleNumber(String value) {
        if (isResultDisplayed) {
            expression.setLength(0);
            isResultDisplayed = false;
        }
        // Prevent leading zero duplication (00, 000): current number already "0" only
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
        int len = expression.length();
        if (len == 0) {
            if (op.equals(OP_MINUS)) expression.append(op);
            lastInputIsOperator = true;
            lastInputIsDecimal = false;
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

    // ---- 7. Delete last ----
    private void deleteLast() {
        if (expression.length() == 0) return;
        if (isResultDisplayed) return;
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
            tvResult.setText("Error");
            expression.setLength(0);
            lastInputIsOperator = false;
            lastInputIsDecimal = false;
            isResultDisplayed = false;
            openParenthesisCount = 0;
            updatingFromCode = true;
            if (tvExpression != null) {
                tvExpression.setText("");
                tvExpression.setVisibility(View.GONE);
            }
            updatingFromCode = false;
            scrollDisplayToEnd();
            return;
        }

        String resultStr = formatResult(result);
        saveToHistory(exprStr, resultStr);

        updatingFromCode = true;
        if (tvExpression != null) {
            tvExpression.setText(expression.toString());
            tvExpression.setVisibility(View.VISIBLE);
            tvExpression.setSelection(expression.length());
        }
        tvResult.setText("= " + resultStr);
        expression.setLength(0);
        expression.append(resultStr.replace(",", ""));
        updatingFromCode = false;
        lastInputIsOperator = false;
        lastInputIsDecimal = false;
        isResultDisplayed = true;
        openParenthesisCount = 0;
        if (historyAdapter != null) historyAdapter.notifyDataSetChanged();
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
        String entry = expressionString + "\n= " + resultString;
        historyList.add(0, entry);
        if (historyList.size() > AppConstants.MAX_HISTORY_ITEMS) {
            historyList.remove(historyList.size() - 1);
        }
    }

    public void updateDisplay() {
        if (tvExpression == null || tvResult == null) return;
        String expr = expression.toString();
        if (expr.isEmpty()) {
            tvExpression.setVisibility(View.GONE);
            updatingFromCode = true;
            tvExpression.setText("");
            updatingFromCode = false;
            tvResult.setText("0");
        } else {
            tvExpression.setVisibility(View.VISIBLE);
            updatingFromCode = true;
            tvExpression.setText(expr);
            tvExpression.setSelection(expr.length());
            updatingFromCode = false;
            String toEval = expandPercentages(expr).replace('×', '*').replace('÷', '/').replace('−', '-').replaceAll("\\s+", "");
            Double live = evaluateExpression(toEval);
            if (live != null) {
                tvResult.setText("= " + formatResult(live));
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

    public void clearHistory() {
        historyList.clear();
        if (historyAdapter != null) historyAdapter.notifyDataSetChanged();
    }

    private void applyResultFromHistory(String resultStr) {
        try {
            double d = Double.parseDouble(resultStr);
            expression.setLength(0);
            expression.append(CalculatorUtils.formatNumber(d).replace(",", ""));
            lastInputIsOperator = false;
            lastInputIsDecimal = false;
            isResultDisplayed = true;
            openParenthesisCount = 0;
            updatingFromCode = true;
            if (tvExpression != null) {
                tvExpression.setText(expression.toString());
                tvExpression.setVisibility(View.VISIBLE);
            }
            tvResult.setText("= " + CalculatorUtils.formatNumber(d));
            updatingFromCode = false;
            updateDisplay();
        } catch (NumberFormatException ignored) {}
    }
}
