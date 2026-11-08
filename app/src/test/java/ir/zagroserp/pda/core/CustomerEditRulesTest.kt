package ir.zagroserp.pda.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomerEditRulesTest {

    @Test
    fun `a blank or one character name is rejected`() {
        assertNotNull(CustomerEditRules.validateName(""))
        assertNotNull(CustomerEditRules.validateName("   "))
        assertNotNull(CustomerEditRules.validateName("م"))
    }

    @Test
    fun `two characters is the minimum accepted name`() {
        assertNull(CustomerEditRules.validateName("آب"))
        assertNull(CustomerEditRules.validateName("علی"))
    }

    @Test
    fun `a name longer than the contract limit is rejected`() {
        assertNull(CustomerEditRules.validateName("ا".repeat(CustomerEditRules.MAX_NAME_LENGTH)))
        assertNotNull(CustomerEditRules.validateName("ا".repeat(CustomerEditRules.MAX_NAME_LENGTH + 1)))
    }

    @Test
    fun `surrounding whitespace does not change the length verdict`() {
        assertNull(CustomerEditRules.validateName("  علی  "))
        assertNotNull(CustomerEditRules.validateName("   م   "))
    }

    @Test
    fun `phone is optional`() {
        assertNull(CustomerEditRules.validatePhone(""))
        assertNull(CustomerEditRules.validatePhone("   "))
    }

    @Test
    fun `phone rejects letters and overlong input`() {
        assertNull(CustomerEditRules.validatePhone("09120000000"))
        assertNull(CustomerEditRules.validatePhone("+98-912-000-0000"))
        assertNotNull(CustomerEditRules.validatePhone("0912abc"))
        assertNotNull(CustomerEditRules.validatePhone("1".repeat(CustomerEditRules.MAX_PHONE_LENGTH + 1)))
    }

    @Test
    fun `address length is bounded`() {
        assertNull(CustomerEditRules.validateAddress(""))
        assertNotNull(
            CustomerEditRules.validateAddress("x".repeat(CustomerEditRules.MAX_ADDRESS_LENGTH + 1)),
        )
    }

    @Test
    fun `zero zero means no coordinates, not a position in the gulf of guinea`() {
        assertFalse(CustomerEditRules.hasCoordinates(0.0, 0.0))
        assertFalse(CustomerEditRules.hasCoordinates(null, null))
        assertFalse(CustomerEditRules.hasCoordinates(32.6, null))
        assertFalse(CustomerEditRules.hasCoordinates(Double.NaN, 51.6))
    }

    @Test
    fun `a real iranian coordinate counts as located`() {
        assertTrue(CustomerEditRules.hasCoordinates(32.6546, 51.6680))
        // Only one axis non-zero is still a recorded position.
        assertTrue(CustomerEditRules.hasCoordinates(0.0, 51.6680))
    }

    @Test
    fun `coordinate label is empty when there is nothing to show`() {
        assertEquals("", CustomerEditRules.coordinateLabel(0.0, 0.0))
        assertEquals("", CustomerEditRules.coordinateLabel(null, null))
        assertTrue(CustomerEditRules.coordinateLabel(32.65, 51.66).contains("32.65"))
    }

    @Test
    fun `radius boundary is inclusive`() {
        assertTrue(CustomerEditRules.withinRadius(50.0))
        assertTrue(CustomerEditRules.withinRadius(0.0))
        assertFalse(CustomerEditRules.withinRadius(50.1))
    }

    @Test
    fun `a coarse fix is warned about but not blocked`() {
        assertNull(CustomerEditRules.accuracyWarning(8.0))
        assertNotNull(CustomerEditRules.accuracyWarning(250.0))
    }

    @Test
    fun `capture confirmation renders persian digits`() {
        val text = CustomerEditRules.captureConfirmation(32.6546, 51.6680, 12.0)
        assertTrue(text.contains("۳۲"))
        assertTrue(text.contains("۵۱"))
        assertFalse(text.contains("32"))
    }
}
