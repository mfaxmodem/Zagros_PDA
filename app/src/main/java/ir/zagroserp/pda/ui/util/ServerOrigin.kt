package ir.zagroserp.pda.ui.util

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Runtime server origin (`scheme://host:port/`) of the company this install points at.
 * Provided once at the root so product images resolve against the same host as the API
 * (API section 4.6.1 – the origin, never `/api/v1`).
 */
val LocalServerOrigin = staticCompositionLocalOf { "" }
