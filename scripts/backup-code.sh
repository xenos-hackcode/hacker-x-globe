#!/usr/bin/env bash
# Encrypted off-GitHub backup of the hacker-x-globe repo, to a private GCS
# bucket only the owner's own Google account can read. GitHub is already
# the primary backup (full history); this is defense-in-depth in case that
# account/repo is ever unavailable - a snapshot of the latest commit, not a
# replacement for GitHub's own history.
#
# Run manually: ./scripts/backup-code.sh
# Run in Cloud Build (scheduled): same script, GITHUB_PAT comes from the
# build's secret env instead of a local `gcloud secrets` fetch.
set -euo pipefail

PROJECT="cedal-fd4a2"
BUCKET="gs://cedal-code-backups-fd4a2"
REPO="https://github.com/xenos-hackcode/hacker-x-globe.git"
WORKDIR=$(mktemp -d)
trap 'rm -rf "$WORKDIR"' EXIT

echo "Fetching credentials..."
GITHUB_PAT="${GITHUB_PAT:-$(gcloud secrets versions access latest --secret=GITHUB_PAT --project "$PROJECT")}"
BACKUP_KEY="${CODE_BACKUP_ENCRYPTION_KEY:-$(gcloud secrets versions access latest --secret=CODE_BACKUP_ENCRYPTION_KEY --project "$PROJECT")}"

echo "Cloning latest snapshot..."
git clone --depth 1 "https://x-access-token:${GITHUB_PAT}@github.com/xenos-hackcode/hacker-x-globe.git" "$WORKDIR/repo" --quiet

TIMESTAMP=$(date -u +%Y%m%d-%H%M%S)
ARCHIVE="$WORKDIR/backup-$TIMESTAMP.tar.gz"
ENCRYPTED="$ARCHIVE.enc"

echo "Archiving..."
tar -czf "$ARCHIVE" -C "$WORKDIR/repo" .

echo "Encrypting..."
openssl enc -aes-256-cbc -pbkdf2 -salt -pass "pass:${BACKUP_KEY}" -in "$ARCHIVE" -out "$ENCRYPTED"

echo "Uploading to $BUCKET..."
gcloud storage cp "$ENCRYPTED" "$BUCKET/backup-$TIMESTAMP.tar.gz.enc" --project "$PROJECT"

echo "Done: $BUCKET/backup-$TIMESTAMP.tar.gz.enc"
