#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="${OUT_DIR:-$ROOT_DIR/artifacts/local-apk-nowbar-scan/$(date +%Y%m%d-%H%M%S)}"
DECOMPILE="${DECOMPILE:-1}"
JADX_ARGS="${JADX_ARGS:---no-res}"

if [[ "$#" -eq 0 ]]; then
  echo "Usage: $0 <apk-or-directory> [more apk-or-directory ...]" >&2
  exit 2
fi

mkdir -p "$OUT_DIR"
summary="$OUT_DIR/summary.txt"
scan_all="$OUT_DIR/nowbar-scan-all.txt"
: > "$summary"
: > "$scan_all"

scan_pattern="ongoingActivityNoti|ongoingActivity|OngoingActivityStyle|nowbar|Now Bar|aodRemoteApp|nowbarRemoteView|expandedRemoteView|chipExpandedView|CustomCardView|ExpandedChipView|CardBackground|ChipBackground|pde_first_shown_time_ms|pde_first_expanded_time_ms|pde_enqueued_time_ms|pde_noti_clicked_count|pde_noti_action_clicked_count|pde_noti_id|pde_noti_pkg|pde_noti_tag|android\\.styledByProgress|android\\.progressEndIcon|android\\.progressStartIcon|android\\.progressTrackerIcon|android\\.progressPoints|android\\.progressSegments|android\\.progressIndeterminate|android\\.progressMax|android\\.progress|isCapsule|capsule_|bg_startColor|bg_endColor|semGetApplicationIconForIconTray|SERVICEBOX_REMOTEVIEWS|FACE_WIDGET|com\\.samsung\\.feature\\.nowbar|com\\.google\\.android\\.feature\\.AMBIENT_DATA|com\\.oplus\\.software\\.feature\\.ambient_alerts|support\\.ongoing_activity|POST_PROMOTED|BigTextStyle|CallStyle|android\\.callType|android\\.callIsVideo|android\\.callPersonCompat|android\\.callPerson|android\\.answerIntent|android\\.declineIntent|android\\.hangUpIntent|android\\.answerColor|android\\.declineColor|android\\.verificationText|android\\.verificationIcon|android\\.subText|setSubText|android\\.showWhen|android\\.showChronometer|android\\.chronometerCountDown|ProgressStyle|MetricStyle|FLAG_PROMOTED|requestPromoted|shortCritical|canPostPromoted|hasPromotable|createSemanticStyleAnnotation|SEMANTIC_STYLE|setDeleteIntent|deleteIntent|setLargeIcon"

scan_paths() {
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

analyze_apk_metadata() {
  local apk="$1"
  local output_dir="$2"

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
  } > "$output_dir/manifest-summary.txt"

  apkanalyzer manifest print "$apk" > "$output_dir/AndroidManifest.xml" 2>/dev/null || true
}

collect_apks() {
  for input in "$@"; do
    if [[ -d "$input" ]]; then
      find "$input" -type f -iname '*.apk' -print
    elif [[ -f "$input" && "$input" == *.apk ]]; then
      printf '%s\n' "$input"
    elif [[ -f "$input" && ( "$input" == *.xapk || "$input" == *.apks || "$input" == *.zip ) ]]; then
      if command -v unzip >/dev/null 2>&1; then
        archive_name="$(basename "$input")"
        safe_archive_name="$(printf '%s' "$archive_name" | tr -c '[:alnum:]_.-' '_')"
        archive_dir="$OUT_DIR/archive-inputs/$safe_archive_name"
        mkdir -p "$archive_dir"
        unzip -oq "$input" '*.apk' -d "$archive_dir" || true
        find "$archive_dir" -type f -iname '*.apk' -print
      else
        echo "skipping archive without unzip: $input" | tee -a "$summary" >&2
      fi
    else
      echo "skipping non-apk path: $input" | tee -a "$summary" >&2
    fi
  done
}

apk_count=0
while IFS= read -r apk; do
  if [[ -z "$apk" ]]; then
    continue
  fi

  apk_count=$((apk_count + 1))
  name="$(basename "$apk" .apk)"
  safe_name="$(printf '%s' "$name" | tr -c '[:alnum:]_.-' '_')"
  package_dir="$OUT_DIR/$safe_name"
  mkdir -p "$package_dir"

  echo "== $apk ==" | tee -a "$summary"
  cp "$apk" "$package_dir/input.apk"
  targets=(
    "$package_dir/input.apk"
    "$package_dir/manifest-summary.txt"
    "$package_dir/AndroidManifest.xml"
  )

  analyze_apk_metadata "$package_dir/input.apk" "$package_dir"

  if [[ "$DECOMPILE" == "1" ]]; then
    if command -v apktool >/dev/null 2>&1; then
      apktool d -f "$package_dir/input.apk" -o "$package_dir/apktool" \
        >> "$package_dir/apktool.log" 2>&1 || true
      targets+=("$package_dir/apktool")
    else
      echo "apktool not found" >> "$summary"
    fi

    if command -v jadx >/dev/null 2>&1; then
      jadx $JADX_ARGS -d "$package_dir/jadx" "$package_dir/input.apk" \
        >> "$package_dir/jadx.log" 2>&1 || true
      targets+=("$package_dir/jadx")
    else
      echo "jadx not found" >> "$summary"
    fi
  fi

  scan_paths "${targets[@]}" > "$package_dir/nowbar-scan.txt"
  {
    echo "== $apk =="
    cat "$package_dir/nowbar-scan.txt"
    echo
  } >> "$scan_all"
done < <(collect_apks "$@")

if [[ "$apk_count" -eq 0 ]]; then
  echo "No APK files found." >&2
  exit 2
fi

if [[ -x "$ROOT_DIR/scripts/check_nowbar_key_coverage.sh" ]]; then
  coverage_dir="$OUT_DIR/key-coverage"
  OUT_DIR="$coverage_dir" "$ROOT_DIR/scripts/check_nowbar_key_coverage.sh" "$scan_all" \
    >> "$summary" 2>&1 || true
fi

echo "Local APK scan artifacts written to $OUT_DIR"
echo "Combined scan: $scan_all"
