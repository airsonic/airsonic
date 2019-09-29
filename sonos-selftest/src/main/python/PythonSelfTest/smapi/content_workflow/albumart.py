import selftestinit #ignore @UnusedImport for packaging 
import sys
import os
import urllib
import urllib2
import re
from elementtree.SimpleXMLWriter import XMLWriter
from sonos.workflow.basicsuite import BasicWorkflowTestSuite
from sonos.smapi.smapiservice import SMAPIService, TRACKMETADATA, ALBUMARTURI, MEDIAMETADATA, STREAMMETADATA, LOGO, ROOT
from sonos.smapi.smapiclient import CR_STANDARD_RANGE, USER_AGENT
from sonos.smapi.xmlparser import XMLParser
from utility import ServiceConfigOptionParser, Validation, SvgValidator
from suds.sax.text import Text

X_HOUSEHOLD_ID_HEADER = 'X-HouseHoldId'  # Note typo in camel case came from spec
X_AUTH_KEY = 'X-AuthKey'
X_AUTH_TOKEN = 'X-AuthToken'
UTF8 = "utf-8"

class Albumart(Validation):
    """
    This class holds the tests that will check that the Albumart provided by the partner's service meets all the
    SMAPI requirements.
    """
    def __init__(self, client, smapiservice):
        super(Albumart,self).__init__()
        self.client = client
        self.smapiservice = smapiservice
        self.xmlparse = XMLParser()
        self.pmaps_dir = 0 #local pmap directory path
        self.output_file_name = 0
        self.image_lib_install_flag = 0 #installed
        self.image_type = 0 #image type
        self.pmap_url = None
        self.requires_auth = False
        self.device_link_enabled = self.client.authModel == 'DeviceLink'
        self.headers = None

    def initialize(self):
        pass

    def setUpFixture(self):
        # pmap is required to run the test fixture
        self.pmap_url = self.smapiservice.get_pmap_file_location()
        if self.pmap_url:
            try:
                hdr = {'User-Agent': USER_AGENT}
                req = urllib2.Request(self.pmap_url, headers=hdr)
                pmapFile = urllib2.urlopen(req)
            except (urllib2.URLError, urllib2.HTTPError) as e:
                self.stop("Attempting to open the presentation map URI ({0}) returned an error: {1}."
                                    .format(self.pmap_url, e))

            # do other setup work as well
            self.create_local_dir()
            self.create_html_result_file()

    def has_python_image_library(self):
        #not included in python install package
        try:
            from PIL import Image #ignore @UnusedImport check for importability
            return True
        except ImportError:
            self.console_logger.info("You do not have the Python Image Library, please download from https://pypi."
                                     "python.org/pypi/Pillow/")
            return False

    def get_image_pixel_dimensions (self, image_file_name):
        try:
            from PIL import Image
            if os.path.exists(image_file_name):
                im = Image.open(image_file_name)
                (width, height) = im.size
                return (width, height)
            else:
                print "Couldn't find the file"
        except ImportError:
            self.console_logger.info("You do not have the Python Image Library, please download from https://pypi."
                                     "python.org/pypi/Pillow")
            return None

    def create_local_dir(self):
        working_dir_path = os.path.dirname(os.path.abspath(__file__))
        self.pmaps_dir = os.path.join(working_dir_path, 'albumart')
        if not os.path.exists(self.pmaps_dir):
            os.makedirs(self.pmaps_dir)
        img_dir = os.path.normpath(os.path.join(working_dir_path, 'albumart','img'))
        if not os.path.exists(img_dir):
            os.makedirs(img_dir)

    def create_html_result_file(self):
        html_dir_path_raw = os.path.join(self.pmaps_dir, self.smapiservice.svcName + ".html")
        self.output_file_name = html_dir_path_raw.decode(UTF8)

    def write_results_file_header(self, writer, url_name):
        html_element = writer.start("html")
        body_element = writer.start("body")
        result_table = writer.start("table")
        tr = writer.start("tr")
        writer.element("th", self.smapiservice.svcName, attrib={"align":"left"})
        writer.element("th", url_name, attrib={"align":"right"})
        writer.close(tr)
        return (html_element, body_element, result_table)

    def write_results_file_images(self, writer, item, url_name):
        tr = writer.start("tr")
        td = writer.start("td")
        writer.element("img", attrib={"src":url_name})
        writer.element("p", item, "")
        writer.close(td)
        writer.close(tr)

    def write_results_file_closer(self, writer, html_element, body_element, result_table):
        writer.close(result_table)
        writer.close(body_element)
        writer.close(html_element)

    def create_name_for_substitute_url(self, substitution, url_name, default_image_size_value):
        substitute_url_name = url_name.replace(str(default_image_size_value), str(substitution))
        return substitute_url_name

    def create_name_for_local_image_file(self, substitution, file_path_name):
        if "jpg" in file_path_name:
            extention = '.jpg'
        elif "jpeg" in file_path_name:
            extention = '.jpeg'
        elif "png" in file_path_name:
            extention = '.png'
        elif "bmp" in file_path_name:
            extention = '.bmp'
        else:
            if "image" in self.image_type:
                extention = "." + self.image_type.split("/")[-1]
            else:
                self.stop('unknown file extension type')

        substitution = re.sub('[\n\t\r]', '', substitution)
        html_dir_path_raw = os.path.join(self.pmaps_dir, "img", self.smapiservice.svcName + "-" + substitution + extention)
        image_file_name = os.path.normpath(html_dir_path_raw)
        return image_file_name.decode(UTF8)

    def get_art_url(self):
        #"""
        #get the test content info from configuration file
        #Note: That in the Pre-Train WSDL albumArtURI is a simpleType meaning you could access the element like so
        #trackMetadata.albumArtURI and get the value directly.  However in train this has changed and the albumArtUri
        #is now a complexType.  Meaning we need to access a child attribute 'value' in order to get the actual value:
        #trackMetadata.albumArtURI.value
        #"""
        if self.smapiservice.get_test_track_id():
            track_metadata = self.client.getMediaMetadata(self.smapiservice.get_test_track_id(), self)
            self.verifyIsNotNoneOrStop(track_metadata,"getMediaMetadata should return something other than None.")
            if TRACKMETADATA in track_metadata:
                self.verifyInOrStop(ALBUMARTURI, track_metadata.trackMetadata, "trackMetadata should contain an "
                                                                               "albumArtURI inside. getMediaMetadata "
                                                                               "was called on ID = {0}."
                                    .format(self.smapiservice.get_test_track_id()))
                if type(track_metadata.trackMetadata.albumArtURI) is Text:
                    return track_metadata.trackMetadata.albumArtURI
                else:
                    require_auth = track_metadata.trackMetadata.albumArtURI._requiresAuthentication
                    self.requires_auth = require_auth if require_auth is not None else False
                    return track_metadata.trackMetadata.albumArtURI.value
            elif MEDIAMETADATA in track_metadata:
                self.verifyInOrStop(TRACKMETADATA,track_metadata.mediaMetadata, "mediaMetadata should contain "
                                                                                "trackMetadata inside. "
                                                                                "getMediaMetadata was called on "
                                                                                "ID = {0}."
                                    .format(self.smapiservice.get_test_track_id()))
                self.verifyInOrStop(ALBUMARTURI,track_metadata.mediaMetadata.trackMetadata, "trackMetadata should "
                                                                                            "contain an albumArtURI "
                                                                                            "inside. getMediaMetadata "
                                                                                            "was called on ID = {0}."
                                    .format(self.smapiservice.get_test_track_id()))
                if type(track_metadata.mediaMetadata.trackMetadata.albumArtURI) is Text:
                    return track_metadata.mediaMetadata.trackMetadata.albumArtURI
                else:
                    require_auth = track_metadata.mediaMetadata.trackMetadata.albumArtURI._requiresAuthentication
                    self.requires_auth = require_auth if require_auth is not None else False
                    return track_metadata.mediaMetadata.trackMetadata.albumArtURI.value
            else:
                self.fail("trackMetadata or mediaMetadata should be found in getMediaMetadataResponse.")

        elif self.smapiservice.get_test_prog_id():
            prog_id = self.smapiservice.get_test_prog_id()
            (prog_metadata, _) = self.client.getMetadata(prog_id, CR_STANDARD_RANGE.index, CR_STANDARD_RANGE.count,
                                                         self) #ignore @UndefinedVariable for named tuple
            self.verifyIsNotNoneOrStop(prog_metadata, "getMetadata should return something other than None.")
            self.verifyTrueOrStop(prog_metadata.count > 0, "getMetadata should return a non-empty list.")
            self.verifyInOrStop(TRACKMETADATA,prog_metadata.Items[0], "mediaMetadata should contain "
                                                                      "trackMetadata inside. "
                                                                      "getMetadata was called on ID = {0}."
                                .format(prog_id))
            self.verifyInOrStop(ALBUMARTURI,prog_metadata.Items[0].trackMetadata, "trackMetadata should contain an "
                                                                                 "albumArtURI inside. getMetadata "
                                                                                 "was called on ID = {0}."
                                .format(prog_id))
            if type(prog_metadata.Items[0].trackMetadata.albumArtURI) is Text:
                return prog_metadata.Items[0].trackMetadata.albumArtURI
            else:
                require_auth = prog_metadata.Items[0].trackMetadata.albumArtURI._requiresAuthentication
                self.requires_auth = require_auth if require_auth is not None else False
                return prog_metadata.Items[0].trackMetadata.albumArtURI.value

        elif self.smapiservice.get_test_stream_id():
            stream_metadata = self.client.getMediaMetadata(self.smapiservice.get_test_stream_id(), self)
            self.verifyIsNotNoneOrStop(stream_metadata,"getMediaMetadata should return something other than None.")
            self.verifyInOrStop(STREAMMETADATA, stream_metadata, "getMediaMetadata(stream) should return a response "
                                                                 "with {0} in it. getMediaMetadata was called on "
                                                                 "ID= {1}."
                                .format(STREAMMETADATA, self.smapiservice.get_test_stream_id()))
            self.verifyInOrStop(LOGO,stream_metadata.streamMetadata, "streamMetadata should contain a {0} inside.  "
                                                                     "getMediaMetadata was called on ID = {1}."
                                .format(LOGO, self.smapiservice.get_test_stream_id()))
            if type(stream_metadata.streamMetadata.logo) is Text:
                return stream_metadata.streamMetadata.logo
            else:
                require_auth = stream_metadata.streamMetadata.logo._requiresAuthentication
                self.requires_auth = require_auth if require_auth is not None else False
                return stream_metadata.streamMetadata.logo.value

        elif self.smapiservice.config.has_option("Test Content", "albumArtURI"):
            albumArtURI = self.smapiservice.config.get("Test Content", "albumArtURI")
            if albumArtURI != '':
                return albumArtURI

        return None

    def get_sample_custom_browse_icon_url(self, images_substitution_list):
        #"""
        #This function tries to find an image uri for container types that have the substitution pattern in the substitution list
        #"""
        (response, _) = self.client.getMetadata(ROOT, CR_STANDARD_RANGE.index, CR_STANDARD_RANGE.count, self)

        # exclude 'album' from the browseable container type
        # as we do not want to try and use custom browse icon substitutions on an album container
        container_types = [t for t in Validation.BROWSEABLE_CONTAINER_TYPES if t.lower() != 'album']

        # Find substitution key of the sizeEntry element that has size="0"
        size_0_sizeEntrys = self.xmlparse.getElementsByXPath(self.pmap_url,
                                                             ".//PresentationMap[@type='BrowseIconSizeMap']/Match/browseIconSizeMap/sizeEntry[@size='0']")
        if not size_0_sizeEntrys:
            return None
        else:
            substitution_str = size_0_sizeEntrys[0].attrib.get('substitution')
            if not substitution_str:
                return None

        # TODO: Need to drill down one level deeper if the target image url cannot be found on the root level containers
        for mediaColl in response.Items:
            if mediaColl.itemType in container_types:
                if substitution_str in mediaColl.albumArtURI:
                    return mediaColl.albumArtURI
                elif hasattr(mediaColl.albumArtURI,'value') and substitution_str in mediaColl.albumArtURI.value:
                    return  mediaColl.albumArtURI.value


        return None

    def inspect_art_url(self, url, require_auth=False):
        #"""see if url is valid and if we need redirect"""
        self.verifyIsNotNoneOrFailCase(url, "The URI ({0}) should be something other than None.".format(url))

        try:
            # the required authentication feature is only allowed for service with device link
            if require_auth:
                headers = {X_HOUSEHOLD_ID_HEADER: self.client.household_id,
                           X_AUTH_TOKEN: self.client.auth_token,
                           X_AUTH_KEY: self.client.private_key}
                request = urllib2.Request(url, None, headers)
            else:
                request = url

            image = urllib2.urlopen(request)
            self.verifyEqualOrFailCase(200, image.getcode(), "Attempting to open the target URI ({0}) should return an "
                                                             "HTTP 200 status.".format(url))
        except IOError, e:
            self.fail("Attempting to open a custom browse icon URI ({0}) returned an HTTP {1} code".format(url, e.code))
            return None
        except Exception, e:
            self.console_logger.error(e)
            self.fail("Attempting to open a custom browse icon URI ({0}) returned an error: {1}".format(url, str(e)))
            return None
        self.image_type = image.headers['content-type']
        return image

    def inspect_file_size(self, substitution_local_name, image_size_expected):
        if self.has_python_image_library():
            try:
                (width, height) = self.get_image_pixel_dimensions(substitution_local_name)
                self.verifyEqualOrFailCase(image_size_expected, str(width), "Image width ({0}) should match the "
                                                                            "expected width ({1})"
                                           .format(width, image_size_expected))
                self.verifyEqualOrFailCase(image_size_expected, str(height), "Image height ({0}) should match the "
                                                                             "expected height ({1})"
                                           .format(height, image_size_expected))
            except Exception, e:
                self.fail("Could not open image at {0}, Reason: {1}".format(substitution_local_name, e))
        else:
            self.image_lib_install_flag = 1

    def save_substitution_file_to_local_disc(self, image, substitution):
        #"""save the image url to a file locally"""
        substitution = re.sub('[\n\t\r]', '', substitution)
        try:
            urllib.urlretrieve(image.url, substitution)
        except urllib.ContentTooShortError, e:
            self.fail("Amount of the downloaded data is less than the expected amount: %i" % (e))

    def get_best_substitution_value(self, url_name, substitution_list):
        best_substitution_length = 0
        best_sub = None
        for sub in substitution_list:
            if sub in url_name:
                substitution_length = len(sub)
                if substitution_length > best_substitution_length:
                    best_substitution_length = substitution_length
                    best_sub = sub

        return best_sub

    def verify_image_url_substitution(self, sample_image_url, images_substitution_list, images_sizes_list, custom_browse=False):
        #"""
        #Required authentication will only be enabled if the following:
        #1) Service is using the device link authentication scheme
        #2) Service has returned "requiredAuthentication" attribute as part of the albumArtUri
        #3) This is a test for high-res album art and not custom browse
        #"""
        require_auth = True if self.requires_auth and self.device_link_enabled and not custom_browse else False

        #inspect the original image
        reference_image = self.inspect_art_url(sample_image_url, require_auth)
        if reference_image is None:
            self.stop("Could not successfully retrieve reference image.")
        #get the default image  size value
        defined_image_size_value = self.get_best_substitution_value(sample_image_url, images_substitution_list)

        #check if we have "one sizer"
        self.verifyIsNotNoneOrStop(defined_image_size_value, "The reference image size ({0}) in the url should be "
                                                             "found in the substitution list."
                                   .format(defined_image_size_value))

        #open html output file once
        with open (self.output_file_name, 'w') as result_file:
            writer = XMLWriter(result_file, encoding=UTF8)
            self.verifyTrueOrFailCase(writer, "Framework: XMLWriter constructor should produce a valid file handle to "
                                              "write to.")
            #write the header to results file
            (html_element, body_element, result_table) = self.write_results_file_header(writer, sample_image_url)
            #iterate trough the size maps and do the rest of work
            for item, dimension in zip(images_substitution_list, images_sizes_list):
                if custom_browse and dimension == '0':
                    log_msg = "Skipping size check for size 0 because the substitution can be anything"
                    self.logger.info(log_msg)
                    self.console_logger.info(log_msg)
                    continue
                #create a local name that has size in it
                substitution_local_name = self.create_name_for_local_image_file(str(dimension), sample_image_url)
                substitution_url_name = self.create_name_for_substitute_url(item, sample_image_url, defined_image_size_value)

                #inspect the image
                image = self.inspect_art_url(substitution_url_name, require_auth)
                if '.svg' not in substitution_url_name:
                    #save file to local directory
                    self.save_substitution_file_to_local_disc(image, substitution_local_name)

                    # explicitly close the opened url
                    image.close()

                    #put an art image in the file
                    self.write_results_file_images(writer, str(dimension), substitution_url_name)
                    if self.image_lib_install_flag == 0:
                        #inspect the file size
                        self.inspect_file_size(substitution_local_name, dimension)
                        #close html output file
                else:
                    log_msg = "{0} SVG images do not require size validation so skipping test." \
                        .format(substitution_url_name)
                    self.logger.info(log_msg)
                    self.console_logger.info(log_msg)

                    # Validate SVG file
                    svg_validator = SvgValidator(image, substitution_url_name)

                    well_formed, error_message = svg_validator.validate_well_formed()
                    self.verifyTrueOrStop(well_formed, error_message)

                    dtd_conform, error_message = svg_validator.validate_against_dtd()
                    self.verifyTrueOrFailCase(dtd_conform, error_message)

                    # explicitly close the opened url
                    image.close()

            self.write_results_file_closer(writer, html_element, body_element, result_table)

    def test_custom_browse_icon_schema(self):
        """
        This test case checks that the elements required by the custom browse icon feature are present in the
        presentation map.
        """
        if not self.pmap_url:
            self.skip("Service must provide a presentation map URI ([Presentation Map File]:pmapLocation in the config "
                      "file).")
        # <PresentationMap type="BrowseIconSizeMap"> is required
        xPath_string = ".//PresentationMap[@type='BrowseIconSizeMap']"
        target = self.xmlparse.getElementsByXPath(self.pmap_url, xPath_string)
        if not target:
            self.warn("Cannot find node with XPath {0} in Presentation Map".format(xPath_string))
            self.skip("Cannot find node with XPath {0} in Presentation Map".format(xPath_string))

        #  <PresentationMap type="BrowseIconSizeMap"><Match><browseIconSizeMap><sizeEntry> is required
        xPath_string = ".//PresentationMap[@type='BrowseIconSizeMap']/Match/browseIconSizeMap/sizeEntry"
        target = self.xmlparse.getElementsByXPath(self.pmap_url, xPath_string)
        self.verifyTrueOrFailCase(target, "Cannot find node with XPath {0} in presentation map file."
                                  .format(xPath_string))


    def test_custom_browse_icon_configuration(self):
        """
        This test case gets a sample container browse icon Url and verifies the Url substitution rules
        """
        if not self.pmap_url:
            self.skip("A presentation map url is required in configuration file to run this test.")
        #get substitution and size list
        (images_substitution_list, images_sizes_list) = self.xmlparse.parsePresentationMapSizeEntry(self.pmap_url, 'PresentationMap/Match/browseIconSizeMap/sizeEntry')
        self.verifyTrueOrSkip(images_substitution_list,"Image substitution rules for custom browse icons "
                                                       "should be found in the presentation map file.")

        #get a sample custom browse icon url
        sample_image_url = self.get_sample_custom_browse_icon_url(images_substitution_list)
        self.verifyIsNotNoneOrStop(sample_image_url, "Discovered custom browse icon URI should be something other "
                                                     "than None.")

        self.verify_image_url_substitution(sample_image_url, images_substitution_list, images_sizes_list, True)

    def test_token_inside_substitution(self):
        if not self.pmap_url:
            self.skip("A presentation map url is required in configuration file to run this test fixture")
        #get substitution list
        (images_substitution_list, _) = self.xmlparse.parsePresentationMapSizeEntry(self.pmap_url, 'PresentationMap/Match/imageSizeMap/sizeEntry')
        error_message = "Image substitution rules for album art should be found in the presentation map file."
        self.verifyTrueOrWarn(len(images_substitution_list) > 0, error_message)
        self.verifyTrueOrSkip(len(images_substitution_list) > 0, error_message)

        #get token list
        token_list = self.xmlparse.parsePresentationMapArtWorkToken(self.xmlparse.ingestXMLFile(self.pmap_url))
        token_count = len(token_list)
        overlap_list = list(set(images_substitution_list).intersection(set(token_list)))
        overlap_count = len(overlap_list)
        self.verifyEqualOrFailCase(token_count, overlap_count, "token values should be a part of substitution rules.")

    def test_hi_res_album_art_configuration(self):
        """
        This test case gets a sample albumart Url and verifies the Url substitution rules for hi resolution album art
        """
        if not self.pmap_url:
            self.skip("A presentation map is required in configuration file to run this test fixture")
        #get substitution and size list
        (images_substitution_list, images_sizes_list) = self.xmlparse.parsePresentationMapSizeEntry(self.pmap_url, 'PresentationMap/Match/imageSizeMap/sizeEntry')
        self.verifyTrueOrSkip(images_substitution_list, "Image substitution rules for album art should be found in the"
                                                       " presentation map file.")

        #get a sample hi-res album art url
        sample_image_url = self.get_art_url()
        self.verifyIsNotNoneOrStop(sample_image_url, "The hi-res albumArtURI ({0}) should be something other than None."
                                   .format(sample_image_url))

        self.verify_image_url_substitution(sample_image_url, images_substitution_list, images_sizes_list)

#Main
if __name__ == "__main__":

    parser = ServiceConfigOptionParser(sys.argv)

    suite = BasicWorkflowTestSuite("Presentation map Validation", args=parser.args)

    suite.smapiservice = SMAPIService(parser.config_file, parser.wsdl, getattr(parser.options, 'content_file'),
                                      suite.logger, suite.console_logger)
    suite.client = suite.smapiservice.buildservice()
    suite.client.login()

    f = Albumart(suite.client, suite.smapiservice)
    f.initialize()

    suite.run(f)