package ir.zagroserp.pda.ui.navigation

object Routes {
    const val SPLASH = "splash"
    const val PERMISSIONS = "permissions"
    const val SERVER_SETUP = "server_setup"
    const val LOGIN = "login"

    const val DASHBOARD = "dashboard"
    const val TODAY_ROUTE = "today"
    const val CUSTOMERS = "customers"
    const val CUSTOMER_DETAIL = "customer/{customerId}"
    const val CUSTOMER_EDIT = "customer/{customerId}/edit"
    const val CUSTOMER_NEW = "customer/new"
    const val CATALOG = "catalog"

    /** Catalog opened for a specific customer – seeds the cart so it is not asked for twice. */
    const val CATALOG_FOR_CUSTOMER = "catalog/customer/{customerId}"
    const val CART = "cart"
    const val ORDERS = "orders"
    const val ORDER_DETAIL = "order/{orderId}"
    const val INVOICES = "invoices"
    const val REPORTS = "reports"
    const val MESSAGES = "messages"

    /** Out-of-zone visit approvals. */
    const val ZONE_PERMISSIONS = "zone_permissions"
    const val SETTINGS = "settings"

    fun customerDetail(id: String) = "customer/$id"
    fun customerEdit(id: String) = "customer/$id/edit"
    fun orderDetail(id: String) = "order/$id"
    fun catalogForCustomer(id: String) = "catalog/customer/$id"

    const val ARG_CUSTOMER_ID = "customerId"
    const val ARG_ORDER_ID = "orderId"
    const val ARG_CUSTOMER_NAME = "customerName"
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val emoji: String,
)

/**
 * Order matters. The layout is RTL, so index 0 renders at the **right** edge. `Dashboard` is
 * deliberately placed at the middle index so it sits in the centre of the bar, flanked by two
 * destinations on each side.
 */
val bottomNavItems = listOf(
    BottomNavItem(Routes.TODAY_ROUTE, "مسیر من", "📍"),
    BottomNavItem(Routes.CUSTOMERS, "مشتریان", "👥"),
    BottomNavItem(Routes.DASHBOARD, "داشبورد", "🏠"),
    BottomNavItem(Routes.CATALOG, "کاتالوگ", "🛒"),
    BottomNavItem(Routes.MESSAGES, "پیام‌ها", "🔔"),
)

/** The destination rendered in the centre of the bottom bar. */
val bottomNavCenterRoute: String = Routes.DASHBOARD
