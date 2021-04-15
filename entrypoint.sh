#!/bin/bash
set -eu # exit on first error, treat substitution of unset env vars as errors

export FLYWAY_URL="jdbc:postgresql://${soh_db_host}:${soh_db_port}/${soh_db_database}"
export FLYWAY_USER="${soh_db_username}"
export FLYWAY_PASSWORD="${soh_db_password}"

/opt/flyway/flyway migrate

# Modify newrelic agent property file
cd /opt/newrelic

if [ -n "${newrelic_license}" ]; then
    sed -i -e "s/'<%= license_key %>'/'${newrelic_license}'/" /opt/newrelic/newrelic.yml
    sed -i -e 's/app_name: My Application/app_name: SOH-business-processing-engine/' /opt/newrelic/newrelic.yml
    java_opts=${java_opts}' -javaagent:/opt/newrelic/newrelic.jar'
    export java_opts
fi

exec java $java_opts -jar /app.jar
