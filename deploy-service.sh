#!/bin/sh
# The following env vars must be specified:
# $DBHUB_API_PORT

envsubst < k8s-service.yaml | kubectl apply -f -
