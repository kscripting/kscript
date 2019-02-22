## How to run the unit-tests?


We use https://github.com/lehmannro/assert.sh for the tests. To install it just do:


```bash
cd ~/bin
wget https://raw.githubusercontent.com/lehmannro/assert.sh/master/assert.sh
chmod u+x assert.sh
```




To run the tests, just run the [`test_suite.sh`](test_suite.sh)
```bash
# scp /Users/brandl/projects/kotlin/kscript/kscript bioinfo:/home/brandl/bin/test/kscript/kscript

#git clone https://github.com/holgerbrandl/kscript ; export KSCRIPT_HOME=$(pwd)/kscript

export KSCRIPT_HOME="/Users/brandl/projects/kotlin/kscript"
#export KSCRIPT_HOME="/cygdrive/z/kscript"
## change into this/test directory

## make sure assert.h is in PATH
which assert.sh || exit 1

cd ${KSCRIPT_HOME}

## build it
./gradlew assemble


## make sure to use devel-version from build/libs
export PATH=${KSCRIPT_HOME}/build/libs:${PATH}
which kscript

## clean up the environment
#sdk use kotlin 1.1-RC
kscript --clear-cache

${KSCRIPT_HOME}/test/test_suite.sh

# # run again with kotlin 1.0.X
# sdk use kotlin 1.0.6
# kscript --clear-cache
# ./test/test_suite.sh
```

## Remove kscript from path
export PATH=`echo ${PATH} | awk -v RS=: -v ORS=: '/kscript/ {next} {print}'`


For more examples see https://github.com/lehmannro/assert.sh/blob/master/tests.sh


## Tests for dynamic dependencies

To allow for dynamic dependency resolution, dependencies need to be present in local `.m2` cache already. So before we can run
```bash
${KSCRIPT_HOME}/test/resources/depends_on_dynamic.kts
```
we need to resolve actual versions into the local cache with something like
```bash
${KSCRIPT_HOME}/test/resources/depends_on_maven_annot.kts
```

## Notes

How to modulate PATH?

See http://stackoverflow.com/questions/370047/what-is-the-most-elegant-way-to-remove-a-path-from-the-path-variable-in-bash

```

## remove devel version from path

PATH=`echo $PATH | sed -e 's/:\/Users\/brandl\/projects\/kotlin\/kscript/$/g'`
which kscript
kscript --version
which resdeps.kts
kscript --clear-cache
kscript https://git.io/v1cG6 my argu ments

```


## Manual testing

As of this writing, testing the credentials is only done manually with a dockerized Artifactory.

#### 1. Set up preconfigured artifactory with docker.

```
# download and start artifactory container
docker run --name artifactory -d -p 8081:8081 docker.bintray.io/jfrog/artifactory-oss:latest

# Copy preconfigured gloabl config (with custom repo) and security config (with credentials user) into container.
docker cp ./test/resources/artifactory_config/artifactory.config.xml artifactory:/var/opt/jfrog/artifactory/etc/artifactory.config.import.xml
docker cp ./test/resources/artifactory_config/security_descriptor.xml artifactory:/var/opt/jfrog/artifactory/etc/security.import.xml

# Make the configs accessable
docker exec -u 0 -it artifactory sh -c 'chmod 777 $ARTIFACTORY_HOME/etc/*.import.xml'

# Restart docker after is done with initial booting (otherwise restart breaks the container).
echo "sleeping for 15..." && sleep 15
docker restart artifactory
```

#### 2. Create and upload a downloadable archive.
```
tmpClass=$(mktemp --suffix ".class")
tmpZipDir=$(mktemp -d)
echo "public class something() {}" > $tmpClass
zip $tmpZipDir/tmp.zip $tmpClass
curl --request PUT -u admin:password -T $tmpZipDir/tmp.zip http://localhost:8081/artifactory/authenticated_repo/group/somejar/1.0/somejar-1.0.jar
```

#### 3. Then run the following kotlin script with the encrypted password
```
echo '
@file:MavenRepository("my-art", "http://localhost:8081/artifactory/authenticated_repo", user="auth_user", password="password")
@file:DependsOn("com.jcabi:jcabi-aether:0.10.1") // If unencrypted works via jcenter
@file:DependsOnMaven("group:somejar:1.0") // If encrypted works.
println("Hello, World!")
' |  kscript -
```

### Additional info for manual testing

- Docker & container docu: https://www.jfrog.com/confluence/display/RTF/Installing+with+Docker
- Loading configs docu: https://www.jfrog.com/confluence/display/RTF/Configuration+Files

```
# get active security descriptor
curl -u admin:password -X GET -H "Accept: application/xml" http://localhost:8081/artifactory/api/system/security > ./test/resources/artifactory_config/security_descriptor.xml

# Also works with encrypted password instead of plaintext.
curl -u admin:password -X GET http://localhost:8081/artifactory/api/security/encryptedPassword
```
