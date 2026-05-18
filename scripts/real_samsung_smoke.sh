#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ANDROID_HOME="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$ANDROID_HOME}"
PACKAGE_NAME="${PACKAGE_NAME:-com.nowbar.demo}"
OUT_DIR="${OUT_DIR:-$ROOT_DIR/artifacts/real-samsung-smoke/$(date +%Y%m%d-%H%M%S)}"
SLEEP_SECONDS="${SLEEP_SECONDS:-2}"
CAPTURE_NOTIFICATION_SHADE="${CAPTURE_NOTIFICATION_SHADE:-1}"
CAPTURE_LOCKSCREEN="${CAPTURE_LOCKSCREEN:-0}"
ADB_CONNECT="${ADB_CONNECT:-}"
WAIT_FOR_DEVICE_SECONDS="${WAIT_FOR_DEVICE_SECONDS:-0}"

export ANDROID_HOME ANDROID_SDK_ROOT

actions=(
  "com.nowbar.demo.SHOW_TIMER"
  "com.nowbar.demo.SHOW_WORKOUT"
  "com.nowbar.demo.SHOW_NAVIGATION"
  "com.nowbar.demo.SHOW_DELIVERY"
  "com.nowbar.demo.SHOW_METRICS"
  "com.nowbar.demo.SHOW_MEDIA"
  "com.nowbar.demo.SHOW_CALL"
  "com.nowbar.demo.SHOW_CALL_SCREENING"
  "com.nowbar.demo.SHOW_BIG_TEXT"
  "com.nowbar.demo.SHOW_CUSTOM_ACTIONS"
  "com.nowbar.demo.SHOW_DUMP"
  "com.nowbar.demo.SHOW_NATIVE_STYLE"
  "com.nowbar.demo.PAUSE"
  "com.nowbar.demo.RESUME"
  "com.nowbar.demo.NEXT"
  "com.nowbar.demo.UNPIN"
  "com.nowbar.demo.DISMISS"
  "com.nowbar.demo.STOP"
)

manufacturer=""
model=""
sdk=""
release=""
oneui=""
incremental=""
instrumentation_status="not-run"
verification_status="not-run"
actions_run=0
ACTION_SUMMARY_FILE=""

pick_device() {
  adb devices | awk 'NR > 1 && $2 == "device" { print $1; exit }'
}

write_adb_preflight() {
  adb devices -l > "$OUT_DIR/adb-devices.txt" 2>&1 || true
  adb mdns services > "$OUT_DIR/adb-mdns-services.txt" 2>&1 || true
}

connect_requested_device() {
  local target="$ADB_CONNECT"

  if [[ -z "$target" && -n "${SERIAL:-}" && "${SERIAL:-}" == *:* ]]; then
    target="$SERIAL"
  fi

  if [[ -n "$target" ]]; then
    adb connect "$target" > "$OUT_DIR/adb-connect.txt" 2>&1 || true
    if [[ -z "${SERIAL:-}" ]]; then
      SERIAL="$target"
    fi
  else
    : > "$OUT_DIR/adb-connect.txt"
  fi
}

wait_for_adb_device() {
  local target="${1:-}"
  local deadline=$((SECONDS + WAIT_FOR_DEVICE_SECONDS))
  local state=""

  while true; do
    if [[ -n "$target" ]]; then
      state="$(adb -s "$target" get-state 2>/dev/null | tr -d '\r' || true)"
      [[ "$state" == "device" ]] && return 0
    elif [[ -n "$(pick_device)" ]]; then
      return 0
    fi

    if [[ "$WAIT_FOR_DEVICE_SECONDS" == "0" || "$SECONDS" -ge "$deadline" ]]; then
      return 1
    fi
    sleep 1
  done
}

count_pattern() {
  local file="$1"
  local pattern="$2"
  if [[ ! -f "$file" ]]; then
    echo "0"
    return
  fi
  grep -Eic -- "$pattern" "$file" 2>/dev/null || true
}

extract_am_status() {
  local file="$1"
  local status=""
  if [[ ! -f "$file" ]]; then
    echo "missing"
    return
  fi
  status="$(awk -F': ' '/^Status:/{ print $2; exit }' "$file" 2>/dev/null || true)"
  echo "${status:-unknown}"
}

