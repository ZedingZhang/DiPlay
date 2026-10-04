package com.shilapi.xcertplay

import android.app.AlertDialog
import android.content.Intent
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import com.shilapi.xcertplay.airplay.SafeAreaRect
import com.shilapi.xcertplay.host.R
import com.shilapi.xcertplay.orchestration.MfiTarget
import java.util.concurrent.ExecutorService
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], qualifiers = "zh-rCN-w800dp-h393dp-land-xhdpi")
class UnifiedSettingsTest {
    private lateinit var activity: DiPlayActivity
    private var host: CarPlayHostActivity? = null

    @After fun cleanUp() {
        CarPlayBackgroundSession.clear()
        host?.let {
            (ReflectionHelpers.getField<ExecutorService>(it, "teardownExecutor")).shutdownNow()
            (ReflectionHelpers.getField<ExecutorService>(it, "airPlayCommandExecutor")).shutdownNow()
        }
    }

    @Test fun homeAndGestureIntentsExposeTheSameSettingsAndKeepAdvancedOptionsCollapsed() {
        create()
        buttons().first { it.text == activity.getString(R.string.settings) }.performClick()
        val homeLabels = buttons().map { it.text.toString() }
        openSettings()
        assertEquals(homeLabels, buttons().map { it.text.toString() })
        assertTrue(buttons().any { it.text == activity.getString(R.string.show_advanced_settings) })
        assertFalse(buttons().any { it.text.startsWith(activity.getString(R.string.manufacturer)) })
        expand()
        assertTrue(buttons().any { it.text.startsWith(activity.getString(R.string.manufacturer)) })
        assertTrue(buttons().any { it.text.startsWith(activity.getString(R.string.mfi_certificate_signing_target)) })
        assertTrue(views(activity.window.decorView).filterIsInstance<Switch>().any {
            it.contentDescription == activity.getString(R.string.hevc_software_decoder)
        })
        assertFalse(button(R.string.safe_area).isEnabled)
        button(R.string.hide_advanced_settings).performClick()
        assertFalse(buttons().any { it.text.startsWith(activity.getString(R.string.manufacturer)) })
    }

    @Test fun identityAndSoftwareDecoderEditsUseTheExistingPreferencesAndCancelDiscardsText() {
        settings()
        fun identity() = buttons().first { it.text.startsWith(activity.getString(R.string.manufacturer)) }
        identity().performClick()
        val cancelled = dialog()
        input(cancelled).setText("Cancelled")
        cancelled.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(AirPlayPersistence.DEFAULT_MANUFACTURER, AirPlayPersistence.loadManufacturer(activity))
        identity().performClick()
        val saved = dialog()
        input(saved).setText("My phone")
        saved.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        assertEquals("My phone", AirPlayPersistence.loadManufacturer(activity))
        val software = views(activity.window.decorView).filterIsInstance<Switch>().single {
            it.contentDescription == activity.getString(R.string.hevc_software_decoder)
        }
        software.performClick()
        assertTrue(AirPlayPersistence.loadHevcSoftwareDecoderEnabled(activity))
        openSettings()
        assertTrue(buttons().any { it.text.toString().contains("My phone") })
    }

    @Test fun missingLocalIdentityDoesNotOverwriteUsbAuthenticationAndCancelKeepsIt() {
        create()
        AirPlayPersistence.saveMfiTarget(activity, MfiTarget.USB_CH341)
        openSettings(); expand()
        val control = buttons().first { it.text.startsWith(activity.getString(R.string.mfi_certificate_signing_target)) }
        control.performClick()
        val invalid = dialog()
        invalid.listView.performItemClick(null, 0, 0L)
        invalid.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        assertEquals(MfiTarget.USB_CH341, AirPlayPersistence.loadMfiTarget(activity))
        assertTrue(invalid.isShowing)
        invalid.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
        assertEquals(MfiTarget.USB_CH341, AirPlayPersistence.loadMfiTarget(activity))
    }

