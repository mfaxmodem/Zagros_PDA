package ir.zagroserp.pda.location

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationUploadPolicyTest {

    @Test
    fun `a small, recent queue is held back`() {
        assertFalse(LocationUploadPolicy.shouldFlush(queueSize = 3, millisSinceLastFlush = 10_000))
    }

    @Test
    fun `the interval triggers a flush even for a single point`() {
        assertTrue(LocationUploadPolicy.shouldFlush(queueSize = 1, millisSinceLastFlush = 60_000))
    }

    @Test
    fun `a large backlog flushes immediately regardless of time`() {
        assertTrue(LocationUploadPolicy.shouldFlush(queueSize = 30, millisSinceLastFlush = 0))
        assertTrue(LocationUploadPolicy.shouldFlush(queueSize = 500, millisSinceLastFlush = 0))
    }

    @Test
    fun `the boundary is inclusive on both rules`() {
        assertTrue(LocationUploadPolicy.shouldFlush(29, 60_000))
        assertTrue(LocationUploadPolicy.shouldFlush(30, 59_999))
        assertFalse(LocationUploadPolicy.shouldFlush(29, 59_999))
    }

    @Test
    fun `an 8 hour shift drops from one request per fix to one per minute`() {
        // 8 hour shift, a fix every 15s, a batch window of 60s.
        val perFix = LocationUploadPolicy.estimatedRequests(8, 15, 1)
        val batched = LocationUploadPolicy.estimatedRequests(8, 15, 60)

        // 1920 individual posts vs 481 batched posts.
        assertTrue("perFix=$perFix", perFix == 1920)
        assertTrue("batched=$batched", batched == 481)

        // 1920 / 481 = 3.99x fewer radio wake-ups, each carrying ~4 points instead of 1.
        assertTrue("batched=$batched perFix=$perFix", perFix > batched * 3)
        assertTrue("ratio", perFix.toDouble() / batched >= 3.9)
    }

    @Test
    fun `the batch window is what bounds the request count, not the fix rate`() {
        // Halving the fix rate must not change how often we talk to the server.
        val fast = LocationUploadPolicy.estimatedRequests(8, 15, 60)
        val slow = LocationUploadPolicy.estimatedRequests(8, 30, 60)
        assertTrue(fast == slow)
    }
}
