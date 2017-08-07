#!/usr/bin/env bash

# The following docker will be generated:
#  - flow.ci.backend: it includes api and control center
#  - flow.ci.agent:

# How to run flow.ci
# - backend: 'docker-compose up'
# - agent: docker run --network=host -e FLOW_ZOOKEEPER_HOST=127.0.0.1:2181 -e FLOW_AGENT_ZONE=default flow.ci.agent

# mvn build artifact of each components
mvn clean install -DskipTests=true

# build docker image for flow.ci backend
docker build -t flow.ci.backend -f ./Dockerfile-backend .

# build docker compose service for flow.ci backend
docker-compose rm -f
docker-compose build

# build docker image for flow.ci agent
docker build -t flow.ci.agent -f ./Dockerfile-agent .