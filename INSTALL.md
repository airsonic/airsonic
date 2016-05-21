<!--
# INSTALL.md
# Libresonic/libresonic
-->
Requirements
------------

  * Recent version of [Maven](http://maven.apache.org/). 
  * A JDK installation. 1.7.x series of OpenJDK or Oracle JDK 7+ should work. 
  * Optional: lintian and fakeroot, for .deb package
  * Test as follows:

```
$ which mvn
/usr/local/bin/mvn
$ echo $JAVA_HOME
/usr/lib/jvm/java-1.7.0-openjdk.x86_64
$
```

Now you can clone a copy of this repository:

```
$ git clone git://github.com/Libresonic/libresonic.git
$ cd libresonic
$
```

Standalone WAR
--------------

At this point you are ready to build the basic Subsonic WAR. This is required for all the other build targets, so you should do it before proceeding. 

```
$ mvn package
<lots of buildspam>
[INFO] Building war: /path/to/repo/libresonic/libresonic-main/target/libresonic.war
<more buildspam>
$
```

Tomcat Installation
-------------------

The WAR may be copied directly to a Tomcat server's webapps/ directory and deployed.

```
$ cp libresonic-main/target/libresonic.war /var/lib/tomcat6/webapps/
$
```


Packaged .deb
-------------

You can furthermore go ahead to create a .deb suitable for installation on Debian or Ubuntu. These instructions should similarly work with rpm(for RedHat/CentOS or Fedora), but it is has not been tested.

```
$ mvn -P full -pl libresonic-booter -am install
$ mvn -P full -pl libresonic-installer-debian/ -am install
$ sudo dpkg -i ./libresonic-installer-debian/target/libresonic-*.deb
$
```

Good luck!

