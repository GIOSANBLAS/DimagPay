package com.paycontrol.app.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    @Test
    fun parseAndFormatRoundTrip() {
        val cents = Money.parseToCents("1234.56")
        assertEquals(123456L, cents)
        val formatted = Money.format(cents!!)
        assert(formatted.contains("1,234.56") || formatted.contains("1234.56"))
    }

    @Test
    fun rejectInvalid() {
        assertNull(Money.parseToCents("abc"))
        assertNull(Money.parseToCents(""))
    }

    @Test
    fun addSubtract() {
        assertEquals(150L, Money.add(100L, 50L))
        assertEquals(50L, Money.subtract(100L, 50L))
    }
}
