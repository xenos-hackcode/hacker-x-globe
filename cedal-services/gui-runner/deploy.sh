#!/usr/bin/env bash
# Deploys gui-runner - one region, up to 10 concurrent sessions (real
# multi-user product, not a personal single-session tool). concurrency=1
# stays - one session still needs a whole instance to itself (its own
# Xvfb/x11vnc/websockify/python tree) - but max-instances now lets Cloud Run
# actually spin up more than one of those at once. --session-affinity routes
# a given client's later requests back to the SAME instance via a cookie
# Cloud Run sets on the first response; see GuiSessionService.kt server-side
# for why that cookie has to be explicitly relayed one hop further (the
# request that establishes it is cedal-server's own server-to-server call,
# not the Android client that actually needs to stay pinned).
set -euo pipefail

cd "$(dirname "$0")"

PROJECT_ID="cedal-fd4a2"
SERVICE_NAME="gui-runner"
REGION="us-central1"

gcloud run deploy "$SERVICE_NAME" \
  --source . \
  --project "$PROJECT_ID" \
  --region "$REGION" \
  --platform managed \
  --allow-unauthenticated \
  --min-instances=0 \
  --max-instances=10 \
  --concurrency=1 \
  --session-affinity \
  --memory=512Mi \
  --cpu=1 \
  --timeout=620 \
  --set-secrets=GUI_RUNNER_SECRET=GUI_RUNNER_SECRET:latest

echo ""
URL=$(gcloud run services describe "$SERVICE_NAME" --project "$PROJECT_ID" --region "$REGION" --format='value(status.url)')
echo "Service URL: ${URL}"