file_size_bytes() {
  local file="$1"
  if [[ ! -f "$file" ]]; then
    echo "0"
    return
  fi
  wc -c < "$file" | tr -d ' '
}

write_package_slice() {
  local dump_file="$1"
  local slice_file="$2"
  if [[ ! -f "$dump_file" ]]; then
    : > "$slice_file"
    return
  fi
  grep -n -A500 -B80 "$PACKAGE_NAME" "$dump_file" > "$slice_file" || true
}

append_action_summary() {
  local action="$1"
  local safe_name="$2"
  local am_file="$3"
  local dump_file="$4"
  local screenshot_file="$5"
  local shade_screenshot_file="$6"
  local lock_screenshot_file="$7"
  local slice_file="$8"
  local evidence_file="$9"

  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
    "$safe_name" \
    "$action" \
    "$(extract_am_status "$am_file")" \
    "$(count_pattern "$am_file" 'Error|Exception|SecurityException|Activity not started|Status: error')" \
    "$(count_pattern "$slice_file" "$PACKAGE_NAME")" \
    "$(count_pattern "$evidence_file" 'android\.requestPromotedOngoing|requestPromoted|requestPromotedOngoing|FLAG_PROMOTED')" \
    "$(count_pattern "$evidence_file" 'android\.ongoingActivityNoti\.|samsungNowBarExtrasCount=[1-9]')" \
    "$(count_pattern "$evidence_file" 'android\.ongoingActivityNoti\.style|samsungStyle=')" \
    "$(count_pattern "$evidence_file" 'progressSegments|progressPoints|progressTrackerIcon|progressStartIcon|progressEndIcon|styledByProgress|ProgressStyle|android\.ongoingActivityNoti\.progress|android\.progress|android\.progressMax|android\.progressIndeterminate|androidProgress')" \
    "$(count_pattern "$evidence_file" 'MetricStyle|android\.app\.Notification\$MetricStyle|metricStyleTemplate=true')" \
    "$(count_pattern "$evidence_file" 'OngoingActivityStyle|android\.app\.Notification\$OngoingActivityStyle|nativeOngoingActivityTemplate=true')" \
    "$(count_pattern "$evidence_file" 'isCapsule|capsule_|bg_startColor|bg_endColor|hasCapsule=true')" \
    "$(file_size_bytes "$screenshot_file")" \
    "$(file_size_bytes "$shade_screenshot_file")" \
    "$(file_size_bytes "$lock_screenshot_file")" \
    "$(count_pattern "$evidence_file" 'BigTextStyle|android\.app\.Notification\$BigTextStyle|bigTextStyleTemplate=true')" \
    "$(count_pattern "$evidence_file" 'CallStyle|android\.app\.Notification\$CallStyle|android\.callType|android\.answerIntent|android\.declineIntent|android\.hangUpIntent|callStyleTemplate=true')" \
    "$(count_pattern "$evidence_file" 'android\.shortCriticalText|android\.showWhen|android\.showChronometer|android\.chronometerCountDown|hasStatusChip=true|shortCriticalText=')" \
    "$(count_pattern "$evidence_file" 'android\.subText|setSubText|subText=')" \
    "$(count_pattern "$evidence_file" 'aodRemoteApp|android\.ongoingActivityNoti\.aodRemoteApp(Name|Icon|PendingIntent)|hasAodRemoteApp=true')" \
    "$(count_pattern "$evidence_file" 'nowbarRemoteView|expandedRemoteView|chipExpandedView|CustomExpandedCardView|ExpandedChipView|ExpandedNowBarView|CustomCardViewCenterUI|hasSamsungRemoteViews=true')" \
    "$(count_pattern "$evidence_file" 'android\.ongoingActivity(Noti\.show|ChipIcon|Badge|CardIcon|ActionBgColors|NowBarExpandableType|PrimaryAction)|android\.reduced\.images|samsungVisual')" \
    "$(count_pattern "$evidence_file" 'deleteIntent|setDeleteIntent|hasDeleteIntent=true')" \
    "$(count_pattern "$evidence_file" 'contentIntent|setContentIntent|hasContentIntent=true')" \
    "$(count_pattern "$evidence_file" 'Unpin|com\.nowbar\.demo\.UNPIN')" \
    "$(count_pattern "$evidence_file" 'android\.ongoingActivityChronometer(Base|Countdown|Format|Speed|Start)|samsungChronometer')" \
    "$(count_pattern "$evidence_file" 'NATIVE_STYLE_REPORT|Hidden style class|Applied methods|Missing methods|Failed methods')" \
    "$(count_pattern "$evidence_file" 'actions=|android\.actions|Action\(|androidActions=|androidActionCount=[1-9]|title=(Track|Tip|Stop|Open|Pause|Unpin|Next)|text=(Track|Tip|Stop|Open|Pause|Unpin|Next)')" \
    "$(count_pattern "$evidence_file" 'title=Tip|text=Tip|Tip|textOnly=true|androidTextOnlyActionCount=[1-9]')" \
    "$(count_pattern "$evidence_file" 'title=Tip|text=Tip|Tip|disabled=true|androidDisabledActionCount=[1-9]|actionIntent=null|intent=null|PendingIntent=null')" \
    "$(count_pattern "$evidence_file" 'com\.nowbar\.action\.(ID|SEMANTIC)|androidActionIds=|androidActionSemantics=|id=(track|tip|stop|open|pause|unpin|next)|semantic=(CUSTOM|STOP|UNPIN|NEXT|DELETE|PAUSE)')" \
    >> "$ACTION_SUMMARY_FILE"
}

