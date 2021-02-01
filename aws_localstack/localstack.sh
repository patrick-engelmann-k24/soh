#/bin/bash

cd `dirname $0`

docker-compose pull
docker-compose up --detach --remove-orphans

#docker-compose logs --tail=50
