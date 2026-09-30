package com.composebasics

import com.composebasics.data.repository.PasswordHasher
import com.composebasics.ui.Validators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ValidatorsTest {
    @Test fun name_blank() = assertNotNull(Validators.nameError(" "))
    @Test fun name_ok() = assertNull(Validators.nameError("Ann"))
    @Test fun email_valid() = assertNull(Validators.emailError("a.b@example.com"))
    @Test fun email_blank() = assertNotNull(Validators.emailError("  "))
    @Test fun email_invalid() = assertNotNull(Validators.emailError("not-an-email"))
    @Test fun password_short() = assertNotNull(Validators.passwordError("123"))
    @Test fun password_ok() = assertNull(Validators.passwordError("123456"))
    @Test fun confirm_mismatch() = assertNotNull(Validators.confirmPasswordError("abcdef", "abcdeg"))
    @Test fun confirm_match() = assertNull(Validators.confirmPasswordError("abcdef", "abcdef"))

    @Test
    fun hasher_isDeterministicPerSalt() {
        val salt = PasswordHasher.newSalt()
        assertEquals(PasswordHasher.hash("pw", salt), PasswordHasher.hash("pw", salt))
        assertNotEquals(PasswordHasher.hash("pw", salt), PasswordHasher.hash("pw", PasswordHasher.newSalt()))
    }
}
