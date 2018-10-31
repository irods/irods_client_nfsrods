#! /bin/bash

irods_port=${1:-9000}

docker run -d --rm --name irods_ub16_postgres -p $irods_port:1247 irods_ub16_postgres
