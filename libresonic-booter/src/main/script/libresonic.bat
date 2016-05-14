@echo off

REM  The directory where Libresonic will create files. Make sure it is writable.
set SUBSONIC_HOME=c:\libresonic

REM  The host name or IP address on which to bind Libresonic. Only relevant if you have
REM  multiple network interfaces and want to make Libresonic available on only one of them.
REM  The default value 0.0.0.0 will bind Libresonic to all available network interfaces.
set SUBSONIC_HOST=0.0.0.0

REM  The port on which Libresonic will listen for incoming HTTP traffic.
set SUBSONIC_PORT=4040

REM  The port on which Libresonic will listen for incoming HTTPS traffic (0 to disable).
set SUBSONIC_HTTPS_PORT=0

REM  The context path (i.e., the last part of the Libresonic URL).  Typically "/" or "/libresonic".
set SUBSONIC_CONTEXT_PATH=/

REM  The memory limit (max Java heap size) in megabytes.
set MAX_MEMORY=150

java -Xmx%MAX_MEMORY%m  -Dlibresonic.home=%SUBSONIC_HOME% -Dlibresonic.host=%SUBSONIC_HOST% -Dlibresonic.port=%SUBSONIC_PORT%  -Dlibresonic.httpsPort=%SUBSONIC_HTTPS_PORT% -Dlibresonic.contextPath=%SUBSONIC_CONTEXT_PATH% -jar libresonic-booter-jar-with-dependencies.jar

