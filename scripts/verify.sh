#!/usr/bin/env bash
#
# Every gate, in one command.
#
# The gates are not optional and not ordered by taste. Formatting and the
# toolchain check run first because they are the cheapest; the unit tests run
# before the integration tests because a broken rule should not wait on a
# container; mutation analysis runs last because it is the slowest and because a
# surviving mutant is only interesting once the tests pass.
#
# Requires JDK 21 and Maven 3.9 on PATH, and a Docker daemon for the integration
# tests. On a machine without the Java toolchain, use scripts/verify-in-docker.sh
# instead, which runs this same script inside a container.
set -euo pipefail

readonly REPOSITORY_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${REPOSITORY_ROOT}"

readonly MAVEN_FLAGS=(--batch-mode --no-transfer-progress)

announce() {
  printf '\n=== %s ===\n' "$1"
}

# One Maven invocation, not two.
#
# verify runs spotless:check, compiles with warnings as errors, runs the unit
# tests, then the integration tests, and finally the conventions module, whose
# tests assert the rules the README states. The mutation goal is appended to the
# same command rather than run afterwards, because a second invocation resolves
# the sibling modules from the local repository instead of from this build. A
# class added to one module and not yet installed would then be missing from the
# next module's analysis, and the failure arrives as a NoClassDefFoundError
# about a class that is plainly there, which sends the reader looking in the
# wrong place entirely.
announce 'format, compile, test, integration test, conventions, mutation'
mvn "${MAVEN_FLAGS[@]}" verify org.pitest:pitest-maven:mutationCoverage

announce 'every gate passed'
