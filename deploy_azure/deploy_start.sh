#!/bin/bash

set -x

if [ $# -eq 0 ] || [ $1 -lt 0 ]
then
  echo "Usage: ./deploy.sh num_peers(min 0 peer)"
  exit 1
fi

export $(grep -v '^#' ./.env | xargs)

set -x

echo Starting leader
az container start -g $RESOURCE_GROUP --name leader

# Wait for a bit wile leader gets ready
LEADER=$(az container show --resource-group $RESOURCE_GROUP --name leader --query ipAddress.ip --output tsv)
sleep 5

echo Starting instances
for (( i=0; i<$1; i++ ))
do
  az container start -g $RESOURCE_GROUP --name "peer$i"\
    --environment-variables LEADER_HOST="$LEADER" &
done

wait
