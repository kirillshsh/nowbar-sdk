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

Tooling used during this pass:

- `apkanalyzer` for manifest metadata.
- `apktool` for manifest, resources, and smali where possible.
- `jadx 1.5.5` for targeted Java decompilation.
- `/opt/homebrew/share/android-commandlinetools/build-tools/37.0.0/dexdump` for
  dex-only checks when full decompilation was unreliable.
- `rg` / raw string scans for positive and negative API hits.

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

Exact Samsung Pay source index:

| Evidence | Location |
| --- | --- |
| `enableNowBarSmallIcon(Bundle)` writes `android.showSmallIcon=true` | `NowBarActivityNotiUtil.java:52-55` |
| `isSupportOngoingActivity(Context)` checks `com.samsung.feature.nowbar` | `NowBarActivityNotiUtil.java:57-60` |
| expanded notification text writes `primaryInfo` / `secondaryInfo` | `NowBarActivityNotiUtil.java:62-68` |
| chip writes `chipIcon`, `chipExpandedText`, `chipBgColor` | `NowBarActivityNotiUtil.java:70-75` |
| Now Bar text writes `nowbarPrimaryInfo` / `nowbarSecondaryInfo` | `NowBarActivityNotiUtil.java:77-84` |
| progress writes `progress` / `progressMax` | `NowBarActivityNotiUtil.java:86-90` |
| builder uses `Notification.Builder(context, channelId)` and group `spay_now_bar_group` | `TravelTicketNowBarUtil.java:131-141` |
| before-boarding notification writes style, chip, expanded text, progress, color | `TravelTicketNowBarUtil.java:317-342` |
| on-boarding notification writes the same core extras | `TravelTicketNowBarUtil.java:344-368` |
| in-progress notification adds location icon, segment bundle, and progress payload | `TravelTicketNowBarUtil.java:381-423` |
| final post path calls `builder.addExtras(...)`, `build()`, `flags=8`, `notify(...)` | `TravelTicketNowBarUtil.java:426-435` |
| alarm setup gates on time, ticket support, and Now Bar support | `TravelTicketNowBarUtil.java:557-575` |
| exact alarm action/extras are written | `TravelTicketNowBarUtil.java:594-600` |
| alarm receiver entry starts worker update for template `1001` | `TravelTicketNowBarUtil.java:606-617` |
| WorkManager input key `appCardId`, tag `now_bar_worker_`, unique name `now_bar_worker_$appCardId` | `BdpNowBarWorkScheduler.java:23-35` |
| worker reloads boarding pass, blocks dismissed cards, updates, and schedules the next minute tick | `BdpNowBarWorker.java:49-109` |
| delete receiver persists dismissal and logs `now_bar/travel_ticket/delete` | `NowBarNotificationBroadcastReceiver.java:41-55` |
| DAO reads `dismiss_ongoing_noti` | `BoardingPassDAO.java:105-106` |
| DAO writes `dismiss_ongoing_noti` | `BoardingPassDAO.java:219-220` |
| settings receiver reads `ongoingActivitySettingValue` | `SpayBroadcastReceiver.java:254-259` |

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

Exact Samsung Pay manifest / resource index:

| Evidence | Location |
| --- | --- |
| `POST_NOTIFICATIONS` and foreground-service permissions | `AndroidManifest.xml:192-193` |
| `com.samsung.android.support.ongoing_activity=true` | `AndroidManifest.xml:541` |
| settings change action `com.samsung.intent.action.ONGOING_ACTIVITY_SETTING_CHANGED` | `AndroidManifest.xml:686` |
| declared `NowBarService` | `AndroidManifest.xml:926` |
| `NowBarNotificationBroadcastReceiver`, `exported=false`, action `notification_cancelled` | `AndroidManifest.xml:927-930` |
| travel strings: arrive/arriving/departing/gate/zone | `res/values/strings.xml:15602-15611` |
| countdown plurals for arriving/departing/gate-closes | `res/values/plurals.xml:1569-1616` |
| Now Bar drawable ids: bus/flight/train/front/location | `res/values/public.xml:25193-25199` |
| chip/progress color `color_475fff=#ff475fff` | `res/values/colors.xml:1263` |

