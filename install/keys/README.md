This directory contains GPG signing keys used to sign and verify Airsonic
releases.

To sign a new release after a successful build:

  $ gpg --clearsign airsonic-main/target/artifacts-checksums.sha

To verify a release:

  $ gpg --verify artifacts-checksums.sha.asc
  gpg: Signature made Fri 01 May 2020 12:45:09 PM CEST
  gpg:                using RSA key 262121A22A609283A6A46BCB7DEEDDBFC5A13AB4
  gpg: Good signature from "François-Xavier Thomas (Git signing key) <fxthomas@users.noreply.github.com>" [ultimate]

To sign a key for adding a new maintainer (replace key id with the one you want
to sign):

  $ gpg --sign-key 262121A22A609283A6A46BCB7DEEDDBFC5A13AB4
  $ gpg --send-keys 262121A22A609283A6A46BCB7DEEDDBFC5A13AB4
  $ gpg --keyserver pgp.mit.edu --send-keys 262121A22A609283A6A46BCB7DEEDDBFC5A13AB4
  $ gpg --keyserver keyserver.ubuntu.com --send-keys 262121A22A609283A6A46BCB7DEEDDBFC5A13AB4

To export a key in the repo:

  $ gpg --export --armor 262121A22A609283A6A46BCB7DEEDDBFC5A13AB4 > install/keys/<email>.pem
