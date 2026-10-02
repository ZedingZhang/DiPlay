package com.shilapi.xcertplay.airplay

/**
 * The single user-facing CarPlay size. Keep the stored physical-width presets compatible,
 * but also change the negotiated canvas: some iPhones ignore physical-size-only changes.
 */
enum class CarPlaySize(val label: String, val widthMillimeters: Int, val uiScalePercent: Int) {
    LARGE("Large", 250, 115),
    MEDIUM("Medium", 300, 100),
    SMALL("Small", 350, 85);

    companion object {
        val DEFAULT = MEDIUM

        /** Maps any stored width, including values from older builds, to the nearest preset. */
        fun fromWidthMillimeters(millimeters: Int): CarPlaySize =
            entries.minBy { kotlin.math.abs(it.widthMillimeters - millimeters) }
    }
}
