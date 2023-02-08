#!/bin/bash

if [ $# -eq 0 ] || [ $1 -lt 0 ]
then
  echo "Usage: ./deploy.sh num_peers(min 0 peer)"
  exit 1
fi

export $(grep -v '^#' ./.env | xargs)

set -x

echo Deploying leader
az container create -g $RESOURCE_GROUP --vnet cbdpVnet --subnet cbdpSubnet\
  --restart-policy Never --name leader\
  --image "$REGISTRY_NAME.azurecr.io"/peer\
  --registry-password=$REG_PWD --registry-username=$REG_USERNAME

LEADER=$(az container show --resource-group $RESOURCE_GROUP --name leader --query ipAddress.ip --output tsv)
for (( i=0; i<$1; i++ ))
do
  echo Deploying peer $i to leader $LEADER
  az container create -g $RESOURCE_GROUP --vnet cbdpVnet --subnet cbdpSubnet\
    --restart-policy Never --name "peer$i"\
    --image "$REGISTRY_NAME.azurecr.io"/peer\
    --environment-variables LEADER_HOST="$LEADER"\
    --registry-password=$REG_PWD --registry-username=$REG_USERNAME &
done

wait
