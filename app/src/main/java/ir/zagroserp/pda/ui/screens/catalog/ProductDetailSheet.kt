package ir.zagroserp.pda.ui.screens.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ir.zagroserp.pda.domain.cart.OrderMath
import ir.zagroserp.pda.domain.model.CategoryOffer
import ir.zagroserp.pda.domain.model.Product
import ir.zagroserp.pda.domain.model.ProductUom
import ir.zagroserp.pda.ui.components.LinearLoading
import ir.zagroserp.pda.ui.components.PillTone
import ir.zagroserp.pda.ui.components.QuantityStepper
import ir.zagroserp.pda.ui.components.StatusPill
import ir.zagroserp.pda.ui.components.ThinDivider
import ir.zagroserp.pda.ui.theme.LocalZagrosAccents
import ir.zagroserp.pda.ui.theme.ZagrosSpacing
import ir.zagroserp.pda.ui.util.formatMoney
import ir.zagroserp.pda.ui.util.formatQty

/**
 * Order configuration sheet.
 *
 * The quantity is entered as **packaging + loose pieces**, matching how the field actually orders:
 * e.g. 2 cartons plus 3 loose pieces. Each part becomes its own order line so the server can
 * convert the packaged line to base units via `uom_id` while the loose line stays on the base unit.
 */
