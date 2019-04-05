<!--
# CHANGELOG.md
# airsonic/airsonic
# -->

## v10.2.1 - 18 Feb 2019

Security:
  * CVE-2018-20222 Prevent xxe during parse

## v10.2.0 - 16 Feb 2019

Fixes:
  * Fix #658 again: content type for unscaled images set based on jaudiotagger output
  * Reverted a93a18a and properly re-encoded with 'native2ascii -encoding UTF-8'
  * fix issues #638 and #574
  * Fix #596 SubStandard theme includes old Subsonic Logo
  * Fix themes using dark background with me\_js
  * Remove potential cast exception
  * Fixed github link opening in frame and not loading
  * Correct corrupted downloaded zip
  * Fix #778: Defragment embedded HSQLDB database more frequently
  * PlayQueue: Fix broken keyboard shortcuts
  * Fix #860 (external database performance) by using connection pooling … (#864)


Changes:
  * Made it easier to see current playing song for dark themes
  * Spring Boot 1.5.18
  * New add\_album to play queue
  * Remove margin of media\_control bar
  * Update to 3.3.0 java-jwt
  * catch exceptions ClientAbortException display a short message and return, to avoid the massive useless traceback in log
  * Update cxf to 3.1.15
  * Issue #164: Show link to MusicBrainz release on album pages
  * Handle player id as an Integer instead of Strin
  * Add Docker health check
  * Use dark media player theme on groove theme (#777)
  * Change to optional reCAPTCHA v2
  * Optionally parse podcast episode duration in seconds to [hh:]mm:ss
  * Add option to disable seeking on transcodes. (Mitigates #548 & #723)
  * White list jars that are scanned for tlds to prevent spurious logs
  * Tweaked logging around servlet container and added warning about jetty
  * Add extended favicons
  * Display folders as a list in Settings-\>Users and include the path.
  * Add 32x32 pixeled favicon / updated favicons in webapp
  * Updated internal maven plugins


Translation Updates:
  * Fixed elipse in english translation

Security:
  * Fix #749 Ensure transcode settings are protected
  * Bump version of guava to deal with CVE-2018-10237
  * Update jackson version
  * Fix #764 LDAP Login allows unauthenticated access

Not Fixed:
  * #685 - transcoding length issue

## v10.1.2 - 28 Aug 2018

Fixes:
  * Fix LDAP authentication bypass

## v10.1.1 - 16 Dec 2017

Changes:
  * Add show-all button on artist landing page
  * Upgrade jaudiotagger to 2.2.5 supporting Java 9

Fixes:
  * DLNA Recent Albums is just listing albums
  * NPE in docker container
  * Substandard theme css
  * Build error causing Jetty to be default container (should be Tomcat)

Translation Updates:
  * English language cleanup

## v10.1.0 - 04 Nov 2017

Changes:
  * New Jukebox player using javasound api
  * Localize artist bios from last.fm
  * Use `ffprobe` and not `ffmpeg` to scrape metadata
  * Added options for excluding files during scan (symlinks and regex)
  * Add "opus" and "mka" extension to default extension list

Fixes:
  * Error message readability
  * Adding album comment
  * Subsonic API wrong error behavior for getAlbumList
  * Stop airsonic from creating double slashes in urls.
  * Search csrf timeout/expiration
  
Security:
  * CVE-2014-3004 - XML playlist parsing

Translation Updates:
  * English

## v10.0.1 - 23 Aug 2017

Note that with this release, the jdbc-extra flavored war is now the default and only war.

  * Translation updates for French, Bulgarian, German, Italian, Spanish,
  * Docker image tweaks
  * Some light cleanup/refactorings
  * Fixed password reset
  * Fixed broken liquibase when airsonic.defaultMusicFolder is modified

## v10.0.0 - 06 Aug 2017

  * Rebranded to Airsonic
  * Replaced JWplayer with MediaElement.js (HTML5 player)
  * Upgraded to Subsonic API version 1.15
  * Added official Docker image
  * Added Airsonic to a Translation service (Weblate)
  * Some translations updates (English, French, German, and Russian)
  * New login page
  * Added additional war with builtin support for external databases
  * Improved playlist handling
  * DLNA browsing improvements
  * Small fixes and improvements

## v6.2 - 02 May 2017

  * Small fixes
  * Release only a month behind schedule! We're improving!

## v6.2.beta4 - 25 Apr 2017

  * Final fixes in Beta! Release soon

## v6.2.beta3 - 08 Apr 2017

  * API endpoint security tightening
  * More documentation
  * Less licensing code
  * More cowbell

## v6.2.beta2 - 24 Mar 2017

  * Add database settings UI
  * Documentation improvements
  * Lots of spit and polish

## v6.2.beta1 - 05 Mar 2017

  * Add external database support
  * Upgrade to new version of Spring
  * Replace subsonic-booter with Spring Boot
  * Remove remote-access service and port-forwarding
  * Remove vestigial Subsonic licensing calls
  * Add a demo site
  * Tests and bugfixes and documentation, oh my!

## v6.1 - 27 Nov 2016

  * First real stable release!

## v6.1.beta2 - 19 Nov 2016

  * Metaproject: Jenkins builds!
  * More documentation
  * Translation updates
  * Improve shuffling behaviour
  * Lots of small fixes, many more to come

## v6.1.beta1 - 15 May 2016

  * Meant as a release candidate; failed to make it past the Primary election.

## v6.1-alpha1 - 14 May 2016

  * Search+replace subsonic-->libresonic
  * Move out of org.sourceforge.subsonic namespace
  * Develop becomes horribly unstable, you shouldn't be using this.

## v6.0.1 - 14 May 2016

  * First recommended release
  * Updated Help/About page text

## v6.0 - 1 May 2016

  * First release as Libresonic
  * Based upon Subsonic 5.3(stable)
