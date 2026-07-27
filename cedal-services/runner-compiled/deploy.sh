#!/usr/bin/env bash
# Builds the code-runner-compiled image once and deploys it to Cloud Run in the
# same 3 regions as runner/deploy.sh. Heavier image (gcc/g++/go/rustc/JDK/.NET),
# so it gets more memory/CPU and a longer timeout than the interpreted tier.
set -euo pipefail

cd "$(dirname "$0")"

PROJECT_ID="cedal-fd4a2"
SERVICE_NAME="code-runner-compiled"
IMAGE="gcr.io/${PROJECT_ID}/${SERVICE_NAME}"
REGIONS=("europe-west2" "europe-west3" "us-east1")

echo "Building and pushing image via Cloud Build..."
gcloud builds submit --tag "$IMAGE" --project "$PROJECT_ID"

for REGION in "${REGIONS[@]}"; do
  echo "Deploying to ${REGION}..."
  gcloud run deploy "$SERVICE_NAME" \
    --image "$IMAGE" \
    --project "$PROJECT_ID" \
    --region "$REGION" \
    --platform managed \
    --allow-unauthenticated \
    --min-instances=0 \
    --max-instances=3 \
    --concurrency=1 \
    --memory=1Gi \
    --cpu=2 \
    --timeout=45s
done

echo ""
echo "Service URLs (copy these into src/api/runCode.ts REGION_URLS.compiled):"
for REGION in "${REGIONS[@]}"; do
  URL=$(gcloud run services describe "$SERVICE_NAME" \
    --project "$PROJECT_ID" --region "$REGION" --format='value(status.url)')
  echo "  ${REGION}: ${URL}"
done
