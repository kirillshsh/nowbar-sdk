# Samsung Health Now Bar extract

This note records the local APK evidence used for the Samsung-specific Now Bar
compatibility layer. It intentionally documents observed keys and behavior only;
it does not copy Samsung implementation code.

## Artifact

Downloaded locally with:

```bash
apkeep -a com.sec.android.app.shealth -d apk-pure artifacts/external-apks
```

Scanned and decompiled with:

```bash
scripts/scan_local_apks_for_nowbar.sh artifacts/external-apks/com.sec.android.app.shealth.apk
```

Current local artifact:

- APK: `artifacts/external-apks/com.sec.android.app.shealth.apk`
- Scan output: `artifacts/local-apk-nowbar-scan/20260518-051355/`
- Package: `com.sec.android.app.shealth`
- Version: `6.32.0.001` (`6320001`)
- min SDK: `29`
- target SDK: `36`

The APK is intentionally under ignored `artifacts/` storage.

## Manifest signals

Observed through `apkanalyzer manifest print`:

- `android.permission.POST_NOTIFICATIONS`
- `android.permission.FOREGROUND_SERVICE`
- `android.permission.FOREGROUND_SERVICE_HEALTH`
- `android.permission.FOREGROUND_SERVICE_LOCATION`
- `android.permission.FOREGROUND_SERVICE_SPECIAL_USE`
- `com.samsung.android.support.ongoing_activity`
- sport live-tracker services with foreground service types

`POST_PROMOTED_NOTIFICATIONS` was not present in this Samsung Health APK. That
matches the split in this SDK: Samsung One UI 7-style extras are handled by the
Samsung extras path, while Android 16+ promoted Live Updates are handled through
public Android APIs.

## Now Bar strings

The decompiled `SportOngoingNotificationHelper` confirms these Samsung extras:

| Key | SDK surface |
| --- | --- |
| `android.ongoingActivityNoti.style` | `OngoingExtrasBuilder.KEY_STYLE` |
| `android.ongoingActivityNoti.actionType` | `OngoingExtrasBuilder.KEY_ACTION_TYPE` |
| `android.ongoingActivityNoti.chipIcon` | `ChipConfig.icon` |
| `android.ongoingActivityNoti.chipBgColor` | `ChipConfig.backgroundColor` |
| `android.ongoingActivityNoti.chipExpandedText` | `ChipConfig.expandedText` |
| `android.ongoingActivityNoti.primaryInfo` | `OngoingExtrasBuilder.setPrimaryInfo(...)` |
| `android.ongoingActivityNoti.secondaryInfo` | `OngoingExtrasBuilder.setSecondaryInfo(...)` |
| `android.ongoingActivityNoti.nowbarSecondaryInfo` | `OngoingExtrasBuilder.setNowBarSecondaryInfo(...)` |
| `android.ongoingActivityNoti.progress` | `OngoingExtrasBuilder.setProgress(...)` |
| `android.ongoingActivityNoti.progressMax` | `OngoingExtrasBuilder.setProgress(...)` |
| `android.ongoingActivityNoti.progressSegments` | `OngoingExtrasBuilder.setProgressSegments(...)` |
| `android.ongoingActivityNoti.progressSegments.progressColor` | `OngoingExtrasBuilder.setProgressColor(...)` |
| `android.ongoingActivityNoti.progressSegments.segmentColor` | `ProgressSegment.color` |
| `android.ongoingActivityNoti.progressSegments.segmentStart` | `ProgressSegment.startPosition` |
| `android.ongoingActivityNoti.progressSegments.icon` | `ProgressSegment.icon` and `setProgressSegmentIcon(...)` |
| `android.showSmallIcon` | `OngoingExtrasBuilder.setShowSmallIcon(...)` |

Samsung Health uses `channel.03.sport` for ongoing workout notifications and
checks `com.samsung.feature.nowbar` before switching Now Bar-specific behavior.

## API consequences

- `ProgressSegment` validates `startPosition` as `0.0..1.0`.
- `OngoingExtrasBuilder.setProgressSegments(...)` sorts segments by start position
  before writing the `Parcelable[]`, matching Samsung Health's observed order.
- The public Android Live Updates path stays separate from Samsung proprietary
  extras so third-party apps can work on Android 16+ without relying on Samsung
  private keys.
