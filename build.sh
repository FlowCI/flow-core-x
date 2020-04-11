#!/usr/bin/env bash

tag=$1

if [[ -n ${tag} ]]; then
  versionTag="-t flowci/core:$tag"
fi

docker build -f ./core/Dockerfile -t flowci/core:latest $versionTag ./core