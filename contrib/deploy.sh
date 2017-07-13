#!/bin/bash
# contrib/deploy.sh
# airsonic/airsonic
#
# Helper script to shorten dev/build/deployment
#

sudo systemctl stop tomcat
sudo rm /var/lib/tomcat/webapps/airsonic* -rf
sudo cp airsonic-main/target/airsonic.war /var/lib/tomcat/webapps/
sudo systemctl start tomcat

