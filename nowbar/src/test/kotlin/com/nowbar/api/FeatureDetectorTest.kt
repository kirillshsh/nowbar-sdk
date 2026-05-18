package com.nowbar.api

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
class FeatureDetectorTest {

    @AfterTest
    fun resetBuild() {
        ShadowBuild.reset()
    }

    @Test
    fun `samsung manufacturer enables extras path without nowbar feature flag`() {
        val context = RuntimeEnvironment.getApplication()
        shadowOf(context.packageManager).setSystemFeature(FeatureDetector.FEATURE_SAMSUNG_NOWBAR, false)
        ShadowBuild.setManufacturer("Samsung")
        ShadowBuild.setBrand("samsung")

        assertFalse(FeatureDetector.isSamsungNowBarSupported(context))
        assertTrue(FeatureDetector.canApplySamsungNowBarExtras(context))
        assertTrue(FeatureDetector.isNativeSurfaceSupported(context))
        assertEquals(NowBarPlatform.SAMSUNG, FeatureDetector.getSupportedPlatform(context))
    }

    @Test
    fun `non samsung android 15 device needs explicit native feature`() {
        val context = RuntimeEnvironment.getApplication()
        shadowOf(context.packageManager).setSystemFeature(FeatureDetector.FEATURE_SAMSUNG_NOWBAR, false)
        ShadowBuild.setManufacturer("Google")
        ShadowBuild.setBrand("google")

        assertFalse(FeatureDetector.canApplySamsungNowBarExtras(context))
        assertFalse(FeatureDetector.isNativeSurfaceSupported(context))
        assertEquals(NowBarPlatform.NONE, FeatureDetector.getSupportedPlatform(context))
    }
}
