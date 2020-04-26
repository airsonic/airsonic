# -*- coding: utf-8 -*-
import selftestinit #ignore @UnusedImport for packaging
import os
import optparse
from sonos.workflow.fixture import WorkflowTestFixture
from sonos.smapi.smapiservice import SMAPIService, ROOT, TRACK, STREAM, PROGRAM, PLAYLIST, ALBUM, SHOW, OTHER, ARTIST, \
    GENRE, PLAYLIST, SEARCH, PROGRAM, FAVORITES, FAVORITE, COLLECTION, CONTAINER, ALBUM_LIST, TRACK_LIST, STREAM_LIST, \
    ARTIST_TRACK_LIST, AUDIOBOOK
from sonos.smapi.smapiclient import SMAPIClient, CR_STANDARD_RANGE, USER_AGENT
from sonos.smapi.smapiexceptions import ItemNotFound, SonosError
from lxml import etree, objectify
from urllib2 import urlopen
import urllib2
import re
import zipfile

class Validation(WorkflowTestFixture, SMAPIClient, SMAPIService):

    BROWSEABLE_CONTAINER_TYPES = (ARTIST, ALBUM, GENRE, PLAYLIST, SEARCH, PROGRAM, FAVORITES, FAVORITE,
                                  COLLECTION, CONTAINER, ALBUM_LIST, TRACK_LIST, STREAM_LIST, ARTIST_TRACK_LIST,
                                  OTHER)
    LEAF_CONTAINER_TYPES = (TRACK, STREAM, SHOW, AUDIOBOOK, OTHER)
    MAX_WITHOUT_YIELD = 2000    #Tolerance for how many containers to drill through without yielding.
    DEPTH_LIMIT = 4             #Will not drill deeper than this
    LIST_LIMIT = 10             #WIll only drill into this many containers per list
    current_item = ''
    current_depth = 0
    yield_count = 0
    service_supported = 0
    required_canPlay_canEnumerate = ['album','trackList','artistTrackList','playlist']

    def validate_for_all_tracks_node(self, currentItem):
        if currentItem.itemType in self.required_canPlay_canEnumerate:
            if (not(hasattr(currentItem, 'canPlay') and currentItem.canPlay == True) or
                (hasattr(currentItem, 'canEnumerate') and currentItem.canEnumerate != True)):
                self.warn(currentItem.itemType + " must have \'canEnumerate\' and \'canPlay\' set to"
                                                       " True in order to enable the \"All Tracks\" node "
                                                       "in the controller.")

    def generate_iterative_list_drill(self, checking_function):
        """
        Drills through the service menu. Each test which uses this function must implement a "determiner" function which resembles

        def determiner_moduleX(self):
            return (self.item_type_filter(type or tuple/list of types to yield), self.number_to_check(# of nodes to yield; 0 = no limit))

        This determiner function is passed in as the "checking_function" argument above. This driller will use the determiner function to
        determine whether or not to yield the node it is currently looking at.  This driller will halt only after yielding the number
        of nodes requested or the number of nodes checked since a node was last returned exceeds MAX_WITHOUT_YIELD.  Nodes containing the
        term "Similar" will not be drilled into as "Similar _____" containers may be cyclical.
        """

        tries_left = self.MAX_WITHOUT_YIELD
        node_id = ROOT
        self.yield_count = 0

        (children,_) = self.client.getMetadata(node_id, CR_STANDARD_RANGE.index, CR_STANDARD_RANGE.count,
                                               self) #ignore @UndefinedVariable for named tuple
        self.verifyIsNotNoneOrStop(children, "getMetdata should return something other than None.")

        stack = []
        for each in children.Items:
            stack.append((each,0))

        while(stack):
            (self.current_item,self.current_depth) = stack.pop()

            (we_yield, we_recur) = checking_function()

            if we_yield == 'Not supported':
                tries_left = 0
                yield 'Not supported'

            if we_yield and we_recur and we_yield != 'Not supported':
                self.yield_count += 1 #Increment Total Yielded to test function
                tries_left = self.MAX_WITHOUT_YIELD #Reset Tries
                self.validate_for_all_tracks_node(self.current_item)
                yield self.current_item
            if we_recur and self.item_type_filter(self.BROWSEABLE_CONTAINER_TYPES) and tries_left > 0 and we_yield != 'Not supported':
                try:
                    (children,_) = self.client.getMetadata(self.current_item.id, CR_STANDARD_RANGE.index,
                                                           CR_STANDARD_RANGE.count,
                                                           self) #ignore @UndefinedVariable for named tuple
                except ItemNotFound, w:
                    children = None
                    self.warn(w)

                #(children,warning) = self.client.getMetadata(self.current_item.id, CR_STANDARD_RANGE.index, CR_STANDARD_RANGE.count) #ignore @UndefinedVariable for named tuple

                if children:
                    for child in children.Items[0:self.LIST_LIMIT]:
                        if child and child.title:
                            if not ('canEnumerate' in child and child.canEnumerate == False) and "Similar" not in child.title and tries_left > 0 and self.current_depth <= self.DEPTH_LIMIT:
                                stack.append((child, self.current_depth + 1))
                                tries_left -= 1

                            self.current_item = child
                            (we_yield, we_recur) = checking_function()
                            if we_yield and we_recur and we_yield != 'Not supported':
                                self.yield_count += 1 #Increment Total Yielded to test function
                                tries_left = self.MAX_WITHOUT_YIELD #Reset Tries
                                self.validate_for_all_tracks_node(self.current_item)
                                yield self.current_item

    def generate_iterative_list_drill_depth_first(self, checking_function):
        """
        Drills through the service menu. Each test which uses this function must implement a "determiner"
        function which resembles

        def determiner_moduleX(self):
            return (self.item_type_filter(type or tuple/list of types to yield), self.number_to_check(# of nodes to
            yield; 0 = no limit))

        This determiner function is passed in as the "checking_function" argument above. This driller will use the
        determiner function to determine whether or not to yield the node it is currently looking at.  This driller will
        halt only after yielding the number of nodes requested or the number of nodes checked since a node was last
        returned exceeds MAX_WITHOUT_YIELD.  Nodes containing the term "Similar" will not be drilled into as
        "Similar _____" containers may be cyclical.
        """

        tries_left = self.MAX_WITHOUT_YIELD
        node_id = ROOT
        self.yield_count = 0

        (children,_) = self.client.getMetadata(node_id, CR_STANDARD_RANGE.index, CR_STANDARD_RANGE.count, self)
        #ignore @UndefinedVariable for named tuple
        self.verifyIsNotNoneOrStop(children, 'getMetdata should return something other than None.')

        stack = []
        if hasattr(children,'Items'):
            for child in children.Items:
                stack.append((child, 0))
                while stack:
                    (self.current_item, self.current_depth) = stack.pop()

                    (we_yield, we_recur) = checking_function()

                    if we_yield == 'Not supported':
                        tries_left = 0
                        yield 'Not supported'

                    if we_yield and we_recur and we_yield != 'Not supported':
                        self.yield_count += 1  # Increment Total Yielded to test function
                        tries_left = self.MAX_WITHOUT_YIELD  # Reset Tries
                        self.validate_for_all_tracks_node(self.current_item)
                        yield self.current_item
                    if we_recur and self.item_type_filter(self.BROWSEABLE_CONTAINER_TYPES) and tries_left > 0 and \
                                    we_yield != 'Not supported':
                        try:
                            (children, _) = self.client.getMetadata(self.current_item.id, CR_STANDARD_RANGE.index,
                                                                    CR_STANDARD_RANGE.count, self)
                            # ignore @UndefinedVariable for named tuple
                        except Exception, w:
                            children = None
                            self.console_logger.warn(w)

                        if children == None:
                            try:
                                (children, _) = self.client.getMediaMetadata(self.current_item.id, self)
                                # ignore @UndefinedVariable for named tuple
                            except Exception, w:
                                children = None
                                self.console_logger.warn(w)

                        # (children,warning) = self.client.getMetadata(self.current_item.id, CR_STANDARD_RANGE.index,
                        # CR_STANDARD_RANGE.count) #ignore @UndefinedVariable for named tuple
                        if hasattr(children,'Items'):
                            for child in children.Items[::-1]:
                                if child and child.title:
                                    stack.append((child, self.current_depth + 1))
                                    tries_left -= 1

    def generate_iterative_entire_list_drill_depth_first(self, checking_function):
        """
        Drills through the service menu. Each test which uses this function must implement a "determiner"
        function which resembles

        def determiner_moduleX(self):
            return (self.item_type_filter(type or tuple/list of types to yield), self.number_to_check(# of nodes to
            yield; 0 = no limit))

        This determiner function is passed in as the "checking_function" argument above. This driller will use the
        determiner function to determine whether or not to yield the node it is currently looking at.  This driller will
        halt only after yielding ALL the nodes. Nodes containing the term "Similar" will not be drilled into as
        "Similar _____" containers may be cyclical.
        """

        node_id = ROOT
        self.yield_count = 0

        (children,_) = self.client.getMetadataWithUserAgent(node_id, CR_STANDARD_RANGE.index, CR_STANDARD_RANGE.count, self, self.user_agent)
        self.verifyIsNotNoneOrStop(children, 'getMetdata should return something other than None.')

        stack = []
        if hasattr(children,'Items'):
            for child in children.Items:
                stack.append((child, 0))
                self.current_path = []
                while stack:
                    (self.current_item, self.current_depth) = stack.pop()

                    while self.current_depth < len(self.current_path):
                        self.current_path.pop()

                    # skipping Playing Soon containers as they don't have any real content besides schedules
                    if self.current_item.title == "Playing Soon":
                        continue

                    (we_yield, we_recur) = checking_function()

                    if we_yield == 'Not supported':
                        yield 'Not supported'

                    if we_yield and we_recur and we_yield != 'Not supported':
                        self.yield_count += 1  # Increment Total Yielded to test function
                        self.validate_for_all_tracks_node(self.current_item)

                        self.current_path_text = " > ".join(self.current_path)
                        yield self.current_path_text + " > " + self.current_item.title

                    if we_recur and self.item_type_filter(self.BROWSEABLE_CONTAINER_TYPES) and \
                                    we_yield != 'Not supported':
                        self.current_path.append(self.current_item.title)
                        index = 0
                        currentCount = 0
                        fetchMore = True
                        while fetchMore:
                            try:
                                (children, _) = self.client.getMetadataWithUserAgent(self.current_item.id, index,
                                                                        CR_STANDARD_RANGE.count, self, self.user_agent)
                                if hasattr(children, 'total'):
                                    currentCount = currentCount + children.count
                                    if (children.total - currentCount) > 0:
                                        index = currentCount
                                    else:
                                        fetchMore = False

                            except Exception, w:
                                children = None
                                self.current_path.pop()
                                self.console_logger.warn(w)

                            if hasattr(children,'Items') and len(children.Items) > 0:
                                for child in children.Items[::-1]:
                                    if child and child.title:
                                        stack.append((child, self.current_depth + 1))
                            else:
                                fetchMore = False


    def item_type_filter(self, check_type):
        """
        Check if current node type is supported and is in list of itemTypes to yield for testing.
        """
        self.service_supported = self.is_service_supported(check_type)
        if self.service_supported == 'Not supported':
            return 'Not supported'
        else:
            if not hasattr(self.current_item, 'itemType'):
                if not self.client.strict:
                    self.current_item.itemType = 'container'
            return len(self.current_item.itemType) and self.current_item.itemType in check_type

    def number_to_check(self, num_check):
        """
        Check if the number of nodes requested has been yielded. A parameter of 0 indicates no limit.
        """
        return ((num_check is 0) or (self.yield_count < num_check))

    def is_browseble_container_type(self, itemType):
        return itemType in self.BROWSEABLE_CONTAINER_TYPES

    def is_service_supported(self, serviceType):
        value = 1
        if serviceType is STREAM:
            value = len(self.smapiservice.get_test_stream_id())
        elif serviceType is TRACK:
            value = len(self.smapiservice.get_test_track_id())
        elif serviceType is PROGRAM:
            value = len(self.smapiservice.get_test_prog_id())

        if value is 0: return 'Not supported'
        else: return 'leaf'

    def verify_service_support(self, name, message):
        """ We have to verify twice because of the driller comeback"""
        if name == 'Not supported':
            self.skip(message)

