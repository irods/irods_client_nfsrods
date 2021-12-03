FROM ubuntu:18.04

ARG sssd=false

# The following environment variables are required to avoid errors
# during the installation of openjdk-17-jdk.
ENV JAVA_HOME "/usr/lib/jvm/java-17-openjdk-amd64"
ENV PATH "$JAVA_HOME/bin:$PATH"

RUN DEBIAN_FRONTEND=noninteractive \
    apt-get update && apt-get upgrade -y && \
    apt-get install -y apt-transport-https && \
    apt-get install -y openjdk-17-jdk libnss-sss

# Provide default log4j configuration.
# This keeps log4j quiet when instructing the container to print the SHA.
ADD irods-vfs-impl/config/log4j.properties /nfsrods_config/log4j.properties

ADD nfsrods.jar start.sh /
RUN chmod u+x start.sh

ENV NFSRODS_CONFIG_HOME=/nfsrods_config

ENTRYPOINT ["./start.sh"]
