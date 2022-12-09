# kscript 4.2 features:

* Release kscript to Maven Central

* Change a way of executing .kts files (investigate if we can get rid of reflections)
* Onboard on brew
* Deprecate KotlinOptions
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
* Onboard on docker etc. and other release channels
* Abstraction for shell command (Command class containing e.g. environment variables)