class ServiceConfigOptionParser ():
    """
    A shared option parser that allows user to specify arguments on the command line. 
    Supports program entry from multiple points for more flexible run/debug.
    
    Example Usage:
        if __name__ == "__main__":
            
            import sys  # only import on main
            parser = ServiceConfigOptionParser(sys.argv)  # parse the options
            
            suite = WorkflowTestSuite("Authentication Test", setup_subnet=False, args=parser.args)  # pass the cleaned args on to the next arg parser
            
            smapiservice = SMAPIService(parser.config_file) # use the args
            
    Eclipse:
        Debug Configurations > Arguments > --config amazon.cfg
        Debug Configurations > Arguments > --config service_configs/amazon.cfg
    """

    def __init__(self, args):
        # setup
        # get to the smapi level from the current working directory level and than to configuration file location
        path_to_configs = os.path.abspath(os.path.join(os.path.dirname( __file__ ), '..','service_configs'))
        default_config_file = os.path.join(path_to_configs, 'smapiConfig.cfg')
        default_config_file = os.path.normpath(default_config_file)

        path_to_run_configs = os.path.abspath(os.path.join(os.path.dirname( __file__ ), '..','testrun_configs'))
        default_run_config_file = os.path.join(path_to_run_configs, 'runAll.cfg')
        default_run_config_file = os.path.normpath(default_run_config_file)

        self.parser = optparse.OptionParser()
        self.parser.add_option("--config", dest="config_file",
                               default = default_config_file,
                               help="Identify the service configuration to use.")

        self.parser.add_option("--runConfig", dest="run_config_file",
                               default = default_run_config_file,
                               help="Identify the run configuration to use.")

        self.parser.add_option("--junit", dest="junit_style_result",
                               action = "store_true",default=False,
                               help = "Output JUnit style result for CI.")

        self.parser.add_option("--wsdl", dest="other_wsdl",
                               default="Sonos.wsdl",
                               help="Identify the wsdl to use.")

        self.parser.add_option("--localMSLogo",dest="local_MSLogoxml",
                               default = "",
                               help = "define path to local mslogo.xml")

        self.parser.add_option("-w", "--writeContent", dest="content_file",
                                action = "store", type="int", default=-1,
                                help = "Write content from crawler to config file.")

        self.parser.add_option("--useragent", dest="user_agent",
                               default="",
                               help="Specify User Agent override")


        # parse
        (self.options, self.args) = self.parser.parse_args(args)

        # make available to tests
        self.config_file = self.options.config_file
        self.mslogo_file = self.options.local_MSLogoxml
        self.wsdl = self.options.other_wsdl
        self.user_agent = self.options.user_agent

        if self.options.junit_style_result:
            self.args.append("--junit")

        # append service_configs dir if necessary (reasonable effort)
        if "service_configs" not in self.config_file:
            self.config_file = os.path.join(path_to_configs, self.config_file)
            self.config_file =  os.path.normpath(self.config_file)

        if "testrun_configs" not in self.options.run_config_file:
            self.options.run_config_file = os.path.join(path_to_run_configs, self.options.run_config_file)
            self.options.run_config_file =  os.path.normpath(self.options.run_config_file)

        # can't make self.config_file contain both a default and an indication of whether a config file was passed (Single/dev mode)
        self.config_file_passed_by_user = "--config" in args

