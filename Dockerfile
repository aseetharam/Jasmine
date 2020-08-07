Bootstrap: docker
From: store/oracle/jdk:11

%post
    yum -y install git wget zlib1g-dev gcc gcc-c++ cmake && yum clean all
    git clone https://github.com/mkirsche/Jasmine.git /usr/local/src/jasmine
    cd /usr/local/src/jasmine
    ./build.sh
    ./build_jar.sh

%environment
    JASMINE_INSTALL_DIR=/usr/local/src/jasmine
    version=1.02

%runscript 
    cd /usr/local/src/jasmine
    exec java -jar "$@"


FROM store/oracle/jdk:11

LABEL description="Docker file for Jasmine/IRIS"
WORKDIR /
RUN yum -y install git wget zlib1g-dev gcc gcc-c++ cmake && yum clean all
RUN git clone https://github.com/mkirsche/Jasmine.git /
WORKDIR /Jasmine
RUN ./build.sh
RUN ./build_jar.sh
