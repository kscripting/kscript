# kscript 4.2 features:

* New Github organization 'kscripting'
* Release kscript to Maven Central
* Get rid of kscript-annotations (needed only by Idea project)
* Change a way of executing .kts files (investigate if we can get rid of reflections)
* do not use shadow jar for kscript distribution
  * switched to java executor; it works a bit faster because of that
  * kscript can be released to Maven Central as library, not the whole package
* Change the name of kscript package to io.github.kscripting:kscript
* Get rid of docopt handler from KscriptHandler (helps with using kscript as library)
* Extract shell library
* Deprecate old annotations 
* Fix the bug on ArchLinux
* Version of kscript should be set by Gradle 



* New package for Windows e.g. scoop
* Release scripts in Kotlin
* Windows console support requires @argfiles as kotlin/kotlinc command line might be too long to execute it from console (especially for big classpaths).
* Improve Unit tests coverage 
* Improve batch file for Windows (currently it does not pass failed exitCode)
* Use compilation option -include-runtime: https://kotlinlang.org/docs/command-line.html#create-and-run-an-application
* Integration tests - more tests should be enabled; 
* kscript - some features might be disabled on specific OSes - handle that on code level e.g. throw exception if for some OS feature is not available.
* Deprecate referencing script by $HOME and by '/' (it is handled now safely, but does it make sense to keep it?)
* Compatibility with Kotlin Scripting
* Consider changing a way of executing last command, so that it is not executed by shell, but is executed directly in kscript (main concern: kotlin interactive shell, but maybe this use case is not that important)
* Onboard on brew, docker etc. and other release channels
* 