write_summary() {
  local status="$1"
  local exit_code="$2"
  [[ -d "${OUT_DIR:-}" ]] || return

  local summary="$OUT_DIR/summary.md"
  {
    echo "# Samsung Now Bar smoke summary"
    echo
    echo "- status: $status"
    echo "- exit_code: $exit_code"
    echo "- serial: ${SERIAL:-}"
    echo "- manufacturer: ${manufacturer:-}"
    echo "- model: ${model:-}"
    echo "- android_release: ${release:-}"
    echo "- sdk: ${sdk:-}"
    echo "- oneui: ${oneui:-}"
    echo "- incremental: ${incremental:-}"
    echo "- instrumentation_status: $instrumentation_status"
    echo "- artifact_verification_status: $verification_status"
    echo "- actions_run: $actions_run/${#actions[@]}"
    echo "- notification_shade_capture_enabled: $CAPTURE_NOTIFICATION_SHADE"
    echo "- lockscreen_capture_enabled: $CAPTURE_LOCKSCREEN"
    echo "- adb_connect: ${ADB_CONNECT:-}"
    echo "- wait_for_device_seconds: $WAIT_FOR_DEVICE_SECONDS"
    echo
    echo "## Evidence files"
    echo
    echo "- adb-connect.txt"
    echo "- adb-devices.txt"
    echo "- adb-mdns-services.txt"
    echo "- device.txt"
    echo "- getprop.txt"
    echo "- features.txt"
    echo "- settings-secure-notification.txt"
    echo "- settings-system-notification.txt"
    echo "- settings-global-notification.txt"
    echo "- samsung-packages.txt"
    echo "- demo-apk-manifest-summary.txt"
    echo "- install.txt"
    echo "- demo-connected-android-test.txt"
    echo "- artifact-verification.txt"
    echo "- action-summary.tsv"
    echo "- dumpsys-<ACTION>.txt and dumpsys-<ACTION>-package-slice.txt"
    echo "- logcat-<ACTION>.txt and evidence-<ACTION>.txt"
    echo "- screen-<ACTION>.png"
    echo "- screen-<ACTION>-shade.png"
    echo "- screen-<ACTION>-lock.png"
    echo "- logcat.txt"
    echo "- nowbar-log.txt"
    echo
    if [[ -f "$ACTION_SUMMARY_FILE" ]]; then
      echo "## Action evidence counts"
      echo
      echo '```tsv'
      sed -n '1,200p' "$ACTION_SUMMARY_FILE"
      echo '```'
      echo
    fi
    echo "## Notes"
    echo
    echo "- package_refs > 0 proves the action posted or retained a notification owned by $PACKAGE_NAME in dumpsys."
    echo "- request_promoted_refs is expected mainly on Android 16+ promoted Live Updates and Samsung native style."
    echo "- samsung_extra_refs/samsung_style_refs prove proprietary Samsung Now Bar extras survived into the posted notification."
    echo "- progress_refs, metric_refs, native_style_refs, native_style_report_refs, big_text_refs, call_refs, action_button_refs, text_only_action_refs, disabled_action_refs, action_metadata_refs, status_chip_refs, subtext_refs, aod_remote_app_refs, remote_view_refs, dump_visual_refs, delete_intent_refs, content_intent_refs, unpin_refs, samsung_chronometer_refs, and capsule_refs identify which compatibility path was exercised."
    echo "- Screenshots must still be visually reviewed on the actual phone or Remote Test Lab session for notification shade, lock screen, and AOD rendering."
  } > "$summary"
}

