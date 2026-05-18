@file:Suppress("DEPRECATION")

package com.nowbar.api

import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import com.nowbar.api.notification.LiveUpdateDiagnostics
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBuild

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NowBarDiagnosticsTest {

    @AfterTest
    fun resetBuild() {
        ShadowBuild.reset()
    }

    @Test
    fun `inspect reports Samsung manufacturer fallback path`() {
        val context = RuntimeEnvironment.getApplication()
        shadowOf(context.packageManager).setSystemFeature(FeatureDetector.FEATURE_SAMSUNG_NOWBAR, false)
        ShadowBuild.setManufacturer("Samsung")
        ShadowBuild.setBrand("samsung")
        ShadowBuild.setModel("Galaxy")

        val report = NowBarDiagnostics.inspect(context)

        assertEquals(NowBarPlatform.SAMSUNG, report.platform)
        assertTrue(report.nativeSurfaceSupported)
        assertTrue(report.samsungDevice)
        assertFalse(report.samsungNowBarFeature)
        assertTrue(report.canApplySamsungExtras)
        assertFalse(report.android16LiveUpdatesSupported)
        assertTrue(report.blockingReasons.isEmpty())
        assertTrue(report.toDisplayString().contains("Samsung extras path: true"))
    }

    @Test
    fun `inspect reports no native surface on non Samsung Android 15`() {
        val context = RuntimeEnvironment.getApplication()
        shadowOf(context.packageManager).setSystemFeature(FeatureDetector.FEATURE_SAMSUNG_NOWBAR, false)
        ShadowBuild.setManufacturer("Google")
        ShadowBuild.setBrand("google")

        val report = NowBarDiagnostics.inspect(context)

        assertEquals(NowBarPlatform.NONE, report.platform)
        assertFalse(report.nativeSurfaceSupported)
        assertFalse(report.canApplySamsungExtras)
        assertEquals(listOf("no-native-nowbar-or-live-updates-surface"), report.blockingReasons)
    }

    @Test
    fun `inspect includes promoted settings availability`() {
        val context = RuntimeEnvironment.getApplication()
        val intent = LiveUpdateDiagnostics.createPromotionSettingsIntent(context)
        val resolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = "com.android.settings"
                name = "PromotedNotificationSettingsActivity"
            }
        }
        shadowOf(context.packageManager).addResolveInfoForIntent(intent, resolveInfo)

        val report = NowBarDiagnostics.inspect(context)

        assertTrue(report.promotionSettingsAvailable)
    }

    @Test
    fun `inspect includes Now Bar settings shortcut availability`() {
        val context = RuntimeEnvironment.getApplication()
        val intent = NowBarDiagnostics.createNowBarSettingsIntent(context)
        val resolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = "com.android.settings"
                name = "LockScreenSettingsActivity"
            }
        }
        shadowOf(context.packageManager).addResolveInfoForIntent(intent, resolveInfo)

        val report = NowBarDiagnostics.inspect(context)

        assertTrue(report.nowBarSettingsShortcutAvailable)
        assertTrue(NowBarDiagnostics.canOpenNowBarSettings(context))
        assertEquals(
            NowBarDiagnostics.ACTION_LOCK_SCREEN_SETTINGS,
            NowBarDiagnostics.resolveNowBarSettingsIntent(context)?.action
        )
        assertTrue(report.toDisplayString().contains("Now Bar settings shortcut: true"))
    }

    @Test
    fun `recommended settings intent prefers Now Bar settings before promoted settings`() {
        val context = RuntimeEnvironment.getApplication()
        val nowBarIntent = NowBarDiagnostics.createNowBarSettingsIntent(context)
        val promotionIntent = LiveUpdateDiagnostics.createPromotionSettingsIntent(context)
        val resolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = "com.android.settings"
                name = "SettingsActivity"
            }
        }
        shadowOf(context.packageManager).addResolveInfoForIntent(promotionIntent, resolveInfo)
        shadowOf(context.packageManager).addResolveInfoForIntent(nowBarIntent, resolveInfo)

        val resolved = NowBarDiagnostics.resolveRecommendedSettingsIntent(context)

        assertEquals(nowBarIntent.action, resolved?.action)
    }

    @Test
    fun `recommended settings intent falls back to promoted notification settings`() {
        val context = RuntimeEnvironment.getApplication()
        val promotionIntent = LiveUpdateDiagnostics.createPromotionSettingsIntent(context)
        val resolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = "com.android.settings"
                name = "PromotedNotificationSettingsActivity"
            }
        }
        shadowOf(context.packageManager).addResolveInfoForIntent(promotionIntent, resolveInfo)

        val resolved = NowBarDiagnostics.resolveRecommendedSettingsIntent(context)

        assertEquals(promotionIntent.action, resolved?.action)
    }
}
