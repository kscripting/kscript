@echo off

rem Based on kotlinc.bat from the Kotlin distribution

setlocal
call :set_home

if "%_KOTLIN_COMPILER%"=="" set _KOTLIN_COMPILER=org.jetbrains.kotlin.cli.jvm.K2JVMCompiler

if not "%JAVA_HOME%"=="" (
  rem Prepend JAVA_HOME to local PATH to be able to simply execute "java" later in the script.
  set "PATH=%JAVA_HOME%\bin;%PATH%"
)

rem We use the value of the JAVA_OPTS environment variable if defined
if "%JAVA_OPTS%"=="" set JAVA_OPTS=-Xmx256M -Xms32M

rem Iterate through arguments and split them into java and kotlin ones
:loop
set _arg=%~1
if "%_arg%" == "" goto loopend

if "%_arg:~0,2%"=="-J" (
  if "%_arg:~2%"=="" (
    echo error: empty -J argument
    goto error
  )
  set JAVA_OPTS=%JAVA_OPTS% "%_arg:~2%"
) else (
  if "%_arg:~0,2%"=="-D" (
    set JAVA_OPTS=%JAVA_OPTS% "%_arg%"
  ) else (
    set KOTLIN_OPTS=%KOTLIN_OPTS% "%_arg%"
  )
)
shift
goto loop
:loopend

setlocal EnableDelayedExpansion

call :set_java_version
if !_java_major_version! geq 9 (
  rem Workaround the illegal reflective access warning from ReflectionUtil to ResourceBundle.setParent, see IDEA-248785.
  set JAVA_OPTS=!JAVA_OPTS! "--add-opens" "java.base/java.util=ALL-UNNAMED"
)

for /f "tokens=* USEBACKQ" %%o in (`where kscript.bat`) do set ABS_KSCRIPT_PATH=%%o
set CLASS_PATH=%ABS_KSCRIPT_PATH:~0,-16%\bin\*

java !JAVA_OPTS! -classpath %CLASS_PATH% io.github.kscripting.kscript.KscriptKt windows %KOTLIN_OPTS%

goto end

rem ##########################################################################
rem # subroutines

:set_home
  set _BIN_DIR=
  for %%i in (%~sf0) do set _BIN_DIR=%_BIN_DIR%%%~dpsi
  set _KOTLIN_HOME=%_BIN_DIR%..
goto :eof

rem Parses "java -version" output and stores the major version to _java_major_version.
rem Note that this only loads the first component of the version, so "1.8.0_265" -> "1".
rem But it's fine because major version is 9 for JDK 9, and so on.
rem Needs to be executed in the EnableDelayedExpansion mode.
:set_java_version
  set _version=
  rem Parse output and take the third token from the string containing " version ".
  rem It should be something like "1.8.0_275" or "15.0.1".
  for /f "tokens=3" %%i in ('java -version 2^>^&1 ^| findstr /i " version "') do (
    rem Split the string by "-" or "." and take the first token.
    for /f "delims=-. tokens=1" %%j in ("%%i") do (
      rem At this point, _version should be something like "1 or "15. Note the leading quote.
      set _version=%%j
    )
  )
  if "!_version!"=="" (
    rem If failed to parse the output, set the version to 1.
    set _java_major_version=1
  ) else (
    rem Strip the leading quote.
    set _java_major_version=!_version:~1!
  )
goto :eof

:error
set ERRORLEVEL=1

:end
exit /b %ERRORLEVEL%
