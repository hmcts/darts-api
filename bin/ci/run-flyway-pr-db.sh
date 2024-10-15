#!/bin/bash
set -e

PR_NUMBER=$CHANGE_ID

PR_HOST="darts-modernisation-dev.postgres.database.azure.com"
PR_USER="hmcts"
PR_PASSWORD="$(kubectl -n darts-modernisation get secret postgres -o json | jq .data.PASSWORD -r | base64 -d)"
PR_DATABASE="pr-${PR_NUMBER}-darts"

FLYWAY_URL="jdbc:postgresql://${PR_HOST}:5432/${PR_DATABASE}"
FLYWAY_USER=$PR_USER
FLYWAY_PASSWORD=$PR_PASSWORD

./gradlew --no-daemon --init-script init.gradle assemble
./gradlew --no-daemon --init-script init.gradle migratePostgresDatabase
