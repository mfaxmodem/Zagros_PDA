package ir.zagroserp.pda.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import ir.zagroserp.pda.core.ThousandSeparator

/**
 * Displays a money field grouped in threes while the state keeps plain digits.
 *
 * The user types `1250000` and sees `1,250,000`; `onValueChange` still delivers raw digits, so
 * no parsing has to be undone anywhere.
 */
class ThousandSeparatorTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val digits = ThousandSeparator.digitsOnly(text.text)
        val formatted = ThousandSeparator.format(digits)
        val (originalToTransformed, transformedToOriginal) = ThousandSeparator.offsetMaps(digits)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                originalToTransformed[offset.coerceIn(0, digits.length)]

            override fun transformedToOriginal(offset: Int): Int =
                transformedToOriginal[offset.coerceIn(0, formatted.length)]
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}
