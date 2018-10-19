#! /bin/bash

# Start the Postgres database.
service postgresql start
seconds=10
echo "Sleeping for $seconds seconds to let the database complete start up ..."
sleep $seconds

# Set up iRODS.
python /var/lib/irods/scripts/setup_irods.py < /var/lib/irods/packaging/localhost_setup_postgres.input

# Keep container running.
tail -f /dev/null
