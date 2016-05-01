@echo off

REM  The directory where Subsonic will create files. Make sure it is writable.
set SUBSONIC_HOME=c:\subsonic

REM  The host name or IP address on which to bind Subsonic. Only relevant if you have
REM  multiple network interfaces and want to make Subsonic available on only one of them.
REM  The default value 0.0.0.0 will bind Subsonic to all available network interfaces.
set SUBSONIC_HOST=0.0.0.0

REM  The port on which Subsonic will listen for incoming HTTP traffic.
set SUBSONIC_PORT=4040

REM  The port on which Subsonic will listen for incoming HTTPS traffic (0 to disable).
set SUBSONIC_HTTPS_PORT=0

REM  The context path (i.e., the last part of the Subsonic URL).  Typically "/" or "/subsonic".
set SUBSONIC_CONTEXT_PATH=/

REM  The memory limit (max Java heap size) in megabytes.
set MAX_MEMORY=150

java -Xmx%MAX_MEMORY%m  -Dsubsonic.home=%SUBSONIC_HOME% -Dsubsonic.host=%SUBSONIC_HOST% -Dsubsonic.port=%SUBSONIC_PORT%  -Dsubsonic.httpsPort=%SUBSONIC_HTTPS_PORT% -Dsubsonic.contextPath=%SUBSONIC_CONTEXT_PATH% -jar subsonic-booter-jar-with-dependencies.jar

