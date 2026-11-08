package ir.zagroserp.pda.data.repository

import ir.zagroserp.pda.data.local.entity.TodayCustomerEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayAssignmentBuilderTest {

    private val date = "2026-09-22"

    @Test
    fun `a tour member keeps its tour`() {
        val rows = TodayAssignmentBuilder.build(
            date = date,
            tours = listOf("tour-1" to listOf("c1", "c2")),
            flatCustomerIds = listOf("c1", "c2"),
        )
        assertEquals(2, rows.size)
        assertTrue(rows.all { it.tourId == "tour-1" })
        assertTrue(rows.none { it.tourId == TodayCustomerEntity.DIRECT_TOUR_ID })
    }

    @Test
    fun `a customer outside every tour is marked direct`() {
        val rows = TodayAssignmentBuilder.build(
            date = date,
            tours = listOf("tour-1" to listOf("c1")),
            flatCustomerIds = listOf("c1", "c9"),
        )
        val direct = rows.single { it.customerId == "c9" }
        assertEquals(TodayCustomerEntity.DIRECT_TOUR_ID, direct.tourId)
        assertEquals("tour-1", rows.single { it.customerId == "c1" }.tourId)
    }

    @Test
    fun `a customer in two tours produces a single row`() {
        // The primary key is (date, customerId); a duplicate would lose a tour silently.
        val rows = TodayAssignmentBuilder.build(
            date = date,
            tours = listOf(
                "tour-1" to listOf("c1", "c2"),
                "tour-2" to listOf("c2", "c3"),
            ),
            flatCustomerIds = listOf("c1", "c2", "c3"),
        )
        assertEquals(3, rows.size)
        assertEquals(1, rows.count { it.customerId == "c2" })
        assertEquals("tour-1", rows.single { it.customerId == "c2" }.tourId)
    }

    @Test
    fun `the flat list never downgrades a tour member to direct`() {
        val rows = TodayAssignmentBuilder.build(
            date = date,
            tours = listOf("tour-1" to listOf("c1")),
            flatCustomerIds = listOf("c1"),
        )
        assertEquals("tour-1", rows.single().tourId)
    }

    @Test
    fun `sort order is sequential and stable`() {
        val rows = TodayAssignmentBuilder.build(
            date = date,
            tours = listOf("tour-1" to listOf("c1", "c2")),
            flatCustomerIds = listOf("c9", "c8"),
        )
        assertEquals(listOf(0, 1, 2, 3), rows.map { it.sortOrder })
        assertEquals(listOf("c1", "c2", "c9", "c8"), rows.map { it.customerId })
    }

    @Test
    fun `an empty day produces no rows so the screen shows its empty state`() {
        assertTrue(TodayAssignmentBuilder.build(date, emptyList(), emptyList()).isEmpty())
    }

    @Test
    fun `blank ids are ignored`() {
        val rows = TodayAssignmentBuilder.build(
            date = date,
            tours = listOf("tour-1" to listOf("", "c1")),
            flatCustomerIds = listOf("  ", "c1"),
        )
        assertEquals(1, rows.size)
        assertEquals("c1", rows.single().customerId)
    }

    @Test
    fun `every row carries the requested date`() {
        val rows = TodayAssignmentBuilder.build(date, listOf("t" to listOf("c1")), listOf("c2"))
        assertTrue(rows.all { it.date == date })
    }
}
