import selftestinit  # ignore @UnusedImport for packaging
import sys
import operator
from sonos.workflow.fixture import WorkflowTestFixture
from sonos.workflow.basicsuite import BasicWorkflowTestSuite
from sonos.smapi.smapiservice import SMAPIService, TRACK, STREAM, PROGRAM, ARTIST, ALBUM, CRAWLER_DISABLE
from sonos.smapi.smapiclient import CR_STANDARD_RANGE
from sonos.smapi.smapiexceptions import ItemNotFound
from sonos.smapi.smapiclient import SMAPIClient
from utility import Validation
from utility import ServiceConfigOptionParser
import fileinput


class UpdateTestContent(Validation):
    """
    This class holds the tests that will attempt to update the test content marked as ephemeral in the partner's
    service config file.
    """
    def __init__(self, client, smapiservice):
        super(UpdateTestContent, self).__init__()
        self.client = client
        self.smapiservice = smapiservice
        self.test_content_needed = {TRACK: self.smapiservice.get_ephemeral_track_id(),
                                    ALBUM: self.smapiservice.get_ephemeral_album_id(),
                                    ARTIST: self.smapiservice.get_ephemeral_artist_id(),
                                    PROGRAM: self.smapiservice.get_ephemeral_program_id(),
                                    STREAM: self.smapiservice.get_ephemeral_stream_id()}

    def initialize(self):
        self.get_test_content_driller = self.generate_iterative_list_drill_depth_first(self.determiner_get_test_content)

    def determiner_get_test_content(self):
        #""" Args (type(s) of node to test,number of matching nodes to find and test)"""
        return (self.item_type_filter(self.LEAF_CONTAINER_TYPES + self.BROWSEABLE_CONTAINER_TYPES),
                self.number_to_check(30))

    get_test_content_driller = None

    def test_combinatorial_get_test_content(self, get_test_content_driller):
        """
        This test will do a depth-first search into the music catalog of the partner's service and attempt to fill
        the test content fields, which are marked as ephemeral in the partner's config file, with the new content found.

        :param get_test_content_driller: this is a content driller used by the framework
        """
        self.verifyTrueOrSkip(reduce(operator.or_, self.test_content_needed.values()),
                              'No test content needs updating.')
        if get_test_content_driller.itemType == TRACK:
            response = self.client.getMediaMetadata(get_test_content_driller.id, self)
            if hasattr(response, 'mediaMetadata'):
                response = response.mediaMetadata
            if self.test_content_needed.get(TRACK) is True:
                self.test_content_needed[TRACK] = False
                self.smapiservice.set_test_content(response.title, response.id, TRACK)

            if self.test_content_needed.get(ALBUM) is True and hasattr(response, 'trackMetadata') and \
                    hasattr(response.trackMetadata, ALBUM) and hasattr(response.trackMetadata, 'albumId'):
                self.test_content_needed[ALBUM] = False
                self.smapiservice.set_test_content(response.trackMetadata.album,
                                                   response.trackMetadata.albumId, ALBUM)

            if self.test_content_needed.get(ARTIST) is True and hasattr(response, 'trackMetadata') and \
                    hasattr(response.trackMetadata, ARTIST) and hasattr(response.trackMetadata, 'artistId'):
                self.test_content_needed[ARTIST] = False
                self.smapiservice.set_test_content(response.trackMetadata.artist,
                                                   response.trackMetadata.artistId, ARTIST)

        elif get_test_content_driller.itemType == ALBUM:
            if self.test_content_needed.get(ALBUM) is True and hasattr(get_test_content_driller, 'title') and \
                    hasattr(get_test_content_driller, 'id'):
                self.test_content_needed[ALBUM] = False
                self.smapiservice.set_test_content(get_test_content_driller.title, get_test_content_driller.id, ALBUM)

        elif get_test_content_driller.itemType == PROGRAM:
            if self.test_content_needed.get(PROGRAM) is True and hasattr(get_test_content_driller, 'title') and \
                    hasattr(get_test_content_driller, 'id'):
                self.test_content_needed[PROGRAM] = False
                self.smapiservice.set_test_content(get_test_content_driller.title, get_test_content_driller.id, PROGRAM)

        elif get_test_content_driller.itemType == STREAM:
            response = self.client.getMediaMetadata(get_test_content_driller.id, self)
            self.verifyIsNotNoneOrFailCase(response,
                                           "Requesting getMediaMetadata on a stream should not return None")
            if self.test_content_needed.get(STREAM) is True and hasattr(response, 'title') \
                and hasattr(response, 'id'):
                self.test_content_needed[STREAM] = False
                self.smapiservice.set_test_content(response.title, response.id, STREAM)

        self.smapiservice.__init__(self.smapiservice.configfile, parser.wsdl, CRAWLER_DISABLE)

#Main
if __name__ == "__main__":
    parser = ServiceConfigOptionParser(sys.argv)

    suite = BasicWorkflowTestSuite("Update Test Content", args=parser.args)

    suite.smapiservice = SMAPIService(parser.config_file, parser.wsdl, CRAWLER_DISABLE)
    suite.client = suite.smapiservice.buildservice()
    suite.client.login()

    f = UpdateTestContent(suite.client, suite.smapiservice)
    f.initialize()

    suite.run(f)