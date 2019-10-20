import selftestinit #ignore @UnusedImport for packaging 
import sys
import urllib2
from urlparse import urljoin
import time
from sonos.workflow.basicsuite import BasicWorkflowTestSuite
from sonos.smapi.smapiservice import SMAPIService, STREAM, METADATAREFRESHDURATION
from utility import Validation, ServiceConfigOptionParser

class StreamValidation(Validation):
    """
    This class holds the tests that will invoke getMetadata successive times,
    confirming that subsequent results are different. (Each GET fetches a new segment of music.)
    """

    def __init__(self, client, smapiservice):
        super(Validation,self).__init__()
        self.client = client
        self.smapiservice = smapiservice
        self.not_supported = 0

    def initialize(self):
        self.test_stream_driller = self.generate_iterative_list_drill(self.determiner_stream)

    def determiner_stream(self):
        #""" Args (type of node to test,number of matching nodes to find and test)"""
        return (self.item_type_filter(STREAM), self.number_to_check(10))

    test_stream_driller = None
    def test_combinatorial_test_stream_radio(self,test_stream_driller):
        """
        This test will compare output from the radio node invoked multiple times

        :param test_stream_driller: this is a content driller used by the framework
        """
        self.verify_service_support(test_stream_driller, "Service must provide a valid stream ID ([Test Content]:"
                                                         "stream in the config file) for this test to run.")
        self.verifyTrueOrSkip(test_stream_driller, "Service must yield valid objects when browsing for this test to "
                                                   "run.")

        response = self.client.getMediaMetadata(test_stream_driller.id, self)
        self.verifyIsNotNoneOrFailCase(response, "getMetdata should return something other than None.")

        # there are two variations of XML schema for getMediaMetadataResult defined in Sonos.wsdl
        # check if the response is wrapped in <mediaMetadata />. If yes, retrieve the inner data and update response
        if hasattr(response, 'mediaMetadata'):
            response = response.mediaMetadata

        """stream.mediaMetadata is tested in httpverifications module"""
        if hasattr(response, 'mimeType'):
            self.verifyNotEqualOrFailCase(response.mimeType, 0, "mediaMetadata should contain a non-empty 'mimeType'.")
        else:
            self.warn('mimeType is missing')
        self.verifyTrueOrFailCase((response.id is not None) and (len(response.id) > 0), "mediaMetadata should contain "
                                                                                        "a non-empty 'ID'.")
        self.verifyTrueOrWarn(hasattr(response,'streamMetadata') and hasattr(response.streamMetadata,'logo') and
                                                                              response.streamMetadata.logo is not None
                               and (len(response.streamMetadata.logo) > 0), "streamMetadata should contain a "
                                                                               "non-empty 'logo'.")
        self.verifyTrueOrFailCase((response.title is not None) and (len(response.title) > 0), "mediaMetadata should "
                                                                                              "contain a non-empty "
                                                                                              "'title'.")

    def test_validate_content_key(self):
        """
        this is a test that validates content key
        """
        streamId = str(self.smapiservice.get_test_stream_id())
        self.verifyTrueOrSkip(streamId is not '', "Service must provide a valid stream ID ([Test Content]:"
                                                  "stream in the config file) for this test to run.")
        self.verifyTrueOrSkip(self.smapiservice.get_HLSContent_setting(), "Service must support HLS content "
                                                                          "([Additional Settings]:HLSContent in the "
                                                                          "config file) for this test to run.")

        fault = False
        URLException = False

        mediaURI = self.client.getMediaURI(streamId, self)
        self.verifyIsNotNoneOrStop(mediaURI, "getMediaURI should return something other than None.")

        variantURI = str(mediaURI.getMediaURIResult)

        #Grab contents of variant file
        try:
            variantContent = urllib2.urlopen(variantURI).read()
        except urllib2.URLError as e:
            self.fail("Attempting to open a mediaURI ({0}) returned an exception: {1}."
                      .format(variantURI,e))

        self.verifyIsNotNoneOrStop(variantContent, "Attempting to open a mediaURI ({0}) should return something other "
                                                   "than None.".format(variantURI))

        for indexURI in str(variantContent).splitlines():  #iterate line-by-line to find the first data source
            if not indexURI.startswith('#'):
                break

        indexFile = None
        try:
            indexFile = urllib2.urlopen(indexURI)
        except ValueError: # indexURI is relative
            abs_indexURI = urljoin(variantURI, indexURI)
        except urllib2.URLError as e:
            self.stop("URLError exception {0} is thrown when opening URL {1}".format(e.message, indexURI))

        #Grab contents of index file
        if indexFile:
            indexContent = indexFile.read()
        else:
            try:
                indexContent = urllib2.urlopen(abs_indexURI).read()
            except urllib2.URLError:
                URLException = True
                self.verifyFalseOrFailCase(URLException, "Encountered problem fetching index URL resource.")

        self.verifyIsNotNoneOrStop(indexContent,'Could not open Index URL')

        # TODO: the following logic of getting contentKeyURI is based on SiriusXM index file format: "#EXT-X-KEY:METHOD=AES,URI=data:application/octet-stream;base64,0Nsco7MAgxowGvkUT8aYag"
        # TODO: it fails for MLB HLS as MLB HLS does not have getContentKey SMAPI API implemented.
        for contentKeyURI in str(indexContent).splitlines():
            if "URI=" in contentKeyURI:
                contentKeyURI = contentKeyURI.split("URI=")[1]
                break
            elif "uri=" in contentKeyURI:
                contentKeyURI = contentKeyURI.split("uri=")[1]
                break

        #now perform getContentKey
        try:
            contentKeyResponse = self.client.getContentKey(streamId, contentKeyURI, self)
            self.verifyEqualOrFailCase(str(contentKeyURI), str(contentKeyResponse.uri), "Content Key mismatch in response")
        except Exception:
            fault=True

        self.verifyFalseOrFailCase(fault, "Fault in GetContentKey response")

    def test_validate_getStreamingMetadata(self):
        """
        this is a test that verifies getStreamingMetadata
        """
        streamId = str(self.smapiservice.get_test_stream_id())
        self.verifyTrueOrSkip(streamId is not '','Service should support streaming or this test will not run.')
        self.verifyTrueOrSkip(self.smapiservice.get_HLSContent_setting(), "HLS content should be enabled to run this test")

        hasOutOfBandMetadata = False
        fault = False
        #Check if extended metadata functionality is supported
        self.verifyTrueOrSkip(self.smapiservice.get_extMetadataSupport(),'Service should support Extended Metadata to run this test')
        streamMetadata = self.client.getExtendedMetadata(streamId, self)#ignore @UndefinedVariable for named tuple
        self.verifyIsNotNoneOrStop(streamMetadata,'getExtendedMetadata should not return None.')

        self.verifyTrueOrSkip(hasattr(streamMetadata.mediaMetadata.streamMetadata, "hasOutOfBandMetadata"),'Stream id does not support hasOutOfBandMetadata attribute')

        hasOutOfBandMetadata = streamMetadata.mediaMetadata.streamMetadata.hasOutOfBandMetadata
        self.verifyTrueOrSkip(hasOutOfBandMetadata,'Stream id has hasOutOfBandMetadata set to False')

        startTime = time.strftime("%Y-%m-%dT%H:%M:%SZ", time.localtime(time.mktime(time.localtime())))

        try:
            streamingMetadataResponse = self.client.getStreamingMetadata(str(streamId), str(startTime),
                                                                         str(METADATAREFRESHDURATION), self)
            self.verifyTrueOrFailCase(hasattr(streamingMetadataResponse.segmentMetadata[0], "startTime"),'GetStreamingMetadata response invalid, failing.')
        except Exception:
            fault=True

        self.verifyFalseOrFailCase(fault, "Fault in getStreamingMetadata response")

#Main
if __name__ == "__main__":

    parser = ServiceConfigOptionParser(sys.argv)

    suite = BasicWorkflowTestSuite("Stream Validation", args=parser.args)

    suite.smapiservice = SMAPIService(parser.config_file, parser.wsdl, getattr(parser.options, 'content_file'),
                                      suite.logger, suite.console_logger)
    suite.client = suite.smapiservice.buildservice()
    suite.client.login()

    f = StreamValidation(suite.client, suite.smapiservice)
    f.initialize()
    suite.run(f)