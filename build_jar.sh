#! /bin/bash

usage() {
cat <<_EOF_
USAGE: bash build_jar.sh [OPTIONS]

Builds a runnable NFSRODS JAR file based on the currently checked out source.

Options:
  --non-interactive   Launches docker container without -i and -t.
  -h, --help          Shows this message.
_EOF_
    exit
}

docker_run_interactive_options="-it"

while [ -n "$1" ]; do
    case "$1" in
        --non-interactive)  shift; unset docker_run_interactive_options;;
        -h|--help)          usage;;
    esac
    shift
done

# Compiles an executable JAR file from the local repository and
# stores all build artifacts in local_maven_repo.
#
# The local_maven_repo directory keeps your personal maven repository
# (i.e. $HOME/.m2) clean and safe from the "root" user.
docker run ${docker_run_interactive_options} --rm --name nfsrods_builder \
    -v $PWD/local_maven_repo:/root/.m2 \
    -v $PWD:/irods_client_nfsrods \
    -w /irods_client_nfsrods \
    maven:3.8.4-openjdk-17 \
    mvn -Dmaven.test.skip=true clean install
