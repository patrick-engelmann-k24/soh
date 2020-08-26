#!/usr/bin/env bash

#
# To run this locally, execute #>source .env.localhost to set flyway environment variables
#
#


# set -x # print commands for debugging purposes
echo "Performing database update"

MVN_BASEDIR="$(cd "`dirname "$0"`" ; pwd)"

function _checkFlyWayEnvVars {
    if [ -z ${FLYWAY_URL} ]
    then
        echo "FLYWAY_URL not set";
        FLYWAY_ERROR=1
    fi

    if [ -z ${FLYWAY_USER} ]
    then
        echo "FLYWAY_USER not set";
        FLYWAY_ERROR=2
    fi

    if [ -z ${FLYWAY_PASSWORD} ]
    then
        echo "FLYWAY_PASSWORD not set";
        FLYWAY_ERROR=3
    fi

    # if error occurred stop here
    if [ ! -z $FLYWAY_ERROR ]
    then
        echo "Stopping upgrade, see errors above"
        exit $FLYWAY_ERROR
    fi
}


_checkFlyWayEnvVars

export FLYWAY_CONFIG_FILE_ENCODING=UTF-8

cd $MVN_BASEDIR

mvn flyway:migrate

if [ $? -ne 0 ]
then
  echo "Migration not successful! Stopping here!"
  exit 255
fi

