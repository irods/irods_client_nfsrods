FROM ubuntu:18.04

RUN apt-get update && \
    apt-get install -y git python nfs-common tcpdump

RUN git clone git://git.linux-nfs.org/projects/mora/nfstest.git
WORKDIR /nfstest/test

ADD run_test.sh .
RUN chmod u+x ./run_test.sh

ENTRYPOINT ["./run_test.sh"]
CMD ["-h", "localhost"]
