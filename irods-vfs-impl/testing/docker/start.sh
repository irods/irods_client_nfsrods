#! /bin/bash

# Start the Postgres database.
service postgresql start
counter=0
until pg_isready -q
do
    sleep 1
    ((counter += 1))
done
echo Postgres took approximately $counter seconds to fully start ...

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

# Keep container running if the test fails.
# Is this better? sleep 2147483647d
tail -f /dev/null

