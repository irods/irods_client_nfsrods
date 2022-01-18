FROM ubuntu:16.04
ARG sssd=false

RUN apt-get update && \
    apt-get install -y apt-transport-https && \
    apt-get install -y maven git openjdk-8-jdk && \
    apt-get install -y libnss-sss rpcbind

ARG _github_account="irods"
ARG _sha="main"

RUN git clone https://github.com/${_github_account}/irods_client_nfsrods

RUN cd irods_client_nfsrods && \
    git checkout ${_sha} && \
    mvn clean install -Dmaven.test.skip=true

# Provide default log4j configuration.
# This keeps log4j quiet when instructing the container to print the SHA.
ADD irods-vfs-impl/config/log4j.properties /nfsrods_config/log4j.properties

ADD start.sh /
RUN chmod +x start.sh

ENV NFSRODS_CONFIG_HOME=/nfsrods_config

ENTRYPOINT ["./start.sh"]
