import selftestinit #ignore @UnusedImport for packaging 
import sys
from sonos.workflow.basicsuite import BasicWorkflowTestSuite
from sonos.workflow.fixture import WorkflowTestFixture
from sonos.smapi.smapiservice import SMAPIService
from sonos.smapi.smapiclient import SMAPIClient
from utility import ServiceConfigOptionParser
import urllib2

class Presentationmap(WorkflowTestFixture, SMAPIClient, SMAPIService):
    """
    class holds the tests that will validate the partner's presentation map.
    """

    def __init__(self, client, smapiservice):
        super(Presentationmap,self).__init__()
        self.client = client
        self.smapiservice = smapiservice
        self.xsd_doc_name = 'presentationmap.xsd'

    def initialize(self):
        pass

    def validate_match_node_choices(self, parsed_pmap, xsd_doc):
        #"""This method verifies that every match node has at least one of the choice nodes outlined in the xsd file."""
        xml = parsed_pmap
        #self.xsd_doc = xsd_doc
        pmap_root = xml.getroot()
        pmap_match_choices = []
        match_nodes = []
        found_choice_in_match_node = None
        pmap_is_valid = None

        # get the valid choice options for a Match node from the xsd and add to the pmap_match_choices list
        xsd_root = xsd_doc.getroot()
        for xsd_node in xsd_root.findall(".//{http://www.w3.org/2001/XMLSchema}element[@name='Presentation']/"
                                         "{http://www.w3.org/2001/XMLSchema}complexType/"
                                         "{http://www.w3.org/2001/XMLSchema}sequence/"
                                         "{http://www.w3.org/2001/XMLSchema}element[@name='PresentationMap']/"
                                         "{http://www.w3.org/2001/XMLSchema}complexType/"
                                         "{http://www.w3.org/2001/XMLSchema}sequence/"
                                         "{http://www.w3.org/2001/XMLSchema}choice/"
                                         "{http://www.w3.org/2001/XMLSchema}element[@name='Match']/"
                                         "{http://www.w3.org/2001/XMLSchema}complexType/"
                                         "{http://www.w3.org/2001/XMLSchema}choice/"):
            pmap_match_choices.append(xsd_node.get('ref'))

        # find all the match nodes in the pmap and add them to the match_nodes list
        for pmap_match_nodes in pmap_root.findall(".//PresentationMap/Match"):
            match_nodes.append(pmap_match_nodes)

        # for each match node, check to see if there is a choice node that is listed as a valid choice in the xsd
        # if at any point we don't find a vaild choice node return pmap_is_valid = False
        for node in match_nodes:
            for choice in pmap_match_choices:
                if node.findall(choice):
                    found_choice_in_match_node = True
                    break
                else:
                    self.found_choice_in_match_node = False
            if found_choice_in_match_node:
                self.logger.info("Found a valid choice node for the match node.")
                pmap_is_valid = True
            else:
                pmap_is_valid = False
                return pmap_is_valid

        return pmap_is_valid

    def test_presentation_map(self):
        """
        This test validates presentation map for the service against xsd file
        """
        #verify that the lxml package is installed
        try:
            from lxml import etree
        except ImportError:
            self.console_logger.info("You do not have lxml toolkit, please download from: https://pypi.python.org/pypi"
                                     "/lxml")
            return None

        #verify that service provided presentation map
        self.verifyTrueOrSkip(self.smapiservice.get_pmap_file_location(), "Service must provide a presentation map URI "
                                                                          "([Presentation Map File]:pmapLocation in the"
                                                                          " config file) for this test to run.")

        #verify if the link provided in config file is a valid link
        try:
            url = self.smapiservice.get_pmap_file_location()
            xml_doc = urllib2.urlopen(url)
            self.verifyEqualOrFailCase(200, xml_doc.getcode(), "Attempting to open the presentation map URI ({0}) "
                                                               "should return an HTTP 200 status.".format(url))
        except urllib2.HTTPError, e:
            self.stop("Attempting to open the presentation map URI ({0}) returned an HTTP error code of {1}: {2}."
                      .format(self.pmap_file, e.code(), e))

        try:
            xsd_doc = etree.parse(self.xsd_doc_name)
        except Exception, e:
            self.stop("Parsing the presentation map ({0}) returned an exception: {1}.".format(self.pmap_file, e))

        xsd = etree.XMLSchema(xsd_doc)
        xml = etree.parse(xml_doc)
        validate_result = xsd.validate(xml)
        validate_match_nodes = self.validate_match_node_choices(xml, xsd_doc)
        xml_doc.close()
        self.verifyTrueOrFailCase(validate_result, xsd.error_log)
        self.verifyTrueOrFailCase(validate_match_nodes, "Presentation map should contain a valid Match node.")

#Main
if __name__ == "__main__":

    parser = ServiceConfigOptionParser(sys.argv)

    suite = BasicWorkflowTestSuite("Presentation map Validation", args=parser.args)

    suite.smapiservice = SMAPIService(parser.config_file, parser.wsdl, getattr(parser.options, 'content_file'),
                                      suite.logger, suite.console_logger)
    suite.client = suite.smapiservice.buildservice()
    suite.client.login()

    f = Presentationmap(suite.client, suite.smapiservice)
    suite.run(f)