#!/bin/bash

if [ $# -eq 0 ] || [ $1 -lt 1 ]
then
  echo "Usage: ./deploy.sh num_peers(min 1 peer)"
  exit 1
fi

export $(grep -v '^#' .env | xargs)

echo Deploying leader
# az container start -g cbdp-resourcegroup --name leader
# #az container create -g cbdp-resourcegroup --vnet cbdpVnet --subnet cbdpSubnet\
# #  --restart-policy Never --name leader\
# #  --image "$REGISTRY_NAME.azurecr.io"/leader\
# #  --registry-password=$REG_PWD --registry-username=$REG_USERNAME

# LEADER=$(az container show --resource-group cbdp-resourcegroup --name leader --query ipAddress.ip --output tsv)
# for (( i=1; i<=$1; i++ ))
# do
#   echo Deploying peer $i to leader $LEADER
#   az container create -g cbdp-resourcegroup --vnet cbdpVnet --subnet cbdpSubnet\
#     --restart-policy Never --name "peer$i" --image "$REGISTRY_NAME.azurecr.io"/peer\
#     --environment-variables CBDP_LEADER="$LEADER"\
#     --registry-password=$REG_PWD --registry-username=$REG_USERNAME &
#   # az container start -g cbdp-resourcegroup --name peer$i &
# done

# wait
