@echo off
setlocal
set MVNW_MAVEN_VERSION=3.9.16
set MVNW_BASE=%USERPROFILE%\.m2\wrapper\dists\apache-maven-%MVNW_MAVEN_VERSION%
set MVNW_HOME=%MVNW_BASE%\apache-maven-%MVNW_MAVEN_VERSION%
if exist "%MVNW_HOME%\bin\mvn.cmd" goto runmaven
if not exist "%MVNW_BASE%" mkdir "%MVNW_BASE%"
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $zip='%MVNW_BASE%\apache-maven-%MVNW_MAVEN_VERSION%-bin.zip'; if (!(Test-Path $zip)) { Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MVNW_MAVEN_VERSION%/apache-maven-%MVNW_MAVEN_VERSION%-bin.zip' -OutFile $zip }; Expand-Archive -LiteralPath $zip -DestinationPath '%MVNW_BASE%' -Force"
if errorlevel 1 exit /b %errorlevel%
:runmaven
call "%MVNW_HOME%\bin\mvn.cmd" %*
set MVNW_EXIT=%ERRORLEVEL%
endlocal & exit /b %MVNW_EXIT%
