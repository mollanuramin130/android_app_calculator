package com.nuramin.calculator.util;

/**
 * Parses and evaluates mathematical expressions: +, -, *, /, ^, !, parentheses,
 * sqrt, sin, cos, tan, asin, acos, atan, ln, log, and constants π, e.
 * % is handled by BasicCalculatorScreen as percent (e.g. 100%10 = 10) before parsing.
 */
public final class ExprParser {
    private final String s;
    private final boolean useDegrees;
    private int pos = 0;

    public ExprParser(String s) {
        this(s, true);
    }

    public ExprParser(String s, boolean useDegrees) {
        this.s = s == null ? "" : s;
        this.useDegrees = useDegrees;
    }

    private char peek() {
        return pos < s.length() ? s.charAt(pos) : '\0';
    }

    private void skipSpaces() {
        while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
    }

    public double parse() {
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
        double left = parseFactorWithPowerAndFactorial();
        while (true) {
            skipSpaces();
            char c = peek();
            if (c == '*') {
                pos++;
                left *= parseFactorWithPowerAndFactorial();
            } else if (c == '/') {
                pos++;
                double right = parseFactorWithPowerAndFactorial();
                if (right == 0) throw new RuntimeException("Divide by zero");
                left /= right;
            } else break;
        }
        return left;
    }

    /** Parses factor, then optional ^ (right-associative) and postfix ! */
    private double parseFactorWithPowerAndFactorial() {
        double left = parseFactor();
        skipSpaces();
        if (peek() == '^') {
            pos++;
            double right = parseFactorWithPowerAndFactorial();
            double r = Math.pow(left, right);
            if (Double.isNaN(r) || Double.isInfinite(r)) throw new RuntimeException("Invalid power");
            left = r;
        }
        skipSpaces();
        while (peek() == '!') {
            pos++;
            left = factorial(left);
        }
        return left;
    }

    private static double factorial(double v) {
        if (v < 0 || v != Math.rint(v)) throw new RuntimeException("Factorial requires non-negative integer");
        int n = (int) v;
        if (n > 20) throw new RuntimeException("Factorial too large");
        long f = 1;
        for (int i = 2; i <= n; i++) f *= i;
        return (double) f;
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
        if (c == 'π') {
            pos++;
            return Math.PI;
        }
        if (Character.isLetter(c)) {
            int start = pos;
            while (pos < s.length() && Character.isLetter(s.charAt(pos))) pos++;
            String id = s.substring(start, pos);
            skipSpaces();
            switch (id) {
                case "e":
                    return Math.E;
                case "pi":
                    return Math.PI;
                case "sqrt":
                    if (peek() != '(') throw new RuntimeException("sqrt expects (");
                    pos++;
                    double sqrtArg = parseExpr();
                    if (sqrtArg < 0) throw new RuntimeException("sqrt of negative");
                    skipSpaces();
                    if (peek() != ')') throw new RuntimeException("Missing )");
                    pos++;
                    return Math.sqrt(sqrtArg);
                case "sin": {
                    if (peek() != '(') throw new RuntimeException("sin expects (");
                    pos++;
                    double x = parseExpr();
                    skipSpaces();
                    if (peek() != ')') throw new RuntimeException("Missing )");
                    pos++;
                    x = useDegrees ? Math.toRadians(x) : x;
                    return Math.sin(x);
                }
                case "cos": {
                    if (peek() != '(') throw new RuntimeException("cos expects (");
                    pos++;
                    double x = parseExpr();
                    skipSpaces();
                    if (peek() != ')') throw new RuntimeException("Missing )");
                    pos++;
                    x = useDegrees ? Math.toRadians(x) : x;
                    return Math.cos(x);
                }
                case "tan": {
                    if (peek() != '(') throw new RuntimeException("tan expects (");
                    pos++;
                    double x = parseExpr();
                    skipSpaces();
                    if (peek() != ')') throw new RuntimeException("Missing )");
                    pos++;
                    x = useDegrees ? Math.toRadians(x) : x;
                    return Math.tan(x);
                }
                case "asin": {
                    if (peek() != '(') throw new RuntimeException("asin expects (");
                    pos++;
                    double x = parseExpr();
                    if (x < -1 || x > 1) throw new RuntimeException("asin out of range");
                    skipSpaces();
                    if (peek() != ')') throw new RuntimeException("Missing )");
                    pos++;
                    double r = Math.asin(x);
                    return useDegrees ? Math.toDegrees(r) : r;
                }
                case "acos": {
                    if (peek() != '(') throw new RuntimeException("acos expects (");
                    pos++;
                    double x = parseExpr();
                    if (x < -1 || x > 1) throw new RuntimeException("acos out of range");
                    skipSpaces();
                    if (peek() != ')') throw new RuntimeException("Missing )");
                    pos++;
                    double r = Math.acos(x);
                    return useDegrees ? Math.toDegrees(r) : r;
                }
                case "atan": {
                    if (peek() != '(') throw new RuntimeException("atan expects (");
                    pos++;
                    double x = parseExpr();
                    skipSpaces();
                    if (peek() != ')') throw new RuntimeException("Missing )");
                    pos++;
                    double r = Math.atan(x);
                    return useDegrees ? Math.toDegrees(r) : r;
                }
                case "ln": {
                    if (peek() != '(') throw new RuntimeException("ln expects (");
                    pos++;
                    double x = parseExpr();
                    if (x <= 0) throw new RuntimeException("ln of non-positive");
                    skipSpaces();
                    if (peek() != ')') throw new RuntimeException("Missing )");
                    pos++;
                    return Math.log(x);
                }
                case "log": {
                    if (peek() != '(') throw new RuntimeException("log expects (");
                    pos++;
                    double x = parseExpr();
                    if (x <= 0) throw new RuntimeException("log of non-positive");
                    skipSpaces();
                    if (peek() != ')') throw new RuntimeException("Missing )");
                    pos++;
                    return Math.log10(x);
                }
                default:
                    throw new RuntimeException("Unknown: " + id);
            }
        }
        int start = pos;
        if (pos < s.length() && (Character.isDigit(s.charAt(pos)) || s.charAt(pos) == '.')) {
            while (pos < s.length() && (Character.isDigit(s.charAt(pos)) || s.charAt(pos) == '.')) pos++;
            if (pos < s.length() && (s.charAt(pos) == 'e' || s.charAt(pos) == 'E')) {
                int save = pos;
                pos++;
                if (pos < s.length() && (s.charAt(pos) == '+' || s.charAt(pos) == '-')) pos++;
                if (pos < s.length() && Character.isDigit(s.charAt(pos))) {
                    while (pos < s.length() && Character.isDigit(s.charAt(pos))) pos++;
                    String num = s.substring(start, pos);
                    try {
                        return Double.parseDouble(num);
                    } catch (NumberFormatException e) {
                        throw new RuntimeException("Invalid number");
                    }
                }
                pos = save;
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
