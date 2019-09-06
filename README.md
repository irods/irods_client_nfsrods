# NFSRODS
An [nfs4j](https://github.com/dCache/nfs4j) Virtual File System implementation supporting the iRODS Data Grid.

![NFSRODS network diagram](nfsrods_diagram.png)

## Table of Contents
- [Features](#features)
- [Requirements](#requirements)
- [General Information](#general-information)
  + [Building](#building)
  + [Configuring](#configuring)
  + [Running](#running)
  + [Mounting](#mounting)
- [Unix Permissions and NFSv4 ACLs](#unix-permissions-and-nfsv4-acls)
  + [Using **nfs4_setfacl**](#using-nfs4_setfacl)
  + [Using **nfs4_getfacl**](#using-nfs4_getfacl)
- [TODOs](#todos)

## Features
- Configurable
- Exposes iRODS through a mount point
- Clients avoid the need for learning icommands
- Clients avoid the need to install additional iRODS packages
- Supports many common *nix commands and software (e.g. mkdir, cat, etc.)

## Requirements
- iRODS v4.2.6
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
$ mkdir ~/nfsrods_config
$ cp /path/to/irods_client_nfsrods/irods-vfs-impl/config/* ~/nfsrods_config
```
These files will be mounted into the NFSRODS docker container. This will be discussed later.

#### Configuration File: exports
At this time, this file should not be modified. Administrators are expected to limit access to the mount point through other means.

#### Configuration File: log4j.properties
NFSRODS uses Log4j 2 for managing and writing log files. The default config will log messages with a level >= `WARN` to `stdout`. Configuring Log4j is out of scope for this documentation. It should be easy to google steps on this.

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
        "irods_mount_point": "/tempZone",

        // The refresh time for cached user information.
        "user_information_refresh_time_in_milliseconds": 3600000,

        // The refresh time for cached stat information.
        "file_information_refresh_time_in_milliseconds": 1000

        // The refresh time for cached user access information.
        "user_access_refresh_time_in_milliseconds": 1000
    },

    // This section defines the location of the iRODS server being presented
    // by NFSRODS. The NFSRODS server can only be configured to present a single zone.
    "irods_client": {
        "host": "hostname",
        "port": 1247,
        "zone": "tempZone",
        
        // Because NFS does not have any notion of iRODS, you must define the
        // target resource for new data objects.
        "default_resource": "demoResc",

        // An administrative iRODS account is required to carry out each request.
        // The account specified here is used as a proxy to connect to the iRODS
        // server. iRODS will still apply policies based on the client's account,
        // not the proxy account.
        "proxy_admin_account": {
            "username": "rods",
            "password": "rods"
        }
    }
}
```

### Running
After updating the config file, you should be able to run the server using the following commands:
```bash
$ docker run -d --name nfsrods \
             -p <public_port>:2049 \
             -v </full/path/to/nfsrods_config>:/nfsrods_config:ro \
             -v </full/path/to/etc/passwd/formatted/file>:/etc/passwd:ro \
             -v </full/path/to/etc/shadow/formatted/file>:/etc/shadow:ro \
             nfsrods
```

This command does the following:
- Launches the container as a daemon
- Names the container **nfsrods**
- Exposes NFSRODS via the port `<public_port>`
- Maps the local config directory into the container as read-only.
- Maps the local `/etc/passwd` formatted file into the container as read-only.
- Maps the local `/etc/shadow` formatted file into the container as read-only.

**IMPORTANT:** `/etc/passwd` and `/etc/shadow` are expected to contain all of the users planning to use NFSRODS. The users defined in these files **MUST** be defined in iRODS as well. Their usernames must match the names defined in these files exactly as this is how NFSRODS matches users to the correct account in iRODS.

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

#### Things To Consider
Depending on your environment and deployment of NFSRODS, you may want to consider passing `lookupcache=none` to the mount command. This instructs the kernel to NOT cache directory entries which forces NFSRODS to lookup information about a directory on every request. While doing this will make NFSRODS less responsive, the benefit is that NFSRODS is less likely to leak information between users if they are operating within the same directory.

## Unix Permissions and NFSv4 ACLs
In iRODS, multiple users and groups can be given different permissions on a collection or data object. Unix does not provide this capability and therefore, iRODS permissions cannot be mapped into Unix permissions without losing information. To get around this, NFSRODS uses NFSv4 ACLs.

NFSv4 ACLs provide more than enough control for reflecting iRODS permissions in Unix. To manage permissions through NFSRODS, you'll need to install the package that contains `nfs4_getfacl` and `nfs4_setfacl`. On Ubuntu 16.04, that package would be `nfs4-acl-tools`. With these commands, you can view and modify all permissions in iRODS.

**IMPORTANT:** The order of ACEs within an ACL does not matter in NFSRODS. When NFSRODS has to decide whether a user is allowed to execute an operation, it takes the highest level of permission for that user (including groups the user is a member of).

### Using nfs4_setfacl
When using `nfs4_setfacl`, it's important to remember the following:
- Domain names within the user and group name field are ignored.
- Special ACE user/group names (e.g. OWNER, GROUP, EVERYONE, etc.) are not supported.
- Unsupported permission bits are ignored.
- The highest permission level provided is what NFSRODS will set as the permission.

Below is the permissions translation table used by NFSRODS when `nfs4_setfacl` is invoked. The list is in descending order of iRODS permissions.

| NFSv4 ACE Perm. Bit | NFSv4 ACE Perm. Bit Name | iRODS Perm. |
|:-------------------:|:------------------------:|:-----------:|
| o                   | ACE4_WRITE_OWNER         | own         |
| w                   | ACE4_WRITE_DATA          | write       |
| a                   | ACE4_APPEND_DATA         | write       |
| r                   | ACE4_READ_DATA           | read        |

#### Example
Given the following:
```bash
$ nfs4_setfacl -a A::john@:ro foo.txt
```
NFSRODS will see that the **ACE4_READ_DATA** and **ACE4_WRITE_OWNER** bits are set. It then maps these to appropriate iRODS permissions and takes the max of those. NFSRODS will then set `john`'s permission on `foo.txt` to `OWN`.

### Using nfs4_getfacl
Using this command is much simpler. When invoked, it returns the list of iRODS permissions on an object as an ACL. The mapping used for translation is shown below.

| iRODS Perm. | NFSv4 ACE Perm. Bits |
|:-----------:|:--------------------:|
| own         | rwado                |
| write       | rwa                  |
| read        | r                    |

### Additional NFSv4 Information
- [RFC 7530](https://tools.ietf.org/html/rfc7530)
- [HOWTO: Use NFSv4 ACL](https://www.osc.edu/book/export/html/4523)

## TODOs
- Implement support for SSL connections to iRODS
- Implement support for Parallel File Transfers
- Provide POSIX Test Suite coverage
