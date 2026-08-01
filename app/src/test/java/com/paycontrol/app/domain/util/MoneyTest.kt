package com.paycontrol.app.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    @Test
    fun parseToCents_validDecimal() {
        assertEquals(125050L, Money.parseToCents("1250.50"))
        assertEquals(100L, Money.parseToCents("1"))
        assertEquals(0L, Money.parseToCents("0"))
    }

    @Test
    fun parseToCents_rejectsInvalid() {
        assertNull(Money.parseToCents(""))
        assertNull(Money.parseToCents("abc"))
        assertNull(Money.parseToCents("1.234"))
    }

    @Test
    fun addAndSubtract_exact() {
        assertEquals(300L, Money.add(100L, 200L))
        assertEquals(50L, Money.subtract(150L, 100L))
    }

    @Test
    fun format_containsCurrencySymbolOrDigits() {
        val formatted = Money.format(1500L)
        assert(formatted.contains("15") || formatted.contains("15.00") || formatted.contains("15,00"))
    }
}
