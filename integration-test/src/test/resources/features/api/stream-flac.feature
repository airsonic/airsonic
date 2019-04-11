Feature: Stream API for FLAC

  Background:
    Given Media file dead is added
    And a scan is done
    And The media file id is found

  Scenario: Airsonic sends stream data
    When A stream is consumed
    Then Print debug output
    Then The response bytes are equal
    Then The length headers are absent

