package com.paycontrol.app.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BackupPasswordPolicyTest {

    @Test
    fun rejectsShortPassword() {
        assertNotNull(BackupPasswordPolicy.validate("Ab1!"))
    }

    @Test
    fun acceptsStrongPassword() {
        assertNull(BackupPasswordPolicy.validate("Segura1!"))
    }

    @Test
    fun strengthIncreasesWithComplexity() {
        assertEquals(
            BackupPasswordPolicy.Strength.WEAK,
            BackupPasswordPolicy.strength("abc")
        )
        assertEquals(
            BackupPasswordPolicy.Strength.STRONG,
            BackupPasswordPolicy.strength("Segura12!@#")
        )
    }
}
