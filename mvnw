#!/bin/sh
set -eu

MVNW_MAVEN_VERSION=3.9.16
MVNW_BASE="${HOME}/.m2/wrapper/dists/apache-maven-${MVNW_MAVEN_VERSION}"
MVNW_HOME="${MVNW_BASE}/apache-maven-${MVNW_MAVEN_VERSION}"
MVNW_ARCHIVE="${MVNW_BASE}/apache-maven-${MVNW_MAVEN_VERSION}-bin.tar.gz"
MVNW_URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/${MVNW_MAVEN_VERSION}/apache-maven-${MVNW_MAVEN_VERSION}-bin.tar.gz"

if [ ! -x "${MVNW_HOME}/bin/mvn" ]; then
    mkdir -p "${MVNW_BASE}"
    if [ ! -f "${MVNW_ARCHIVE}" ]; then
        if command -v curl >/dev/null 2>&1; then
            curl --fail --location --silent --show-error "${MVNW_URL}" --output "${MVNW_ARCHIVE}"
        elif command -v wget >/dev/null 2>&1; then
            wget --quiet "${MVNW_URL}" --output-document="${MVNW_ARCHIVE}"
        else
            echo "The Maven wrapper requires curl or wget to download Maven." >&2
            exit 1
        fi
    fi
    tar -xzf "${MVNW_ARCHIVE}" -C "${MVNW_BASE}"
fi

exec "${MVNW_HOME}/bin/mvn" "$@"
