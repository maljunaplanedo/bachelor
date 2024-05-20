#!/bin/sh
# The following env vars must be specified:
# $DBHUB_IMAGE_REGISTRY
# $DBHUB_DATABASE_URL
# $DBHUB_DATABASE_USERNAME
# $DBHUB_DATABASE_PASSWORD
# $DBHUB_FRONTEND_HOST
# $DBHUB_FRONTEND_PORT
# $DBHUB_ADMIN_USERNAME
# $DBHUB_ADMIN_PASSWORD
# $DBHUB_API_HOST
# $DBHUB_API_PORT

export DBHUB_VERSION=$(date +%s)

docker build -t dbhub-collector:$DBHUB_VERSION -f backend/collector/Dockerfile backend
docker tag dbhub-collector:$DBHUB_VERSION $DBHUB_IMAGE_REGISTRY/dbhub-collector:$DBHUB_VERSION
docker push $DBHUB_IMAGE_REGISTRY/dbhub-collector:$DBHUB_VERSION

docker build -t dbhub-apigateway:$DBHUB_VERSION -f backend/apigateway/Dockerfile backend
docker tag dbhub-apigateway:$DBHUB_VERSION $DBHUB_IMAGE_REGISTRY/dbhub-apigateway:$DBHUB_VERSION
docker push $DBHUB_IMAGE_REGISTRY/dbhub-apigateway:$DBHUB_VERSION

docker build -t dbhub-frontend:$DBHUB_VERSION --build-arg DBHUB_API_URL=$DBHUB_API_HOST:$DBHUB_API_PORT frontend
docker tag dbhub-frontend:$DBHUB_VERSION $DBHUB_IMAGE_REGISTRY/dbhub-frontend:$DBHUB_VERSION
docker push $DBHUB_IMAGE_REGISTRY/dbhub-frontend:$DBHUB_VERSION

envsubst < k8s.yaml | kubectl apply -f -
