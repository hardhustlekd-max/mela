package com.mela.ussdrunner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class UssdEncoderTest {
    @Test
    fun hashBecomesPercent23SoItIsNotAUriFragment() {
        assertEquals("tel:*999*1*2%23", UssdEncoder.encodeForTel("*999*1*2#"))
        assertEquals("tel:*804%23", UssdEncoder.encodeForTel("*804#"))
        assertEquals("tel:%23123%23", UssdEncoder.encodeForTel("#123#"))
    }

    @Test
    fun starsStayIntact() {
        val encoded = UssdEncoder.encodeForTel("*127#")
        assertEquals("tel:*127%23", encoded)
        assertFalse(encoded.contains("%2A"))
    }

    @Test
    fun trimsBeforeEncoding() {
        assertEquals("tel:*804%23", UssdEncoder.encodeForTel("  *804#  "))
    }
}
