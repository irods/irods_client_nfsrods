#! /bin/bash

# Start the Postgres database.
service postgresql start

# Clone iRODS repo and initialize.
git clone https://github.com/korydraughn/irods
cd irods
git checkout re_plugin_issue_4144_partial
git submodule update --init

# Build iRODS.
cd /
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

# Build and install iCommands.
cd /
mkdir icommands_build
cd icommands_build
cmake -GNinja /irods_client_icommands
ninja package
dpkg -i *.deb

# Install remaining iRODS packages.
cd /irods_build
dpkg -i irods-{server,database-plugin-postgres}*.deb

# Clone iRODS rule engine plugin (REP).
cd /
git clone https://github.com/korydraughn/irods_rule_engine_plugin_update_collection_mtime

# Build and install REP.
cd /
mkdir rep_build
cd rep_build
cmake -GNinja /irods_rule_engine_plugin_update_collection_mtime
ninja package
dpkg -i *.deb

# Make sure that Postgres is accepting connections before starting iRODS.
# Postgres should be ready by the time this block is reached on fast systems.
counter=0
until pg_isready -q
do
    sleep 1
    ((counter += 1))
done
((counter > 0)) && echo Postgres started in approximately $counter seconds.

# Set up iRODS.
python /var/lib/irods/scripts/setup_irods.py < /var/lib/irods/packaging/localhost_setup_postgres.input

# Add REP to iRODS server configuration.
# The REP must be prepended to the list of rule engine plugins.
server_config=/etc/irods/server_config.json
server_config_updated=$server_config.updated
jq '.plugin_configuration.rule_engines = [
    {
        instance_name: "irods_rule_engine_plugin-update_collection_mtime-instance",
        plugin_name: "irods_rule_engine_plugin-update_collection_mtime",
        plugin_specific_configuration: {}
    },
    .plugin_configuration.rule_engines[]
]' $server_config > $server_config_updated && mv $server_config_updated $server_config

# Disable SSL.
sed -i 's/CS_NEG_DONT_CARE/CS_NEG_REFUSE/g' /etc/irods/core.re

# Keep container running.
tail -f /dev/null
