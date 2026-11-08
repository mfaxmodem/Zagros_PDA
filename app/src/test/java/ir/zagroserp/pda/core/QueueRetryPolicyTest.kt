package ir.zagroserp.pda.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueRetryPolicyTest {

    @Test
    fun `content rejections are permanent`() {
        assertTrue(QueueRetryPolicy.isPermanent(AppError.Validation("too far")))
        assertTrue(QueueRetryPolicy.isPermanent(AppError.Conflict("duplicate")))
        assertTrue(QueueRetryPolicy.isPermanent(AppError.Forbidden("disabled")))
        assertTrue(QueueRetryPolicy.isPermanent(AppError.NotFound("gone")))
    }

    @Test
    fun `transport failures are retryable so a valid visit is never discarded`() {
        assertFalse(QueueRetryPolicy.isPermanent(AppError.Network()))
        assertFalse(QueueRetryPolicy.isPermanent(AppError.Timeout()))
        assertFalse(QueueRetryPolicy.isPermanent(AppError.Server()))
        assertFalse(QueueRetryPolicy.isPermanent(AppError.RateLimited("slow down")))
        assertFalse(QueueRetryPolicy.isPermanent(AppError.Unauthorized()))
        assertFalse(QueueRetryPolicy.isPermanent(AppError.Unknown("?")))
    }

    @Test
    fun `an entry is dropped only after the rejection budget is spent`() {
        assertFalse(QueueRetryPolicy.shouldDrop(0))
        assertFalse(QueueRetryPolicy.shouldDrop(1))
        assertFalse(QueueRetryPolicy.shouldDrop(QueueRetryPolicy.MAX_REJECTIONS - 1))
        assertTrue(QueueRetryPolicy.shouldDrop(QueueRetryPolicy.MAX_REJECTIONS))
        assertTrue(QueueRetryPolicy.shouldDrop(QueueRetryPolicy.MAX_REJECTIONS + 5))
    }

    @Test
    fun `a blank server message still produces an explanation`() {
        assertTrue(QueueRetryPolicy.dropReason("").isNotBlank())
        assertTrue(QueueRetryPolicy.dropReason("   ").isNotBlank())
    }

    @Test
    fun `the server message is preserved when present`() {
        val message = "فاصله‌ی شما تا مشتری 340 متر است"
        assertTrue(QueueRetryPolicy.dropReason(message) == message)
    }
}
