@echo off
rem ===========================================================================
rem  Totem Name Display - Windows build entry
rem
rem  Usage:
rem      gradlew.bat              build the jar
rem      gradlew.bat runClient    launch the game for debugging
rem      gradlew.bat clean build  clean rebuild
rem
rem  Prefers a Gradle already installed on this machine.
rem  Falls back to the official wrapper (needs network, ~130MB once) if none found.
rem ===========================================================================
setlocal

set "PROJECT_DIR=%~dp0"
if "%PROJECT_DIR:~-1%"=="\" set "PROJECT_DIR=%PROJECT_DIR:~0,-1%"

rem ---- 1) make sure we have a JDK (a JRE cannot compile) ----
if not defined JAVA_HOME if exist "D:\java\bin\javac.exe" set "JAVA_HOME=D:\java"
if not defined JAVA_HOME for /d %%D in ("%ProgramFiles%\Java\jdk*") do if exist "%%D\bin\javac.exe" set "JAVA_HOME=%%D"
if not defined JAVA_HOME (
    echo [ERROR] No JDK found. Set JAVA_HOME to a folder containing bin\javac.exe and jmods\
    echo         e.g.  set JAVA_HOME=D:\java
    exit /b 1
)
if not exist "%JAVA_HOME%\bin\javac.exe" (
    echo [ERROR] JAVA_HOME=%JAVA_HOME% has no bin\javac.exe - that is a JRE, not a JDK.
    exit /b 1
)

rem ---- 2) Gradle user home (default %USERPROFILE%\.gradle can be unwritable) ----
if not defined GRADLE_USER_HOME set "GRADLE_USER_HOME=%PROJECT_DIR%\..\.gradle-home"

rem ---- 3) locate a usable Gradle ----
set "GRADLE_EXE="
if defined GRADLE_HOME if exist "%GRADLE_HOME%\bin\gradle.bat" set "GRADLE_EXE=%GRADLE_HOME%\bin\gradle.bat"
if not defined GRADLE_EXE if exist "%PROJECT_DIR%\.gradle-local\bin\gradle.bat" set "GRADLE_EXE=%PROJECT_DIR%\.gradle-local\bin\gradle.bat"
if not defined GRADLE_EXE if exist "%TEMP%\gradle\gradle-8.12\bin\gradle.bat" set "GRADLE_EXE=%TEMP%\gradle\gradle-8.12\bin\gradle.bat"
if not defined GRADLE_EXE if exist "%LOCALAPPDATA%\Temp\gradle\gradle-8.12\bin\gradle.bat" set "GRADLE_EXE=%LOCALAPPDATA%\Temp\gradle\gradle-8.12\bin\gradle.bat"
if not defined GRADLE_EXE if exist "%USERPROFILE%\gradle\gradle-8.12\bin\gradle.bat" set "GRADLE_EXE=%USERPROFILE%\gradle\gradle-8.12\bin\gradle.bat"
if not defined GRADLE_EXE if exist "%USERPROFILE%\scoop\apps\gradle\current\bin\gradle.bat" set "GRADLE_EXE=%USERPROFILE%\scoop\apps\gradle\current\bin\gradle.bat"
if not defined GRADLE_EXE if exist "C:\Gradle\gradle-8.12\bin\gradle.bat" set "GRADLE_EXE=C:\Gradle\gradle-8.12\bin\gradle.bat"
if not defined GRADLE_EXE if exist "C:\ProgramData\chocolatey\bin\gradle.bat" set "GRADLE_EXE=C:\ProgramData\chocolatey\bin\gradle.bat"
if not defined GRADLE_EXE for /d %%G in ("%LOCALAPPDATA%\Temp\gradle\gradle-*") do if not defined GRADLE_EXE if exist "%%G\bin\gradle.bat" set "GRADLE_EXE=%%G\bin\gradle.bat"
if not defined GRADLE_EXE for /d %%G in ("%USERPROFILE%\gradle\gradle-*") do if not defined GRADLE_EXE if exist "%%G\bin\gradle.bat" set "GRADLE_EXE=%%G\bin\gradle.bat"

echo [info] JAVA_HOME        = %JAVA_HOME%
echo [info] GRADLE_USER_HOME = %GRADLE_USER_HOME%

if defined GRADLE_EXE goto useLocal
echo [info] no local Gradle found, using official wrapper (first run downloads ~130MB)
echo.
call "%PROJECT_DIR%\gradle\wrapper\gradlew-official.bat" %*
goto done

:useLocal
echo [info] using local Gradle = %GRADLE_EXE%
echo.
call "%GRADLE_EXE%" --no-daemon --console=plain -p "%PROJECT_DIR%" %*

:done
endlocal & exit /b %ERRORLEVEL%