# Complementary User Guide


See [README](../README.md) for general instructions about how to use `kscript.

In here more specific use-cases are discussed.


__{work in progress}__


Feel welcome to submit a [ticket](https://github.com/holgerbrandl/kscript/issues) if you think something should be detailed out better in here.

[TOC levels=3]: # " "

- [Requirements for running `kscript`](#requirements-for-running-kscript)
- [Create interpreters for custom DSLs](#create-interpreters-for-custom-dsls)
- [Tips and tricks](#tips-and-tricks)
    - [Display images inline and open other files](#display-images-inline-and-open-other-files)
- [Testing](#testing)
- [Text Processing](#text-processing)
- [Examples](#examples)
    - [Bioinformatics](#bioinformatics)


## Requirements for running `kscript`

Always needed when running `kscript`:
* `jar` (not sure if this is part of jre or just jdk;
* `kotlinc` to compile the script and a wrapper class
* `kotlin` to run the user application (and to run itself) which itself requires a jre
* `bash` is used for the launcher

optional dependencies
* `mvn` to resolve dependencies if present in the script

Actually the repo contains a docker container spec that details out what is needed to run kscript. See https://github.com/holgerbrandl/kscript/blob/master/misc/Dockerfile


## Create interpreters for custom DSLs

`kscript` makes it easy to derive custom interpreters. All you need to do is to write a wrapper that declares a preamble. See [`mydsl`](https://github.com/holgerbrandl/kscript/tree/master/test/resources/custom_dsl/mydsl) for an example interpreter called `mydsl`.

Before you can start using `mydsl` you need to add it to it to your `$PATH`

```
export PATH=${PATH}:${KSCRIPT_HOME}/test/resources/custom_dsl
```


It can be used in the same way as `kscript` including all command-line arguments:

```bash
mydsl "1+1"
```

Usage help is customized (within some limits).
For more customizability you could for sure also fork the repo, and take it from there. :-)
```bash
mydsl --help
```

```
mydsl - Enhanced scripting support for Kotlin on *nix-based systems.

Usage:
 mydsl [options] <script> [<script_args>]...
 mydsl --clear-cache
...
```

_Feel_ the preamble:
```bash
mydsl - <<"EOF"

println(foo)
included()
EOF
```

```
bar
```

Here `foo` and `included()` were declared in the `mydsl` preamble.


For sure you can still declare additional dependencies, and `kscript` will consolidate the file structure internally
```
mydsl - <<"EOF"
@file:DependsOn("com.beust:klaxon:0.24")

import com.beust.klaxon.Parser

val p = Parser()

println(foo)
included()
EOF
```


Note: For remote debugging export the preamble and run the instrumented version of `kscript`
```
$KSCRIPT_HOME/misc/experimental/kscriptD "42"
```


## Tips and tricks


### Display images inline and open other files

When using iterm2 it is possible to print inlined image output directly into the terminal.

Suggested by [@yschimke](https://github.com/yschimke) in  [#51](https://github.com/holgerbrandl/kscript/issues/51)

## Testing
When an IDEA project is generated with the `--idea` command it is configured to consider all project files as test sources.

This means that you can include unit test code, such as with JUnit, in your script files and run the tests with Gradle.

For example, to run JUnit tests you would include the junit dependency at the top of your Kotlin file:
```kotlin
@file:DependsOn("junit:junit:4.12")
```

and then include a class with your tests somewhere in that file:
```kotlin
class MyTests {
    @Test
    fun myTest() {
        check(1 + 1 == 2)
    }
}
```

You can run this test like usual, either from the IDE (by right clicking on the function or class and selecting "run") or from the command line (with `./gradlew test`).

### Caveats
- These tests can go in either .kts files or the .kt files that your script depend on via `@file:Include`. However, tests in a `.kts` file can only be run from the command line
- If you encounter any Gradle build errors such as "Duplicate JVM class name" then try cleaning the project first - `./gradlew clean test`
- Kscript does not differentiate test dependencies, so any test code or test imports in your script will be included when creating a packaged binary with `--package`

### Testing on CI
If you have script tests that you would like to run in automated environments, like CI, then you likely don't want to open up an Intellij IDEA instance to do so.

However, to setup the gradle project to run the tests we still need to use the `--idea` command. To prevent this command from failing, you can create a mock `idea`:
```
touch idea && chmod +x idea
```

The `--idea` command outputs the directory that the project was created in, which you can use to determine where to run your tests.

For example, you could do something like this to generate the project and test it in one command.
```bash
project_path=$(kscript --idea YourScript.kts 2>&1 | grep --line-buffered  "Project set up at" | cut -d'/' -f2-)
cd "/$project_path"
gradle test
```

Note that the Gradlew wrapper is not automatically created by `kscript --idea` so `./gradlew` will not be available. This uses a gradle instance from the PATH instead.

## Text Processing

`kscript` allows to perform `sed`/`awk` text streaming/processing.


Perform grep-lik operations
```kscript
kscript 'lines.split().filter{ it[7] == "UA" }' src/test/resources/some_flights.txt
```

See see this [blog post](http://holgerbrandl.github.io/kotlin/2017/05/08/kscript_as_awk_substitute.html).

For very general table processing, we recommend [csvkit](https://csvkit.readthedocs.io). For more specific table processing you should most likely push tables through R.


## Examples




### Bioinformatics


1. Fasta

2. Fastq

3. Bed

3. Bam

4. HPC



