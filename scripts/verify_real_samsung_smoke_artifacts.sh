#!/usr/bin/env bash
set -euo pipefail

SMOKE_DIR="${1:-}"
ALLOW_MISSING_SCREENSHOTS="${ALLOW_MISSING_SCREENSHOTS:-0}"
ALLOW_MISSING_SHADE_SCREENSHOTS="${ALLOW_MISSING_SHADE_SCREENSHOTS:-0}"
REQUIRE_LOCKSCREEN_SCREENSHOTS="${REQUIRE_LOCKSCREEN_SCREENSHOTS:-0}"
VERIFY_SCREENSHOT_FILES="${VERIFY_SCREENSHOT_FILES:-1}"

if [[ -z "$SMOKE_DIR" || ! -d "$SMOKE_DIR" ]]; then
  echo "Usage: $0 <artifacts/real-samsung-smoke/run-dir>" >&2
  exit 2
fi

ACTION_SUMMARY="$SMOKE_DIR/action-summary.tsv"
DEVICE_FILE="$SMOKE_DIR/device.txt"

if [[ ! -f "$ACTION_SUMMARY" ]]; then
  echo "Missing action summary: $ACTION_SUMMARY" >&2
  exit 2
fi

if [[ ! -f "$DEVICE_FILE" ]]; then
  echo "Missing device evidence: $DEVICE_FILE" >&2
  exit 2
fi

is_png_file() {
  local file="$1"
  local signature=""

  if [[ ! -f "$file" ]]; then
    return 1
  fi

  signature="$(dd if="$file" bs=8 count=1 2>/dev/null | od -An -tx1 | tr -d ' \n')"
  [[ "$signature" == "89504e470d0a1a0a" ]]
}

verify_png_file() {
  local file="$1"
  local label="$2"

  if ! is_png_file "$file"; then
    echo "FAIL: $label is missing or is not a PNG file: $file" >&2
    return 1
  fi
}

verify_screenshot_files() {
  local header=""
  local has_shade_screenshots=0
  local has_lockscreen_screenshots=0
  local failures=0
  local safe_name=""
  local screenshot_bytes=""
  local shade_screenshot_bytes=""
  local lock_screenshot_bytes=""

  [[ "$VERIFY_SCREENSHOT_FILES" == "1" ]] || return 0

  IFS= read -r header < "$ACTION_SUMMARY"
  if [[ "$header" == *$'\tshade_screenshot_bytes'* ]]; then
    has_shade_screenshots=1
  fi
  if [[ "$header" == *$'\tlock_screenshot_bytes'* ]]; then
    has_lockscreen_screenshots=1
  fi

  while IFS=$'\t' read -r safe_name _action _am_status _am_error_refs _package_refs _request_promoted_refs _samsung_extra_refs _samsung_style_refs _progress_refs _metric_refs _native_style_refs _capsule_refs screenshot_bytes shade_screenshot_bytes lock_screenshot_bytes _rest; do
    [[ -n "$safe_name" ]] || continue

    if [[ "$ALLOW_MISSING_SCREENSHOTS" != "1" && "${screenshot_bytes:-0}" != "0" ]]; then
      verify_png_file "$SMOKE_DIR/screen-$safe_name.png" "$safe_name app screenshot" || failures=$((failures + 1))
    fi

    if [[ "$has_shade_screenshots" == "1" && "$ALLOW_MISSING_SHADE_SCREENSHOTS" != "1" && "${shade_screenshot_bytes:-0}" != "0" ]]; then
      verify_png_file "$SMOKE_DIR/screen-$safe_name-shade.png" "$safe_name notification shade screenshot" || failures=$((failures + 1))
    fi

    if [[ "$has_lockscreen_screenshots" == "1" && "$REQUIRE_LOCKSCREEN_SCREENSHOTS" == "1" ]]; then
      verify_png_file "$SMOKE_DIR/screen-$safe_name-lock.png" "$safe_name lockscreen screenshot" || failures=$((failures + 1))
    fi
  done < <(tail -n +2 "$ACTION_SUMMARY")

  if [[ "$failures" -gt 0 ]]; then
    exit 1
  fi
}

