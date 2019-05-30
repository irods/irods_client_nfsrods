#! /bin/bash

host=
port=2049

usage() {
cat <<_EOF_
Usage: docker run -it --privileged nfsrods_tester [OPTIONS]...

Available options:
    -h     The hostname of the computer running NFSRODS.
    -p     The port number of the NFSRODS server. [Default=2049]
    --help This message

Example:
    docker run -it --rm --name nfsrods_tester --privileged nfsrods_tester -h localhost -p 2049
_EOF_
    exit
}

while [ -n "$1" ]; do
    case "$1" in
        -h) shift; host=${1};;
        -p) shift; port=${1};;
        --help) usage;;
    esac
    shift
done

if [ -z "$host" ]; then
    echo Hostname not set.
    exit 1
fi

if [ -z "$port" ]; then
    echo Port number not set.
    exit 1
fi

export PYTHONPATH=/nfstest

./nfstest_posix -s $host -p $port --runtest 'access'
./nfstest_posix -s $host -p $port --runtest 'chdir'
./nfstest_posix -s $host -p $port --runtest 'close'
./nfstest_posix -s $host -p $port --runtest 'closedir'
./nfstest_posix -s $host -p $port --runtest 'creat'
./nfstest_posix -s $host -p $port --runtest 'fcntl'
./nfstest_posix -s $host -p $port --runtest 'fdatasync'
./nfstest_posix -s $host -p $port --runtest 'fstat'
./nfstest_posix -s $host -p $port --runtest 'fstatvfs'
./nfstest_posix -s $host -p $port --runtest 'fsync'
./nfstest_posix -s $host -p $port --runtest 'link'
./nfstest_posix -s $host -p $port --runtest 'lseek'
./nfstest_posix -s $host -p $port --runtest 'lstat'
./nfstest_posix -s $host -p $port --runtest 'mkdir'
./nfstest_posix -s $host -p $port --runtest 'mmap'
./nfstest_posix -s $host -p $port --runtest 'munmap'
./nfstest_posix -s $host -p $port --runtest 'opendir'
./nfstest_posix -s $host -p $port --runtest 'read'
./nfstest_posix -s $host -p $port --runtest 'readdir'
./nfstest_posix -s $host -p $port --runtest 'readlink'
./nfstest_posix -s $host -p $port --runtest 'rename'
./nfstest_posix -s $host -p $port --runtest 'rewinddir'
./nfstest_posix -s $host -p $port --runtest 'rmdir'
./nfstest_posix -s $host -p $port --runtest 'seekdir'
./nfstest_posix -s $host -p $port --runtest 'stat'
./nfstest_posix -s $host -p $port --runtest 'statvfs'
./nfstest_posix -s $host -p $port --runtest 'symlink'
./nfstest_posix -s $host -p $port --runtest 'sync'
./nfstest_posix -s $host -p $port --runtest 'telldir'
./nfstest_posix -s $host -p $port --runtest 'unlink'
./nfstest_posix -s $host -p $port --runtest 'write'
./nfstest_posix -s $host -p $port --runtest 'open'
./nfstest_posix -s $host -p $port --runtest 'chmod'
