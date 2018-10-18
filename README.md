# NFSRODS
An [nfs4j](https://github.com/dCache/nfs4j) Virtual File System implementation supporting the iRODS Data Grid.

## Features
- Configurable
- Authentication via Kerberos
- Exposes iRODS through a mount point
- Clients avoid the need for learning icommands
- Clients avoid the need to install additional iRODS packages
- Supports many common *nix commands and software (e.g. mkdir, truncate, cat, vim, etc.)

## Requirements
- Kerberos
- Java Development Kit (JDK v8)
- Maven
- OS NFS packages (e.g Ubuntu 16.04: nfs-common)

## Compiling, Running, and Mounting
The following instructions assume you're running Ubuntu 16.04 and Bash.

The root account is only needed when running the software and using mount. It is
not necessary for compiling.

Clients are expected to have Kerberos principals matching their iRODS account name. For
example, if a user has an iRODS account with the name **john**, then the Kerberos server should
have a principal with the name **john@REALM**. This is required because the Kerberos principal
name is passed through to the iRODS server via the iRODS proxy account. This allows the iRODS
server to run rules and apply policies based on the client's iRODS account.

### Compiling
```bash
$ cd /path/to/irods_client_nfsrods
$ mvn clean install -Dmaven.test.skip=true
```

After compiling, you should see output similar to the following:
```bash
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary:
[INFO] 
[INFO] nfs4j-irodsvfs ..................................... SUCCESS [  0.671 s]
[INFO] nfsrods ............................................ SUCCESS [ 12.955 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 17.378 s
[INFO] Finished at: 2018-09-13T13:10:25+00:00
[INFO] Final Memory: 43M/1844M
[INFO] ------------------------------------------------------------------------
```

You should also have two new JAR files under the `irods-vfs-impl/target` directory. Listing
the contents of this directory should produce output similar to the following:
```bash
$ ls -l /path/to/irods_client_nfsrods/irods-vfs-impl/target
total 11564
drwxr-xr-x 2 root root     4096 Sep 13 13:10 archive-tmp
drwxr-xr-x 3 root root     4096 Sep 13 13:10 classes
drwxr-xr-x 3 root root     4096 Sep 13 13:10 generated-sources
-rw-r--r-- 1 root root 11792342 Sep 13 13:10 nfsrods-1.0.0-SNAPSHOT-jar-with-dependencies.jar
-rw-r--r-- 1 root root    30246 Sep 13 13:10 nfsrods-1.0.0-SNAPSHOT.jar
drwxr-xr-x 2 root root     4096 Sep 13 13:10 maven-archiver
```

### Running
You must have a running Kerberos server with a valid service principal for
the NFSRODS server. Make sure to update the NFSRODS server config file with the correct
iRODS and Kerberos information.

The config file is located at `/path/to/irods_client_nfsrods/irods-vfs-impl/config/server.json`.
Each config option is explained below.
```javascript
{
    // This section defines options needed by the NFS server.
    "nfs_server": {
        // The port number to listen on for requests. Here, we've chosen to
        // use a non-standard port number instead of the default, 2049. However,
        // this change does require the client to specify the port when creating
        // the mount point (this is demonstrated later).
        "port": 2050,
        
        // The Kerberos service principal used to authenticate the NFSRODS
        // server to the KDC. The prefix, "nfs/", is required.
        "kerberos_service_principal": "nfs/hostname@REALM",
        
        // The absolute path to the Kerberos keytab file containing all principals.
        // Clients will need to use the same keytab file as required by Kerberos.
        "kerberos_keytab": "/etc/krb5.keytab",
        
        // The path within iRODS that will represent the root collection.
        // We recommend setting this to the zone. Using the zone as the root
        // collection allows all clients to access shared collections and data
        // objects outside of their home collection.
        "irods_mount_point": "/tempZone"
    },

    // This section defines the location of the iRODS server being presented
    // by NFSRODS. Multiple iRODS servers and zones is not supported. The
    // NFSRODS server can only be configured to present a single zone.
    "irods_server": {
        "host": "hostname",
        "port": 1247,
        
        // The iRODS zone to operate under.
        "zone": "tempZone",
        
        // Because NFS does not have any notion of iRODS, you must define which
        // resource to operate under.
        "default_resource": "demoResc"
    },

    // An administrative iRODS account is required to carry out each request.
    // The account specified here is used as a proxy to connect to the iRODS
    // server. iRODS will still apply policies based on the client's account,
    // not the proxy account.
    "irods_proxy_admin_account": {
        "username": "rods",
        "password": "rods"
    }
}
```

After updating the config file, you should be able to run the server using the following commands:
```bash
$ export NFSRODS_HOME=/path/to/irods_client_nfsrods/irods-vfs-impl
$ sudo -E java -jar $NFSRODS_HOME/target/nfsrods-1.0.0-SNAPSHOT-jar-with-dependencies.jar
```

### Mounting
```bash
$ kinit -f <iRODS_account_name>
$ sudo mkdir <mount_point>
$ sudo mount -o sec=krb5,port=2050 <hostname>:/ <mount_point>
```

If you do not receive any errors after mounting, then you should be able to access the mount
point like so:
```bash
$ cd <mount_point>/path/to/collection_or_data_object
```

## Logging
NFSRODS uses Log4j for managing and writing log files. The default config will log messages with a level >= `WARN` to `stdout`. The config file is located at `/path/to/irods_client_nfsrods/irods-vfs-impl/config/log4j.properties`.
Configuring Log4j is out of scope for this documentation. It should be easy to google steps on this.

## TODOs
- Implement support for Parallel File Transfers