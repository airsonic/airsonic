import selftestinit #ignore @UnusedImport it is for packaging
import sys
import os
import glob
import ConfigParser
from sonos.workflow.basicsuite import BasicWorkflowTestSuite
from sonos.workflow.fixture import WorkflowTestFixture
from sonos.smapi.smapiservice import SMAPIService, USERID
from sonos.smapi.smapiclient import SMAPIClient

class Credential(WorkflowTestFixture, SMAPIClient, SMAPIService):
    """
    This test verifies that all the provided credentials function as expected.
    """
    def __init__(self, client, smapiservice):
        super(Credential,self).__init__()

        self.client = client
        self.smapiservice = smapiservice
        self.supported_account = self.smapiservice.get_supported_account()

    def initialize(self):
        pass

    def setUpFixture (self):
        # override the default _testFixtureName which is __class__.__name__ so that we could differentiate tests for each service partner
        self._testFixtureName = '{0} {1}'.format(self.smapiservice.svcName, self.__class__.__name__)
        super(Credential,self).setUpFixture()

    def test_credential_valid(self):
        """
        This test verified that the credential can be used to login and get a valid SessionID
        """
        self.client.login()
        response = self.client.getSessionId(self.supported_account.username,self.supported_account.password, self)
        self.verifyIsNotNoneOrFailCase(response,"getSessionId should return something other than None.")

    def test_credential_authorized(self):
        """
        This test verifies that the credential can be used to play test track. Sometimes a premium account is required
        to play track.
        """
        self.client.login()
        self.verifyTrueOrSkip(self.smapiservice.get_test_track_id(), "Service must provide a valid track ID and/or "
                                                                     "stream ID ([Test Content]:track/stream in the "
                                                                     "config file) for this test to run.")

        media_uri = self.client.getMediaURI(self.smapiservice.get_test_track_id(), self)
        self.verifyIsNotNoneOrStop(media_uri, "getMediaURI should return something other than None.")
        self.verifyInOrStop('getMediaURIResult', media_uri, "getMediaUri should return a getMediaURIResult.")

#Main
if __name__ == "__main__":
    path_to_configs = os.path.abspath(os.path.join(os.path.dirname( __file__ ), '..','service_configs'))
    # find all .cfg files
    partner_cfgs = glob.glob1(path_to_configs, '*.cfg')

    fixture = []
    for cfg_file in partner_cfgs:
        config_file = os.path.normpath(os.path.join(path_to_configs, cfg_file))
        config = ConfigParser.SafeConfigParser(allow_no_value=True)
        config.read(config_file)

        service_name = config.get('Service Name', 'serviceName')
        if service_name:
            smapiservice = SMAPIService(config_file)
            # only do validation for service that uses SessionId authentication
            if smapiservice.get_authType() == USERID:
                client = smapiservice.buildservice()
                fixture.append(Credential(client, smapiservice))

    suite = BasicWorkflowTestSuite("Validate Credentials", args=None)
    suite.run(fixture)
