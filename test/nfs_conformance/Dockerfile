FROM ubuntu:18.04

RUN apt-get update && \
    apt-get install -y git python3 nfs-common tcpdump sudo

RUN git clone git://git.linux-nfs.org/projects/mora/nfstest.git && \
    cd nfstest && \
    git checkout v3.0
WORKDIR /nfstest/test

RUN echo '%sudo ALL=(ALL:ALL) NOPASSWD:ALL' >> /etc/sudoers && \
    adduser --disabled-password --gecos "" nfstest_user && \
    adduser nfstest_user sudo && \
    mkdir -p /mnt/t && chmod 777 /mnt/t

ADD start.sh /

ENTRYPOINT ["bash", "/start.sh"]
CMD ["--help"]