    @Test fun usbAuthenticationCanBeSavedWithoutCreatingLocalIdentity() {
        settings()
        buttons().first { it.text.startsWith(activity.getString(R.string.mfi_certificate_signing_target)) }.performClick()
        val saved = dialog()
        saved.listView.performItemClick(null, 1, 1L)
        saved.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        assertEquals(MfiTarget.USB_CH341, AirPlayPersistence.loadMfiTarget(activity))
        assertFalse(saved.isShowing)
    }

    @Test fun safeAreaCancelAndResetAreDraftsAndSavingUsesTheCapturedCarPlayDimensions() {
        settings()
        host = Robolectric.buildActivity(CarPlayHostActivity::class.java).get()
        val sizeClass = Class.forName("com.shilapi.xcertplay.CarPlayHostActivity\$DisplaySize")
        fun size(width: Int, height: Int): Any = sizeClass.getDeclaredConstructor(Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
            .apply { isAccessible = true }.newInstance(width, height)
        ReflectionHelpers.setField(host, "activeDisplaySize", size(1920, 990))
        ReflectionHelpers.setField(CarPlayBackgroundSession, "owner", host)
        val original = SafeAreaRect(20, 20, 1800, 900)
        AirPlayPersistence.saveSafeAreaRect(activity, 1920, 990, original)
        render()
        assertTrue(button(R.string.safe_area).isEnabled)
        button(R.string.safe_area).performClick()
        val cancelled = dialog()
        cancelled.getButton(AlertDialog.BUTTON_NEUTRAL).performClick()
        assertEquals(original, AirPlayPersistence.loadSafeAreaRect(activity, 1920, 990))
        cancelled.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
        assertEquals(original, AirPlayPersistence.loadSafeAreaRect(activity, 1920, 990))
        button(R.string.safe_area).performClick()
        val saved = dialog()
        val edited = SafeAreaRect(30, 30, 1700, 850)
        views(saved.window!!.decorView).filterIsInstance<SafeAreaEditorView>().single().setRect(edited, 1920, 990)
        ReflectionHelpers.setField(host, "activeDisplaySize", size(990, 1920))
        saved.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        assertEquals(edited, AirPlayPersistence.loadSafeAreaRect(activity, 1920, 990))
        assertNull(AirPlayPersistence.loadSafeAreaRect(activity, 990, 1920))
    }

    @Test fun fineFrameRatesReadAndSaveWithoutLosingAStoredIntermediateValue() {
        create()
        AirPlayPersistence.saveFps(activity, 55)
        openSettings()
        val control = buttons().first { it.text.startsWith(activity.getString(R.string.frame_rate)) }
        assertTrue(control.text.contains("55"))
        control.performClick()
        val saved = dialog()
        saved.listView.performItemClick(null, 1, 1L)
        saved.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(35, AirPlayPersistence.loadFps(activity))
    }

    private fun create() {
        ReflectionHelpers.setStaticField(DiPlayBootstrap::class.java, "ready", false)
        activity = Robolectric.buildActivity(DiPlayActivity::class.java).get()
        activity.setTheme(R.style.Theme_Xcertplay)
        render()
    }
    private fun settings() { create(); openSettings(); expand() }
    private fun openSettings() = activity.javaClass.getDeclaredMethod("onNewIntent", Intent::class.java)
        .apply { isAccessible = true }.invoke(activity, Intent(activity, DiPlayActivity::class.java).putExtra("page", "settings"))
    private fun expand() = button(R.string.show_advanced_settings).performClick()
    private fun render() = activity.javaClass.getDeclaredMethod("render").apply { isAccessible = true }.invoke(activity)
    private fun dialog(): AlertDialog {
        shadowOf(Looper.getMainLooper()).idle()
        return ShadowAlertDialog.getLatestAlertDialog()
    }
    private fun input(dialog: AlertDialog) = views(dialog.window!!.decorView).filterIsInstance<EditText>().single()
    private fun buttons() = views(activity.window.decorView).filterIsInstance<Button>()
    private fun button(id: Int) = buttons().first { it.text == activity.getString(id) }
    private fun views(view: View): List<View> = listOf(view) +
        if (view is ViewGroup) (0 until view.childCount).flatMap { views(view.getChildAt(it)) } else emptyList()
}
