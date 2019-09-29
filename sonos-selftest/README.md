Airsonic Sonos integration Self Test 
===

A set of tests provided by Sonos to validate the implementation of the Sonos Music API (SMAPI). 

This set of tests is written in Python and can be downloaded at the following address: 

    https://musicpartners.sonos.com/sites/default/files/PythonSelfTest.tar.gz

See the documentation at the following address: 

    https://musicpartners.sonos.com/?q=node/134

Integration into the Airsonic environment 
===

The sound structure (music, db, ect...) is in the directory : 

    main/resources/airsonic.

The source of the tests downloaded in.tar.gz of version 1.2.0.697, is in main/resources and is decompressed in main/python/PythonSelfTest

Before starting the tests, Python2 must be installed on the machine and the test dependencies must also be installed, see : 
    main/python/Python/PythonSelfTest/README

Structure of test data 
===
The description of the tests for sonos is in the file : 

    main/python/PythonSelfTest/smapi/service_configs/airsonic.cfg

In the database there are two user/password:  

    admin/admin and test/test. the "test" user is used for tests.

There is a record of the data required for the link to Sonos.

The BD contains some albums in flac format. 

The formats supported by Sonos can be found here:

    https://developer.sonos.com/build/content-service-add-features/supported-audio-formats/

Execution of tests 
===

1) Copy sonos-selftest/src/main/resources/airsonic to /var/airsonic or make a symbolic link 

    ```shell script
    sudo ln -s /home/me/me/workspace/airsonic/sonos-selftest/src/main/resources/airsonic /var/airsonic    
    ```

2) Start airsonic under tomcat (or other) deployed at root "/", with address 4040. Otherwise you can change in airsonic.cfg. Use the following parameters:

    ```Properties
    -Dairsonic.home=/var/airsonic/data
    -Dairsonic.defaultMusicFolder=/var/airsonic/music
    -Dairsonic.defaultPodcastFolder=/var/airsonic/podcasts
    -Dairsonic.defaultPlaylistFolder=/var/airsonic/playlists
    -Djava.awt.headless=true
    ```

3) run the python tests in a terminal in the directory: sonos-selftest/src/main/python/PythonSelfTest/smapi/content_workflow, as follows:

    ```shell script
    python2 suite_selftest.py --config airsonic.cfg
    ```
    
4) The result is in: 

    sonos-selftest/src/main/python/PythonSelfTest/smapi/content_workflow/log_files/airsonic

Result in error 
===

For the moment there is a certain standard of failure due to a:

1) "ssl_validation test_use_secure_endpoint" test

    Could be fixed by setting up https on the test deployment

2) Warning on the "Search test_..."

    Not investigated

3) "HTTPBasedTests test_byte_range_seeking" test

    These tests are based on Stream reading, see below "authentication".

Authentication 
===

When sonos communicates with airsonic, authentication is done through the token. The latter contains the user name.
For the moment this token does not expire, but we should put in place its renewal. 

For playback of a piece of music, Sonos requests the playback link of the song via the getMediaUrl request. This link is a link that is standard for airsonic APIs and contains a JWTToken. The latter does not contain the user's name and Sonos is not authenticated to Airsonic, it works with the token sonos normally to authenticate itself. 

Then on the Get "ext/stream", airsonic authenticates the request as anonymous. In addition to the fact that we are not authenticated on the user who requests the request via Sonos, airsonic still allows the requested music to be played.

So to complete the integration correctly with Sonos we should:

1) Allow authentication when calling the airsonic API

2) Restricted access to music only to the authenticated user.
