#!/usr/bin/env bash

# build artifact of each components
mvn clean install -DskipTests=true

# build docker image via compose
docker-compose rm -f
docker-compose build