Feature: Stream API for MP3

  Background:
    Given Media file piano is added
    And a scan is done
    And The media file id is found

  Scenario: Airsonic sends stream data
    When A stream is consumed
    Then Print debug output
    Then The response bytes are equal
    Then The length headers are correct

