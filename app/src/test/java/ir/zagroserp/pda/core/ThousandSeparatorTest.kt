package ir.zagroserp.pda.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ThousandSeparatorTest {

    @Test
    fun `groups digits in threes from the right`() {
        assertEquals("1,000", ThousandSeparator.format("1000"))
        assertEquals("12,500", ThousandSeparator.format("12500"))
        assertEquals("1,250,000", ThousandSeparator.format("1250000"))
        assertEquals("1,234,567,890", ThousandSeparator.format("1234567890"))
    }

    @Test
    fun `short values are left alone`() {
        assertEquals("", ThousandSeparator.format(""))
        assertEquals("5", ThousandSeparator.format("5"))
        assertEquals("42", ThousandSeparator.format("42"))
        assertEquals("999", ThousandSeparator.format("999"))
    }

    @Test
    fun `exact group boundaries do not gain a leading separator`() {
        assertEquals("100", ThousandSeparator.format("100"))
        assertEquals("1,000", ThousandSeparator.format("1000"))
        assertEquals("10,000", ThousandSeparator.format("10000"))
        assertEquals("100,000", ThousandSeparator.format("100000"))
    }

    @Test
    fun `input is sanitised to digits only`() {
        assertEquals("1250000", ThousandSeparator.digitsOnly("1,250,000"))
        assertEquals("1250000", ThousandSeparator.digitsOnly("۱٬۲۵۰٬۰۰۰"))
        assertEquals("123", ThousandSeparator.digitsOnly("1a2b3"))
    }

    @Test
    fun `formatting is idempotent when the digits are already clean`() {
        val once = ThousandSeparator.format("1250000")
        assertEquals(once, ThousandSeparator.format(ThousandSeparator.digitsOnly(once)))
    }

    @Test
    fun `parse round trips formatted and raw values`() {
        assertEquals(1_250_000L, ThousandSeparator.parse("1,250,000"))
        assertEquals(1_250_000L, ThousandSeparator.parse("1250000"))
        assertEquals(0L, ThousandSeparator.parse(""))
        assertEquals(0L, ThousandSeparator.parse("abc"))
    }

    @Test
    fun `original to transformed offsets account for inserted separators`() {
        val (o2t, _) = ThousandSeparator.offsetMaps("1250000")
        // "1250000" -> "1,250,000"
        //  index: 0='1' 1=',' 2='2' 3='5' 4='0' 5=',' 6='0' 7='0' 8='0'
        assertEquals(0, o2t[0])
        assertEquals(2, o2t[1])
        assertEquals(3, o2t[2])
        assertEquals(6, o2t[4])
        assertEquals(7, o2t[5])
        assertEquals(8, o2t[6])
        assertEquals(9, o2t[7]) // end of string
    }

    @Test
    fun `transformed to original offsets map separators onto the following digit`() {
        val (_, t2o) = ThousandSeparator.offsetMaps("1250000")
        assertEquals(0, t2o[0])
        assertEquals(1, t2o[1]) // the first separator sits before digit 1
        assertEquals(1, t2o[2])
        assertEquals(4, t2o[5]) // the second separator sits before digit 4
        assertEquals(7, t2o[9]) // end of string
    }

    @Test
    fun `persian and arabic-indic digits are normalised to ascii`() {
        assertEquals("1250000", ThousandSeparator.digitsOnly("۱۲۵۰۰۰۰"))
        assertEquals("1250000", ThousandSeparator.digitsOnly("١٢٥٠٠٠٠"))
        // Mixed input, with separators, must still parse.
        assertEquals(1_250_000L, ThousandSeparator.parse("۱٬۲۵۰٬۰۰۰"))
    }

    @Test
    fun `offset maps are valid for empty and short input`() {
        val (o2t, t2o) = ThousandSeparator.offsetMaps("")
        assertEquals(1, o2t.size)
        assertEquals(1, t2o.size)
        assertEquals(0, o2t[0])

        val (shortO2t, shortT2o) = ThousandSeparator.offsetMaps("42")
        assertEquals(3, shortO2t.size)
        assertEquals(3, shortT2o.size)
    }
}
