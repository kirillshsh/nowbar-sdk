# Samsung Pay and Personal Data Intelligence Now Bar extract

This note records the local APK evidence gathered on 2026-05-23 from two Samsung
APKs provided outside the repository. It documents observed names, classes, manifest
signals, resources, and notification / RemoteViews shapes only; it does not copy
Samsung implementation code.

## Artifacts

Source APKs:

| App | Package | Version | Target SDK | SHA-256 |
| --- | --- | --- | ---: | --- |
| Samsung Pay | `com.samsung.android.spay` | `6.6.03` | `36` | `be6e365c8c2f30b854578d45cc5cc40a694c9163a114b55d97f0ff7afd49cc47` |
| Personal Data Intelligence | `com.samsung.android.smartsuggestions` | `7.2.30.20` | `36` | `031e5c65b17b91e953fe2dcaa46dce92117fb0b22ee94228d406164356d2c9e0` |

Local scan artifacts were kept outside committed sources:

- Samsung Pay analysis: `/tmp/analysis-agent-spay/`
- Personal Data Intelligence analysis:
  `/Users/kirill/zalupa/.analysis-agent-pdi/pdi-031e5c65/`
- Parent dex/string sweep:
  `artifacts/local-apk-nowbar-scan/current-spay-pdi/`

Samsung Pay contains a malformed embedded asset dex that makes a full smali decode
fail in `apktool`. The reliable path was a resource / manifest decode plus targeted
`jadx` and `dexdump` on the relevant `classes*.dex` files. Personal Data Intelligence
also has decompilation errors in `jadx`, so smali was used for the exact GenUI bridge.

## Samsung Pay

Samsung Pay exposes a real Now Bar / ongoing activity path for travel tickets and
boarding passes. It is implemented as Samsung-specific notification extras on top of
a normal `android.app.Notification.Builder`, not through Android 16 promoted ongoing
notifications.

Manifest signals:

- `android.permission.POST_NOTIFICATIONS`
- `android.permission.FOREGROUND_SERVICE`
- `android.permission.FOREGROUND_SERVICE_DATA_SYNC`
- `android.permission.FOREGROUND_SERVICE_SPECIAL_USE`
- `com.samsung.android.support.ongoing_activity=true`
- receiver action `com.samsung.intent.action.ONGOING_ACTIVITY_SETTING_CHANGED`
- declared `NowBarService`
- `NowBarNotificationBroadcastReceiver`
- no `android.permission.POST_PROMOTED_NOTIFICATIONS`

Main classes and responsibilities:

| Class | Observed role |
| --- | --- |
| `com.samsung.android.spay.pay.card.wltcontainer.nowbar.NowBarActivityNotiUtil` | Writes Samsung ongoing extras and checks `com.samsung.feature.nowbar`. |
| `TravelTicketNowBarUtil` | Builds travel-ticket notifications, writes extras, schedules alarms, posts Now Bar notifications. |
| `BdpNowBarWorkScheduler` | Enqueues unique WorkManager work named `now_bar_worker_$appCardId`. |
| `BdpNowBarWorker` | Reloads boarding-pass state, respects `dismiss_ongoing_noti`, updates and reschedules every minute. |
| `NowBarNotificationBroadcastReceiver` | Handles notification cancellation and persists the dismiss flag. |

Observed Samsung notification extras:

| Key | Existing SDK surface |
| --- | --- |
| `android.showSmallIcon` | `NowBarExtrasKeys.SHOW_SMALL_ICON` / `OngoingExtrasBuilder.setShowSmallIcon(...)` |
| `android.ongoingActivityNoti.style` | `NowBarExtrasKeys.STYLE` |
| `android.ongoingActivityNoti.primaryInfo` | `NowBarExtrasKeys.PRIMARY_INFO` |
| `android.ongoingActivityNoti.secondaryInfo` | `NowBarExtrasKeys.SECONDARY_INFO` |
| `android.ongoingActivityNoti.nowbarPrimaryInfo` | `NowBarExtrasKeys.NOWBAR_PRIMARY_INFO` |
| `android.ongoingActivityNoti.nowbarSecondaryInfo` | `NowBarExtrasKeys.NOWBAR_SECONDARY_INFO` |
| `android.ongoingActivityNoti.chipIcon` | `NowBarExtrasKeys.CHIP_ICON` |
| `android.ongoingActivityNoti.chipExpandedText` | `NowBarExtrasKeys.CHIP_EXPANDED_TEXT` |
| `android.ongoingActivityNoti.chipBgColor` | `NowBarExtrasKeys.CHIP_BG_COLOR` |
| `android.ongoingActivityNoti.progress` | `NowBarExtrasKeys.PROGRESS` |
| `android.ongoingActivityNoti.progressMax` | `NowBarExtrasKeys.PROGRESS_MAX` |
| `android.ongoingActivityNoti.progressSegments.progressColor` | `NowBarExtrasKeys.PROGRESS_COLOR` |

