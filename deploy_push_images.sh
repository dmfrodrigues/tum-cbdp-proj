#!/bin/bash

export $(grep -v '^#' .env | xargs)

docker build -t "$REGISTRY_NAME.azurecr.io"/peer --target peer .
docker push "$REGISTRY_NAME.azurecr.io"/peer
docker build -t "$REGISTRY_NAME.azurecr.io"/leader --target leader .
docker push "$REGISTRY_NAME.azurecr.io"/leader
