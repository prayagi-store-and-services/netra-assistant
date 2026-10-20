package com.prayagi.netraassistant

import com.prayagi.netraassistant.bridge.BridgeContract
import com.prayagi.netraassistant.commands.CommandParser
import com.prayagi.netraassistant.commands.PhoneCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BridgeTest {

    @Test
    fun contractConstantsStable() {
        assertEquals("com.prayagi.netra.BRIDGE_COMMAND", BridgeContract.ACTION)
        assertEquals(1, BridgeContract.CONTRACT_VERSION)
        assertEquals("com.prayagi.netraassistant", BridgeContract.SOURCE_PACKAGE)
    }

    @Test
    fun commandNamesValidated() {
        assertTrue(BridgeContract.isValidCommand("battery-status"))
        assertTrue(BridgeContract.isValidCommand("alert1"))
        assertFalse(BridgeContract.isValidCommand("Battery Status"))
        assertFalse(BridgeContract.isValidCommand(""))
        assertFalse(BridgeContract.isValidCommand("बैटरी"))
    }

    @Test
    fun requestIdsUniqueAndPrefixed() {
        val a = BridgeContract.newRequestId()
        val b = BridgeContract.newRequestId()
        assertNotEquals(a, b)
        assertTrue(a.startsWith("nb-"))
    }

    @Test
    fun parserBridgeEcoEnglish() {
        val c = CommandParser.parse("bridge eco battery-status")
        assertTrue(c is PhoneCommand.BridgeSend)
        c as PhoneCommand.BridgeSend
        assertEquals("com.prayagi.netraeco", c.targetPackage)
        assertEquals("battery-status", c.command)
        assertEquals("", c.args)
    }

    @Test
    fun parserBridgeHinglish() {
        val c = CommandParser.parse("battery ko bridge low-power mode on karo")
        assertTrue(c is PhoneCommand.BridgeSend)
        c as PhoneCommand.BridgeSend
        assertEquals("com.aistudio.batterysentinel.ntra", c.targetPackage)
        assertEquals("low-power", c.command)
        assertEquals("mode on", c.args)
    }

    @Test
    fun parserBridgeInvalidCommandFallsThrough() {
        assertNull(CommandParser.parse("bridge eco बैटरी"))
        assertNull(CommandParser.parse("bridge eco"))
    }

    @Test
    fun parserBridgeUnknownAppFallsThrough() {
        assertNull(CommandParser.parse("bridge telegram battery-status"))
    }
}
