#!/usr/bin/env bash

tag=$1

if [[ -n ${tag} ]]; then
  versionTag="-t flowci/core:$tag"
fi

docker buildx build -f ./core/Dockerfile --platform linux/arm64,linux/amd64 --push -t flowci/core:latest $versionTag ./core