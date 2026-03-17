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
 * Core logic controller: decides LOCAL AI vs DeepSeek API.
 * - Simple input (EMI, age, discount, GST, or mostly numbers/operators) → existing AIEngine (offline).
 * - Complex input → DeepSeek API when network and API limit allow; else fallback to local AI.
 * Failsafe: no internet or API failure → always fallback to local AI. Never crash.
 */
public final class AIHybridManager {

    private static final String TAG = "AIHybridManager";

    /** Pattern: mostly digits, spaces, and simple operators (for "simple" expression-like input). */
    private static final java.util.regex.Pattern SIMPLE_EXPRESSION = java.util.regex.Pattern.compile("^[\\d\\s.,+\\-×÷%()]+$", java.util.regex.Pattern.UNICODE_CASE);

    private final AIEngine localEngine = new AIEngine();
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

        String apiKey = getApiKey(appContext);
        if (apiKey == null || apiKey.isEmpty()) {
            Log.w(TAG, "DeepSeek API key empty. Set it in AI screen menu (⋮) > API key, or add DEEPSEEK_API_KEY to local.properties and Rebuild.");
            AIResult fallback = localEngine.process(trimmed);
            AIResult withHint = new AIResult(fallback.getType(), fallback.getTitle(),
                    fallback.getResultText(), "No API key. Use menu (⋮) > API key to paste your DeepSeek key.", false);
            mainHandler.post(() -> callback.onResult(withHint));
            return;
        }

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
                String errNote = "Cloud failed: " + message;
                AIResult withError = new AIResult(fallback.getType(), fallback.getTitle(),
                        fallback.getResultText(), errNote, fallback.isSuccess());
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

    /** API key: BuildConfig (from local.properties) first, then SharedPreferences (set in-app via menu). */
    private static String getApiKey(@NonNull Context context) {
        String fromBuild = "";
        try {
            String k = com.nuramin.sunsetcoralcalculator.BuildConfig.DEEPSEEK_API_KEY;
            fromBuild = (k != null ? k.trim() : "");
        } catch (Exception ignored) { }
        if (fromBuild.length() > 0) return fromBuild;
        String fromPrefs = ApiKeyPrefs.get(context);
        return fromPrefs != null ? fromPrefs : "";
    }
}
