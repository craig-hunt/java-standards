#!/usr/bin/env bash
#
# Runs scripts/verify.sh inside a container that already has JDK 21 and Maven.
#
# This exists because a verification that depends on what happens to be installed
# verifies the machine as much as the code. Pinning the toolchain in an image
# means the gate answers the same way on a laptop with JDK 8 as it does in CI.
#
# The Docker socket is mounted through so Testcontainers can start PostgreSQL
# from inside this container, and TESTCONTAINERS_HOST_OVERRIDE tells it that the
# port the database lands on is reachable at the host rather than at localhost.
# Forgetting either one makes the integration tests report that there is no
# Docker environment, which reads as a broken machine rather than a missing flag.
set -euo pipefail

readonly REPOSITORY_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

readonly TOOLCHAIN_IMAGE='maven:3.9-eclipse-temurin-21'
readonly DEPENDENCY_CACHE='java-standards-m2'
readonly DOCKER_SOCKET='/var/run/docker.sock'
readonly HOST_ALIAS='host.docker.internal'

exec docker run --rm \
  --volume "${REPOSITORY_ROOT}:/work" \
  --workdir /work \
  --volume "${DEPENDENCY_CACHE}:/root/.m2" \
  --volume "${DOCKER_SOCKET}:${DOCKER_SOCKET}" \
  --add-host "${HOST_ALIAS}:host-gateway" \
  --env "TESTCONTAINERS_HOST_OVERRIDE=${HOST_ALIAS}" \
  "${TOOLCHAIN_IMAGE}" \
  ./scripts/verify.sh
