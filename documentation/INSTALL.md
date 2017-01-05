<!--
# INSTALL.md
# Libresonic/libresonic
-->

# Installing Libresonic

This document is designed to explain how to install LibreSonic as a Tomcat module, on a computer running Linux. As the project expands, this guide will also expand to include other operating systems.

## Installing From Pre-Compiled Package

### Prerequisites

In order to install and run Libresonic, you will need:

  * A JDK installation. 1.8.x series of OpenJDK or Oracle JDK 8+ should work.
  * A running [Tomcat](http://tomcat.apache.org/) server. If you're unfamiliar with Tomcat, there are many [guides](https://www.linode.com/docs/websites/frameworks/apache-tomcat-on-ubuntu-16-04) on it. For debian/ubuntu like distributions, you may need to ensure /etc/default/tomcat8 has the correct JAVA_HOME set.


### Download Libresonic

Daily WAR files are built by Jenkins and available [here](https://jenkins.zifnab.net/job/libresonic/), curtesy of [zifnab06](https://github.com/zifnab06).

1.  Download the latest war file:

		wget https://jenkins.zifnab.net/job/libresonic/lastSuccessfulBuild/artifact/.repository/org/libresonic/player/libresonic-main/6.1.beta2/libresonic-main-6.1.beta2.war -O /var/lib/tomcat8/webapps/libresonic.war

	Note that this command copies the war file directly to the Tomcat webapps directory, and renames it to `libresonic.war`.

2.  Create the libresonic directory and assign ownership to the Tomcat system user (if running tomcat as a service):

		mkdir /var/libresonic
		chown tomcat8:tomcat8 /var/libresonic/

3.  Start Tomcat, or restart it if running as a service, as in the example below using Systemd:

		systemctl restart tomcat8.service

	Note that it may take ~30 seconds after the service restarts for Tomcat to fully deploy the app. You can monitor /var/log/tomcat8/catalina.out for the following message:

		INFO: Deployment of web application archive /var/lib/tomcat8/webapps/libresonic.war has finished in 46,192 ms

4.  In your web browser, navigate to `192.0.2.10:8080/libresonic/`, replacing `192.0.2.0` with your server's IP address, or `127.0.0.1` if installing locally.

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

3.  Using Maven, build Subsonic:

		mvn package

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
