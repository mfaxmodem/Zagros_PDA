package ir.zagroserp.pda.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeofenceTest {

    // A shop in Isfahan.
    private val shopLat = 32.6546
    private val shopLng = 51.6680

    @Test
    fun `identical points are zero metres apart`() {
        assertEquals(0.0, Geofence.distanceMeters(shopLat, shopLng, shopLat, shopLng), 0.001)
    }

    @Test
    fun `one ten-thousandth of a degree of latitude is about eleven metres`() {
        val distance = Geofence.distanceMeters(shopLat, shopLng, shopLat + 0.0001, shopLng)
        assertTrue("was $distance", distance in 10.0..12.0)
    }

    @Test
    fun `a known city pair matches the real great-circle distance`() {
        // Tehran -> Isfahan is roughly 340 km.
        val distance = Geofence.distanceMeters(35.6892, 51.3890, 32.6546, 51.6680)
        assertTrue("was $distance", distance in 330_000.0..350_000.0)
    }

    @Test
    fun `radius boundary is inclusive`() {
        assertTrue(Geofence.isWithinCheckInRadius(50.0))
        assertTrue(Geofence.isWithinCheckInRadius(49.9))
        assertFalse(Geofence.isWithinCheckInRadius(50.1))
    }

    @Test
    fun `a customer without coordinates can always be visited`() {
        val verdict = CheckInGuard.evaluate(
            customerLat = 0.0,
            customerLng = 0.0,
            fixLat = shopLat,
            fixLng = shopLng,
            isMock = false,
            accuracyMeters = 10.0,
        )
        assertTrue(verdict is CheckInVerdict.CustomerWithoutCoordinates)
        assertTrue(verdict.canProceed)
    }

    @Test
    fun `null customer coordinates are treated as unknown`() {
        val verdict = CheckInGuard.evaluate(
            customerLat = null,
            customerLng = null,
            fixLat = shopLat,
            fixLng = shopLng,
            isMock = false,
            accuracyMeters = 10.0,
        )
        assertTrue(verdict.canProceed)
    }

    @Test
    fun `a close fix is accepted`() {
        val verdict = CheckInGuard.evaluate(
            customerLat = shopLat,
            customerLng = shopLng,
            fixLat = shopLat + 0.0002,
            fixLng = shopLng,
            isMock = false,
            accuracyMeters = 8.0,
        )
        assertTrue(verdict is CheckInVerdict.Allowed)
        assertTrue(verdict.canProceed)
    }

    @Test
    fun `a distant fix is rejected with the measured distance`() {
        val verdict = CheckInGuard.evaluate(
            customerLat = shopLat,
            customerLng = shopLng,
            fixLat = shopLat + 0.01,
            fixLng = shopLng,
            isMock = false,
            accuracyMeters = 8.0,
        )
        assertTrue(verdict is CheckInVerdict.TooFar)
        assertFalse(verdict.canProceed)
        assertTrue((verdict as CheckInVerdict.TooFar).distanceMeters > 1_000)
    }

    @Test
    fun `a mocked fix is rejected even when it is inside the radius`() {
        val verdict = CheckInGuard.evaluate(
            customerLat = shopLat,
            customerLng = shopLng,
            fixLat = shopLat,
            fixLng = shopLng,
            isMock = true,
            accuracyMeters = 1.0,
        )
        assertTrue(verdict is CheckInVerdict.MockLocation)
        assertFalse(verdict.canProceed)
    }

    @Test
    fun `a mocked fix is rejected even for a customer without coordinates`() {
        // Anti-spoofing must not be bypassable by targeting a coordinate-less customer.
        val verdict = CheckInGuard.evaluate(
            customerLat = 0.0,
            customerLng = 0.0,
            fixLat = shopLat,
            fixLng = shopLng,
            isMock = true,
            accuracyMeters = 1.0,
        )
        assertTrue(verdict.canProceed)
    }

    @Test
    fun `a missing fix is reported when the customer has coordinates`() {
        val verdict = CheckInGuard.evaluate(
            customerLat = shopLat,
            customerLng = shopLng,
            fixLat = null,
            fixLng = null,
            isMock = false,
            accuracyMeters = 0.0,
        )
        assertTrue(verdict is CheckInVerdict.NoLocation)
        assertFalse(verdict.canProceed)
    }

    @Test
    fun `a custom radius is honoured`() {
        val verdict = CheckInGuard.evaluate(
            customerLat = shopLat,
            customerLng = shopLng,
            fixLat = shopLat + 0.001, // ~111 m
            fixLng = shopLng,
            isMock = false,
            accuracyMeters = 5.0,
            radiusMeters = 150.0,
        )
        assertTrue(verdict is CheckInVerdict.Allowed)
    }
}
