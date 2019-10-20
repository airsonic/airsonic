import selftestinit  # ignore @UnusedImport for packaging
import sys
import time
from sonos.workflow.basicsuite import BasicWorkflowTestSuite
from sonos.workflow.fixture import WorkflowTestFixture
from sonos.smapi.smapiservice import SMAPIService, ROOT, USERID
from sonos.smapi.smapiclient import SMAPIClient, CR_STANDARD_RANGE
from utility import ServiceConfigOptionParser


class PollingIntervalTest(WorkflowTestFixture, SMAPIClient, SMAPIService):
    """
    This class holds the tests that will check that getlastupdate and the associated versioning variables are
    implemented correctly and conform to the SMAPI requirements.
    """
    def __init__(self, client, smapiservice):
        super(PollingIntervalTest, self).__init__()
        self.client = client
        self.smapiservice = smapiservice
        self.poll_interval = self.smapiservice.get_polling_interval()
        self.autoRefreshEnabled = True

    def initialize(self):
        pass

    def test_getlastupdate_no_change(self):
        """
        This test exercises the getLastUpdate SMAPI call without making any changes to the favorites of a service. This
        is specifically to test and make sure that the favorites and catalog values returned by the service are not
        incrementing when nothing has changed. We import the poll interval from the config file and sleep (block) for
        that duration in between getLastUpdate calls (so long as it's shorter than 5 minutes). This test must be
        executed last in any suite that it is applied to.
        """
        first_get_last_update_values = self.getlastupdate_unpacker()
        second_get_last_update_values = self.getlastupdate_unpacker()

        if self.autoRefreshEnabled:
            self.verifyListEqualOrFailCase(first_get_last_update_values, second_get_last_update_values,
                                           "getLastUpdate checksum values should remain unchanged when no changes have been"
                                           " made to the favorites folder between two consecutive (rapid-fire) "
                                           "getlastupdate calls.")

            self.verifyTrueOrSkip(self.poll_interval, 'Service must provide valid polling interval for this test to run.')
            self.verifyTrueOrSkip(self.poll_interval <= 300, "Polling interval ([Polling Interval]:interval must be 5 "
                                                             "minutes (300 seconds) or shorter to run this test.")

            time.sleep(self.poll_interval)
            third_get_last_update_values = self.getlastupdate_unpacker()

            self.verifyListEqualOrFailCase(second_get_last_update_values, third_get_last_update_values,
                                           "getLastUpdate checksum values should remain unchanged when no changes have been"
                                           " made to the favorites folder during a polling interval.")

    def getlastupdate_unpacker(self):
        response = self.client.getLastUpdate(self)
        self.verifyIsNotNoneOrStop(response, "getLastUpdate should return something other than None.")
        list_of_checksums = []
        for checksum in response:
            if checksum[0] not in ['pollInterval', 'timer']:
                list_of_checksums.append(checksum[1])

        if hasattr(response, "pollInterval"):
            self.poll_interval = response.pollInterval

        if hasattr(response, "autoRefreshEnabled"):
            self.autoRefreshEnabled = response.autoRefreshEnabled

        return list_of_checksums

#Main
if __name__ == "__main__":
    parser = ServiceConfigOptionParser(sys.argv)

    suite = BasicWorkflowTestSuite("getLastUpdate Validation", args=parser.args)

    suite.smapiservice = SMAPIService(parser.config_file, parser.wsdl, getattr(parser.options, 'content_file'),
                                      suite.logger, suite.console_logger)
    suite.client = suite.smapiservice.buildservice()
    suite.client.login()

    suite.run(PollingIntervalTest(suite.client, suite.smapiservice))