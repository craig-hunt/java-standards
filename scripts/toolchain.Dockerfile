# The toolchain scripts/verify.sh expects, pinned by digest.
#
# A verification that depends on what happens to be installed verifies the
# machine as much as the code. Both images are addressed by digest, not by tag:
# a tag is a name somebody can repoint, and maven:3.9-eclipse-temurin-21 floats
# across patch releases of both Maven and the JDK. A file that claims to pin a
# toolchain and names a moving tag is worse than one that makes no claim.
#
# The readable tags are kept alongside each digest so a reader can tell what the
# digest is, and so a bump is a visible two-line change rather than an opaque
# one.
#
# osv-scanner arrives by copying the binary out of Google's own published image
# rather than by downloading a release asset during the build. A curl into a
# container image is an unpinned fetch at build time, and this repository audits
# its dependencies precisely so that it does not do that sort of thing.

# ghcr.io/google/osv-scanner:v2.6.0
FROM ghcr.io/google/osv-scanner@sha256:afd838850ac1a0fcc15ff4a041dc9ba11123c3f0d2666217a5f0fcf9222b55fa AS scanner

# maven:3.9.16-eclipse-temurin-21
FROM maven@sha256:99e61abcff91a9b1333463bd8451fb18495d6eba9250ac66a338b518f8278320
COPY --from=scanner /osv-scanner /usr/local/bin/osv-scanner
