package com.nuramin.calculator.favorites;

/** UI-only metadata for a favourite row (pin, priority, auto-delete). */
public final class FavoriteRowMeta {
    public static final int PRIORITY_LOW = 0;
    public static final int PRIORITY_MEDIUM = 1;
    public static final int PRIORITY_HIGH = 2;
    public static final int PRIORITY_CUSTOM = 3;

    public boolean pinned;
    public long pinnedAtMs;
    public int priorityLevel;
    public int customWeight;
    public boolean autoDeleteEnabled;
    public long autoDeleteExpireAtMs;

    public FavoriteRowMeta() {
        this.pinned = false;
        this.pinnedAtMs = 0L;
        this.priorityLevel = PRIORITY_MEDIUM;
        this.customWeight = 50;
        this.autoDeleteEnabled = false;
        this.autoDeleteExpireAtMs = 0L;
    }

    public int sortPriorityWeight() {
        switch (priorityLevel) {
            case PRIORITY_LOW:
                return 1;
            case PRIORITY_MEDIUM:
                return 2;
            case PRIORITY_HIGH:
                return 3;
            case PRIORITY_CUSTOM:
                return Math.max(1, Math.min(100, customWeight));
            default:
                return 0;
        }
    }

    /** High priority and very high custom scores skip timed auto-delete. */
    public boolean shouldBlockAutoDelete() {
        if (priorityLevel == PRIORITY_HIGH) return true;
        return priorityLevel == PRIORITY_CUSTOM && customWeight >= 85;
    }
}