Observed flow details:

- `isSupportOngoingActivity(Context)` checks
  `PackageManager.hasSystemFeature("com.samsung.feature.nowbar")`.
- `enableNowBarSmallIcon(Bundle)` writes `android.showSmallIcon=true`.
- Expanded text uses `primaryInfo` and `secondaryInfo`.
- Now Bar text uses `nowbarPrimaryInfo` and `nowbarSecondaryInfo`.
- The chip uses `chipIcon`, `chipExpandedText`, and `chipBgColor`.
- Progress uses `progress`, `progressMax`, and `progressSegments.progressColor`.
- The notification builder sets title, small icon, color, content/delete intents, and
  group `spay_now_bar_group`, then attaches the extras with `builder.addExtras(...)`.
- The final notification has flag value `8`, which is `FLAG_ONLY_ALERT_ONCE`; this
  is not `FLAG_ONGOING_EVENT`.
- Travel-ticket alarms use action `intent_action_now_bar_notification`, template type
  `1001`, and card id extras.
- A settings-change broadcast reads the extra `ongoingActivitySettingValue`.

Samsung Pay resources also contain travel-oriented Now Bar strings and icons, including
`Arrived at`, `Arriving at`, `Departing at`, `Gate`, `Zone`, `now_bar_bus`,
`now_bar_flight`, `now_bar_location`, and `now_bar_train`. The chip color observed in
the helper is `#ff475fff`.

No first-party usage was found for:

- `setRequestPromotedOngoing`
- `Notification.EXTRA_REQUEST_PROMOTED_ONGOING`
- `FLAG_PROMOTED_ONGOING`
- `canPostPromotedNotifications`
- `android.app.Notification$ProgressStyle`
- `POST_PROMOTED_NOTIFICATIONS`

## Personal Data Intelligence

Personal Data Intelligence does not look like a notification-extras producer for
Now Bar. The relevant path is a Samsung-private GenUI / AppSearch / RemoteViews
pipeline that builds card views and sends them through a binder callback.

Manifest and permission signals:

- `android.permission.POST_NOTIFICATIONS`
- `android.permission.ACCESS_NOTIFICATIONS`
- `android.permission.MANAGE_NOTIFICATIONS`
- `android.permission.FOREGROUND_SERVICE`
- `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE` on
  `PdeNotificationListenerService`
- `android.permission.BIND_REMOTEVIEWS` on RemoteViews services
- `com.samsung.android.sharelive.permission.SHARE_LIVE_UPDATE`
- `com.samsung.systemui.permission.AI_BRIEF`
- `com.samsung.android.smartsuggestions.permission.BRIEF_LIFECYCLE`
- `WidgetNowBarAgent$WidgetNowBarReceiver`

Main bridge:

| Component | Observed role |
| --- | --- |
| `GenUIEventManager.updateViewForCallback(...)` | Calls `IGenUICallback.updateViewWithRemoteViews(type, view, remoteViews)`. |
| `GenUIEventManager.updateView(channel, type, view, remoteViews)` | Public in-app handoff into the GenUI event manager. |
| `TaskGenUIController.updateGenUIData(...)` | Maps task data to the GenUI string and `Map<String, RemoteViews>`. |
| `GenUIUpdateManager` | Registers `DEFAULT_CHANNEL`, `DEFAULT_SECURE_CHANNEL`, and `DEFAULT_SECURE_SUGGESTION_CHANNEL`. |
| `GenUIService` binder registration | Uses `ON_SCREEN_NUDGE_CHANNEL` for on-screen nudges, otherwise `DEFAULT_CHANNEL`. |

AppSearch decorator constants observed in smali:

