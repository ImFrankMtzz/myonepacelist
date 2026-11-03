package com.myonepacelist.player

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

/** True when the given capabilities cost data (mobile data, a phone hotspot, a metered Wi-Fi). */
fun NetworkCapabilities.isMetered(): Boolean {
    if (hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) return false
    // 5G plans the carrier marks as unlimited for now.
    return !(Build.VERSION.SDK_INT >= 30 && hasCapability(NetworkCapabilities.NET_CAPABILITY_TEMPORARILY_NOT_METERED))
}

/** Whether the active network costs data. No network at all counts as "not metered": the player shows its own error. */
fun Context.isOnMeteredNetwork(): Boolean {
    val cm = getSystemService(ConnectivityManager::class.java) ?: return false
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
    return caps.isMetered()
}
