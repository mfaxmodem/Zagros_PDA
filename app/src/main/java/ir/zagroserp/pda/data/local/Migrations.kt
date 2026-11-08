package ir.zagroserp.pda.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Schema migrations.
 *
 * These exist so an app update never destroys local data. The offline queue
 * (`pending_orders`, `pending_visits`, `pending_locations`) can hold a full day of field work
 * that has not reached the server yet — a destructive migration would throw it away.
 */
object Migrations {

    /**
     * v1 → v2: adds the day-scoped assignment table behind the "My Route" screen.
     *
     * Purely additive, so no existing row is touched.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `today_customers` (
                    `date` TEXT NOT NULL,
                    `customerId` TEXT NOT NULL,
                    `tourId` TEXT NOT NULL,
                    `sortOrder` INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(`date`, `customerId`)
                )
                """.trimIndent(),
            )
        }
    }

    val ALL = arrayOf(MIGRATION_1_2)
}