Dex-only string and class placement:

- `classes17.dex`: main Now Bar classes, helpers, extras, and feature strings.
- `classes15.dex`: `SpayBroadcastReceiver`, `ongoingActivitySettingValue`, and
  boarding-pass reschedule references.
- `classes8.dex` / `classes15.dex`: extra references to `TravelTicketNowBarUtil`.
- Exact string hits include `showNowBarNotification`, `setNowBarAlarm`,
  `rescheduleNowBarAlarm`, `enableNowBarSmallIcon`, `#nowbar summary notification size =`,
  and `com.samsung.feature.nowbar`.

Adjacent Samsung Pay findings that were not promoted into SDK API:

- Generic `BubbleMetadata`, `NotificationCompat`, and `RemoteViews` symbols exist through
  AndroidX / library code, but no Samsung Pay Now Bar flow calls them directly.
- `com.dejamobile.sdk.ugap.common.entrypoint.ServiceEntryPoint` declares
  `android.permission.BIND_REMOTEVIEWS`, but it is not the travel-ticket Now Bar path.
- `NowBarService` is declared in the manifest, but the targeted dex/string search did not
  find a first-party implementation body.

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

Exact Personal Data Intelligence source index:

| Evidence | Location |
| --- | --- |
| Java bridge into `updateViewWithRemoteViews(type, view, remoteViews)` | `GenUIEventManager.java:92-96` |
| public handoff `updateView(channel, type, view, remoteViews)` | `GenUIEventManager.java:224-232` |
| smali bridge calls `IGenUICallback.updateViewWithRemoteViews(...)`; falls back to `updateView(...)` when map is empty | `GenUIEventManager.smali:362-380` |
| `TaskGenUIController.updateGenUIData(...)` builds GenUI string then RemoteViews map | `TaskGenUIController.java:148-192` |
| final controller callback calls `genUIEventManager.updateView(...)` | `TaskGenUIController$updateGenUIData$2$1.java:64` |
| `GenUIUpdateManager` creates `TaskGenUIController` and registers default/secure channels | `GenUIUpdateManager.java:257-267` |
| binder registration chooses `ON_SCREEN_NUDGE_CHANNEL` or `DEFAULT_CHANNEL` | `GenUIService$binder$1$register$1.java:50-53` |

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

Exact AppSearch decorator source index:

| Evidence | Location |
| --- | --- |
| `FLAG_CHIP`, `FLAG_NORMAL`, `FLAG_EXPAND`, view keys, wallpaper key | `AppSearchViewDecoratorApi.smali:84-117` |
| commute decorator returns bundle entries for `normalView`, `expandView`, and `chipView` | `decorators/a.smali:480-569` |
| finance decorator returns `normalView` and `expandView` | `FinanceViewDecorator.smali:3134-3188` |
| sports decorator reads `KEY_IS_WHITE_WALLPAPER` and switches colors/icons | `SportsViewDecorator.smali:4796-4890` |
| sports decorator builds `chipView`, `normalView`, and `expandView` based on flags | `SportsViewDecorator.smali:5257-5496` |
| sports GenUI mapper calls `buildSportsInfoRemoteViews(..., FLAG_EXPAND, KEY_IS_WHITE_WALLPAPER=true/false)` | `GenUIRuleMapper$remoteViewsMap$1.java:35-39` |
| sports mapper stores returned expanded RemoteViews under `_light` and `_dark` keys | `GenUIRuleMapper$remoteViewsMap$1.smali:160-208` |

RemoteViews builders:

