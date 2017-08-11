# kscript - Having fun with Kotlin scripting

[![Build Status](https://travis-ci.org/holgerbrandl/kscript.svg?branch=master)](https://travis-ci.org/holgerbrandl/kscript)

Enhanced scripting support for [Kotlin](https://kotlinlang.org/) on *nix-based systems.

Kotlin has some built-in support for scripting already but it is not yet feature-rich enough to be a viable alternative in the shell.

In particular this wrapper around `kotlinc` adds
* Compiled script caching (using md5 checksums)
* Dependency declarations using gradle-style resource locators and automatic dependency resolution with maven
* More options to provide scripts including interpreter mode, reading from stdin, local files or URLs
* Embedded configuration for Kotlin runtime options
* Support library to ease the writing of Kotlin scriptlets

Taken all these features together, `kscript` provides an easy-to-use, very flexible, and almost zero-overhead solution to write self-contained mini-applications with Kotlin.


Installation
------------

To use `kscript` just Kotlin and Maven are required. To [install Kotlin](https://kotlinlang.org/docs/tutorials/command-line.html) we recommend [sdkman](http://sdkman.io/install.html):
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

If you have Kotlin and Maven already and you would like to install the latest `kscript` release to your `~/bin` without using `sdkman` you can do so with:

```bash
curl -Lso ~/bin/kscript https://git.io/v9R73 && chmod u+x ~/bin/kscript
```

Interpreter Usage
-----------------

To use `kscript` as interpreter for a script just point to it in the shebang line of your Kotlin scripts:

```kotlin
#!/usr/bin/env kscript

println("Hello from Kotlin!")
for (arg in args) {
    println("arg: $arg")
}
```

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


Inlined Usage
-------------


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


URL usage
---------

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


Support API
-----------


`kscript` is complemented by a [support library](https://github.com/holgerbrandl/kscript-support-api) to ease the writing of Kotlin scriptlets. The latter includes solutions to common use-cases like argument parsing, data streaming, IO utilities, and various iterators to streamline the development of kscript applications.

When using the direct script arguments (like in the example below) the methods in the the `kscript.*` namespace and the corresponding dependency `com.github.holgerbrandl:kscript:1.2.2` are automatically added as prefix to the script by convention. This allows for  sed-like constructs like

```bash
cat some_file | kscript 'stdin.filter { "^de0[-0]*".toRegex().matches(it) }.map { it + "foo:" }.print()'
```

The elements that come from our [support library](https://github.com/holgerbrandl/kscript-support-api) in the example are the [`stdin`](https://github.com/holgerbrandl/kscript-support-api/blob/master/src/main/kotlin/kscript/text/StreamUtil.kt#11) object of type `Sequence<String>` to iterate over the standard input, and the extension method [`print`](https://github.com/holgerbrandl/kscript-support-api/blob/master/src/main/kotlin/kscript/text/StreamUtil.kt#L34) to print the lines to stdout. The rest is stdlib Kotlin.

 For more  examples using the support library see this [blog post](http://holgerbrandl.github.io/kotlin/2017/05/08/kscript_as_awk_substitute.html).



FAQ
---


### Does it also work for regular kotlin class files `.kt`?

Yes, you can run kotlin classes through kscript. By default `kscript` will assume a top-level `main` method as entry-point.

However in case you're using a companion object to declare the entry point, you can indicate this via the `//ENTRY` directive:

```kotlin
#!/usr/bin/env kscript

package test

//DEPS log4j:log4j:1.2.14
//ENTRY Foo

class User(val age: Int)

fun findUserByName(name: String): User = null!!

class Foo{
    companion object {
        @JvmStatic fun main(args: Array<String>) {
            println("foo companion was called")
        }
    }
}
```
The intial shebang could be left out by running the script directly with `kscript some.kt`



### How to adjust the memory settings for the JVM in my scriptlet?

`kscript` allows to provide a `//KOTLIN_OPTS` line followed by parameters passed on to `kotlin` similar to how dependencies are defined:
```kotlin
#!/usr/bin/env kscript
//KOTLIN_OPTS -J-Xmx5g  -J-server

println("Hello from Kotlin with 5g of heap memory running in server mode!")
```

If environmental variable `KOTLIN_OPTS` is set, it will override the value set in the scriptlet.

### Scripts with a main method do not run with `kscript`?

There is [no need](https://kotlinlang.org/docs/tutorials/command-line.html#using-the-command-line-to-run-scripts) for a main method in a Kotlin script. Kotlin `*.kts` scripts can be more simplistic compared to regular kotlin `*.kt` source files and work without a main method by directly running the provided code. E.g.
```
print("hello kotlin!")
```
is a valid Kotlin `kts` script.

Regular class files are not supported [yet](https://github.com/holgerbrandl/kscript/issues/31#issuecomment-309976103) by `kscript`. This is because a `kt` Kotlin source file might contain multiple classes with a main method in each, so the entry point might not be always clearly defined. (see #31)

Support
-------

Feel welcome to post ideas and suggestions to our [tracker](https://github.com/holgerbrandl/kscript/issues).


How to contribute?
------------------

We always welcome pull requests. :-)

You could also show your support by upvoting `kscript` here on github, or by voting for issues in Intellij IDEA which impact  `kscript`ing. Here are our top 3 tickets/annoyances that we would love to see fixed:

1. [KT-13347](https://youtrack.jetbrains.com/issue/KT-13347) Good code is red in injected kotlin language snippets
2. [KT-16802](https://youtrack.jetbrains.com/issue/KT-16802) Good code is always red when editing kts-script files under Kotlin v1.1
3. [KT-10368](https://youtrack.jetbrains.com/issue/KT-10368) Kotlin scratch files not runnable?



Acknowledgements
----------------

The initial version of `kscript` was kindly contributed by [Oscar Gonzalez](https://github.com/oskargb).

`kscript` was inspired by [kotlin-script](https://github.com/andrewoma/kotlin-script) which is another great way to do scripting in Kotlin.

