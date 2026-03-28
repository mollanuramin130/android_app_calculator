package com.nuramin.sunsetcoralcalculator.ai.cloud;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import androidx.annotation.NonNull;

/**
 * Checks internet connectivity before making API calls.
 * Used by AIHybridManager to decide if cloud API can be used.
 */
public final class NetworkUtil {

    private NetworkUtil() {}

    /**
     * Returns true if the device has an active internet connection.
     */
    public static boolean isConnected(@NonNull Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network network = cm.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
}
