# Adopted from https://github.com/serandel/kscript-docker/blob/master/Dockerfile

# Alpine had problems with Java and SDKMAN!
# lchmod (file attributes) error: Not supported
# https://bugs.alpinelinux.org/issues/8089

FROM ubuntu:focal

MAINTAINER holgerbrandl@gmail.com

SHELL ["/bin/bash", "-c"]

RUN apt-get update && \
	apt-get -y install curl zip unzip && \
    rm -rf /var/lib/apt/lists/*

# Install SDKMAN!
RUN curl -s "https://get.sdkman.io" | bash

RUN source /root/.sdkman/bin/sdkman-init.sh && \
    sdk install java 8.0.265-zulu && \
    sdk install kotlin 1.4.10
#    sdk install maven 3.6.0 && \

ARG KSCRIPT_VERSION
ENV KSCRIPT_VERSION=$KSCRIPT_VERSION

## run separately to better use docker build cache
RUN source /root/.sdkman/bin/sdkman-init.sh && \
    sdk install kscript $KSCRIPT_VERSION

ENTRYPOINT source /root/.sdkman/bin/sdkman-init.sh && /usr/bin/env kscript "$0" "$@"
CMD [ "--help" ]