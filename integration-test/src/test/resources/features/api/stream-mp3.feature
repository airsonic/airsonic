Feature: Stream API for MP3

  Background:
    Given Media file stream/piano/piano.mp3 is added
    And a scan is done

  Scenario: Airsonic sends stream data
    When A stream request is sent
    Then The response bytes are equal
    # TODO check length

