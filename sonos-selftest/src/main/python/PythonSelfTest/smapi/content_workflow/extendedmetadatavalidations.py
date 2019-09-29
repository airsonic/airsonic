import selftestinit #ignore @UnusedImport for packaging
import sys
from sonos.workflow.basicsuite import BasicWorkflowTestSuite
from sonos.workflow.fixture import WorkflowTestFixture
from sonos.smapi.smapiservice import SMAPIService, STREAM, ARTIST, PROGRAM, TRACK, ALBUM, PLAYLIST, OTHER
from sonos.smapi.smapiclient import SMAPIClient, CR_STANDARD_RANGE
from sonos.smapi.smapiexceptions import ItemNotFound
from utility import ServiceConfigOptionParser
from suds.sax.text import Text
import string

def generate_test_data ():
    yield ( TRACK   ,"self.smapiservice.get_test_track_id()"   )
    yield ( ARTIST  ,"self.smapiservice.get_test_artist_id()"  )
    yield ( ALBUM   ,"self.smapiservice.get_test_album_id()"   )
    yield ( PROGRAM ,"self.smapiservice.get_test_prog_id()"    )
    yield ( STREAM  ,"self.smapiservice.get_test_stream_id()"  )
    yield ( PLAYLIST,"self.smapiservice.get_test_playlist_id()")
    yield ( OTHER   ,"self.smapiservice.get_test_other_id()"   )

