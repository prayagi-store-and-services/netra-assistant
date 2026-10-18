package com.prayagi.netraassistant

import com.prayagi.netraassistant.actions.BatteryInfo
import com.prayagi.netraassistant.actions.Result
import com.prayagi.netraassistant.assistant.Persona
import com.prayagi.netraassistant.assistant.Responder
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponderTest {
    @Test fun unavailableIsShownHonestly() {
        val t = Responder.batteryText(Persona.NETRA_ONE, Result.Unavailable("x"))
        assertTrue(t.contains("Unavailable"))
    }
    @Test fun batteryValueShown() {
        val t = Responder.batteryText(Persona.NETRA_ONE, Result.Value(BatteryInfo(55, true)))
        assertTrue(t.contains("55%") && t.contains("charge ho rahi"))
    }
}
