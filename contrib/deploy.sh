#!/bin/bash
# contrib/deploy.sh
# Libresonic/libresonic
#
# Helper script to shorten dev/build/deployment
#

sudo systemctl stop tomcat
sudo rm /var/lib/tomcat/webapps/libresonic* -rf
sudo cp libresonic-main/target/libresonic.war /var/lib/tomcat/webapps/
sudo systemctl start tomcat