class SvgValidator():
    """
    Utility class to check:
    1. SVG file is 'well-formed' in the XML sense
    2. SVG file is valid against 'SVG 1.1 Basic' DTD defined here:
       http://www.w3.org/Graphics/SVG/1.1/DTD/svg11-basic.dtd (hosted in Sonos S3 bucket)
    """
    SVG_DTD = {'SVG 1.1 Basic': 'https://s3.amazonaws.com/sonos-selftest/Graphics/SVG/1.1/DTD/svg11-basic.dtd',
               'SVG 1.1': 'https://s3.amazonaws.com/sonos-selftest/Graphics/SVG/1.1/DTD/svg11.dtd'}

    def __init__(self, file, image_url, validating_dtd_name = 'SVG 1.1 Basic' ):
        if not self.SVG_DTD.has_key(validating_dtd_name):
            raise ValueError("{0} is not a supported SVG DTD name".format(validating_dtd_name))

        self.validating_dtd_name = validating_dtd_name
        self.target_file = file
        self.image_url = image_url
        self.dtd_errors = ''

        try:
            folder, dtd_file_name = self.download_all_dtd_files(self.SVG_DTD[validating_dtd_name])
            self.dtd_validator = etree.DTD(os.path.join('..', 'utilities', folder, dtd_file_name))
            for error in self.dtd_validator.error_log:
                self.dtd_errors += str(error) + '\n'

        except etree.DTDParseError as e:
            self.dtd_errors += e.message
            self.dtd_validator = None

        self.element_tree = None

    def validate_well_formed(self):
        error_message = None
        if self.element_tree is not None:
            return True, error_message

        try:
            self.element_tree = etree.parse(self.target_file)
            objectify.deannotate(self.element_tree.getroot(), cleanup_namespaces=True)
        except etree.ParseError as e:
            error_message = "{0} is NOT well-formed XML. Exception thrown when parsing XML: {1}".format(self.image_url, e)
        except:
            error_message = "{0} is NOT well-formed XML".format(self.image_url)

        return self.element_tree is not None, error_message

    def validate_against_dtd(self):
        dtd_conforming = False

        if self.element_tree is not None and self.dtd_validator is not None:
            dtd_conforming = self.dtd_validator.validate(self.element_tree)
            error_log_content = "\n"
            number_of_errors = 0
            for error in self.dtd_validator.error_log.filter_from_errors():
                number_of_errors += 1
                error_log_content += ("Error #{0}  - Messsage: ".format(number_of_errors) + error.message + ". Line: "
                                      + str(error.line) + " Column: " + str(error.column) + '\n')

            if not dtd_conforming:
                error_message = "{0} does NOT conform to DTD defined in {1}\nThe file contains the following errors " \
                                "({2}):{3}The file should conform".format(self.image_url, self.SVG_DTD[
                    self.validating_dtd_name], number_of_errors, error_log_content)

            else:
                error_message = None

        elif self.dtd_errors != '':
            error_message = "There were errors retrieving the DTD file:\n" + self.dtd_errors

        else:
            error_message = "There was an error retrieving the test image."

        return dtd_conforming, error_message

    def download_all_dtd_files(self, target_url):
        url_list = target_url.split('/')
        target_url = '/'.join(url_list[:-1])
        filename = 'CVS'

        local_path = os.path.join('..', 'utilities', filename + '.zip')

        if not os.path.exists(local_path):
            remotefile = urlopen(target_url+'/' + filename + '.zip')
            localfile = open(local_path, 'wb')
            localfile.write(remotefile.read())
            localfile.close()
            remotefile.close()
            zip_file = zipfile.ZipFile(local_path)
            zip_file.extractall(os.path.join('..', 'utilities', filename, ''))
        return filename, url_list[-1]


