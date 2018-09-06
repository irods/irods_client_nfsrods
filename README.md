# NFSRODS
An [nfs4j](https://github.com/dCache/nfs4j) Virtual File System implementation supporting the iRODS Data Grid.

## Features
- Configurable
- Authentication via Kerberos
- Exposes iRODS through a mount point
- Clients avoid the need for learning icommands
- Clients avoid the need to install additional iRODS packages
- Supports many common *nix commands (e.g. mkdir, truncate, chmod, etc.)

## Requirements
- Kerberos
- Java Development Kit (JDK v8)
- Maven

## Compiling, Running, and Mounting
The following instructions assume you're running Ubuntu 16 and BaSH.
These instructions should be easily translated to work on other flavors of linux.

### Compiling
```bash
$ cd /path/to/irods_client_nfsrods
$ mvn clean install -Dmaven.test.skip=true
```

### Running
```bash
$ export NFSRODS_HOME=$(pwd)/irods-vfs-impl
$ sudo -E java -jar irods-vfs-impl/target/irods-vfs-impl-1.0.0-SNAPSHOT-jar-with-dependencies.jar
```

### Mounting
```bash
$ sudo mkdir <mount_point>
$ sudo mount -o sec=krb5,port=2050 <hostname>:/ <mount_point>
```

### TODOs
- Implement support for Parallel File Systems
- Implement support for connection pooling
