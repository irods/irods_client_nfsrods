# iRODS NFS4J Virtual File System implementation

## A prototype of an NFS4J Mount for iRODS: https://github.com/dCache/nfs4j

#Installation

1) Stop NFS service on machine
```
$ sudo systemctl stop nfs-kernel-server.service
```

2) Compile project
```
$ cd <project_dir>; mvn install
```

3)Compile and run jar file
```
$ sudo java -jar target/irods-vfs-impl-1.0.0-SNAPSHOT-jar-with-dependencies.jar
```

4)Mount
```
$ sudo mount <hostname>:<host_dir> <client_dir>
```



