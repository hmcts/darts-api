#!/usr/bin/env bash
set +x
set -eo pipefail

case "${1:-}" in
  setup|test|merge) mode="$1" ;;
  *) echo 'Usage: run-playwright.sh setup|test|merge' >&2; exit 2 ;;
esac

if [[ "$mode" != setup ]]; then
  if [[ ! "${PW_SHARD_TOTAL:-}" =~ ^[1-9][0-9]*$ ]]; then
    echo 'PW_SHARD_TOTAL must be a positive integer' >&2
    exit 2
  fi
fi

if [[ "$mode" == test ]]; then
  if [[ ! "${PW_SHARD_INDEX:-}" =~ ^[1-9][0-9]*$ ]] || (( PW_SHARD_INDEX > PW_SHARD_TOTAL )); then
    echo 'PW_SHARD_INDEX must be between 1 and PW_SHARD_TOTAL' >&2
    exit 2
  fi
  required_variables=(
    DARTS_PORTAL DARTS_API AUTOMATION_USERNAME AUTOMATION_PASSWORD
    AAD_B2C_ROPC_CLIENT_ID AAD_B2C_ROPC_CLIENT_SECRET
    AZURE_AD_FUNCTIONAL_TEST_GLOBAL_USERNAME AZURE_AD_FUNCTIONAL_TEST_GLOBAL_PASSWORD
    E2E_AUDIO_INBOUND_SAS_URL
  )
  missing_environment=0
  for variable_name in "${required_variables[@]}"; do
    variable_value="${!variable_name:-}"
    if [[ ! "$variable_value" =~ [^[:space:]] ]]; then
      echo "Jenkins must provide $variable_name" >&2
      missing_environment=1
    fi
  done
  if (( missing_environment )); then
    exit 2
  fi
  if [[ ! "$DARTS_PORTAL" =~ ^https://darts-portal-api-pr-([1-9][0-9]*)\.dev\.platform\.hmcts\.net$ ]]; then
    echo 'DARTS_PORTAL must be an API-owned PR portal HTTPS origin' >&2
    exit 2
  fi
  expected_api="https://darts-api-pr-${BASH_REMATCH[1]}.dev.platform.hmcts.net"
  if [[ "${DARTS_API%/}" != "$expected_api" ]]; then
    echo 'DARTS_API must be the HTTPS origin for the same PR as DARTS_PORTAL' >&2
    exit 2
  fi
fi

export NVM_DIR=/home/jenkinsssh/.nvm
# Defer activation until install/use below; a fresh agent may not have the .nvmrc version yet.
source /opt/nvm/nvm.sh --no-use
export PATH="$HOME/.local/bin:$PATH"

if [[ "$mode" == setup ]]; then
  nvm install
  mkdir -p "$HOME/.local/bin"
  corepack enable --install-directory "$HOME/.local/bin"
  yarn install --immutable
  yarn typecheck:e2e
  yarn playwright install chromium
  exit 0
fi

nvm use

if [[ "$mode" == test ]]; then
  export PLAYWRIGHT_BLOB_OUTPUT_DIR="$PWD/shard-$PW_SHARD_INDEX/blob-report"
  yarn test:e2e --shard="$PW_SHARD_INDEX/$PW_SHARD_TOTAL" --workers=4 \
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
