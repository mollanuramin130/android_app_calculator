package com.nuramin.calculator.favorites;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nuramin.calculator.util.FavoriteStorage;
import com.nuramin.sunsetcoralcalculator.R;

import java.util.ArrayList;
import java.util.List;

/** Favourites list dialog with swipe rows and overflow menu. */
public final class FavoritesDialogHelper {

    private FavoritesDialogHelper() {}

    public static void show(AppCompatActivity activity) {
        FavoriteStorage.processExpiredAutoDeletes(activity);
        List<FavoriteStorage.FavoriteItem> items = new ArrayList<>(FavoriteStorage.getAll(activity));
        if (items.isEmpty()) {
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.favorites_title)
                    .setMessage(R.string.favorites_empty)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }

        View content = LayoutInflater.from(activity).inflate(R.layout.dialog_favorites, null, false);
        RecyclerView recyclerView = content.findViewById(R.id.fav_dialog_recycler);
        float density = activity.getResources().getDisplayMetrics().density;
        int maxPx = (int) (density * 420);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) recyclerView.getLayoutParams();
        lp.height = maxPx;
        recyclerView.setLayoutParams(lp);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        recyclerView.setHasFixedSize(false);
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.favorites_title)
                .setView(content)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        FavoritesRowAdapter adapter = new FavoritesRowAdapter(activity, items, dialog::dismiss);
        recyclerView.setAdapter(adapter);
        dialog.setOnDismissListener(d -> {
            adapter.stopTicker();
            adapter.closeSwipeIfAny();
        });
        dialog.show();
        adapter.startTicker();
    }
}
