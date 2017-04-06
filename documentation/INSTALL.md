<!--
# INSTALL.md
# Libresonic/libresonic
-->

# Installing Libresonic

This document is designed to explain how to run Libresonic. Although the
commands here are tailored to Linux, these steps should be easy to also perform
on Windows. If you find something that could be improved in this documentation, please help
contribute on the github project at [https://github.com/Libresonic/libresonic](https://github.com/Libresonic/libresonic).

This document aims to provide steps to install Libresonic in a quick fashion.
However, there are other documents detailing specifics on running Libresonic.
It is recommended to also read them after this document but before actually
running Libresonic. To list some of the important ones:

  * [PROXY](PROXY.md) - Recommended if you plan on exposing Libresonic to the internet
  * [CONFIGURATION](CONFIGURATION.md) - Documents some startup configurations that cannot be changed in the Libresonic Settings Page
  * [DATABASE](DATABASE.md) - Provides guidance on using a database other than HSQL 1.8. Strongly recommended for long term / heavy use.
  * [MIGRATE](MIGRATE.md) - Documents upgrading from an old Subsonic installation
  * [TRANSCODE](TRANSCODE.md) - Mandatory setup if you want Libresonic to convert between formats on the fly

## Installing From Pre-Compiled Package

Libresonic is packaged in a [WAR format](https://en.wikipedia.org/wiki/WAR_(file_format)). 
This format is suitable for any OS running Java. Although the WAR format
typically requires an application container such as
[Tomcat](http://tomcat.apache.org/), Libresonic produces an executable WAR that
can be run standalone.

### Prerequisites

In order to install and run Libresonic, you will need:

  * A JDK installation. 1.8.x series of OpenJDK or Oracle JDK 8+ should work.

### Download Libresonic

WAR files are available on the [Releases Page](https://github.com/Libresonic/libresonic/releases). Choose either stable or develop.

### Tomcat Method

You will need a running [Tomcat](http://tomcat.apache.org/) server. If you're unfamiliar with Tomcat, there are many [guides](https://www.linode.com/docs/websites/frameworks/apache-tomcat-on-ubuntu-16-04) on it. For debian/ubuntu like distributions, you may need to ensure /etc/default/tomcat8 has the correct JAVA\_HOME set.

1.  Download the latest war file:

		wget https://github.com/Libresonic/libresonic/releases/download/v6.2.beta1/libresonic-v6.2.beta1.war -O /var/lib/tomcat8/webapps/libresonic.war

	Note that this command copies the war file directly to the Tomcat webapps directory, and renames it to `libresonic.war`.

2.  Create the libresonic directory and assign ownership to the Tomcat system user (if running tomcat as a service):

		mkdir /var/libresonic
		chown tomcat8:tomcat8 /var/libresonic/

3.  Start Tomcat, or restart it if running as a service, as in the example below using Systemd:

		systemctl restart tomcat8.service

	Note that it may take ~30 seconds after the service restarts for Tomcat to fully deploy the app. You can monitor /var/log/tomcat8/catalina.out for the following message:

		INFO: Deployment of web application archive /var/lib/tomcat8/webapps/libresonic.war has finished in 46,192 ms

4.  In your web browser, navigate to `http://IP_ADDRESS:8080/libresonic/`, replacing `IP_ADDRESS` with your server's IP address, or `127.0.0.1` if installing locally.

### SpringBoot Alternative to Tomcat

If you'd prefer not to use a Tomcat container, you can also run Libresonic as a standalone application.
Note that, in that case, libresonic will available at `http://IP_ADDRESS:8080` (and not `IP_ADDRESS:8080/libresonic/`).

Download the Libresonic Pre-Compiled Package as explained above and put it
anywhere. Then create the libresonic directory. You may have to change the
permissions on the folder to align with the user you will run libresonic as.

```
mkdir /var/libresonic/
```

Now you can simply run java against the libresonic.war package using a command like:

```
java -jar libresonic.war
```

## Installing From Source

### Prerequisites

In order to build, install, and run Libresonic, you will need:

  * A recent version of [Maven](http://maven.apache.org/).
  * A JDK installation. 1.8.x series of OpenJDK or Oracle JDK 8+ should work.
  * A running [Tomcat](http://tomcat.apache.org/) server. If you're unfamiliar with Tomcat, there are many [guides](https://www.linode.com/docs/websites/frameworks/apache-tomcat-on-ubuntu-16-04) on it.

On a Debian-based system, you can install all these prerequisites at once with:

	apt-get update; apt-get install tomcat8 openjdk-7-jdk maven

### Test Your System

1.  Confirm your Maven installation:

		which mvn

2.  Confirm that the $JAVA_HOME environment variable is set:

		echo $JAVA_HOME

3.  If Java is installed, but the `JAVA_HOME` variable not set, be sure to [set it](http://www.cyberciti.biz/faq/linux-unix-set-java_home-path-variable/) before you continue.


### Download and Build Libresonic

1.  Clone the Libresonic repo:

		git clone git://github.com/Libresonic/libresonic.git
		cd libresonic

	If you don't have a GitHub account, use https://github.com/Libresonic/libresonic.git instead.

2.  At the time of this writing, we reccomend building from the development branch, as Libresonic has not had a stable release since being forked.

		git checkout develop

3.  Using Maven, build Libresonic:

		mvn clean package

4.  You should know have a war file:

		ls libresonic-main/target/libresonic.war 
		libresonic-main/target/libresonic.war

5.  Copy the war file to the Tomcat webapps directory:

		cp libresonic-main/target/libresonic.war /var/lib/tomcat8/webapps

6.  Create the libresonic directory and assign ownership to the Tomcat system user (if running tomcat as a service):

		mkdir /var/libresonic
		chown tomcat8:tomcat8 /var/libresonic/

7.  Start Tomcat, or restart it if running as a service, as in the example below using Systemd:

		systemctl restart tomcat8.service

	Note that it may take ~30 seconds after the service restarts for Tomcat to fully deploy the app. You can monitor /var/log/tomcat8/catalina.out for the following message:

		INFO: Deployment of web application archive /var/lib/tomcat8/webapps/libresonic.war has finished in 46,192 ms

8.  In your web browser, navigate to `192.0.2.10:8080/libresonic/`, replacing `192.0.2.0` with your server's IP address, or `127.0.0.1` if installing locally.
