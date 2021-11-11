#!/usr/bin/env bash


start_docker_container() {
  docker-compose -f docker/docker-compose.yml pull "$1"
  docker-compose -f docker/docker-compose.yml up --detach --remove-orphans "$1"
}

is_localstack_ready() {
  local response
  response=$(curl -s http://localhost:4566/health 2>/dev/null)

  local sns_up
  sns_up=0
  if [[ "$response" == *'"sns": "available"'* ]]; then
    sns_up=1
  fi

  local sqs_up
  sqs_up=0
  if [[ "$response" == *'"sqs": "available"'* ]]; then
    sqs_up=1
  fi

  if [[ $sns_up -eq 1 && $sqs_up -eq 1 ]]; then
    echo true
  else
    echo false
  fi
}

start_localstack() {
  start_docker_container localstack
  echo -n "Waiting until localstack is ready..."
  while [[ $(is_localstack_ready) == false ]];
   do
     echo -n "."
     sleep 0.5
   done
  echo "done"
  sleep 2

  ./.github/scripts/prepare_sns_sqs.sh
}

execute_migration() {
  ./docker/postgres/migrate-db.sh
}

start_postgres() {
  start_docker_container postgres
  echo -n "Waiting until postgres is ready..."
  local pg_status=1
  until [[ $pg_status == 0 ]]; do
    echo -n "."
    sleep 0.5
    pg_isready -q -d "$soh_db_database" -U "$soh_db_username" -h "$soh_db_host"
    pg_status=$?
  done
  echo "done"
  execute_migration
}

start_dependencies() {
  start_localstack
  start_postgres
}

start_service() {
  mvn spring-boot:run
}

start_all() {
  start_dependencies
  start_service
}

shutdown_containers() {
 docker-compose -f docker/docker-compose.yml down
}

print_usage() {
  echo "Usage: start.sh COMMAND"
  echo
  echo "Commands:"
  printf "  dependencies\tStart localstack, postgres and execute the flyway migration\n"
  printf "  service\tStart the soh-business-processing-engine only\n"
  printf "  all\t\tStart dependencies and the soh-business-processing-engine\n"
  printf "  migration\tExecute the flyway migration only (needs the postgres container running)\n"
  printf "  shutdown\tStop all containers\n"
}

#
# Main
#
source .env.localhost.dist

case "$1" in
    "dependencies")
      start_dependencies
      ;;
    "all")
      start_all
      ;;
    "service")
      start_service
      ;;
    "migration")
      execute_migration
      ;;
    "shutdown")
      shutdown_containers
      ;;
    *)
      print_usage
      exit 1;;
esac
