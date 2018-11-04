<!--
# CHANGELOG.md
# airsonic/airsonic
# -->

## v10.1.2

Fixes:
  * Fix LDAP authentication bypass

## v10.1.1

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

## v10.1.0

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

## v10.0.1

Note that with this release, the jdbc-extra flavored war is now the default and only war.

  * Translation updates for French, Bulgarian, German, Italian, Spanish,
  * Docker image tweaks
  * Some light cleanup/refactorings
  * Fixed password reset
  * Fixed broken liquibase when airsonic.defaultMusicFolder is modified

## v10.0.0

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

## v6.2

  * Small fixes
  * Release only a month behind schedule! We're improving!

## v6.2.beta4

  * Final fixes in Beta! Release soon

## v6.2.beta3

  * API endpoint security tightening
  * More documentation
  * Less licensing code
  * More cowbell

## v6.2.beta2

  * Add database settings UI
  * Documentation improvements
  * Lots of spit and polish

## v6.2.beta1

  * Add external database support
  * Upgrade to new version of Spring
  * Replace subsonic-booter with Spring Boot
  * Remove remote-access service and port-forwarding
  * Remove vestigial Subsonic licensing calls
  * Add a demo site
  * Tests and bugfixes and documentation, oh my!

## v6.1

  * First real stable release!

## v6.1.beta2

  * Metaproject: Jenkins builds!
  * More documentation
  * Translation updates
  * Improve shuffling behaviour
  * Lots of small fixes, many more to come

## v6.1.beta1

  * Meant as a release candidate; failed to make it past the Primary election.

## v6.1-alpha1

  * Search+replace subsonic-->libresonic
  * Move out of org.sourceforge.subsonic namespace
  * Develop becomes horribly unstable, you shouldn't be using this.

## v6.0.1

  * First recommended release
  * Updated Help/About page text

## v6.0

  * First release as Libresonic
  * Based upon Subsonic 5.3(stable)
