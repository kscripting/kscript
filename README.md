# kscript - Having fun with Kotlin scripting


[![GitHub release](https://img.shields.io/github/release/holgerbrandl/kscript.svg)](https://github.com/holgerbrandl/kscript/releases)
[![Build Status](https://travis-ci.org/holgerbrandl/kscript.svg?branch=master)](https://travis-ci.org/holgerbrandl/kscript)
[![Gitter](https://badges.gitter.im/holgerbrandl/kscript.svg)](https://gitter.im/holgerbrandl/kscript?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

Enhanced scripting support for [Kotlin](https://kotlinlang.org/) on *nix-based systems.

Kotlin has some built-in support for scripting already but it is not yet feature-rich enough to be a viable alternative in the shell.

In particular this wrapper around `kotlinc` adds
* Compiled script caching (using md5 checksums)
* Dependency declarations using gradle-style resource locators and automatic dependency resolution with maven
* More options to provide scripts including interpreter mode, reading from stdin, local files or URLs
* Embedded configuration for Kotlin runtime options
* Support library to ease the writing of Kotlin scriptlets
* Deploy scripts as stand-alone binaries

Taken all these features together, `kscript` provides an easy-to-use, very flexible, and almost zero-overhead solution to write self-contained mini-applications with Kotlin.


---
**[`kscript` presentation from KotlinConf2017!](https://holgerbrandl.github.io/kscript_kotlinconf_2017/kscript_kotlinconf.html)**

---

[TOC levels=2,2]: # " "

- [Installation](#installation)
- [Script Input Modes](#script-input-modes)
- [Script Configuration](#script-configuration)
- [Text Processing Mode](#text-processing-mode)
- [Treat yourself a REPL with `--interactive`](#treat-yourself-a-repl-with---interactive)
- [Boostrap IDEA from a `kscript`let](#boostrap-idea-from-a-kscriptlet)
- [Deploy scripts as standalone binaries](#deploy-scripts-as-standalone-binaries)
- [FAQ](#faq)
- [Support](#support)
- [How to contribute?](#how-to-contribute)
- [Acknowledgements](#acknowledgements)


Installation
------------

To use `kscript` just Kotlin and Maven are required. To [install Kotlin](https://kotlinlang.org/docs/tutorials/command-line.html) we recommend [sdkman](http://sdkman.io/install):
```
curl -s "https://get.sdkman.io" | bash  # install sdkman
source ~/.bash_profile                  # add sdkman to PATH

sdk install kotlin                      # install Kotlin
sdk install maven                       # install Maven
```

Once Maven and Kotlin are ready, you can install `kscript` with
```
sdk install kscript
```


To test your installation simply run
```bash
kscript --help
```

`kscript` can update itself to its latest version by running
```bash
kscript --self-update
```

#### Installation without `sdkman`

If you have Kotlin and Maven already and you would like to install the latest `kscript` release without using `sdkman` you can do so by unzipping the [latest ](https://github.com/holgerbrandl/kscript/releases/latest) binary release. Don't forget to update your `$PATH` accordingly.

#### Installation with Homebrew

On MacOS you can install `kscript` also with [Homebrew](https://brew.sh/)
```bash
brew install holgerbrandl/tap/kscript
```

To upgrade to latest version
```bash
brew update
brew upgrade holgerbrandl/tap/kscript
```
#### Build it yourself

To build `kscript` yourself, simply clone the repo and do
```bash
./gradlew assemble

## Run kscript from output dir
./build/libs/kscript
```

## Script Input Modes

The main mode of operation is `kscript <script>`.

The `<script>` can be a Kotlin `*.kts` script file , a script URL, `-` for stdin, a process substitution file handle, a `*.kt` source file with a main method, or some kotlin code.


### Interpreter Usage

To use `kscript` as interpreter for a script just point to it in the shebang line of your Kotlin scripts:

```kotlin
#!/usr/bin/env kscript

println("Hello from Kotlin!")
for (arg in args) {
    println("arg: $arg")
}
```

### Inlined Usage


To use `kscript` in a workflow without creating an additional script file, you can also use one of its supported modes for _inlined usage_. The following modes are supported:

* Directly provide a Kotlin scriptlet as argument
```{bash}
kscript 'println("hello world")'
```


* Pipe a Kotlin snippet into `kscript` and instruct it to read from `stdin` by using `-` as script argument

```{bash}
echo '
println("Hello Kotlin.")
' |  kscript -
```


* Using `heredoc` (preferred solution for inlining) which gives you some more flexibility to also use single quotes in your script:

```{bash}
kscript - <<"EOF"
println("It's a beautiful day!")
EOF
```

* Since the piped content is considered as a regular script it can also have dependencies
```{bash}
kscript - <<"EOF"
//DEPS com.offbytwo:docopt:0.6.0.20150202 log4j:log4j:1.2.14

import org.docopt.Docopt
val docopt = Docopt("Usage: jl <command> [options] [<joblist_file>]")

println("hello again")
EOF
```

* Finally (for sake of completeness), it also works with process substitution and for sure you can always provide additional arguments (exposed as `args : Array<String>` within the script)
```{bash}
kscript <(echo 'println("k-onliner")') arg1 arg2 arg3 
```

Inlined _kscripts_ are also cached based on `md5` checksum, so running the same snippet again will use a cached jar (sitting in `~/.kscript`).


### URL usage

To support remote scriplet repositories, `kscript` can also work with URLs. Consider the following [hello-world-gist-scriptlet](https://github.com/holgerbrandl/kscript/blob/master/examples/url_example.kts) which is hosted on github (but any URL would work). To run it locally as a tool simply refer to it (here using the shortened [raw-URL](https://raw.githubusercontent.com/holgerbrandl/kscript/master/examples/url_example.kts) of the script for better readability)

```bash
kscript https://git.io/v1cG6 my argu ments 
```

To streamline the usage, the first part could be even aliased:
```bash
alias hello_kscript="kscript https://git.io/v1cG6"
hello_kscript my argu ments
```
Via this mechanism, `kscript` allows for easy integration of remotely hosted (mini) programs into data workflows.

URL-scripts are cached locally to speed up processing, and `kscript --clear-cache` can be used to wipe the cache if needed.

See this [blogpost](http://holgerbrandl.github.io/kotlin/2016/12/02/mini_programs_with_kotlin.html) for a more extensive overview about URL support in `kscript`.

Script Configuration
--------------------

The following directives supported by `kscript` to configure scripts:

* `//DEPS` to declare dependencies with gradle-style locators
* `//KOTLIN_OPTS`  to configure the kotlin/java runtime environment
* `//INCLUDE` to source kotlin files into the script
* `//ENTRY` to declare the application entrypoint for kotlin `*.kt` applications


### Declare dependencies with `//DEPS`

To specify dependencies simply use gradle-style locators. Here's an example using [docopt](https://github.com/docopt/docopt.java) and [log4j](http://logging.apache.org/log4j/2.x/)

```kotlin
#!/usr/bin/env kscript
//DEPS com.offbytwo:docopt:0.6.0.20150202,log4j:log4j:1.2.14

import org.docopt.Docopt
import java.util.*


val usage = """
Use this cool tool to do cool stuff
Usage: cooltool.kts [options] <igenome> <fastq_files>...

Options:
 --gtf <gtfFile>     Custom gtf file instead of igenome bundled copy
 --pc-only           Use protein coding genes only for mapping and quantification
"""

val doArgs = Docopt(usage).parse(args.toList())

println("Hello from Kotlin!")
println("Parsed script arguments are: \n" + doArgs)
```

`kscript` will read dependencies from all lines in a script that start with `//DEPS` (if any). Multiple dependencies can be split by comma, space or semicolon.

### Configure the runtime  with `//KOTLIN_OPTS`

`kscript` allows to provide a `//KOTLIN_OPTS` directive followed by parameters passed on to `kotlin` similar to how dependencies are defined:
```kotlin
#!/usr/bin/env kscript
//KOTLIN_OPTS -J-Xmx5g  -J-server

println("Hello from Kotlin with 5g of heap memory running in server mode!")
```

Note: Similar to the runtime you can also tweak the compile step by providing `//COMPILER_OPTS`.

### Ease prototyping with `//INCLUDE`

`kscript` supports an `//INCLUDE` directive to directly include other source files without prior compilation. Absolute and relative paths, as well as URLs are supported. Example:

```kotlin
//utils.kt
fun Array<Double>.median(): Double {
    val (lower, upper) = sorted().let { take(size / 2) to takeLast(size / 2) }
    return if (size % 2 == 0) (lower.last() + upper.first()) / 2.0 else upper.first()
}
```

Which can be now used using the `//INCLUDE` directive with

```kotlin
#!/usr/bin/env kscript

//INCLUDE utils.kt

val robustMean = listOf(1.3, 42.3, 7.).median()
println(robustMean)
```
The argument can be an URL, absolute or relative file path. Note that URLs used in include directives are cached locally to speed up processing, that is `kscript` won't fetch URLs again unless the user actively clears the cache with `kscript --clear-cache`.

For more examples see [here](test/resources/includes/include_variations.kts).


### Use `//ENTRY` to run applications with `main` method


`kscript` also supports running regular Kotlin `kt` files.

Example: `./examples/Foo.kt`:

```kotlin
package examples

//ENTRY examples.Bar

class Bar{
    companion object {
        @JvmStatic fun main(args: Array<String>) {
            println("Foo was called")
        }
    }
}

fun main(args: Array<String>) = println("main was called")
```

To run top-level main instead we would use `//ENTRY examples.FooKt`

The latter is the default for `kt` files and could be omitted


### Annotation driven script configuration

Using annotations instead of comment directives to configure scripts is cleaner and allow for better tooling.

```kotlin
// annotation-driven script configuration
@file:DependsOn("de.mpicbg.scicomp:kutils:0.4")

// comment directive
//DEPS de.mpicbg.scicomp:kutils:0.4
```

 To do so `kscript` supports [annotations](https://github.com/holgerbrandl/kscript-annotations) to be used instead of comment directives:
```kotlin
#!/usr/bin/env kscript

// Declare dependencies
@file:DependsOn("de.mpicbg.scicomp:kutils:0.4")
@file:DependsOn("com.beust:klaxon:0.24", "com.github.kittinunf.fuel:fuel:1.3.1")


// To use a custom maven repository you can declare it with
@file:MavenRepository("imagej-releases","http://maven.imagej.net/content/repositories/releases" )

// For compatibility with https://github.com/ligee/kotlin-jupyter kscript supports also
@file:DependsOnMaven("net.clearvolume:cleargl:2.0.1")
// Note that for compatibility reasons, only one locator argument is allowed for @DependsOnMaven


// Include helper scripts without deployment or prior compilation
@file:Include("util.kt")


// Define kotlin options
@file:KotlinOpts("-J-Xmx5g")
@file:KotlinOpts("-J-server")
@file:CompilerOpts("-jvm-target 1.8")

// declare application entry point (applies on for kt-files)
@file:EntryPoint("Foo.bar") 

print("1+1")
```

To enable the use of these annotations in Intellij, the user must add the following artifact (hosted on jcenter) to the project dependencies:
```
com.github.holgerbrandl:kscript-annotations:1.2
```

`kscript` will automatically detect an annotation-driven script, and if so will declare a dependency on this artifact internally.

Note, that if a script is located in a package other than the root package, you need to import the annotations with (e.g. `import DependsOn`).

Text Processing Mode
--------------------

`kscript` can be used as a speedier and more flexible substitute for built-in terminal text tools such as `awk` or `sed`. Its text processing mode can be enabled with `-t` or `--text`. If so, `kscript` will

* Declare `com.github.holgerbrandl:kscript-support:1.2.4` as dependency for the script. This [support library](https://github.com/holgerbrandl/kscript-support-api) eases the writing of Kotlin scriptlets for text-processing. It includes solutions to common use-cases like argument parsing, data streaming, IO utilities, and various iterators to streamline the writing of scriptlets for text processing.
* Import the `kscript.*` namespace
* Define variable `val lines = kscript.text.StreamUtilKt.resolveArgFile(args)` which returns an iterator over the lines in the first input argument of the script, or the standard input if no file arguments are provided to the script


This allows to to replace `awk`ward constructs (or `sed` or`perl`) with _kotlinesque_ solutions such as

```bash
cat some_file | kscript -t 'lines
    .filter { "^de0[-0]*".toRegex().matches(it) }
    .map { it + "foo:" }
    .print()
'
```

In this example, the extension method [`Iterable<String>.print()`](https://github.com/holgerbrandl/kscript-support-api/blob/master/src/main/kotlin/kscript/text/StreamUtil.kt#L34) to print the lines to stdout comes from the support API. The rest is stdlib Kotlin.

 For more examples using the support library see this [blog post](http://holgerbrandl.github.io/kotlin/2017/05/08/kscript_as_awk_substitute.html).

Treat yourself a REPL with `--interactive`
------------------------------------------

To create an interactive kotlin shell (aka [REPL](https://kotlinlang.org/docs/tutorials/command-line.html#running-the-repl)) with all script dependencies added to the classpath you can use `--interactive`.

For example, let's assume the following short script, named `CountRecords.kts`
```kotlin
#!/usr/bin/env kscript
@file:DependsOn("de.mpicbg.scicomp:kutils:0.4")

import de.mpicbg.scicomp.bioinfo.openFasta

if (args.size != 1) {
    System.err.println("Usage: CountRecords <fasta>")
    kotlin.system.exitProcess(-1)
}

val records = openFasta(java.io.File(args[0]))
println(records.count())
```

To build a REPL that has the declared artifact in its classpath, we can just do

```bash
kscript --interactive CountRecords.kts
```
which will bring up the classpath-enhanced REPL:
```
Creating REPL from CountRecords.kts
Welcome to Kotlin version 1.1.51 (JRE 1.8.0_151-b12)
>>> import de.mpicbg.scicomp.bioinfo.openFasta
>>> 
```

Boostrap IDEA from a `kscript`let
-----------------------------------

Artifacts and versions will differ between scripts, so it is hard to maintain them all in a single project. To nevertheless provide optimal tooling when scripting with Kotlin `kscript` allows to create temporary projects for `<script>` arguments. .

```bash
kscript --idea CountRecords.kts
```
This will open [IntelliJ IDEA](https://www.jetbrains.com/idea/) with a minimalistic project containing just your (1) `<script>` and (2) a generated `gradle.build` file:

![](misc/readme_images/minus_idea.png)

This assumes that you have the Intellij IDEA command line launcher `idea` in your `PATH`. It can be created in IntelliJ under `Tools -> Create Command-line Launcher`

Deploy scripts as standalone binaries
--------------------------------------

To deploy a script simply do

```bash
kscript --package some_script.kts
./some_script --arg u ments
```

The created binary will contain a compiled copy of the script, as well as all declared dependencies (fatjar). Also runtime jvm parameters declared via `@file:KotlinOpts` are used to spin up the JVM.


Just `java` is required to run these binaries.

FAQ
---

### Why is `kscript` not calling the main method in my `.kts` script?

There is [no need](https://kotlinlang.org/docs/tutorials/command-line.html#using-the-command-line-to-run-scripts) for a `main` method in a Kotlin script. Kotlin `*.kts` scripts can be more simplistic compared to more common kotlin `*.kt` source files. The former work without a `main` method by directly running the provided code from top to bottom. E.g.
```
print("hello kotlin!")
```
is a valid Kotlin `kts` script. Plain and simple, no `main`, no `companion`, just a few bits of code.


### Does `kscript` also work for regular kotlin `.kt` source files with a `main` as entry point?

Yes, (since v1.6) you can run kotlin source files through `kscript`. By default it will assume a top-level `main` method as entry-point.

However in case you're using a companion object to declare the entry point, you need to indicate this via the `//ENTRY`/`@file:Entry` directive:


### Why does it fail to read my script file when using cygwin?

In order to use cygwin you need to use windows paths to provide your scripts. You can map cygwin paths using `cygpath`. Example
```bash
kscript $(cygpath -w /cygdrive/z/some/path/my_script.kts)
```

### What are performance and resource usage difference between scripting with kotlin and python?

Kotlin is a compiled language, so there is a compilation overhead when you run a script/application written in Kotlin for the first time.

Kotlin runs (mainly) on the JVM which needs some time (~200ms) to start up. In contrast, the python interpreter has close to zero warmup time.

I think there is a consensus that JVM programs execute much faster than python equivalents. Still, python might be faster depending on your specific usecase. Also, with kotlin-native becoming more mature, you could compile into native binaries directly, which should bring it close to C/C++ performance.

Main motivations for using Kotlin over Python for scripting and development are
* Kotlin is the better designed, more fluent language with much better tooling around it
* The JVM dependency ecosystem allows for strict versioning. No more messing around with virtualenv, e.g. to run a short 10liner against a specific version of numpy.





### Can I use custom artifact repositories?

Yes, via the `@MavenRepository` annotation. See [annotations section](#annotation-driven-script-configuration) or [custom_mvn_repo_annot](test/resources/custom_mvn_repo_annot.kts) for a complete example


Support
-------

Feel welcome to post ideas and suggestions to our [tracker](https://github.com/holgerbrandl/kscript/issues).

More advanced use-cases are documented in the [complementary user guide](docs/user_guide.md)

How to contribute?
------------------

We always welcome pull requests and trouble tickets. :-)


Help to spread the word. Great community articles about `kscript` include
* [Type-safety in the shell with kscript](https://medium.com/@OhadShai/type-safety-in-the-shell-with-kscript-7dd40d022537)
* [Using Kotlin kscript for Preprocessing Data](https://medium.com/@kenkyee/using-kotlin-kscript-for-preprocessing-data-1dbff4eae292)
* [kscript as substitute for awk](https://holgerbrandl.github.io/kotlin/2017/05/08/kscript_as_awk_substitute.html)
* [A complete backend with Kotlin and your favourite text-editor](https://hackernoon.com/a-complete-backend-only-with-kotlin-and-your-favourite-text-editor-582b4528688) -using kscript

You could also show your support by upvoting `kscript` here on github, or by voting for issues in Intellij IDEA which impact `kscript`ing. Here are our top 2 tickets/annoyances that we would love to see fixed:

* [KT-11473](https://youtrack.jetbrains.com/issue/KT-11473) Idea does not allow to debug scripts using the builtin debugger
* [KT-13347](https://youtrack.jetbrains.com/issue/KT-13347) Good code is red in injected kotlin language snippets


To allow for more interactive script development, you could also vote/comment on the most annoying REPL issues.

* [KT-11473](https://youtrack.jetbrains.net/issue/KT-11473) Debugging of Kotlin scripts
* [KT-24789](https://youtrack.jetbrains.net/issue/KT-24789) "Unresolved reference" when running a script which is a symlink to a script outside of source roots
* [KT-12583](https://youtrack.jetbrains.com/issue/KT-12583) IDE REPL should run in project root directory
* [KT-11409](https://youtrack.jetbrains.com/issue/KT-11409) Allow to "Send Selection To Kotlin Console"


Acknowledgements
----------------

The initial version of `kscript` was kindly contributed by [Oscar Gonzalez](https://github.com/oskargb).

Special thanks to [Ilan Pillemer](https://github.com/ilanpillemer), [Andrey Mischenko ](https://github.com/gildor), [Stephen Byrne](https://github.com/stephenbyrne-mfj), [Eugene Susla](https://github.com/EugeneSusla), [Eli Hart](https://github.com/elihart), [Hwijae Lee](https://github.com/hj-lee) and [oshai](https://github.com/oshai) for [contributing](https://github.com/holgerbrandl/kscript/graphs/contributors) PRs to this repo.

Thanks also to the [Scionics Computer Innovation GmbH](https://www.scionics.com/) and the [MPI-CBG](http://www.mpi-cbg.de) for supporting this project.

`kscript` was inspired by [kotlin-script](https://github.com/andrewoma/kotlin-script) which is another great way (now deprecated) to do scripting in Kotlin.
