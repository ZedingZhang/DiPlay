package com.shilapi.xcertplay

import android.app.AlertDialog
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import com.shilapi.xcertplay.network.WifiP2pChannels
import com.shilapi.xcertplay.orchestration.WirelessHotspotMode
import com.shilapi.xcertplay.host.R
import java.io.File
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], qualifiers = "zh-rCN-w800dp-h393dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DiPlayLandscapeLayoutTest {
    private lateinit var activity: DiPlayActivity

    @After fun cleanUp() { CarPlayBackgroundSession.clear() }

    @Test fun settingsShowTwoCompleteGroupsAndMoreSectionsAboveTheFold() {
        create()
        button(layout(), R.string.settings).performClick()
        val root = layout()
        val controls = sectionBounds(root, R.string.carplay_controls)
        val connection = sectionBounds(root, R.string.connection_setup)
        assertEquals(controls.top, connection.top)
        assertTrue(controls.right < connection.left)
        assertTrue(controls.bottom <= root.height)
        assertTrue(connection.bottom <= root.height)
        listOf(R.string.diagnostics, R.string.automatic_connection).forEach { id ->
            assertTrue("${activity.getString(id)} should be visible", bounds(root, text(root, id)).bottom <= root.height)
        }
        val gesture = buttons(root).single { it.text.startsWith(activity.getString(R.string.settings_gesture_fingers_label)) }
        gesture.performClick()
        var dialog = ShadowAlertDialog.getLatestAlertDialog()
        dialog.listView.performItemClick(null, 2, 2L)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
        assertEquals(3, AirPlayPersistence.loadSettingsGestureFingers(activity))
        gesture.performClick()
        dialog = ShadowAlertDialog.getLatestAlertDialog()
        dialog.listView.performItemClick(null, 2, 2L)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        assertEquals(4, AirPlayPersistence.loadSettingsGestureFingers(activity))
        val switches = descendants(root).filterIsInstance<Switch>()
        val hevc = switches.single { it.contentDescription == activity.getString(R.string.efficient_video) }
        val drive = switches.single { it.contentDescription == activity.getString(R.string.right_hand_drive) }
        assertEquals(bounds(root, hevc).top, bounds(root, drive).top)
        assertTrue(bounds(root, hevc).right < bounds(root, drive).left)
        preview(root, "mi10-settings-two-column")
    }

    @Test fun connectionModesAndPairingShareTheFirstScreenWithWirelessAndUsbActions() {
        create()
        button(layout(), R.string.connection_setup).performClick()
        for (mode in listOf(WirelessHotspotMode.MANUAL, WirelessHotspotMode.WIFI_P2P)) {
            AirPlayPersistence.saveWirelessHotspotMode(activity, mode)
            render()
            val root = layout()
            val first = text(root, R.string.s_1_choose_your_connection)
            val second = text(root, R.string.s_2_pair_your_iphone)
            assertEquals(bounds(root, first).top, bounds(root, second).top)
            assertTrue(bounds(root, first).right < bounds(root, second).left)
            val wireless = button(root, R.string.connect_phone)
            val usb = button(root, R.string.connect_with_usb)
            assertEquals(bounds(root, wireless).top, bounds(root, usb).top)
            listOf(wireless, usb, button(root, R.string.review_app_permissions)).forEach { action ->
                assertTrue("${action.text} below the viewport", bounds(root, action).bottom <= root.height)
                assertTrue(action.height >= dp(48) && action.width >= dp(48))
            }
            val shown = if (mode == WirelessHotspotMode.MANUAL) R.string.hotspot_mode_manual_desc else R.string.hotspot_mode_p2p_desc
            val hidden = if (mode == WirelessHotspotMode.MANUAL) R.string.hotspot_mode_p2p_desc else R.string.hotspot_mode_manual_desc
            assertNotNull(text(root, shown))
            assertFalse(descendants(root).filterIsInstance<TextView>().any { it.text == activity.getString(hidden) })
            preview(root, "mi10-connection-${mode.name.lowercase()}")
        }
    }

    @Test fun compactHotspotSetupStillRequiresSaveAndCancelKeepsWifiDirect() {
        create()
        AirPlayPersistence.saveWirelessHotspotMode(activity, WirelessHotspotMode.WIFI_P2P)
        button(layout(), R.string.connection_setup).performClick()
        button(layout(), R.string.built_in_car_hotspot).performClick()
        assertEquals(WirelessHotspotMode.WIFI_P2P, AirPlayPersistence.loadWirelessHotspotMode(activity))
        button(layout(), R.string.save_hotspot_details_and_use_this_mode).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        var dialog = ShadowAlertDialog.getLatestAlertDialog()
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
        assertEquals(WirelessHotspotMode.WIFI_P2P, AirPlayPersistence.loadWirelessHotspotMode(activity))
        button(layout(), R.string.save_hotspot_details_and_use_this_mode).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        dialog = ShadowAlertDialog.getLatestAlertDialog()
        val fields = descendants(dialog.window!!.decorView).filterIsInstance<EditText>()
        fields[0].setText("Landscape test hotspot")
        fields[1].setText("12345678")
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        assertEquals(WirelessHotspotMode.MANUAL, AirPlayPersistence.loadWirelessHotspotMode(activity))
        assertEquals("Landscape test hotspot", AirPlayPersistence.loadManualHotspotSsid(activity))
        assertNotNull(button(layout(), R.string.connect_with_usb))
    }

    @Test @Config(qualifiers = "zh-rCN-w580dp-h330dp-land-xhdpi")
    fun narrowSettingsKeepOneColumnWithoutSqueezingTheGroups() {
        create()
        button(layout(), R.string.settings).performClick()
        val root = layout()
        val controls = sectionBounds(root, R.string.carplay_controls)
        val connection = sectionBounds(root, R.string.connection_setup)
        assertEquals(controls.left, connection.left)
        assertEquals(controls.right, connection.right)
        assertTrue(controls.bottom < connection.top)
        buttons(root).forEach { assertTrue(it.height >= dp(48)) }
    }

    @Test @Config(qualifiers = "zh-rCN-w1024dp-h600dp-land-xhdpi")
    fun carSizedSettingsRetainTheOriginalFullWidthSectionsAndLargeButtons() {
        create()
        button(layout(), R.string.settings).performClick()
        val root = layout()
        val controls = sectionBounds(root, R.string.carplay_controls)
        val connection = sectionBounds(root, R.string.connection_setup)
        assertEquals(controls.left, connection.left)
        assertEquals(controls.right, connection.right)
        assertTrue(controls.bottom < connection.top)
        assertEquals(34f * activity.resources.displayMetrics.scaledDensity, text(root, R.string.your_drive_your_way).textSize, .1f)
        buttons(root).forEach { assertTrue(it.height >= dp(56)) }
    }

    @Test fun bothSettingsPagesWrapLargeEnglishTextWithoutClippingControls() {
        RuntimeEnvironment.setQualifiers("en-w800dp-h343dp-land-xhdpi")
        create()
        val config = Configuration(activity.resources.configuration).apply { fontScale = 1.3f }
        @Suppress("DEPRECATION")
        activity.resources.updateConfiguration(config, activity.resources.displayMetrics)
        button(layout(), R.string.settings).performClick()
        for (page in listOf("settings", "connection")) {
            ReflectionHelpers.setField(activity, "page", page)
            render()
            val root = layout()
            buttons(root).forEach { control ->
                assertTrue(control.height >= dp(48) && control.width >= dp(48))
                val lines = requireNotNull(control.layout)
                assertTrue("${control.text} clipped", lines.getLineBottom(lines.lineCount - 1) <=
                    control.height - control.compoundPaddingTop - control.compoundPaddingBottom)
            }
            preview(root, "phone-$page-english-large-text")
        }
    }

    @Test fun connectedPhoneActionsFitAboveTheFoldWithAccessibleTouchTargets() {
        create()
        // A running session state without starting real USB, Bluetooth or native transports.
        ReflectionHelpers.setField(CarPlayBackgroundSession, "stopAction", { done: () -> Unit ->
            CarPlayBackgroundSession.clear(); done()
        })
        CarPlayBackgroundSession.active = true
        render()
        val root = layout()
        val actions = listOf(R.string.open_carplay, R.string.choose_iphone, R.string.disconnect,
            R.string.connect_with_usb, R.string.connection_setup, R.string.settings)
        actions.forEach { id ->
            val button = buttons(root).last { it.text.toString() == activity.getString(id) }
            val bounds = bounds(root, button)
            assertTrue("${button.text} below the viewport: $bounds", bounds.bottom <= root.height)
            assertTrue("${button.text} above the viewport: $bounds", bounds.top >= 0)
            assertTrue("${button.text} touch height", button.height >= dp(48))
            assertTrue("${button.text} touch width", button.width >= dp(48))
        }
        preview(root, "mi10-home-connected")
        button(root, R.string.disconnect).performClick()
        assertEquals(View.GONE, button(root, R.string.disconnect).visibility)
        assertEquals(activity.getString(R.string.connect_phone), button(root, R.string.connect_phone).text)
    }

    @Test fun settingsKeepNavigationVisibleAndPairedChoicesStillSave() {
        create()
        var root = layout()
        button(root, R.string.settings).performClick()
        root = layout()
        val back = button(root, R.string.back)
        val before = bounds(root, back)
        val resolution = buttons(root).first { it.text.startsWith(activity.getString(R.string.resolution)) }
        val buffer = buttons(root).first { it.text.startsWith(activity.getString(R.string.music_buffer)) }
        assertEquals(bounds(root, resolution).top, bounds(root, buffer).top)
        assertTrue(bounds(root, resolution).right < bounds(root, buffer).left)
        val scroll = descendants(root).filterIsInstance<ScrollView>().single()
        scroll.scrollTo(0, bounds(root, resolution).top - scroll.top - dp(60))
        preview(root, "mi10-display-settings")
        resolution.performClick()
        // OnShow installs the validated Save listener via the main looper.
        shadowOf(Looper.getMainLooper()).idle()
        val dialog = ShadowAlertDialog.getLatestAlertDialog()
        descendants(dialog.window!!.decorView).filterIsInstance<EditText>().single().setText("57")
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(57, AirPlayPersistence.loadDisplayScalePercent(activity))
        assertTrue(resolution.text.contains("57"))
        resolution.performClick()
        shadowOf(Looper.getMainLooper()).idle()
        val cancelled = ShadowAlertDialog.getLatestAlertDialog()
        descendants(cancelled.window!!.decorView).filterIsInstance<EditText>().single().setText("83")
        cancelled.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(57, AirPlayPersistence.loadDisplayScalePercent(activity))
        scroll.scrollTo(0, scroll.getChildAt(0).height)
        assertTrue(scroll.scrollY > 0)
        assertEquals(before, bounds(root, back))
        back.performClick()
        assertNotNull(button(layout(), R.string.connect_phone))
    }

    @Test fun rotationPreservesThePageAndNarrowPortraitKeepsPrimaryActionsAccessible() {
        create()
        button(layout(), R.string.settings).performClick()
        RuntimeEnvironment.setQualifiers("zh-rCN-w393dp-h800dp-port-xhdpi")
        activity.onConfigurationChanged(activity.resources.configuration)
        assertNotNull(button(layout(), R.string.back))
        assertTrue(button(layout(), R.string.back).height >= dp(48))
        button(layout(), R.string.back).performClick()
        val portrait = layout()
        listOf(R.string.connect_phone, R.string.choose_iphone, R.string.connect_with_usb,
            R.string.connection_setup, R.string.settings).forEach { id ->
            val action = button(portrait, id)
            assertTrue(action.height >= dp(48))
            assertTrue(bounds(portrait, action).bottom <= portrait.height)
        }
        RuntimeEnvironment.setQualifiers("zh-rCN-w800dp-h393dp-land-xhdpi")
        activity.onConfigurationChanged(activity.resources.configuration)
        val landscape = layout()
        assertFalse(descendants(landscape).filterIsInstance<TextView>()
            .any { it.text == activity.getString(R.string.a_familiar_drive) })
        assertTrue(bounds(landscape, button(landscape, R.string.connect_with_usb)).bottom <= landscape.height)
    }

    @Test fun narrowWindowKeepsNavigationVisibleWhileSettingsScroll() {
        RuntimeEnvironment.setQualifiers("zh-rCN-w360dp-h300dp-land-xhdpi")
        create()
        var root = layout()
        val headerSettings = buttons(root).first { it.text == activity.getString(R.string.settings) }
        assertTrue(bounds(root, headerSettings).right <= root.width)
        assertTrue(headerSettings.height >= dp(48))
        assertNotNull(button(root, R.string.connection_setup))
        headerSettings.performClick()
        root = layout()
        val back = button(root, R.string.back)
        val before = bounds(root, back)
        val scroll = descendants(root).filterIsInstance<ScrollView>().single()
        scroll.scrollTo(0, scroll.getChildAt(0).height)
        assertTrue(scroll.scrollY > 0)
        assertEquals(before, bounds(root, back))
        assertTrue(before.right <= root.width && before.top >= 0 && before.bottom <= root.height)
        assertTrue(back.width >= dp(48) && back.height >= dp(48))
        preview(root, "mi10-narrow-window")
        back.performClick()
        assertNotNull(button(layout(), R.string.connect_phone))
    }

    @Test fun unifiedAdvancedOptionsKeepTheFixedHeaderAndAccessibleControls() {
        create()
        button(layout(), R.string.settings).performClick()
        var root = layout()
        button(root, R.string.show_advanced_settings).performClick()
        root = layout()
        val back = button(root, R.string.back)
        val before = bounds(root, back)
        val scroll = descendants(root).filterIsInstance<ScrollView>().single()
        val hide = button(root, R.string.hide_advanced_settings)
        scroll.scrollTo(0, bounds(root, hide).top - scroll.top)
        assertEquals(before, bounds(root, back))
        buttons(root).filter { it.visibility == View.VISIBLE }.forEach {
            assertTrue("${it.text} touch height", it.height >= dp(48))
        }
        preview(root, "mi10-unified-advanced-settings")
        assertNotNull(button(root, R.string.safe_area))
    }

    @Test fun upstreamChannelChoiceAndScrollRestorationWorkInCompactConnectionPage() {
        create()
        AirPlayPersistence.saveWirelessHotspotMode(activity, WirelessHotspotMode.WIFI_P2P)
        button(layout(), R.string.connection_setup).performClick()
        var root = layout()
        fun channelControl() = buttons(root).first {
            it.text.startsWith(activity.getString(R.string.wifi_direct_channel_summary, ""))
        }
        val control = channelControl()
        assertTrue(control.height >= dp(48))
        control.performClick()
        val dialog = ShadowAlertDialog.getLatestAlertDialog()
        val channelIndex = (listOf(WifiP2pChannels.AUTO) + WifiP2pChannels.channels).indexOf(6)
        dialog.listView.performItemClick(null, channelIndex, channelIndex.toLong())
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(6, AirPlayPersistence.loadWifiP2pPreferredChannel(activity))
        val scroll = descendants(root).filterIsInstance<ScrollView>().single()
        scroll.scrollTo(0, scroll.getChildAt(0).height)
        val position = scroll.scrollY
        assertTrue(position > 0)
        render()
        root = layout()
        assertEquals(position, descendants(root).filterIsInstance<ScrollView>().single().scrollY)
        assertTrue(channelControl().text.toString().contains("6"))
        assertTrue(bounds(root, button(root, R.string.back)).bottom <= dp(60))
        preview(root, "mi10-upstream-channel-settings")
    }

    @Test
    @Config(qualifiers = "zh-rCN-w1024dp-h600dp-land-xhdpi")
    fun tallerCarWindowsRetainTheirLargeControls() {
        create()
        val root = layout()
        val hero = descendants(root).filterIsInstance<TextView>()
            .single { it.text == activity.getString(R.string.a_familiar_drive) }
        assertEquals(42f * activity.resources.displayMetrics.scaledDensity, hero.textSize, .1f)
        assertEquals(dp(68), button(root, R.string.connect_phone).height)
    }

    @Test fun largerEnglishTextCanWrapWithoutClippingButtons() {
        RuntimeEnvironment.setQualifiers("en-w800dp-h393dp-land-xhdpi")
        create()
        val config = Configuration(activity.resources.configuration).apply { fontScale = 1.3f }
        @Suppress("DEPRECATION")
        activity.resources.updateConfiguration(config, activity.resources.displayMetrics)
        render()
        val root = layout()
        assertEquals(1.3f, activity.resources.displayMetrics.scaledDensity /
            activity.resources.displayMetrics.density, .01f)
        buttons(root).filter { it.visibility == View.VISIBLE }.forEach { button ->
            assertTrue(button.height >= dp(48))
            val text = requireNotNull(button.layout)
            assertTrue("${button.text} clipped", text.getLineBottom(text.lineCount - 1) <=
                button.height - button.compoundPaddingTop - button.compoundPaddingBottom)
        }
        preview(root, "phone-home-english-large-text")
    }

    private fun create() {
        CarPlayBackgroundSession.clear()
        activity = Robolectric.buildActivity(DiPlayActivity::class.java).get()
        activity.setTheme(R.style.Theme_Xcertplay)
        render()
    }

    private fun render() {
        activity.javaClass.getDeclaredMethod("render").apply { isAccessible = true }.invoke(activity)
    }

    private fun layout(): ViewGroup {
        val root = activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0) as ViewGroup
        val config = activity.resources.configuration
        val width = dp(config.screenWidthDp)
        val height = dp(config.screenHeightDp)
        root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        root.layout(0, 0, width, height)
        return root
    }

    private fun descendants(view: View): List<View> = listOf(view) +
        if (view is ViewGroup) (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) } else emptyList()

    private fun buttons(root: View) = descendants(root).filterIsInstance<Button>()
    private fun text(root: View, id: Int) = descendants(root).filterIsInstance<TextView>().first { it.text == activity.getString(id) }
    private fun sectionBounds(root: ViewGroup, id: Int) = bounds(root, text(root, id).parent.parent as View)
    private fun button(root: View, id: Int) = buttons(root).first { it.text.toString() == activity.getString(id) }
    private fun dp(value: Int) = (value * activity.resources.displayMetrics.density).toInt()
    private fun bounds(root: ViewGroup, view: View) = Rect(0, 0, view.width, view.height).also {
        root.offsetDescendantRectToMyCoords(view, it)
    }

    private fun preview(root: View, name: String) {
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        val output = File("build/reports/tests/landscape-preview/$name.png")
        output.parentFile.mkdirs()
        output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
