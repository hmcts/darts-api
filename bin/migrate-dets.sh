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

# Port 5433 is the local port that is mapped to the DB on the production bastion, per
# https://tools.hmcts.net/confluence/display/DMP/Applying+Flyway+changes+to+Migration
export FLYWAY_URL="jdbc:postgresql://localhost:5433/$DATABASE_NAME"

cd ..
./gradlew flywayInfo

read -p 'Execute flywayMigrate? (y/n): ' continue
if [ "$continue" != "y" ]
then
  exit 1
fi

./gradlew flywayMigrate
./gradlew flywayValidate
./gradlew flywayInfo
