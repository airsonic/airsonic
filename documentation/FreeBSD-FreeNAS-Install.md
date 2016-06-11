# Installing Libresonic on FreeBSD 10.3 and FreeNAS 9.10

### Preamble
This guide will wallk you through the process of deploying Libresonic on FreeBSD either in a Jail on on the main system.  The prerequisites are you have root access on your FreeBSD machine (or jail), the ip address of the machine (or jail) and the Libresonic war available at the [Libresonic github page](https://github.com/Libresonic/libresonic/releases)

If on FreeNAS create a standard jail in the web interface and enter the shell.

### 1. Install Tomcat
To run Libresonic we need a server to run it in.  Log into your machine and then run these commands either as root or with sudo

    #pkg install tomcat8 nano

Hit y on all prompts to complete installation of Tomcat

### 2.Configure Tomcat
Edit Tomcat's user configuration file with your favourite text editor.  We installed nano in step 1.

	#rm /usr/local/apache-tomcat-8.0/conf/tomcat-users.xml
    #nano /usr/local/apache-tomcat-8.0/conf/tomcat-users.xml 


Copy this

	<tomcat-users xmlns="http://tomcat.apache.org/xml"
              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="http://tomcat.apache.org/xml tomcat-users.xsd"
              version="1.0">

	<role rolename="manager-gui"/>
 	<role rolename="manager-script"/>
 	<role rolename="manager-jmx"/>
 	<role rolename="manager-status"/>
 	<role rolename="admin-gui"/>
 	<role rolename="admin-script"/>
 	<user username="admin" password="admin" roles="manager-gui,manager-script,manager-jmx,manager-status,admin-gui,admin-script"/>

	</tomcat-users>

Use Ctrl-O to save and y to confirm.  Ctrl-X to exit Nano.

If you wish to use a different username and password please append the second last line to contain your preferred username and password.

    <user username="yourusername" password="yourpassword" roles="manager-gui,manager-script,manager-jmx,manager-status,admin-gui,admin-script"/>

### 3. Start and test Tomcat
    #echo tomcat8_enable="YES" >> /etc/rc.conf
    #service tomcat8 start

Test Tomcat is listening on port 8080

    # netstat -an | grep 8080
It should return a line containing the IP address of your system (or jail).  ie mine returns

     tcp4	0	0 10.0.0.10.8080	*.*	LISTEN
If in a jail it may also return the line "netstat: kvm not available: /dev/mem: No such file or directory" This can be ignored.

### 4. Create directories and set up permissions
    #mkdir /var/libresonic
    #chown -R www:www /var/libresonic
    #chown -R www:www /usr/local/apache-tomcat-8.0/webapps

### 5. Deploy Libresonic
Open a web browser and enter your servers IP address in the url bar followed by :8080  eg

    10.0.0.10:8080

You should be greeted by the Apache Tomcat page.  Click on the Manager App button on the right of the page and enter the username and password used in step 3.  Default was username: admin and password: admin

Scroll down to Deploy and the subheading "WAR file to deploy" hit choose file and select the libresonic.war downloaded in the preamble.  After selecting press the deploy button.  Scroll up and press start.  When the page refreshes a message "OK - Started application at context path /libresonic-v6.1.beta1" should be visible.

### 6. Navigate to Libresonic

In a browser.  Take your server IP address and port and append the the context path from above

ie if the War deployed was called libresonic-v6.1.beta1.war navigate to:

    10.0.0.10:8080/libresonic-v6.1.beta1/

### 7. Log into Libresonic

Log in.  The default is username: admin password: admin
Follow the prompts on the web page to change the password.  This will log you out.  Please re-login with your new password

### 8. Set up media
If you are on FreeBSD in a jail, consult the documentation for your Jail Manager tool on how to pass through storage.  If using FreeNAS please use the FreeNAS webui to pass through the dataset containing your music.

In Libresonic click 2. Setup Media folders.

Name your media folder and put in the path to your music.  Then click "Scan media folders now"

Congratulations you have set up Libresonic

##Transcoding Support

If you want transcoding and DON'T need mp3 support

    #pkg install ffmpeg
    #ln -s /usr/local/bin/ffmpeg /var/libresonic/transcode/ffmpeg
    #service tomcat8 restart

Congratulations you have transcoding enabled

If you need mp3 support and most likely you will the process is more arduous as FreeBSD's ffmpeg doesn't contain mp3 support by default and must be configured and compiled by the user.

### 1. Install ffmpeg dependencies and Ports Tree

Install the dependencies required to build and use ffmpeg

    #pkg install yasm binutils texi2html frei0r v4l_compat gmake pkgconf perl5 fontconfig freetype2 opencv-core schroedinger libtheora libv4l libva libvdpau libvorbis libvpx libx264 xvid gnutls libiconv

Now install the FreeBSD Ports Tree

    #portsnap fetch
    #portsnap extract
    

### 2. Build ffmpeg
Navigate to the ffmpeg port directory

    #cd /usr/ports/multimedia/ffmpeg

Configure ffmpeg build

    #make configure

This will bring up a menu.  Scroll down using arrow keys to "LAME" and hit the spacebar to enable it.  Press enter to continue.
The ffmpeg source files will automatically be downloaded then you will be presented with an additional prompt to install documentation.  I uncheck with spacebar then press enter to continue.  

Commence build and installation of ffmpeg

    #make install clean

Building ffmpeg will take some time depending on the capabilities of your machine, please be patient.

Link ffmpeg to where Libresonic expects the transcoder to be.  

    #ln -s /usr/local/bin/ffmpeg /var/libresonic/transcode/ffmpeg
    
Finally restart tomcat

    #service tomcat8 restart

Congratulations you have ffmpeg with mp3 support installed ready for Libresonic to use.
