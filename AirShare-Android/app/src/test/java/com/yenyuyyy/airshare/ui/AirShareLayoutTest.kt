package com.yenyuyyy.airshare.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AirShareLayoutTest {
    @Test
    fun `compact and medium widths keep the focused single-pane flow`() {
        val compact = airShareLayoutProfile(AirShareWidthClass.COMPACT, hasMediumHeight = true)
        val medium = airShareLayoutProfile(AirShareWidthClass.MEDIUM, hasMediumHeight = true)

        assertFalse(compact.useSupportingPane)
        assertEquals(236.dp, compact.qrSize)
        assertFalse(medium.useSupportingPane)
        assertEquals(800.dp, medium.maxContentWidth)
        assertEquals(248.dp, medium.qrSize)
    }

    @Test
    fun `expanded width introduces a supporting pane without crowding the session`() {
        val profile = airShareLayoutProfile(AirShareWidthClass.EXPANDED, hasMediumHeight = true)

        assertTrue(profile.useSupportingPane)
        assertEquals(300.dp, profile.supportingPaneWidth)
        assertEquals(AirShareSessionLayout.STACKED, profile.sessionLayout)
    }

    @Test
    fun `large desktop windows show the QR and controls side by side`() {
        val profile = airShareLayoutProfile(AirShareWidthClass.LARGE, hasMediumHeight = true)

        assertTrue(profile.useSupportingPane)
        assertEquals(AirShareSessionLayout.SIDE_BY_SIDE, profile.sessionLayout)
        assertEquals(280.dp, profile.qrSize)
    }

    @Test
    fun `short windows fall back to a compact-height single pane at any width`() {
        val profile = airShareLayoutProfile(AirShareWidthClass.LARGE, hasMediumHeight = false)

        assertFalse(profile.useSupportingPane)
        assertEquals(AirShareSessionLayout.STACKED, profile.sessionLayout)
        assertEquals(200.dp, profile.qrSize)
        assertEquals(16.dp, profile.horizontalPadding)
    }
}
