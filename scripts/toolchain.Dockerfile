# The toolchain scripts/verify.sh expects, pinned.
#
# A verification that depends on what happens to be installed verifies the
# machine as much as the code. Both tools are pinned here by image tag, so the
# gate answers the same way on a laptop with JDK 8 as it does in CI.
#
# osv-scanner arrives by copying the binary out of Google's own published image
# rather than by downloading a release asset during the build. A curl into a
# container image is an unpinned fetch at build time, and this repository audits
# its dependencies precisely so that it does not do that sort of thing.

FROM ghcr.io/google/osv-scanner:v2.6.0 AS scanner

FROM maven:3.9-eclipse-temurin-21
COPY --from=scanner /osv-scanner /usr/local/bin/osv-scanner