| Name | Value |
| --- | --- |
| `FLAG_CHIP` | `0x1` |
| `FLAG_NORMAL` | `0x2` |
| `FLAG_EXPAND` | `0x4` |
| `KEY_CHIP_VIEW` | `chipView` |
| `KEY_NORMAL_VIEW` | `normalView` |
| `KEY_EXPAND_VIEW` | `expandView` |
| `KEY_IS_WHITE_WALLPAPER` | `KEY_IS_WHITE_WALLPAPER` |

RemoteViews builders:

- Commute builds `chipView`, `normalView`, and `expandView`.
- Finance builds `normalView` and `expandView`.
- Sports builds `chipView`, `normalView`, and `expandView`.
- Sports also reads `KEY_IS_WHITE_WALLPAPER` to select light / dark colors and icons
  for the Now Bar surface.
- The sports GenUI mapper builds light and dark RemoteViews variants and stores them
  under string keys that end in `_light` and `_dark`.

Now Bar / ongoing resources:

- `common_layout_ongoing_commute_expand`
- `common_layout_ongoing_commute_normal`
- `common_layout_ongoing_finance_chip`
- `common_layout_ongoing_finance_expand`
- `common_layout_ongoing_finance_normal`
- `common_layout_ongoing_sports_chip`
- `common_layout_ongoing_sports_expand`
- `common_layout_ongoing_sports_normal`
- `nowbar_commute_chip_view`
- `nowbar_bg_afternoon`
- `nowbar_bg_evening`
- `nowbar_bg_morning`
- `nowbar_google_sports_pop_up_bg`
- `nowbar_sports_chip_no_logo_bg`
- `nowbar_sports_expand_no_logo_bg`
- `nowbar_sports_normal_no_logo_bg`

Data and analysis classes:

- `NowBarGoogleFinanceRawData`
- `NowBarSportsRawData`
- `NowBarGoogleFinanceEntity`
- `NowBarSportsEntity`
- `NowBarSportsData`
- `NowBarSportsDataV2`
- `NowBarGoogleFinanceDataHandler`
- `NowBarGoogleFinanceDataProvider`
- `NowBarSportsDataHandler`
- `NowBarSportsDataProvider`
- `CollectorNowBarGoogleFinanceRepositoryImpl`
- `CollectorNowBarSportsRepositoryImpl`
- `SportsNowBarSportsAnalyzer`
- `StockNowBarGoogleFinanceAnalyzer`

Notification-related code in PDI appears adjacent, not the primary Now Bar transport:

- Audio Brief creates a normal `NotificationCompat` notification and channel.
- Notification permission copy mentions playback controls for Audio Brief.
- `PdeNotificationListenerService` ingests `StatusBarNotification` data and reads
  `pde_noti_id`.
- No first-party Now Bar `startForeground()` or promoted ongoing notification builder
  path was found.

No first-party usage was found for:

- `setRequestPromotedOngoing`
- `isRequestPromotedOngoing`
- `promoted_ongoing`
- `BubbleMetadata`
- `setBubbleMetadata`
- `android.ongoingActivityNoti.*`

## SDK consequences

Samsung Pay strengthens the existing SDK split:

- Samsung One UI Now Bar path: manifest metadata, `com.samsung.feature.nowbar`, and
  `android.ongoingActivityNoti.*` extras.
- Android 16 Live Updates path: `POST_PROMOTED_NOTIFICATIONS`,
  `setRequestPromotedOngoing`, `ProgressStyle`, and
  `canPostPromotedNotifications`.

Samsung Pay target SDK 36 still uses the Samsung extras path and does not use Android
promoted ongoing APIs. The keys it exposes are already represented by
`NowBarExtrasKeys` and `OngoingExtrasBuilder`, so no new SDK constant was added from
Samsung Pay.

Personal Data Intelligence points to a different private layer: Samsung system services
can receive ready-made `RemoteViews` through GenUI callbacks and AppSearch decorators.
That is useful evidence for interpreting Samsung Google Sports / Finance dumps, but it
is not a public third-party app contract. The SDK should keep dump-style RemoteViews
support separate from public notification-builder APIs and avoid silently treating the
PDI GenUI binder contract as something normal apps can call.

Runtime rendering, package allow-listing, and ranking behavior still require real Samsung
device proof. Static APK evidence can prove names, extras, resources, and call shapes; it
cannot prove that a third-party package will be accepted by SystemUI / AOD.
