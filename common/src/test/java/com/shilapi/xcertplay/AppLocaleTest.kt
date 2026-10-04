package com.shilapi.xcertplay

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import android.view.View
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class AppLocaleTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val manager get() = context.getSystemService(LocaleManager::class.java)

    @Test fun pickerAndSystemSettingsShareTheSamePreference() {
        AppLocale.save(context, AppLocale.ARABIC)
        assertEquals("ar", manager.applicationLocales.toLanguageTags())
        manager.applicationLocales = LocaleList.forLanguageTags("es")
        assertEquals(AppLocale.SPANISH, AppLocale.preference(context))
        assertSame(context, AppLocale.wrap(context))
        AppLocale.save(context, AppLocale.SYSTEM)
        assertTrue(manager.applicationLocales.isEmpty)
        assertEquals(AppLocale.SYSTEM, AppLocale.preference(context))
    }

    @Test fun oldPreferenceMigratesOnceAndCannotOverrideLaterSystemChanges() {
        context.getSharedPreferences("diplay", Context.MODE_PRIVATE).edit()
            .putString("app_language", "ar").commit()
        AppLocale.wrap(context)
        assertEquals("ar", manager.applicationLocales.toLanguageTags())
        manager.applicationLocales = LocaleList.getEmptyLocaleList()
        AppLocale.wrap(context)
        assertEquals(AppLocale.SYSTEM, AppLocale.preference(context))
    }

    @Test fun existingSystemChoiceWinsOverLegacyPreference() {
        context.getSharedPreferences("diplay", Context.MODE_PRIVATE).edit()
            .putString("app_language", "ar").commit()
        manager.applicationLocales = LocaleList.forLanguageTags("zh-CN")
        AppLocale.wrap(context)
        assertEquals(AppLocale.SIMPLIFIED_CHINESE, AppLocale.preference(context))
        assertEquals("zh-CN", manager.applicationLocales.toLanguageTags())
    }

    @Test @Config(sdk = [28, 32])
    fun olderAndroidWrapsArabicAndReturnsToSystemWithoutChangingGlobalResources() {
        val original = context.resources.configuration.locales.toLanguageTags()
        AppLocale.save(context, AppLocale.ARABIC)
        val wrapped = AppLocale.wrap(context)
        assertEquals(Locale("ar"), wrapped.resources.configuration.locales[0])
        assertEquals(View.LAYOUT_DIRECTION_RTL, wrapped.resources.configuration.layoutDirection)
        assertEquals(original, context.resources.configuration.locales.toLanguageTags())
        AppLocale.save(context, AppLocale.SYSTEM)
        assertSame(context, AppLocale.wrap(context))
    }

    @Test @Config(sdk = [30], qualifiers = "w393dp-h800dp-port-xhdpi")
    fun olderAndroidLanguageOverrideFollowsRotationWindowSizeAndFontScale() {
        AppLocale.save(context, AppLocale.SIMPLIFIED_CHINESE)
        val wrapped = AppLocale.wrap(context)
        RuntimeEnvironment.setQualifiers("w800dp-h393dp-land-xhdpi")
        assertEquals(Configuration.ORIENTATION_LANDSCAPE, wrapped.resources.configuration.orientation)
        assertEquals(800, wrapped.resources.configuration.screenWidthDp)
        assertEquals(393, wrapped.resources.configuration.screenHeightDp)
        assertEquals(Locale.SIMPLIFIED_CHINESE, wrapped.resources.configuration.locales[0])
        RuntimeEnvironment.setFontScale(1.3f)
        assertEquals(1.3f, wrapped.resources.configuration.fontScale, .01f)
        RuntimeEnvironment.setQualifiers("w393dp-h800dp-port-xhdpi")
        assertEquals(Configuration.ORIENTATION_PORTRAIT, wrapped.resources.configuration.orientation)
        assertEquals(393, wrapped.resources.configuration.screenWidthDp)
    }
}