@Composable
fun ProductDetailSheet(
    product: Product,
    offers: List<CategoryOffer>,
    uoms: List<ProductUom>,
    isLoading: Boolean,
    imageUrl: String?,
    onDismiss: () -> Unit,
    onViewImage: () -> Unit,
    onAdd: (
        packCount: Int,
        uom: ProductUom?,
        looseCount: Int,
        looseUnitPrice: Double,
        offer: CategoryOffer?,
    ) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accents = LocalZagrosAccents.current

    // Category supplies only carry a price (no conversion factor), so they are offered as a price
    // tier only for products that do not define multi-UOM packaging. Mixing both would produce
    // ambiguous pricing.
    val showOffers = uoms.isEmpty() && offers.isNotEmpty()

    var selectedUom by remember(product.id) { mutableStateOf(product.defaultUom) }
    var selectedOffer by remember(product.id) {
        mutableStateOf(if (showOffers) product.defaultOffer else null)
    }
    var packCount by remember(product.id) { mutableIntStateOf(if (product.defaultUom != null) 1 else 0) }
    var looseCount by remember(product.id) { mutableIntStateOf(if (product.defaultUom != null) 0 else 1) }

    val basePrice = selectedOffer?.salePrice ?: product.sellPrice
    val looseUnitPrice = basePrice
    val conversion = selectedUom?.conversionFactor ?: 1.0
    val packUnitPrice = selectedUom?.unitPrice ?: 0.0

    val packTotal = OrderMath.packTotal(packCount, packUnitPrice)
    val looseTotal = OrderMath.looseTotal(looseCount, looseUnitPrice)
    val total = OrderMath.lineTotal(packCount, packUnitPrice, looseCount, looseUnitPrice)
    val totalInBaseUnits = OrderMath.baseUnits(packCount, conversion, looseCount)
    val exceedsStock = OrderMath.exceedsStock(
        packCount = packCount,
        conversionFactor = conversion,
        looseCount = looseCount,
        availableBaseUnits = product.availableQty,
    )
    val canAdd = totalInBaseUnits > 0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
            contentPadding = PaddingValues(ZagrosSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(ZagrosSpacing.md),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProductThumb(
                        imageUrl = imageUrl,
                        title = product.name,
                        onClick = onViewImage,
                    )
                    Spacer(Modifier.width(ZagrosSpacing.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "کد ${product.code} • ${product.category.ifBlank { product.unit }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = accents.textSecondary,
                        )
                        Spacer(Modifier.height(ZagrosSpacing.xs))
                        StatusPill(
                            text = "موجودی: ${formatQty(product.availableQty)} ${product.unit}",
                            tone = if (product.inStock) PillTone.SUCCESS else PillTone.DANGER,
                            leadingDot = true,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "بستن",
                        )
                    }
                }
            }

            if (isLoading) item { LinearLoading() }

            if (product.hasPromotion) {
                item {
                    StatusPill(
                        text = "٪${formatQty(product.promoPercent)} تخفیف فعال",
                        tone = PillTone.DANGER,
                    )
                }
            }

            if (uoms.isNotEmpty()) {
                item {
                    Text(
                        text = "واحد شمارش",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(ZagrosSpacing.sm)) {
                        SelectableRow(
                            selected = selectedUom == null,
                            title = "تک‌فروشی (${product.unit})",
                            subtitle = formatMoney(basePrice),
                            badge = null,
                            onClick = {
                                selectedUom = null
                                selectedOffer = null
                                packCount = 0
                                if (looseCount == 0) looseCount = 1
                            },
                        )
                        uoms.forEach { uom ->
                            SelectableRow(
                                selected = selectedUom?.id == uom.id,
                                title = uom.unitName,
                                subtitle = "${formatMoney(uom.unitPrice)} • هر ${uom.unitName} = " +
                                    "${formatQty(uom.conversionFactor)} ${product.unit}",
                                badge = if (uom.isDefaultForSale) "پیش‌فرض" else null,
                                onClick = {
                                    selectedUom = uom
                                    selectedOffer = null
                                    if (packCount == 0) packCount = 1
                                },
                            )
                        }
                    }
                }
            }

            if (showOffers) {
                item {
                    Text(
                        text = "نوع عرضه",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(ZagrosSpacing.sm)) {
                        offers.forEach { offer ->
                            SelectableRow(
                                selected = selectedOffer?.id == offer.id,
                                title = offer.name,
                                subtitle = formatMoney(offer.salePrice),
                                badge = if (offer.isDefaultSale) "پیش‌فرض" else null,
                                onClick = { selectedOffer = offer },
                            )
                        }
                    }
                }
            }

            item { ThinDivider() }

            item {
                Text(
                    text = "تعداد",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }

            item {
                if (selectedUom != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ZagrosSpacing.lg),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        QuantityStepper(
                            value = packCount,
                            onValueChange = { packCount = it },
                            label = selectedUom!!.unitName,
                            modifier = Modifier.weight(1f),
                        )
                        QuantityStepper(
                            value = looseCount,
                            onValueChange = { looseCount = it },
                            label = "خرده (${product.unit})",
                            modifier = Modifier.weight(1f),
                            gradient = listOf(accents.success, accents.success),
                        )
                    }
                } else {
                    QuantityStepper(
                        value = looseCount,
                        onValueChange = { looseCount = it },
                        label = product.unit,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(ZagrosSpacing.xs)) {
                    if (selectedUom != null) {
                        SummaryRow(
                            label = "${formatQty(packCount.toDouble())} × ${selectedUom!!.unitName}",
                            value = formatMoney(packTotal),
                        )
                    }
                    if (looseCount > 0) {
                        SummaryRow(
                            label = "${formatQty(looseCount.toDouble())} × ${product.unit}",
                            value = formatMoney(looseTotal),
                        )
                    }
                    SummaryRow(
                        label = "معادل واحد پایه",
                        value = "${formatQty(totalInBaseUnits)} ${product.unit}",
                        muted = true,
                    )
                }
            }

            if (exceedsStock) {
                item {
                    StatusPill(
                        text = "موجودی کافی نیست؛ سفارش در حالت «در انتظار تأمین» ثبت می‌شود",
                        tone = PillTone.WARNING,
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "جمع این کالا",
                        style = MaterialTheme.typography.bodyMedium,
                        color = accents.textSecondary,
                    )
                    Text(
                        text = formatMoney(total),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            item {
                Text(
                    text = "مبلغ نهایی پس از تأیید سرور محاسبه می‌شود؛ مبالغ نمایش‌داده‌شده تخمینی است.",
                    style = MaterialTheme.typography.bodySmall,
                    color = accents.textSecondary,
                )
            }

            item {
                Button(
                    onClick = {
                        onAdd(packCount, selectedUom, looseCount, looseUnitPrice, selectedOffer)
                    },
                    enabled = canAdd,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .navigationBarsPadding(),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text("افزودن به سبد")
                }
            }

            item { Spacer(Modifier.height(ZagrosSpacing.md)) }
        }
    }
}

@Composable
private fun ProductThumb(imageUrl: String?, title: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            )
        } else {
            Text(text = "📦", style = MaterialTheme.typography.titleMedium)
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(2.dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "🔍",
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, muted: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = LocalZagrosAccents.current.textSecondary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = if (muted) {
                LocalZagrosAccents.current.textSecondary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
private fun SelectableRow(
    selected: Boolean,
    title: String,
    subtitle: String,
    badge: String?,
    onClick: () -> Unit,
) {
    val accents = LocalZagrosAccents.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface,
            )
            .clickable(onClick = onClick)
            .padding(ZagrosSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(if (selected) MaterialTheme.colorScheme.primary else accents.border),
        )
        Spacer(Modifier.width(ZagrosSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = accents.textSecondary,
            )
        }
        if (badge != null) {
            StatusPill(text = badge, tone = PillTone.SUCCESS)
        }
    }
}
