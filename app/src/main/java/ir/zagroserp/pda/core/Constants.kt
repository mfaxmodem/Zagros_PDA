package ir.zagroserp.pda.core

object Api {
    const val LOGIN = "auth/login"
    const val REFRESH = "auth/refresh"
    const val LOGOUT = "auth/logout"

    const val HEALTH = "mobile/visitor/health"
    const val TODAY = "mobile/visitor/today"
    const val PROFILE = "mobile/visitor/profile"
    const val DISCOUNTS = "mobile/visitor/discounts"
    const val PRODUCTS = "mobile/visitor/products"
    const val PRODUCTS_PROMO = "mobile/visitor/products-promo"
    const val PRODUCT_LOOKUP = "mobile/visitor/products/lookup"
    const val PRODUCT_STOCK = "mobile/products/stock"
    const val CUSTOMERS = "mobile/visitor/customers"
    const val DEVICES = "mobile/visitor/devices"
    const val VISITS = "mobile/visitor/visits"
    const val VISITS_BATCH = "mobile/visitor/visits/batch"
    const val ORDERS = "mobile/visitor/orders"
    const val ORDERS_BATCH = "mobile/visitor/orders/batch"
    const val GOALS = "mobile/visitor/goals"
    const val REPORTS = "mobile/visitor/reports"
    const val MESSAGES = "mobile/visitor/messages"
    const val MESSAGES_STREAM = "mobile/visitor/messages/stream"
    const val SYNC = "mobile/visitor/sync"
    const val APPROVED_INVOICES = "mobile/visitor/approved-invoices"
    const val ZONE_PERMISSIONS = "mobile/visitor/zone-permissions"
    const val LOCATION = "mobile/visitor/location"
    const val LOCATIONS_BATCH = "mobile/visitor/locations/batch"
}

object Db {
    const val NAME = "zagros_pda.db"
    const val VERSION = 1
}

object SyncLimits {
    const val ORDER_BATCH_MAX = 50
    const val VISIT_BATCH_MAX = 100
    const val LOCATION_BATCH_MAX = 500
}

object WorkNames {
    const val UNIQUE_SYNC = "zagros_offline_sync"
    const val UNIQUE_DELTA = "zagros_delta_sync"
    const val UNIQUE_LOCATION = "zagros_location_sync"
}

object Notifications {
    const val LOCATION_CHANNEL_ID = "zagros_location_channel"
    const val LOCATION_NOTIFICATION_ID = 4201
}

object Defaults {
    const val PAGE_SIZE = 50
    const val MAX_PAGE_SIZE = 200
}
