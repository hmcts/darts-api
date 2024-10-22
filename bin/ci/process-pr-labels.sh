#!/bin/bash
set -e

echo "Using PR_NUMBER: $CHANGE_ID"

LABELS_ARRAY=$(curl -L \
  -H "Accept: application/vnd.github+json" \
  -H "Authorization: Bearer $GITHUB_API_TOKEN" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  https://api.github.com/repos/hmcts/darts-api/pulls/$CHANGE_ID | jq .labels)

# don't override unless the `enable_keep_helm` label is set
ENABLE_KEEP_HELM=$(echo $LABELS_ARRAY | jq | grep '"name": "enable_keep_helm"' | wc -l | jq)
if [[ ENABLE_KEEP_HELM -eq 0 ]]; then
  echo "enable_keep_helm label not found, exiting..."
  exit 1
fi

# used to override files within the charts directory, depends on labels set on the PR
CHART_OVERRIDE=''

# check for known labels on the PR, setting the value to 1 or 0 depending on whether the label is present or not
ENABLE_DARTS_FULLSTACK=$(echo $LABELS_ARRAY | jq | grep '"name": "enable_darts_fullstack"' | wc -l | jq)
ENABLE_DARTS_PORTAL=$(echo $LABELS_ARRAY | jq | grep '"name": "enable_darts_portal"' | wc -l | jq)

if [[ ENABLE_DARTS_FULLSTACK -eq 1 ]]; then
  echo "Using DARTS fullstack dev deployment is not yet supported, using deployment with portal"
  CHART_OVERRIDE='enable_darts_portal' # set to enable_darts_fullstack when supported
elif [[ ENABLE_DARTS_PORTAL -eq 1 ]]; then
  CHART_OVERRIDE='enable_darts_portal'
fi

if [[ ! -z "${CHART_OVERRIDE}" ]]; then
  echo "Using chart override: $CHART_OVERRIDE"
  cp ./charts-dev-overrides/$CHART_OVERRIDE/*.* ./charts/darts-api/
else
  echo "Using default dev deployment chart"
fi
