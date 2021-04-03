#!/bin/bash

set -euo pipefail

echo ::set-output name=postgres_version::"11"
echo ::set-output name=integration_test_db_port::"5432"