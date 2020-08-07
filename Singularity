Bootstrap: docker
From: adoptopenjdk/openjdk11

%post
    apt update && apt-get install -qq -y git wget zlib1g-dev g++ cmake && apt clean
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
