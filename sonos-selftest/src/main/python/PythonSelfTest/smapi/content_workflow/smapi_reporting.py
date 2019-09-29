import selftestinit  # ignore @UnusedImport for packaging
import sys
from sonos.workflow.basicsuite import BasicWorkflowTestSuite
from sonos.workflow.fixture import WorkflowTestFixture
from sonos.smapi.smapiservice import SMAPIService, MESSAGESTRINGID
from sonos.smapi.smapiclient import SMAPIClient
from sonos.smapi.xmlparser import XMLParser
from utility import ServiceConfigOptionParser


class SMAPIReporting(WorkflowTestFixture, SMAPIClient, SMAPIService):
    """
    This class holds the tests that will check that SMAPI reporting functionality is implemented correctly.
    """
    def __init__(self, client, smapiservice):
        super(SMAPIReporting, self).__init__()
        self.client = client
        self.smapiservice = smapiservice
        self.test_track_id = self.smapiservice.get_test_track_id()
        self.support_event_and_duration_logging_during_playback = \
            self.smapiservice.get_support_event_and_duration_logging_during_playback()
        self.support_account_logging = self.smapiservice.get_support_account_logging()
        self.account_actions = ['addAccount']

    def initialize(self):
        pass

    def test_report_play_seconds(self):
        """
        this is a test that verifies that reportplayseconds functions properly
        """
        self.verifyTrueOrSkip(self.support_event_and_duration_logging_during_playback,
                              "Service must support playback duration and event logging during track play "
                              "([Capabilities]:eventAndDurationLoggingDuringPlayback in the config file) for this test "
                              "to run.")
        if self.test_track_id:
            response = self.client.reportPlaySeconds(self.test_track_id, 0, self)
            self.verifyIsNotNoneOrStop(response, "reportPlaySeconds should return something other than None.")
            if hasattr(response, 'interval'):
                interval = response.interval
                self.verifyTrueOrFailCase((isinstance(interval, int) and interval >= 0), "reportPlaySeconds should "
                                                                                        "return a non-negative integer.")
            else:
                self.fail("reportPlaySeconds should return a non-negative integer.")
        else:
            self.skip("Service must provide a valid track ID ([Test Content]: track in the config file) for this test "
                      "to run.")

    def test_report_play_status(self):
        """
        this is a test that verifies that reportplaystatus functions properly
        """
        self.verifyTrueOrSkip(self.support_event_and_duration_logging_during_playback,
                              "Service must support playback duration and event logging during track play "
                              "([Capabilities]:eventAndDurationLoggingDuringPlayback in the config file) for this test "
                              "to run.")
        if self.test_track_id:
            response = self.client.reportPlayStatus(self.test_track_id, "skippedTrack", self)
            self.verifyIsNoneOrFailCase(response, "reportPlayStatus should return an empty 'reportPlayStatusResult "
                                                  "'node.")
        else:
            self.skip("Service must provide a valid track ID ([Test Content]: track in the config file) for this test "
                      "to run.")

    def test_report_account_action(self):
        """
        this is a test that verifies that reportaccountaction functions properly
        """
        self.verifyTrueOrSkip(self.support_account_logging, "Service must support account logging ([Capabilities]:"
                                                            "accountLogging in the config file) for this test to "
                                                            "run.")
        for action in self.account_actions:
            try:
                response = self.client.reportAccountAction(action, self)
                self.verifyIsNoneOrFailCase(response, "reportAccountAction('{0}') should return an empty "
                                                      "'reportAccountActionResult' node.".format(action))
            except Exception as e:
                self.fail("reportAccountAction returned an exception: {0}.".format(e))

if __name__ == "__main__":
    parser = ServiceConfigOptionParser(sys.argv)
    suite = BasicWorkflowTestSuite("SMAPI Reporting Validation", args=parser.args)

    suite.smapiservice = SMAPIService(parser.config_file, parser.wsdl, getattr(parser.options, 'content_file'),
                                      suite.logger, suite.console_logger)
    suite.client = suite.smapiservice.buildservice()
    suite.client.login()

    f = SMAPIReporting(suite.client, suite.smapiservice)
    f.initialize()

    suite.run(f)