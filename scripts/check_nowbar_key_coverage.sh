#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="${OUT_DIR:-$ROOT_DIR/artifacts/nowbar-key-coverage/$(date +%Y%m%d-%H%M%S)}"
STRICT="${STRICT:-0}"

key_pattern='android\.ongoingActivity[A-Za-z0-9_.]*|android\.requestPromotedOngoing|android\.permission\.POST_PROMOTED_NOTIFICATIONS|POST_PROMOTED_NOTIFICATIONS|android\.app\.Notification\$OngoingActivityStyle|android\.app\.Notification\$BigTextStyle|android\.app\.Notification\$CallStyle|android\.app\.Notification\$MetricStyle|Notification\$OngoingActivityStyle|Notification\$BigTextStyle|Notification\$CallStyle|Notification\$MetricStyle|android\.callType|android\.callIsVideo|android\.callPersonCompat|android\.callPerson|android\.answerIntent|android\.declineIntent|android\.hangUpIntent|android\.answerColor|android\.declineColor|android\.verificationText|android\.verificationIcon|android\.subText|android\.styledByProgress|android\.progressEndIcon|android\.progressStartIcon|android\.progressTrackerIcon|android\.progressPoints|android\.progressSegments|android\.progressIndeterminate|android\.progressMax|android\.progress|android\.showSmallIcon|android\.substName|android\.showWhen|android\.chronometerCountDown|android\.showChronometer|pde_first_shown_time_ms|pde_first_expanded_time_ms|pde_enqueued_time_ms|pde_noti_clicked_count|pde_noti_action_clicked_count|pde_noti_id|pde_noti_pkg|pde_noti_tag|isCapsule|capsule_layout|capsule_action|capsule_priority|bg_startColor|bg_endColor|com\.samsung\.feature\.nowbar|com\.oplus\.software\.feature\.ambient_alerts|com\.google\.android\.feature\.AMBIENT_DATA'

normalize_keys() {
  sed \
    -e 's/\\\$/$/g' \
    -e 's/^POST_PROMOTED_NOTIFICATIONS$/android.permission.POST_PROMOTED_NOTIFICATIONS/' \
    -e 's/^Notification\$OngoingActivityStyle$/android.app.Notification$OngoingActivityStyle/' \
    -e 's/^Notification\$BigTextStyle$/android.app.Notification$BigTextStyle/' \
    -e 's/^Notification\$CallStyle$/android.app.Notification$CallStyle/' \
    -e 's/^Notification\$MetricStyle$/android.app.Notification$MetricStyle/' |
    awk 'NF' |
    sort -u
}

find_default_scans() {
  find "$ROOT_DIR/artifacts/local-apk-nowbar-scan" "$ROOT_DIR/artifacts/samsung-apk-research" \
    -name 'nowbar-scan-all.txt' -type f 2>/dev/null | sort
}

extract_observed_keys() {
  local input
  local scan_file
  for input in "$@"; do
    if [[ -d "$input" ]]; then
      while IFS= read -r -d '' scan_file; do
        rg --no-filename -o "$key_pattern" "$scan_file" 2>/dev/null || true
      done < <(find "$input" -name 'nowbar-scan-all.txt' -type f -print0)
    elif [[ -f "$input" ]]; then
      rg --no-filename -o "$key_pattern" "$input" 2>/dev/null || true
    else
      echo "Skipping missing input: $input" >&2
    fi
  done | normalize_keys
}

extract_known_keys() {
  rg --no-filename -o '"[^"]+"' "$ROOT_DIR/nowbar/src/main/kotlin" "$ROOT_DIR/demo/src/main/kotlin" 2>/dev/null |
    sed -e 's/^"//' -e 's/"$//' |
    normalize_keys |
    rg "$key_pattern" || true
}

if [[ "$#" -eq 0 ]]; then
  inputs=()
  while IFS= read -r input; do
    inputs+=("$input")
  done < <(find_default_scans)
else
  inputs=("$@")
fi

if [[ "${#inputs[@]}" -eq 0 ]]; then
  echo "No nowbar-scan-all.txt inputs found. Pass scan files or scan directories." >&2
  exit 2
fi

mkdir -p "$OUT_DIR"
observed="$OUT_DIR/observed-keys.txt"
known="$OUT_DIR/sdk-known-keys.txt"
unknown="$OUT_DIR/unknown-observed-keys.txt"
known_observed="$OUT_DIR/known-observed-keys.txt"
known_not_observed="$OUT_DIR/sdk-known-not-observed-keys.txt"
report="$OUT_DIR/report.md"

extract_observed_keys "${inputs[@]}" > "$observed"
extract_known_keys > "$known"

comm -12 "$observed" "$known" > "$known_observed"
comm -23 "$observed" "$known" > "$unknown"
comm -13 "$observed" "$known" > "$known_not_observed"

{
  echo "# Now Bar key coverage"
  echo
  echo "- strict: $STRICT"
  echo "- inputs: ${#inputs[@]}"
  echo "- observed_keys: $(wc -l < "$observed" | tr -d ' ')"
  echo "- sdk_known_keys: $(wc -l < "$known" | tr -d ' ')"
  echo "- known_observed_keys: $(wc -l < "$known_observed" | tr -d ' ')"
  echo "- unknown_observed_keys: $(wc -l < "$unknown" | tr -d ' ')"
  echo "- sdk_known_not_observed_keys: $(wc -l < "$known_not_observed" | tr -d ' ')"
  echo
  echo "## Inputs"
  echo
  printf -- '- %s\n' "${inputs[@]}"
  echo
  echo "## Unknown Observed Keys"
  echo
  if [[ -s "$unknown" ]]; then
    echo '```text'
    cat "$unknown"
    echo '```'
  else
    echo "None."
  fi
  echo
  echo "## Known Observed Keys"
  echo
  echo '```text'
  cat "$known_observed"
  echo '```'
  echo
  echo "## SDK Known Keys Not Seen In These Inputs"
  echo
  if [[ -s "$known_not_observed" ]]; then
    echo '```text'
    cat "$known_not_observed"
    echo '```'
  else
    echo "None."
  fi
} > "$report"

echo "Now Bar key coverage written to $OUT_DIR"
echo "Report: $report"

if [[ "$STRICT" == "1" && -s "$unknown" ]]; then
  echo "Unknown Now Bar keys were observed. See $unknown" >&2
  exit 1
fi
