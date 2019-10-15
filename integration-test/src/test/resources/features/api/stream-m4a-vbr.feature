Feature: Stream API for VBR M4A

  Background:
    Given Media file dance is added
    And a scan is done
    And The media file id is found

  Scenario: Airsonic sends stream data
    When A stream is consumed
    Then Print debug output
    Then The response bytes are equal
    Then The length headers are absent

