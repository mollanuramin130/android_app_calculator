package com.nuramin.sunsetcoralcalculator;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.HorizontalScrollView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView tvExpression;
    private TextView tvResult;
    private HorizontalScrollView displayScroll;
    private Button btnEquals;

    /** Full expression as user types: e.g. "23×4-34/(10×3.4×2-1)-10+20×2" */
    private String expression = "";

    private boolean justComputed = false;

    private final List<String> historyList = new ArrayList<>();
    private static final int MAX_HISTORY = 50;

    private static final DecimalFormat FORMATTER;
    private static final DecimalFormat SCIENTIFIC_FORMATTER;
    private static final double LARGE_THRESHOLD = 1e9;
    private static final double SMALL_THRESHOLD = 1e-3;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        FORMATTER = new DecimalFormat("#,###.##", symbols);
        FORMATTER.setGroupingUsed(true);
        SCIENTIFIC_FORMATTER = new DecimalFormat("0.####E0", symbols);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvExpression = findViewById(R.id.tv_expression);
        tvResult = findViewById(R.id.tv_result);
        displayScroll = findViewById(R.id.display_scroll);

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

        findViewById(R.id.btn_plus).setOnClickListener(v -> appendOperator("+"));
        findViewById(R.id.btn_minus).setOnClickListener(v -> appendOperator("−"));
        findViewById(R.id.btn_multiply).setOnClickListener(v -> appendOperator("×"));
        findViewById(R.id.btn_divide).setOnClickListener(v -> appendOperator("÷"));
        findViewById(R.id.btn_lparen).setOnClickListener(v -> appendChar("("));
        findViewById(R.id.btn_rparen).setOnClickListener(v -> appendChar(")"));
        btnEquals = findViewById(R.id.btn_equals);
        btnEquals.setEnabled(true);
        btnEquals.setOnClickListener(v -> {
            onEquals();
            // Keep equals button clickable and clear stuck pressed state
            btnEquals.post(() -> {
                btnEquals.setEnabled(true);
                btnEquals.refreshDrawableState();
            });
        });
        findViewById(R.id.btn_clear).setOnClickListener(v -> onClear());
        findViewById(R.id.btn_percent).setOnClickListener(v -> onPercent());

        ImageButton btnHistory = findViewById(R.id.btn_history);
        btnHistory.setOnClickListener(v -> showHistoryDialog());

        ImageButton btnBackspace = findViewById(R.id.btn_backspace);
        btnBackspace.setOnClickListener(v -> onBackspace());

        Button btnDelete = findViewById(R.id.btn_delete);
        btnDelete.setOnClickListener(v -> onBackspace());

        updateDisplay();
    }

    private void setNumberButton(int id, String digit) {
        findViewById(id).setOnClickListener(v -> onDigit(digit));
    }

    private void onDigit(String digit) {
        if (justComputed) {
            expression = "";
            justComputed = false;
        }
        if (digit.equals(".")) {
            if (lastNumberHasDot()) return;
            if (expression.isEmpty() || isOperatorOrParen(lastChar())) expression += "0";
            expression += ".";
        } else {
            if (expression.equals("0") && digit.equals("0")) return;
            if (expression.equals("0") && !digit.equals(".")) expression = "";
            expression += digit;
        }
        updateDisplay();
    }

    private boolean lastNumberHasDot() {
        int i = expression.length() - 1;
        while (i >= 0) {
            char c = expression.charAt(i);
            if (c == '.') return true;
            if (c == '+' || c == '−' || c == '×' || c == '÷' || c == '(' || c == ')') return false;
            i--;
        }
        return false;
    }

    private boolean isOperatorOrParen(char c) {
        return c == '+' || c == '−' || c == '×' || c == '÷' || c == '(' || c == ')';
    }

    private char lastChar() {
        return expression.isEmpty() ? '\0' : expression.charAt(expression.length() - 1);
    }

    private void appendOperator(String op) {
        if (justComputed) {
            expression = "";
            justComputed = false;
        }
        if (expression.isEmpty() && (op.equals("+") || op.equals("−"))) {
            expression += op;
        } else if (!expression.isEmpty() && isOperatorOrParen(lastChar()) && lastChar() != ')') {
            expression = expression.substring(0, expression.length() - 1) + op;
        } else if (!expression.isEmpty()) {
            expression += op;
        }
        updateDisplay();
    }

    private void appendChar(String s) {
        if (justComputed) {
            expression = "";
            justComputed = false;
        }
        expression += s;
        updateDisplay();
    }

    private void onEquals() {
        if (expression.trim().isEmpty()) return;
        String expr = expression.trim();
        // Replace %: e.g. "50%" -> "50/100"
        expr = expr.replaceAll("(\\d+\\.?\\d*)%", "($1/100)");
        String toEval = expr.replace('×', '*').replace('÷', '/').replace('−', '-');
        Double result = evaluateExpression(toEval);
        if (result == null) {
            tvResult.setText("Error");
            scrollDisplayToEnd();
            return;
        }
        String resultStr = formatNumber(result);
        String historyEntry = expression + "\n= " + resultStr;
        addToHistory(historyEntry);
        tvExpression.setText(expression);
        tvExpression.setVisibility(View.VISIBLE);
        tvResult.setText("= " + resultStr);
        expression = resultStr.replace(",", "");
        justComputed = true;
        scrollDisplayToEnd();
    }

    private void onClear() {
        expression = "";
        justComputed = false;
        tvExpression.setText("");
        tvExpression.setVisibility(View.GONE);
        tvResult.setText("0");
        scrollDisplayToEnd();
    }

    private void onBackspace() {
        if (justComputed) return;
        if (expression.isEmpty()) return;
        expression = expression.substring(0, expression.length() - 1);
        updateDisplay();
    }

    private void onPercent() {
        if (justComputed) expression = "";
        justComputed = false;
        if (expression.isEmpty()) return;
        expression += "%";
        updateDisplay();
    }

    private void updateDisplay() {
        if (expression.isEmpty()) {
            tvExpression.setVisibility(View.GONE);
            tvResult.setText("0");
        } else {
            tvExpression.setVisibility(View.VISIBLE);
            tvExpression.setText(expression);
            tvResult.setText("");
        }
        scrollDisplayToEnd();
    }

    private void scrollDisplayToEnd() {
        if (displayScroll != null) {
            displayScroll.post(() -> displayScroll.fullScroll(View.FOCUS_RIGHT));
        }
    }

    /**
     * Evaluates a mathematical expression with +, -, *, / and parentheses.
     * Returns null on error.
     */
    private Double evaluateExpression(String s) {
        s = s.replaceAll("\\s+", "");
        if (s.isEmpty()) return null;
        try {
            return new ExprParser(s).parse();
        } catch (Exception e) {
            return null;
        }
    }

    private static class ExprParser {
        private final String s;
        private int pos = 0;

        ExprParser(String s) {
            this.s = s;
        }

        private char peek() {
            return pos < s.length() ? s.charAt(pos) : '\0';
        }

        private void skipSpaces() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
        }

        private double parse() {
            skipSpaces();
            double v = parseExpr();
            skipSpaces();
            if (pos != s.length()) throw new RuntimeException("Unexpected character");
            return v;
        }

        private double parseExpr() {
            double left = parseTerm();
            while (true) {
                skipSpaces();
                char c = peek();
                if (c == '+') {
                    pos++;
                    left += parseTerm();
                } else if (c == '-') {
                    pos++;
                    left -= parseTerm();
                } else break;
            }
            return left;
        }

        private double parseTerm() {
            double left = parseFactor();
            while (true) {
                skipSpaces();
                char c = peek();
                if (c == '*') {
                    pos++;
                    left *= parseFactor();
                } else if (c == '/') {
                    pos++;
                    double right = parseFactor();
                    if (right == 0) throw new RuntimeException("Divide by zero");
                    left /= right;
                } else break;
            }
            return left;
        }

        private double parseFactor() {
            skipSpaces();
            char c = peek();
            if (c == '(') {
                pos++;
                double v = parseExpr();
                skipSpaces();
                if (peek() != ')') throw new RuntimeException("Missing )");
                pos++;
                return v;
            }
            if (c == '-') {
                pos++;
                return -parseFactor();
            }
            if (c == '+') {
                pos++;
                return parseFactor();
            }
            int start = pos;
            if (pos < s.length() && (Character.isDigit(s.charAt(pos)) || s.charAt(pos) == '.')) {
                while (pos < s.length() && (Character.isDigit(s.charAt(pos)) || s.charAt(pos) == '.')) pos++;
                if (pos < s.length() && (s.charAt(pos) == 'e' || s.charAt(pos) == 'E')) {
                    pos++;
                    if (pos < s.length() && (s.charAt(pos) == '+' || s.charAt(pos) == '-')) pos++;
                    while (pos < s.length() && Character.isDigit(s.charAt(pos))) pos++;
                }
                String num = s.substring(start, pos);
                try {
                    return Double.parseDouble(num);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Invalid number");
                }
            }
            throw new RuntimeException("Unexpected: " + c);
        }
    }

    private void addToHistory(String entry) {
        historyList.add(0, entry);
        if (historyList.size() > MAX_HISTORY) {
            historyList.remove(historyList.size() - 1);
        }
    }

    private void showHistoryDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_history, null);
        ListView listView = dialogView.findViewById(R.id.history_list);
        TextView emptyView = dialogView.findViewById(R.id.history_empty);

        AlertDialog historyDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        if (historyList.isEmpty()) {
            listView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            emptyView.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
            List<String> reversed = new ArrayList<>(historyList);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, reversed);
            listView.setAdapter(adapter);
            listView.setOnItemClickListener((parent, view, position, id) -> {
                String item = reversed.get(position);
                int eq = item.lastIndexOf("= ");
                if (eq >= 0) {
                    String resultPart = item.substring(eq + 2).trim().replace(",", "");
                    try {
                        double d = Double.parseDouble(resultPart);
                        expression = formatNumber(d).replace(",", "");
                        justComputed = true;
                        tvExpression.setText(expression);
                        tvExpression.setVisibility(View.VISIBLE);
                        tvResult.setText("= " + formatNumber(d));
                        scrollDisplayToEnd();
                    } catch (NumberFormatException ignored) { }
                }
                historyDialog.dismiss();
            });
        }

        historyDialog.show();
    }

    private String formatNumber(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "Error";
        }
        double abs = Math.abs(value);
        if (abs >= LARGE_THRESHOLD || (abs > 0 && abs < SMALL_THRESHOLD)) {
            return SCIENTIFIC_FORMATTER.format(value);
        }
        if (value == (long) value) {
            return FORMATTER.format((long) value);
        }
        return FORMATTER.format(value);
    }
}
