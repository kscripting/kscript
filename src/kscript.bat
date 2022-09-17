@echo off
setlocal

for /f "tokens=* USEBACKQ" %%o in (`where kscript.bat`) do set ABS_KSCRIPT_PATH=%%o
set JAR_PATH=%ABS_KSCRIPT_PATH:~0,-16%\lib\*

set COMMAND=java -classpath %JAR_PATH% io.github.kscripting.kscript.KscriptKt windows %*

set STDOUT=
set ERRORLEVEL=

for /f "tokens=* USEBACKQ" %%o in (`%COMMAND%`) do set STDOUT=%%o

rem https://stackoverflow.com/questions/10935693/foolproof-way-to-check-for-nonzero-error-return-code-in-windows-batch-file/10936093#10936093
if ERRORLEVEL 1 (
    echo Execution failure: %ERRORLEVEL%
    exit /b %ERRORLEVEL%
)

if not defined STDOUT (
    exit /b %ERRORLEVEL%
)

%STDOUT%

exit /b %ERRORLEVEL%
