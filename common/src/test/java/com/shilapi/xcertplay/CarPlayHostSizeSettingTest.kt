package com.shilapi.xcertplay

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import com.shilapi.xcertplay.airplay.*
import java.util.concurrent.ExecutorService
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedConstruction
import org.mockito.Mockito.*
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class CarPlayHostSizeSettingTest {
    private lateinit var activity: CarPlayHostActivity
    private lateinit var codecLists: MockedConstruction<MediaCodecList>
    private val sizeClass = Class.forName("com.shilapi.xcertplay.CarPlayHostActivity\$DisplaySize")
    private var supportsSize = true
    private var supportsRate = true

    @Before fun setUp() {
        activity = Robolectric.buildActivity(CarPlayHostActivity::class.java).get()
        field("airPlayIdentity", AirPlayIdentity.generate())
        val decoder = mock(MediaCodecInfo::class.java)
        val capabilities = mock(MediaCodecInfo.CodecCapabilities::class.java)
        val video = mock(MediaCodecInfo.VideoCapabilities::class.java)
        `when`(decoder.name).thenReturn("OMX.test.hardware.avc")
        `when`(decoder.supportedTypes).thenReturn(arrayOf(MediaFormat.MIMETYPE_VIDEO_AVC))
        `when`(decoder.isHardwareAccelerated).thenReturn(true)
        `when`(decoder.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC)).thenReturn(capabilities)
        `when`(capabilities.videoCapabilities).thenReturn(video)
        `when`(video.isSizeSupported(anyInt(), anyInt())).thenAnswer { supportsSize }
        `when`(video.areSizeAndRateSupported(anyInt(), anyInt(), anyDouble())).thenAnswer { supportsRate }
        codecLists = mockConstruction(MediaCodecList::class.java) { list, _ ->
            `when`(list.codecInfos).thenReturn(arrayOf(decoder))
        }
    }

    @After fun tearDown() {
        codecLists.close()
        (field("teardownExecutor") as ExecutorService).shutdownNow()
        (field("airPlayCommandExecutor") as ExecutorService).shutdownNow()
    }

    @Test fun savedSizeChangesAffectTheAdvertisedCanvasAfterReload() {
        val expected = listOf(1956 to 940, 2250 to 1080, 2648 to 1270)
        CarPlaySize.entries.forEachIndexed { index, preset ->
            select(preset)
            val config = config()
            assertCanvas(config.main, expected[index])
            val screen = (AirPlayInfoPlist.build(config)["displays"] as List<*>).first() as Map<*, *>
            assertEquals(expected[index].first, screen["widthPixels"])
            assertEquals(expected[index].second, screen["heightPixels"])
            assertTrue(DisplayDiagnosticSnapshot.report(activity).contains("selected=${preset.label}"))
        }
    }

    @Test fun sizeComposesWithResolutionAndPortraitRotation() {
        select(CarPlaySize.SMALL, resolution = 8)
        assertCanvas(config().main, 2118 to 1016)
        assertCanvas(config(width = 1080, height = 2250).main, 1016 to 2118)
    }

    @Test fun changingPhysicalWidthInTheHostAlsoChangesTheCanvas() {
        select(CarPlaySize.MEDIUM)
        field("widthPhysicalMm", CarPlaySize.LARGE.widthMillimeters)
        assertCanvas(config().main, 1956 to 940)
    }

    @Test fun unsupportedSmallCanvasFallsBackWithoutLosingThePreferenceAndCanRetry() {
        select(CarPlaySize.SMALL)
        supportsSize = false
        assertCanvas(config().main, 2250 to 1080)
        val report = DisplayDiagnosticSnapshot.report(activity)
        assertTrue(report.contains("selected=Small percent=85"))
        assertTrue(report.contains("size=Medium percent=100"))
        assertTrue(report.contains("canvas_dimensions_unsupported"))
        assertEquals(CarPlaySize.SMALL.widthMillimeters, AirPlayPersistence.loadWidthPhysicalMm(activity))

        supportsSize = true
        assertCanvas(config().main, 2648 to 1270)
    }

    @Test fun unsupportedFrameRateFallsBackAndReportsTheReason() {
        select(CarPlaySize.SMALL)
        supportsRate = false
        assertCanvas(config().main, 2250 to 1080)
        assertTrue(DisplayDiagnosticSnapshot.report(activity).contains("frame_rate_unsupported"))
    }

    @Test fun fourKLimitFallsBackBeforeQueryingTheDecoder() {
        select(CarPlaySize.SMALL)
        assertCanvas(config(width = 3840, height = 2160).main, 3840 to 2160)
        assertTrue(codecLists.constructed().isEmpty())
        assertTrue(DisplayDiagnosticSnapshot.report(activity).contains("canvas_4k_limit"))
    }

    @Test fun obsoleteScalePreferenceDoesNotOverrideTheVisibleSizeSetting() {
        AirPlayPersistence.saveUiScalePercent(activity, 75)
        select(CarPlaySize.LARGE)
        assertCanvas(config().main, 1956 to 940)
        select(CarPlaySize.MEDIUM)
        assertCanvas(config().main, 2250 to 1080)
        assertTrue(codecLists.constructed().isEmpty())
    }

    private fun select(size: CarPlaySize, resolution: Int = 10) {
        AirPlayPersistence.saveWidthPhysicalMm(activity, size.widthMillimeters)
        AirPlayPersistence.saveDisplayScaleTenths(activity, resolution)
        AirPlayPersistence.saveFps(activity, 60)
        activity.javaClass.getDeclaredMethod("loadPersistedSettings").apply { isAccessible = true }.invoke(activity)
    }

    private fun config(width: Int = 2250, height: Int = 1080): AirPlayConfig {
        val size = sizeClass.getDeclaredConstructor(Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
            .apply { isAccessible = true }.newInstance(width, height)
        return activity.javaClass.getDeclaredMethod("createAirPlayConfig", sizeClass)
            .apply { isAccessible = true }.invoke(activity, size) as AirPlayConfig
    }

    private fun assertCanvas(display: AirPlayDisplayConfig, expected: Pair<Int, Int>) {
        assertEquals(expected.first, display.widthPixels)
        assertEquals(expected.second, display.heightPixels)
    }

    private fun field(name: String): Any? = activity.javaClass.getDeclaredField(name)
        .apply { isAccessible = true }.get(activity)

    private fun field(name: String, value: Any?) {
        activity.javaClass.getDeclaredField(name).apply { isAccessible = true }.set(activity, value)
    }
}
