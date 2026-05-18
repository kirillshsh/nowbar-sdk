#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="${OUT_DIR:-$ROOT_DIR/artifacts/samsung-apk-research/$(date +%Y%m%d-%H%M%S)}"
DECOMPILE="${DECOMPILE:-1}"
JADX_ARGS="${JADX_ARGS:---no-res}"
DISCOVER_SAMSUNG_PACKAGES="${DISCOVER_SAMSUNG_PACKAGES:-1}"
MAX_DISCOVERED_PACKAGES="${MAX_DISCOVERED_PACKAGES:-60}"
ADB_CONNECT="${ADB_CONNECT:-}"
WAIT_FOR_DEVICE_SECONDS="${WAIT_FOR_DEVICE_SECONDS:-0}"

default_packages=(
  "com.android.systemui"
  "com.android.settings"
  "com.sec.android.app.launcher"
  "com.sec.android.app.clockpackage"
  "com.samsung.android.app.shealth"
  "com.sec.android.app.voicenote"
  "com.samsung.android.app.routines"
  "com.samsung.android.game.gametools"
  "com.samsung.android.app.aodservice"
  "com.google.android.apps.maps"
  "com.google.android.googlequicksearchbox"
)

pick_device() {
  adb devices | awk 'NR > 1 && $2 == "device" { print $1; exit }'
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

mkdir -p "$OUT_DIR"
connect_requested_device
wait_for_adb_device "${SERIAL:-}" || true
adb devices -l > "$OUT_DIR/adb-devices.txt" 2>&1 || true
adb mdns services > "$OUT_DIR/adb-mdns-services.txt" 2>&1 || true

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
  echo "No adb device is connected. Connect a Samsung phone with USB debugging enabled, or set ADB_CONNECT=localhost:<port> for Samsung Remote Test Lab RDB." >&2
  adb devices -l >&2
  exit 2
fi

if [[ "$#" -gt 0 ]]; then
  packages=("$@")
else
  packages=("${default_packages[@]}")
fi

summary="$OUT_DIR/summary.txt"
scan_all="$OUT_DIR/nowbar-scan-all.txt"
: > "$summary"
: > "$scan_all"

manufacturer="$(adb -s "$SERIAL" shell getprop ro.product.manufacturer | tr -d '\r')"
model="$(adb -s "$SERIAL" shell getprop ro.product.model | tr -d '\r')"
echo "serial=$SERIAL manufacturer=$manufacturer model=$model" | tee -a "$summary"

scan_paths() {
  scan_pattern="ongoingActivityNoti|ongoingActivity|OngoingActivityStyle|nowbar|Now Bar|aodRemoteApp|nowbarRemoteView|expandedRemoteView|chipExpandedView|CustomCardView|ExpandedChipView|CardBackground|ChipBackground|pde_first_shown_time_ms|pde_first_expanded_time_ms|pde_enqueued_time_ms|pde_noti_clicked_count|pde_noti_action_clicked_count|pde_noti_id|pde_noti_pkg|pde_noti_tag|android\\.styledByProgress|android\\.progressEndIcon|android\\.progressStartIcon|android\\.progressTrackerIcon|android\\.progressPoints|android\\.progressSegments|android\\.progressIndeterminate|android\\.progressMax|android\\.progress|isCapsule|capsule_|bg_startColor|bg_endColor|semGetApplicationIconForIconTray|SERVICEBOX_REMOTEVIEWS|FACE_WIDGET|com\\.samsung\\.feature\\.nowbar|com\\.google\\.android\\.feature\\.AMBIENT_DATA|com\\.oplus\\.software\\.feature\\.ambient_alerts|support\\.ongoing_activity|POST_PROMOTED|BigTextStyle|CallStyle|android\\.callType|android\\.callIsVideo|android\\.callPersonCompat|android\\.callPerson|android\\.answerIntent|android\\.declineIntent|android\\.hangUpIntent|android\\.answerColor|android\\.declineColor|android\\.verificationText|android\\.verificationIcon|android\\.subText|setSubText|android\\.showWhen|android\\.showChronometer|android\\.chronometerCountDown|ProgressStyle|MetricStyle|FLAG_PROMOTED|requestPromoted|shortCritical|canPostPromoted|hasPromotable|createSemanticStyleAnnotation|SEMANTIC_STYLE|setDeleteIntent|deleteIntent|setLargeIcon"
  for target in "$@"; do
    if [[ -f "$target" ]]; then
      strings "$target" | grep -nE "$scan_pattern" | sed "s|^|$target:strings:|" || true
    elif command -v rg >/dev/null 2>&1; then
      rg -n "$scan_pattern" "$target" || true
    else
      grep -RInE "$scan_pattern" "$target" || true
    fi
  done
}

discover_samsung_packages() {
  adb -s "$SERIAL" shell pm list packages |
    tr -d '\r' |
    sed 's/^package://' |
    grep -Ei '(^com\.samsung\.|^com\.sec\.|^com\.android\.systemui$|^com\.android\.settings$|aod|clock|health|routine|game|launcher|maps|googlequicksearchbox)' |
    sort -u |
    head -n "$MAX_DISCOVERED_PACKAGES" || true
}

dedupe_packages() {
  printf '%s\n' "$@" | awk 'NF && !seen[$0]++'
}

analyze_apk_metadata() {
  local apk="$1"
  local output_dir="$2"
  local apk_name="$3"

  if ! command -v apkanalyzer >/dev/null 2>&1; then
    echo "apkanalyzer not found" >> "$summary"
    return
  fi

  {
    echo "application-id=$(apkanalyzer manifest application-id "$apk" 2>/dev/null || true)"
    echo "version-name=$(apkanalyzer manifest version-name "$apk" 2>/dev/null || true)"
    echo "version-code=$(apkanalyzer manifest version-code "$apk" 2>/dev/null || true)"
    echo "min-sdk=$(apkanalyzer manifest min-sdk "$apk" 2>/dev/null || true)"
    echo "target-sdk=$(apkanalyzer manifest target-sdk "$apk" 2>/dev/null || true)"
    echo "debuggable=$(apkanalyzer manifest debuggable "$apk" 2>/dev/null || true)"
    echo
    echo "permissions:"
    apkanalyzer manifest permissions "$apk" 2>/dev/null || true
  } > "$output_dir/manifest-$apk_name-summary.txt"

  apkanalyzer manifest print "$apk" > "$output_dir/manifest-$apk_name.xml" 2>/dev/null || true
}

if [[ "$#" -eq 0 && "$DISCOVER_SAMSUNG_PACKAGES" == "1" ]]; then
  mapfile -t discovered_packages < <(discover_samsung_packages)
  mapfile -t packages < <(dedupe_packages "${packages[@]}" "${discovered_packages[@]}")
  {
    echo "discovery=enabled max=$MAX_DISCOVERED_PACKAGES"
    printf 'package=%s\n' "${packages[@]}"
    echo
  } >> "$summary"
fi

for package_name in "${packages[@]}"; do
  echo "== $package_name ==" | tee -a "$summary"
  package_dir="$OUT_DIR/$package_name"
  apk_dir="$package_dir/apks"
  mkdir -p "$apk_dir"

  remote_paths=()
  while IFS= read -r remote_path; do
    if [[ -n "$remote_path" ]]; then
      remote_paths+=("$remote_path")
    fi
  done < <(
    adb -s "$SERIAL" shell pm path "$package_name" 2>/dev/null |
      tr -d '\r' |
      sed 's/^package://'
  )

  if [[ "${#remote_paths[@]}" -eq 0 ]]; then
    echo "not installed" | tee -a "$summary"
    continue
  fi

  pulled=()
  for remote_path in "${remote_paths[@]}"; do
    local_apk="$apk_dir/$(basename "$remote_path")"
    adb -s "$SERIAL" pull "$remote_path" "$local_apk" >/dev/null
    pulled+=("$local_apk")
    echo "pulled $remote_path -> $local_apk" >> "$summary"
  done

  adb -s "$SERIAL" shell dumpsys package "$package_name" \
    > "$package_dir/dumpsys-package.txt" 2>&1 || true

  targets=("${pulled[@]}")

  for apk in "${pulled[@]}"; do
    apk_name="$(basename "$apk" .apk)"
    analyze_apk_metadata "$apk" "$package_dir" "$apk_name"
    targets+=(
      "$package_dir/manifest-$apk_name-summary.txt"
      "$package_dir/manifest-$apk_name.xml"
    )
  done

  if [[ "$DECOMPILE" == "1" ]]; then
    if command -v apktool >/dev/null 2>&1; then
      mkdir -p "$package_dir/apktool"
      for apk in "${pulled[@]}"; do
        apk_name="$(basename "$apk" .apk)"
        apktool d -f "$apk" -o "$package_dir/apktool/$apk_name" >> "$package_dir/apktool.log" 2>&1 || true
      done
      targets+=("$package_dir/apktool")
    else
      echo "apktool not found" >> "$summary"
    fi

    if command -v jadx >/dev/null 2>&1; then
      mkdir -p "$package_dir/jadx"
      for apk in "${pulled[@]}"; do
        apk_name="$(basename "$apk" .apk)"
        jadx $JADX_ARGS -d "$package_dir/jadx/$apk_name" "$apk" >> "$package_dir/jadx.log" 2>&1 || true
      done
      targets+=("$package_dir/jadx")
    else
      echo "jadx not found" >> "$summary"
    fi
  fi

  scan_paths "${targets[@]}" > "$package_dir/nowbar-scan.txt"
  {
    echo "== $package_name =="
    cat "$package_dir/nowbar-scan.txt"
    echo
  } >> "$scan_all"
done

if [[ -x "$ROOT_DIR/scripts/check_nowbar_key_coverage.sh" ]]; then
  coverage_dir="$OUT_DIR/key-coverage"
  OUT_DIR="$coverage_dir" "$ROOT_DIR/scripts/check_nowbar_key_coverage.sh" "$scan_all" \
    >> "$summary" 2>&1 || true
fi

echo "Samsung APK research artifacts written to $OUT_DIR"
echo "Combined scan: $scan_all"
