#!/usr/bin/env bash

# This script is a wrapper around various flyway gradle tasks, and can be used to perform data migration against a named database in DETS.
# The script requires the environment variables FLYWAY_USER and FLYWAY_PASSWORD to be set, and the executed tasks take their configuration from the flyway{}
# block in the build.gradle file located in the project root.

set -euo pipefail

if [ -z "${FLYWAY_USER:-}" ]; then
  echo "Env var FLYWAY_USER must be set"
  exit 1
fi

if [ -z "${FLYWAY_PASSWORD:-}" ]; then
  echo "Env var FLYWAY_PASSWORD must be set"
  exit 1
fi

DATABASE_NAME=${1:-}
if [ -z "$DATABASE_NAME" ]; then
  echo "Usage: $0 <database_name>"
  exit 1
fi

if [ "$(git symbolic-ref --short -q HEAD)" != "master" ]; then
  echo "This script is only permitted to run on the master branch."
  exit 1
fi

cd ..

MIGRATION_SRC_DIR="src/main/resources/db/migration/"
if [ -n "$(git status --porcelain ${MIGRATION_SRC_DIR})" ]; then
  echo "Uncommitted changes exist in ${MIGRATION_SRC_DIR}, which is probably not intentional. Please check the state of your branch."
  exit 1
fi

./gradlew clean assemble

# Port 5433 is the local port that is mapped to the DB on the production bastion, per
# https://tools.hmcts.net/confluence/display/DMP/Applying+Flyway+changes+to+Migration
export FLYWAY_URL="jdbc:postgresql://localhost:5433/$DATABASE_NAME"

./gradlew flywayInfo

read -p "Execute flywayMigrate as user ${FLYWAY_USER}? (y/n): " continue
if [ "$continue" != "y" ]
then
  exit 1
fi

./gradlew flywayMigrate
./gradlew flywayValidate
./gradlew flywayInfo
