package ir.zagroserp.pda.data.mapper

import ir.zagroserp.pda.data.remote.dto.CategoryOfferDto
import ir.zagroserp.pda.data.remote.dto.MobileCustomerDto
import ir.zagroserp.pda.data.remote.dto.ProductResponseDto
import ir.zagroserp.pda.data.remote.dto.UomDto
import ir.zagroserp.pda.domain.model.OrderStatus
import ir.zagroserp.pda.domain.model.VisitStatus
import ir.zagroserp.pda.domain.model.VisitType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MappersTest {

    @Test
    fun `product with category offers and uoms survives an entity round trip`() {
        val dto = ProductResponseDto(
            id = "p1",
            code = "P001",
            name = "کالا",
            barcode = "123456",
            category = "عمومی",
            unit = "عدد",
            sellPrice = 100_000.0,
            imageUrl = "/uploads/products/P001.jpg",
            availableQty = 95.0,
            minStock = 10.0,
            categories = listOf(
                CategoryOfferDto("c1", "باکس ۸ تایی", 800_000.0, isDefaultSale = true),
                CategoryOfferDto("c2", "شل", 100_000.0, isDefaultSale = false),
            ),
            uoms = listOf(
                UomDto("u1", "uom-1", "شل", 8.0, 800_000.0, "", isDefaultForSale = true),
            ),
        )

        val entity = dto.toEntity()
        val domain = entity.toDomain()

        assertEquals("p1", domain.id)
        assertEquals("/uploads/products/P001.jpg", domain.imageUrl)
        assertEquals(2, domain.offers.size)
        assertEquals("باکس ۸ تایی", domain.defaultOffer?.name)
        assertEquals(800_000.0, domain.defaultOffer?.salePrice ?: 0.0, 0.001)
        assertEquals(1, domain.uoms.size)
        assertEquals("شل", domain.defaultUom?.unitName)
        assertEquals(100_000.0, domain.defaultUom?.pricePerBaseUnit ?: 0.0, 0.001)
        assertTrue(domain.inStock)
    }

    @Test
    fun `mobile product keeps the previously cached image url`() {
        val existing = ProductResponseDto(
            id = "p1",
            name = "کالا",
            imageUrl = "/uploads/products/P001.jpg",
            sellPrice = 50_000.0,
            availableQty = 5.0,
        ).toEntity()

        val delta = ir.zagroserp.pda.data.remote.dto.MobileProductDto(
            id = "p1",
            code = "P001",
            name = "کالا",
            sellPrice = 60_000.0,
            hasPromotion = true,
            promoPercent = 10.0,
        )

        val merged = delta.toEntity(existing).toDomain()
        assertEquals("/uploads/products/P001.jpg", merged.imageUrl)
        assertEquals(60_000.0, merged.sellPrice, 0.001)
        assertEquals(5.0, merged.availableQty, 0.001)
        assertTrue(merged.hasPromotion)
    }

    @Test
    fun `customer delta does not wipe locally known fields`() {
        val existing = MobileCustomerDto(
            id = "c1",
            name = "فروشگاه",
            phone = "09120000000",
            nationalId = "1234567890",
            creditLimit = 5_000_000.0,
            lat = 30.28,
            lng = 51.28,
        ).toEntity()

        val delta = MobileCustomerDto(id = "c1", name = "فروشگاه")
        val merged = delta.toEntity(existing).toDomain()

        assertEquals("09120000000", merged.phone)
        assertEquals("1234567890", merged.nationalId)
        assertEquals(5_000_000.0, merged.creditLimit, 0.001)
        assertEquals(30.28, merged.lat, 0.001)
        assertTrue(merged.hasLocation)
    }

    @Test
    fun `unknown enum values fall back to safe defaults`() {
        assertEquals(OrderStatus.PENDING, OrderStatus.from("PENDING"))
        assertEquals(OrderStatus.UNKNOWN, OrderStatus.from("SOMETHING_NEW"))
        assertEquals(VisitType.ROUTE, VisitType.from("ROUTE"))
        assertEquals(VisitType.ROUTE, VisitType.from("weird"))
        assertEquals(VisitStatus.VISITED, VisitStatus.from("VISITED"))
        assertEquals(VisitStatus.VISITED, VisitStatus.from("weird"))
    }

    @Test
    fun `order status labels cover every documented state`() {
        val labels = listOf("PENDING", "APPROVED", "REJECTED", "CANCELLED", "BLOCKED")
            .map { OrderStatus.from(it).label }
        assertTrue(labels.none { it == OrderStatus.UNKNOWN.label })
        assertFalse(OrderStatus.from("REJECTED").label.isBlank())
    }

    @Test
    fun `json codecs never throw on corrupt input`() {
        assertEquals(emptyList<CategoryOfferDto>(), Mappers.decodeOffers("not-json"))
        assertEquals(emptyList<UomDto>(), Mappers.decodeUoms("{"))
        assertEquals("[]", Mappers.encodeOffers(emptyList()))
    }
}