class EasyXML():
    """
    Utility class that simplifies xml parsing to single line functions:
    xml_xpath_findall - this function takes a URL and xpath as arguments, then returns a list of objects found
    xml_xpath_exists - this function takes a URL and xpath as arguments, then returns a boolean indicating if the
    xpath matched any objects
    """
    def __init__(self):
        pass

    @staticmethod
    def xml_xpath_findall(xml_url, xpath):
        hdr = {'User-Agent': USER_AGENT}
        req = urllib2.Request(xml_url, headers=hdr)
        xml_file = urllib2.urlopen(req)
        if not (399 >= xml_file.getcode() >= 200):
            raise Exception('Fetching an XML file should return an HTTP response code between 200 and 400.')
        xml_file_content = etree.parse(xml_file)
        return xml_file_content.findall(xpath)

    @staticmethod
    def xml_xpath_exists(xml_url, xpath):
        hdr = {'User-Agent': USER_AGENT}
        req = urllib2.Request(xml_url, headers=hdr)
        xml_file = urllib2.urlopen(req)
        if not (399 >= xml_file.getcode() >= 200):
            raise Exception('Fetching an XML file should return an HTTP response code between 200 and 400.')
        xml_file_content = etree.parse(xml_file)
        if xml_file_content.find(xpath):
            return True
        else:
            return False


