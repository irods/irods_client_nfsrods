#! /bin/bash

java -Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector -jar /irods_client_nfsrods/irods-vfs-impl/target/nfsrods-1.0.0-SNAPSHOT-jar-with-dependencies.jar "$@"
