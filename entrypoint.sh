#!/bin/bash
set -eu # exit on first error, treat substitution of unset env vars as errors

export FLYWAY_URL="jdbc:postgresql://${soh_db_host}:${soh_db_port}/${soh_db_database}"
export FLYWAY_USER="${soh_db_username}"
export FLYWAY_PASSWORD="${soh_db_password}"

/opt/flyway/flyway migrate

exec java $java_opts -jar /app.jar
