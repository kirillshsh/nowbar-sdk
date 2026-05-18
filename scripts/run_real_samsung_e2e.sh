#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUNDLE_DIR="${BUNDLE_DIR:-$ROOT_DIR/artifacts/real-samsung-e2e/$(date +%Y%m%d-%H%M%S)}"
RUN_APK_RESEARCH="${RUN_APK_RESEARCH:-1}"
APK_RESEARCH_DECOMPILE="${APK_RESEARCH_DECOMPILE:-1}"
CONTINUE_AFTER_SMOKE_FAILURE="${CONTINUE_AFTER_SMOKE_FAILURE:-0}"

mkdir -p "$BUNDLE_DIR"

smoke_dir="$BUNDLE_DIR/smoke"
apk_research_dir="$BUNDLE_DIR/apk-research"
smoke_status="not-run"
apk_research_status="not-run"

write_host_snapshot() {
  {
    echo "date=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    echo "root=$ROOT_DIR"
    echo "bundle=$BUNDLE_DIR"
    echo "run_apk_research=$RUN_APK_RESEARCH"
    echo "apk_research_decompile=$APK_RESEARCH_DECOMPILE"
    echo "continue_after_smoke_failure=$CONTINUE_AFTER_SMOKE_FAILURE"
    echo "capture_notification_shade=${CAPTURE_NOTIFICATION_SHADE:-1}"
    echo "capture_lockscreen=${CAPTURE_LOCKSCREEN:-0}"
    echo "allow_missing_shade_screenshots=${ALLOW_MISSING_SHADE_SCREENSHOTS:-0}"
    echo "require_lockscreen_screenshots=${REQUIRE_LOCKSCREEN_SCREENSHOTS:-0}"
    echo "adb_connect=${ADB_CONNECT:-}"
    echo "serial=${SERIAL:-}"
    echo "wait_for_device_seconds=${WAIT_FOR_DEVICE_SECONDS:-0}"
    echo
    echo "adb:"
    command -v adb || true
    adb version 2>/dev/null || true
    echo
    echo "connected devices:"
    adb devices -l 2>/dev/null || true
    echo
    echo "adb mdns services:"
    adb mdns services 2>/dev/null || true
    echo
    echo "tool availability:"
    for tool in apkanalyzer apktool jadx rg; do
      if command -v "$tool" >/dev/null 2>&1; then
        printf '%s=%s\n' "$tool" "$(command -v "$tool")"
      else
        printf '%s=missing\n' "$tool"
      fi
    done
  } > "$BUNDLE_DIR/host.txt"
}

write_summary() {
  local status="$1"
  local summary="$BUNDLE_DIR/summary.md"

  {
    echo "# Real Samsung Now Bar E2E Evidence"
    echo
    echo "- status: $status"
    echo "- bundle: $BUNDLE_DIR"
    echo "- smoke_status: $smoke_status"
    echo "- apk_research_status: $apk_research_status"
    echo "- run_apk_research: $RUN_APK_RESEARCH"
    echo "- apk_research_decompile: $APK_RESEARCH_DECOMPILE"
    echo "- continue_after_smoke_failure: $CONTINUE_AFTER_SMOKE_FAILURE"
    echo "- capture_notification_shade: ${CAPTURE_NOTIFICATION_SHADE:-1}"
    echo "- capture_lockscreen: ${CAPTURE_LOCKSCREEN:-0}"
    echo "- allow_missing_shade_screenshots: ${ALLOW_MISSING_SHADE_SCREENSHOTS:-0}"
    echo "- require_lockscreen_screenshots: ${REQUIRE_LOCKSCREEN_SCREENSHOTS:-0}"
    echo "- adb_connect: ${ADB_CONNECT:-}"
    echo "- serial: ${SERIAL:-}"
    echo "- wait_for_device_seconds: ${WAIT_FOR_DEVICE_SECONDS:-0}"
    echo
    echo "## Files"
    echo
    echo "- host.txt"
    echo "- smoke.log"
    echo "- smoke/"
    echo "- apk-research.log"
    echo "- apk-research/"
    echo
    if [[ -f "$smoke_dir/summary.md" ]]; then
      echo "## Smoke Summary"
      echo
      sed -n '1,220p' "$smoke_dir/summary.md"
      echo
    fi
    if [[ -f "$smoke_dir/artifact-verification.txt" ]]; then
      echo "## Smoke Artifact Verification"
      echo
      echo '```text'
      sed -n '1,200p' "$smoke_dir/artifact-verification.txt"
      echo '```'
      echo
    fi
    if [[ -f "$apk_research_dir/summary.txt" ]]; then
      echo "## APK Research Summary"
      echo
      echo '```text'
      sed -n '1,220p' "$apk_research_dir/summary.txt"
      echo '```'
      echo
    fi
    if [[ -f "$apk_research_dir/key-coverage/report.md" ]]; then
      echo "## APK Key Coverage"
      echo
      sed -n '1,220p' "$apk_research_dir/key-coverage/report.md"
      echo
    fi
  } > "$summary"
}

on_exit() {
  local exit_code=$?
  set +e
  if [[ "$exit_code" -eq 0 ]]; then
    write_summary "completed"
  else
    write_summary "failed"
  fi
}

trap on_exit EXIT

write_host_snapshot

mkdir -p "$smoke_dir"
OUT_DIR="$smoke_dir" "$ROOT_DIR/scripts/real_samsung_smoke.sh" \
  > "$BUNDLE_DIR/smoke.log" 2>&1 || smoke_status=$?
if [[ "$smoke_status" == "not-run" ]]; then
  smoke_status=0
fi

if [[ "$smoke_status" != "0" && "$CONTINUE_AFTER_SMOKE_FAILURE" != "1" ]]; then
  echo "Smoke failed with exit code $smoke_status. See $BUNDLE_DIR/smoke.log" >&2
  exit "$smoke_status"
fi

if [[ "$RUN_APK_RESEARCH" == "1" ]]; then
  mkdir -p "$apk_research_dir"
  OUT_DIR="$apk_research_dir" DECOMPILE="$APK_RESEARCH_DECOMPILE" \
    "$ROOT_DIR/scripts/pull_decompile_samsung_apks.sh" \
    > "$BUNDLE_DIR/apk-research.log" 2>&1 || apk_research_status=$?
  if [[ "$apk_research_status" == "not-run" ]]; then
    apk_research_status=0
  fi
fi

if [[ "$smoke_status" != "0" ]]; then
  exit "$smoke_status"
fi

if [[ "$apk_research_status" != "not-run" && "$apk_research_status" != "0" ]]; then
  exit "$apk_research_status"
fi

echo "Real Samsung E2E evidence written to $BUNDLE_DIR"
