package com.nuramin.calculator.favorites;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import com.nuramin.calculator.util.FavoriteStorage;
import com.nuramin.sunsetcoralcalculator.R;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Swipe + overflow menu for favourites list. */
public final class FavoritesRowAdapter extends RecyclerView.Adapter<FavoritesRowAdapter.VH> {

    private final AppCompatActivity activity;
    private final List<FavoriteStorage.FavoriteItem> items;
    private final Runnable onListEmptyDismiss;
    private final DateFormat dateFormat;
    private SwipeRevealFrameLayout currentOpenSwipe;
    private final Handler tickerHandler = new Handler(Looper.getMainLooper());
    private Runnable tickRunnable;
    private final float density;

    public FavoritesRowAdapter(AppCompatActivity activity, List<FavoriteStorage.FavoriteItem> items, Runnable onListEmptyDismiss) {
        this.activity = activity;
        this.items = items;
        this.onListEmptyDismiss = onListEmptyDismiss;
        this.dateFormat = android.text.format.DateFormat.getDateFormat(activity);
        this.density = activity.getResources().getDisplayMetrics().density;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_favorite_row_swipe, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        if (currentOpenSwipe == holder.swipe) currentOpenSwipe = null;
        holder.swipe.close();
        FavoriteStorage.FavoriteItem item = items.get(position);
        holder.title.setText(item.title);
        String when = item.timestamp > 0 ? dateFormat.format(new Date(item.timestamp)) : "";
        String screen = item.screen == null || item.screen.isEmpty() ? "" : " - " + item.screen;
        String noteLine = item.note == null ? "" : item.note.replace('\n', ' ');
        if (noteLine.length() > 80) noteLine = noteLine.substring(0, 80) + "...";
        holder.detail.setText(noteLine + "\n" + when + screen);

        holder.pinInline.setVisibility(item.pinned ? View.VISIBLE : View.GONE);

        FavoriteRowMeta meta = FavoriteRowMetadataStore.get(activity, item.id);
        int stripColor = FavoriteUrgencyHelper.priorityStripColor(activity, item.priorityLevel, item.customWeight);
        holder.badge.setText(FavoriteUrgencyHelper.priorityBadgeText(activity, item.priorityLevel, item.customWeight));
        applyPriorityStripAndBadge(holder, stripColor);

        long now = System.currentTimeMillis();
        if (meta.shouldBlockAutoDelete()) {
            holder.countdown.setVisibility(View.VISIBLE);
            holder.countdown.setText(R.string.favorites_row_protected);
            holder.countdown.setTextColor(ContextCompat.getColor(activity, R.color.fav_urgency_safe_text));
            holder.textColumn.setBackgroundColor(Color.TRANSPARENT);
        } else if (item.autoDeleteEnabled && item.autoDeleteExpireAtMs > 0) {
            FavoriteUrgencyHelper.Urgency u = FavoriteUrgencyHelper.urgencyForExpiry(item.autoDeleteExpireAtMs, now);
            holder.countdown.setVisibility(View.VISIBLE);
            holder.countdown.setText(FavoriteUrgencyHelper.formatRemaining(activity, item.autoDeleteExpireAtMs, now));
            holder.countdown.setTextColor(FavoriteUrgencyHelper.urgencyTextColor(activity, u));
            holder.textColumn.setBackgroundColor(FavoriteUrgencyHelper.urgencyRowOverlay(activity, u));
        } else {
            holder.countdown.setVisibility(View.GONE);
            holder.textColumn.setBackgroundColor(Color.TRANSPARENT);
        }

        holder.swipe.setListener(new SwipeRevealFrameLayout.Listener() {
            @Override
            public void onSwipeOpened(SwipeRevealFrameLayout layout, boolean leftOpen, boolean rightOpen) {
                if (currentOpenSwipe != null && currentOpenSwipe != layout) {
                    currentOpenSwipe.close();
                }
                currentOpenSwipe = layout;
            }

            @Override
            public void onSwipeClosed(SwipeRevealFrameLayout layout) {
                if (currentOpenSwipe == layout) currentOpenSwipe = null;
            }
        });

        holder.leftBg.setOnClickListener(v -> {
            FavoriteStorage.togglePinned(activity, item.id);
            holder.swipe.close();
            refreshFromStorage();
            Toast.makeText(activity,
                    FavoriteStorage.isPinned(activity, item.id)
                            ? activity.getString(R.string.favorites_pinned)
                            : activity.getString(R.string.favorites_unpinned_toast),
                    Toast.LENGTH_SHORT).show();
        });
        holder.rightBg.setOnClickListener(v -> confirmDelete(item));

        holder.foreground.setOnClickListener(v -> {
            if (holder.swipe.isOpen()) {
                holder.swipe.close();
                return;
            }
            FavoriteStorage.markUsed(activity, item.id);
            new AlertDialog.Builder(activity)
                    .setTitle(item.title)
                    .setMessage(item.note)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        });

        holder.menu.setOnClickListener(v -> showPopupMenu(holder, item));
    }

