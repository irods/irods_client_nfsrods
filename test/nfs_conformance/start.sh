#! /bin/bash

#export PYTHONPATH=/nfstest
set -x

#username="$1"
#host="${1:-localhost}"
#port="${2:-2049}"

#adduser --disabled-password --no-create-home --gecos "" $username
#adduser $username sudo

# Run the tests!
#cmd="cd /nfstest/test && ./nfstest_posix $@"
#su -p nfstest_user -c "$cmd"
sudo -u nfstest_user PYTHONPATH=/nfstest /nfstest/test/nfstest_posix "$@"
#su -p $username -c "
#su -p nfstest_user -c "
#cd /nfstest/test
#./nfstest_posix -s $host -p $port --runtest access
#./nfstest_posix -s $host -p $port --runtest chdir
#./nfstest_posix -s $host -p $port --runtest close
#./nfstest_posix -s $host -p $port --runtest closedir
#./nfstest_posix -s $host -p $port --runtest creat
#./nfstest_posix -s $host -p $port --runtest fcntl
#./nfstest_posix -s $host -p $port --runtest fdatasync
#./nfstest_posix -s $host -p $port --runtest fstat
#./nfstest_posix -s $host -p $port --runtest fstatvfs
#./nfstest_posix -s $host -p $port --runtest fsync
#./nfstest_posix -s $host -p $port --runtest link
#./nfstest_posix -s $host -p $port --runtest lseek
#./nfstest_posix -s $host -p $port --runtest lstat
#./nfstest_posix -s $host -p $port --runtest mkdir
#./nfstest_posix -s $host -p $port --runtest mmap
#./nfstest_posix -s $host -p $port --runtest munmap
#./nfstest_posix -s $host -p $port --runtest opendir
#./nfstest_posix -s $host -p $port --runtest read
#./nfstest_posix -s $host -p $port --runtest readdir
#./nfstest_posix -s $host -p $port --runtest readlink
#./nfstest_posix -s $host -p $port --runtest rename
#./nfstest_posix -s $host -p $port --runtest rewinddir
#./nfstest_posix -s $host -p $port --runtest rmdir
#./nfstest_posix -s $host -p $port --runtest seekdir
#./nfstest_posix -s $host -p $port --runtest stat
#./nfstest_posix -s $host -p $port --runtest statvfs
#./nfstest_posix -s $host -p $port --runtest symlink
#./nfstest_posix -s $host -p $port --runtest sync
#./nfstest_posix -s $host -p $port --runtest telldir
#./nfstest_posix -s $host -p $port --runtest unlink
#./nfstest_posix -s $host -p $port --runtest write
#./nfstest_posix -s $host -p $port --runtest open
#./nfstest_posix -s $host -p $port --runtest chmod
#"
