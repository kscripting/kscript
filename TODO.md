# kscript 4.1 features:

* Windows console support requires @argfiles as kotlin/kotlinc command line is too long to execute it from console.
* Depreciation of @MavenRepository -> @Repository is Kotlin standard
* Depreciation of some old features with WARN (comment based annotations, referencing script by $HOME and by '/' - those references won't work for web scripts)
* Improve Unit tests coverage
* Packaging:
  - Tests should be adjusted, so that they are using previously printed package location to do the checks.
  - missing -classpath ?
  - missing kotlin-args ?
    (real java command should be taken from kotlinc script)
* Use compilation option -include-runtime: https://kotlinlang.org/docs/command-line.html#create-and-run-an-application
* Integration tests - more tests should be enabled; 
* kscript - some features might be disabled on specific OSes - handle that on code level e.g. throw exception if for some OS feature is not available.
* Improve batch file for Windows (currently it does not pass failed exitCode)
