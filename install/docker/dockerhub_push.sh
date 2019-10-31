#!/bin/bash

echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
docker push airsonic/airsonic:latest
docker push airsonic/airsonic:edge/$TRAVIS_TAG
