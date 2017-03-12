# Startup Configuration Guide

Libresonic has some system-wide configuration. These configurations are stored in the 
`libresonic.properties` file. There is one exception, which is the `libresonic.home` parameter which 
is supplied as a Java System Property.

## `libresonic.properties`
These parameters are simple key-value pairs stored in a list. It is recommended that these parameters
are changed through the web interface settings page. However, they can also be modified directly. Shutdown
your server first, modify, then start it for changes to take effect.

## Java Parameters
The `libresonic.home` parameter is a Java System Property that is not modifiable through the web interface. 
It must be configured via Java startup parameters. See below for steps to do this.

#### `libresonic.home`
This parameter dictates the folder where Libresonic will store its logs, 
settings, transcode binaries, index and database if using the default H2 
database. As such it is recommended to backup this folder.

*default: `/var/libresonic` or `C:\\music`*

#### Setting Java Parameters on Tomcat
As described in the [RUNNING.txt](http://tomcat.apache.org/tomcat-8.0-doc/RUNNING.txt) doc provided by tomcat,
you can create a file named `setenv.sh` or for windows `setenv.bat` in the Tomcat home `bin` folder to  modify  the 
java args.

Here is an example of a `setenv.sh` file (`setenv.bat` has slightly different syntax):
```
export JAVA_OPTS="$JAVA_OPTS -Dlibresonic.home=/home/andrew/.cache/libresonic-test"
```

#### Setting Java Parameters for Standalone Package (SpringBoot)
When running the standalone package, add `-Dlibresonic.home=YOUR_PATH_HERE` to the `java` command line right before the 
`-jar` argument. Here is an example for linux (windows users will want to use their OS specific path syntax i.e. 
`C:\\your\path`)

```
java -Dlibresonic.home=/home/andrew/libresonichome -jar libresonic.war
```
