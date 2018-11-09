#
# iRODS Packages Builder Image.
#
FROM ubuntu:16.04 as irods_package_builder

ARG irods_repo="https://github.com/irods/irods"
ENV IRODS_BUILD_IRODS_REPO ${irods_repo}

ARG irods_branch="master"
ENV IRODS_BUILD_IRODS_COMMIT ${irods_branch}

ARG icommands_repo="https://github.com/irods/irods_client_icommands"
ENV IRODS_BUILD_ICOMMANDS_REPO ${icommands_repo}

ARG icommands_branch="master"
ENV IRODS_BUILD_ICOMMANDS_COMMIT ${icommands_branch}

ARG cmake_path="/opt/irods-externals/cmake3.11.4-0/bin"
ENV PATH ${cmake_path}:$PATH

ARG clang_path="/opt/irods-externals/clang6.0-0/bin"
ENV PATH ${clang_path}:$PATH

ADD build_irods_packages.sh /

# Install iRODS dependencies.
RUN apt-get update && \
    apt-get install -y apt-transport-https git vim postgresql wget lsb-release ninja-build python \
                       libssl-dev libpam0g-dev unixodbc-dev libkrb5-dev sudo libfuse-dev libcurl4-gnutls-dev \
                       libbz2-dev python-psutil python-requests libxml2-dev zlib1g-dev odbc-postgresql python-dev \
                       make gcc help2man libjson-perl python-jsonschema super lsof

# Install irods-externals packages.
RUN wget -qO - https://core-dev.irods.org/irods-core-dev-signing-key.asc | apt-key add -; \
    echo "deb [arch=amd64] https://core-dev.irods.org/apt/ $(lsb_release -sc) main" | tee /etc/apt/sources.list.d/renci-irods-core-dev.list; \
    apt-get update && \
    apt-get install -y irods-externals* && \
    chmod u+x /build_irods_packages.sh && \
    ./build_irods_packages.sh



#
# iRODS Test Environment Image.
#
FROM ubuntu:16.04

# Copy iRODS packages into image.
COPY --from=irods_package_builder /irods_build/*.deb /icommands_build/*.deb /rep_build/*.deb /

RUN apt-get update && \
    apt-get install -y apt-transport-https vim postgresql wget lsb-release python python-pip \
                       sudo python-psutil python-requests odbc-postgresql libfuse2 libjson-perl \
                       python-jsonschema libssl-dev super lsof unixodbc jq && \
    wget -qO - https://core-dev.irods.org/irods-core-dev-signing-key.asc | apt-key add -; \
    echo "deb [arch=amd64] https://core-dev.irods.org/apt/ $(lsb_release -sc) main" | tee /etc/apt/sources.list.d/renci-irods-core-dev.list; \
    apt-get update && \
    apt-get install -y irods-externals*;

# Setup ICAT database and install iRODS packages (no setup).
ADD db_commands.txt /
RUN service postgresql start && su - postgres -c 'psql -f /db_commands.txt'; \
    dpkg -i irods-runtime*.deb \
            irods-dev*.deb \
            irods-icommands*.deb \
            irods-server*.deb \
            irods-database-plugin-postgres*.deb \
            irods-rule-engine-plugin-update-collection*.deb

# Set command to execute when launching the container.
ADD start.sh /
RUN chmod u+x /start.sh
ENTRYPOINT ["./start.sh"]
