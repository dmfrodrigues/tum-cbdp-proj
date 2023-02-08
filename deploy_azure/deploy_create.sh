#!/bin/bash

if [ $# -eq 0 ] || [ $1 -lt 0 ]
then
  echo "Usage: ./deploy.sh num_peers(min 0 peer)"
  exit 1
fi

export $(grep -v '^#' ./.env | xargs)

set -x

echo Deploying leader
az storage share create --name cbdp-volume-leader \
  --account-name $STORAGE_ACCOUNT \
  --account-key $STORAGE_ACCOUNT_KEY
az container create -g $RESOURCE_GROUP --vnet cbdpVnet --subnet cbdpSubnet\
  --restart-policy Never --name leader\
  --image "$REGISTRY_NAME.azurecr.io"/peer\
  --azure-file-volume-account-name $STORAGE_ACCOUNT \
  --azure-file-volume-account-key $STORAGE_ACCOUNT_KEY \
  --azure-file-volume-share-name cbdp-volume-leader \
  --azure-file-volume-mount-path /data/db \
  --registry-password=$REG_PWD --registry-username=$REG_USERNAME

LEADER=$(az container show --resource-group $RESOURCE_GROUP --name leader --query ipAddress.ip --output tsv)
for (( i=0; i<$1; i++ ))
do
  echo Deploying peer $i to leader $LEADER
  az storage share create --name cbdp-volume-peer$i \
    --account-name $STORAGE_ACCOUNT \
    --account-key $STORAGE_ACCOUNT_KEY
  az container create -g $RESOURCE_GROUP --vnet cbdpVnet --subnet cbdpSubnet\
    --restart-policy Never --name "peer$i"\
    --image "$REGISTRY_NAME.azurecr.io"/peer\
    --environment-variables LEADER_HOST="$LEADER"\
    --azure-file-volume-account-name $STORAGE_ACCOUNT \
    --azure-file-volume-account-key $STORAGE_ACCOUNT_KEY \
    --azure-file-volume-share-name cbdp-volume-peer$i \
    --azure-file-volume-mount-path /data/db \
    --registry-password=$REG_PWD --registry-username=$REG_USERNAME &
done

wait
