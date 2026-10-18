package com.prayagi.netraassistant

import com.prayagi.netraassistant.assistant.Persona
import com.prayagi.netraassistant.assistant.VoiceCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceCommandTest {
    @Test fun netravanPrefix() {
        val p = VoiceCommand.parse("Netravan, battery kitni hai")
        assertEquals(Persona.NETRAVAN, p.persona); assertEquals("battery kitni hai", p.text)
    }
    @Test fun heyNetra() {
        val p = VoiceCommand.parse("hey Netra volume up")
        assertEquals(Persona.NETRAVAN, p.persona); assertEquals("volume up", p.text)
    }
    @Test fun devanagariName() {
        val p = VoiceCommand.parse("नेत्रवान बैटरी")
        assertEquals(Persona.NETRAVAN, p.persona); assertEquals("बैटरी", p.text)
    }
    @Test fun noName() {
        val p = VoiceCommand.parse("hello")
        assertNull(p.persona); assertEquals("hello", p.text)
    }
    @Test fun nameOnly() {
        val p = VoiceCommand.parse("Netravan")
        assertEquals(Persona.NETRAVAN, p.persona); assertEquals("", p.text)
    }
}
