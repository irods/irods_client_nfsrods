FROM ubuntu:16.04

ENV PATH /opt/irods-externals/clang6.0-0/bin:$PATH
ENV PATH /opt/irods-externals/cmake3.11.4-0/bin:$PATH
#ENV PATH /opt/irods-externals/clang3.8-0/bin:$PATH
#ENV PATH /opt/irods-externals/cmake3.5.2-0/bin:$PATH

RUN apt-get update && \
    apt-get install -y apt-transport-https git vim postgresql wget lsb-release ninja-build python \
                       libssl-dev libpam0g-dev unixodbc-dev libkrb5-dev sudo libfuse-dev libcurl4-gnutls-dev \
                       libbz2-dev python-psutil python-requests libxml2-dev zlib1g-dev odbc-postgresql python-dev \
                       make gcc help2man libjson-perl python-jsonschema super

# Setup ICAT database.
ADD db_commands.txt /
RUN service postgresql start && su - postgres -c 'psql -f /db_commands.txt'

# Install iRODS.
#RUN service postgresql start && \
RUN wget -qO - https://core-dev.irods.org/irods-core-dev-signing-key.asc | apt-key add -; \
    echo "deb [arch=amd64] https://core-dev.irods.org/apt/ $(lsb_release -sc) main" | tee /etc/apt/sources.list.d/renci-irods-core-dev.list; \
    apt-get update && \
    apt-get install -y irods-externals*

# Set command to execute when launching the container.
ADD start.sh /
RUN chmod u+x /start.sh
CMD ./start.sh
