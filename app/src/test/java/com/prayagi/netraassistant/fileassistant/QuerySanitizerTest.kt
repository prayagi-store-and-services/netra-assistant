package com.prayagi.netraassistant.fileassistant

import org.junit.Assert.assertEquals
import org.junit.Test

class QuerySanitizerTest {
    @Test
    fun simpleTermsAreQuotedAndAnded() {
        assertEquals("\"kitna\" AND \"bill\"", QuerySanitizer.toMatchQuery("kitna bill"))
    }

    @Test
    fun quotesAreStripped() {
        assertEquals("\"bill\"", QuerySanitizer.toMatchQuery("\"bill\""))
    }

    @Test
    fun blankGivesEmpty() {
        assertEquals("", QuerySanitizer.toMatchQuery("   "))
    }

    @Test
    fun punctuationOnlyTermsDropped() {
        assertEquals("\"bill\"", QuerySanitizer.toMatchQuery("*** bill"))
    }
}
