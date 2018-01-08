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

## make sure to use devel-version
export PATH=${KSCRIPT_HOME}:${PATH}
which kscript

## make sure assert.h is in PATH
which assert.sh || exit 1

cd ${KSCRIPT_HOME}

## build it
./gradlew shadowJar


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
