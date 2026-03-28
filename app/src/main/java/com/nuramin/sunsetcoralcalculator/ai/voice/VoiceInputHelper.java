package com.nuramin.sunsetcoralcalculator.ai.voice;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nuramin.sunsetcoralcalculator.R;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Voice input via SpeechRecognizer. Converts speech to text and returns to callback.
 * No network required for on-device recognition when available.
 */
public final class VoiceInputHelper {

    private static final String TAG = "VoiceInputHelper";

    public interface Callback {
        void onResult(@NonNull String text);
        void onError(@Nullable String message);
    }

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private SpeechRecognizer speechRecognizer;
    private Callback callback;
    private boolean listening;

    public VoiceInputHelper(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    private void dispatchResult(@NonNull String text) {
        mainHandler.post(() -> {
            if (callback != null) callback.onResult(text);
        });
    }

    private void dispatchError(@Nullable String message) {
        mainHandler.post(() -> {
            if (callback != null) callback.onError(message);
        });
    }

    public void setCallback(@Nullable Callback callback) {
        this.callback = callback;
    }

    public boolean isListening() {
        return listening;
    }

    public void startListening() {
        if (listening) return;
        if (speechRecognizer == null) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            } catch (Exception e) {
                Log.e(TAG, "createSpeechRecognizer", e);
                dispatchError(context.getString(R.string.ai_voice_unavailable));
                return;
            }
            if (speechRecognizer == null) {
                dispatchError(context.getString(R.string.ai_voice_unavailable));
                return;
            }
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) { listening = true; }

                @Override
                public void onBeginningOfSpeech() { }

                @Override
                public void onRmsChanged(float rmsdB) { }

                @Override
                public void onBufferReceived(byte[] buffer) { }

                @Override
                public void onEndOfSpeech() { listening = false; }

                @Override
                public void onError(int error) {
                    listening = false;
                    if (error == SpeechRecognizer.ERROR_SERVER || error == SpeechRecognizer.ERROR_NETWORK) {
                        dispatchError(context.getString(R.string.ai_voice_service_unavailable));
                        return;
                    }
                    if (error == SpeechRecognizer.ERROR_NO_MATCH) {
                        dispatchError(context.getString(R.string.ai_voice_no_speech));
                        return;
                    }
                    if (error == SpeechRecognizer.ERROR_CLIENT) return; // User cancelled, no toast
                    dispatchError(context.getString(R.string.ai_voice_unavailable));
                }

                @Override
                public void onResults(Bundle results) {
                    listening = false;
                    ArrayList<String> list = results != null
                            ? results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            : null;
                    if (list != null && !list.isEmpty()) {
                        dispatchResult(list.get(0).trim());
                    } else {
                        Log.d(TAG, "onResults: empty list (no toast)");
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) { }

                @Override
                public void onEvent(int eventType, Bundle params) { }
            });
        }
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        try {
            speechRecognizer.startListening(intent);
        } catch (Exception e) {
            Log.e(TAG, "startListening", e);
            dispatchError(context.getString(R.string.ai_voice_retry));
        }
    }

    public void stopListening() {
        if (speechRecognizer != null && listening) {
            try { speechRecognizer.stopListening(); } catch (Exception ignored) { }
            listening = false;
        }
    }

    public void destroy() {
        stopListening();
        mainHandler.removeCallbacksAndMessages(null);
        if (speechRecognizer != null) {
            try { speechRecognizer.destroy(); } catch (Exception ignored) { }
            speechRecognizer = null;
        }
        callback = null;
    }
}
