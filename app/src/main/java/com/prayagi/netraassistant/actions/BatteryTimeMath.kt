package com.prayagi.netraassistant.actions

/** Pure conversion of Android BatteryManager's microampere/microampere-hour readings. */
internal object BatteryTimeMath {
    data class Estimate(val hours: Double, val drawMa: Int)

    fun estimate(counterUah: Long, currentUa: Long): Estimate? {
        // Positive current means charging. MIN_VALUE is Android's unsupported sentinel.
        if (counterUah <= 0L || currentUa >= 0L || currentUa == Long.MIN_VALUE) return null
        val drawMa = -currentUa.toDouble() / 1000.0
        val chargeMah = counterUah / 1000.0
        // Keep the existing minimum trustworthy reading, expressed in the documented units.
        if (drawMa < 1.0 || chargeMah < 1.0) return null
        return Estimate(chargeMah / drawMa, drawMa.toInt())
    }
}
