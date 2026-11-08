package ir.zagroserp.pda.domain.alerts

import ir.zagroserp.pda.domain.model.PendingQueueSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemAlertsTest {

    @Test
    fun `an empty queue produces no alerts`() {
        assertTrue(SystemAlerts.derive(PendingQueueSummary()).isEmpty())
    }

    @Test
    fun `a few queued items while offline are not an alert`() {
        val summary = PendingQueueSummary(orders = 2, visits = 3, locations = 40)
        assertTrue(SystemAlerts.derive(summary).isEmpty())
    }

    @Test
    fun `failed orders always raise an alert`() {
        val alerts = SystemAlerts.derive(PendingQueueSummary(orders = 1, failedOrders = 1))
        assertEquals(1, alerts.size)
        assertEquals(SystemAlerts.ID_FAILED_ORDERS, alerts.first().id)
        assertEquals(SystemAlerts.TYPE_QUEUE_FAILED, alerts.first().type)
    }

    @Test
    fun `queue alerts appear once the thresholds are reached`() {
        val alerts = SystemAlerts.derive(
            PendingQueueSummary(
                orders = SystemAlerts.PENDING_ORDERS_THRESHOLD,
                visits = SystemAlerts.PENDING_VISITS_THRESHOLD,
                locations = SystemAlerts.PENDING_LOCATIONS_THRESHOLD,
            ),
        )
        assertEquals(3, alerts.size)
        assertTrue(alerts.all { it.type == SystemAlerts.TYPE_QUEUE_PENDING })
    }

    @Test
    fun `one item below each threshold stays silent`() {
        val alerts = SystemAlerts.derive(
            PendingQueueSummary(
                orders = SystemAlerts.PENDING_ORDERS_THRESHOLD - 1,
                visits = SystemAlerts.PENDING_VISITS_THRESHOLD - 1,
                locations = SystemAlerts.PENDING_LOCATIONS_THRESHOLD - 1,
            ),
        )
        assertTrue(alerts.isEmpty())
    }

    @Test
    fun `alert ids are stable and namespaced`() {
        val first = SystemAlerts.derive(PendingQueueSummary(failedOrders = 3))
        val second = SystemAlerts.derive(PendingQueueSummary(failedOrders = 9))
        assertEquals(first.first().id, second.first().id)
        assertTrue(SystemAlerts.isSystemAlert(first.first().id))
    }

    @Test
    fun `server notifications are not mistaken for system alerts`() {
        assertFalse(SystemAlerts.isSystemAlert("8f3c-uuid-from-server"))
    }

    @Test
    fun `alert bodies render counts with persian digits`() {
        val alert = SystemAlerts.derive(PendingQueueSummary(failedOrders = 3)).first()
        assertTrue(alert.body.contains("۳"))
        assertFalse(alert.body.contains("3"))
    }
}
