import selftestinit  #ignore @UnusedImport for packaging
import re
import sys
from sonos.workflow.basicsuite import BasicWorkflowTestSuite
from sonos.smapi.smapiservice import SMAPIService, TRACK, STREAM, PROGRAM
from sonos.smapi.smapiclient import CR_STANDARD_RANGE
from sonos.smapi.smapiexceptions import ItemNotFound
from utility import Validation
from utility import EasyXML
from utility import ServiceConfigOptionParser
from operator import itemgetter
from collections import Counter


def generate_pagination_container(determiner_pagination_container):
    for node in determiner_pagination_container:
        yield node

class Browse(Validation):
    """
    This class holds the tests that will check the browsability of the service. These tests
    will check that basic requested content is returned without error and any extra features are implemented correctly.
    """
    def __init__(self, client, smapiservice):
        super(Validation,self).__init__()
        self.client = client
        self.request_index = client.actual_CR_STANDARD_RANGE.index
        self.request_count = client.actual_CR_STANDARD_RANGE.count
        self.smapiservice = smapiservice
        self.max_fail_count = 5
        self.played_seconds = 10
        self.timeaccumulator = 0
        self.validated_display_types = []
        self.validated_tags = []

    def initialize(self):
        self.test_scroll_driller = self.generate_iterative_list_drill(self.determiner_browse_scroll)
        self.test_leaf_driller = self.generate_iterative_list_drill(self.determiner_browse_leaf)
        self.pagination_total_count = self.generate_iterative_list_drill(self.determiner_browse_pagination)
        self.pagination_container = generate_pagination_container(self.generate_iterative_list_drill(self.determiner_pagination_container))
        self.pagination_container_nooverlap = generate_pagination_container(self.generate_iterative_list_drill(self.determiner_pagination_container))

    def determiner_browse_leaf(self):
        #""" Args (type(s) of node to test,number of matching nodes to find and test)"""
        return (self.item_type_filter(self.LEAF_CONTAINER_TYPES), self.number_to_check(30))

    test_leaf_driller = None
    def test_combinatorial_test_browse_to_leaf(self,test_leaf_driller):
        """
        This test will navigate into several containers until it hits a leaf, then verify getMediaMetadata(leaf).
        Expected results: Standard browsing requests should not return SOAP faults or other errors.

        :param test_leaf_driller: this is a content driller used by the framework
        """
        self.verifyTrueOrSkip(test_leaf_driller, "Service must yield valid objects when browsing for this test to run.")

        if test_leaf_driller.itemType == TRACK:

            response = self.client.getMediaMetadata(test_leaf_driller.id, self)
            self.verifyIsNotNoneOrFailCase(response, "Requesting getMediaMetdata on a track ID should return something "
                                                     "other than None.")

        elif test_leaf_driller.itemType == PROGRAM:

            (response, _) = self.client.getMetadata(test_leaf_driller.id, self.request_index,
                                                    self.request_count,
                                                    self) #ignore @UndefinedVariable for named tuple
            self.verifyIsNotNoneOrFailCase(response, "Requesting getMetadata on a station ID should return something "
                                                     "other than None.")

        elif test_leaf_driller.itemType == STREAM:

            response = self.client.getMediaMetadata(test_leaf_driller.id, self)
            self.verifyIsNotNoneOrFailCase(response, "Requesting getMediaMetdata on a stream ID should return something"
                                                     " other than None.")

        self.verify_display_types(response)
        self.verify_tags(response)

    def determiner_browse_scroll(self):
        #""" Args (type(s) of node to test,number of matching nodes to find and test)"""
        return (self.item_type_filter(self.BROWSEABLE_CONTAINER_TYPES), self.number_to_check(100))

    test_scroll_driller = None
    def test_combinatorial_test_browse_scroll_indices(self,test_scroll_driller):
        """
        This test will thoroughly browse to ensure canScroll value & getScroll Indices are properly set.
        Expected result: Properly formatted Scroll Indices returned for containers that have canScroll == True

        :param test_scroll_driller: this is a content driller used by the framework
        """
        self.verifyTrueOrSkip(test_scroll_driller, "Service must yield valid objects when browsing for this test to "
                                                   "run.")
        self.verifyTrueOrSkip(hasattr(test_scroll_driller,'canScroll') and test_scroll_driller.canScroll,
                              "The returned container must support scrolling for this test to run.")

        scroll_index_temp = self.client.getScrollIndices(test_scroll_driller.id, self)
        self.verifyIsNotNoneOrStop(scroll_index_temp, "getScrollIndices should return something other than"
                                                     " None.")
        match = re.findall(r'(\s*[a-z]|[A-Z]\s*,\s*[0-9]+\s*)',scroll_index_temp)
        empty = re.search(r'(\s*[a-z]|[A-Z]\s*,\s*[0-9]+\s*)',scroll_index_temp)

        #if no results, response either empty or completely malformed
        self.verifyIsNotNoneOrFailCase(empty, "getScrollIndices should return [#,A-Z] alphabetically, with a "
                                              "non-decreasing sequence of indices.")

        last_alpha = '"' #This precedes '#' in ASCII/Unicode
        last_digi = 0 #Lowest index position can be 0

        for entry in map(lambda x:str(x).upper().split(','), match):
            if entry:
                if ord(entry[0]) not in [ord('#')] + range(ord('A'),ord('[')) or entry[0] <= last_alpha:
                    self.stop("getScrollIndices should contain characters [#,A-Z] in alphabetical order.")

                if int(entry[1]) < int(last_digi):
                    self.stop("getScrollIndices should return a non-decreasing sequence of indices.")

                last_alpha = entry[0]
                last_digi = entry[1]

        self.verify_display_types(test_scroll_driller)
        self.verify_tags(test_scroll_driller)

    def determiner_pagination_container(self):
        #""" Args (type(s) of node to test,number of matching nodes to find and test)"""
        return (self.item_type_filter(self.BROWSEABLE_CONTAINER_TYPES), self.number_to_check(10))

    pagination_container_nooverlap = None

    def test_combinatorial_pagination_browse_range_no_overlap(self, pagination_container_nooverlap):
        """
        This test will get request containers in non overlapped manner.
        Expected result: no repeats in consecutive responses

        :param test_leaf_driller: this is a content driller used by the framework
        """
        self.verifyTrueOrSkip(pagination_container_nooverlap, "Service must yield valid objects when browsing for this "
                                                              "test to run.")
        self.verifyFalseOrSkip(pagination_container_nooverlap.itemType == "playlist",
                               "Since duplicates are allowed in playlists, this test will not be run against playlists.")
        self.fail_count = 0
        response = None

        self.verify_display_types(pagination_container_nooverlap)
        self.verify_tags(pagination_container_nooverlap)

        #Get response for the first block of data with index and count specified in PM
        try:
            (response, _) = self.client.getMetadata(pagination_container_nooverlap.id, self.request_index,
                                                    self.request_count,
                                                    self)  # ignore @UndefinedVariable for named tuple
        except ItemNotFound, w:
            self.warn(w)
        self.verifyIsNotNoneOrStop(response, "getMetadata should return something other than None.")
        self.verify_display_types(response)
        self.verify_tags(response)
        self.verifyTrueOrSkip(response.count > 0 and len(response.Items) > 0,
                              "getMetadata should return a non-empty list.")

        duplicate_counter = Counter(item.id for item in response.Items)
        if duplicate_counter.most_common(1)[0][1] > 1:
            self.stop("A single page should not have duplicate tracks. The first duplicate found was {0}"
            .format(duplicate_counter.most_common(1)[0][0]))

        self.verifyTrueOrSkip(response.total > self.request_count,
                              "There should be more than one page in a container for testing whether duplicates exist "
                             "across non-overlapping ranges.")

        # Get the next block of data
        (response1, _) = self.client.getMetadata(pagination_container_nooverlap.id, self.request_count,
                                                 self.request_count,
                                                 self)  # ignore @UndefinedVariable for named tuple
        self.verifyIsNotNoneOrStop(response1, "getMetadata should return something other than None.")
        self.verify_display_types(response)
        self.verify_tags(response)
        self.verifyTrueOrSkip(response1.count > 0, "getMetadata should return a non-empty list.")

        duplicate_counter1 = Counter(item.id for item in response1.Items)
        if duplicate_counter1.most_common(1)[0][1] > 1:
            self.stop("A single page should not have duplicate tracks. The first duplicate found was {0}"
                .format(duplicate_counter1.most_common(1)[0][0]))

        duplicate_counter1.update(duplicate_counter)
        if duplicate_counter1.most_common(1)[0][1] > 1:
            self.stop("There should be no duplicates across non-overlapping ranges of objects returned by getmetadata."
                      " The first duplicate found was {0}".format(duplicate_counter1.most_common(1)[0][0]))

    pagination_container = None
    def test_combinatorial_pagination_browse_range_overlap(self, pagination_container):
        """
        This test will get request containers in overlapped manner.
        Expected result: overlapped requests produce the same responses

        :param pagination_container: this is a content driller used by the framework
        """
        """Get response for the root the get the total count"""
        self.verifyTrueOrSkip(self.client.strict, "Service must set strict WSDL enforcement ([Validation Settings]:"
                                                  "strict in the config file) for this test to run.")
        self.verifyTrueOrSkip(pagination_container, "Service must yield valid objects when browsing for this test to "
                                                    "run.")
        self.fail_count = 0
        response = None

        self.verify_display_types(pagination_container)
        self.verify_tags(pagination_container)
        try:
            (response,_) = self.client.getMetadata(pagination_container.id, self.request_index,
                                                   self.request_count * 2, self)
                                                   #ignore @UndefinedVariable for named tuple
            self.verifyIsNotNoneOrStop(response, "getMetadata should return something other than None.")
            self.verify_display_types(response)
            self.verify_tags(response)
            self.verifyTrueOrSkip(response.count > 0 and len(response.Items) > 0, "getMetadata should return a "
                                                                                        "non-empty list.")
        except ItemNotFound, w:
            self.warn(w)

        self.verifyIsNotNoneOrStop(response, "getMetadata should return something other than None.")
        self.verifyTrueOrSkip(response.total > self.request_count, "There should more than 1 page of items in a "
                                                                        "container for this test to run.")

        (response1, _) = self.client.getMetadata(pagination_container.id, self.request_count,
                                                 self.request_count,
                                                 self)#ignore @UndefinedVariable for named tuple
        self.verifyIsNotNoneOrStop(response1, "getMetadata should return something other than None.")
        self.verify_display_types(response)
        self.verify_tags(response)
        self.verifyTrueOrFailCase(response1.count > 0 and len(response1.Items) > 0, "getMetadata should return a "
                                                                                    "non-empty list.")
        self.verifyEqualOrFailCase(response1.index, self.request_count, "Response index should equal request "
                                                                             "index.")
        self.verifyLessEqualOrFailCase(response1.count, self.request_count, "Response count should be less than"
                                                                                 " or equal to the request count.")

        # Sort the two responses by 'id' before comparison; as the order is not important as long as the two responses have the same list of ids
        items_sorted = sorted(response.Items[self.request_count:], key = itemgetter("id"))
        items1_sorted = sorted(response1.Items[0:self.request_count], key = itemgetter("id"))
        for item, item1 in zip(items_sorted, items1_sorted):#ignore @UndefinedVariable for named tuple
            ret = self.verifyEqualOrFailCase(item.id, item1.id, "Requesting the same indexed item from overlapping "
                                                                "ranges should return the same Content ID.")
            if not ret:
                self.fail_count += 1
                self.verifyNotEqualOrStop(self.fail_count, self.max_fail_count, "Test should not exceed {0} failures."
                                          .format(self.max_fail_count))

    def determiner_browse_pagination(self):
        #""" Args (type(s) of node to test,number of matching nodes to find and test)"""
        return (self.item_type_filter(self.BROWSEABLE_CONTAINER_TYPES), self.number_to_check(5))

    pagination_total_count = None
    def test_combinatorial_browse_results_pagination(self, pagination_total_count):
        """
        This test will iterate through all BROWSEABLE_CONTAINER_TYPES types and verify:\n
        1. The total amount of returned data does not exceed the requested data.\n
         THEN\n
        2. On the last call if there are fewer items left than requested, returned data equals left over data

        **Reference:** BROWSEABLE_CONTAINER_TYPES = 'artist', 'album', 'genre', 'playlist', 'favorites', 'albumList', 'trackList', 'artistTrackList', 'container', 'favorite', 'collection', 'other', 'program'

        :param pagination_total_count: this is a content driller used by the framework
        """
        self.verifyTrueOrSkip(pagination_total_count, "Service must yield valid objects when browsing for this test to "
                                                      "run.")
        self.verify_display_types(pagination_total_count)
        self.verify_tags(pagination_total_count)
        self.fail_count = 0
        request_index = self.request_index # ignore @UndefinedVariable named tuple
        request_count = self.request_count # ignore @UndefinedVariable named tuple
        if self.client.pageSize is not None:
            request_count = self.client.pageSize
        browse_result = None
        try:
            (browse_result,_) = self.client.getMetadata(pagination_total_count.id, request_index, request_count, self)
        except ItemNotFound, w:
            self.warn(w)

        self.verifyIsNotNoneOrStop(browse_result, "getMetadata should return something other than None.")
        self.verify_display_types(browse_result)
        self.verify_tags(browse_result)
        self.verifyTrueOrSkip(browse_result.count > 0, "getMetadata should return a non-empty list.")

        grand_total = browse_result.total

        # Determine where the 5th page from the end begins, we'll be paginating through that
        num_pages = (grand_total + request_count - 1) / request_count;
        final_start_index = (num_pages - 5) * request_count
        if final_start_index < request_count * 5:
            final_start_index = request_count * 5

        for current_index, max_index in [[0, min(request_count * 5, grand_total)], [final_start_index, grand_total]]:
            while current_index < max_index:
                (browse_result, _) = self.client.getMetadata(pagination_total_count.id, current_index, request_count,
                                                             self)
                self.verifyIsNotNoneOrStop(browse_result, "getMetadata should return something other than"
                                                          " None.")
                self.verify_display_types(browse_result)
                self.verify_tags(browse_result)

                ret = self.verifyLessEqualOrFailCase(browse_result.count, request_count, "Result count should never "
                                                                                         "exceed request count.")
                ret = ret and self.verifyEqualOrFailCase(browse_result.index, current_index, "Result index should equal"
                                                                                             " request index.")
                if ret == False or browse_result.count == 0:
                    self.fail_count += 1
                    self.verifyNotEqualOrStop(self.fail_count, self.max_fail_count, "Test should not exceed {0} "
                                                                                    "failures."
                                              .format(self.max_fail_count))
                current_index += request_count

        self.verifyEqualOrFailCase(browse_result.index + browse_result.count, browse_result.total, "Index + count "
                                                                                                   "should equal total "
                                                                                                   "at the end of a "
                                                                                                   "container.")

    def verify_display_types(self, response):
        if (hasattr(response, "Items")):
            for item in response.Items:
                self.check_display_type(item)
        self.check_display_type(response)

    def check_display_type(self, object):
        if hasattr(object, "displayType") and object.displayType not in self.validated_display_types:
            self.validated_display_types.append(object.displayType)
            self.verifyTrueOrFailCase(EasyXML.xml_xpath_exists(self.smapiservice.get_pmap_file_location(),
                                                               "//DisplayType[@id='" + object.displayType + "']"),
                                      "displayType (" + object.displayType + ") returned in metadata should be listed "
                                                                             "in presentation map")
    def verify_tags(self, response):
        if (hasattr(response, "Items")):
            for item in response.Items:
                self.check_tags(item)
        self.check_tags(response)

    def check_tags(self, object):
        if hasattr(object, "tags"):
            if hasattr(object.tags, "explicit"):
                if object.tags.explicit != 0 and object.tags.explicit != 1:
                    self.fail("Invalid value for tags:explicit in mediaMetadata/Collection")
            if hasattr(object.tags, "premium"):
                if object.tags.premium != 0 and object.tags.premium != 1:
                    self.fail("Invalid value for tags:premium in mediaMetadata/Collection")

#Main
if __name__ == "__main__":

    parser = ServiceConfigOptionParser(sys.argv)

    suite = BasicWorkflowTestSuite("Browse Validation", args=parser.args)

    suite.smapiservice = SMAPIService(parser.config_file, parser.wsdl, getattr(parser.options, 'content_file'),
                                      suite.logger, suite.console_logger)
    suite.client = suite.smapiservice.buildservice()
    suite.client.login()

    f = Browse(suite.client, suite.smapiservice)
    f.initialize()

    suite.run(f)