on_exit() {
  local exit_code=$?
  set +e
  if [[ -d "${OUT_DIR:-}" ]]; then
    if [[ "$exit_code" -eq 0 ]]; then
      write_summary "completed" "$exit_code"
    else
      write_summary "failed" "$exit_code"
    fi
  fi
}

trap on_exit EXIT

mkdir -p "$OUT_DIR"
connect_requested_device
wait_for_adb_device "${SERIAL:-}" || true
write_adb_preflight

if [[ -n "${SERIAL:-}" ]]; then
  if [[ "$(adb -s "$SERIAL" get-state 2>/dev/null | tr -d '\r' || true)" != "device" ]]; then
    echo "Requested adb device is not connected: $SERIAL" >&2
    echo "Set ADB_CONNECT=localhost:<port> for Samsung Remote Test Lab RDB, or connect a Samsung phone with USB debugging enabled." >&2
    adb devices -l >&2
    exit 2
  fi
else
  SERIAL="$(pick_device)"
fi

if [[ -z "$SERIAL" ]]; then
  echo "No adb device is connected. Connect a Samsung phone, enable USB debugging, or set ADB_CONNECT=localhost:<port> for Samsung Remote Test Lab RDB, then rerun." >&2
  adb devices -l >&2
  exit 2
fi

export ANDROID_SERIAL="$SERIAL"

manufacturer="$(adb -s "$SERIAL" shell getprop ro.product.manufacturer | tr -d '\r')"
model="$(adb -s "$SERIAL" shell getprop ro.product.model | tr -d '\r')"
sdk="$(adb -s "$SERIAL" shell getprop ro.build.version.sdk | tr -d '\r')"
release="$(adb -s "$SERIAL" shell getprop ro.build.version.release | tr -d '\r')"
oneui="$(adb -s "$SERIAL" shell getprop ro.build.version.oneui | tr -d '\r')"
incremental="$(adb -s "$SERIAL" shell getprop ro.build.version.incremental | tr -d '\r')"

{
  echo "serial=$SERIAL"
  echo "manufacturer=$manufacturer"
  echo "model=$model"
  echo "sdk=$sdk"
  echo "release=$release"
  echo "oneui=$oneui"
  echo "incremental=$incremental"
  echo
  echo "nowbar features:"
  adb -s "$SERIAL" shell pm list features | tr -d '\r' | grep -i 'samsung.*nowbar\|nowbar' || true
} > "$OUT_DIR/device.txt"
adb -s "$SERIAL" shell getprop | tr -d '\r' > "$OUT_DIR/getprop.txt" 2>&1 || true
adb -s "$SERIAL" shell settings list secure | tr -d '\r' \
  | grep -Ei 'nowbar|now_bar|notification|promot|aod|ambient|ongoing' \
  > "$OUT_DIR/settings-secure-notification.txt" || true
adb -s "$SERIAL" shell settings list system | tr -d '\r' \
  | grep -Ei 'nowbar|now_bar|notification|promot|aod|ambient|ongoing' \
  > "$OUT_DIR/settings-system-notification.txt" || true
adb -s "$SERIAL" shell settings list global | tr -d '\r' \
  | grep -Ei 'nowbar|now_bar|notification|promot|aod|ambient|ongoing' \
  > "$OUT_DIR/settings-global-notification.txt" || true
adb -s "$SERIAL" shell pm list features | tr -d '\r' > "$OUT_DIR/features.txt" 2>&1 || true
adb -s "$SERIAL" shell cmd notification --help | tr -d '\r' > "$OUT_DIR/notification-cmd-help.txt" 2>&1 || true
adb -s "$SERIAL" shell pm list packages -f | tr -d '\r' \
  | grep -Ei 'samsung|sec.android|systemui|aod|launcher|clock|health|routine|voicenote' \
  > "$OUT_DIR/samsung-packages.txt" || true