awk -F '\t' \
  -v allow_missing_screenshots="$ALLOW_MISSING_SCREENSHOTS" \
  -v allow_missing_shade_screenshots="$ALLOW_MISSING_SHADE_SCREENSHOTS" \
  -v require_lockscreen_screenshots="$REQUIRE_LOCKSCREEN_SCREENSHOTS" '
function fail(message) {
  failures++
  print "FAIL: " message
}

function require_positive(value, message) {
  if ((value + 0) <= 0) {
    fail(message " (got " value ")")
  }
}

function require_zero(value, message) {
  if ((value + 0) != 0) {
    fail(message " (got " value ")")
  }
}

BEGIN {
  split("SHOW_TIMER SHOW_WORKOUT SHOW_NAVIGATION SHOW_DELIVERY SHOW_METRICS SHOW_MEDIA SHOW_CALL SHOW_CALL_SCREENING SHOW_BIG_TEXT SHOW_CUSTOM_ACTIONS SHOW_DUMP SHOW_NATIVE_STYLE PAUSE RESUME NEXT UNPIN DISMISS STOP", all_actions, " ")
  split("SHOW_TIMER SHOW_WORKOUT SHOW_NAVIGATION SHOW_DELIVERY SHOW_METRICS SHOW_MEDIA SHOW_CALL SHOW_CALL_SCREENING SHOW_BIG_TEXT SHOW_CUSTOM_ACTIONS PAUSE RESUME NEXT", normal_actions, " ")
  split("SHOW_TIMER SHOW_WORKOUT SHOW_NAVIGATION SHOW_DELIVERY SHOW_METRICS SHOW_MEDIA SHOW_CALL SHOW_CALL_SCREENING SHOW_BIG_TEXT SHOW_CUSTOM_ACTIONS SHOW_DUMP SHOW_NATIVE_STYLE PAUSE RESUME NEXT UNPIN", active_actions, " ")

  for (i in all_actions) expected[all_actions[i]] = 1
  for (i in normal_actions) normal[normal_actions[i]] = 1
  for (i in active_actions) active[active_actions[i]] = 1
}

NR == 1 {
  if ($1 != "safe_name" || $2 != "action" || $5 != "package_refs" || $13 != "screenshot_bytes") {
    fail("unexpected action-summary.tsv header")
  }
  if (NF > 13 && $14 != "shade_screenshot_bytes") {
    fail("unexpected shade screenshot column in action-summary.tsv header")
  }
  if (NF > 14 && $15 != "lock_screenshot_bytes") {
    fail("unexpected lockscreen screenshot column in action-summary.tsv header")
  }
  if (NF > 15 && $16 != "big_text_refs") {
    fail("unexpected big text column in action-summary.tsv header")
  }
  if (NF > 16 && $17 != "call_refs") {
    fail("unexpected call style column in action-summary.tsv header")
  }
  if (NF > 17 && $18 != "status_chip_refs") {
    fail("unexpected status chip column in action-summary.tsv header")
  }
  if (NF > 18 && $19 != "subtext_refs") {
    fail("unexpected subtext column in action-summary.tsv header")
  }
  if (NF > 19 && $20 != "aod_remote_app_refs") {
    fail("unexpected AOD remote app column in action-summary.tsv header")
  }
  if (NF > 20 && $21 != "remote_view_refs") {
    fail("unexpected remote view column in action-summary.tsv header")
  }
  if (NF > 21 && $22 != "dump_visual_refs") {
    fail("unexpected dump visual column in action-summary.tsv header")
  }
  if (NF > 22 && $23 != "delete_intent_refs") {
    fail("unexpected delete intent column in action-summary.tsv header")
  }
  if (NF > 23 && $24 != "content_intent_refs") {
    fail("unexpected content intent column in action-summary.tsv header")
  }
  if (NF > 24 && $25 != "unpin_refs") {
    fail("unexpected unpin column in action-summary.tsv header")
  }
  if (NF > 25 && $26 != "samsung_chronometer_refs") {
    fail("unexpected Samsung chronometer column in action-summary.tsv header")
  }
  if (NF > 26 && $27 != "native_style_report_refs") {
    fail("unexpected native style report column in action-summary.tsv header")
  }
  if (NF > 27 && $28 != "action_button_refs") {
    fail("unexpected action button column in action-summary.tsv header")
  }
  if (NF > 28 && $29 != "text_only_action_refs") {
    fail("unexpected text-only action column in action-summary.tsv header")
  }
  if (NF > 29 && $30 != "disabled_action_refs") {
    fail("unexpected disabled action column in action-summary.tsv header")
  }
  if (NF > 30 && $31 != "action_metadata_refs") {
    fail("unexpected action metadata column in action-summary.tsv header")
  }
  has_shade_screenshots = ($14 == "shade_screenshot_bytes")
  has_lockscreen_screenshots = ($15 == "lock_screenshot_bytes")
  has_status_chip_refs = ($18 == "status_chip_refs")
  has_subtext_refs = ($19 == "subtext_refs")
  has_aod_remote_app_refs = ($20 == "aod_remote_app_refs")
  has_remote_view_refs = ($21 == "remote_view_refs")
  has_dump_visual_refs = ($22 == "dump_visual_refs")
  has_delete_intent_refs = ($23 == "delete_intent_refs")
  has_content_intent_refs = ($24 == "content_intent_refs")
  has_unpin_refs = ($25 == "unpin_refs")
  has_samsung_chronometer_refs = ($26 == "samsung_chronometer_refs")
  has_native_style_report_refs = ($27 == "native_style_report_refs")
  has_action_button_refs = ($28 == "action_button_refs")
  has_text_only_action_refs = ($29 == "text_only_action_refs")
  has_disabled_action_refs = ($30 == "disabled_action_refs")
  has_action_metadata_refs = ($31 == "action_metadata_refs")
  if (require_lockscreen_screenshots == "1" && !has_lockscreen_screenshots) {
    fail("lockscreen screenshot column is required")
  }
  next
}

