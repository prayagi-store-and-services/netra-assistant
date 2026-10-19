package com.prayagi.netraassistant

import com.prayagi.netraassistant.commands.ConfirmWords
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfirmWordsTest {
    @Test fun yes() {
        assertTrue(ConfirmWords.isConfirm("haan")); assertTrue(ConfirmWords.isConfirm("Haan"))
        assertTrue(ConfirmWords.isConfirm("हाँ")); assertTrue(ConfirmWords.isConfirm("theek hai"))
    }
    @Test fun no() {
        assertTrue(ConfirmWords.isCancel("nahi")); assertTrue(ConfirmWords.isCancel("रुक जाओ"))
        assertTrue(ConfirmWords.isCancel("band karo"))
    }
    @Test fun neither() {
        assertFalse(ConfirmWords.isConfirm("maybe")); assertFalse(ConfirmWords.isCancel("haan"))
        assertFalse(ConfirmWords.isConfirm("")); assertFalse(ConfirmWords.isCancel(""))
    }
}
