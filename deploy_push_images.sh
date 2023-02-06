#!/bin/bash

export $(grep -v '^#' .env | xargs)

docker build -t "$REGISTRY_NAME.azurecr.io"/peer --target prod .
docker push "$REGISTRY_NAME.azurecr.io"/peer
