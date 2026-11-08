package ir.zagroserp.pda.ui.navigation

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Jumps straight to the dashboard from anywhere.
 *
 * This exists instead of hijacking the system back button: Android users expect back to undo one
 * step, and the Material guidelines require it. A dedicated home affordance gives the visitor the
 * one-tap escape they actually want without breaking that contract.
 */
val LocalGoHome = staticCompositionLocalOf<(() -> Unit)?> { null }
