package com.nuramin.sunsetcoralcalculator.ai.voice;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
    private SpeechRecognizer speechRecognizer;
    private Callback callback;
    private boolean listening;

    public VoiceInputHelper(@NonNull Context context) {
        this.context = context.getApplicationContext();
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
                if (callback != null) callback.onError("Voice input isn't available on this device. Please type your question.");
                return;
            }
            if (speechRecognizer == null) {
                if (callback != null) callback.onError("Voice input isn't available on this device. Please type your question.");
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
                        if (callback != null) callback.onError("Voice isn't available. Please type your question above.");
                        return;
                    }
                    if (error == SpeechRecognizer.ERROR_NO_MATCH) {
                        if (callback != null) callback.onError("No speech heard. Try again or type your question.");
                        return;
                    }
                    if (error == SpeechRecognizer.ERROR_CLIENT) return; // User cancelled, no toast
                    if (callback != null) callback.onError("Voice input isn't available. Please type your question above.");
                }

                @Override
                public void onResults(Bundle results) {
                    listening = false;
                    ArrayList<String> list = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (list != null && !list.isEmpty() && callback != null) {
                        callback.onResult(list.get(0).trim());
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
            if (callback != null) callback.onError("Retry");
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
        if (speechRecognizer != null) {
            try { speechRecognizer.destroy(); } catch (Exception ignored) { }
            speechRecognizer = null;
        }
        callback = null;
    }
}
