# kscript 4.1 features:

* Depreciation of @MavenRepository -> @Repository is Kotlin standard
* Depreciation of some old features with WARN (comment based annotations, referencing script by $HOME and by '/' - those references won't work for web scripts)

# kscript 4.2 features:
* Changes in kscript release process - new organization, release KScript jar to Maven, new package for Windows
* Compatibility with Kotlin Scripting
* Windows console support requires @argfiles as kotlin/kotlinc command line might be too long to execute it from console (especially for big classpaths).
* Improve Unit tests coverage 
* Improve batch file for Windows (currently it does not pass failed exitCode)
* Consider changing a way of executing last command, so that it is not executed by shell, but is executed directly in kscript (main concern: kotlin interactive shell, but maybe this use case is not that important)
* Use compilation option -include-runtime: https://kotlinlang.org/docs/command-line.html#create-and-run-an-application
* Integration tests - more tests should be enabled; 
* kscript - some features might be disabled on specific OSes - handle that on code level e.g. throw exception if for some OS feature is not available.
* 
