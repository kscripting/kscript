# kscript 4.1 features:

* Multiplatform tests for different OS-es
* Windows console support requires @argfiles as kotlin/kotlinc command line is too long to execute it from console.

* Fix for packaging + re-enable tests
* Depreciation of @MavenRepository -> @Repository is Kotlin standard
* Depreciation of some old features with WARN (comment based annotations, referencing script by $HOME and by '/' - those references won't work for web scripts)
* OSPath handling ~ home
* Improve Unit tests
