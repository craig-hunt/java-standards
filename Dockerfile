# Two runnable images from one build: the migrator and the API.
#
# A multi-stage build so the shipped layers carry no Maven, no sources and no
# test dependencies. What reaches a deployment is a JRE and the jars the service
# needs, which is both smaller and a smaller attack surface.

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# The POMs land first and resolve on their own layer, so a source change does not
# re-download the dependency tree.
COPY pom.xml ./
COPY domain/pom.xml domain/
COPY application/pom.xml application/
COPY infrastructure/pom.xml infrastructure/
COPY web/pom.xml web/
COPY conventions/pom.xml conventions/
RUN mvn --batch-mode --no-transfer-progress -pl .,domain,application,infrastructure,web \
        dependency:go-offline

COPY domain/src domain/src
COPY application/src application/src
COPY infrastructure/src infrastructure/src
COPY web/src web/src

# The image build does not run the tests. scripts/verify.sh and the CI workflow
# run them, and the integration tests need a Docker daemon that is not available
# here. An image build that skipped a gate while appearing to run it would be
# worse than one that plainly does not.
# install rather than package: dependency:copy-dependencies below resolves the
# sibling modules from the local repository, and package leaves them only in
# each module's target directory.
#
# The leading "." installs the parent POM. Without it the next step fails while
# reading infrastructure's descriptor, because resolving any module's parent
# means resolving this POM and it was never installed. The conventions module is
# left out: it carries only tests, and nothing in the image needs it.
RUN mvn --batch-mode --no-transfer-progress -pl .,domain,application,infrastructure,web \
        -DskipTests install

# The dependencies are copied beside the jar rather than shaded into it, so a
# single changed class rebuilds one small layer. copy-dependencies brings the
# three sibling modules along with the third-party jars, because web depends on
# them; only web's own jar has to be copied by hand.
RUN mvn --batch-mode --no-transfer-progress -pl web \
        dependency:copy-dependencies -DoutputDirectory=/build/libs \
    && cp web/target/web-*.jar /build/libs/

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
COPY --from=build /build/libs /app/libs

# An unprivileged user, because nothing this process does needs root and a
# container escape is cheaper to exploit from an account that has it.
RUN addgroup --system standards && adduser --system --ingroup standards standards
USER standards

FROM runtime AS migrator
ENTRYPOINT ["java", "-cp", "/app/libs/*", "com.sagecrest.standards.web.Migrator"]

FROM runtime AS api
EXPOSE 8080
ENTRYPOINT ["java", "-cp", "/app/libs/*", "com.sagecrest.standards.web.Service"]
