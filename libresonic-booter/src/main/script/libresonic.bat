@echo off

REM  The directory where Libresonic will create files. Make sure it is writable.
set LIBRESONIC_HOME=c:\libresonic

REM  The host name or IP address on which to bind Libresonic. Only relevant if you have
REM  multiple network interfaces and want to make Libresonic available on only one of them.
REM  The default value 0.0.0.0 will bind Libresonic to all available network interfaces.
set LIBRESONIC_HOST=0.0.0.0

REM  The port on which Libresonic will listen for incoming HTTP traffic.
set LIBRESONIC_PORT=4040

REM  The port on which Libresonic will listen for incoming HTTPS traffic (0 to disable).
set LIBRESONIC_HTTPS_PORT=0

REM  The context path (i.e., the last part of the Libresonic URL).  Typically "/" or "/libresonic".
set LIBRESONIC_CONTEXT_PATH=/

REM  The memory limit (max Java heap size) in megabytes.
set MAX_MEMORY=150

java -Xmx%MAX_MEMORY%m  -Dlibresonic.home=%LIBRESONIC_HOME% -Dlibresonic.host=%LIBRESONIC_HOST% -Dlibresonic.port=%LIBRESONIC_PORT%  -Dlibresonic.httpsPort=%LIBRESONIC_HTTPS_PORT% -Dlibresonic.contextPath=%LIBRESONIC_CONTEXT_PATH% -jar libresonic-booter-jar-with-dependencies.jar

