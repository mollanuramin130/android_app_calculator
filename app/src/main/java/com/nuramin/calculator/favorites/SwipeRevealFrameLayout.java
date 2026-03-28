package com.nuramin.calculator.favorites;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

/**
 * Horizontal swipe: drag foreground to reveal left (pin) and right (delete) underlays.
 * Foreground translationX in [-rightW, +leftW]; positive reveals left strip.
 */
public final class SwipeRevealFrameLayout extends FrameLayout {

    public interface Listener {
        void onSwipeOpened(SwipeRevealFrameLayout layout, boolean leftOpen, boolean rightOpen);

        void onSwipeClosed(SwipeRevealFrameLayout layout);
    }

    private View leftPanel;
    private View rightPanel;
    private View foreground;
    private int leftWidth;
    private int rightWidth;
    private float translation;
    private float downX;
    private float downY;
    private float lastX;
    private int touchSlop;
    private boolean dragging;

    @Nullable
    private Listener listener;

    public SwipeRevealFrameLayout(Context context) {
        super(context);
        init(context);
    }

    public SwipeRevealFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SwipeRevealFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setClipChildren(false);
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() >= 3) {
            leftPanel = getChildAt(0);
            rightPanel = getChildAt(1);
            foreground = getChildAt(2);
            setupTouch();
        }
    }

    private void setupTouch() {
        if (foreground == null) return;
        foreground.setClickable(true);
        foreground.setOnTouchListener((v, event) -> handleTouch(event));
    }

    private boolean handleTouch(MotionEvent event) {
        if (foreground == null) return false;
        float x = event.getRawX();
        float y = event.getRawY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = x;
                downY = y;
                lastX = x;
                dragging = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = x - downX;
                float dy = y - downY;
                if (!dragging && (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop)) {
                    if (Math.abs(dx) > Math.abs(dy)) {
                        dragging = true;
                        requestAncestorsDisallowIntercept(true);
                    }
                }
                if (dragging) {
                    float delta = x - lastX;
                    lastX = x;
                    setTranslationRaw(translation + delta);
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                requestAncestorsDisallowIntercept(false);
                if (dragging) {
                    snap();
                } else {
                    float adx = Math.abs(x - downX);
                    float ady = Math.abs(y - downY);
                    if (adx < touchSlop && ady < touchSlop) {
                        foreground.performClick();
                    }
                }
                dragging = false;
                return true;
            default:
                return false;
        }
    }

    private void requestAncestorsDisallowIntercept(boolean disallow) {
        ViewParent p = getParent();
        while (p != null) {
            p.requestDisallowInterceptTouchEvent(disallow);
            if (p instanceof View) {
                p = ((View) p).getParent();
            } else {
                break;
            }
        }
    }

    private void setTranslationRaw(float tx) {
        float min = -rightWidth;
        float max = leftWidth;
        if (leftWidth <= 0 && rightWidth <= 0) {
            translation = 0;
        } else {
            translation = Math.max(min, Math.min(max, tx));
        }
        applyTranslation();
        notifyListenerMove();
    }

    private void notifyListenerMove() {
        if (listener == null) return;
        boolean leftOpen = translation > 2f;
        boolean rightOpen = translation < -2f;
        if (leftOpen || rightOpen) {
            listener.onSwipeOpened(this, leftOpen, rightOpen);
        }
    }

    private void snap() {
        float min = -rightWidth;
        float max = leftWidth;
        float thirdL = max / 3f;
        float thirdR = min / 3f;
        float target;
        if (translation > thirdL) {
            target = max;
        } else if (translation < thirdR) {
            target = min;
        } else {
            target = 0;
        }
        translation = target;
        applyTranslation();
        if (listener != null) {
            if (Math.abs(translation) < 1f) {
                listener.onSwipeClosed(this);
            } else {
                listener.onSwipeOpened(this, translation > 0, translation < 0);
            }
        }
    }

    private void applyTranslation() {
        if (foreground != null) {
            // Keep foreground above underlays even after calling close/openLeft/openRight
            // (close() does not trigger onLayout()).
            foreground.bringToFront();
            foreground.setTranslationX(translation);
        }
    }

    public void close() {
        translation = 0;
        applyTranslation();
        if (listener != null) listener.onSwipeClosed(this);
    }

    public void openLeft() {
        translation = leftWidth;
        applyTranslation();
        if (listener != null) listener.onSwipeOpened(this, true, false);
    }

    public void openRight() {
        translation = -rightWidth;
        applyTranslation();
        if (listener != null) listener.onSwipeOpened(this, false, true);
    }

    public boolean isOpen() {
        return Math.abs(translation) > 2f;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int w = right - left;
        int h = bottom - top;
        leftWidth = leftPanel != null ? leftPanel.getMeasuredWidth() : 0;
        rightWidth = rightPanel != null ? rightPanel.getMeasuredWidth() : 0;
        if (leftPanel != null) {
            leftPanel.layout(0, 0, leftWidth, h);
        }
        if (rightPanel != null) {
            rightPanel.layout(w - rightWidth, 0, w, h);
        }
        if (foreground != null) {
            // Always cover full container width so underlay icons can't peek from the sides.
            foreground.layout(0, 0, w, h);
            applyTranslation();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int hMode = MeasureSpec.getMode(heightMeasureSpec);
        int hSize = MeasureSpec.getSize(heightMeasureSpec);
        int maxH = 0;
        if (leftPanel != null) {
            measureChildWithMargins(leftPanel, widthMeasureSpec, 0, heightMeasureSpec, 0);
            maxH = Math.max(maxH, leftPanel.getMeasuredHeight());
        }
        if (rightPanel != null) {
            measureChildWithMargins(rightPanel, widthMeasureSpec, 0, heightMeasureSpec, 0);
            maxH = Math.max(maxH, rightPanel.getMeasuredHeight());
        }
        if (foreground != null) {
            int fwSpec = MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY);
            measureChild(foreground, fwSpec, heightMeasureSpec);
            maxH = Math.max(maxH, foreground.getMeasuredHeight());
        }
        int height = hMode == MeasureSpec.EXACTLY ? hSize : maxH + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(w, height);
    }
}
