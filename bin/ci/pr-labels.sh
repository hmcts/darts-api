#!/bin/bash
set -e

echo "Using PR_NUMBER: $CHANGE_ID"

LABELS_ARRAY=$(curl -L \
  -H "Accept: application/vnd.github+json" \
  -H "Authorization: Bearer $GITHUB_API_TOKEN" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  https://api.github.com/repos/hmcts/darts-api/pulls/$CHANGE_ID | jq .labels)

# if "enable_darts_portal" label is set on the PR, then set the DARTS_PORTAL_REPLICAS to 1, otherwise set it to 0
# DARTS_PORTAL_REPLICAS is used in charts/darts-api/values.dev.template.yaml to set the number of replicas for the DARTS Portal
export DARTS_PORTAL_REPLICAS=$(echo $LABELS_ARRAY | jq | grep '"name": "enable_darts_portal"' | wc -l | jq)
echo "Required DARTS Portal replicas: $DARTS_PORTAL_REPLICAS"
# replace the replicas value in the values.dev.template.yaml file
sed -i '' "s/replicas: 0 #DARTS_PORTAL_REPLICAS/replicas: ${DARTS_PORTAL_REPLICAS}/g" ./charts/darts-api/values.dev.template.yaml