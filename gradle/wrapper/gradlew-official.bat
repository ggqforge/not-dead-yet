@echo off
rem ===========================================================================
rem  Official Gradle wrapper launcher (fallback path).
rem
rem  Only reached when gradlew.bat finds no local Gradle.
rem  Reads gradle-wrapper.properties and downloads that distribution once
rem  (about 130MB), caching it under GRADLE_USER_HOME\wrapper\dists.
rem ===========================================================================
setlocal

pushd "%~dp0..\.."
set "PROJECT_DIR=%CD%"
popd

if not defined JAVA_HOME (
    echo [ERROR] JAVA_HOME is not set, the official wrapper cannot start.
    exit /b 1
)

"%JAVA_HOME%\bin\java.exe" -Xmx64m -Xms64m ^
    "-Dorg.gradle.appname=gradlew" ^
    -classpath "%PROJECT_DIR%\gradle\wrapper\gradle-wrapper.jar" ^
    org.gradle.wrapper.GradleWrapperMain %*

endlocal & exit /b %ERRORLEVEL%