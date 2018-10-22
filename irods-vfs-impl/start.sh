#! /bin/bash

# Start the Postgres database.
service postgresql start

# Clone iRODS repo and initialize.
git clone https://github.com/korydraughn/irods
cd irods
git checkout re_plugin_issue_4144
git submodule update --init
cd /

# Build iRODS.
mkdir irods_build
cd irods_build
cmake -GNinja /irods
ninja package

# Install new irods-dev package.
dpkg -i irods-{runtime,dev}*.deb

# Clone iCommands repo.
cd /
git clone https://github.com/korydraughn/irods_client_icommands
cd irods_client_icommands
git checkout 4-2-stable
cd /

# Build and install iCommands.
mkdir icommands_build
cd icommands_build
cmake -GNinja /irods_client_icommands
ninja package
dpkg -i *.deb

# Install remaining iRODS packages.
cd /irods_build
dpkg -i irods-{server,database-plugin-postgres}*.deb

# Set up iRODS.
python /var/lib/irods/scripts/setup_irods.py < /var/lib/irods/packaging/localhost_setup_postgres.input

# Keep container running.
tail -f /dev/null
