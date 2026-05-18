# Additional Samsung APK scan

This note records an extra static sweep across Samsung apps that looked plausible for
ongoing surfaces but did not add new Now Bar API.

## Artifacts

### Music / SmartThings / Find

```bash
apkeep -a com.sec.android.app.music -d apk-pure artifacts/external-apks
apkeep -a com.samsung.android.oneconnect -d apk-pure artifacts/external-apks
apkeep -a com.samsung.android.app.find -d apk-pure artifacts/external-apks

OUT_DIR=artifacts/local-apk-nowbar-scan/current-samsung-extra-apps \
  scripts/scan_local_apks_for_nowbar.sh \
    artifacts/external-apks/com.sec.android.app.music.apk \
    artifacts/external-apks/com.samsung.android.oneconnect.apk \
    artifacts/external-apks/com.samsung.android.app.find.apk
```

Scan output:

- `artifacts/local-apk-nowbar-scan/current-samsung-extra-apps/`
- Combined scan: `artifacts/local-apk-nowbar-scan/current-samsung-extra-apps/nowbar-scan-all.txt`

### Communication / Notes

```bash
apkeep -a com.samsung.android.app.notes -d apk-pure artifacts/external-apks-extra
apkeep -a com.samsung.android.messaging -d apk-pure artifacts/external-apks-extra
apkeep -a com.samsung.android.dialer -d apk-pure artifacts/external-apks-extra

OUT_DIR=artifacts/local-apk-nowbar-scan/current-samsung-communication-notes-decompiled \
  DECOMPILE=1 \
  JADX_ARGS='--no-res' \
  scripts/scan_local_apks_for_nowbar.sh \
    artifacts/external-apks-extra/com.samsung.android.app.notes.apk \
    artifacts/external-apks-extra/com.samsung.android.messaging.apk \
    artifacts/external-apks-extra/com.samsung.android.dialer.apk
```

Scan output:

- `artifacts/local-apk-nowbar-scan/current-samsung-communication-notes-decompiled/`
- Combined scan: `artifacts/local-apk-nowbar-scan/current-samsung-communication-notes-decompiled/nowbar-scan-all.txt`
- Coverage report: `artifacts/local-apk-nowbar-scan/current-samsung-communication-notes-decompiled/key-coverage/report.md`

## APK metadata

| App | Package | Version | Target SDK | Result |
| --- | --- | --- | ---: | --- |
| Samsung Music | `com.sec.android.app.music` | `6.0` | `21` | no Now Bar / Live Update keys |
| SmartThings | `com.samsung.android.oneconnect` | `1.8.21.28` | `34` | generic notification and AOD permissions only |
| Samsung Find | `com.samsung.android.app.find` | `1.9.01.11` | `36` | generic notification builder usage only |
| Samsung Notes | `com.samsung.android.app.notes` | `4.9.06.8` | `30` | no Now Bar / Live Update keys |
| Samsung Messages | `com.samsung.android.messaging` | `14.2.15.7` | `33` | generic notification/card/chip UI strings only |
| Samsung Dialer | `com.samsung.android.dialer` | `15.1.85` | `34` | generic notification/card/chip UI strings only |

`com.samsung.android.calendar` and `com.samsung.android.app.reminder` were attempted
through the same APKPure source but did not produce APK files in this run.
`com.samsung.android.game.gametools`, `com.samsung.android.app.routines`, and
`com.samsung.android.app.clockface` were also attempted in the communication / notes
sweep but did not produce APK files from the same source.

## Findings

No new API was added from these APKs. The scan found generic notification calls such as
`setDeleteIntent`, `setLargeIcon`, old `ProgressDialog.setProgressStyle`, and resource
names like `indeterminateProgressStyle`. It did not find:

- `android.ongoingActivityNoti.*`
- `android.app.Notification$OngoingActivityStyle`
- `com.samsung.android.support.ongoing_activity`
- `android.permission.POST_PROMOTED_NOTIFICATIONS`
- `setRequestPromotedOngoing`
- `canPostPromotedNotifications`
- `hasPromotableCharacteristics`
- `Notification.MetricStyle`

SmartThings declares Samsung AOD / SystemUI-adjacent permissions such as
`com.samsung.android.app.aodservice.permission.SERVICEBOX_REMOTEVIEWS` and
`com.samsung.systemui.permission.FACE_WIDGET`, but the APK did not expose the Now Bar
ongoing-activity extras or hidden style surface discovered in Samsung Health and Voice
Recorder. Those permissions are therefore documented as adjacent evidence, not as SDK API.

The Notes / Messages / Dialer decompiled sweep produced `observed_keys=0` and
`unknown_observed_keys=0` in the key-coverage report. It also did not expose
`android.ongoingActivityNoti.*`, `android.app.Notification$OngoingActivityStyle`,
`com.samsung.android.support.ongoing_activity`, promoted-notification APIs, or
`Notification.MetricStyle`. No SDK API was added from that sweep.
