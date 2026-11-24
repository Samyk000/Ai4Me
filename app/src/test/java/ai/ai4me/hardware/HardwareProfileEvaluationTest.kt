package ai.ai4me.hardware

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HardwareProfileEvaluationTest {

    private fun createSpec(
        totalRamGb: Float,
        availRamGb: Float,
        is64Bit: Boolean = true,
        cpuCores: Int = 8,
        freeStorageGb: Float = 32f,
        isLowMemory: Boolean = false
    ): DeviceHardwareSpec {
        return DeviceHardwareSpec(
            androidApiLevel = 34,
            supportedAbis = if (is64Bit) listOf("arm64-v8a") else listOf("armeabi-v7a"),
            is64Bit = is64Bit,
            cpuCoreCount = cpuCores,
            memory = MemoryMetrics(
                totalRamBytes = (totalRamGb * 1024 * 1024 * 1024).toLong(),
                availableRamBytes = (availRamGb * 1024 * 1024 * 1024).toLong(),
                isLowMemory = isLowMemory,
                thresholdBytes = 1024L * 1024 * 1024
            ),
            storage = StorageMetrics(
                totalInternalBytes = 128L * 1024 * 1024 * 1024,
                freeInternalBytes = (freeStorageGb * 1024 * 1024 * 1024).toLong()
            ),
            thermalState = ThermalState.NOMINAL,
            battery = BatteryMetrics(levelPercent = 85, isCharging = false)
        )
    }

    @Test
    fun test12GbDevice_RecommendsPerformanceProfile() {
        val spec = createSpec(totalRamGb = 12.0f, availRamGb = 6.2f, is64Bit = true)
        val rec = HardwareProfileEvaluator.evaluate(spec)

        assertEquals(HardwareProfileType.PERFORMANCE, rec.recommendedProfile)
        assertTrue(rec.canRunLocalModels)
        assertTrue(rec.warnings.isEmpty())
    }

    @Test
    fun test8GbDevice_RecommendsBalancedProfile() {
        val spec = createSpec(totalRamGb = 8.0f, availRamGb = 3.5f, is64Bit = true)
        val rec = HardwareProfileEvaluator.evaluate(spec)

        assertEquals(HardwareProfileType.BALANCED, rec.recommendedProfile)
        assertTrue(rec.canRunLocalModels)
    }

    @Test
    fun testConstrainedMemory_RecommendsBatterySaver() {
        val spec = createSpec(totalRamGb = 4.0f, availRamGb = 1.2f, is64Bit = true)
        val rec = HardwareProfileEvaluator.evaluate(spec)

        assertEquals(HardwareProfileType.BATTERY_SAVER, rec.recommendedProfile)
        assertTrue(rec.canRunLocalModels)
        assertTrue(rec.warnings.any { it.contains("constrained") })
    }

    @Test
    fun test32BitDevice_RefusesModelExecution() {
        val spec = createSpec(totalRamGb = 8.0f, availRamGb = 4.0f, is64Bit = false)
        val rec = HardwareProfileEvaluator.evaluate(spec)

        assertFalse(rec.canRunLocalModels)
        assertTrue(rec.warnings.any { it.contains("32-bit") })
    }

    @Test
    fun testLowStorage_AddsWarning() {
        val spec = createSpec(totalRamGb = 12.0f, availRamGb = 6.0f, freeStorageGb = 2.5f)
        val rec = HardwareProfileEvaluator.evaluate(spec)

        assertTrue(rec.warnings.any { it.contains("Low storage") })
    }
}
