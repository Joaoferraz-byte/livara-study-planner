#!/usr/bin/env bash
set -Eeuo pipefail

repo_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
app="$repo_root/src/main/java/com/joaoferraz/livara/studyplanner/ui/StudyPlannerApp.java"
css="$repo_root/src/main/resources/style.css"

require() {
  local pattern="$1"
  local file="$2"
  grep -Eq "$pattern" "$file" || {
    printf 'Missing visual contract: %s in %s\n' "$pattern" "$file" >&2
    exit 1
  }
}

require 'private void applyMatugenPalette\(Node root\)' "$app"
require 'showModal\(' "$app"
require 'modalEscapeHandler' "$app"
require 'styleOwnedPopupWindows' "$app"
require '\.root\.popup' "$css"
require '\.popup-root' "$css"
require '\.editor-modal' "$css"
require '\.modal-backdrop' "$css"
require 'background-radius: 0' "$css"
! grep -Eq 'editor-popup|new Popup|stylePopupScene' "$app" "$css"

printf '%s\n' 'Planner visual contracts: OK'
