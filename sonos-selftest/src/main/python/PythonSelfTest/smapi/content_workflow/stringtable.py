import selftestinit #ignore @UnusedImport for packaging 
import sys
from sonos.workflow.basicsuite import BasicWorkflowTestSuite
from sonos.workflow.fixture import WorkflowTestFixture
from sonos.smapi.smapiservice import SMAPIService
from sonos.smapi.smapiclient import SMAPIClient, USER_AGENT
from sonos.smapi.xmlparser import XMLParser
from utility import ServiceConfigOptionParser
from sonos.smapi.smapiservice import ROOT
import urllib2

full_language_list = ['en-US', 'da-DK', 'de-DE', 'es-ES', 'fr-FR', 'it-IT', 'ja-JP', 'nb-NO', 'nl-NL',
                      'pt-BR', 'sv-SE',' zh-CN']

def generate_language_list():
    yield 'en-US'
    yield 'da-DK'
    yield "de-DE"
    yield "fr-FR"
    yield "it-IT"
    yield "es-ES"
    yield "pt-BR"
    yield "sv-SE"
    yield "nl-NL"
    yield "nb-NO"
    yield "ja-JP"
    yield "zh-CN"


class Stringtable(WorkflowTestFixture, SMAPIClient, SMAPIService):
    """
    This class holds the tests that validate the structure and content of the partner's strings.xml
    """

    def __init__(self, client, smapiservice):
        self.fail_whole_suite = 0
        super(Stringtable,self).__init__()
        self.client = client
        self.smapiservice = smapiservice
        self.xmlparse = XMLParser()
        self.stringId = "ServicePromo"
        self.xsd_doc_name = 'stringtable.xsd'
        self.strings_file = self.smapiservice.get_strings_file_location()

        if self.strings_file:

            self.parsed_response = self.xmlparse.ingestXMLFile(self.strings_file)

            try:
                hdr = {'User-Agent': USER_AGENT}
                req = urllib2.Request(self.strings_file, headers=hdr)
                self.non_parsed_response = urllib2.urlopen(req)

            except urllib2.HTTPError, e:
                self.warn("There was a problem loading the strings.xml file at: {0}. The error was: {1}"
                    .format(self.strings_file, e))
                self.fail_whole_suite = 1
                self.non_parsed_response = None

            if self.non_parsed_response.getcode() != 200:
                self.warn("Fetching a strings file location URI should return an HTTP 200 response.")
                self.fail_whole_suite = 1

            if self.parsed_response is not None and self.non_parsed_response is not None:
                self.parse_language_list = self.xmlparse.getStringsLanguage(self.parsed_response)

            else:
                self.fail_whole_suite = 1

    def initialize(self):
        self.language = generate_language_list()

    def string_file_validation(self):
        self.verifyTrueOrWarn(len(self.strings_file) != 0, "Service must provide a strings file URI "
                                                           "([Strings File]:stringsLocation in the config file).")
        self.verifyTrueOrSkip(len(self.strings_file) != 0, "Service must provide a strings file URI "
                                                           "([Strings File]:stringsLocation in the config file).")
        self.verifyTrueOrStop(self.parsed_response is not None, "The strings file URI ({0}) should point to a valid "
                                                                "file.".format(self.strings_file))

    def test_against_xsd(self):
        """
        This test validates strings file for the service against xsd file
        """
        #verify that the lxml package is installed
        self.verifyFalseOrFailCase(self.fail_whole_suite, "The strings file URI ({0}) should point to a valid "
                                                          "file.".format(self.strings_file))
        try:
            from lxml import etree
        except ImportError:
            self.console_logger.info("You do not have lxml toolkit, please download from: https://pypi.python.org/pypi"
                                     "/lxml")
            return None

        #verify that service provided a strings file
        self.verifyTrueOrSkip(self.strings_file, "Service must provide a strings file URI "
                                                 "([Strings File]:stringsLocation in the config file).")

        try:
            xsd_doc = etree.parse(self.xsd_doc_name)
        except Exception, e:
            self.stop("Parsing the strings file ({0}) returned an exception: {1}.".format(self.strings_file, e))

        xml_doc = self.non_parsed_response

        xsd = etree.XMLSchema(xsd_doc)
        xml = etree.parse(xml_doc)
        validate_result = xsd.validate(xml)
        self.verifyTrueOrFailCase(validate_result, xsd.error_log)

    language = None

    def test_combinatorial_all_languages(self, language):
        """
        This test verifies that all languages are defined within the strings.xml file
        Absence of a language results in warning

        :param language: this is a content generator used by the framework
        """
        self.verifyFalseOrFailCase(self.fail_whole_suite, "The strings file URI ({0}) should point to a valid "
                                                          "file.".format(self.strings_file))
        self.string_file_validation()
        self.found = 0
        for parsed_language in self.parse_language_list:
            if language == parsed_language:
                self.found = 1
                break
        self.verifyTrueOrWarn(self.found == 1, "The strings file should contain an entry for the language {0}."
                              .format(language))

    def test_getmetadata_all_languages(self):
        """
        This test verifies that requesting getMetadata('Root') with different languages in the http header
        doesn't return any errors.
        """
        self.verifyFalseOrFailCase(self.fail_whole_suite, "The strings file URI ({0}) should point to a valid "
                                                          "file.".format(self.strings_file))
        self.string_file_validation()
        for language in full_language_list:
            self.client.transport.options.headers['Accept-Language'] = language
            try:
                (self.serviceroot,warning) = self.client.getMetadata(ROOT, '0', '100',
                                                                     self)
            except Exception:
                self.fail("getMetadata should not return an error if the {0} language is requested."
                          .format(language))

        self.client.transport.options.headers['Accept-Language'] = 'en-US'

    def test_unsupported_languages(self):
        """
        This test verifies that the language codes defined in the strings.xml are all supported
        by our controllers, anything that is not support will fail.
        """
        self.verifyFalseOrFailCase(self.fail_whole_suite, "The strings file URI ({0}) should point to a valid "
                                                          "file.".format(self.strings_file)
            .format(self.strings_file))
        self.string_file_validation()
        found_unsupported_languages = []
        for parsed_language in self.parse_language_list:
            supported_languages = generate_language_list()
            if parsed_language not in supported_languages:
                found_unsupported_languages.append(parsed_language)

        message_ending = ""
        if len(found_unsupported_languages) != 0:
            message_ending = " The following unsupported languages were found: " + \
                             ', '.join(found_unsupported_languages) + "."
        self.verifyTrueOrFailCase(len(found_unsupported_languages) == 0, "The strings file should only contain "
                                                                         "supported languages.{0}"
                                  .format(message_ending))

    def test_minimum_config(self):
        """
        This test verifies that minimum configuration is implemented within the strings.xml file. Minimum configuration
        is defined as having a Service Promo string for each language
        """
        self.verifyFalseOrFailCase(self.fail_whole_suite, "The strings file URI ({0}) should point to a valid "
                                                          "file.".format(self.strings_file)
            .format(self.strings_file))
        self.string_file_validation()
        self.count = 0
        strings_list =  self.xmlparse.parseStringsFile(self.parsed_response)
        amount_languages = len(self.parse_language_list)
        for stringID in strings_list:
            if stringID == self.stringId:
                self.count += 1
        self.verifyFalseOrSkip(self.count == 0, "The strings file must contain a 'ServicePromo' for this test to run.")
        self.verifyEqualOrFailCase(self.count, amount_languages, "The strings file should contain a 'ServicePromo' for "
                                                                 "each language listed.")

    def test_equal_language_configuration(self):
        """
        This test verifies that configurations are equal
        for all present languages
        """
        self.verifyFalseOrFailCase(self.fail_whole_suite, "The strings file URI ({0}) should point to a valid "
                                                          "file.".format(self.strings_file)
            .format(self.strings_file))
        self.string_file_validation()
        # Total amount of languages
        amount_languages = len(self.parse_language_list)

        # Calculate amount of elements for the first language
        first_strings_list =  self.xmlparse.parseStringsFileNode(self.parsed_response)
        first_group_strings_count = 0
        for _ in first_strings_list: # This should be replaced with first_group_strings_count=len(first_strings_list)
            first_group_strings_count += 1

        # Calculate total amount of elements
        strings_list = self.xmlparse.parseStringsFile(self.parsed_response)
        total_strings_count = 0
        for _ in strings_list: # This should be replaced with first_group_strings_count=len(first_strings_list)
            total_strings_count += 1

        # Divide totals to check if we get correct amount of languages
        amount_languages_calculated = total_strings_count/first_group_strings_count

        self.verifyTrueOrFailCase(amount_languages == amount_languages_calculated, "The strings file should contain "
                                                                                   "identical configurations for each "
                                                                                   "language listed.")

#Main
if __name__ == "__main__":

    parser = ServiceConfigOptionParser(sys.argv)

    suite = BasicWorkflowTestSuite("Stringtable Validation", args=parser.args)

    suite.smapiservice = SMAPIService(parser.config_file, parser.wsdl, getattr(parser.options, 'content_file'),
                                      suite.logger, suite.console_logger)
    suite.client = suite.smapiservice.buildservice()
    suite.client.login()

    f = Stringtable(suite.client, suite.smapiservice)
    f.initialize()

    suite.run(f)
