import selftestinit #ignore @UnusedImport for packaging 
import sys
from sonos.workflow.basicsuite import BasicWorkflowTestSuite
from sonos.smapi.smapiservice import SMAPIService, PROGRAM, USERID
from sonos.smapi.smapiclient import CR_STANDARD_RANGE
from utility import Validation, ServiceConfigOptionParser
from collections import Counter

class Progvalidation(Validation):
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
        self.test_program_driller = self.generate_iterative_list_drill(self.determiner_program)
        self.test_pagination_total_count = self.generate_iterative_list_drill(self.determiner_browse_pagination)

    def determiner_program(self):
        #""" Args (type of node to test,number of matching nodes to find and test)"""
        return (self.item_type_filter(PROGRAM), self.number_to_check(5))

    test_program_driller = None
    def test_combinatorial_test_program_radio(self,test_program_driller):
        """
        This test will check for repeats across n pages.
        Expected result: each piece of data is unique, if not - warn

        :param test_program_driller: this is a content generator used by the framework
        """
        self.verify_service_support(test_program_driller, "Service must provide a valid program ID ([Test Content]:"
                                                          "program in the config file) for this test to run.")
        self.verifyTrueOrSkip(test_program_driller, "Service must yield valid objects when browsing for this test to "
                                                    "run.")

        # get all containers within
        (program_children,_) = self.client.getMetadata(test_program_driller.id, CR_STANDARD_RANGE.index,
                                                       CR_STANDARD_RANGE.count,
                                                       self) # ignore @UndefinedVariable named tuple
        self.verifyIsNotNoneOrFailCase(program_children, "getMetadata should return something other than None.")

        self.verifyTrueOrSkip(hasattr(program_children, 'Items') and program_children.Items,
                              "getMetadata should return a non-empty list of results for this test to run.")

        duplicate_counter = Counter(item.id for item in program_children.Items)
        """check if the data is unique"""
        if duplicate_counter.most_common(1)[0][1] > 1:
            self.warn("GetMetadata should return a list of unique objects. The first duplicate found was {0}"
                      .format(duplicate_counter.most_common(1)[0][0]))


    def determiner_browse_pagination(self):
        #""" Args (type of node to test,number of matching nodes to find and test)"""
        return (self.item_type_filter(PROGRAM), self.number_to_check(5))

    test_pagination_total_count = None
    def test_combinatorial_browse_results_pagination(self, test_pagination_total_count):
        """
        This test will iterate through program type and verify:\n
        1. the totals of requested and reported data are less or equal 'n' times in the row\n
        2. in case count comes back as 0 and total is not reached yet, retry

        :param test_pagination_total_count: this is a content generator used by the framework
        """
        self.verify_service_support(test_pagination_total_count, "Service must provide a valid program ID "
                                                                 "([Test Content]:program in the config file) for this"
                                                                 " test to run.")
        self.verifyTrueOrSkip(test_pagination_total_count, "Service must yield valid objects when browsing for this "
                                                           "test to run.")

        request_index = CR_STANDARD_RANGE.index#ignore @UndefinedVariable for named tuple
        request_count = CR_STANDARD_RANGE.count#ignore @UndefinedVariable for named tuple
        retry_count_max = 5

        for segment_index in range(0, 5):
            for retry_count in range(0, retry_count_max):
                (browse_result,_) = self.client.getMetadata(test_pagination_total_count.id, request_index,
                                                            request_count, self)
                self.verifyIsNotNoneOrFailCase(browse_result, "getMetadata should return something other than None.")
                if  browse_result.count > 0:
                    break
                    self.warn("getMetadata({0}) returned an empty list. {1} retries left."
                              .format(test_pagination_total_count.id, str(retry_count_max - retry_count - 1)))
            self.verifyNotEqualOrFailCase(retry_count, retry_count_max, "Test should not exceed {0} failures."
                                          .format(retry_count_max))
            self.verifyLessEqualOrFailCase(browse_result.count, browse_result.total, "getMetadata should return a "
                                                                                     "'count' value smaller than or "
                                                                                     "equal to the returned 'total' "
                                                                                     "value.")


#Main
if __name__ == "__main__":

    parser = ServiceConfigOptionParser(sys.argv)

    suite = BasicWorkflowTestSuite("Program Validation", args=parser.args)

    suite.smapiservice = SMAPIService(parser.config_file, parser.wsdl, getattr(parser.options, 'content_file'),
                                      suite.logger, suite.console_logger)
    suite.client = suite.smapiservice.buildservice()
    suite.client.login()

    f = Progvalidation(suite.client, suite.smapiservice)
    f.initialize()
    suite.run(f)