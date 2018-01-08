#!/usr/bin/env bash



# todo push image to docker registry
cd ${KSCRIPT_HOME}/misc
#docker build --no-cache -t kscript_tester .
docker build -t kscript_tester .
docker run -it kscript_tester


## or with host-local repo-copy
#cd ~/Desktop/
#git clone https://github.com/holgerbrandl/kscript.git kscript_docker
docker run -it --rm -v $(pwd)/kscript_docker:/kscript kscript_tester

cd $KSCRIPT_HOME
docker run -it --rm -v $(pwd):/kscript kscript_tester


#docker rm  `docker ps -q -l` # restart it in the background
docker start  `docker ps -q -l` # restart it in the background
docker attach `docker ps -q -l` # reattach the terminal & stdin

#curl -Lso /bin/kscript https://raw.githubusercontent.com/holgerbrandl/kscript/abb5f4c6ee72ec90d22c0fe913284e92363cad0e/kscript && chmod u+x /bin/kscript
#curl -Lso /bin/kscript https://www.dropbox.com/s/l5g8vr0wz78y3zy/kscript?dl=1 && chmod u+x /bin/kscript

#kscript --help
#kscript --self-update


## or using github repo
#git clone https://github.com/holgerbrandl/kscript.git
cd kscript
export KSCRIPT_HOME=$(pwd)

./gradlew shadowJar

wget https://raw.githubusercontent.com/lehmannro/assert.sh/master/assert.sh
chmod u+x assert.sh

export PATH=$(pwd):${PATH}
kscript --clear-cache


${KSCRIPT_HOME}/test/test_suite.sh


## manuallt test dependency lookup
./kscript --clear-cache
rm -rf ~/.m2/; kscript --clear-cache
resolve_deps() { kotlin -classpath kscript.jar kscript.app.DependencyUtilKt "$@";}

resolve_deps log4j:log4j:1.2.14

mvn -f /tmp/__resdeps__temp__6026124609748923742_pom.xml dependency:build-classpath

## copy newer jar into container
#scp brandl@scicomp-mac-12-usb:/Users/brandl/projects/kotlin/kscript/kscript.jar .


##https://github.com/moby/moby/issues/26872
##https://stackoverflow.com/questions/38532483/where-is-var-lib-docker-on-mac-os-x


## network address docker ps && docker inspect <id> -> IPAdress
#https://stackoverflow.com/questions/17157721/how-to-get-a-docker-containers-ip-address-from-the-host
#sshfs -o root@172.17.0.3:/ /Users/brandl/Desktop/docker_container/ #-o idmap=user -o uid=1001 -o gid=1001
#sshfs -o 172.17.0.3:/ /Users/brandl/Desktop/docker_container/ #-o idmap=user -o uid=1001 -o gid=1001
