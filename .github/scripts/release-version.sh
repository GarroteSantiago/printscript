#!/usr/bin/env bash
# Release decisions for the PR-title check and the release workflow (see README.md, "Releasing").
#
#   release-version.sh bump "<pr title>" ["<pr body>"]
#       Prints the release a merged PR produces: major, minor, patch or none. Fails if the title
#       isn't a Conventional Commit.
#         type! (e.g. "feat!: ...") or a "BREAKING CHANGE:" line in the body -> major
#         feat                                                              -> minor
#         fix, perf, refactor, revert                                        -> patch
#         docs, test, ci, build, chore, style                                -> none
#   release-version.sh latest
#       Prints the highest vX.Y.Z release tag in the repository, without the v (0.0.0 if none).
#   release-version.sh next <X.Y.Z> <major|minor|patch>
#       Prints the version after X.Y.Z.
set -euo pipefail

readonly TYPES='feat|fix|perf|refactor|revert|docs|test|ci|build|chore|style'

bump() {
  local title=$1 body=${2:-}
  local pattern="^(${TYPES})(\([a-z0-9._/, -]+\))?(!)?: [^ ].*$"
  if ! [[ $title =~ $pattern ]]; then
    echo "PR title \"$title\" is not a Conventional Commit." >&2
    echo "Use \"<type>(<optional scope>): <summary>\", e.g. \"feat(repl): ...\" or \"fix: ...\"." >&2
    echo "Types: ${TYPES//|/, }. Add ! after the type for a breaking change." >&2
    return 1
  fi
  local type=${BASH_REMATCH[1]} breaking=${BASH_REMATCH[3]}
  if [[ -n $breaking ]] || grep -qE '^BREAKING[ -]CHANGE:' <<<"$body"; then
    echo major
    return
  fi
  case $type in
    feat) echo minor ;;
    fix | perf | refactor | revert) echo patch ;;
    *) echo none ;;
  esac
}

latest() {
  local tag
  tag=$(git tag --list 'v*' | grep -E '^v[0-9]+\.[0-9]+\.[0-9]+$' | sort -V | tail -n 1 || true)
  tag=${tag#v}
  echo "${tag:-0.0.0}"
}

next() {
  local version=$1 kind=$2 major minor patch
  if ! [[ $version =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
    echo "\"$version\" is not a X.Y.Z version." >&2
    return 1
  fi
  major=${BASH_REMATCH[1]} minor=${BASH_REMATCH[2]} patch=${BASH_REMATCH[3]}
  case $kind in
    major) echo "$((major + 1)).0.0" ;;
    minor) echo "$major.$((minor + 1)).0" ;;
    patch) echo "$major.$minor.$((patch + 1))" ;;
    *)
      echo "\"$kind\" is not major, minor or patch." >&2
      return 1
      ;;
  esac
}

case ${1:-} in
  bump) bump "${2:?pr title}" "${3:-}" ;;
  latest) latest ;;
  next) next "${2:?version}" "${3:?major|minor|patch}" ;;
  *)
    echo "usage: $0 bump <title> [body] | latest | next <X.Y.Z> <major|minor|patch>" >&2
    exit 2
    ;;
esac
