package com.nuramin.sunsetcoralcalculator.ai.cloud;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Requires Robolectric (Android {@link android.content.SharedPreferences}).
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class APILimiterTest {

    /** Must match {@link APILimiter} prefs name (package-private). */
    private static final String PREFS_NAME = "ai_cloud_api_limiter";

    @Before
    public void clearPrefs() {
        Context ctx = RuntimeEnvironment.getApplication();
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit();
    }

    @Test
    public void canCallAPI_freshPrefs_returnsTrue() {
        Context ctx = RuntimeEnvironment.getApplication();
        APILimiter limiter = new APILimiter(ctx);
        assertTrue(limiter.canCallAPI());
    }

    @Test
    public void increaseCount_thenRemainingDecreases() {
        Context ctx = RuntimeEnvironment.getApplication();
        APILimiter limiter = new APILimiter(ctx);
        assertTrue(limiter.canCallAPI());
        int before = limiter.getRemainingCalls();
        limiter.increaseCount();
        int after = limiter.getRemainingCalls();
        assertEquals(before - 1, after);
    }
}
