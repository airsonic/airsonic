Name:           libresonic
Version:        @VERSION@
Release:        @BUILD_NUMBER@
Summary:        A web-based music streamer, jukebox and Podcast receiver

Group:          Applications/Multimedia
License:        GPLv3
URL:            http://libresonic.org

%description
Libresonic is a web-based music streamer, jukebox and Podcast receiver,
providing access to your music collection wherever you are. Use it
to share your music with friends, or to listen to your music while away
from home.

Apps for Android, iPhone and Windows Phone are also available.

Java 1.6 or higher is required to run Libresonic.

Libresonic can be found at http://libresonic.org

%files
%defattr(644,root,root,755)
/usr/share/libresonic/libresonic-booter-jar-with-dependencies.jar
/usr/share/libresonic/libresonic.war
%attr(755,root,root) /usr/share/libresonic/libresonic.sh
%attr(755,root,root) /etc/init.d/libresonic
%attr(755,root,root) /var/libresonic/transcode/ffmpeg
%attr(755,root,root) /var/libresonic/transcode/lame
%config(noreplace) /etc/sysconfig/libresonic

%pre
# Stop Libresonic service.
if [ -e /etc/init.d/libresonic ]; then
  service libresonic stop
fi

# Backup database.
if [ -e /var/libresonic/db ]; then
  rm -rf /var/libresonic/db.backup
  cp -R /var/libresonic/db /var/libresonic/db.backup
fi

exit 0

%post
ln -sf /usr/share/libresonic/libresonic.sh /usr/bin/libresonic
chmod 750 /var/libresonic

# Clear jetty cache.
rm -rf /var/libresonic/jetty

# For SELinux: Set security context
chcon -t java_exec_t /etc/init.d/libresonic 2>/dev/null

# Configure and start Libresonic service.
chkconfig --add libresonic
service libresonic start

exit 0

%preun
# Only do it if uninstalling, not upgrading.
if [ $1 = 0 ] ; then

  # Stop the service.
  [ -e /etc/init.d/libresonic ] && service libresonic stop

  # Remove symlink.
  rm -f /usr/bin/libresonic

  # Remove startup scripts.
  chkconfig --del libresonic

fi

exit 0

