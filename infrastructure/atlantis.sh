#!/usr/bin/env bash
export TF_IN_AUTOMATION=true

case $BASE_BRANCH_NAME in
  "master" )
    env="prod"
  ;;
  "develop" )
    env="stage"
  ;;
  *)
    echo "Will not run terraform plan for this Pull Request."
    echo "We only run Atlantis for PRs into develop and master."
    exit 0;
  ;;
esac

echo "Running Terraform Plan on"
echo "+-------------------------"
echo "+       ${env}"
echo "+-------------------------"

function exit_on_error {
  echo $1
  exit 1
}
terraform${ATLANTIS_TERRAFORM_VERSION} init -no-color -backend=true -backend-config=environments/${env}/backend.tfvars || exit_on_error "could not initialize terraform"

terraform${ATLANTIS_TERRAFORM_VERSION} plan  -no-color \
-var-file=environments/${env}/variables.tfvars \
-var "environment=${env}" \
-var "github_token=${GITHUB_TOKEN}"  \
-out=$PLANFILE