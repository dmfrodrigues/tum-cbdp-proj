#!/bin/bash

export $(grep -v '^#' ./.env | xargs)

set -x

echo Deploying Test
az container create -g $RESOURCE_GROUP --vnet cbdpVnet --subnet cbdpSubnet\
  --restart-policy Never --name test\
  --image "$REGISTRY_NAME.azurecr.io"/test\
  --registry-password=$REG_PWD --registry-username=$REG_USERNAME