import selftestinit  #ignore @UnusedImport for packaging
import sys
from sonos.workflow.basicsuite import BasicWorkflowTestSuite
from sonos.smapi.smapiservice import SMAPIService, TRACK, STREAM, PROGRAM
from utility import Validation
from utility import ServiceConfigOptionParser

class GetUserInfoTest(Validation):
    """
    This class holds tests for getUserInfo. It expects getUserInfo to return a hash of a user id. It will check
    that the userInfo does not contain "@" and that it has a minimum length of 32 characters.
    This test only runs if a music services enabled "supports user info" capability flag
    """
    def __init__(self, client, smapiservice):
        super(Validation,self).__init__()
        self.client = client
        self.smapiservice = smapiservice
        self.support_userInfo = self.smapiservice.get_supports_userInfo()

    def test_getUserInfo(self):
        self.verifyTrueOrSkip(self.support_userInfo, "Service must support getUserInfo for this test to run.")
        response = self.getUserInfoRequest()
        if hasattr(response, "userIdHashCode"):
            self.verifyTrueOrFailCase(len(response.userIdHashCode) > 0, "getUserInfoResponse did not return a 'userIdHashCode' value")
            self.verifyTrueOrFailCase(("@" not in response.userIdHashCode and "." not in response.userIdHashCode), "userIdHasCode does not seem be hashed!")

    def getUserInfoRequest(self):
        response = self.client.getUserInfo(self)
        self.verifyIsNotNoneOrStop(response, "getUserInfo request failed")
        return response

#Main
if __name__ == "__main__":

    parser = ServiceConfigOptionParser(sys.argv)

    suite = BasicWorkflowTestSuite("getUserInfo Validation", args=parser.args)

    suite.smapiservice = SMAPIService(parser.config_file, parser.wsdl, getattr(parser.options, 'content_file'),
                                      suite.logger, suite.console_logger)
    suite.client = suite.smapiservice.buildservice()
    suite.client.login()

    f = GetUserInfoTest(suite.client, suite.smapiservice)

    suite.run(f)