- Commute builds `chipView`, `normalView`, and `expandView`.
- Finance builds `normalView` and `expandView`.
- Sports builds `chipView`, `normalView`, and `expandView`.
- Sports also reads `KEY_IS_WHITE_WALLPAPER` to select light / dark colors and icons
  for the Now Bar surface.
- The sports GenUI mapper builds light and dark RemoteViews variants and stores them
  under string keys that end in `_light` and `_dark`.

GenUI model / renderer evidence:

- `GenUINode$ExtraRemoteViews` exists in the embedded GenUI model layer.
- `ExtraRemoteViews` carries `key`, `style`, `priority`, `action`, and `type` fields.
- `GenUINodeAdapterFactory`, `GenUIStyleKit`, and `GenUIRenderer` reference
  `GenUINode$ExtraRemoteViews`, which confirms that RemoteViews are a first-class GenUI
  node type, not only a side bundle.

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
- `nowbar_ic_galaxy_intelligence`
- `nowbar_commute_chip_background`
- `nowbar_daily_activity`
- `nowbar_driving`
- `nowbar_schedule`
- `nowbar_sports_chip_no_logo_bg`
- `nowbar_sports_expand_no_logo_bg`
- `nowbar_sports_normal_no_logo_bg`
- `lock_listen_brief_nowbar_afternoon`
- `lock_listen_brief_nowbar_evening`
- `lock_listen_brief_nowbar_morning`
- `rounded_nowbar_button_background`

Resource / string evidence index:

| Evidence | Location |
| --- | --- |
| `app_name_now_bar` | `res/values/strings.xml:357` |
| Now brief stays on Now bar until tapped and reappears with new content | `res/values/strings.xml:674` |
| lock-screen Now brief can expand without unlocking | `res/values/strings.xml:708-710` |
| Audio Brief notification permission copy is for playback controls | `res/values/strings.xml:2535` |
| sports scores require live notifications in Now bar settings | `res/values/strings.xml:3101` |
| `common_layout_ongoing_commute_*`, `finance_*`, `sports_*` | `res/values/public.xml:16616-16623` |
| `nowbar_commute_chip_view` | `res/values/public.xml:17373` |
| Now Bar background / sports / Galaxy Intelligence drawables | `res/values/public.xml:11424-11446` |

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

Additional PDI data / provider / document surface:

- `NowBarGoogleFinanceDao_ChinaRawDataDatabase_Impl`
- `NowBarGoogleFinanceDao_GlobalRawDataDatabase_Impl`
- `NowBarSportsDao_ChinaRawDataDatabase_Impl`
- `NowBarSportsDao_GlobalRawDataDatabase_Impl`
- `NowBarGoogleFinanceSourceData`
- `NowBarSportsSourceData`
- AppSearch sports/finance document decorators:
  `SportsTeamDocument`, `SportsEventDocument`, `FinanceDocument`, `PublisherName`,
  `ISportsInfo`, and `ISportsTeamInfo`.
- Source-data fields observed for sports include `identifier`, `sports`, `homeTeam`,
  `awayTeam`, and `startDate`.
- Source-data fields observed for finance include `description`, `alternateNames`,
  `type`, `name`, `fullName`, `symbol`, `price`, `priceChangeDelta`,
  `priceChangePercentage`, `marketName`, `marketDescription`, `marketCurrency`,
  `marketCountry`, `marketCode`, and `marketTimeZone`.

Broader GenUI converter families seen around the same pipeline:

