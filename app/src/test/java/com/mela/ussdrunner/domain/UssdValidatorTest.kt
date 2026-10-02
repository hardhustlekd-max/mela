package com.mela.ussdrunner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UssdValidatorTest {
    @Test
    fun acceptsTypicalCarrierCodes() {
        assertTrue(UssdValidator.isValid("*999*1*2#"))
        assertTrue(UssdValidator.isValid("*804#"))
        assertTrue(UssdValidator.isValid("#123#"))
        assertTrue(UssdValidator.isValid("*127#"))
        assertTrue(UssdValidator.isValid("  *999*1*2#  "))
        assertTrue(UssdValidator.isValid("*#06#"))
        assertTrue(UssdValidator.isValid("*100#"))
    }

    @Test
    fun rejectsEmptyAndJunk() {
        assertFalse(UssdValidator.isValid(""))
        assertFalse(UssdValidator.isValid("   "))
        assertFalse(UssdValidator.isValid("abc"))
        assertFalse(UssdValidator.isValid("12345"))
        assertFalse(UssdValidator.isValid("*99A#"))
        assertFalse(UssdValidator.isValid("*804#+"))
        assertFalse(UssdValidator.isValid("*"))
        assertFalse(UssdValidator.isValid("#"))
    }

    @Test
    fun normalizeStripsSpaces() {
        assertEquals("*999*1*2#", UssdValidator.normalize(" *999*1*2# "))
    }

    @Test
    fun validResultReturnsNormalizedCode() {
        val result = UssdValidator.validate(" *804# ")
        assertTrue(result is UssdValidator.ValidationResult.Valid)
        assertEquals("*804#", (result as UssdValidator.ValidationResult.Valid).normalized)
    }

    @Test
    fun emptyCodeHasClearMessage() {
        val result = UssdValidator.validate("  ")
        assertTrue(result is UssdValidator.ValidationResult.Invalid)
        assertTrue((result as UssdValidator.ValidationResult.Invalid).message.contains("Enter a USSD"))
    }
}
