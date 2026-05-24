# Samsung Google Now Bar dumps

These notes document the notification shape observed in live `dumpsys notification` captures for Google Finance and Google Sports rendered through Samsung AOD / Now Bar.

## Observed notification topology

The Samsung side uses `pkg=com.samsung.android.app.aodservice` and creates a pair of notifications in one group:

| Scenario | Summary id | Child id | Channel | Group key |
| --- | ---: | ---: | --- | --- |
| Google Finance | `3000` | `3115` | `google_finance_nowbar_ongoing_channel` | `google_finance_nowbar_group_key` |
| Google Sports | `1000` | `1123` | `google_sports_nowbar_ongoing_channel` | `google_sports_nowbar_group_key` |

The summary notification has `GROUP_SUMMARY`. The child notification carries `android.template=android.app.Notification$OngoingActivityStyle`, the richer `android.ongoingActivityNoti.*` extras, `nowbarRemoteView`, and `expandedRemoteView`.

## PDI / GenUI correlation

The later Personal Data Intelligence APK pass recorded in
[`samsung-spay-pdi-nowbar-extract.md`](./samsung-spay-pdi-nowbar-extract.md) found a
Samsung-private GenUI / AppSearch / RemoteViews pipeline for sports, finance, and
commute cards. That APK builds `chipView`, `normalView`, and `expandView` RemoteViews
through `AppSearchViewDecoratorApi`, then hands a `Map<String, RemoteViews>` to
`IGenUICallback.updateViewWithRemoteViews(...)`.

This does not prove the full SystemUI / AOD server-side contract, but it is the best
static explanation found so far for how Google Sports / Finance data can become the
dumped Samsung AOD Now Bar RemoteViews payloads. The SDK keeps this evidence in the
dump/inspection lane rather than exposing the private GenUI binder path as public API.

## Keys added from the dumps

### Remote app identity

- `android.ongoingActivityNoti.aodRemoteAppPendingIntent`
- `android.ongoingActivityNoti.aodRemoteAppIcon`
- `android.ongoingActivityNoti.aodRemoteAppName`

### RemoteViews

- `android.ongoingActivityNoti.nowbarRemoteView`
- `android.ongoingActivityNoti.expandedRemoteView`
- `android.ongoingActivityNoti.chipExpandedView`
- `android.ongoingActivityCustomExpandedCardView`
- `android.ongoingActivityExpandedChipView`
- `android.ongoingActivityExpandedNowBarView`
- `android.ongoingActivityCustomCardViewCenterUI`

### OngoingActivity wrapper keys

- `android.ongoingActivityPrimaryInfo`
- `android.ongoingActivitySecondaryInfo`
- `android.ongoingActivityPrimaryAction`
- `android.ongoingActivityChipIcon`
- `android.ongoingActivityChipBackground`
- `android.ongoingActivityCardBackground`
- `android.ongoingActivityActionBgColors`
- `android.ongoingActivityNowBarExpandableType`
- `android.ongoingActivityMoreInfo`
- `android.ongoingActivityCardIcon`

### Chronometer state

- `android.ongoingActivityChronometerFormat`
- `android.ongoingActivityChronometerCountdown`
- `android.ongoingActivityChronometerBase`
- `android.ongoingActivityChronometerSpeed`
- `android.ongoingActivityChronometerStart`

### PDE telemetry keys

The Finance dump also contains Samsung PDE counters such as `pde_enqueued_time_ms`, `pde_noti_id`, and `pde_first_shown_time_ms`. They are exposed as constants for dump parity, but normal apps should not need to set them.

## Usage pattern

```kotlin
val extras = SamsungOngoingActivityDumpExtras.build(
    remoteApp = SamsungRemoteAppConfig(
        name = "Google Finance",
        icon = appIcon,
        pendingIntent = openAppIntent
    ),
    text = SamsungOngoingActivityText(
        primaryInfo = "AAPL",
        secondaryInfo = "189.98 · +1.2%",
        nowBarPrimaryInfo = "AAPL 189.98",
        nowBarSecondaryInfo = "+1.2% today",
        notificationPrimaryInfo = "AAPL",
        notificationSecondaryInfo = "189.98 · +1.2%"
    ),
    views = SamsungOngoingActivityViews(
        nowBarRemoteView = nowBarView,
        expandedRemoteView = expandedView
    ),
    visuals = SamsungOngoingActivityVisuals(
        chipIcon = appIcon,
        chipBackgroundColor = 0xff4285f4.toInt(),
        nowBarExpandableType = 0
    ),
    progress = SamsungOngoingActivityProgress(
        current = 65,
        max = 100,
        color = 0xff4285f4.toInt()
    ),
    substName = "Google Finance"
)

val notification = NotificationCompat.Builder(context, CHANNEL_ID)
    .setSmallIcon(R.drawable.ic_stat_finance)
    .setContentTitle("Google Finance")
    .setContentText("AAPL 189.98 · +1.2%")
    .setOngoing(true)
    .setOnlyAlertOnce(true)
    .setGroup(SamsungNowBarGroupSummaryBuilder.GOOGLE_FINANCE_GROUP_KEY)
    .addExtras(extras)
    .build()
```

For the dump-style two-notification topology, post a summary notification built by `SamsungNowBarGroupSummaryBuilder` and then post the child notification with the same `groupKey`.
If you are using the normal SDK manager/session path instead of the dump helper,
`NowBarConfig(aodRemoteApp = SamsungRemoteAppConfig(...))` writes the same AOD app
name, icon, and tap `PendingIntent` keys into every Samsung-extras notification.
For manual bundles, `OngoingExtrasBuilder.setAodRemoteApp(SamsungRemoteAppConfig(...))`
exposes the same key set directly.
`NowBarNotificationEvidence.inspect(...)` reports this shape through `groupSummary`,
`nativeOngoingActivityTemplate`, `aodRemoteAppName`, structured `aodRemoteApp`,
structured `samsungViews`, per-RemoteViews booleans,
`samsungRemoteViewCount`, `samsungDumpShow`, `samsungReducedImages`,
`samsungPrimaryAction`, `samsungNowBarExpandableType`, visual icon flags,
`samsungActionBackgroundColorCount`, optional `samsungText`, optional `samsungVisuals`,
optional `samsungChronometerState`, and optional `pdeState`, which makes dump-style
smoke artifacts easier to verify without relying only on broad key counts.
Dump-style extras can also carry a `SamsungOngoingActivityProgress` payload; the
inspector exposes it through the same structured `samsungNowBar.progress` state as the
first-class Samsung extras path.
For AndroidX ProgressStyle cards the same inspector also reports compat payload counts
and icon flags from `android.progressSegments`, `android.progressPoints`,
`android.progressTrackerIcon`, `android.progressStartIcon`, `android.progressEndIcon`,
and `android.styledByProgress`.