if [[ "$(printf '%s' "$manufacturer" | tr '[:upper:]' '[:lower:]')" != *samsung* ]]; then
  echo "Connected device is not a Samsung phone: $manufacturer $model" >&2
  echo "Evidence written to $OUT_DIR/device.txt" >&2
  exit 3
fi

"$ROOT_DIR/gradlew" -p "$ROOT_DIR" :demo:assembleDebug --console=plain

apk="$ROOT_DIR/demo/build/outputs/apk/debug/demo-debug.apk"
if command -v apkanalyzer >/dev/null 2>&1; then
  {
    echo "application-id=$(apkanalyzer manifest application-id "$apk" 2>/dev/null || true)"
    echo "version-name=$(apkanalyzer manifest version-name "$apk" 2>/dev/null || true)"
    echo "version-code=$(apkanalyzer manifest version-code "$apk" 2>/dev/null || true)"
    echo "min-sdk=$(apkanalyzer manifest min-sdk "$apk" 2>/dev/null || true)"
    echo "target-sdk=$(apkanalyzer manifest target-sdk "$apk" 2>/dev/null || true)"
    echo
    echo "permissions:"
    apkanalyzer manifest permissions "$apk" 2>/dev/null || true
  } > "$OUT_DIR/demo-apk-manifest-summary.txt"
  apkanalyzer manifest print "$apk" > "$OUT_DIR/demo-apk-AndroidManifest.xml" 2>/dev/null || true
fi

adb -s "$SERIAL" install -r -d -g "$apk" > "$OUT_DIR/install.txt"
adb -s "$SERIAL" shell pm grant "$PACKAGE_NAME" android.permission.POST_NOTIFICATIONS >/dev/null 2>&1 || true
adb -s "$SERIAL" shell dumpsys package "$PACKAGE_NAME" > "$OUT_DIR/demo-package.txt" 2>&1 || true
adb -s "$SERIAL" shell appops get "$PACKAGE_NAME" > "$OUT_DIR/demo-appops.txt" 2>&1 || true
adb -s "$SERIAL" shell dumpsys notification --noredact > "$OUT_DIR/dumpsys-before-actions.txt" 2>&1 || true

adb -s "$SERIAL" logcat -c
adb -s "$SERIAL" shell monkey -p "$PACKAGE_NAME" 1 > "$OUT_DIR/launch.txt" 2>&1 || true

instrumentation_status=0
"$ROOT_DIR/gradlew" -p "$ROOT_DIR" :demo:connectedDebugAndroidTest --console=plain \
  > "$OUT_DIR/demo-connected-android-test.txt" 2>&1 || instrumentation_status=$?

ACTION_SUMMARY_FILE="$OUT_DIR/action-summary.tsv"
printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
  "safe_name" \
  "action" \
  "am_status" \
  "am_error_refs" \
  "package_refs" \
  "request_promoted_refs" \
  "samsung_extra_refs" \
  "samsung_style_refs" \
  "progress_refs" \
  "metric_refs" \
  "native_style_refs" \
  "capsule_refs" \
  "screenshot_bytes" \
  "shade_screenshot_bytes" \
  "lock_screenshot_bytes" \
  "big_text_refs" \
  "call_refs" \
  "status_chip_refs" \
  "subtext_refs" \
  "aod_remote_app_refs" \
  "remote_view_refs" \
  "dump_visual_refs" \
  "delete_intent_refs" \
  "content_intent_refs" \
  "unpin_refs" \
  "samsung_chronometer_refs" \
  "native_style_report_refs" \
  "action_button_refs" \
  "text_only_action_refs" \
  "disabled_action_refs" \
  "action_metadata_refs" \
  > "$ACTION_SUMMARY_FILE"

