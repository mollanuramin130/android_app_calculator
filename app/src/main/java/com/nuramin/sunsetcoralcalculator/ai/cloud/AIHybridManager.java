package com.nuramin.sunsetcoralcalculator.ai.cloud;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.nuramin.sunsetcoralcalculator.ai.core.AIEngine;
import com.nuramin.sunsetcoralcalculator.ai.core.AIResult;
import com.nuramin.sunsetcoralcalculator.ai.core.IntentClassifier;

/**
 * Core logic controller: LOCAL AI vs cloud (Gemini free first, then DeepSeek).
 * - Simple input → AIEngine (offline).
 * - Complex input → Gemini API (free) or DeepSeek when key + network + limit allow; else local.
 * Failsafe: no internet or API failure → fallback to local AI. Never crash.
 */
public final class AIHybridManager {

    private static final String TAG = "AIHybridManager";

    private static final java.util.regex.Pattern SIMPLE_EXPRESSION = java.util.regex.Pattern.compile("^[\\d\\s.,+\\-×÷%()]+$", java.util.regex.Pattern.UNICODE_CASE);

    private final AIEngine localEngine = new AIEngine();
    private final GeminiService geminiService = new GeminiService();
    private final DeepSeekService deepSeekService = new DeepSeekService();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onResult(@NonNull AIResult result);
    }

    /**
     * Process input: use local AI for simple cases, else try DeepSeek (if connected and under limit).
     * Callback is invoked on the main thread.
     */
    public void processAsync(@NonNull String input, @NonNull Context context, @NonNull Callback callback) {
        String trimmed = input == null ? "" : input.trim();
        if (trimmed.isEmpty()) {
            mainHandler.post(() -> callback.onResult(AIResult.unknown("Try: 5000 loan at 8% for 5 years")));
            return;
        }

        if (isSimpleInput(trimmed)) {
            AIResult result = localEngine.process(trimmed);
            mainHandler.post(() -> callback.onResult(result));
            return;
        }

        // Complex input: try cloud if network + limit OK
        Context appContext = context.getApplicationContext();
        if (!NetworkUtil.isConnected(appContext)) {
            AIResult fallback = localEngine.process(trimmed);
            mainHandler.post(() -> callback.onResult(fallback));
            return;
        }

        APILimiter limiter = new APILimiter(appContext);
        if (!limiter.canCallAPI()) {
            AIResult fallback = localEngine.process(trimmed);
            mainHandler.post(() -> callback.onResult(fallback));
            return;
        }

        String geminiKey = getGeminiKey(appContext);
        String deepSeekKey = getDeepSeekKey(appContext);

        if (geminiKey != null && !geminiKey.isEmpty()) {
            geminiService.chat(trimmed, geminiKey, new GeminiService.Callback() {
                @Override
                public void onSuccess(@NonNull String content) {
                    limiter.increaseCount();
                    AIResult cloudResult = new AIResult(AIResult.Type.UNKNOWN, "AI", content, "", true);
                    mainHandler.post(() -> callback.onResult(cloudResult));
                }

                @Override
                public void onError(@NonNull String message) {
                    Log.e(TAG, "Gemini API error: " + message);
                    tryDeepSeekFallback(trimmed, deepSeekKey, limiter, callback, message);
                }
            });
            return;
        }

        if (deepSeekKey != null && !deepSeekKey.isEmpty()) {
            callDeepSeek(trimmed, deepSeekKey, limiter, callback);
            return;
        }

        Log.w(TAG, "No cloud API key. Set Gemini (free) in menu (⋮) > API key, or add GEMINI_API_KEY to local.properties.");
        AIResult fallback = localEngine.process(trimmed);
        AIResult withHint = new AIResult(fallback.getType(), fallback.getTitle(),
                fallback.getResultText(), "No API key. Use menu (⋮) > API key to paste your free Gemini key (aistudio.google.com/apikey).", false);
        mainHandler.post(() -> callback.onResult(withHint));
    }

    private void tryDeepSeekFallback(String trimmed, String deepSeekKey, APILimiter limiter, Callback callback, String geminiError) {
        if (deepSeekKey != null && !deepSeekKey.isEmpty()) {
            callDeepSeek(trimmed, deepSeekKey, limiter, callback);
        } else {
            AIResult fallback = localEngine.process(trimmed);
            AIResult withError = new AIResult(fallback.getType(), fallback.getTitle(),
                    fallback.getResultText(), "Cloud failed: " + geminiError, fallback.isSuccess());
            mainHandler.post(() -> callback.onResult(withError));
        }
    }

    private void callDeepSeek(String trimmed, String apiKey, APILimiter limiter, Callback callback) {
        deepSeekService.chat(trimmed, apiKey, new DeepSeekService.Callback() {
            @Override
            public void onSuccess(@NonNull String content) {
                limiter.increaseCount();
                AIResult cloudResult = new AIResult(AIResult.Type.UNKNOWN, "AI", content, "", true);
                mainHandler.post(() -> callback.onResult(cloudResult));
            }

            @Override
            public void onError(@NonNull String message) {
                Log.e(TAG, "DeepSeek API error: " + message);
                AIResult fallback = localEngine.process(trimmed);
                AIResult withError = new AIResult(fallback.getType(), fallback.getTitle(),
                        fallback.getResultText(), "Cloud failed: " + message, fallback.isSuccess());
                mainHandler.post(() -> callback.onResult(withError));
            }
        });
    }

    /**
     * Simple input = classified as EMI/AGE/DISCOUNT/GST, or looks like a math expression (digits/operators only).
     * Question-like input (what, how, explain, ?, etc.) always goes to cloud when possible.
     */
    private boolean isSimpleInput(@NonNull String input) {
        if (isClearlyComplexQuestion(input)) return false;
        AIResult.Type type = IntentClassifier.classify(input);
        if (type != AIResult.Type.UNKNOWN) return true;
        return SIMPLE_EXPRESSION.matcher(input).matches();
    }

    /** True if input looks like a natural language question → use DeepSeek API. */
    private static boolean isClearlyComplexQuestion(@NonNull String input) {
        if (input.length() > 50) return true;
        String lower = input.toLowerCase().trim();
        return lower.contains("what") || lower.contains("how") || lower.contains("explain")
                || lower.contains("why") || lower.contains("tell me") || lower.contains("define")
                || lower.contains("?") || lower.contains("difference between")
                || lower.startsWith("can you") || lower.startsWith("could you");
    }

    private static String getGeminiKey(@NonNull Context context) {
        try {
            String k = com.nuramin.sunsetcoralcalculator.BuildConfig.GEMINI_API_KEY;
            if (k != null && !k.trim().isEmpty()) return k.trim();
        } catch (Exception ignored) { }
        String fromPrefs = ApiKeyPrefs.getGemini(context);
        return fromPrefs != null ? fromPrefs : "";
    }

    private static String getDeepSeekKey(@NonNull Context context) {
        try {
            String k = com.nuramin.sunsetcoralcalculator.BuildConfig.DEEPSEEK_API_KEY;
            if (k != null && !k.trim().isEmpty()) return k.trim();
        } catch (Exception ignored) { }
        String fromPrefs = ApiKeyPrefs.getDeepSeek(context);
        return fromPrefs != null ? fromPrefs : "";
    }
}