- `CommutingToOfficeTaskConverter`
- `DailyMomentTaskConverter`
- `DailyReminderReviewDataTaskConverter`
- `DeliveryMessageDailyReportConverter`
- `EvBatteryInfoTaskConverter`
- `GalleryStorySuggestionTaskConverter`
- `HealthBloodGlucoseBgmTaskConverter`
- `HealthDailyActivityTaskConverter`
- `HealthSleepBedtimeTaskConverter`
- `HealthSleepScoreTaskConverter`
- `HealthWorkOutResultTaskConverter`
- `ImminentCouponAlertTaskConverter`
- `MissedCallTaskConverter`
- `NewsDataTaskConverter`
- `ParkingFeeInfoTaskConverter`
- `ParkingTaskConverter`
- `RecallListTaskConverter`
- `ReminderCardRoutineDataTaskConverter`
- `ScheduleTaskConverter`
- `SleepingEnvironmentReportTaskConverter`
- `SynapseCardTaskConverter`
- `TravelTicketTaskConverter`
- `UpcomingContactAppDomainDataTaskConverter`
- `WeatherTaskConverter`

Notification-related code in PDI appears adjacent, not the primary Now Bar transport:

- Audio Brief creates a normal `NotificationCompat` notification and channel.
- Notification permission copy mentions playback controls for Audio Brief.
- `PdeNotificationListenerService` ingests `StatusBarNotification` data and reads
  `pde_noti_id`.
- No first-party Now Bar `startForeground()` or promoted ongoing notification builder
  path was found.

Exact adjacent-notification source index:

| Evidence | Location |
| --- | --- |
| Audio Brief builds `NotificationCompat` on `ListenBriefNotificationDataSource_CHANNEL_ID` | `ListenBriefNotificationRepository.smali:1097-1157` |
| Audio Brief posts `NotificationManager.notify(0x3e9, notification)` | `ListenBriefNotificationRepository.smali:1195-1201` |
| Audio Brief creates its notification channel | `ListenBriefNotificationRepository.smali:1528-1588` |
| Audio Brief checks `android.permission.POST_NOTIFICATIONS` | `ListenBriefNotificationRepository.smali:1672-1674` |
| notification listener converts `StatusBarNotification` into document data | `PdeNotificationListenerService.smali:242-270` |
| notification user-action path reads extra `pde_noti_id` | `PdeNotificationListenerService.smali:317-325` |

Exact Personal Data Intelligence manifest index:

| Evidence | Location |
| --- | --- |
| custom permission `BRIEF_LIFECYCLE` and self-use | `AndroidManifest.apkanalyzer.xml:323-328` |
| `POST_NOTIFICATIONS` | `AndroidManifest.apkanalyzer.xml:333-334` |
| `ACCESS_NOTIFICATIONS` / `MANAGE_NOTIFICATIONS` | `AndroidManifest.apkanalyzer.xml:795-799` |
| `com.samsung.android.sharelive.permission.SHARE_LIVE_UPDATE` | `AndroidManifest.apkanalyzer.xml:1024-1025` |
| `FOREGROUND_SERVICE` | `AndroidManifest.apkanalyzer.xml:1068-1069` |
| `WidgetNowBarAgent$WidgetNowBarReceiver`, exported false | `AndroidManifest.apkanalyzer.xml:2451-2453` |
| `PdeNotificationListenerService`, disabled, `BIND_NOTIFICATION_LISTENER_SERVICE`, process `:moneta` | `AndroidManifest.apkanalyzer.xml:3945-3957` |
| AndroidX WorkManager foreground service | `AndroidManifest.apkanalyzer.xml:4292-4296` |

No first-party usage was found for:

- `setRequestPromotedOngoing`
- `isRequestPromotedOngoing`
- `promoted_ongoing`
- `BubbleMetadata`
- `setBubbleMetadata`
- `android.ongoingActivityNoti.*`

Negative-search interpretation:

- Bubble APIs were present only through AndroidX compatibility classes in PDI.
- Samsung Pay also has generic bubble/RemoteViews strings from library code, but no
  first-party Now Bar path used them.
- `ProgressStyle` hits in Samsung Pay were ordinary resource / style names such as
  `indeterminateProgressStyle`, not Android `Notification.ProgressStyle`.
- PDI has notification APIs for Audio Brief and data ingestion, but not as the carrier
  for the Now Bar / Now Brief GenUI cards.

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
