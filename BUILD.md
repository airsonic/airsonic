<!--
# BUILD.md
# Libresonic/libresonic
-->
Requirements
------------

  * Recent version of [Maven](http://maven.apache.org/).
  * A JDK installation. 1.8.x series of OpenJDK or Oracle JDK 8+ should work.
  * Optional: lintian and fakeroot, for .deb package
  * Optional: rpm and rpmlint, for .rpm package
  * Test as follows:

```
$ which mvn
/usr/local/bin/mvn
$ echo $JAVA_HOME
/usr/lib/jvm/java-1.8.0-openjdk.x86_64
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

At this point you are ready to build the basic Libresonic WAR. This is required for all the other build targets, so you should do it before proceeding.

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

You can furthermore go ahead to create a .deb suitable for installation on
Debian or Ubuntu.

```
$ mvn -P full -pl libresonic-booter -am install
$ mvn -P full -pl libresonic-installer-debian -am install
$ sudo dpkg -i ./libresonic-installer-debian/target/libresonic-*.deb
```

Packaged RPM
------------

Building a RPM package is very similar :

```
$ mvn -P full -pl libresonic-booter -am install
$ mvn -P full,rpm -pl libresonic-installer-rpm -am install
$ sudo rpm -ivh libresonic-installer-rpm/target/libresonic-*.rpm
```

Additional release archives
---------------------------

Additional release archives can be built using the following commands :

```
$ mvn -Pfull -pl libresonic-assembly assembly:single
```

These archives are built in `libresonic-assembly/targets` and include :

* The source distribution
* The standalone archive (for use without a WAR container)
* The WAR archive (for WAR containers)

Good luck!