class ExtendedMetadataValidations(WorkflowTestFixture, SMAPIClient, SMAPIService):
    """
    This class provides tests for getExtendedMetadata functionality
    """
    def __init__(self, client, smapiservice):
        super(ExtendedMetadataValidations,self).__init__()
        self.client = client
        self.smapiservice = smapiservice
        self.played_seconds = 10

    def initialize(self):
        self.metadata_data = generate_test_data()

    def content_type_list_from_config_file(self, content_type, related_type):
        #"""this function creates a list of types from config file"""
        if related_type == 'relatedText':
            config_list = self.smapiservice.get_extMetadataTxt_type()
        else:
            config_list = self.smapiservice.get_extMetadataBrowse_type()

        type_list = []
        for each in config_list:
            if each[0] == content_type:
                type_list.append(each[1])
                for content_type_list in type_list:
                    content_type_list.replace("'", "")
                return content_type_list

    def match_content_against_config(self, related_data, content_type_list):
        #"""This function compares retrieved content with provided in the config file"""
        for node in related_data:
            self.verifyInOrFailCase('id', node, "relatedText/Browse nodes should contain an id.")
            self.verifyInOrFailCase('type', node, "relatedText/Browse nodes should contain a type.")
            for pair in node:
                if 'type' in pair and pair[0] == 'type':
                    type_given = pair[1]
                    self.verifyInOrFailCase(type_given, content_type_list, "The relatedText/Browse types should correspond to those provided in configuration.")

    def browse(self, content_type, content_list):
        #"""This function executes browse call on the relatedBrowse ID"""
        for node in content_list:
            for pair in node:
                if 'id' in pair:
                    content_id = pair[1]
                    try:
                        (response, _) = self.client.getMetadata(content_id, CR_STANDARD_RANGE.index,
                                                                CR_STANDARD_RANGE.count,
                                                                self) #ignore @UndefinedVariable for named tuple
                        self.verifyIsNotNoneOrFailCase(response, "getMetadata should return something other than None.")
                    except ItemNotFound, w: #we do not require for the service to have Text data, we do require certain types being return
                        self.warn(w)

    def getextendedmetadatatext(self, content_type, content_id):
        #"""This function verifies that text or it's instance are present"""
        extended_metadata_text_types = self.smapiservice.get_extMetadataTxt_type()
        for content_node in extended_metadata_text_types:
            if content_node[0] == content_type:
                self.verifyTrueOrSkip(len(content_node[1]) != 0, "Service must provide a valid {0} ID ([Test Content]:"
                                                                 "{0} in the config file) must be provided in "
                                                                 "configuration in order for this test to run."
                                      .format(content_type))
                get_extended_metadata_text_response = None
                if len(content_type) != 0 and content_id != 0:
                    content_node_list = string.split(content_node[1])
                    #Case for multiple types available
                    for each_content_node in content_node_list:
                        try:
                            each_content_node = each_content_node.strip(',')
                            get_extended_metadata_text_response = self.client.getExtendedMetadataText(content_id,
                                                                                                      each_content_node,
                                                                                                      self)
                        except ItemNotFound, w: #we do not require for the service to have Text data, we do require certain types being return
                            self.warn(w)
                        self.verifyTrueOrFailCase((get_extended_metadata_text_response is None) or
                                                  isinstance(get_extended_metadata_text_response,Text),
                                                  "getExtendedMetadataText should return None or Text.")
                break

    metadata_data = None
    def test_combinatorial_get_extendedmetadata(self, metadata_data):
        """
        This test verifies\n
        1. for album and artist - that return is of mediaCollection type\n
        2. for track and stream - that return is of mediaMetadata type\n
        3. for program radio after getting to the track level - that return is of mediaMetadata type\n
        4. for all types - verifies that if related browse/text is there, the format is correct: id and type\n
        5. for all types - verifies that relatedText and relatedBrowse types correspond to those provided in configuration\n
        6. for relatedText - that it has appropriate response type\n
        7. for relatedBrowse - that it has the ability to browse using obtained relatedBrowse ID\n
        8. for relatedActions - verifies list of related actions contains supported generic action types\n

        :param metadata_data: the metadata returned for a particular object (determined by the driller).
        """
        #Check if extended metadata functionality is supported
        self.verifyTrueOrSkip(self.smapiservice.get_extMetadataSupport(), "Service must support extended metadata "
                                                                          "([Capabilities]:extendedMetadata in the "
                                                                          "config file) for this test to run.")

        #Get data from the generator and convert it
        (content_type, getter_function) = metadata_data
        try:
            content_id = eval(getter_function)
            #Check if necessary for test to run data is provided
            self.verifyTrueOrSkip(content_id, "Content ID must be provided in configuration in order for this test to "
                                              "run.")
            if content_type == PROGRAM:
                (response, _) = self.client.getMetadata(content_id, CR_STANDARD_RANGE.index, CR_STANDARD_RANGE.count,
                                                        self) #ignore @UndefinedVariable for named tuple
                self.verifyIsNotNoneOrStop(response, "getMetadata should return something other than None.")
                self.verifyTrueOrSkip(self.smapiservice.get_extMetadataSupport(), "Service must support extended "
                                                                                  "metadata ([Capabilities]:"
                                                                                  "extendedMetadata in the config file)"
                                                                                  " in order for this test to run.")
                self.verifyTrueOrStop(response.Items, "getMetadata on a Program (id is {0}) should return a list"
                                                      "of tracks.".format(content_id))
                for track in response.Items:
                    extended_metadata = self.client.getExtendedMetadata(track.id, self)
                    self.verifyTrueOrSkip(extended_metadata is not None, "getExtendedMetadata should return something"
                                                                         "other than None.")
                    self.verifyTrueOrFailCase(('mediaMetadata' in extended_metadata), "getExtendedMetadata(track) "
                                                                                      "should return mediaMetadata.")

            else:
                extended_metadata = self.client.getExtendedMetadata(content_id, self)
                self.verifyTrueOrSkip(extended_metadata is not None, "getExtendedMetadata should return something "
                                                                     "other than None")
                if content_type == ALBUM or content_type == ARTIST:
                    self.verifyTrueOrFailCase(('mediaCollection' in extended_metadata), "getExtendedMetadata(album or "
                                                                                        "artists) should return "
                                                                                        "a mediaCollection.")

                elif content_type == TRACK or content_type == STREAM:
                    self.verifyTrueOrFailCase(('mediaMetadata' in extended_metadata), "getExtendedMetadata(track or "
                                                                                      "stream) should return "
                                                                                      "mediaMetadata."
                    )
            self.verifyTrueOrSkip(('relatedBrowse' in extended_metadata) or ('relatedText' in extended_metadata),
                                  "getExtendedMetadata must be implemented for this test to run.")

            if 'relatedBrowse' in extended_metadata:
                content_type_browse = 'browse' + content_type
                if len(extended_metadata.relatedBrowse):
                    #create a list of types from config file
                    content_type_list = self.content_type_list_from_config_file(content_type_browse, 'relatedBrowse')
                    self.verifyTrueOrSkip(content_type_list != '', "relatedBrowse data should be provided in the "
                                                                   "config file for this test to run.")
                    #content retrieved and provided in the config file should match
                    self.match_content_against_config(extended_metadata.relatedBrowse, content_type_list)
                    #perform basic browse calls
                    self.browse(content_type_browse, extended_metadata.relatedBrowse)

            if 'relatedText' in extended_metadata:
                content_type_text = 'text' + content_type
                if len(extended_metadata.relatedText):
                    #create a list of types from config file
                    content_type_list = self.content_type_list_from_config_file(content_type_text, 'relatedText')
                    self.verifyTrueOrSkip(content_type_list and content_type_list != '', "relatedText data should be "
                                                                                         "provided in the config file"
                                                                                         " for this test to run.")
                    #content retrieved and provided in the config file should match
                    self.match_content_against_config(extended_metadata.relatedText, content_type_list)
                    #text or it's instance should be found
                    self.getextendedmetadatatext(content_type_text, content_id)
                    
            if content_type == TRACK:
                self.verifyTrueOrSkip(('relatedActions' in extended_metadata), "getExtendedMetadata must be implemented for this test to run.")
                if 'relatedActions' in extended_metadata:
                    self.verifyTrueOrFailCase(('action' in extended_metadata.relatedActions), "getExtendedMetadata must return at least one generic action")
                    for node in extended_metadata.relatedActions:
                        actionNode = node[1];
                        self.verifyInOrFailCase('id', actionNode[1], "relatedActions/action nodes should contain an id.")
                        self.verifyInOrFailCase('title', actionNode[1], "relatedActions/action nodes should contain a title.")
                        self.verifyTrueOrFailCase(('openUrlAction' in actionNode[1]) or
                                                  ('simpleHttpRequestAction' in actionNode[1]) or
                                                  ('controlAction' in actionNode[1]) ,
                                                   "generic action must contain on of: openUrlAction, simpleHttpRequestAction, controlAction")
                
        except ItemNotFound, w:
            if content_type == PLAYLIST:
                self.warn(w)
            else:
                raise

if __name__ == "__main__":

    parser = ServiceConfigOptionParser(sys.argv)

    suite = BasicWorkflowTestSuite("Extended Metadata Validations", args=parser.args)

    suite.smapiservice = SMAPIService(parser.config_file, parser.wsdl, getattr(parser.options, 'content_file'),
                                      suite.logger, suite.console_logger)
    suite.client = suite.smapiservice.buildservice()
    suite.client.login()

    f = ExtendedMetadataValidations(suite.client, suite.smapiservice)
    f.initialize()

    suite.run(f)
