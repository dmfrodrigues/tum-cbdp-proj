#!/bin/bash

export $(grep -v '^#' ./.env | xargs)

set -x

echo Starting Test
az container start -g $RESOURCE_GROUP --name test