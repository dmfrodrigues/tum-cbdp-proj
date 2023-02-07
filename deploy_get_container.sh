#!/bin/bash

set -x

if [ $# -eq 0 ]
then
  echo "Usage: ./deploy.sh my_container_name"
  exit 1
fi

export $(grep -v '^#' .env | xargs)

echo $(az container show --resource-group $RESOURCE_GROUP --name $1 --query ipAddress.ip --output tsv)