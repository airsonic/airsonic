import selftestinit  # ignore @UnusedImport for packaging
import sys
import time
from sonos.workflow.basicsuite import BasicWorkflowTestSuite
from sonos.workflow.fixture import WorkflowTestFixture
from sonos.smapi.smapiservice import SMAPIService
from sonos.smapi.smapiclient import SMAPIClient
from utility import ServiceConfigOptionParser
from sonos.smapi.smapiexceptions import NotLinkedRetry, NotLinkedFailure


class DeviceLink(WorkflowTestFixture, SMAPIClient, SMAPIService):
    """
    This class will test getDeviceLinkCode() and getDeviceAuthToken() calls.
    getDeviceLinkCode() call and parameters from config file will generate
    registration code. This code will be displayed and you will be asked to
    go to the website and register your device using the code. The test will
    wait for you to successfully finished registration and code execution
    will continue and you will be presented with a token and a key.
    """
    def initialize(self):
        pass

    def __init__(self, client, smapiservice):
        super(DeviceLink, self).__init__()
        self.client = client
        self.smapiservice = smapiservice
        self.polling_interval_seconds = 5  # waiting time before sending another request
        self.max_polling_secs = 480  # 8 minutes = maximum duration of polling until controller quits
        self.support_userInfo = self.smapiservice.get_supports_userInfo()

    def test_manual_device_link(self):

        login_cred = self.client.login()

        try:
            link_code_resp = self.client.getDeviceLinkCode(login_cred.loginToken.householdId, self)
        except Exception, e:
            self.stop('Could not retrieve link code Exception {0}'.format(e))

        # there are conditions when code is missing, while other components are present
        self.verifyIsNotNoneOrStop(link_code_resp.linkCode, "getDeviceLinkCode should return a linkCode.")
        self.verifyIsNotNoneOrStop(link_code_resp.regUrl, "getDeviceLinkCode should return a regUrl.")

        self.log_message('[MANUAL STEP] Please go to ' + link_code_resp.regUrl + ' and register your device')
        self.log_message('Your registration code is: ' + link_code_resp.linkCode)

        self.log_message(
            'Begin polling every {0} seconds for an authentication token'.format(self.polling_interval_seconds))

        start_time = time.time()  # Log the start time
        while time.time() < start_time + self.max_polling_secs:
            try:
                result = self.client.getDeviceAuthToken(link_code_resp, self)

                if not hasattr(result, 'fault'):
                    # Validate the result
                    self.verifyIsNotNoneOrFailCase(result, "getdeviceAuthToken should return something other than "
                                                           "None.")
                    self.verifyTrueOrStop(hasattr(result,'authToken'), "deviceAuthTokenResults should return an authToken.")
                    self.verifyIsNotNoneOrFailCase(result.authToken, "deviceAuthTokenResults should return an authToken.")
                    self.verifyTrueOrStop(hasattr(result,'privateKey'), "deviceAuthTokenResults should return a privateKey.")
                    self.verifyIsNotNoneOrFailCase(result.privateKey, "deviceAuthTokenResults should return a privateKey.")

                    # if service supports user info, user info should be returned in the response of getDeviceAuthToken
                    if self.support_userInfo:
                        self.verifyTrueOrStop(hasattr(result, "userInfo"), "deviceAuthTokenResult should include userInfo.")
                        self.verifyTrueOrStop(hasattr(result.userInfo, "userIdHashCode"), "deviceAuthTokenResult userInfo should include userIdHashCode.")


                # Display Results
                    self.log_message('Your authToken: ' + result.authToken)
                    self.log_message('Your privateKey: ' + result.privateKey)

                    break
                else:
                    self.warn(result.fault.message)
            except NotLinkedRetry as retry_exception:
                self.log_message(retry_exception, 'WARN')
                time.sleep(self.polling_interval_seconds)
                continue
            except NotLinkedFailure as failure_exception:
                self.stop('Encountered NOT_LINK_FAILURE {0}'.format(failure_exception))
                break
            except Exception as e:
                self.stop('Found unknown exception: {0}'.format(e))
                break
        else:
            self.fail('Polling timeout, have not received a response within 8 minutes')

    # TODO: Once the story to consolidate the loggers on the parent class into one call is complete this can be removed
    def log_message(self, message, level='INFO'):
        if level == 'ERROR':
            self.console_logger.error(message)
            self.logger.error(message)
        elif level == 'WARN':
            self.console_logger.warn(message)
            self.logger.warn(message)
        else:
            self.console_logger.info(message)
            self.logger.info(message)

# Main
if __name__ == "__main__":
    parser = ServiceConfigOptionParser(sys.argv)
    suite = BasicWorkflowTestSuite("Device Link Test", args=parser.args)

    smapi_service = SMAPIService(parser.config_file, content_file=getattr(parser.options, 'content_file'), logger=suite.logger, console_logger=suite.console_logger)
    client = smapi_service.buildservice()

    suite.run(DeviceLink(client, smapi_service))