FROM ubuntu:18.04

ARG sssd=false

# The following environment variables are required to avoid errors
# during the installation of openjdk-17-jdk.
ENV JAVA_HOME "/usr/lib/jvm/java-17-openjdk-amd64"
ENV PATH "$JAVA_HOME/bin:$PATH"

RUN DEBIAN_FRONTEND=noninteractive \
    apt-get update && apt-get upgrade -y && \
    apt-get install -y apt-transport-https && \
    apt-get install -y wget git openjdk-17-jdk && \
    apt-get install -y libnss-sss rpcbind

RUN wget https://dlcdn.apache.org/maven/maven-3/3.8.4/binaries/apache-maven-3.8.4-bin.tar.gz && \
    tar -xf apache-maven-3.8.4-bin.tar.gz -C /opt
ENV PATH "/opt/apache-maven-3.8.4/bin:$PATH"

ARG github_account="irods"
ARG commitish="main"

RUN git clone https://github.com/${github_account}/irods_client_nfsrods

RUN cd irods_client_nfsrods && \
    git checkout ${commitish} && \
    mvn clean install -Dmaven.test.skip=true

# Provide default log4j configuration.
# This keeps log4j quiet when instructing the container to print the SHA.
ADD irods-vfs-impl/config/log4j.properties /nfsrods_config/log4j.properties

ADD start.sh /
RUN chmod +x start.sh

ENV NFSRODS_CONFIG_HOME=/nfsrods_config

ENTRYPOINT ["./start.sh"]
