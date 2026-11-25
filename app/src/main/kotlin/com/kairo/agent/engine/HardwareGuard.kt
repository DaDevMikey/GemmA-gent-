package com.kairo.agent.engine

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager

data class HardwareState(
    val batteryPercent: Int,
    val isCharging: Boolean,
    val isThermallyThrottled: Boolean,
    val thermalLabel: String
)

/**
 * Hardware-aware guard for thermal and battery throttling of heavy on-device tasks.
 */
object HardwareGuard {
    fun getState(context: Context): HardwareState {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val pct = if (level >= 0 && scale > 0) (level * 100) / scale else 100
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        var throttled = false
        var thermalLabel = "Nominal"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val thermal = pm?.currentThermalStatus ?: PowerManager.THERMAL_STATUS_NONE
            throttled = thermal >= PowerManager.THERMAL_STATUS_SEVERE
            thermalLabel = when (thermal) {
                PowerManager.THERMAL_STATUS_NONE -> "Nominal"
                PowerManager.THERMAL_STATUS_LIGHT -> "Warm"
                PowerManager.THERMAL_STATUS_MODERATE -> "Moderate"
                PowerManager.THERMAL_STATUS_SEVERE -> "Severe (Throttling)"
                PowerManager.THERMAL_STATUS_CRITICAL,
                PowerManager.THERMAL_STATUS_EMERGENCY,
                PowerManager.THERMAL_STATUS_SHUTDOWN -> "Critical (Throttling)"
                else -> "Nominal"
            }
        }
        return HardwareState(
            batteryPercent = pct,
            isCharging = charging,
            isThermallyThrottled = throttled,
            thermalLabel = thermalLabel
        )
    }
}
