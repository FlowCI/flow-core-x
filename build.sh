#!/usr/bin/env bash

openjdk_version=8
maven_version=3.6
build_image=maven:$maven_version-jdk-$openjdk_version

## build jar from docker
docker run -it --rm \
--name flowci.core.build \
-v "$PWD":/usr/src/flowci.core \
-v "$HOME/.m2":/root/.m2 \
-w /usr/src/flowci.core \
$build_image \
mvn clean package -Dmaven.test.skip=true

## create docker image
docker_version=$1

if [[ -n ${docker_version} ]]; then
  versionTag="-t flowci/core:$docker_version"
fi

docker build -f ./core/Dockerfile -t flowci/core:latest $versionTag ./core