FROM ubuntu:16.04

RUN apt-get update && \
    apt-get install -y apt-transport-https && \
    apt-get install -y maven git openjdk-8-jdk

ARG _github_account="irods"
ARG _sha="master"

RUN git clone https://github.com/${_github_account}/irods_client_nfsrods

RUN cd irods_client_nfsrods && \
    git checkout ${_sha} && \
    mvn clean install -Dmaven.test.skip=true

ADD start.sh /
RUN chmod +x start.sh

ENV NFSRODS_CONFIG_HOME=/nfsrods_config

ENTRYPOINT ["./start.sh"]
