package com.prayagi.netraassistant

import com.prayagi.netraassistant.commands.CommandParser
import com.prayagi.netraassistant.commands.PhoneCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandParserTest {
    @Test fun callWithNumber() {
        val c = CommandParser.parse("call karo 9876543210")
        assertTrue(c is PhoneCommand.Call)
        assertEquals("9876543210", (c as PhoneCommand.Call).number)
    }
    @Test fun callWithoutNumberIsNotACommand() {
        assertNull(CommandParser.parse("mummy ko call karo"))
    }
    @Test fun alarmBaje() {
        val c = CommandParser.parse("alarm lagao 6 baje")
        assertTrue(c is PhoneCommand.SetAlarm)
        val a = c as PhoneCommand.SetAlarm
        assertTrue(a.hour == 6 || a.hour == 18)
        assertEquals(0, a.minutes)
    }
    @Test fun alarmShaamPM() {
        val c = CommandParser.parse("alarm lagao shaam ke 7 baje")
        assertEquals(19, (c as PhoneCommand.SetAlarm).hour)
    }
    @Test fun alarmColon() {
        val c = CommandParser.parse("अलार्म 6:30 subah")
        assertTrue(c is PhoneCommand.SetAlarm)
        assertEquals(6, (c as PhoneCommand.SetAlarm).hour)
        assertEquals(30, c.minutes)
    }
    @Test fun openWhatsapp() {
        val c = CommandParser.parse("whatsapp kholo")
        assertTrue(c is PhoneCommand.OpenApp)
        assertEquals("com.whatsapp", (c as PhoneCommand.OpenApp).packageId)
    }
    @Test fun openFamilyApp() {
        val c = CommandParser.parse("eco app open karo")
        assertTrue(c is PhoneCommand.OpenApp)
        assertEquals("com.prayagi.netraeco", (c as PhoneCommand.OpenApp).packageId)
    }
    @Test fun search() {
        val c = CommandParser.parse("search karo prayagraj weather")
        assertTrue(c is PhoneCommand.WebSearch)
        assertEquals("prayagraj weather", (c as PhoneCommand.WebSearch).query)
    }
    @Test fun sms() {
        val c = CommandParser.parse("sms likho 9876543210 ki main late aaunga")
        assertTrue(c is PhoneCommand.Sms)
        val s = c as PhoneCommand.Sms
        assertEquals("9876543210", s.number)
        assertTrue(s.body.contains("late aaunga"))
    }
    @Test fun plainQuestionIsNotACommand() {
        assertNull(CommandParser.parse("battery kitni hai"))
        assertNull(CommandParser.parse("what is the time"))
    }
}
