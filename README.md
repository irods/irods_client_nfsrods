# NFSRODS
An [nfs4j](https://github.com/dCache/nfs4j) Virtual File System implementation supporting the iRODS Data Grid.

![NFSRODS network diagram](nfsrods_diagram.png)

## Features
- Configurable
- Exposes iRODS through a mount point
- Clients avoid the need for learning icommands
- Clients avoid the need to install additional iRODS packages
- Supports many common *nix commands and software (e.g. mkdir, truncate, cat, vim, etc.)

## Requirements
- iRODS v4.2.5+
- [iRODS REP for Collection Mtime](https://github.com/irods/irods_rule_engine_plugin_update_collection_mtime)
- Docker (as of this writing, v18.09.0)
- OS NFS packages (e.g. Ubuntu 16.04: nfs-common)

## General Information
The following instructions assume you're running Ubuntu 16.04 and Bash.

### Building
```bash
$ cd /path/to/irods_client_nfsrods
$ docker build -t nfsrods .
```

### Configuring
There are three config files located under `/path/to/irods_client_nfsrods/irods-vfs-impl/config`:
- exports
- log4j.properties
- server.json

The first step in configuring the server is to copy these files into another location on disk like so:
```bash
$ mkdir ~/nfsrods_configs
$ cp /path/to/irods_client_nfsrods/irods-vfs-impl/config/* ~/nfsrods_configs
```
These files will be mounted into the NFSRODS docker container. This will be discussed later.

#### Configuration File: exports
At this time, this file should not be modified. Administrators are expected to limit access to the mount point through other means.

#### Configuration File: log4j.properties
NFSRODS uses Log4j for managing and writing log files. The default config will log messages with a level >= `WARN` to `stdout`. Configuring Log4j is out of scope for this documentation. It should be easy to google steps on this.

#### Configuration File: server.json
You'll need to set each option to match your iRODS environment. Each option is explained below.
```javascript
{
    // This section defines options needed by the NFS server.
    "nfs_server": {
        // The port number within the container to listen for NFS requests.
        "port": 2049,
        
        // The path within iRODS that will represent the root collection.
        // We recommend setting this to the zone. Using the zone as the root
        // collection allows all clients to access shared collections and data
        // objects outside of their home collection.
        "irods_mount_point": "/tempZone"
    },

    // This section defines the location of the iRODS server being presented
    // by NFSRODS. The NFSRODS server can only be configured to present a single zone.
    "irods_server": {
        "host": "hostname",
        "port": 1247,
        "zone": "tempZone",
        
        // Because NFS does not have any notion of iRODS, you must define the
        // target resource for new data objects.
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

### Running
After updating the config file, you should be able to run the server using the following commands:
```bash
$ docker run -d --name nfsrods \
             -p <public_port>:2049 \
             -v </full/path/to/nfsrods_configs>:/nfsrods_ext:ro \
             -v /etc/passwd:/etc/passwd:ro \
             -v /etc/shadow:/etc/shadow:ro \
             nfsrods
```

This command does the following:
- Launches the container as a daemon
- Names the container **nfsrods**
- Exposes NFSRODS via the port `<public_port>`
- Maps the local config directory into the container as read-only.
- Maps `/etc/passwd` into the container as read-only.
- Maps `/etc/shadow` into the container as read-only.

**IMPORTANT**: `/etc/passwd` and `/etc/shadow` are expected to contain all of the users planning to use NFSRODS. The users defined in these files **MUST** be defined in iRODS as well. Their usernames must match the names defined in these files exactly as this is how NFSRODS matches users to the correct account in iRODS.

If you want to see the output of the server, run the following command:
```bash
$ docker logs -f nfsrods
```
This only works if the logging has been configured to write to stdout.

### Mounting
```bash
$ sudo mkdir <mount_point>
$ sudo mount -o sec=sys,port=<public_port> <hostname>:/ <mount_point>
```

If you do not receive any errors after mounting, then you should be able to access the mount point like so:
```bash
$ cd <mount_point>/path/to/collection_or_data_object
```

## TODOs
- Implement support for Parallel File Transfers
