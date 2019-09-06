#! /bin/bash

java -Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector -jar /opt/irods-clients/nfsrods/nfsrods-1.0.0-SNAPSHOT-jar-with-dependencies.jar
