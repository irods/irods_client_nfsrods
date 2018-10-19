FROM ubuntu:16.04

RUN apt-get update && \
    apt-get install -y apt-transport-https vim postgresql wget lsb-release

# Setup ICAT database.
ADD db_commands.txt /
RUN service postgresql start && su - postgres -c 'psql -f /db_commands.txt'

# Install iRODS.
RUN service postgresql start && \
    wget -qO - https://packages.irods.org/irods-signing-key.asc | apt-key add -; \
    echo "deb [arch=amd64] https://packages.irods.org/apt/ $(lsb_release -sc) main" | tee /etc/apt/sources.list.d/renci-irods.list; \
    apt-get update && \
    apt-get install -y irods-server irods-database-plugin-postgres

# Set command to execute when launching the container.
ADD start.sh /
RUN chmod u+x /start.sh
CMD ./start.sh
