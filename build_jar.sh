#! /bin/bash

# Compiles an executable JAR file from the local repository and
# stores all build artifacts in local_maven_repo.
#
# The local_maven_repo directory keeps your personal maven repository
# (i.e. $HOME/.m2) clean and safe from the "root" user.
docker run -it --rm --name nfsrods_builder \
    -v $PWD/local_maven_repo:/root/.m2 \
    -v $PWD:/irods_client_nfsrods \
    -w /irods_client_nfsrods \
    maven:3.8.4-openjdk-17 \
    mvn -Dmaven.test.skip=true clean install
