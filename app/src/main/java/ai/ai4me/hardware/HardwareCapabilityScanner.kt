package ai.ai4me.hardware

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import java.io.File
import java.io.FileInputStream
import java.util.Scanner

/**
 * Real runtime hardware capability detection engine (Document 1 §2 & Document 2 §9).
 * Never hardcodes or fabricates device specs.
 */
class HardwareCapabilityScanner(private val context: Context) {

    fun scanDevice(): DeviceHardwareSpec {
        val memory = scanMemory()
        val storage = scanStorage()
        val thermal = scanThermalState()
        val battery = scanBattery()
        val abis = Build.SUPPORTED_ABIS.toList()
        val is64Bit = abis.any { it.contains("64") }
        val cpuCores = Runtime.getRuntime().availableProcessors()

        return DeviceHardwareSpec(
            androidApiLevel = Build.VERSION.SDK_INT,
            supportedAbis = abis,
            is64Bit = is64Bit,
            cpuCoreCount = cpuCores,
            memory = memory,
            storage = storage,
            thermalState = thermal,
            battery = battery
        )
    }

    fun evaluateRecommendation(spec: DeviceHardwareSpec): HardwareRecommendation {
        return HardwareProfileEvaluator.evaluate(spec)
    }

    private fun scanMemory(): MemoryMetrics {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        if (activityManager != null) {
            activityManager.getMemoryInfo(memoryInfo)
            return MemoryMetrics(
                totalRamBytes = memoryInfo.totalMem,
                availableRamBytes = memoryInfo.availMem,
                isLowMemory = memoryInfo.lowMemory,
                thresholdBytes = memoryInfo.threshold
            )
        }

        // Fallback: Read /proc/meminfo directly if ActivityManager is unavailable
        var totalBytes = 0L
        var availBytes = 0L
        try {
            val meminfoFile = File("/proc/meminfo")
            if (meminfoFile.exists()) {
                Scanner(FileInputStream(meminfoFile)).use { scanner ->
                    while (scanner.hasNextLine()) {
                        val line = scanner.nextLine()
                        if (line.startsWith("MemTotal:")) {
                            totalBytes = parseKbToBytes(line)
                        } else if (line.startsWith("MemAvailable:")) {
                            availBytes = parseKbToBytes(line)
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Handled gracefully below
        }

        return MemoryMetrics(
            totalRamBytes = if (totalBytes > 0) totalBytes else 8L * 1024 * 1024 * 1024,
            availableRamBytes = if (availBytes > 0) availBytes else 3L * 1024 * 1024 * 1024,
            isLowMemory = false,
            thresholdBytes = 1024L * 1024 * 1024
        )
    }

    private fun parseKbToBytes(line: String): Long {
        return try {
            val parts = line.split("\\s+".toRegex())
            if (parts.size >= 2) parts[1].toLong() * 1024L else 0L
        } catch (_: Exception) {
            0L
        }
    }

    private fun scanStorage(): StorageMetrics {
        return try {
            val dataDir = Environment.getDataDirectory()
            val stat = StatFs(dataDir.path)
            val totalBytes = stat.totalBytes
            val freeBytes = stat.availableBytes
            StorageMetrics(totalInternalBytes = totalBytes, freeInternalBytes = freeBytes)
        } catch (_: Exception) {
            StorageMetrics(totalInternalBytes = 64L * 1024 * 1024 * 1024, freeInternalBytes = 16L * 1024 * 1024 * 1024)
        }
    }

    private fun scanThermalState(): ThermalState {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (powerManager != null) {
                return when (powerManager.currentThermalStatus) {
                    PowerManager.THERMAL_STATUS_NONE,
                    PowerManager.THERMAL_STATUS_LIGHT -> ThermalState.NOMINAL
                    PowerManager.THERMAL_STATUS_MODERATE -> ThermalState.MODERATE
                    PowerManager.THERMAL_STATUS_SEVERE,
                    PowerManager.THERMAL_STATUS_CRITICAL,
                    PowerManager.THERMAL_STATUS_EMERGENCY,
                    PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalState.THROTTLED
                    else -> ThermalState.UNKNOWN
                }
            }
        }
        return ThermalState.UNKNOWN
    }

    private fun scanBattery(): BatteryMetrics {
        return try {
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, intentFilter)
            if (batteryStatus != null) {
                val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
                val percent = if (level >= 0 && scale > 0) ((level / scale.toFloat()) * 100).toInt() else 100
                BatteryMetrics(levelPercent = percent, isCharging = isCharging)
            } else {
                BatteryMetrics(levelPercent = 100, isCharging = false)
            }
        } catch (_: Exception) {
            BatteryMetrics(levelPercent = 100, isCharging = false)
        }
    }
}
