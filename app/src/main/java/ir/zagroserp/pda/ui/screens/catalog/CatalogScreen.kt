package ir.zagroserp.pda.ui.screens.catalog
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import ir.zagroserp.pda.domain.model.Product
import ir.zagroserp.pda.ui.components.CountBadge
import ir.zagroserp.pda.ui.components.EmptyState
import ir.zagroserp.pda.ui.components.ErrorState
import ir.zagroserp.pda.ui.components.LoadingBox
import ir.zagroserp.pda.ui.components.PillTone
import ir.zagroserp.pda.ui.components.ProductImageViewer
import ir.zagroserp.pda.ui.components.ScreenScaffold
import ir.zagroserp.pda.ui.components.StatusPill
import ir.zagroserp.pda.ui.components.ZagrosTextField
import ir.zagroserp.pda.ui.theme.LocalZagrosAccents
import ir.zagroserp.pda.ui.theme.ZagrosSpacing
import ir.zagroserp.pda.ui.util.LocalServerOrigin
import ir.zagroserp.pda.ui.util.absoluteImageUrl
import ir.zagroserp.pda.ui.util.formatMoney
import ir.zagroserp.pda.ui.util.formatQty

@Composable
fun CatalogScreen(
    onBack: () -> Unit,
    onOpenCart: () -> Unit,
    viewModel: CatalogViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val cartCount by viewModel.cartCount.collectAsStateWithLifecycle()
    val cartQuantities by viewModel.cartQuantities.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val cartCustomer by viewModel.cartCustomer.collectAsStateWithLifecycle()
    val products = viewModel.products.collectAsLazyPagingItems()
    var viewerProduct by remember { mutableStateOf<Product?>(null) }
    val accents = LocalZagrosAccents.current
    val serverOrigin = LocalServerOrigin.current

    ScreenScaffold(
        title = "کاتالوگ کالا",
        subtitle = cartCustomer?.name?.let { "سفارش برای: $it" }
            ?: "قیمت‌گذاری چندواحدی، عرضه‌های دسته‌بندی و تخفیف‌ها",
        onBack = onBack,
        actions = {
            IconButton(onClick = viewModel::refresh) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.Refresh,
                    contentDescription = "به‌روزرسانی",
                )
            }
        },
        floatingActionButton = {
            if (cartCount > 0) {
                ExtendedFloatingActionButton(
                    onClick = onOpenCart,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Filled.ShoppingCart, contentDescription = null) },
                    text = { Text("سبد ($cartCount)") },
                )
            }
        },
    ) {
        // Search controls and the result grid must be siblings in a Column, never siblings in a
        // Box – a Box would stack them on top of each other and hide the results.
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(horizontal = ZagrosSpacing.lg)) {
                ZagrosTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    label = "جست‌وجوی کالا",
                    placeholder = "نام، کد یا بارکد",
                    leadingIcon = androidx.compose.material.icons.Icons.Filled.Search,
                )
                Spacer(Modifier.height(ZagrosSpacing.sm))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(ZagrosSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusPill(
                        text = "فقط تخفیف‌دار",
                        tone = if (state.promotionsOnly) PillTone.PRIMARY else PillTone.NEUTRAL,
                        modifier = Modifier.clickable { viewModel.togglePromotions() },
                    )
                    if (state.isRefreshing) {
                        Text(
                            text = "در حال به‌روزرسانی…",
                            style = MaterialTheme.typography.labelSmall,
                            color = accents.textSecondary,
                        )
                    }
                }
                    if (categories.isNotEmpty()) {
                        Spacer(Modifier.height(ZagrosSpacing.sm))
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(ZagrosSpacing.sm),
                        ) {
                            item(key = "__all__") {
                                FilterChip(
                                    selected = state.selectedCategory.isEmpty(),
                                    onClick = { viewModel.selectCategory("") },
                                    label = { Text("همه دسته‌ها") },
                                )
                            }
                            items(categories.size, key = { index -> categories[index] }) { index ->
                                val category = categories[index]
                                FilterChip(
                                    selected = state.selectedCategory == category,
                                    onClick = { viewModel.selectCategory(category) },
                                    label = { Text(category) },
                                )
                            }
                        }
                    }

                    if (state.error != null) {
                        Spacer(Modifier.height(ZagrosSpacing.sm))
                        StatusPill(text = state.error!!, tone = PillTone.DANGER)
                    }
                if (state.info != null) {
                    Spacer(Modifier.height(ZagrosSpacing.sm))
                    StatusPill(text = state.info!!, tone = PillTone.SUCCESS)
                }
                Spacer(Modifier.height(ZagrosSpacing.sm))
            }

            // The result area is a sibling of the search panel and claims the remaining height,
            // so the lazy grid always gets a bounded height from the Column.
            when {
                products.loadState.refresh is androidx.paging.LoadState.Loading &&
                    products.itemCount == 0 -> LoadingBox(modifier = Modifier.weight(1f))

                products.loadState.refresh is androidx.paging.LoadState.Error -> ErrorState(
                    message = state.error
                        ?: "بارگذاری کاتالوگ ناموفق بود. اتصال به سرور را بررسی کنید.",
                    modifier = Modifier.weight(1f),
                    onRetry = viewModel::refresh,
                )

                products.itemCount == 0 -> EmptyState(
                    title = "کالایی یافت نشد",
                    subtitle = state.error
                        ?: "برای دریافت کاتالوگ، اتصال اینترنت را بررسی کنید.",
                    emoji = "📦",
                    modifier = Modifier.weight(1f),
                    actionLabel = "تلاش دوباره",
                    onAction = viewModel::refresh,
                )

                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        start = ZagrosSpacing.lg,
                        end = ZagrosSpacing.lg,
                        top = ZagrosSpacing.sm,
                        bottom = 96.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(ZagrosSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(ZagrosSpacing.md),
                ) {
                    items(products.itemCount, key = { index -> products.peek(index)?.id ?: index }) { index ->
                        products[index]?.let { product ->
                            ProductCard(
                                product = product,
                                inCart = cartQuantities[product.id] ?: 0.0,
                                serverOrigin = serverOrigin,
                                onClick = { viewModel.openProduct(product) },
                                onImageClick = { viewerProduct = product },
                                onQuickAdd = {
                                    val uom = product.defaultUom
                                    viewModel.addToCart(
                                        product = product,
                                        packCount = if (uom != null) 1 else 0,
                                        uom = uom,
                                        looseCount = if (uom == null) 1 else 0,
                                        looseUnitPrice = product.defaultOffer?.salePrice
                                            ?: product.sellPrice,
                                        offer = if (uom == null) product.defaultOffer else null,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    state.selectedProduct?.let { product ->
        ProductDetailSheet(
            product = product,
            offers = state.offers,
            uoms = state.uoms,
            isLoading = state.isLoadingDetail,
            imageUrl = absoluteImageUrl(serverOrigin, product.imageUrl),
            onDismiss = viewModel::closeProduct,
            onViewImage = { viewerProduct = product },
            onAdd = { packCount, uom, looseCount, looseUnitPrice, offer ->
                viewModel.addToCart(
                    product = product,
                    packCount = packCount,
                    uom = uom,
                    looseCount = looseCount,
                    looseUnitPrice = looseUnitPrice,
                    offer = offer,
                )
            },
        )
    }

    viewerProduct?.let { product ->
        ProductImageViewer(
            imageUrl = absoluteImageUrl(serverOrigin, product.imageUrl),
            title = product.name,
            onDismiss = { viewerProduct = null },
        )
    }
}

@Composable
private fun ProductCard(
    product: Product,
    inCart: Double,
    serverOrigin: String,
    onClick: () -> Unit,
    onImageClick: () -> Unit,
    onQuickAdd: () -> Unit,
) {
    val accents = LocalZagrosAccents.current
    androidx.compose.material3.Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, accents.border),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.35f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    // The photo has its own tap target: it opens the full-screen viewer instead
                    // of the order sheet.
                    .clickable(onClick = onImageClick),
            ) {
                val imageUrl = absoluteImageUrl(serverOrigin, product.imageUrl)
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "📦", style = MaterialTheme.typography.headlineSmall)
                    }
                }
                // Zoom affordance so the tap target is discoverable.
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(ZagrosSpacing.sm)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "مشاهده تصویر",
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                if (product.hasPromotion) {
                    Box(modifier = Modifier.padding(ZagrosSpacing.sm)) {
                        StatusPill(
                            text = "٪${formatQty(product.promoPercent)} تخفیف",
                            tone = PillTone.DANGER,
                        )
                    }
                }
                if (inCart > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(ZagrosSpacing.sm),
                    ) {
                        CountBadge(count = inCart.toInt(), tone = PillTone.PRIMARY)
                    }
                }
            }
            Column(modifier = Modifier.padding(ZagrosSpacing.md)) {
                // `minLines = 2` is what keeps every tile the same height: a one-line product
                // name would otherwise make its card shorter than a two-line neighbour in the
                // same grid row, since LazyVerticalGrid measures items independently.
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = product.category.ifBlank { product.unit },
                    style = MaterialTheme.typography.bodySmall,
                    color = accents.textSecondary,
                    maxLines = 1,
                )
                Spacer(Modifier.height(ZagrosSpacing.sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = formatMoney(product.defaultOffer?.salePrice ?: product.sellPrice),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        // Same line, same height: when the product is already in the basket the
                        // stock figure is replaced by the basket count, so the card still tells
                        // the visitor what he has picked without changing the layout.
                        Text(
                            text = if (inCart > 0) {
                                "✓ در سبد: ${formatQty(inCart)}"
                            } else {
                                "موجودی: ${formatQty(product.availableQty)}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                inCart > 0 -> accents.success
                                product.inStock -> accents.success
                                else -> accents.danger
                            },
                            fontWeight = if (inCart > 0) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (inCart > 0) accents.success
                                else MaterialTheme.colorScheme.primaryContainer,
                            )
                            .clickable(onClick = onQuickAdd),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "افزودن به سبد",
                            tint = if (inCart > 0) {
                                Color.White
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            },
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}