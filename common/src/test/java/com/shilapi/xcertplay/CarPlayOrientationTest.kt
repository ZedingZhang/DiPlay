package com.shilapi.xcertplay

import android.content.ComponentName
import android.content.pm.ActivityInfo
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class CarPlayOrientationTest {
    @Test fun settingsRotateFreelyAndCarPlayStartsLocked() {
        val context = RuntimeEnvironment.getApplication()
        fun orientation(type: Class<*>) = context.packageManager
            .getActivityInfo(ComponentName(context, type), 0).screenOrientation
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR, orientation(DiPlayActivity::class.java))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_LOCKED, orientation(CarPlayHostActivity::class.java))
    }

    @Test fun phonesPreserveAllFourDirectionsIncludingBothLandscapeSides() {
        assertDirections(false, false, listOf(1, 0, 9, 8))
    }

    @Test fun naturallyLandscapeDisplaysPreserveAllFourDirections() {
        assertDirections(true, false, listOf(0, 9, 8, 1))
    }

    @Test fun platformReverseDefaultRotationPreservesTheActualDirection() {
        assertDirections(false, true, listOf(1, 8, 9, 0))
        assertDirections(true, true, listOf(0, 1, 8, 9))
    }

    private fun assertDirections(naturalLandscape: Boolean, reverse: Boolean, expected: List<Int>) {
        for (rotation in 0..3) {
            assertEquals(expected[rotation], CarPlayOrientation.fixedOrientation(rotation, naturalLandscape, reverse))
        }
    }
}
