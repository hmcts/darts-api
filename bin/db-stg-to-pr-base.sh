#!/bin/bash
set -e

echo "*** WARNING: This script will destroy the PR base DARTS API database and restore it from staging. ***"
echo "It requires \"az\" \"pg_dump\" and \"psql\", and you must also be connected to the HMCTS VPN and have a postgres database running locally."

command -v jq >/dev/null 2>&1 || { echo >&2 "I require \"jq\" but it's not installed. Aborting."; exit 1; }
command -v az >/dev/null 2>&1 || { echo >&2 "I require \"az\" but it's not installed. Aborting."; exit 1; }
command -v pg_dump >/dev/null 2>&1 || { echo >&2 "I require \"pg_dump\" but it's not installed. Aborting."; exit 1; }
command -v psql >/dev/null 2>&1 || { echo >&2 "I require \"psql\" but it's not installed. Aborting."; exit 1; }

read -p 'Are you sure you want to continue (y/n): ' continue
if [ "$continue" != "y" ]
then
  exit 1
fi

echo "Fetching secrets from staging key-vault..."

DUMP_FILE="/tmp/darts-api-stg-dump.sql"
RESTORE_LOG_FILE="/tmp/darts-api-pr-restore.log"
RESTORE_OUTPUT="/tmp/darts-api-pr-stdout.log"

SCHEMA="$(az keyvault secret show --vault-name darts-stg --name api-POSTGRES-SCHEMA | jq .value -r)"
DATABASE="$(az keyvault secret show --vault-name darts-stg --name api-POSTGRES-DATABASE | jq .value -r)"

PR_HOST="darts-modernisation-dev.postgres.database.azure.com"
PR_USER="hmcts"
PR_PASSWORD="$(kubectl --context ss-dev-01-aks -n darts-modernisation get secret postgres -o json | jq .data.PASSWORD -r | base64 -d)"
PR_DATABASE="pr-base-darts"

STG_HOST="$(az keyvault secret show --vault-name darts-stg --name api-POSTGRES-HOST | jq .value -r)"
STG_USER="$(az keyvault secret show --vault-name darts-stg --name api-POSTGRES-USER | jq .value -r)"
STG_PASSWORD="$(az keyvault secret show --vault-name darts-stg --name api-POSTGRES-PASS | jq .value -r)"
STG_PORT="$(az keyvault secret show --vault-name darts-stg --name api-POSTGRES-PORT | jq .value -r)"

echo "Dumping staging database..."

# make the password available for pg_dump
export PGPASSWORD="$STG_PASSWORD"
pg_dump -h $STG_HOST -p $STG_PORT -U $STG_USER -n $SCHEMA -d $DATABASE > $DUMP_FILE

echo "Dump complete, dump file: $DUMP_FILE"
echo "Restoring PR database ($PR_DATABASE)..."

# make the password available for psql
export PGPASSWORD="$PR_PASSWORD"
# drop the darts schema
psql -h $PR_HOST -U $PR_USER -d $PR_DATABASE -c "DROP SCHEMA IF EXISTS $SCHEMA CASCADE" &> /dev/null
# restore from the dump file
psql -h $PR_HOST -U $PR_USER -d $PR_DATABASE -L $RESTORE_LOG_FILE < $DUMP_FILE &> $RESTORE_OUTPUT

echo "Restore complete, stdout: $RESTORE_OUTPUT  log file: $RESTORE_LOG_FILE"
echo "Output: $RESTORE_OUTPUT"
echo "Log file: $RESTORE_LOG_FILE"
