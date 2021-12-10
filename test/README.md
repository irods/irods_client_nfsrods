# Testing

## How to run the NFS conformance test suite

### Requirements
- The conformance tests expect the NFSRODS server to expose a collection owned by the invoking user.
- The name of the invoking user.

### Running the test suite
First, build the nfstest docker image. Enter `<project>/test/nfs_conformance` and run the following:
```bash
$ docker build -t nfsrods_nfstest .
```
Upon a successful build, the tests can be launched using the following:
```bash
$ docker run -it --rm --name nfsrods_nfstest --cap-add SYS_ADMIN --security-opt apparmor:unconfined nfsrods_nfstest <user> <hostname> <port>
$ docker run -it --rm --name nfsrods_nfstest --cap-add SYS_ADMIN --security-opt apparmor:unconfined nfsrods_nfstest -s <hostname> -p <port> --runtest <test>[,<test>...]
```
#### Parameters
- **<user>**    : The name of UNIX user to run the tests as. This username must map to an iRODS user.
- **<hostname>**: The hostname of the computer running the NFSRODS server.
- **<port>**    : The port number the NFSRODS server is listening on.
