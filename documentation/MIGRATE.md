Upgrade to Libresonic from Subsonic
================

This guide helps you to migrate your data from Subsonic to Libresonic. It has been tested with Subsonic 5 to Libresonic 6.

Install Libresonic as described in INSTALL.md. The author of this guide used the standalone solution without Java Tomcat.

After installation of Libresonic, the database needs to be migrated. In preperation for that, stop the Libresonic service

    sudo service libresonic stop

If you ran Subsonic before, your data will be (by default) stored in `/var/subsonic`. Assuming you did not use Libresonic before, we will delete all data from Libresonic

    sudo rm -r /var/libresonic # WARNING: DELETES all Libresonic data (Subsonic data will be kept)

We then copy Subsonic data to Libresonic location. Be aware that a couple of files need to be renamed:

    sudo cp -a /var/subsonic /var/libresonic
    sudo mv /var/libresonic/subsonic_sh.log libresonic_sh.log
    sudo mv /var/libresonic/subsonic.log libresonic.log
    sudo mv /var/libresonic/subsonic.properties libresonic.properties
    sudo mv /var/libresonic/db/subsonic.backup /var/libresonic/db/libresonic.backup
    sudo mv /var/libresonic/db/subsonic.data /var/libresonic/db/libresonic.data
    sudo mv /var/libresonic/db/subsonic.lck /var/libresonic/db/libresonic.lck
    sudo mv /var/libresonic/db/subsonic.log /var/libresonic/db/libresonic.log
    sudo mv /var/libresonic/db/subsonic.properties /var/libresonic/db/libresonic.properties
    sudo mv /var/libresonic/db/subsonic.script /var/libresonic/db/libresonic.script
  
Then start Libresonic service again.

    sudo service libresonic start

Your old settings will be there. If you wish, you can delete subsonic data

    sudo rm -r /var/subsonic # Optional, THIS WILL DELETE SUBSONIC DATA
  
