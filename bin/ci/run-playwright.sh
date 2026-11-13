#!/usr/bin/env bash
set -eo pipefail

case "${1:-}" in
  setup|test|merge) mode="$1" ;;
  *) echo 'Usage: run-playwright.sh setup|test|merge' >&2; exit 2 ;;
esac

export NVM_DIR=/home/jenkinsssh/.nvm
source /opt/nvm/nvm.sh
export PATH="$HOME/.local/bin:$PATH"

if [[ "$mode" == setup ]]; then
  nvm install
  mkdir -p "$HOME/.local/bin"
  corepack enable --install-directory "$HOME/.local/bin"
  yarn install --immutable
  yarn playwright install chromium
  exit 0
fi

nvm use
if [[ ! "${PW_SHARD_TOTAL:-}" =~ ^[1-9][0-9]*$ ]]; then
  echo 'PW_SHARD_TOTAL must be a positive integer' >&2
  exit 2
fi

if [[ "$mode" == test ]]; then
  if [[ ! "${PW_SHARD_INDEX:-}" =~ ^[1-9][0-9]*$ ]] || (( PW_SHARD_INDEX > PW_SHARD_TOTAL )); then
    echo 'PW_SHARD_INDEX must be between 1 and PW_SHARD_TOTAL' >&2
    exit 2
  fi
  : "${DARTS_PORTAL:?Jenkins must provide the portal URL}"
  : "${AUTOMATION_USERNAME:?Jenkins must provide the login username}"
  : "${AUTOMATION_PASSWORD:?Jenkins must provide the login password}"
  export PLAYWRIGHT_BLOB_OUTPUT_DIR="$PWD/shard-$PW_SHARD_INDEX/blob-report"
  yarn test:e2e --shard="$PW_SHARD_INDEX/$PW_SHARD_TOTAL" \
    --output="$PWD/shard-$PW_SHARD_INDEX/test-results" --reporter=list,blob
  exit 0
fi

mkdir -p all-blob-reports
missing_report=0
shopt -s nullglob
for (( shard=1; shard<=PW_SHARD_TOTAL; shard++ )); do
  reports=("shard-$shard"/blob-report/*.zip)
  if (( ${#reports[@]} == 0 )); then
    echo "Missing blob report for shard $shard" >&2
    missing_report=1
  else
    cp "${reports[@]}" all-blob-reports/
  fi
done

# An explicit test root also supports merging reports produced in different workspaces.
printf '%s\n' "module.exports = { testDir: require('node:path').resolve('e2e/tests') };" > merge.config.cjs
export PLAYWRIGHT_HTML_OUTPUT_DIR=playwright-report
export PLAYWRIGHT_HTML_OPEN=never
export PLAYWRIGHT_JSON_OUTPUT_NAME=report-results.json
yarn playwright merge-reports --config=merge.config.cjs --reporter=html,json all-blob-reports
exit "$missing_report"
