package com.nuramin.sunsetcoralcalculator.ai.ui;

import android.content.Intent;
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
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nuramin.sunsetcoralcalculator.R;
import com.nuramin.sunsetcoralcalculator.ai.cloud.ApiKeyPrefs;
import com.nuramin.sunsetcoralcalculator.ai.cloud.AIHybridManager;
import com.nuramin.sunsetcoralcalculator.ai.core.AIResult;
import com.nuramin.sunsetcoralcalculator.ai.voice.VoiceInputHelper;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

/**
 * AI Smart Calculator – chat-style UI with hybrid AI (local + DeepSeek when needed).
 * Shows loading state while API runs; share button in toolbar. Voice when available.
 */
public final class AISmartActivity extends AppCompatActivity {

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
            shareApp();
            return true;
        }
        if (id == R.id.ai_menu_api_key) {
            showApiKeyDialog();
            return true;
        }
        return false;
    }

    private void showApiKeyDialog() {
        String current = ApiKeyPrefs.get(this);
        final EditText edit = new EditText(this);
        edit.setHint("Paste Gemini key");
        edit.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        edit.setMinEms(20);
        if (current != null && !current.isEmpty()) {
            edit.setText(current);
            edit.setSelection(edit.getText().length());
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.ai_api_key_dialog_title)
                .setMessage(R.string.ai_api_key_dialog_message)
                .setView(edit)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String key = edit.getText() != null ? edit.getText().toString().trim() : "";
                    ApiKeyPrefs.set(AISmartActivity.this, key.isEmpty() ? null : key);
                    Toast.makeText(AISmartActivity.this,
                            key.isEmpty() ? R.string.ai_api_key_cleared : R.string.ai_api_key_saved,
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(getString(R.string.ai_api_key_clear_btn), (dialog, which) -> {
                    ApiKeyPrefs.set(AISmartActivity.this, null);
                    Toast.makeText(AISmartActivity.this, R.string.ai_api_key_cleared, Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void shareApp() {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, getString(R.string.ai_share_message));
        startActivity(Intent.createChooser(share, getString(R.string.ai_share_app)));
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
        setChipClick(R.id.ai_chip_emi, "5 lakh loan at 8% for 5 years");
        setChipClick(R.id.ai_chip_age, "age from 15 March 1990");
        setChipClick(R.id.ai_chip_gst, "1000 with 18% GST");
        setChipClick(R.id.ai_chip_discount, "500 with 20% discount");
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
            if (!loading) return;
            setLoading(false);
            Toast.makeText(AISmartActivity.this, R.string.ai_timeout_message, Toast.LENGTH_LONG).show();
            chatAdapter.addBotMessage(new AIResult(AIResult.Type.UNKNOWN, "Timeout",
                    getString(R.string.ai_timeout_fallback), "", false));
            scrollToBottom();
        };
        mainHandler.postDelayed(loadingTimeout, LOADING_TIMEOUT_MS);

        hybridManager.processAsync(text, this, new AIHybridManager.Callback() {
            @Override
            public void onResult(@NonNull AIResult result) {
                runOnUiThread(() -> {
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
        if (chatList != null && chatAdapter != null) {
            chatList.post(() -> chatList.smoothScrollToPosition(chatAdapter.getItemCount() - 1));
        }
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
