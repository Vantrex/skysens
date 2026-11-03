#!/usr/bin/env bash
# Builds the changelog for a Modrinth version / GitHub release and writes it to stdout.
#
# 1. Release notes: if the pushed commit merged a promotion PR (head branch development or beta),
#    that PR's description is used as hand-written release notes.
# 2. Changes: every pull request merged since the latest v* release tag, grouped by branch prefix
#    (feature/ -> New features, fix/ and bugfix/ -> Fixes, everything else -> Other).
#    Promotion PRs (development, beta, master) are left out.
# If neither yields anything, the pushed commit's message is used.
#
# Needs: git history with tags (fetch-depth: 0), gh with GH_TOKEN, GITHUB_REPOSITORY and GITHUB_SHA.
set -euo pipefail

SHA="${GITHUB_SHA:-$(git rev-parse HEAD)}"
LATEST_TAG=$(git tag -l 'v[0-9]*.[0-9]*.[0-9]*' --sort=-v:refname | grep -E '^v[0-9]+\.[0-9]+\.[0-9]+$' | head -n1 || true)
RANGE="${LATEST_TAG:+$LATEST_TAG..}$SHA"

notes=""
if [ -n "${GITHUB_REPOSITORY:-}" ]; then
  notes=$(gh api "repos/$GITHUB_REPOSITORY/commits/$SHA/pulls" \
    --jq '[.[] | select(.head.ref == "development" or .head.ref == "beta")][0].body // ""' 2>/dev/null || true)
  notes=$(printf '%s' "$notes" | tr -d '\r')
fi

features="" fixes="" other=""
while IFS= read -r -d '' hash; do
  subject=$(git log -1 --format=%s "$hash")
  [[ "$subject" =~ ^Merge\ pull\ request\ \#([0-9]+)\ from\ [^/]+/(.+)$ ]] || continue
  number="${BASH_REMATCH[1]}"
  branch="${BASH_REMATCH[2]}"
  case "$branch" in development | beta | master) continue ;; esac
  title=$(git log -1 --format=%b "$hash" | tr -d '\r' | sed '/^[[:space:]]*$/d' | head -n1)
  [ -n "$title" ] || title="$branch"
  line="- $title (#$number)"$'\n'
  case "$branch" in
    feature/*) features+="$line" ;;
    fix/* | bugfix/*) fixes+="$line" ;;
    *) other+="$line" ;;
  esac
done < <(git log --merges --format='%H%x00' "$RANGE" | tr -d '\n')

out=""
[ -n "$notes" ] && out+="$notes"$'\n\n'
if [ -n "$features$fixes$other" ]; then
  out+="## Changes${LATEST_TAG:+ since $LATEST_TAG}"$'\n\n'
  [ -n "$features" ] && out+="### New features"$'\n'"$features"$'\n'
  [ -n "$fixes" ] && out+="### Fixes"$'\n'"$fixes"$'\n'
  [ -n "$other" ] && out+="### Other"$'\n'"$other"$'\n'
fi
[ -n "$out" ] || out=$(git log -1 --format=%B "$SHA")

printf '%s\n' "$out"
