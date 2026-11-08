package ir.zagroserp.pda.data.repository

import ir.zagroserp.pda.data.local.entity.TodayCustomerEntity

/**
 * Builds the day-scoped assignment rows from a `GET /mobile/visitor/today` response.
 *
 * The response contains `tours[]` (each with its customers) and a flat `customers[]` list that
 * also includes the tour members. A customer appearing in two tours, or in both a tour and the
 * flat list, must end up as a **single** row — the primary key is `(date, customerId)` and a
 * duplicate insert would silently drop one of the tours.
 *
 * Pure and free of Android types so the ordering and de-duplication rules can be unit tested.
 */
object TodayAssignmentBuilder {

    /**
     * @param tours tour id paired with the ids of the customers assigned to it, in priority order
     * @param flatCustomerIds the flat `customers[]` list from the response
     */
    fun build(
        date: String,
        tours: List<Pair<String, List<String>>>,
        flatCustomerIds: List<String>,
    ): List<TodayCustomerEntity> {
        val rows = mutableListOf<TodayCustomerEntity>()
        val assigned = mutableSetOf<String>()
        var order = 0

        tours.forEach { (tourId, customerIds) ->
            customerIds.forEach { customerId ->
                if (customerId.isBlank() || !assigned.add(customerId)) return@forEach
                rows += TodayCustomerEntity(
                    date = date,
                    customerId = customerId,
                    tourId = tourId,
                    sortOrder = order++,
                )
            }
        }

        flatCustomerIds.forEach { customerId ->
            if (customerId.isBlank() || !assigned.add(customerId)) return@forEach
            rows += TodayCustomerEntity(
                date = date,
                customerId = customerId,
                tourId = TodayCustomerEntity.DIRECT_TOUR_ID,
                sortOrder = order++,
            )
        }

        return rows
    }
}
