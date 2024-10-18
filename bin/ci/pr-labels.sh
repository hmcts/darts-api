#!/bin/bash
set -e

echo "Using token: ***${GITHUB_API_TOKEN: -3}"
echo "Using PR_NUMBER: $CHANGE_ID"

curl -L -i \
  -H "Accept: application/vnd.github+json" \
  -H "Authorization: Bearer $GITHUB_API_TOKEN" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  https://api.github.com/repos/hmcts/darts-api/pulls/$CHANGE_ID

curl -L \
  -H "Accept: application/vnd.github+json" \
  -H "Authorization: Bearer $GITHUB_API_TOKEN" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  https://api.github.com/repos/hmcts/darts-api/pulls/$CHANGE_ID | jq .labels
