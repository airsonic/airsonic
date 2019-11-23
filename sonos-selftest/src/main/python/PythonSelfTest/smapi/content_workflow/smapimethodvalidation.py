import selftestinit #ignore @UnusedImport for packaging
import sys
from sonos.workflow.basicsuite import BasicWorkflowTestSuite
from sonos.workflow.fixture import WorkflowTestFixture
from sonos.smapi.smapiservice import SMAPIService, USERID, ROOT, SEARCH
from sonos.smapi.smapiclient import SMAPIClient, EMPTYLIST
from utility import ServiceConfigOptionParser
from suds.sax.text import Text

class SMAPIMethodValidation(WorkflowTestFixture, SMAPIClient, SMAPIService):
    """
    These tests exercise each SMAPI method using valid inputs to verify that each response does not return None.
    All SMAPI methods, at the minimum, must be handled by the client and return either a valid response or a
    soap fault. A None response is not acceptable and will cause problems within the Sonos controller.
    """
    def __init__(self, client, smapiservice):
        super(SMAPIMethodValidation,self).__init__()
        self.client = client
        self.smapiservice = smapiservice
        self.request_index = client.actual_CR_STANDARD_RANGE.index
        self.request_count = client.actual_CR_STANDARD_RANGE.count
        self.played_seconds = 10

    def initialize(self):
        pass

    def test_getsessionid(self):
        """
        this is a test that verifies that getsessionid returns a response
        """
        auth = self.smapiservice.get_authType()
        self.verifyTrueOrSkip(auth == USERID, "Service must use session ID authentication ([Authentication Type]:"
                                              "sessionID in the config file) for this test to run.")

        tempCred = self.client.login()
        self.verifyIsNotNoneOrFailCase(tempCred.sessionId, "getSessionId should return something other than None.")

    def test_getmetadata(self):
        """
        this is a test that verifies that getmetadata functions properly
        """
        (self.serviceroot,warning) = self.client.getMetadata(ROOT, self.request_index, self.request_count,
                                                             self) #ignore @UndefinedVariable for named tuple
        if warning is EMPTYLIST:
            self.warn("getMetadata should return a non-empty list.")
        self.verifyIsNotNoneOrFailCase(self.serviceroot, "getMetadata(root) should return something other than None.")
        self.verifyTrueOrFailCase(self.serviceroot.count <= self.request_count ,
                                  "getMetadata should return a 'count' value smaller than or equal to the requested "
                                  "'count' value.")
        self.verifyTrueOrFailCase(self.serviceroot.index == self.request_index,
                                  "getMetadata should return an 'index' value equal to the requested 'index' value.")

    def test_getmediametadata(self):
        """
        this is a test that verifies that getmediametadata functions properly
        """
        track_id = self.smapiservice.get_test_track_id()
        stream_id = self.smapiservice.get_test_stream_id()
        self.verifyTrueOrSkip(track_id or stream_id, "Service must provide a valid track ID and/or stream ID ([Test "
                                                     "Content]:track/stream in the config file) for this test to run.")
        if track_id:
            get_media_metadata_response = self.client.getMediaMetadata(track_id, self)
            self.verifyIsNotNoneOrFailCase(get_media_metadata_response, "getMediaMetadata should return something "
                                                                        "other than None")
        if stream_id:
            get_media_metadata_response = self.client.getMediaMetadata(stream_id, self)
            self.verifyIsNotNoneOrFailCase(get_media_metadata_response, "getMediaMetadata should return something "
                                                                        "other than None")

    def test_rateitem(self):
        """
        this is a test that verifies that rateitem functions properly
        """
        self.verifyTrueOrSkip(self.smapiservice.get_supports_ratings(), "Service must support Ratings for this test"
                                                                        "to run.")
        track_id = self.smapiservice.get_test_track_id()
        self.verifyTrueOrSkip(track_id, "Service must provide a valid track ID ([Test Content]:track in the config "
                                        "file) for this test to run.")
        ratings = self.smapiservice.get_ratings_values()
        self.verifyTrueOrStop(ratings, "Service must provide rating values in the presentation map if Ratings are "
                                       "supported.")

        rate_item_response = self.client.rateItem(track_id, ratings[0], self)
        self.verifyIsNotNoneOrFailCase(rate_item_response, "rateItem should return something other than None.")

    def test_createitem(self):
        """
        this is a test that verifies that createitem functions properly
        """
        self.verifyTrueOrSkip(self.smapiservice.get_support_favorites(), "Service must support adding/removing "
                                                                        "favorites tracks and/or albums and/or artists"
                                                                        "([Capabilities]:favoritesTracks/"
                                                                        "favoritesAlbums/favoritesArtists in the "
                                                                        "config file) for this test to run.")
        track_id = self.smapiservice.get_test_track_id()
        self.verifyTrueOrSkip(track_id, "Service must provide a valid track ID ([Test Content]:track in the config "
                                        "file) for this test to run.")
        create_item_response = self.client.createItem(track_id, self)

        self.verifyTrueOrFailCase(create_item_response or (create_item_response == '')
                                  or (create_item_response == None), "createItem should return the id of the item "
                                                                     "created or nothing.")

    def test_deleteitem(self):
        """
        this is a test that verifies that deleteitem functions properly
        """
        self.verifyTrueOrSkip(self.smapiservice.get_support_favorites(), "Service must support adding/removing "
                                                                         "favorites tracks and/or albums and/or artists"
                                                                         "([Capabilities]:favoritesTracks/"
                                                                         "favoritesAlbums/favoritesArtists in the "
                                                                         "config file) for this test to run.")
        track_id =self.smapiservice.get_test_track_id()
        self.verifyTrueOrSkip(track_id, "Service must provide a valid track ID ([Test Content]:track in the config "
                                        "file) for this test to run.")
        delete_item_response = self.client.deleteItem(track_id, self)

        self.verifyIsNoneOrFailCase(delete_item_response, "deleteItem should return something other than None.")

    def test_setplayedseconds(self):
        """
        this is a test that verifies that setplayedseconds functions properly
        """
        track_id = self.smapiservice.get_test_track_id()
        self.verifyTrueOrSkip(track_id, "Service must provide a valid track ID ([Test Content]:track in the config "
                                        "file) for this test to run.")

        self.verifyTrueOrSkip(self.smapiservice.get_support_playbacklogging(), "Service must support playback logging "
                                                                               "([Capabilities]:playbackLogging in the"
                                                                               " config file) to run this test.")
        set_played_seconds_response = self.client.setPlayedSeconds(track_id, 30, self)
        self.verifyIsNoneOrFailCase(set_played_seconds_response, "setPlayedSeconds should return None.")

    def test_search(self):
        """
        this is a test that verifies that search functions properly
        """
        self.verifyTrueOrSkip(self.smapiservice.get_support_search(), "Service must support search ([Capabilities]:"
                                                                      "search in the config file) for this test to "
                                                                      "run.")

        (search_node,_) = self.client.getMetadata(SEARCH, self.request_index, self.request_count,
                                                  self) #ignore @UndefinedVariable for named tuple
        self.verifyIsNotNoneOrFailCase(search_node, "getMetadata should return something other than None.")

        #here we are literally just searching for 'rock' under whatever search container is first
        try:
            (search_result,_) = self.client.search(search_node.Items[0].id, "rock", self.request_index,
                                                   self.request_count,
                                                   self) #ignore @UndefinedVariable for named tuple
        except Exception, e:
            self.stop("search({0}) returned an exception: {1}.".format("rock", e))

        self.verifyIsNotNoneOrWarn(search_result, "search should return something other than None.")

    def test_getlastupdate(self):
        """
        this is a test that verifies that getlastupdate functions properly
        """
        get_last_update_response = self.client.getLastUpdate(self)
        self.verifyIsNotNoneOrFailCase(get_last_update_response, "getLastUpdate should return something other than "
                                                                 "None.")

#Main
if __name__ == "__main__":

    parser = ServiceConfigOptionParser(sys.argv)

    suite = BasicWorkflowTestSuite("SMAPI Method Validation", args=parser.args)

    suite.smapiservice = SMAPIService(parser.config_file, parser.wsdl, getattr(parser.options, 'content_file'),
                                      suite.logger, suite.console_logger)
    suite.client = suite.smapiservice.buildservice()
    suite.client.login()

    f = SMAPIMethodValidation(suite.client, suite.smapiservice)

    suite.run(f)#(SMAPIMethodValidation(client, smapiservice))