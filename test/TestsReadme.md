## How to run the unit-tests?

#### 1. Install sdkman
SdkMan https://sdkman.io/ is a nice package manager which makes it easy to install software development tools.


```bash
curl -s "https://get.sdkman.io" | bash
```

#### 2. Install java, kotlin and gradle

```bash
sdk install java 11.0.2-open
sdk install kotlin 1.6.21
sdk install gradle 7.4.1
```

#### 3. Clone repository
Put the cloned repository into $DIR directory.

#### 4. Configure your working environment [optional]
You can configure your working environment by sourcing setup environment script:

```bash
cd $DIR
source linux_environment.sh
```

Sourcing this script allows to use some useful for development commands:
```bash
cdk # bash alias which moves you to your kscript development directory
switchPath # adds/removes development kscript from $PATH env variable
help-dev # prints some useful commands 
```

#### 5. Run integration test suite

```bash
cdk
./gradlew clean assemble test
./gradlew -Dos.type=linux -DincludeTags='posix' integration
<or> 
./gradlew -Dos.type=linux -DincludeTags='posix' integration --tests BootstrapHeaderTest 
```

os.type is usually same as $OSTYPE, except of windows 'cmd' shell.

includeTags can be combined:
https://junit.org/junit5/docs/current/user-guide/#running-tests-tag-syntax-rules
usually you would like something like 'posix | linux' (all tests for posix + linux specific tests)

-- tests - comes from Gradle:
https://docs.gradle.org/current/userguide/java_testing.html#test_filtering


#### 6. Check if all tests passed...

---
Useful commands:

```bash
# scp /Users/brandl/projects/kotlin/kscript/kscript bioinfo:/home/brandl/bin/test/kscript/kscript

#git clone https://github.com/holgerbrandl/kscript ; export KSCRIPT_HOME=$(pwd)/kscript

#export KSCRIPT_HOME="/Users/brandl/projects/kotlin/kscript"
#export KSCRIPT_HOME="/mnt/hgfs/sharedDB/db_projects/kscript"
export KSCRIPT_HOME="/mnt/hgfs/d_data/projects/misc/kscript"
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
kscript --clear-cache

# configure java and kotlin
#export JAVA_HOME=`/usr/libexec/java_home -v 9.0.1`
java -version
#sudo update-alternatives --config java
#update-alternatives -l
#sudo update-alternatives --set java-1.8.0-openjdk-amd64
# export JAVA_HOME=$(update-java-alternatives -l | head -n 1 | awk -F ' ' '{print $NF}')

kotlin -version
sdk list kotlin
#sdk use kotlin 1.3.72
#sdk use kotlin 1.4.10

${KSCRIPT_HOME}/test/linux_suite.sh

# # run again with kotlin 1.0.X
# sdk use kotlin 1.0.6
# kscript --clear-cache
# ./test/test_suite.sh
```

## Remove kscript from path
export PATH=`echo ${PATH} | awk -v RS=: -v ORS=: '/kscript/ {next} {print}'`


For more examples see https://github.com/lehmannro/assert.sh/blob/master/tests.sh

## Bootstrap Header Testing

See [Test Protocol for Bootstrap Header](../misc/bootstrap_tester/README.md)

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

```bash
# download and start artifactory container
modified_image=$(DOCKER_BUILDKIT=1 docker build -q \
  -f test/resources/artifactory_config/Dockerfile \
  test/resources/artifactory_config)
docker run --name artifactory -d -p 8081:8081 $modified_image
```

#### 2. Create and upload a downloadable archive.

```bash
gradle --project-dir test/resources/jar_dependency publishAllPublicationsToMavenRepository
```

#### 3. Then run the following kotlin script with the encrypted password

```bash
echo '
@file:Repository("http://localhost:8081/artifactory/authenticated_repo", user="auth_user", password="password")
@file:DependsOn("com.jcabi:jcabi-aether:0.10.1") // If unencrypted works via jcenter
@file:DependsOnMaven("com.github.holgerbrandl.kscript.test:jartester:1.0-SNAPSHOT") // If encrypted works.
println("Hello, World!")
' |  kscript -c -
```

#### 4. Test environment variable substitution

```bash
export auth_user="auth_user"
export auth_password="password"

echo '
@file:Repository("http://localhost:8081/artifactory/authenticated_repo", user="$auth_user", password="$auth_password")
@file:DependsOn("com.jcabi:jcabi-aether:0.10.1") // If unencrypted works via jcenter
@file:DependsOnMaven("com.github.holgerbrandl.kscript.test:jartester:1.0-SNAPSHOT") // If encrypted works.
println("Hello, World!")
' |  kscript -c -
```

#### 5. Test environment variable substitution with packaging

```bash
export auth_user="auth_user"
export auth_password="password"

echo '
@file:Repository("http://localhost:8081/artifactory/authenticated_repo", user="$auth_user", password="$auth_password")
@file:DependsOn("com.jcabi:jcabi-aether:0.10.1") // If unencrypted works via jcenter
@file:DependsOnMaven("com.github.holgerbrandl.kscript.test:jartester:1.0-SNAPSHOT") // If encrypted works.
println("Hello, World!")
' |  kscript -c --package -
```

#### 6. (Optional) Cleanup Local Maven Cache

```bash
rm -fr ~/.m2/repository/com/github/holgerbrandl/kscript/test/
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
