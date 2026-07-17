#!/usr/bin/env bash
# Builds and deploys the android-builder service to a single region (us-central1,
# matching the default region of the Firebase Functions client in src/api/firebase.ts).
# Unlike the code-runner tiers, this isn't multi-region: builds take minutes
# regardless of region, and it's only ever called server-to-server from
# functions/src/requestAndroidBuild.ts, never directly by the mobile client.
set -euo pipefail

cd "$(dirname "$0")"

PROJECT_ID="cedal-fd4a2"
SERVICE_NAME="android-builder"
IMAGE="gcr.io/${PROJECT_ID}/${SERVICE_NAME}"
REGION="us-central1"

echo "Building and pushing image via Cloud Build (this is a large image, expect several minutes)..."
gcloud builds submit --tag "$IMAGE" --project "$PROJECT_ID" --timeout=1800s

echo "Deploying to ${REGION}..."
gcloud run deploy "$SERVICE_NAME" \
  --image "$IMAGE" \
  --project "$PROJECT_ID" \
  --region "$REGION" \
  --platform managed \
  --allow-unauthenticated \
  --set-secrets=ANDROID_BUILDER_SECRET=ANDROID_BUILDER_SECRET:latest \
  --min-instances=0 \
  --max-instances=2 \
  --concurrency=1 \
  --memory=4Gi \
  --cpu=4 \
  --no-cpu-throttling \
  --timeout=600s

URL=$(gcloud run services describe "$SERVICE_NAME" \
  --project "$PROJECT_ID" --region "$REGION" --format='value(status.url)')

echo ""
echo "Service URL: ${URL}"
echo "Set this as the ANDROID_BUILDER_URL secret:"
echo "  firebase functions:secrets:set ANDROID_BUILDER_URL"
echo "(paste ${URL} when prompted, then redeploy functions)"
