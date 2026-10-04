package com.shilapi.xcertplay

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Point
import android.view.Surface

/** A fixed request survives a trip to the freely rotating settings activity. */
internal object CarPlayOrientation {
    @Suppress("DEPRECATION")
    fun capture(activity: Activity, rotation: Int): Int {
        val display = activity.windowManager.defaultDisplay
        val size = Point().also { display.getRealSize(it) }
        val quarterTurn = display.rotation == Surface.ROTATION_90 || display.rotation == Surface.ROTATION_270
        val naturalLandscape = (size.x > size.y) != quarterTurn
        // Android uses this platform resource to swap normal/reverse quarter-turn directions.
        val reverseId = activity.resources.getIdentifier("config_reverseDefaultRotation", "bool", "android")
        val reverseDefault = reverseId != 0 && activity.resources.getBoolean(reverseId)
        return fixedOrientation(rotation, naturalLandscape, reverseDefault)
    }

    fun fixedOrientation(rotation: Int, naturalLandscape: Boolean, reverseDefault: Boolean = false): Int {
        val portrait = if (naturalLandscape) {
            if (reverseDefault) Surface.ROTATION_90 else Surface.ROTATION_270
        } else Surface.ROTATION_0
        val landscape = if (naturalLandscape) Surface.ROTATION_0
            else if (reverseDefault) Surface.ROTATION_270 else Surface.ROTATION_90
        return when (rotation) {
            portrait -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            landscape -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            (portrait + 2) % 4 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
            else -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
        }
    }
}