for action in "${actions[@]}"; do
  safe_name="${action##*.}"
  am_file="$OUT_DIR/am-$safe_name.txt"
  dump_file="$OUT_DIR/dumpsys-$safe_name.txt"
  slice_file="$OUT_DIR/dumpsys-$safe_name-package-slice.txt"
  log_file="$OUT_DIR/logcat-$safe_name.txt"
  evidence_file="$OUT_DIR/evidence-$safe_name.txt"
  screen_file="$OUT_DIR/screen-$safe_name.png"
  shade_screen_file="$OUT_DIR/screen-$safe_name-shade.png"
  lock_screen_file="$OUT_DIR/screen-$safe_name-lock.png"

  adb -s "$SERIAL" logcat -c >/dev/null 2>&1 || true
  adb -s "$SERIAL" shell am start -W -n "$PACKAGE_NAME/.MainActivity" -a "$action" \
    > "$am_file" 2>&1 || true
  sleep "$SLEEP_SECONDS"
  adb -s "$SERIAL" shell dumpsys notification --noredact > "$dump_file" 2>&1 || true
  write_package_slice "$dump_file" "$slice_file"
  adb -s "$SERIAL" logcat -d > "$log_file" 2>&1 || true
  cat "$slice_file" "$log_file" > "$evidence_file" 2>/dev/null \
    || cp "$slice_file" "$evidence_file" 2>/dev/null \
    || : > "$evidence_file"
  adb -s "$SERIAL" exec-out screencap -p > "$screen_file" 2>/dev/null || true
  if [[ "$CAPTURE_NOTIFICATION_SHADE" == "1" ]]; then
    adb -s "$SERIAL" shell cmd statusbar expand-notifications >/dev/null 2>&1 || true
    sleep 1
    adb -s "$SERIAL" exec-out screencap -p > "$shade_screen_file" 2>/dev/null || true
    adb -s "$SERIAL" shell cmd statusbar collapse >/dev/null 2>&1 \
      || adb -s "$SERIAL" shell cmd statusbar collapse-panels >/dev/null 2>&1 \
      || true
  fi
  if [[ "$CAPTURE_LOCKSCREEN" == "1" ]]; then
    adb -s "$SERIAL" shell input keyevent KEYCODE_SLEEP >/dev/null 2>&1 || true
    sleep 1
    adb -s "$SERIAL" exec-out screencap -p > "$lock_screen_file" 2>/dev/null || true
    adb -s "$SERIAL" shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true
    sleep 1
  fi
  append_action_summary "$action" "$safe_name" "$am_file" "$dump_file" "$screen_file" "$shade_screen_file" "$lock_screen_file" "$slice_file" "$evidence_file"
  actions_run=$((actions_run + 1))
done

cat "$OUT_DIR"/logcat-*.txt > "$OUT_DIR/logcat.txt" 2>/dev/null \
  || adb -s "$SERIAL" logcat -d > "$OUT_DIR/logcat.txt" 2>&1 \
  || true
grep -n "NowBarDemo\|Samsung native style report\|LiveUpdate report\|hasPromotable\|canPostPromoted" "$OUT_DIR/logcat.txt" \
  > "$OUT_DIR/nowbar-log.txt" || true

adb -s "$SERIAL" shell dumpsys notification --noredact \
  > "$OUT_DIR/dumpsys-final.txt" 2>&1 || true

verification_status=0
if [[ -x "$ROOT_DIR/scripts/verify_real_samsung_smoke_artifacts.sh" ]]; then
  if [[ "$CAPTURE_NOTIFICATION_SHADE" == "1" ]]; then
    "$ROOT_DIR/scripts/verify_real_samsung_smoke_artifacts.sh" "$OUT_DIR" \
      > "$OUT_DIR/artifact-verification.txt" 2>&1 || verification_status=$?
  else
    ALLOW_MISSING_SHADE_SCREENSHOTS=1 \
      "$ROOT_DIR/scripts/verify_real_samsung_smoke_artifacts.sh" "$OUT_DIR" \
      > "$OUT_DIR/artifact-verification.txt" 2>&1 || verification_status=$?
  fi
fi

echo "Samsung smoke evidence written to $OUT_DIR"
echo "Summary will be written to $OUT_DIR/summary.md"

if [[ "$instrumentation_status" -ne 0 ]]; then
  echo "Instrumentation smoke failed with exit code $instrumentation_status. See $OUT_DIR/demo-connected-android-test.txt" >&2
  exit "$instrumentation_status"
fi

if [[ "$verification_status" != "0" ]]; then
  echo "Artifact verification failed with exit code $verification_status. See $OUT_DIR/artifact-verification.txt" >&2
  exit "$verification_status"
fi
