#!/usr/bin/env bash

env=$1

case ${env} in
  "dev" | "stage" | "prod" )
  ;;
  *)
  echo "Undefined environment. Please use 'dev', 'stage' or 'prod'."
  exit 0
  ;;
esac

function exit_on_error {
  echo $1
  exit 1
}

export GITHUB_TOKEN=$(aws ssm get-parameter --name /github-token --with-decryption --query Parameter.Value --output text)

# delete all but plugins data
if [[ -d .terrafom ]];then
  find .terraform -type f ! -path ".terraform/plugins*" -delete
  rm -rf .terraform/modules
fi
# install & use min-required terraform version
if [[ -x "$(command -v tfenv)" ]]; then
  tfenv install min-required
  tfenv use min-required
fi

terraform init -backend=true -backend-config=environments/${env}/backend.tfvars || exit_on_error "could not initialize terraform"

terraform plan -out=out.plan \
-var-file=environments/${env}/variables.tfvars \
-var "github_token=${GITHUB_TOKEN}" \
-var "environment=${env}"