NF > 0 {
  safe_name = $1
  seen[safe_name] = 1
  rows++

  require_zero($4, safe_name " am start should not contain error refs")

  if (allow_missing_screenshots != "1") {
    require_positive($13, safe_name " screenshot should be captured")
  }

  if (has_shade_screenshots && allow_missing_shade_screenshots != "1") {
    require_positive($14, safe_name " notification shade screenshot should be captured")
  }

  if (require_lockscreen_screenshots == "1") {
    require_positive($15, safe_name " lockscreen screenshot should be captured")
  }

  if (safe_name in active) {
    require_positive($5, safe_name " should leave a package-owned notification in dumpsys")
    if (has_delete_intent_refs) {
      require_positive($23, safe_name " should expose delete intent refs")
    }
    if (has_content_intent_refs) {
      require_positive($24, safe_name " should expose content intent refs")
    }
  }

  if (safe_name in normal) {
    if ((($6 + 0) + ($7 + 0)) <= 0) {
      fail(safe_name " should expose Android promoted refs or Samsung ongoingActivity extras")
    }
    if (has_aod_remote_app_refs && ($7 + 0) > 0) {
      require_positive($20, safe_name " should expose AOD remote app refs on the Samsung extras path")
    }
  }

  if (safe_name == "SHOW_DUMP") {
    require_positive($7, "SHOW_DUMP should expose Samsung ongoingActivity extras")
    require_positive($11, "SHOW_DUMP should expose OngoingActivityStyle/native template refs")
    if (has_aod_remote_app_refs) {
      require_positive($20, "SHOW_DUMP should expose AOD remote app refs")
    }
    if (has_remote_view_refs) {
      require_positive($21, "SHOW_DUMP should expose Samsung RemoteViews refs")
    }
    if (has_dump_visual_refs) {
      require_positive($22, "SHOW_DUMP should expose Samsung visual dump refs")
    }
    if (has_samsung_chronometer_refs) {
      require_positive($26, "SHOW_DUMP should expose Samsung dump chronometer refs")
    }
  }

  if (safe_name == "SHOW_NATIVE_STYLE") {
    if ((($6 + 0) + ($11 + 0)) <= 0) {
      fail("SHOW_NATIVE_STYLE should expose promoted ongoing or native style refs")
    }
    if (has_native_style_report_refs) {
      require_positive($27, "SHOW_NATIVE_STYLE should expose hidden style build report refs")
    }
  }

  if (safe_name == "SHOW_METRICS") {
    if ((($10 + 0) + ($6 + 0) + ($7 + 0)) <= 0) {
      fail("SHOW_METRICS should expose metric, promoted ongoing, or Samsung extras refs")
    }
  }

  if ((safe_name == "SHOW_DELIVERY" || safe_name == "SHOW_METRICS") && has_subtext_refs) {
    require_positive($19, safe_name " should expose Live Update subtext refs")
  }

  if ((safe_name == "SHOW_TIMER" || safe_name == "SHOW_DELIVERY" ||
      safe_name == "SHOW_METRICS" || safe_name == "SHOW_CUSTOM_ACTIONS" ||
      safe_name == "PAUSE" || safe_name == "RESUME" || safe_name == "NEXT") &&
      has_status_chip_refs) {
    require_positive($18, safe_name " should expose status chip refs")
  }

  if (safe_name == "SHOW_BIG_TEXT") {
    require_positive($16, "SHOW_BIG_TEXT should expose BigTextStyle refs")
    if (has_action_button_refs) {
      require_positive($28, "SHOW_BIG_TEXT should expose action button refs")
    }
    if (has_action_metadata_refs) {
      require_positive($31, "SHOW_BIG_TEXT should expose action metadata refs")
    }
  }

  if (safe_name == "SHOW_DELIVERY") {
    if (has_action_button_refs) {
      require_positive($28, "SHOW_DELIVERY should expose action button refs")
    }
    if (has_text_only_action_refs) {
      require_positive($29, "SHOW_DELIVERY should expose text-only action refs")
    }
    if (has_disabled_action_refs) {
      require_positive($30, "SHOW_DELIVERY should expose disabled action refs")
    }
    if (has_action_metadata_refs) {
      require_positive($31, "SHOW_DELIVERY should expose action metadata refs")
    }
  }

  if (safe_name == "SHOW_METRICS" && has_action_button_refs) {
    require_positive($28, "SHOW_METRICS should expose action button refs")
  }
  if (safe_name == "SHOW_METRICS" && has_action_metadata_refs) {
    require_positive($31, "SHOW_METRICS should expose action metadata refs")
  }

  if (safe_name == "SHOW_CUSTOM_ACTIONS" && has_unpin_refs) {
    if (has_action_button_refs) {
      require_positive($28, "SHOW_CUSTOM_ACTIONS should expose action button refs")
    }
    if (has_action_metadata_refs) {
      require_positive($31, "SHOW_CUSTOM_ACTIONS should expose action metadata refs")
    }
    require_positive($25, "SHOW_CUSTOM_ACTIONS should expose Unpin action refs")
  }

  if (safe_name == "PAUSE" || safe_name == "RESUME" || safe_name == "NEXT" || safe_name == "UNPIN") {
    if (has_action_button_refs) {
      require_positive($28, safe_name " should preserve custom control action refs")
    }
    if (has_action_metadata_refs) {
      require_positive($31, safe_name " should preserve custom control action metadata refs")
    }
    if (has_unpin_refs) {
      require_positive($25, safe_name " should preserve Unpin action refs")
    }
  }

  if (safe_name == "UNPIN") {
    require_zero($6, "UNPIN should remove Android promoted refs")
    require_zero($7, "UNPIN should remove Samsung ongoingActivity extras")
    require_zero($11, "UNPIN should remove Samsung native style refs")
  }

  if (safe_name == "SHOW_CALL" || safe_name == "SHOW_CALL_SCREENING") {
    require_positive($17, safe_name " should expose CallStyle refs")
  }
}

END {
  for (name in expected) {
    if (!(name in seen)) {
      fail("missing action row for " name)
    }
  }

  if (rows == 0) {
    fail("action-summary.tsv has no action rows")
  }

  print "rows=" rows
  print "failures=" failures + 0

  if (failures > 0) {
    exit 1
  }
}
' "$ACTION_SUMMARY"

if ! grep -qi '^manufacturer=.*samsung' "$DEVICE_FILE"; then
  echo "FAIL: device.txt does not identify a Samsung manufacturer" >&2
  exit 1
fi

verify_screenshot_files

echo "Samsung smoke artifact verification passed: $SMOKE_DIR"
