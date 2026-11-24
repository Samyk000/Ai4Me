package ai.ai4me.hardware

data class MemoryMetrics(
    val totalRamBytes: Long,
    val availableRamBytes: Long,
    val isLowMemory: Boolean,
    val thresholdBytes: Long
) {
    val totalRamGb: Float = totalRamBytes / (1024f * 1024f * 1024f)
    val availableRamGb: Float = availableRamBytes / (1024f * 1024f * 1024f)
}

data class StorageMetrics(
    val totalInternalBytes: Long,
    val freeInternalBytes: Long
) {
    val totalGb: Float = totalInternalBytes / (1024f * 1024f * 1024f)
    val freeGb: Float = freeInternalBytes / (1024f * 1024f * 1024f)
}

enum class ThermalState(val label: String) {
    NOMINAL("Nominal (Cool)"),
    MODERATE("Moderate"),
    THROTTLED("Throttling"),
    UNKNOWN("Unavailable")
}

data class BatteryMetrics(
    val levelPercent: Int,
    val isCharging: Boolean
)

data class DeviceHardwareSpec(
    val androidApiLevel: Int,
    val supportedAbis: List<String>,
    val is64Bit: Boolean,
    val cpuCoreCount: Int,
    val memory: MemoryMetrics,
    val storage: StorageMetrics,
    val thermalState: ThermalState,
    val battery: BatteryMetrics,
    val timestampMs: Long = System.currentTimeMillis()
)
