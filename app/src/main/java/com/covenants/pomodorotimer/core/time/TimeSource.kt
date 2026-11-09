package com.covenants.pomodorotimer.core.time

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import java.io.File
import java.util.TimeZone

/**
 * All time reads go through this interface so the timer engine is deterministic in tests.
 *
 * [elapsedRealtimeMs] is monotonic while the device is booted. [bootMarker] is a boot identity
 * that stays stable when the user manually changes the wall clock and changes after a reboot.
 */
interface TimeSource {
    fun elapsedRealtimeMs(): Long
    fun wallClockMs(): Long
    fun zoneId(): String
    fun bootMarker(): String?
}

class SystemTimeSource(private val context: Context? = null) : TimeSource {
    override fun elapsedRealtimeMs(): Long = SystemClock.elapsedRealtime()
    override fun wallClockMs(): Long = System.currentTimeMillis()
    override fun zoneId(): String = TimeZone.getDefault().id

    override fun bootMarker(): String {
        // /proc boot_id is a random identifier generated once per boot.
        runCatching {
            val id = File("/proc/sys/kernel/random/boot_id").readText().trim()
            if (id.isNotEmpty()) return "boot:$id"
        }
        // BOOT_COUNT is readable on Android 7+ and increments once per boot.
        context?.let { ctx ->
            runCatching {
                val count = Settings.Global.getString(ctx.contentResolver, "boot_count")
                if (!count.isNullOrBlank()) return "count:$count"
            }
        }
        // Last resort: a coarse wall/elapsed offset. Correct after a reboot in most cases, but a
        // large manual clock change is indistinguishable from a reboot with this marker.
        val offsetBucket = (wallClockMs() - elapsedRealtimeMs()) / 10_000L
        return "offset:$offsetBucket"
    }
}

class FakeTimeSource(
    var elapsed: Long = 0L,
    var wall: Long = 1_700_000_000_000L,
    var zone: String = "UTC",
    var marker: String = "boot-1",
) : TimeSource {
    override fun elapsedRealtimeMs(): Long = elapsed
    override fun wallClockMs(): Long = wall
    override fun zoneId(): String = zone
    override fun bootMarker(): String? = marker

    fun advance(ms: Long) {
        elapsed += ms
        wall += ms
    }

    fun changeWallClock(ms: Long) {
        wall += ms
    }

    fun reboot(newMarker: String = "boot-2") {
        elapsed = 0L
        marker = newMarker
    }
}
