#!/bin/bash
set -e

echo "*** WARNING: This script will destroy your PR DARTS API database and restore it from the PR base schema. ***"
echo "It requires \"az\" \"pg_dump\" and \"psql\", and you must also be connected to the HMCTS VPN and have a postgres database running locally."

command -v jq >/dev/null 2>&1 || { echo >&2 "I require \"jq\" but it's not installed. Aborting."; exit 1; }
command -v az >/dev/null 2>&1 || { echo >&2 "I require \"az\" but it's not installed. Aborting."; exit 1; }
command -v pg_dump >/dev/null 2>&1 || { echo >&2 "I require \"pg_dump\" but it's not installed. Aborting."; exit 1; }
command -v psql >/dev/null 2>&1 || { echo >&2 "I require \"psql\" but it's not installed. Aborting."; exit 1; }

PR_NUMBER=$CHANGE_ID

DUMP_FILE="/tmp/darts-api-prbase-dump.sql"
RESTORE_LOG_FILE="/tmp/darts-api-pr-$PR_NUMBER-restore.log"
RESTORE_OUTPUT="/tmp/darts-api-pr-$PR_NUMBER-stdout.log"

SCHEMA="darts"

PR_HOST="darts-modernisation-dev.postgres.database.azure.com"
PR_USER="hmcts"
PR_PASSWORD="$(kubectl --context ss-dev-01-aks -n darts-modernisation get secret postgres -o json | jq .data.PASSWORD -r | base64 -d)"
PR_DATABASE="pr-${PR_NUMBER}-darts"
PR_BASE_DATABASE="pr-base-darts"

echo "Using Database password: ***${PR_PASSWORD: -3}"
echo "Using PR_NUMBER: $PR_NUMBER"

echo "Dumping PR base database..."

# make the password available for pg_dump / psql
export PGPASSWORD="$PR_PASSWORD"
pg_dump -h $PR_HOST -U $PR_USER -n $SCHEMA -d $PR_BASE_DATABASE > $DUMP_FILE

echo "Dump complete, dump file: $DUMP_FILE"
echo "Restoring PR database ($PR_DATABASE)..."

# drop the darts schema
psql -h $PR_HOST -U $PR_USER -d $PR_DATABASE -c "DROP SCHEMA IF EXISTS $SCHEMA CASCADE" &> /dev/null
# restore from the dump file
psql -h $PR_HOST -U $PR_USER -d $PR_DATABASE -L $RESTORE_LOG_FILE < $DUMP_FILE &> $RESTORE_OUTPUT

echo "Restore complete, stdout: $RESTORE_OUTPUT  log file: $RESTORE_LOG_FILE"
echo "Output: $RESTORE_OUTPUT"
echo "Log file: $RESTORE_LOG_FILE"
