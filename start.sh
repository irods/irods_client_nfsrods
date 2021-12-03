#! /bin/bash

if [ "$1" != "sha" ]; then
    # Handle SSL/TLS cert.
    if [ -f /nfsrods_ssl.crt ]; then
       echo "Cert found for NFSRODS"
       set +e
       keytool -delete -noprompt -alias mycert -keystore /etc/ssl/certs/java/cacerts -storepass changeit
       set -e
       echo "Importing cert to OpenJDK keystore"
       keytool -import -trustcacerts -keystore /etc/ssl/certs/java/cacerts -storepass changeit -noprompt -alias mycert -file /nfsrods_ssl.crt
       echo "Done"
    else
       echo "Cert not found for NFSRODS - not importing"
    fi
fi

exec java \
    -Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector \
    -Dlog4j2.configurationFile=$NFSRODS_CONFIG_HOME/log4j.properties \
    -Dlog4j.shutdownHookEnabled=false \
    -jar /nfsrods.jar "$@"
