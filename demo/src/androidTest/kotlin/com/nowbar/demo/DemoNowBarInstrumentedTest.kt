package com.nowbar.demo

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nowbar.api.NowBarDiagnostics
import com.nowbar.api.notification.ActionSemantic
import com.nowbar.api.notification.LiveUpdateDiagnostics
import com.nowbar.api.notification.LiveUpdateEligibilityReport
import com.nowbar.api.notification.NowBarEvidencePath
import com.nowbar.api.notification.NowBarNotificationEvidence
import com.nowbar.api.notification.NowBarNotificationEvidenceReport
import com.nowbar.api.notification.OngoingExtrasBuilder
import com.nowbar.api.notification.SamsungNowBarGroupSummaryBuilder
import com.nowbar.api.notification.SamsungOngoingActivityDumpKeys
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DemoNowBarInstrumentedTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    @Before
    fun setUp() {
        grantPostNotifications()
        notificationManager.cancelAll()
    }

    @After
    fun tearDown() {
        launchAction(DemoNowBarService.ACTION_STOP)
        notificationManager.cancelAll()
    }

    @Test
    fun allCardActionsPostAnOngoingNotificationWithNativeSurfaceHints() {
        val actions = listOf(
            DemoNowBarService.ACTION_SHOW_TIMER,
            DemoNowBarService.ACTION_SHOW_WORKOUT,
            DemoNowBarService.ACTION_SHOW_NAVIGATION,
            DemoNowBarService.ACTION_SHOW_DELIVERY,
            DemoNowBarService.ACTION_SHOW_METRICS,
            DemoNowBarService.ACTION_SHOW_MEDIA,
            DemoNowBarService.ACTION_SHOW_CALL,
            DemoNowBarService.ACTION_SHOW_CALL_SCREENING,
            DemoNowBarService.ACTION_SHOW_BIG_TEXT,
            DemoNowBarService.ACTION_SHOW_CUSTOM_ACTIONS,
            DemoNowBarService.ACTION_SHOW_NATIVE_STYLE,
            DemoNowBarService.ACTION_PAUSE,
            DemoNowBarService.ACTION_RESUME,
            DemoNowBarService.ACTION_NEXT
        )

        actions.forEach { action ->
            launchAction(action)
            val notification = waitForNotification(DemoNowBarService.NOTIFICATION_ID)
            val capability = NowBarDiagnostics.inspect(context)

            assertTrue(
                "$action should post an ongoing notification",
                notification.flags and Notification.FLAG_ONGOING_EVENT != 0
            )
            assertFalse(
                "$action should have a content title",
                notification.extras.getCharSequence(Notification.EXTRA_TITLE).isNullOrBlank()
            )

            val report = LiveUpdateDiagnostics.inspect(context, notification)
            val evidence = NowBarNotificationEvidence.inspect(notification)

            assertTrue("$action should have a content title", report.hasContentTitle)
            assertTrue("$action should be ongoing", report.ongoing)
            assertTrue("$action should expose a content intent", report.hasContentIntent)
            assertTrue("$action should expose a delete intent", report.hasDeleteIntent)
            assertFalse("$action should not be group summary", report.groupSummary)
            assertFalse("$action should not be colorized", report.colorized)
            assertTrue("$action should be ongoing in evidence report", evidence.ongoing)
            assertTrue("$action should expose content intent evidence", evidence.hasContentIntent)
            assertTrue(
                "$action should expose content intent evidence path",
                NowBarEvidencePath.ANDROID_CONTENT_INTENT in evidence.evidencePaths
            )
            assertTrue("$action should expose delete intent evidence", evidence.hasDeleteIntent)
            assertTrue(
                "$action should expose delete intent evidence path",
                NowBarEvidencePath.ANDROID_DELETE_INTENT in evidence.evidencePaths
            )
            assertTrue("$action should expose content title evidence", evidence.hasContentTitle)
            assertFalse("$action should not expose custom content views", evidence.hasCustomContentView)
            assertFalse("$action should not be colorized in evidence report", evidence.colorized)
            assertFalse("$action should not be summary in evidence report", evidence.groupSummary)

            if (LiveUpdateDiagnostics.isSupported()) {
                assertTrue("$action should request promoted ongoing", report.requestPromotedOngoing)
                assertTrue(
                    "$action should expose promoted ongoing evidence paths",
                    NowBarEvidencePath.ANDROID_PROMOTED_ONGOING in evidence.evidencePaths
                )
            } else if (capability.samsungPathAvailable && action != DemoNowBarService.ACTION_SHOW_NATIVE_STYLE) {
                assertEquals(
                    "$action should carry Samsung Now Bar extras",
                    OngoingExtrasBuilder.STYLE_BOTH,
                    notification.extras.getInt(OngoingExtrasBuilder.KEY_STYLE)
                )
                assertTrue(
                    "$action should expose Samsung extras evidence path: $evidence",
                    NowBarEvidencePath.SAMSUNG_EXTRAS in evidence.evidencePaths
                )
            }

            if (capability.samsungPathAvailable && action != DemoNowBarService.ACTION_SHOW_NATIVE_STYLE) {
                assertTrue("$action should expose AOD remote-app identity", evidence.hasAodRemoteApp)
                assertEquals("NowBar SDK Demo", evidence.aodRemoteAppName)
                assertTrue(
                    "$action should expose AOD remote-app evidence path",
                    NowBarEvidencePath.SAMSUNG_AOD_REMOTE_APP in evidence.evidencePaths
                )
            }

            if (capability.nativeSurfaceSupported || evidence.hasAndroidLiveUpdateEvidence) {
                assertTrue("$action should be Now Bar-compatible: $evidence", evidence.likelyNowBarCompatible)
                assertTrue("$action should not miss core hints: ${evidence.missingCoreHints}", evidence.missingCoreHints.isEmpty())
            }

            if (action == DemoNowBarService.ACTION_SHOW_DELIVERY || action == DemoNowBarService.ACTION_SHOW_METRICS) {
                assertFalse("$action should expose Live Update header subtext", report.subText.isNullOrBlank())
                assertEquals(report.subText, evidence.subText)
                assertTrue(
                    "$action should expose subtext evidence path",
                    NowBarEvidencePath.ANDROID_SUB_TEXT in evidence.evidencePaths
                )
            }

            expectedActionMetadata(action)?.let { expected ->
                assertActionMetadata(action, report, evidence, expected)
            }
        }
    }

    @Test
    fun dumpStyleActionPostsSummaryAndChildWithSamsungDumpExtras() {
        launchAction(DemoNowBarService.ACTION_SHOW_DUMP)

        val summary = waitForNotification(SamsungNowBarGroupSummaryBuilder.GOOGLE_SPORTS_SUMMARY_ID)
        val child = waitForNotification(SamsungNowBarGroupSummaryBuilder.GOOGLE_SPORTS_CHILD_ID)

        assertTrue(summary.flags and Notification.FLAG_GROUP_SUMMARY != 0)
        assertEquals(
            SamsungNowBarGroupSummaryBuilder.GOOGLE_SPORTS_GROUP_KEY,
            child.group
        )
        assertEquals(
            "NowBar SDK Demo",
            child.extras.getCharSequence(SamsungOngoingActivityDumpKeys.AOD_REMOTE_APP_NAME)
        )
        assertEquals(
            SamsungOngoingActivityDumpKeys.TEMPLATE_ONGOING_ACTIVITY_STYLE,
            child.extras.getString(SamsungOngoingActivityDumpKeys.TEMPLATE)
        )

        val childEvidence = NowBarNotificationEvidence.inspect(child)
        assertTrue(childEvidence.likelyNowBarCompatible)
        assertEquals("NowBar SDK Demo", childEvidence.aodRemoteAppName)
        assertTrue(childEvidence.hasNowBarRemoteView)
        assertTrue(childEvidence.hasExpandedRemoteView)
        assertTrue(childEvidence.samsungRemoteViewCount >= 2)
        assertEquals(true, childEvidence.samsungDumpShow)
        assertEquals(true, childEvidence.samsungReducedImages)
        assertEquals(0, childEvidence.samsungPrimaryAction)
        assertEquals(0, childEvidence.samsungNowBarExpandableType)
        assertEquals(2, childEvidence.samsungActionBackgroundColorCount)
        assertTrue(childEvidence.hasOngoingActivityChipIcon)
        assertTrue(childEvidence.hasOngoingActivityBadge)
        assertTrue(childEvidence.hasOngoingActivityCardIcon)
        assertTrue(childEvidence.hasContentTitle)
        assertTrue(childEvidence.hasContentIntent)
        assertTrue(childEvidence.hasDeleteIntent)
        assertFalse(childEvidence.hasCustomContentView)
        assertFalse(childEvidence.colorized)
        assertTrue(NowBarEvidencePath.SAMSUNG_DUMP_EXTRAS in childEvidence.evidencePaths)
        assertTrue(NowBarEvidencePath.SAMSUNG_NATIVE_TEMPLATE in childEvidence.evidencePaths)
        assertTrue(NowBarEvidencePath.SAMSUNG_AOD_REMOTE_APP in childEvidence.evidencePaths)
        assertTrue(NowBarEvidencePath.SAMSUNG_REMOTE_VIEWS in childEvidence.evidencePaths)
        assertTrue(NowBarEvidencePath.ANDROID_CONTENT_INTENT in childEvidence.evidencePaths)
        assertTrue(NowBarEvidencePath.ANDROID_DELETE_INTENT in childEvidence.evidencePaths)
    }

    @Test
    fun nativeStyleActionPostsInspectableNotification() {
        launchAction(DemoNowBarService.ACTION_SHOW_NATIVE_STYLE)

        val notification = waitForNotification(DemoNowBarService.NOTIFICATION_ID)
        val report = LiveUpdateDiagnostics.inspect(context, notification)
        val evidence = NowBarNotificationEvidence.inspect(notification)

        assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertTrue(report.requestPromotedOngoing)
        assertTrue(evidence.requestPromotedOngoing)
        assertTrue(evidence.likelyNowBarCompatible)
        assertTrue(NowBarEvidencePath.ANDROID_PROMOTED_ONGOING in evidence.evidencePaths)
        assertEquals(
            com.nowbar.api.notification.SamsungOngoingActivityStyleBuilder.isAvailable(),
            notification.extras.getBoolean(DemoNowBarService.EXTRA_NATIVE_STYLE_AVAILABLE)
        )
    }

    @Test
    fun customActionsExposeUnpinAndUnpinDemotesToStandardNotification() {
        launchAction(DemoNowBarService.ACTION_SHOW_CUSTOM_ACTIONS)
        val pinnedNotification = waitForNotification(DemoNowBarService.NOTIFICATION_ID)
        val pinnedReport = LiveUpdateDiagnostics.inspect(context, pinnedNotification)
        val pinnedEvidence = NowBarNotificationEvidence.inspect(pinnedNotification)
        val expectedMetadata = expectedActionMetadata(DemoNowBarService.ACTION_SHOW_CUSTOM_ACTIONS)
            ?: error("Missing custom-action metadata expectation")

        assertEquals(
            listOf("Pause", "Unpin", "Stop"),
            pinnedNotification.actions.map { it.title.toString() }
        )
        assertActionMetadata(
            DemoNowBarService.ACTION_SHOW_CUSTOM_ACTIONS,
            pinnedReport,
            pinnedEvidence,
            expectedMetadata
        )

        launchAction(DemoNowBarService.ACTION_UNPIN)
        val unpinnedNotification = waitForNotification(DemoNowBarService.NOTIFICATION_ID)
        val report = LiveUpdateDiagnostics.inspect(context, unpinnedNotification)
        val evidence = NowBarNotificationEvidence.inspect(unpinnedNotification)

        assertTrue(report.ongoing)
        assertFalse(report.requestPromotedOngoing)
        assertFalse(evidence.requestPromotedOngoing)
        assertFalse(evidence.hasSamsungNowBarEvidence)
        assertFalse(evidence.hasAndroidLiveUpdateEvidence)
        assertFalse(evidence.likelyNowBarCompatible)
        assertTrue(evidence.hasContentTitle)
        assertTrue(evidence.hasContentIntent)
        assertTrue(evidence.hasDeleteIntent)
        assertActionMetadata(DemoNowBarService.ACTION_UNPIN, report, evidence, expectedMetadata)
    }

    @Test
    fun capabilityReportMatchesInstalledDemoAndRuntimeDevice() {
        val report = NowBarDiagnostics.inspect(context)

        assertEquals(Build.VERSION.SDK_INT, report.sdkInt)
        assertEquals(Build.MANUFACTURER, report.manufacturer)
        assertEquals(Build.BRAND, report.brand)
        assertEquals(Build.MODEL, report.model)
        assertEquals(
            LiveUpdateDiagnostics.declaresPostPromotedNotifications(context),
            report.declaresPostPromotedNotifications
        )
        assertTrue(report.declaresPostPromotedNotifications)
    }

    private fun grantPostNotifications() {
        if (Build.VERSION.SDK_INT >= 33) {
            shell("pm grant ${context.packageName} ${Manifest.permission.POST_NOTIFICATIONS}")
        }
    }

    private fun launchAction(action: String) {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(action)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        context.startActivity(intent)
    }

    private fun waitForNotification(id: Int): Notification {
        repeat(30) {
            notificationManager.activeNotifications
                .firstOrNull { it.id == id }
                ?.let { return it.notification }
            Thread.sleep(250)
        }

        val activeIds = notificationManager.activeNotifications.joinToString { it.id.toString() }
        throw AssertionError("Notification $id was not posted. Active ids: [$activeIds]")
    }

    private fun expectedActionMetadata(action: String): Pair<List<String>, List<ActionSemantic>>? =
        when (action) {
            DemoNowBarService.ACTION_SHOW_DELIVERY -> Pair(
                listOf("track", "tip", "stop"),
                listOf(ActionSemantic.CUSTOM, ActionSemantic.CUSTOM, ActionSemantic.STOP)
            )
            DemoNowBarService.ACTION_SHOW_METRICS,
            DemoNowBarService.ACTION_SHOW_BIG_TEXT -> Pair(
                listOf("open"),
                listOf(ActionSemantic.CUSTOM)
            )
            DemoNowBarService.ACTION_SHOW_CUSTOM_ACTIONS,
            DemoNowBarService.ACTION_PAUSE,
            DemoNowBarService.ACTION_RESUME,
            DemoNowBarService.ACTION_NEXT -> Pair(
                listOf("pause", "unpin", "stop"),
                listOf(ActionSemantic.PAUSE, ActionSemantic.UNPIN, ActionSemantic.STOP)
            )
            else -> null
        }

    private fun assertActionMetadata(
        action: String,
        report: LiveUpdateEligibilityReport,
        evidence: NowBarNotificationEvidenceReport,
        expected: Pair<List<String>, List<ActionSemantic>>
    ) {
        val (ids, semantics) = expected
        assertEquals("$action should expose action ids in Live Update report", ids, report.androidActionIds)
        assertEquals("$action should expose action semantics in Live Update report", semantics, report.androidActionSemantics)
        assertEquals("$action should expose action ids in evidence report", ids, evidence.androidActionIds)
        assertEquals("$action should expose action semantics in evidence report", semantics, evidence.androidActionSemantics)
    }

    private fun shell(command: String) {
        val output: ParcelFileDescriptor = instrumentation.uiAutomation.executeShellCommand(command)
        ParcelFileDescriptor.AutoCloseInputStream(output).use { it.readBytes() }
    }
}
