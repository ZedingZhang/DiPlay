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
