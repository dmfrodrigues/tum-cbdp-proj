#!/bin/bash

export $(grep -v '^#' ./.env | xargs)

if [ $# -ne 3 ];
then
  echo "Usage: ./deploy.sh docker_path script_name [script_args]"
  exit 1
fi

set -x

docker build -t "$REGISTRY_NAME.azurecr.io"/test --build-arg TEST_SCRIPT="$2" --build-arg TEST_SCRIPT_ARG="$3" "$1" && \
docker push "$REGISTRY_NAME.azurecr.io"/test
