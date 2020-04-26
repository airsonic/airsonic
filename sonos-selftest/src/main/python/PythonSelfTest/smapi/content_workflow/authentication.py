import selftestinit #ignore @UnusedImport it is for packaging
import sys
from sonos.workflow.basicsuite import BasicWorkflowTestSuite
from sonos.workflow.fixture import WorkflowTestFixture
from sonos.smapi.smapiexceptions import LoginInvalid, LoginUnsupported, UnsupportedTerritory, LoginUnauthorized, AuthTokenExpired, SonosError
from sonos.smapi.smapiservice import SMAPIService, USERID, DEVICELINK
from sonos.smapi.smapiclient import SMAPIClient
from utility import ServiceConfigOptionParser

class Authentication(WorkflowTestFixture, SMAPIClient, SMAPIService):
    """
    This class holds the tests that will check that the authentication methods of the partner's SMAPI service meet the
    SMAPI requirements.
    """

    def __init__(self, client, smapiservice):
        super(Authentication,self).__init__()
        self.client = client
        self.smapiservice = smapiservice

        self.supported_account = self.smapiservice.get_supported_account()
        self.unsupported_account = self.smapiservice.get_unsupported_account()
        self.expired_trial_account = self.smapiservice.get_expired_trial_account()
        self.unsupported_territory_account = self.smapiservice.get_unsupported_territory_account()
        self.migrate_to_devicelink_auth = self.smapiservice.get_migration_status()
        self.run_refresh_auth_token_test = self.smapiservice.get_test_refresh_auth_token()
        self.auth = self.smapiservice.get_authType()

    def initialize(self):
        pass

    def test_supported_account(self):
        """
        This test case checks that a user is able to log in to the service using a supported account.
        """
        self.verifyTrueOrSkip(self.auth == USERID, "Service must use session ID authentication ([Authentication Type]:"
                                                   "sessionID in the config file) for this test to run.")

        response = self.client.getSessionId(self.supported_account.username, self.supported_account.password, self)
        self.verifyIsNotNoneOrFailCase(response, "getSessionId should return something other than None.")

    def test_invalid_credentials(self):
        """
        This test case checks that a LoginInvalid or SonosError error is raised if a user attempts to log in with
        invalid credentials.
        """
        self.verifyTrueOrSkip(self.auth == USERID, "Service must use session ID authentication ([Authentication Type]:"
                                                   "sessionID in the config file) for this test to run.")
        with self.verifyRaisesOrFailCase(LoginInvalid or SonosError, "getSessionId with an incorrect username/password "
                                                                     "should return Client.LoginInvalid."):
            self.client.getSessionId('foo', 'bar', self)

    def test_unsupported_account(self):
        """
        This test case checks that a LoginUnsupported or SonosError error is raised if a user attempts to log in with the
        wrong tier of account. (e.g. free vs paid)
        """
        self.verifyTrueOrSkip(self.auth == USERID, "Service must use session ID authentication ([Authentication Type]:"
                                                   "sessionID in the config file) for this test to run.")
        self.verifyTrueOrSkip(self.unsupported_account.username, "Service must provide an unsupported account "
                                                                 "([Accounts]:unsupportedAccount/unsupportedPassword "
                                                                 "in the config file) for this test to run.")

        with self.verifyRaisesOrFailCase(LoginUnsupported, "getSessionId with an unsupported account should return "
                                                           "Client.LoginUnsupported."):
            self.client.getSessionId(self.unsupported_account.username, self.unsupported_account.password, self)

    def test_expired_trial_account(self):
        """
        This test case checks that a LoginUnauthorized error is raised if a user attempts to log in with an expired
        account.
        """
        self.verifyTrueOrSkip(self.auth == USERID, "Service must use session ID authentication ([Authentication Type]:"
                                                   "sessionID in the config file) for this test to run.")
        self.verifyTrueOrSkip(self.expired_trial_account.username, "Service must provide an expired trial account "
                                                                   "([Accounts]:expiredTrialAccount/expiredTrial"
                                                                   "Password in the config file) for this test to run.")

        with self.verifyRaisesOrFailCase(LoginUnauthorized, "getSessionId with an expired trial account should return"
                                                            "Client.LoginUnauthorized."):
            self.client.getSessionId(self.expired_trial_account.username, self.expired_trial_account.password, self)

    def test_unsupported_territory_account(self):
        """
        This test case checks that an UnsupportedTerritory error is raised if a user attempts to log in with an account
        from an unsupported region.
        """
        self.verifyTrueOrSkip(self.auth == USERID, "Service must use session ID authentication ([Authentication Type]:"
                                                   "sessionID in the config file) for this test to run.")
        self.verifyTrueOrSkip(self.unsupported_territory_account.username, "Service must provide an unsupported "
                                                                           "territory account ([Accounts]:"
                                                                           "unsupportedTerritoryAccount/"
                                                                           "unsupportedTerritoryPassword in the config "
                                                                           "file) for this test to run.")

        with self.verifyRaisesOrFailCase(UnsupportedTerritory,
                                         "getSessionId with an account that isn't supported in the current region "
                                         "should return Client.UnsupportedTerritory."):
            self.client.getSessionId(self.unsupported_territory_account.username,
                                     self.unsupported_territory_account.password, self)

    def test_migration_to_deviceLink(self):
        """
        This test case checks that an AuthTokenExpired error is raised if a user attempts to log in after performing
        a deviceLink migration.
        """
        self.verifyTrueOrSkip(self.auth == DEVICELINK, "Service must use devicelink authentication ([Authentication "
                                                       "Type]:oauth in the config file) for this test to run.")
        self.verifyTrueOrSkip(self.migrate_to_devicelink_auth, "Service must set migrationToDeviceLink ([Device Link]:"
                                                               "migrationToDeviceLink in the config file) for this test"
                                                               " to run.")
        #if the two previous steps pass, then the service must supply a "supported account" we can
        # send with getSessionId or we fail the case
        self.verifyTrueOrFailCase(self.supported_account.username, "Service must provide a supported account"
                                                                   " ([Accounts]:supportedAccount/supportedPassword in "
                                                                   "the config file) for this test to run.")

        with self.verifyRaisesOrFailCase(AuthTokenExpired,
                                         "getSessionId with a supported account on a partner which has migrated to "
                                         "DeviceLink authentication should return Client.AuthTokenExpired."):
            self.client.getSessionId(self.supported_account.username, self.supported_account.password, self)

    def test_refreshAuthToken(self):
        """
        You must implement refreshAuthToken when using getExtendedMetadata relatedActions. This SOAP call is used when
        a SimpleHttpAction returns 401 Unauthorized.
        """
        self.verifyTrueOrSkip(self.auth == DEVICELINK, "Service must use devicelink authentication ([Authentication "
                                                       "Type]:oauth in the config file) for this test to run.")
        self.verifyTrueOrSkip(self.run_refresh_auth_token_test, "Service does not implement refreshAuthToken. Skipping test.")

        login_status = self.client.login()
        self.verifyIsNotNoneOrFailCase(login_status, "refreshAuthToken: failed to login.")

        return_value = self.client.refreshAuthToken(self)
        self.verifyIsNotNoneOrFailCase(return_value, "refreshAuthToken should return something other than None.")

#Main
if __name__ == "__main__":

    parser = ServiceConfigOptionParser(sys.argv)
    suite = BasicWorkflowTestSuite("Authentication Validation", args=parser.args)

    smapiservice = SMAPIService(parser.config_file, content_file=getattr(parser.options, 'content_file'),
                                logger=suite.logger, console_logger=suite.console_logger)
    client = smapiservice.buildservice()

    suite.run(Authentication(client, smapiservice))