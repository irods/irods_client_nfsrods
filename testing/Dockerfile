FROM ubuntu:16.04

RUN apt-get update
RUN apt-get install -y git python nfs-common

RUN git clone git://git.linux-nfs.org/projects/mora/nfstest.git
WORKDIR /nfstest/test

ADD run_test.sh .
RUN chmod u+x ./run_test.sh

ENTRYPOINT ["./run_test.sh"]
CMD ["-h", "localhost"]
