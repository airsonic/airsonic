import selftestinit #ignore @UnusedImport for packaging
import sys
import urllib2

from urlparse import urlparse
from sonos.workflow.basicsuite import BasicWorkflowTestSuite
from sonos.workflow.fixture import WorkflowTestFixture
from sonos.smapi.smapiservice import SMAPIService, MESSAGESTRINGID
from sonos.smapi.smapiclient import SMAPIClient, USER_AGENT
from sonos.smapi.xmlparser import XMLParser
from utility import ServiceConfigOptionParser, SvgValidator, EasyXML

class Ratings(WorkflowTestFixture, SMAPIClient, SMAPIService):
    """
    This class holds the tests that will check that the SMAPI service correctly implemented ratings.
    """
    def __init__(self, client, smapiservice):
        super(Ratings, self).__init__()
        self.client = client
        self.smapiservice = smapiservice

        self.id = self.smapiservice.get_test_track_id()
        self.strings_file = self.smapiservice.get_strings_file_location()
        self.pmap_file = self.smapiservice.get_pmap_file_location()
        self.xsd_doc_name = 'presentationmap.xsd'
        self.xmlparse = XMLParser()
        self.support_ratings = self.smapiservice.get_supports_ratings()

    def initialize(self):
        pass


    def generate_ratings(self):
        values_list = self.smapiservice.get_ratings_values()
        for value in values_list:
            yield value

    def test_combinatorial_all_ratings(self, generate_ratings):
        self.verifyTrueOrSkip(self.support_ratings, "Service must support Ratings for this test to run.")

        self.verifyFalseOrStop(self.id == '', "Service must provide a valid track ID ([Test Content]:track in the "
                                              "config file) for this test to run.")

        self.verifyTrueOrStop(generate_ratings, "Service must provide valid ratings in the presentation map for this"
                                                " test to run.")

        rate_item_response = self.client.rateItem(self.id, generate_ratings, self)
        self.verifyIsNotNoneOrStop(rate_item_response, "rateItem should return something other than None.")

        if MESSAGESTRINGID in rate_item_response:#messageStringId is optional
            strings_list = self.xmlparse.parseStringsFileNode(self.xmlparse.ingestXMLFile(self.strings_file))
            pmap_list = self.xmlparse.parsePresentationMapRatings(self.xmlparse.ingestXMLFile(self.pmap_file))
            result = self.xmlparse.comparePmapToStrings(pmap_list, strings_list)
            self.verifyTrueOrFailCase(result, "{0} value should be found in both the strings and presentation map "
                                              "files.".format(MESSAGESTRINGID))

    """
    Function will check to see if ratings are enabled for service. If they are, it will load the pmap, parse out each individual
    Rating element under PresentationMap type="NowPlayingRatings" Matches.

    - Will validate that the pmap is valid and returns between 200-400 response code
    - Icon must be in SVG format (FAIL)
    - URL must not return 404 (FAIL)
    - If the URL for the "universal" icon is one of the Sonos fallback icons, output an INFO message as appropriate
    - Presentation map xsd validation will be handled by the presentationmap.py test, not this test.
    eg:
    	<PresentationMap type="NowPlayingRatings">
		<!--

		 Each of the Rating Id's that are specified in this file
		 must be unique. In order for the rateItem call to not have
		 an exhaustive list of these Id -> rating mappings 0 and even numbered
		 Ids denote a vote down, while odd numbered Ids denote a vote up

		 -->
		<Match propname="vote" value="-">
			<Ratings>
				<Rating AutoSkip="NEVER" Id="1" StringId="VoteUp" OnSuccessStringId="VoteUpSuccess">
					<Icon Controller="icr" LastModified="18:56:26 29 July 2013" Uri="http://songza.com/devices/sonos/api/1.1/static/images/ratings/vote-up-52x52.png?v=d3c6b815319f"/>
					<Icon Controller="acr" LastModified="18:56:26 29 July 2013" Uri="http://songza.com/devices/sonos/api/1.1/static/images/ratings/vote-up-52x52.png?v=d3c6b815319f"/>
					<Icon Controller="acr-hdpi" LastModified="18:56:26 29 July 2013" Uri="http://songza.com/devices/sonos/api/1.1/static/images/ratings/vote-up-80x80.png?v=d3c6b815319f"/>
					<Icon Controller="macdcr" LastModified="18:56:26 29 July 2013" Uri="http://songza.com/devices/sonos/api/1.1/static/images/ratings/vote-up-40x32.png?v=d3c6b815319f"/>
					<Icon Controller="pcdcr" LastModified="18:56:26 29 July 2013" Uri="http://songza.com/devices/sonos/api/1.1/static/images/ratings/vote-up-40x32.png?v=d3c6b815319f"/>
					<Icon Controller="cr200" LastModified="18:56:26 29 July 2013" Uri="http://songza.com/devices/sonos/api/1.1/static/images/ratings/vote-up-66x66.png?v=d3c6b815319f"/>
					<Icon Controller="universal" LastModified="18:56:26 29 July 2013" Uri="http://sonos-img.ws.sonos.com/thumbs-up-unselected.svg"/>
				</Rating>
				<Rating AutoSkip="ALWAYS" Id="0" StringId="VoteDown" OnSuccessStringId="VoteDownSuccess">
					<Icon Controller="icr" LastModified="18:56:26 29 July 2013" Uri="http://songza.com/devices/sonos/api/1.1/static/images/ratings/vote-down-52x52.png?v=d3c6b815319f"/>
					<Icon Controller="acr" LastModified="18:56:26 29 July 2013" Uri="http://songza.com/devices/sonos/api/1.1/static/images/ratings/vote-down-52x52.png?v=d3c6b815319f"/>
					<Icon Controller="acr-hdpi" LastModified="18:56:26 29 July 2013" Uri="http://songza.com/devices/sonos/api/1.1/static/images/ratings/vote-down-80x80.png?v=d3c6b815319f"/>
					<Icon Controller="macdcr" LastModified="18:56:26 29 July 2013" Uri="http://songza.com/devices/sonos/api/1.1/static/images/ratings/vote-down-40x32.png?v=d3c6b815319f"/>
					<Icon Controller="pcdcr" LastModified="18:56:26 29 July 2013" Uri="http://songza.com/devices/sonos/api/1.1/static/images/ratings/vote-down-40x32.png?v=d3c6b815319f"/>
					<Icon Controller="cr200" LastModified="18:56:26 29 July 2013" Uri="http://songza.com/devices/sonos/api/1.1/static/images/ratings/vote-down-66x66.png?v=d3c6b815319f"/>
					<Icon Controller="universal" LastModified="18:56:26 29 July 2013" Uri="http://sonos-img.ws.sonos.com/thumbs-down-unselected.svg"/>
				</Rating>
			</Ratings>
		</Match>

    """
    def test_res_ratings_icons(self):
        try:
            from lxml import etree
        except ImportError:
            self.console_logger.info("You do not have lxml toolkit, please download from: https://pypi.python.org/pypi"
                                     "/lxml")
            return None

        self.verifyTrueOrSkip(self.support_ratings, "Service must support Ratings for this test to run.")

        self.verifyTrueOrSkip(self.pmap_file, "Service must provide a presentation map URI ([Presentation Map File]:"
                                              "pmapLocation in the config file) for this test to run.")

        try:
            pmapFile = urllib2.urlopen(self.pmap_file)
            self.verifyInRangeOrStop(pmapFile.getcode(), 200, 399, "Attempting to open the presentation map URI ({0}) "
                                                                   "should return an HTTP status between 200 and 399."
                                     .format(self.pmap_file))
        except Exception, e:
            self.stop("Attempting to open the presentation map URI ({0}) returned an exception: {1}."
                      .format(self.pmap_file, e))

        # Validate presentation map against XSD
        try:
            xsd_doc = etree.parse(self.xsd_doc_name)
        except Exception, e:
            self.stop("Parsing the presentation map ({0}) returned an exception: {1}.".format(self.pmap_file, e))

        xsd = etree.XMLSchema(xsd_doc)
        xml = etree.parse(pmapFile)
        validate_result = xsd.validate(xml)
        pmapFile.close()
        self.verifyTrueOrWarn(validate_result, xsd.error_log)

        pmap_list = self.xmlparse.parsePresentationMapHiResRatings(self.pmap_file)

        """
        pmap_list will be an array that represents a dictionary of each element [{propname, value, Ratings: [{Id, StringId, Icons: [{Controller, Uri}, ...]}, ...]} ...}] from the pmap with the url to the icon.
        eg: [{propname: isStarred, value:0, Ratings: [{Id: 1, StringId: STAR_STRING, Icons: [{Controller: universal, Uri: http://...}, ...]}, ...}, ...]
        """

        missing_universal_icon_node = 'Each Rating node MUST have an Icon node with Controller attribute set to "universal" with a valid Uri.'
        sonos_rating_icon_msg = 'Service is using a default sonos rating icon'
        user_agent = USER_AGENT

        self.verifyTrueOrStop(len(pmap_list) > 0, "The presentation map node with type='NowPlayingRatings' "
                                                  "must contain at least one Match node.")
        for match in pmap_list:
            self.verifyTrueOrStop(len(match['Ratings']) > 0, "Match node must contain at least one Ratings node.")

            for rating in match['Ratings']:
                universal_icon_found = False

                for icon in rating['Icons']:
                    if icon['Controller'] == 'universal':
                        universal_icon_found = True
                        universal_url = icon['Uri']

                        # Verify that we have a value in place for the universal icon uri
                        self.verifyIsNotNoneOrFailCase(universal_url, missing_universal_icon_node)

                        # Provide some logging if the service is using our supplied icons
                        if urlparse(universal_url).netloc == 'sonos-img.ws.sonos.com':
                            self.logger.info(sonos_rating_icon_msg)
                            self.console_logger.info(sonos_rating_icon_msg)

                        # Validate that image is an svg
                        self.verifyEqualOrStop('svg', universal_url[-3:], "Universal rating icons must be in SVG 1.1 "
                                                                          "basic format.")

                        # Verify that the link provided can be reached
                        try:
                            site = universal_url
                            hdr = {'User-Agent': user_agent}

                            req = urllib2.Request(site, headers=hdr)
                            image = urllib2.urlopen(req)
                            svg_validator = SvgValidator(image, universal_url)

                            well_formed, error_message = svg_validator.validate_well_formed()
                            self.verifyTrueOrStop(well_formed, error_message)

                            dtd_conform, error_message = svg_validator.validate_against_dtd()
                            self.verifyTrueOrFailCase(dtd_conform, error_message)
                        except urllib2.HTTPError, http_e:
                            self.fail("Attempting to open a ratings icon URI ({0}) returned an HTTP {1} code."
                                      .format(universal_url, http_e.code))
                        except urllib2.URLError, url_e:
                            self.fail("Attempting to open a ratings icon URI ({0}) returned a error: {1}."
                                      .format(universal_url, url_e))
                    else:
                        non_universal_url = icon['Uri']

                        #Verify that we have a value in place for the non_universal icon uri
                        xpath_to_url = ".//PresentationMap[@type='NowPlayingRatings']/Match/Ratings/Rating/icon" \
                                       "[@Controller='{0}']".format(icon["Controller"])
                        self.verifyIsNotNoneOrFailCase(non_universal_url, "All icons of {0} controller should have a "
                        "url attribute in the icon tag at {1}.".format(icon["Controller"], xpath_to_url))

                        # Validate that image is an png
                        self.verifyEqualOrStop('png', non_universal_url[-3:], "Legacy rating icons must be in png format.")

                        try:
                            site = non_universal_url
                            hdr = {'User-Agent': user_agent}

                            req = urllib2.Request(site, headers=hdr)
                            non_universal_image = urllib2.urlopen(req)

                        except urllib2.HTTPError, http_e:
                            self.fail("Attempting to open a ratings icon URI ({0}) returned an HTTP {1} code."
                                      .format(non_universal_url, http_e.code))
                        except urllib2.URLError, url_e:
                            self.fail("Attempting to open a ratings icon URI ({0}) returned an error: {1}."
                                      .format(non_universal_url, url_e))

                self.verifyTrueOrFailCase(universal_icon_found, "Rating nodes must contain an Icon node with "
                                                                "Controller='universal'.")

    def test_meta_data(self):
        """
        This test will check if services that support ratings are appropriately formatting their metadata with
        dynamic tags that match those in their pmap
        """
        self.verifyTrueOrSkip(self.support_ratings, "Service must support Ratings for this test to run.")

        self.verifyTrueOrStop(self.id != '', "Service must provide a valid track ID ([Test Content]:track in the "
                                              "config file) for this test to run.")

        test_content = self.id

        try:
            response = self.client.getMediaMetadata(test_content, self)
            if hasattr(response, 'dynamic'):
                if hasattr(response.dynamic, 'property'):
                    for property in response.dynamic.property:
                        if hasattr(property, 'name') and hasattr(property, 'value'):
                            rating_name = property.name
                            rating_value = property.value
                            name_and_value_exist = EasyXML.xml_xpath_findall(self.pmap_file,
                                                                                   ".//PresentationMap[@type="
                                                                                   "'NowPlayingRatings']/Match[@propname="
                                                                                   "'{0}'][@value='{1}']"
                                                                                       .format(rating_name, rating_value))
                            self.verifyTrueOrFailCase(name_and_value_exist, "getMediaMetadata should return a rating value "
                                                                            "and name which both map to entries in the "
                                                                            "presentation map.")
                        else:
                            self.fail("'property' nodes should contain 'name' and 'value' nodes inside.")
                else:
                    self.fail("'dynamic' nodes should contain 'property' nodes inside.")
            else:
                self.fail("getMediaMetadataResult should contain 'dynamic' nodes inside.")
        except Exception, w:
            self.fail(w)

        try:
            response = self.client.getExtendedMetadata(test_content, self)
            if hasattr(response, 'mediaMetadata'):
                if hasattr(response.mediaMetadata, 'dynamic'):
                    if hasattr(response.mediaMetadata.dynamic, 'property'):
                        if hasattr(response.mediaMetadata.dynamic.property[0], 'name') and \
                                hasattr(response.mediaMetadata.dynamic.property[0], 'value'):
                            rating_name = response.mediaMetadata.dynamic.property[0].name
                            rating_value = response.mediaMetadata.dynamic.property[0].value
                            name_and_value_exist = EasyXML.xml_xpath_findall(self.pmap_file,
                                                                                   ".//PresentationMap"
                                                                                   "[@type='NowPlayingRatings']/Match"
                                                                                   "[@propname='{0}'][@value='{1}']"
                                                                                       .format(rating_name,
                                                                                               rating_value))
                            self.verifyTrueOrFailCase(name_and_value_exist, "getMediaMetadata should return a rating "
                                                                            "value and name which both map to entries "
                                                                            "in the presentation map.")
                        else:
                            self.fail("'property' nodes should contain 'name' and 'value' nodes inside.")
                    else:
                        self.fail("'dynamic' nodes should contain 'property' nodes inside.")
                else:
                    self.fail("getMediaMetadataResult should contain 'dynamic' nodes inside.")
            else:
                self.fail("getExtendedMetadata should return mediaMetadata.")
        except Exception, w:
            self.fail(w)

#Main
if __name__ == "__main__":
    parser = ServiceConfigOptionParser(sys.argv)
    suite = BasicWorkflowTestSuite("Ratings Validation", args=parser.args)

    suite.smapiservice = SMAPIService(parser.config_file, parser.wsdl, getattr(parser.options, 'content_file'),
                                      suite.logger, suite.console_logger)
    suite.client = suite.smapiservice.buildservice()
    suite.client.login()

    suite.run(Ratings(suite.client, suite.smapiservice))