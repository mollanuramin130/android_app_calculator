package com.nuramin.calculator.util;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.inputmethod.InputMethodManager;
import android.widget.ScrollView;

import androidx.core.widget.NestedScrollView;

/**
 * Helper for converter screens: hide soft keyboard and scroll so result is visible after convert/calculate.
 */
public final class ConverterUiHelper {

    private static final int SCROLL_PADDING_DP = 24;

    private ConverterUiHelper() {}

    /**
     * Hides the soft keyboard. Call after user presses convert/calculate.
     */
    public static void hideSoftKeyboard(View view) {
        if (view == null) return;
        Context ctx = view.getContext();
        InputMethodManager imm = (InputMethodManager) ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) return;
        View focus = view.getRootView() != null ? view.getRootView().findFocus() : null;
        if (focus != null) {
            imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        } else {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * Finds the first ScrollView or NestedScrollView that contains the given view and scrolls
     * so that the result view is visible (with a small padding from top). Call after showing result.
     */
    public static void scrollToShowResult(View panelRoot, View resultView) {
        if (panelRoot == null || resultView == null) return;
        View scrollView = findScrollParent(resultView);
        if (scrollView == null) return;
        final int paddingPx = (int) (SCROLL_PADDING_DP * panelRoot.getResources().getDisplayMetrics().density);
        resultView.post(() -> {
            int offset = getOffsetFromScrollContent(scrollView, resultView);
            if (scrollView instanceof ScrollView) {
                int y = Math.max(0, offset - paddingPx);
                ((ScrollView) scrollView).smoothScrollTo(0, y);
            } else if (scrollView instanceof NestedScrollView) {
                int y = Math.max(0, offset - paddingPx);
                ((NestedScrollView) scrollView).smoothScrollTo(0, y);
            }
        });
    }

    private static View findScrollParent(View view) {
        ViewParent parent = view.getParent();
        while (parent instanceof ViewGroup) {
            if (parent instanceof ScrollView || parent instanceof NestedScrollView) {
                return (View) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    /** Y offset of the view relative to the scroll view's content (first child). */
    private static int getOffsetFromScrollContent(View scrollView, View target) {
        if (!(scrollView instanceof ViewGroup)) return 0;
        ViewGroup sg = (ViewGroup) scrollView;
        if (sg.getChildCount() == 0) return 0;
        View contentChild = sg.getChildAt(0);
        int offset = 0;
        View v = target;
        while (v != null && v != contentChild) {
            offset += v.getTop();
            if (v.getParent() instanceof View) {
                v = (View) v.getParent();
            } else {
                break;
            }
        }
        return offset;
    }
}
