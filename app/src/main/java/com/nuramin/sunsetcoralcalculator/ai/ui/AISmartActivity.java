package com.nuramin.sunsetcoralcalculator.ai.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.nuramin.sunsetcoralcalculator.R;
import com.nuramin.sunsetcoralcalculator.ai.system.ShareController;
import com.nuramin.sunsetcoralcalculator.ai.cloud.AIHybridManager;
import com.nuramin.sunsetcoralcalculator.ai.core.AIResult;
import com.nuramin.sunsetcoralcalculator.ai.voice.VoiceInputHelper;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

/**
 * AI Smart Calculator – chat-style UI with hybrid AI (local + cloud when needed).
 * Voice input is supported when available.
 */
public final class AISmartActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "calculator_prefs";
    private static final String KEY_THEME = "theme_mode";

    private EditText input;
    private RecyclerView chatList;
    private AIChatAdapter chatAdapter;
    private AIHybridManager hybridManager;
    private VoiceInputHelper voiceHelper;
    private View loadingOverlay;
    private ImageButton sendBtn;
    private ImageButton micBtn;
    private boolean loading;
    private Runnable loadingTimeout;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applySavedTheme();
        setContentView(R.layout.activity_ai_smart);

        hybridManager = new AIHybridManager();
        input = findViewById(R.id.ai_input);
        chatList = findViewById(R.id.ai_chat_list);
        loadingOverlay = findViewById(R.id.ai_loading_overlay);
        sendBtn = findViewById(R.id.ai_send);
        micBtn = findViewById(R.id.ai_mic);

        chatAdapter = new AIChatAdapter();
        chatList.setLayoutManager(new LinearLayoutManager(this));
        chatList.setAdapter(chatAdapter);
        addWelcomeMessage();

        setupToolbar();
        setupSendAndInput();
        setupChips();
        setupVoice();
    }

    /** Same prefs as MainActivity so Light / Dark / System applies here too. */
    private void applySavedTheme() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int mode = Math.max(0, Math.min(2, prefs.getInt(KEY_THEME, 2)));
        switch (mode) {
            case 0:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case 1:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    private void addWelcomeMessage() {
        chatAdapter.addBotMessage(new AIResult(AIResult.Type.UNKNOWN, getString(R.string.ai_smart_title),
                getString(R.string.ai_welcome_result),
                getString(R.string.ai_empty_try), false));
    }

    private void setupToolbar() {
        View toolbar = findViewById(R.id.ai_toolbar);
        if (toolbar instanceof com.google.android.material.appbar.MaterialToolbar) {
            com.google.android.material.appbar.MaterialToolbar t = (com.google.android.material.appbar.MaterialToolbar) toolbar;
            t.setNavigationOnClickListener(v -> finish());
            t.setOnMenuItemClickListener(this::onToolbarMenuItemClick);
        }
    }

    private boolean onToolbarMenuItemClick(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.ai_menu_share) {
            ShareController.shareApp(this, getString(R.string.share_via_chooser));
            return true;
        }
        return false;
    }

    private void setupSendAndInput() {
        if (sendBtn != null) sendBtn.setOnClickListener(v -> processInput());
        if (input != null) {
            input.setOnEditorActionListener((v, actionId, event) -> {
                processInput();
                return true;
            });
        }
    }

    private void setupChips() {
        setChipClick(R.id.ai_chip_emi, getString(R.string.ai_prompt_emi_example));
        setChipClick(R.id.ai_chip_age, getString(R.string.ai_prompt_age_example));
        setChipClick(R.id.ai_chip_gst, getString(R.string.ai_prompt_tax_example));
        setChipClick(R.id.ai_chip_discount, getString(R.string.ai_prompt_discount_example));
    }

    private void setChipClick(int id, String text) {
        View v = findViewById(id);
        if (v instanceof Chip) {
            ((Chip) v).setOnClickListener(v1 -> {
                if (input != null) input.setText(text);
                processInput();
            });
        }
    }

    private void setupVoice() {
        voiceHelper = new VoiceInputHelper(this);
        voiceHelper.setCallback(new VoiceInputHelper.Callback() {
            @Override
            public void onResult(String text) {
                if (input != null) input.setText(text);
                processInput();
            }

            @Override
            public void onError(String message) {
                if (message != null) Toast.makeText(AISmartActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
        ActivityResultLauncher<String> requestPermission = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> { if (granted && voiceHelper != null) voiceHelper.startListening(); });
        if (micBtn != null) micBtn.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && ContextCompat.checkSelfPermission(AISmartActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermission.launch(Manifest.permission.RECORD_AUDIO);
                return;
            }
            voiceHelper.startListening();
        });
    }

    private static final int LOADING_TIMEOUT_MS = 35_000;

    private void processInput() {
        String text = input != null ? input.getText().toString().trim() : "";
        if (text.isEmpty() || loading) return;

        if (input != null) input.setText("");
        chatAdapter.addUserMessage(text);
        setLoading(true);
        cancelLoadingTimeout();
        loadingTimeout = () -> {
            if (!loading || isFinishing() || isDestroyed()) return;
            setLoading(false);
            Toast.makeText(AISmartActivity.this, R.string.ai_timeout_message, Toast.LENGTH_LONG).show();
            chatAdapter.addBotMessage(new AIResult(AIResult.Type.UNKNOWN, getString(R.string.ai_timeout_title),
                    getString(R.string.ai_timeout_fallback), "", false));
            scrollToBottom();
        };
        mainHandler.postDelayed(loadingTimeout, LOADING_TIMEOUT_MS);

        hybridManager.processAsync(text, getApplicationContext(), new AIHybridManager.Callback() {
            @Override
            public void onResult(@NonNull AIResult result) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    cancelLoadingTimeout();
                    setLoading(false);
                    chatAdapter.addBotMessage(result);
                    scrollToBottom();
                    if (!result.isSuccess() && result.getExplanation() != null && !result.getExplanation().isEmpty()) {
                        Toast.makeText(AISmartActivity.this, result.getExplanation(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void cancelLoadingTimeout() {
        if (loadingTimeout != null) {
            mainHandler.removeCallbacks(loadingTimeout);
            loadingTimeout = null;
        }
    }

    private void setLoading(boolean show) {
        loading = show;
        if (loadingOverlay != null) loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        if (sendBtn != null) sendBtn.setEnabled(!show);
        if (micBtn != null) micBtn.setEnabled(!show);
    }

    private void scrollToBottom() {
        if (chatList == null || chatAdapter == null) return;
        int n = chatAdapter.getItemCount();
        if (n <= 0) return;
        chatList.post(() -> {
            if (isFinishing() || isDestroyed()) return;
            chatList.smoothScrollToPosition(n - 1);
        });
    }

    @Override
    protected void onDestroy() {
        cancelLoadingTimeout();
        if (voiceHelper != null) {
            voiceHelper.destroy();
            voiceHelper = null;
        }
        super.onDestroy();
    }
}
