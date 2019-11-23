import selftestinit #ignore @UnusedImport for packaging 
import sys
from sonos.workflow.fixture import WorkflowTestFixture
from sonos.workflow.basicsuite import BasicWorkflowTestSuite
from sonos.smapi.smapiservice import SMAPIService, ROOT, SEARCH, ALBUM, ARTIST, PROGRAM, STREAM, TRACK, PLAYLIST, OTHER, USERID
from sonos.smapi.smapiclient import SMAPIClient, CR_STANDARD_RANGE, SEARCH_PHRASE
from sonos.smapi.xmlparser import XMLParser
from xml.dom.minidom import parse
from xml.etree.ElementTree import ElementTree as ET
from utility import ServiceConfigOptionParser
import urllib2

class Search(WorkflowTestFixture, SMAPIClient, SMAPIService):
    """
    These tests cover all permutations of search functions against all available search containers.
    """
    def __init__(self, client, smapiservice):
        super(Search,self).__init__()
        self.client = client
        self.smapiservice = smapiservice
        self.request_index = client.actual_CR_STANDARD_RANGE.index
        self.request_count = client.actual_CR_STANDARD_RANGE.count
        self.big_total = 1000
        self.max_fail_count = 5
        self.xsd_doc_name = 'presentationmap.xsd'
        self.xmlparse = XMLParser()
        self.search_type_exists = True
        self.search_categories_node_exists = True


    def initialize(self):
        self.incremental_input   = self.generate_search_containers_and_input_incremental()
        self.search_input           = self.generate_input_strings()
        self.function_input         = self.generate_function_name_strings()
        self.function_input_pag     = self.generate_pagination_names()
        self.item_type_input        = self.generate_item_type()
        self.pagination_range_input = self.generate_pagination_range()

    def generate_search_containers_and_input_incremental(self):
        search_containers = ["self.smapiservice.get_search_container_track()",
                             "self.smapiservice.get_search_container_artist()",
                             "self.smapiservice.get_search_container_album()",
                             "self.smapiservice.get_search_container_program()",
                             "self.smapiservice.get_search_container_stream()",
                             "self.smapiservice.get_search_container_playlist()",
                             "self.smapiservice.get_search_container_other()"]

        search_terms = ["self.smapiservice.get_search_term_track()",
                        "self.smapiservice.get_search_term_artist()",
                        "self.smapiservice.get_search_term_album()",
                        "self.smapiservice.get_search_term_program()",
                        "self.smapiservice.get_search_term_stream()",
                        "self.smapiservice.get_search_term_playlist()",
                        "self.smapiservice.get_search_term_other()" ]

        for search_container,search_term in zip(search_containers, search_terms):
            search_container = eval(search_container)
            search_term = eval(search_term)
            if len(search_container) != 0 and len(search_term) != 0:
                for letters in range(6, len(search_term)):
                    yield (search_container, search_term[:letters])
            else:
                yield(search_container, search_term)

    def generate_input_strings(self):
        yield "1234567890abcdefINVALID987654321"
        yield "911"
        yield "??"
        yield u'Los del R\355o'

    def generate_function_name_strings(self):
        yield ( "self.smapiservice.get_search_container_track()"   ,"self.smapiservice.get_search_term_track()"   )
        yield ( "self.smapiservice.get_search_container_artist()"  ,"self.smapiservice.get_search_term_artist()"  )
        yield ( "self.smapiservice.get_search_container_album()"   ,"self.smapiservice.get_search_term_album() "  )
        yield ( "self.smapiservice.get_search_container_program()" ,"self.smapiservice.get_search_term_program()" )
        yield ( "self.smapiservice.get_search_container_stream()"  ,"self.smapiservice.get_search_term_stream()"  )
        yield ( "self.smapiservice.get_search_container_playlist()","self.smapiservice.get_search_term_playlist()")
        yield ( "self.smapiservice.get_search_container_other()"   ,"self.smapiservice.get_search_term_other()"   )

    def generate_item_type(self):
        yield "itemType"
        yield "id"

    def generate_pagination_names(self):
        yield ( "self.smapiservice.get_search_container_track()"   ,TRACK   )
        yield ( "self.smapiservice.get_search_container_artist()"  ,ARTIST  )
        yield ( "self.smapiservice.get_search_container_album()"   ,ALBUM   )
        yield ( "self.smapiservice.get_search_container_program()" ,PROGRAM )
        yield ( "self.smapiservice.get_search_container_stream()"  ,STREAM  )
        yield ( "self.smapiservice.get_search_container_playlist()",PLAYLIST)
        yield ( "self.smapiservice.get_search_container_other()"   ,OTHER   )

    def generate_pagination_range(self):
        container_type = ["self.smapiservice.get_search_container_track()",
                          "self.smapiservice.get_search_container_artist()",
                          "self.smapiservice.get_search_container_album()",
                          "self.smapiservice.get_search_container_program()",
                          "self.smapiservice.get_search_container_stream()",
                          "self.smapiservice.get_search_container_playlist()",
                          "self.smapiservice.get_search_container_other()"]

        range_type = (-1, 10),(0, -1),(999999, 5),(0, 999999)#index/count

        for container in container_type:
            for range_value in range_type:
                yield (container, range_value)

    search_input = None
    def test_combinatorial_search_all_inputs(self, search_input):
        """
        This will test all available search containers with non-alphanumeric inputs to verify a webfault is not returned
        and that the returned results are not empty.
        Expected result: No webfaults

        :param search_input: this is a content generator used by the framework
        """
        self.verifyTrueOrSkip(self.smapiservice.get_support_search(), "Service must support search ([Capabilities]:"
                                                                      "search in the config file) for this test to "
                                                                      "run.")

        search_containers = self.smapiservice.get_all_search_containers()
        self.verifyTrueOrSkip(search_containers, "Service must provide valid search containers ([Search Containers]:"
                                                 "searchArtist/searchAlbum/searchTrack/searchStream/searchProgram/"
                                                 "searchPlaylist/searchOther in the config file) to run the test.")
        for search_container in search_containers:
            try:
                (search_result,_) = self.client.search(search_container, search_input, CR_STANDARD_RANGE.index,
                                                       CR_STANDARD_RANGE.count,
                                                       self) #ignore @UndefinedVariable for named tuple
            except Exception, e:
                self.stop("search({0}) returned an exception: {1}.".format(search_input.encode('utf-8'), e))

            self.verifyIsNotNoneOrWarn(search_result, "Search should return a non-empty list of results.")

    incremental_input = None
    def test_combinatorial_search_all_incremental_search(self, incremental_input):
        """
        This tests all available search containers using an existing search term and incrementally adding letters to it.
        This is to verify that no webfaults are returned, or when results are returned that no empty responses are received.
        Expected result: No webfaults and no empty containers.

        :param incremental_input: this is a content generator used by the framework
        """
        self.verifyTrueOrSkip(self.smapiservice.get_support_search(), "Service must support search ([Capabilities]:"
                                                                      "search in the config file) for this test to "
                                                                      "run.")

        (search_container, search_term) = incremental_input
        self.verifyTrueOrSkip(search_container, "Service must provide valid search containers ([Search Containers]:"
                                                "searchArtist/searchAlbum/searchTrack/searchStream/searchProgram/"
                                                "searchPlaylist/searchOther in the config file) to run the test.")
        self.verifyTrueOrSkip(search_term, "Service must provide search terms ([Test Content]:track title/album title"
                                           "/artist name/stream title/program title/playlist title/other title in the "
                                           "config file) for this test to run.")

        (search_result,_) = self.client.search(search_container, search_term, CR_STANDARD_RANGE.index,
                                               CR_STANDARD_RANGE.count, self) #ignore @UndefinedVariable for named tuple
        self.verify_tags(search_result)
        self.verifyIsNotNoneOrFailCase(search_result, "Search should return something other than None.")

    function_input = None
    def test_combinatorial_search_verify_results(self, function_input):
        """
        This test will process a search for a known test content from the config file against the correct search
        container and then verify that the test content shows up in the results list. A webfault returned will fail
        this test.\n
        Expected results: test content searched for should be present in results, no webfaults.

        :param function_input: this is a content generator used by the framework
        """
        self.verifyTrueOrSkip(self.smapiservice.get_support_search(), "Service must support search ([Capabilities]:"
                                                                      "search in the config file) for this test to "
                                                                      "run.")
        (container, term) = function_input
        container = eval(container)
        term = eval(term)

        self.verifyTrueOrSkip(term and container, "Service must provide valid search containers ([Search Containers]:"
                                                  "searchArtist/searchAlbum/searchTrack/searchStream/searchProgram/"
                                                  "searchPlaylist/searchOther in the config file) and valid search "
                                                  "terms ([Test Content]:track title/album title/artist name/stream "
                                                  "title/program title/playlist title/other title in the config file) "
                                                  "for this test to run.")

        (search_results,_) = self.client.search(container, term.decode('utf-8'), CR_STANDARD_RANGE.index, CR_STANDARD_RANGE.count,
                                                self) #ignore @UndefinedVariable for named tuple
        self.verifyIsNotNoneOrStop(search_results, "search should return something other than None.")

        if search_results.Items == []:
            self.fail("\"{0}\" could not be found, test content for \"{1}\" ([Test Content] in the config file) may "
                      "be invalid/outdated.".format(term, container))
            return

        found_term = False
        for result in search_results.Items:
            if result.title.encode('utf-8').find(term) != -1:
                found_term = True
                break

        self.verifyTrueOrFailCase(found_term, "search should return the search term in one of the result titles.")

    item_type_input = None
    def test_combinatorial_item_types(self, item_type_input):
        """
        This test will check that all itemTypes which return from a getMetadata request for 'search' are correct.
        Expected results: All mediaCollections returned should have an itemType of 'search'.

        :param item_type_input: this is a content generator used by the framework
        """
        self.verifyTrueOrSkip(self.smapiservice.get_support_search(), "Service must support search ([Capabilities]:"
                                                                      "search in the config file) for this test to "
                                                                      "run.")

        type_list = []
        (search_result,_) = self.client.getMetadata(SEARCH, CR_STANDARD_RANGE.index, CR_STANDARD_RANGE.count,
                                                    self) #ignore @UndefinedVariable for named tuple
        self.verifyIsNotNoneOrStop(search_result, "getMetadata should return something other than None.")

        for search_lines in search_result.Items:
            for item_type in search_lines:
                if item_type_input in item_type:
                    type_list.append(item_type[-1])
        if item_type_input == 'itemType':
            self.verifyTrueOrWarn(((len(set(type_list)) == 1) and SEARCH in type_list), "Search containers should have "
                                                                                        "their 'itemType' value equal "
                                                                                        "to '{0}'."
                                  .format(SEARCH))
        else:
            self.verifyTrueOrSkip(self.smapiservice.get_all_search_containers(), "Service must provide valid search "
                                                                                 "containers ([Search Containers]:"
                                                                                 "searchArtist/searchAlbum/searchTrack/"
                                                                                 "searchStream/searchProgram/"
                                                                                 "searchPlaylist/searchOther in the "
                                                                                 "config file) to run the test.")
            self.verifyListEqualOrFailCase(sorted(type_list), sorted(self.smapiservice.get_all_search_containers()),
                                           "getMetadata(search) should return the same containers as those listed in "
                                           "the config file ([Search Containers]:searchArtist/searchAlbum/searchTrack/"
                                           "searchStream/searchProgram/searchPlaylist/searchOther).")

    function_input_pag = None
    def test_combinatorial_search_results_pagination(self, function_input_pag):
        """
        This test will iterate through all types of search containers and verify:\n
        1. The amount of pages and the reported amount are the same\n
        2. On the last call if there are less data left than requested, 
        returned data equals left over data

        :param function_input_pag: this is a content generator used by the framework
        """
        ret = True
        self.fail_count = 0
        (container, name) = function_input_pag
        # convert string from generator into function call
        container = eval(container)

        self.verifyTrueOrSkip(self.smapiservice.get_support_search(), "Service must support search ([Capabilities]:"
                                                                      "search in the config file) for this test to "
                                                                      "run.")
        self.verifyTrueOrSkip(container, "Service must provide valid search containers ([Search Containers]:"
                                         "searchArtist/searchAlbum/searchTrack/searchStream/searchProgram/"
                                         "searchPlaylist/searchOther in the config file) to run the test.")

        # call to get to the container from the generator list and pay attention to the total
        (search_result,_) = self.client.search(container, SEARCH_PHRASE, self.request_index, self.request_count, self)
        self.verifyIsNotNoneOrStop(search_result, "search should return something other than None.")
        self.verifyTrueOrSkip(search_result.total > search_result.count, "search must return multiple pages of results "
                                                                   " for this test to run.")

        request_total = search_result.total
        # to avoid falling asleep if the tree is really long look at 4% of data only
        request_index = request_total*96/100
        response_total = request_index
        page_size = search_result.count
        while request_total > request_index:
            (search_result,warning) = self.client.search(container, SEARCH_PHRASE, request_index, self.request_count,
                                                         self)

            if request_total - request_index > search_result.count and search_result.count > 0:
                ret = self.verifyTrueOrFailCase((request_index == search_result.index and
                                                 self.request_count >= search_result.count),
                                                 "search should return an 'index' value equal to the requested 'index' "
                                                 "and a 'count' value smaller than or equal to the requested 'count'")

                self.verifyTrueOrFailCase(page_size == search_result.count, "search should return a consistent page "
                                                                            "size if searching on a non-final page")

            if warning is not None:
                self.warn("search(id: {0}; term: {1}; index: {2}; count: {3}) returned a warning: {4}."
                          .format(name, SEARCH_PHRASE, request_index, self.request_count, warning))
            if ret == False or search_result.count == 0:
                self.fail_count += 1
                self.verifyNotEqualOrStop(self.fail_count, self.max_fail_count, "Test should not exceed {0} failures."
                                          .format(self.max_fail_count))

            request_index += search_result.count
            response_total += search_result.count
        # Check the last data set and verify we received leftovers and not the whole set
        self.verifyTrueOrWarn((request_index - search_result.count) == search_result.index
                                                                        and search_result.count <= page_size,
                                  "search should return an 'index' value equal to the requested 'index' value and a "
                                  "'count' value smaller than or equal to the requested 'count' when called on the "
                                  "final page of results.")
        # Check that totals match
        self.verifyEqualOrFailCase(request_total,response_total, "A series of search requests should return an initial "
                                                                 "'index' value and a number of 'count' values which "
                                                                 "sum to equal the 'total' value returned in every "
                                                                 "call.")

    pagination_range_input = None
    def test_combinatorial_search_invalid_range_pagination(self, pagination_range_input):
        """
        This test will iterate through all types of search containers and 
        faulty ranges to verify that we have a proper response

        :param pagination_range_input: this is a content generator used by the framework
        """
        (container, range_value) = pagination_range_input
        invalid_request_index = range_value[0]
        invalid_request_count = range_value[1]
        """convert string from generator into function call"""
        container = eval(container)

        self.verifyTrueOrSkip(self.smapiservice.get_support_search(), "Service must support search ([Capabilities]:"
                                                                      "search in the config file) for this test to "
                                                                      "run.")
        self.verifyTrueOrSkip(container, "Service must provide valid search containers ([Search Containers]:"
                                         "searchArtist/searchAlbum/searchTrack/searchStream/searchProgram/"
                                         "searchPlaylist/searchOther in the config file) to run the test.")

        try:
            (search_result,_) = self.client.search(container, SEARCH_PHRASE, invalid_request_index,
                                                   invalid_request_count, self)
        except Exception, e: # try to avoid catching Exception unless you have too
            self.skip("search with an invalid 'index' and/or 'count' must return a valid response for this test to "
                      "run.")

        self.verifyIsNotNoneOrStop(search_result, "search should return something other than None.")

    def test_train_ux_search_configuration(self):
        """
        This test will validate the presentationmap.xml file contains
        required nodes for train UX search, if the service supports search.
        """
        # verify that the lxml package is installed
        try:
            from lxml import etree
        except ImportError:
            self.console_logger.info("lxml must be installed for this test to run. (http://pypi.python.org/pypi/lxml)")
            self.skip("lxml must be installed for this test to run. (pypi.python.org/pypi/lxml)")

        # verify the service supports search and that a presentation map file URL is supplied in the config file
        self.verifyTrueOrSkip(self.smapiservice.get_support_search(), "Service must support search ([Capabilities]:"
                                                                      "search in the config file) for this test to "
                                                                      "run.")
        self.verifyIsNotNoneOrFailCase(self.smapiservice.get_pmap_file_location(), "Service must provide a presentation"
                                                                                   " map URI ([Presentation Map File]:"
                                                                                   "pmapLocation in the config file) "
                                                                                   "for this test to run.")

        # verify if the link provided in config file is a valid link
        try:
            url = self.smapiservice.get_pmap_file_location()
            xml_doc = urllib2.urlopen(url)
            self.verifyInRangeOrFailCase(xml_doc.getcode(), 200, 399, "Attempting to open the presentation map URI "
                                                                      "({0}) should return an HTTP status between 200 "
                                                                      "and 399.".format(url))
        except urllib2.HTTPError as e:
            self.fail("Attempting to open the presentation map URI ({0}) returned an HTTP error code of {1}: {2}."
                      .format(self.pmap_file, e.code(), e))
            self.verifyFalseOrStop('Error reading file: %s', e)
        except urllib2.URLError as e:
            self.stop("Attempting to open the presentation map URI ({0}) returned a URI error: {1}."
                      .format(self.pmap_file, e))

        # verify the presentation map file conforms to the xsd
        try:
            xsd_doc = etree.parse(self.xsd_doc_name)
        except Exception, e:
            self.stop("Parsing the presentation map ({0}) returned an exception: {1}.".format(url, e))

        xsd = etree.XMLSchema(xsd_doc)
        xml = etree.parse(xml_doc)
        validate_result = xsd.validate(xml)
        xml_doc.close()
        self.verifyTrueOrWarn(validate_result, xsd.error_log)

        try:
            pmap_root = xml.getroot()
            # verify a PresentationMap node with type == "Search" is present or fail
            if not pmap_root.findall(".//PresentationMap[@type='Search']"):
                self.search_type_exists = False
            self.verifyTrueOrFailCase(self.search_type_exists, "The presentation map file ({0}) should contain a "
                                                               "'PresentationMap' node with type equal to "
                                                               "'Search'.".format(url))

            # verify the SearchCategories node is found within the PresentationMap/type=='Search' node
            if pmap_root.findall(".//PresentationMap[@type='Search']"):
                if not pmap_root.findall("./PresentationMap[@type='Search']/Match/SearchCategories"):
                    self.search_categories_node_exists = False
            self.verifyTrueOrFailCase(self.search_categories_node_exists, "'PresentationMap' nodes with type equal to"
                                                                          " 'Search'  should contain at least one "
                                                                          "'Match' node with a 'SearchCategories' "
                                                                          "node inside.")

            # verify the IDs returned in a getMetadata(search) call match those found in the presentation map file
            get_metadata_search_result = self.client.getMetadata('search', CR_STANDARD_RANGE.index,
                                                                 CR_STANDARD_RANGE.count, self)
            self.verifyIsNotNoneOrStop(get_metadata_search_result, "getMetadata(search) should return something other "
                                                                   "than None.")
            getmetadata_search_id_list = []
            pmap_search_category_id_list = []
            for item in get_metadata_search_result[0].Items:
                getmetadata_search_id_list.append(item.id)
            for pmap_search_categories in pmap_root.findall(".//PresentationMap[@type='Search']/Match/SearchCategories/Category"):
                # mappedId is an optional attribute if the search id the partner used is in the following list of
                # canonical IDs: artists, albums, tracks, genres, composers, stations, playlists, podcasts, people,
                # hosts, tags

                # The test below checks to see if mappedId is present, if it is, it will use that id to compare to
                # the getMetadata(search) results.  Else it will use the id attribute value instead.
                if pmap_search_categories.get('mappedId'):
                    pmap_search_category_id_list.append(pmap_search_categories.get('mappedId'))
                else:
                    pmap_search_category_id_list.append(pmap_search_categories.get('id'))
            #if there are custom categories make sure to append them to the list too
            if pmap_root.findall(".//PresentationMap[@type='Search']/Match/SearchCategories/CustomCategory"):
                for pmap_search_categories in pmap_root.findall(".//PresentationMap[@type='Search']/Match/SearchCategories/CustomCategory"):
                    if pmap_search_categories.get('mappedId'):
                        pmap_search_category_id_list.append(pmap_search_categories.get('mappedId'))
                    else:
                        #verify the mappedId is present in the CustomCategory node or fail
                        self.fail("mappedId is a required attribute of a CustomCategory node")
            for id in getmetadata_search_id_list:
                self.verifyInOrWarn(id, pmap_search_category_id_list, "Each ID returned by getMetadata(search) should "
                                                                      "have a corresponding entry in the presentation "
                                                                      "map file ({0}) under the 'PresentationMap' node"
                                                                      " with type equal to 'Search'.".format(url))

            # verify if a partner uses a custom search mappedId the matching string Id is in the strings.xml file
            #check to see if the presentation map has a CustomCategory node.  If it does continue with the verification
            if pmap_root.findall(".//PresentationMap[@type='Search']/Match/SearchCategories/CustomCategory"):
                #grab all of the custom string IDs from the presentation map and put them in a list
                pmap_custom_string_ids_list = []
                string_ids_list = []
                for custom_string_ids in pmap_root.findall(".//PresentationMap[@type='Search']/Match/SearchCategories/CustomCategory"):
                    pmap_custom_string_ids_list.append(custom_string_ids.get('stringId'))

                    #grab the strings.xml file and parse
                strings_file = self.smapiservice.get_strings_file_location()
                parsed_strings_response = self.xmlparse.ingestXMLFile(strings_file)
                string_ids_list = self.xmlparse.parseStringsFile(parsed_strings_response)

                #now loop through each of the custom category string IDs and see if they are in the string IDs list
                for string_id in pmap_custom_string_ids_list:
                    self.verifyInOrWarn(string_id, string_ids_list, "Each stringId found in the 'CustomCategory' "
                                                                    "node of the presentation map file ({0}) should "
                                                                    "have a valid mapping in the strings file ({1})."
                                        .format(url, strings_file))

        except Exception, e:
            self.console_logger.error(e)
            self.logger.error(e)

    def verify_tags(self, response):
        if (hasattr(response, "Items")):
            for item in response.Items:
                self.check_tags(item)
        self.check_tags(response)

    def check_tags(self, object):
        if hasattr(object, "tags"):
            if hasattr(object.tags, "explicit"):
                if object.tags.explicit.value != 0 and object.tags.explicit.value != 1:
                    self.fail("Invalid value for tags:explicit in mediaMetadata/Collection")
            if hasattr(object.tags, "premium"):
                if object.tags.premium.value != 0 and object.tags.premium.value != 1:
                    self.fail("Invalid value for tags:premium in mediaMetadata/Collection")
#Main
if __name__ == "__main__":

    parser = ServiceConfigOptionParser(sys.argv)

    suite = BasicWorkflowTestSuite("SMAPI Search Validation", args=parser.args)

    suite.smapiservice = SMAPIService(parser.config_file, parser.wsdl, getattr(parser.options, 'content_file'),
                                      suite.logger, suite.console_logger)
    suite.client = suite.smapiservice.buildservice()
    suite.client.login()

    f = Search(suite.client, suite.smapiservice)
    f.initialize()

    suite.run(f)
