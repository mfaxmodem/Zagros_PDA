package ir.zagroserp.pda.domain.cart

import kotlin.math.max

/**
 * Pricing and unit-conversion math for an order line entered as **packaging + loose pieces**.
 *
 * Kept out of the Compose sheet so it can be unit tested: getting this wrong means charging the
 * wrong amount, which is the one thing a field sales app cannot do.
 */
object OrderMath {

    /**
     * Total quantity expressed in base units.
     * e.g. 2 cartons of 8 plus 3 loose pieces = 19 base units.
     */
    fun baseUnits(packCount: Int, conversionFactor: Double, looseCount: Int): Double {
        val packs = max(0, packCount)
        val loose = max(0, looseCount)
        val factor = if (conversionFactor > 0) conversionFactor else 1.0
        return packs * factor + loose
    }

    /** Price of the packaged part. Zero when no packaging is selected. */
    fun packTotal(packCount: Int, unitPrice: Double): Double =
        max(0, packCount) * max(0.0, unitPrice)

    /** Price of the loose part, charged at the base-unit price. */
    fun looseTotal(looseCount: Int, unitPrice: Double): Double =
        max(0, looseCount) * max(0.0, unitPrice)

    fun lineTotal(
        packCount: Int,
        packUnitPrice: Double,
        looseCount: Int,
        looseUnitPrice: Double,
    ): Double = packTotal(packCount, packUnitPrice) + looseTotal(looseCount, looseUnitPrice)

    /**
     * Whether the requested amount fits the available stock, expressed in base units.
     * A tiny epsilon absorbs floating point noise from conversion factors.
     */
    fun exceedsStock(
        packCount: Int,
        conversionFactor: Double,
        looseCount: Int,
        availableBaseUnits: Double,
    ): Boolean = baseUnits(packCount, conversionFactor, looseCount) > availableBaseUnits + 0.0001
}