class DynamicTestContent():
    """
    This utility class uses the test content under 'program', 'album', and 'playlist' to attempt to find a track
    """
    def __init__(self):
        pass

    @staticmethod
    def update_test_track(self):
        if self.verifyTrueOrFailCase(self.smapiservice.get_ephemeral_track_id(), "Service must indicate ephemeral IDs "
                                                                                 "([Additional Settings]: "
                                                                                 "ephemeralTrackId/ephemeralArtistId"
                                                                                 "/ephemeralAlbumId/ephemeralStreamId/"
                                                                                 "ephemeralProgramId in the config "
                                                                                 "file) for the update process to "
                                                                                 "run."):
            for current_item in [PROGRAM, PLAYLIST, ALBUM]:
                response = None
                if current_item == PROGRAM and self.smapiservice.get_test_prog_id():
                    response = self.client.getMetadata(self.smapiservice.get_test_prog_id(), 0, 100, self)
                elif current_item == PLAYLIST and self.smapiservice.get_test_playlist_id():
                    response = self.client.getMetadata(self.smapiservice.get_test_playlist_id(), 0, 100, self)
                elif current_item == ALBUM and self.smapiservice.get_test_album_id():
                    response = self.client.getMetadata(self.smapiservice.get_test_album_id(), 0, 100, self)
                if response is not None and len(response) > 0:
                    response = response[0]
                    if hasattr(response, 'Items') and len(response.Items) > 0:
                        response = response.Items[0]
                        if hasattr(response, 'title') and hasattr(response, 'id') and hasattr(response, 'itemType') \
                            and response.itemType == TRACK:
                            self.smapiservice.set_test_content(response.title, response.id, TRACK)
                            break
