<!--
# README.md
# Libresonic/libresonic
-->
Libresonic
========

What is Libresonic?
-----------------

Libresonic is a free, web-based media streamer, providing ubiqutious access to your music. Use it to share your music with friends, or to listen to your own music while at work. You can stream to multiple players simultaneously, for instance to one player in your kitchen and another in your living room.

Libresonic is designed to handle very large music collections (hundreds of gigabytes). Although optimized for MP3 streaming, it works for any audio or video format that can stream over HTTP, for instance AAC and OGG. By using transcoder plug-ins, Libresonic supports on-the-fly conversion and streaming of virtually any audio format, including WMA, FLAC, APE, Musepack, WavPack and Shorten.

If you have constrained bandwidth, you may set an upper limit for the bitrate of the music streams. Libresonic will then automatically resample the music to a suitable bitrate.

In addition to being a streaming media server, Libresonic works very well as a local jukebox. The intuitive web interface, as well as search and index facilities, are optimized for efficient browsing through large media libraries. Libresonic also comes with an integrated Podcast receiver, with many of the same features as you find in iTunes.

Based on Java technology, Libresonic runs on most platforms, including Windows, Mac, Linux and Unix variants.


History
-----

The original [Subsonic](http://www.subsonic.org/) is developed by [Sindre Mehus](mailto:sindre@activeobjects.no). Subsonic was open source through version 6.0-beta1, and closed-source from then onwards.

Libresonic is maintained by [Eugene E. Kashpureff Jr](mailto:eugene@kashpureff.org). It originated as an unofficial("Kang") of Subsonic which did not contain the Licensing code checks present in the official builds. With the announcement of Subsonic's closed-source future, a decision was made to make a full fork and rebrand to Libresonic.

Libresonic will strive to maintain compatibility and stability for Subsonic users, including a clean upgrade path. New features and refactoring are welcomed as a Pull Request on Github.


License
-------

Libresonic is free software and licensed under the [GNU General Public License version 3](http://www.gnu.org/copyleft/gpl.html). The code in this repository(and associated binaries) are free of any "license key" or other restrictions. If you wish to thank the maintainer of this repository, please consider a donation to the [Electronic Frontier Foundation](https://supporters.eff.org/donate).

The [Subsonic source code](https://github.com/Libresonic/subsonic-svn) was released under the GPLv3 through version 6.0-beta1. Beginning with 6.0-beta2, source is no longer provided. Binaries of Subsonic are only available under a commercial license. There is a [Subsonic Premium](http://www.subsonic.org/pages/premium.jsp) service which adds functionality not available in Libresonic. Subsonic also offers RPM, Deb, Exe, and other pre-built packages that Libresonic [currently does not](https://github.com/Libresonic/libresonic/issues/65).


Usage
-----

Libresonic can be downloaded from [Github](https://github.com/Libresonic/libresonic/releases) for personal usage. Packagers can reference the [release repository](https://libresonic.org/release/), but please contact the maintainer or wait until a [stable release policy](https://github.com/Libresonic/libresonic/issues/73) is available.

Libresonic is packaged in [WAR format](https://en.wikipedia.org/wiki/WAR_(file_format)), suitable for deployment as a [webapp under Tomcat](https://tomcat.apache.org/tomcat-6.0-doc/deployer-howto.html). Comprehensive install documentation [is pending](https://github.com/Libresonic/libresonic/issues/64).

Please see the [INSTALL document](https://github.com/Libresonic/libresonic/blob/develop/INSTALL.md) for instructions on building from source.