    private void applyPriorityStripAndBadge(VH holder, int stripColor) {
        float corner = 5f * density;
        GradientDrawable strip = new GradientDrawable();
        strip.setShape(GradientDrawable.RECTANGLE);
        strip.setCornerRadius(corner);
        strip.setColor(stripColor);
        holder.priorityStrip.setBackground(strip);

        GradientDrawable pill = new GradientDrawable();
        pill.setShape(GradientDrawable.RECTANGLE);
        pill.setCornerRadius(12f * density);
        pill.setColor(setAlphaComponent(stripColor, 0x44));
        holder.badge.setBackground(pill);
        int ph = Math.round(7f * density);
        int pv = Math.round(4f * density);
        holder.badge.setPadding(ph, pv, ph, pv);
        holder.badge.setTextColor(stripColor);
    }

    /**
     * Sets the alpha component on an ARGB color without needing androidx ColorUtils.
     * @param color ARGB packed int
     * @param alpha alpha in [0..255]
     */
    private static int setAlphaComponent(int color, int alpha) {
        int a = alpha & 0xFF;
        return (color & 0x00FFFFFF) | (a << 24);
    }

    private void confirmDelete(FavoriteStorage.FavoriteItem item) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.favorites_delete)
                .setMessage(R.string.favorites_delete_confirm)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    FavoriteStorage.remove(activity, item.id);
                    refreshFromStorage();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showPopupMenu(VH holder, FavoriteStorage.FavoriteItem item) {
        closeSwipeIfAny();
        PopupMenu pm = new PopupMenu(activity, holder.menu);
        boolean pinned = FavoriteStorage.isPinned(activity, item.id);
        pm.getMenu().add(0, 1, 0, pinned ? activity.getString(R.string.favorites_unpin) : activity.getString(R.string.favorites_pin));
        pm.getMenu().add(0, 2, 0, activity.getString(R.string.favorites_priority_low));
        pm.getMenu().add(0, 3, 0, activity.getString(R.string.favorites_priority_medium));
        pm.getMenu().add(0, 4, 0, activity.getString(R.string.favorites_priority_high));
        pm.getMenu().add(0, 5, 0, activity.getString(R.string.favorites_priority_custom));
        FavoriteRowMeta meta = FavoriteRowMetadataStore.get(activity, item.id);
        if (meta.autoDeleteEnabled) {
            pm.getMenu().add(0, 6, 0, activity.getString(R.string.favorites_auto_delete_disable));
        } else {
            pm.getMenu().add(0, 7, 0, activity.getString(R.string.favorites_auto_delete_enable));
        }
        pm.getMenu().add(0, 8, 0, activity.getString(R.string.favorites_edit_title));
        pm.getMenu().add(0, 9, 0, activity.getString(R.string.favorites_delete));

        pm.setOnMenuItemClickListener(mi -> {
            switch (mi.getItemId()) {
                case 1:
                    FavoriteStorage.togglePinned(activity, item.id);
                    refreshFromStorage();
                    return true;
                case 2:
                    FavoriteStorage.setPriority(activity, item.id, FavoriteRowMeta.PRIORITY_LOW, 50);
                    refreshFromStorage();
                    return true;
                case 3:
                    FavoriteStorage.setPriority(activity, item.id, FavoriteRowMeta.PRIORITY_MEDIUM, 50);
                    refreshFromStorage();
                    return true;
                case 4: {
                    boolean hadAd = FavoriteRowMetadataStore.get(activity, item.id).autoDeleteEnabled;
                    FavoriteStorage.setPriority(activity, item.id, FavoriteRowMeta.PRIORITY_HIGH, 50);
                    refreshFromStorage();
                    if (hadAd) {
                        Toast.makeText(activity, R.string.favorites_high_priority_clears_auto_delete, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(activity, R.string.favorites_hint_high_priority_auto_delete, Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
                case 5:
                    showCustomPriorityDialog(item);
                    return true;
                case 6:
                    FavoriteStorage.setAutoDelete(activity, item.id, false, 0L);
                    refreshFromStorage();
                    return true;
                case 7: {
                    FavoriteRowMeta m = FavoriteRowMetadataStore.get(activity, item.id);
                    if (FavoriteUrgencyHelper.blocksAutoDelete(m)) {
                        Toast.makeText(activity, R.string.favorites_auto_delete_blocked_high_priority, Toast.LENGTH_LONG).show();
                        return true;
                    }
                    long dur = FavoriteUrgencyHelper.suggestedAutoDeleteDurationMs(item.screen);
                    long expireAt = System.currentTimeMillis() + dur;
                    FavoriteStorage.setAutoDelete(activity, item.id, true, expireAt);
                    String label = FavoriteUrgencyHelper.formatSuggestedDurationLabel(activity, dur);
                    Toast.makeText(activity, activity.getString(R.string.favorites_auto_delete_enabled_toast, label), Toast.LENGTH_SHORT).show();
                    refreshFromStorage();
                    return true;
                }
                case 8:
                    showEditTitleDialog(item);
                    return true;
                case 9:
                    confirmDelete(item);
                    return true;
                default:
                    return false;
            }
        });
        pm.show();
    }

    private void showCustomPriorityDialog(FavoriteStorage.FavoriteItem item) {
        EditText input = new EditText(activity);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("1–100");
        FavoriteRowMeta m = FavoriteRowMetadataStore.get(activity, item.id);
        input.setText(String.valueOf(m.customWeight));
        new AlertDialog.Builder(activity)
                .setTitle(R.string.favorites_priority_custom)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    int v = 50;
                    try {
                        v = Integer.parseInt(input.getText() == null ? "50" : input.getText().toString().trim());
                    } catch (NumberFormatException ignored) {
                    }
                    boolean hadAd = FavoriteRowMetadataStore.get(activity, item.id).autoDeleteEnabled;
                    FavoriteStorage.setPriority(activity, item.id, FavoriteRowMeta.PRIORITY_CUSTOM, v);
                    refreshFromStorage();
                    if (hadAd && v >= 85) {
                        Toast.makeText(activity, R.string.favorites_custom_priority_clears_auto_delete, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showEditTitleDialog(FavoriteStorage.FavoriteItem item) {
        EditText input = new EditText(activity);
        input.setText(item.title);
        input.setSelection(item.title.length());
        new AlertDialog.Builder(activity)
                .setTitle(R.string.favorites_edit_title)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    String t = input.getText() == null ? "" : input.getText().toString().trim();
                    if (TextUtils.isEmpty(t)) return;
                    FavoriteStorage.updateTitle(activity, item.id, t);
                    refreshFromStorage();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void refreshFromStorage() {
        items.clear();
        items.addAll(FavoriteStorage.getAll(activity));
        notifyDataSetChanged();
        if (items.isEmpty() && onListEmptyDismiss != null) {
            onListEmptyDismiss.run();
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void closeSwipeIfAny() {
        if (currentOpenSwipe != null) {
            currentOpenSwipe.close();
            currentOpenSwipe = null;
        }
    }

    /** Batched refresh for countdown text; intervals adapt to nearest expiry. */
    public void startTicker() {
        stopTicker();
        tickRunnable = () -> {
            notifyDataSetChanged();
            scheduleNextTick();
        };
        tickerHandler.post(tickRunnable);
    }

    public void stopTicker() {
        tickerHandler.removeCallbacksAndMessages(null);
        tickRunnable = null;
    }

    private void scheduleNextTick() {
        if (tickRunnable == null) return;
        long delay = computeNextTickDelayMs();
        tickerHandler.postDelayed(tickRunnable, delay);
    }

    private long computeNextTickDelayMs() {
        long now = System.currentTimeMillis();
        long minRem = Long.MAX_VALUE;
        for (FavoriteStorage.FavoriteItem item : items) {
            if (!item.autoDeleteEnabled || item.autoDeleteExpireAtMs <= 0) continue;
            FavoriteRowMeta m = FavoriteRowMetadataStore.get(activity, item.id);
            if (m.shouldBlockAutoDelete()) continue;
            long r = item.autoDeleteExpireAtMs - now;
            if (r > 0 && r < minRem) minRem = r;
        }
        if (minRem == Long.MAX_VALUE) return 600_000L;
        if (minRem <= 10 * 60 * 1000L) return 30_000L;
        if (minRem <= 60 * 60 * 1000L) return 60_000L;
        if (minRem <= 24 * 60 * 60 * 1000L) return 300_000L;
        return 600_000L;
    }

    static final class VH extends RecyclerView.ViewHolder {
        final SwipeRevealFrameLayout swipe;
        final FrameLayout leftBg;
        final FrameLayout rightBg;
        final MaterialCardView foreground;
        final View priorityStrip;
        final TextView title;
        final TextView badge;
        final TextView detail;
        final TextView countdown;
        final LinearLayout textColumn;
        final ImageView pinInline;
        final ImageButton menu;

        VH(@NonNull View itemView) {
            super(itemView);
            swipe = itemView.findViewById(R.id.fav_swipe_root);
            leftBg = itemView.findViewById(R.id.fav_left_pin_bg);
            rightBg = itemView.findViewById(R.id.fav_right_delete_bg);
            foreground = itemView.findViewById(R.id.fav_foreground);
            priorityStrip = itemView.findViewById(R.id.fav_priority_strip);
            title = itemView.findViewById(R.id.fav_row_title);
            badge = itemView.findViewById(R.id.fav_row_badge);
            detail = itemView.findViewById(R.id.fav_row_detail);
            countdown = itemView.findViewById(R.id.fav_row_countdown);
            textColumn = itemView.findViewById(R.id.fav_row_text_column);
            pinInline = itemView.findViewById(R.id.fav_row_pin_inline);
            menu = itemView.findViewById(R.id.fav_row_menu);
        }
    }
}
