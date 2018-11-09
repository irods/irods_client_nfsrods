#! /bin/bash

# Clone iRODS repo and initialize.
git clone $IRODS_BUILD_IRODS_REPO
cd irods
git checkout $IRODS_BUILD_IRODS_COMMIT
git submodule update --init
cd /

# Build iRODS.
mkdir irods_build
cd irods_build
cmake -GNinja /irods
ninja package

# Install packages for building iCommands.
dpkg -i irods-{runtime,dev}*.deb

# Clone iCommands repo.
cd /
git clone $IRODS_BUILD_ICOMMANDS_REPO
cd irods_client_icommands
git checkout $IRODS_BUILD_ICOMMANDS_COMMIT

# Build iCommands.
cd /
mkdir icommands_build
cd icommands_build
cmake -GNinja /irods_client_icommands
ninja package

# Clone iRODS rule engine plugin (REP).
cd /
git clone https://github.com/korydraughn/irods_rule_engine_plugin_update_collection_mtime

# Build REP.
cd /
mkdir rep_build
cd rep_build
cmake -GNinja /irods_rule_engine_plugin_update_collection_mtime
ninja package
