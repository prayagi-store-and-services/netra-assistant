package com.prayagi.netraassistant

import com.prayagi.netraassistant.actions.BatteryTimeMath
import org.junit.Assert.*
import org.junit.Test

class BatteryTimeMathTest {
    @Test fun twentyMilliampsStaysTwentyMilliamps() {
        val v = BatteryTimeMath.estimate(3_000_000, -20_000)!!
        assertEquals(150.0, v.hours, 0.000001)
        assertEquals(20, v.drawMa)
    }
    @Test fun noUnitSwitchAtFormerBoundary() {
        val a = BatteryTimeMath.estimate(3_000_000, -20_000)!!
        val b = BatteryTimeMath.estimate(3_000_000, -20_001)!!
        assertEquals(a.hours, b.hours, 0.01)
        assertEquals(20, b.drawMa)
    }
    @Test fun largerDischargeUsesSameUnits() {
        val v = BatteryTimeMath.estimate(3_000_000, -500_000)!!
        assertEquals(6.0, v.hours, 0.000001)
        assertEquals(500, v.drawMa)
    }
    @Test fun chargingOrMissingCurrentHasNoDischargeEstimate() {
        for (i in listOf(0L, 20_000L, Long.MIN_VALUE))
            assertNull(BatteryTimeMath.estimate(3_000_000, i))
    }
    @Test fun missingChargeAndTinyReadingsAreRejected() {
        for (c in listOf(0L, -1L, Long.MIN_VALUE, 999L))
            assertNull(BatteryTimeMath.estimate(c, -20_000))
        assertNull(BatteryTimeMath.estimate(3_000_000, -999))
        assertEquals(3000.0, BatteryTimeMath.estimate(3_000_000, -1000)!!.hours, 0.000001)
    }
}
