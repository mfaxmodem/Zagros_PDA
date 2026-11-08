package ir.zagroserp.pda.domain.cart

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderMathTest {

    @Test
    fun `packaging converts to base units using the uom factor`() {
        // 2 cartons of 8 pieces each.
        assertEquals(16.0, OrderMath.baseUnits(packCount = 2, conversionFactor = 8.0, looseCount = 0), 0.001)
    }

    @Test
    fun `packaging and loose pieces are summed`() {
        // 2 cartons of 8 + 3 loose = 19 pieces.
        assertEquals(19.0, OrderMath.baseUnits(packCount = 2, conversionFactor = 8.0, looseCount = 3), 0.001)
    }

    @Test
    fun `loose only orders equal the entered count`() {
        assertEquals(5.0, OrderMath.baseUnits(packCount = 0, conversionFactor = 1.0, looseCount = 5), 0.001)
    }

    @Test
    fun `a non positive conversion factor is treated as one`() {
        assertEquals(4.0, OrderMath.baseUnits(packCount = 4, conversionFactor = 0.0, looseCount = 0), 0.001)
    }

    @Test
    fun `negative inputs are clamped instead of producing negative totals`() {
        assertEquals(0.0, OrderMath.baseUnits(packCount = -3, conversionFactor = 8.0, looseCount = -2), 0.001)
        assertEquals(0.0, OrderMath.lineTotal(-1, 1000.0, -5, 500.0), 0.001)
    }

    @Test
    fun `line total charges packaging and loose at their own prices`() {
        // 2 cartons @ 800,000 + 3 loose @ 100,000 = 1,900,000
        val total = OrderMath.lineTotal(
            packCount = 2,
            packUnitPrice = 800_000.0,
            looseCount = 3,
            looseUnitPrice = 100_000.0,
        )
        assertEquals(1_900_000.0, total, 0.001)
    }

    @Test
    fun `stock check uses base units not packaging counts`() {
        // 2 cartons of 8 = 16 pieces against 15 available -> short.
        assertTrue(
            OrderMath.exceedsStock(
                packCount = 2,
                conversionFactor = 8.0,
                looseCount = 0,
                availableBaseUnits = 15.0,
            ),
        )
        assertFalse(
            OrderMath.exceedsStock(
                packCount = 1,
                conversionFactor = 8.0,
                looseCount = 7,
                availableBaseUnits = 15.0,
            ),
        )
    }

    @Test
    fun `floating point noise from conversion factors does not flag a false shortage`() {
        assertFalse(
            OrderMath.exceedsStock(
                packCount = 3,
                conversionFactor = 0.1,
                looseCount = 0,
                availableBaseUnits = 0.3,
            ),
        )
    }
}
