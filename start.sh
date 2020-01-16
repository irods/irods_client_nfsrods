#! /bin/bash

java -Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector -jar /irods_client_nfsrods/irods-vfs-impl/target/nfsrods-0.9.3-jar-with-dependencies.jar "$@"
