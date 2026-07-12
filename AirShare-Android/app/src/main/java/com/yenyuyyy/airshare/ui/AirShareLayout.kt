package com.yenyuyyy.airshare.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass

internal const val AIRSHARE_LARGE_WIDTH_DP = 1200

internal enum class AirShareWidthClass {
    COMPACT,
    MEDIUM,
    EXPANDED,
    LARGE,
}

internal enum class AirShareSessionLayout {
    STACKED,
    SIDE_BY_SIDE,
}

internal data class AirShareLayoutProfile(
    val useSupportingPane: Boolean,
    val sessionLayout: AirShareSessionLayout,
    val maxContentWidth: Dp,
    val horizontalPadding: Dp,
    val supportingPaneWidth: Dp,
    val paneGap: Dp,
    val qrSize: Dp,
)

internal fun WindowSizeClass.toAirShareLayoutProfile(
    hasLargeWidth: Boolean,
): AirShareLayoutProfile {
    val widthClass = when {
        hasLargeWidth -> AirShareWidthClass.LARGE
        isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> AirShareWidthClass.EXPANDED
        isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> AirShareWidthClass.MEDIUM
        else -> AirShareWidthClass.COMPACT
    }
    return airShareLayoutProfile(
        widthClass = widthClass,
        hasMediumHeight = isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND),
    )
}

internal fun airShareLayoutProfile(
    widthClass: AirShareWidthClass,
    hasMediumHeight: Boolean,
): AirShareLayoutProfile {
    if (!hasMediumHeight) {
        return AirShareLayoutProfile(
            useSupportingPane = false,
            sessionLayout = AirShareSessionLayout.STACKED,
            maxContentWidth = 760.dp,
            horizontalPadding = 16.dp,
            supportingPaneWidth = 0.dp,
            paneGap = 0.dp,
            qrSize = 200.dp,
        )
    }
    return when (widthClass) {
        AirShareWidthClass.COMPACT -> AirShareLayoutProfile(
            useSupportingPane = false,
            sessionLayout = AirShareSessionLayout.STACKED,
            maxContentWidth = 760.dp,
            horizontalPadding = 20.dp,
            supportingPaneWidth = 0.dp,
            paneGap = 0.dp,
            qrSize = 236.dp,
        )

        AirShareWidthClass.MEDIUM -> AirShareLayoutProfile(
            useSupportingPane = false,
            sessionLayout = AirShareSessionLayout.STACKED,
            maxContentWidth = 800.dp,
            horizontalPadding = 32.dp,
            supportingPaneWidth = 0.dp,
            paneGap = 0.dp,
            qrSize = 248.dp,
        )

        AirShareWidthClass.EXPANDED -> AirShareLayoutProfile(
            useSupportingPane = true,
            sessionLayout = AirShareSessionLayout.STACKED,
            maxContentWidth = 1120.dp,
            horizontalPadding = 32.dp,
            supportingPaneWidth = 300.dp,
            paneGap = 24.dp,
            qrSize = 244.dp,
        )

        AirShareWidthClass.LARGE -> AirShareLayoutProfile(
            useSupportingPane = true,
            sessionLayout = AirShareSessionLayout.SIDE_BY_SIDE,
            maxContentWidth = 1280.dp,
            horizontalPadding = 40.dp,
            supportingPaneWidth = 340.dp,
            paneGap = 32.dp,
            qrSize = 280.dp,
        )
    }
}
