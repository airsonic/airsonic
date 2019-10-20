import selftestinit #ignore @UnusedImport for packaging
import sys
import os
import subprocess
import base64
import hashlib
import requests
import urllib2
import traceback
import re
from datetime import datetime
from sonos.workflow.basicsuite import BasicWorkflowTestSuite
from sonos.smapi.smapiservice import SMAPIService
from utility import Validation, ServiceConfigOptionParser
from urlparse import urlparse
from xml.etree import ElementTree
from sslyze import sslyze
from urlparse import urljoin
from suds import WebFault
from lxml import etree
from threading import Thread

sonos_validator = 'https://sslvalidator.sonos.com/v1/dcv'

dictionary_status_codes = {
    0:"OK",
    2:"ERR_UNABLE_TO_GET_ISSUER_CERT",
    3:"ERR_UNABLE_TO_GET_CRL",
    4:"ERR_UNABLE_TO_DECRYPT_CERT_SIGNATURE",
    5:"ERR_UNABLE_TO_DECRYPT_CRL_SIGNATURE",
    6:"ERR_UNABLE_TO_DECODE_ISSUER_PUBLIC_KEY",
    7:"ERR_CERT_SIGNATURE_FAILURE",
    8:"ERR_CRL_SIGNATURE_FAILURE",
    9:"ERR_CERT_NOT_YET_VALID",
    10:"ERR_CERT_HAS_EXPIRED",
    11:"ERR_CRL_NOT_YET_VALID",
    12:"ERR_CRL_HAS_EXPIRED",
    13:"ERR_ERROR_IN_CERT_NOT_BEFORE_FIELD",
    14:"ERR_ERROR_IN_CERT_NOT_AFTER_FIELD",
    15:"ERR_ERROR_IN_CRL_LAST_UPDATE_FIELD",
    16:"ERR_ERROR_IN_CRL_NEXT_UPDATE_FIELD",
    17:"ERR_OUT_OF_MEM",
    18:"ERR_DEPTH_ZERO_SELF_SIGNED_CERT",
    19:"ERR_SELF_SIGNED_CERT_IN_CHAIN",
    20:"ERR_UNABLE_TO_GET_ISSUER_CERT_LOCALLY",
    21:"ERR_UNABLE_TO_VERIFY_LEAF_SIGNATURE",
    22:"ERR_CERT_CHAIN_TOO_LONG",
    23:"ERR_CERT_REVOKED",
    24:"ERR_INVALID_CA",
    25:"ERR_PATH_LENGTH_EXCEEDED",
    26:"ERR_INVALID_PURPOSE",
    27:"ERR_CERT_UNTRUSTED",
    28:"ERR_CERT_REJECTED",
    29:"ERR_SUBJECT_ISSUER_MISMATCH",
    30:"ERR_AKID_SKID_MISMATCH",
    31:"ERR_AKID_ISSUER_SERIAL_MISMATCH",
    32:"ERR_KEYUSAGE_NO_CERTSIGN",
    33:"ERR_UNABLE_TO_GET_CRL_ISSUER",
    34:"ERR_UNHANDLED_CRITICAL_EXTENSION",
    35:"ERR_KEYUSAGE_NO_CRL_SIGN",
    36:"ERR_UNHANDLED_CRITICAL_CRL_EXTENSION",
    37:"ERR_INVALID_NON_CA",
    38:"ERR_PROXY_PATH_LENGTH_EXCEEDED",
    39:"ERR_KEYUSAGE_NO_DIGITAL_SIGNATURE",
    40:"ERR_PROXY_CERTIFICATES_NOT_ALLOWED",
    41:"ERR_INVALID_EXTENSION",
    42:"ERR_INVALID_POLICY_EXTENSION",
    43:"ERR_NO_EXPLICIT_POLICY",
    44:"ERR_UNNESTED_RESOURCE",
    50:"ERR_APPLICATION_VERIFICATION",
    101:"SONOS_NOT_IN_CACHE",
    102:"SONOS_SUBJECT_NAME_MISMATCH"
}

class ssl_validation(Validation):
    """
    This class holds the tests that verify partner server security
    """

    def __init__(self, smapiclient, smapiservice):
        super(Validation, self).__init__()
        self.smapiservice = smapiservice
        self.secure_domains_result_xml_path = {}
        self.scan_secure_urls_has_run = False
        self.use_secure_endpoint = False
        self.secure_url = None
        self.secure_domain = None
        self.client = smapiclient

    def initialize(self):
        pass

    def setUpFixture(self):
        self.get_secure_domains()
        #moved scan_secure_urls to each test as the external call will print sslyze info when this test is not run

    def get_secure_domains(self):
        self.secure_url = self.smapiservice.config.get('Endpoints', 'secure')
        if check_type_of_service(self.secure_url):
            if self.secure_url.startswith("https"):
                self.use_secure_endpoint = True
                self.secure_domain = urlparse(self.secure_url).netloc
                self.secure_domains_result_xml_path[self.secure_domain] = None
            self.get_secure_streamingURI()
            if self.smapiservice.get_test_stream_id() and self.smapiservice.get_HLSContent_setting():
                self.get_content_keyURI()


        # find albumart secure domain and if exist add to list
        # any more secure domain? Also find a mediaUri.
    def get_secure_streamingURI(self):
        media_ID_list = []
        if self.smapiservice.get_test_track_id():
            media_ID_list.append(self.smapiservice.get_test_track_id())
        if self.smapiservice.get_test_stream_id():
            media_ID_list.append(self.smapiservice.get_test_stream_id())
        for media_ID in media_ID_list:
            try:
                mediaURI_result = self.client.getMediaURI(media_ID, self)
                if(mediaURI_result and hasattr(mediaURI_result, 'getMediaURIResult')):
                    secure_mediaURI = str(mediaURI_result.getMediaURIResult)
                    if secure_mediaURI and secure_mediaURI.startswith("https"):
                        secure_media_domain = urlparse(secure_mediaURI).netloc
                        self.secure_domains_result_xml_path[secure_media_domain] = None
            except WebFault:
                self.warn("Service should return a valid mediaURI with a getMediaURI request for the object"
                                 " {0}".format(media_ID))




    def get_content_keyURI(self):
        """
        this is a function that gets the function key from streaming URI of hls service
        """
        streamId = str(self.smapiservice.get_test_stream_id())

        try:
            mediaURI = self.client.getMediaURI(streamId, self)
            if not (mediaURI and hasattr(mediaURI, 'getMediaURIResult')):
             self.fail("getMediaURI should return something other than None.")
             return

            variantURI = str(mediaURI.getMediaURIResult)

            #Grab contents of variant file
            try:
                variantContent = urllib2.urlopen(variantURI).read()
            except urllib2.URLError as e:
                self.fail("Attempting to open a mediaURI ({0}) returned an exception: {1}."
                          .format(variantURI,e))
                return

            if not variantContent:
                self.fail("Attempting to open a mediaURI ({0}) should return something other "
                                                           "than None.".format(variantURI))
                return

            for indexURI in str(variantContent).splitlines():  #iterate line-by-line to find the first data source
                if not indexURI.startswith('#'):
                    break

            indexFile = None
            try:
                indexFile = urllib2.urlopen(indexURI)
            except ValueError: # indexURI is relative
                abs_indexURI = urljoin(variantURI, indexURI)
            except urllib2.URLError as e:
                self.fail("URLError exception {0} is thrown when opening URL {1}".format(e.message, indexURI))
                return

            #Grab contents of index file
            if indexFile:
                indexContent = indexFile.read()
            else:
                try:
                    indexContent = urllib2.urlopen(abs_indexURI).read()
                except urllib2.URLError:
                    self.fail("Encountered problem fetching index URL resource.")
                    return

            if not indexContent:
                self.fail('Could not open Index URL')
                return

            # TODO: the following logic of getting contentKeyURI is based on SiriusXM and MLB index file format: "#EXT-X-KEY:METHOD=AES,URI=data:application/octet-stream;base64,0Nsco7MAgxowGvkUT8aYag"
            # TODO: other services's contentKeyURIs having different index file format may not be recorded
            # TODO: ONLY contentKeyURI that starts with https gets recorded into secure_domains_result_xml_path. Waiting for Keith to verify this.
            for contentKeyURI in str(indexContent).splitlines():
                if re.search("uri=", contentKeyURI, re.IGNORECASE):
                    regex = re.compile("uri=", flags=re.IGNORECASE)
                    contentKeyURI = regex.split(contentKeyURI)[1].replace('"', '')
                    if contentKeyURI.startswith('https'):
                        contentKeyURI = contentKeyURI.split(",")[0]
                        contentKeyURI_domain = urlparse(contentKeyURI).netloc
                        self.secure_domains_result_xml_path[contentKeyURI_domain] = None
                        break
        except WebFault:
            self.failIfNotCi("Service should return a valid mediaURI upon getMediaURI request for stream "
                             "{0}".format(streamId))



    def scan_secure_urls(self):
        try:
            command_template = " sslyze.py --tlsv1 --tlsv1_2 --reneg --certinfo=basic --http_get --hide_rejected_ciphers --sni=DOMAIN DOMAIN:443 --xml_out=PATH_OUTPUT_XML"

            dir_ssl = os.path.dirname(os.path.abspath(__file__))
            dir_result = os.path.join(os.path.abspath(dir_ssl), "result")
            if not os.path.exists(dir_result):
                os.makedirs(dir_result)


            for domain in self.secure_domains_result_xml_path:
                path_output_xml = os.path.join(dir_result, "{0}.xml".format(domain))

                self.secure_domains_result_xml_path[domain] = path_output_xml

                command = command_template.replace('DOMAIN', domain)

                command_list = command.split()
                path_index = command_list.index('--xml_out=PATH_OUTPUT_XML')
                command_list[path_index] = '--xml_out={0}'.format(path_output_xml)
                sslyze.external_call(command_list)

            domain_items = self.secure_domains_result_xml_path.items()

            for domain, result_xml_path in domain_items:
                xml_file_content = etree.parse(result_xml_path)
                xpath = "//results/target"
                if xml_file_content.find(xpath) is None:
                    self.failIfNotCi("SSLyze unable to scan the secure endpoint {0}.".format(domain))
                    del self.secure_domains_result_xml_path[domain]
        except Exception as e:
            traceback.print_exc()
            raise e

    def test_use_secure_endpoint(self):
        """
        This test verifies that the partner's secure end point is using https
        """
        if not self.use_secure_endpoint:
            self.failIfNotCi("The partner's secure endpoint should use https.")

    def test_support_tls_10(self):
        """
        This test verifies the partner's server supports TLS 1.0 and at least
        one of our required TLS 1.0 cipher suites.
        TLS 1.0 is a version of Transport Layer Security Protocol
        More information can be found at: http://tools.ietf.org/html/rfc4346
        """
        self.verifyTrueOrSkip(self.secure_domains_result_xml_path, "The partner should have at least one secure domain.")
        if not self.scan_secure_urls_has_run:
            self.scan_secure_urls()
            self.scan_secure_urls_has_run = True
        tls_10_accepted_xpath = ".//*/tlsv1/acceptedCipherSuites/cipherSuite"
        tls_10_ciphers_any = ('AES128-SHA', 'AES256-SHA')
        for domain, result_xml_path in self.secure_domains_result_xml_path.iteritems():
            try:
                result_xml_object = self.get_xml_content(result_xml_path)
                acceptedCipherSuites_nodes = result_xml_object.findall(tls_10_accepted_xpath)
                good_tls_10 = False
                for cipherSuite in acceptedCipherSuites_nodes:
                    if cipherSuite.attrib['name'] in tls_10_ciphers_any:
                        good_tls_10 = True
                        break
                self.verifyTrueOrFailCase(good_tls_10,
                                          "The partner domain {} must support at least one of the following TLS 1.0 cipher suites: {}".format(domain,
                                          ', '.join(tls_10_ciphers_any)))
            except ElementTree.ParseError as e:
                fail_message = "Exception {0} raised because the designated XML " \
                             "file ({1}) does not follow the XML schema." \
                            .format(e.code, result_xml_path)
                self.fail(fail_message)

    def test_support_tls_12(self):
        """
        This test verifies the partner's server supports TLS 1.2 and all
        of our required TLS 1.2 cipher suites.
        TLS 1.2 is a version of Transport Layer Security Protocol
        More information can be found at: http://tools.ietf.org/html/rfc5246
        """
        self.verifyTrueOrSkip(self.secure_domains_result_xml_path, "The partner should have at least one secure domain.")
        if not self.scan_secure_urls_has_run:
            self.scan_secure_urls()
            self.scan_secure_urls_has_run = True
        tls_12_accepted_xpath = ".//*/tlsv1_2/acceptedCipherSuites/cipherSuite"
        for domain, result_xml_path in self.secure_domains_result_xml_path.iteritems():
            try:
                result_xml_object = self.get_xml_content(result_xml_path)
                acceptedCipherSuites_nodes = result_xml_object.findall(tls_12_accepted_xpath)
                # they must support all of these, so we track whether we have
                # seen each one
                tls_12_ciphers_seen = {
                    'ECDHE-RSA-AES256-GCM-SHA384' : False,
                    'AES256-GCM-SHA384' : False,
                    'ECDHE-RSA-AES128-GCM-SHA256' : False,
                    'AES128-GCM-SHA256' : False,
                }
                for cipherSuite in acceptedCipherSuites_nodes:
                    if cipherSuite.attrib['name'] in tls_12_ciphers_seen:
                        tls_12_ciphers_seen[cipherSuite.attrib['name']] = True
                missing = [k for k in tls_12_ciphers_seen.keys() if not tls_12_ciphers_seen[k]]
                if missing:
                    missing.sort()
                    self.failIfNotCi("The partner domain {} is missing support for "
                              "all of the following TLS 1.2 "
                              "cipher suites: {}".format(domain,
                                                         ', '.join(missing)))
            except ElementTree.ParseError as e:
                fail_message = "Exception {0} raised because the designated XML " \
                             "file ({1}) does not follow the XML schema." \
                            .format(e.code, result_xml_path)
                self.fail(fail_message)

    def test_support_secure_renegotiation(self):
        """
        This test verifies the partner's server supports RFC 5746 (secure renegotiation)
        Secure renegotiation prevents attacker from injecting traffic of his own as a prefix
        to the intercepted client's interaction with the server.
        More information can be referenced at: http://tools.ietf.org/html/rfc5746
        """
        self.verifyTrueOrSkip(self.secure_domains_result_xml_path, "The partner should have at least one secure domain.")
        if not self.scan_secure_urls_has_run:
            self.scan_secure_urls()
            self.scan_secure_urls_has_run = True
        renegotiation_xpath = ".//*/sessionRenegotiation"
        renegotiation_node_xpath = ".//*/reneg"
        for domain, result_xml_path in self.secure_domains_result_xml_path.iteritems():
            try:
                result_xml_object = self.get_xml_content(result_xml_path)
                sessionRenegotiation_nodes = result_xml_object.findall(renegotiation_xpath)
                renegotiation_node = result_xml_object.findall(renegotiation_node_xpath)

                if len(renegotiation_node) == 1:
                    if len(sessionRenegotiation_nodes) == 1:
                        is_support_secure_renegotiation = sessionRenegotiation_nodes[0].get('isSecure').lower() == \
                                                          'true'

                        self.verifyTrueOrStop(is_support_secure_renegotiation,
                                "The attribute isSecure should be true, indicating that {0} supports secure session "
                                              "renegotiation".format(domain))
                        continue
                    elif renegotiation_node[0].get('exception') is not None:
                        self.failIfNotCi("There was an exception while scanning the domain ({0}) for secure session "
                                         "renegotiation: {1}"
                                         .format(domain, renegotiation_node[0].get('exception')))
                        continue

                self.stop("One sessionRenegotiation element should be returned, indicating that {0} supports secure "
                           "session renegotiation".format(domain))

            except ElementTree.ParseError as e:
                fail_message = "Exception {0} raised because the designated XML " \
                               "file ({1}) does not follow the XML schema." \
                    .format(e.code, result_xml_path)
                self.fail(fail_message)


    def test_DNS_has_valid_x509_certificate(self):
        """
        This test verifies the partner's server has a valid X.509 certificate that matches the DNS name
        More information about X.509 Public Key Infrastructure Certificate can be referenced at:
        http://tools.ietf.org/html/rfc5280#section-3.1
        """
        self.verifyTrueOrSkip(self.secure_domains_result_xml_path, "The partner should have at least one secure domain.")
        if not self.scan_secure_urls_has_run:
            self.scan_secure_urls()
            self.scan_secure_urls_has_run = True
        x509_certificate_xpath = ".//*/certificate"
        for domain, result_xml_path in self.secure_domains_result_xml_path.iteritems():
            try:
                result_xml_object = self.get_xml_content(result_xml_path)
                x509_certificate_node = result_xml_object.findall(x509_certificate_xpath)
                self.verifyTrueOrStop(len(x509_certificate_node) > 0, "A X.509_certificate element should be returned, "
                                    "indicating that {0} has a valid X.509 certificate for the DNS name".format(domain))
                self.verifyTrueOrStop(x509_certificate_node[0].get('hasMatchingHostname').lower() == 'true',
                             "The attribute hasMatchingHostname should be true, indicating that {0} has a valid X.509 "
                               "certificate for the DNS name".format(domain))
            except ElementTree.ParseError as e:
                fail_message = "Exception {0} raised because the designated XML " \
                              "file ({1}) does not follow the XML schema." \
                           .format(e.code, result_xml_path)
                self.fail(fail_message)


    def get_xml_content(self, result_xml_path):
        if os.path.exists(result_xml_path):
            with open(result_xml_path, 'r') as result_xml:
                result_xml_content = result_xml.read()
                result_xml.close()
            return ElementTree.fromstring(result_xml_content)
        else:
            fail_message = "File at {0} does not exist.".format(result_xml_path)
            self.stop(fail_message)

    def test_certificate_expiration(self):
        """
        This test verifies that the leaf certificate is not expired. Will warn if it will expire within 30 days.
        """
        self.verifyTrueOrSkip(self.secure_domains_result_xml_path, "The partner should have at least one secure domain.")
        if not self.scan_secure_urls_has_run:
            self.scan_secure_urls()
            self.scan_secure_urls_has_run = True
        date_time_xpath = ".//*/certificate/validity"
        for domain, result_xml_path in self.secure_domains_result_xml_path.iteritems():
            try:
                result_xml_object = self.get_xml_content(result_xml_path)
                date_not_before = result_xml_object.findall(date_time_xpath + '/notBefore')
                date_not_after = result_xml_object.findall(date_time_xpath + '/notAfter')
                if (not (date_not_after and date_not_after)):
                    self.fail("A notBefore and notAfter element should be returned, indicating that {0} has a date "
                              "range for validity.".format(domain))
                else:
                    date_nb = datetime.strptime(date_not_before[0].text.strip(), "%b %d %H:%M:%S %Y %Z")
                    date_na = datetime.strptime(date_not_after[0].text.strip(), "%b %d %H:%M:%S %Y %Z")
                    now = datetime.now()
                    delta = date_na - now
                    if 0 < delta.days <= 30:
                        self.fail("Certificate should not expire within the next 30 days. {0} days remaining!".format(delta.days))
                    elif delta.days <= 0:
                        self.fail("Certificate should not be expired.")
                    self.verifyTrueOrFailCase(date_nb < now , "Certificate validity date range should not be in the "
                                                              "future." )
            except ElementTree.ParseError as e:
                fail_message = "Exception {0} raised because the designated XML " \
                               "file ({1}) does not follow the XML schema." \
                    .format(e.code, result_xml_path)
                self.fail(fail_message)

    def communicate_call(self, p1, input_tuple):
        input_tuple.extend(p1.communicate('HEAD / HTTP/1.0\r\n\r\n'))

    def test_certificate_chain(self):
        """
        This test verifies that the entire certificate chain for the secure endpoint is valid.
        """
        self.verifyTrueOrSkip(self.secure_domains_result_xml_path, "The partner should have at least one secure domain.")
        if not self.scan_secure_urls_has_run:
            self.scan_secure_urls()
            self.scan_secure_urls_has_run = True

        for domain in self.secure_domains_result_xml_path:
            c1 = ['openssl', 's_client', '-servername', domain, '-showcerts', '-state', '-connect', '{0}:443'
                .format(domain)]
            p1 = subprocess.Popen(c1, stdin=subprocess.PIPE,
                                  stdout=subprocess.PIPE, stderr=subprocess.PIPE)

            try:
                import psutil
                pconns = psutil.Process(p1.pid).connections()
                if pconns:
                    printout = '\n\tConnections Opened for OpenSSL:\n'
                    for index, connection in enumerate(pconns):
                        printout += '\t\tConnection {0}: '.format(index + 1) + str(connection) + '\n'
                    self.console_logger.info(printout)

            except ImportError:
                pass

            cert_output = ""
            cert_error = ""
            input_list = []
            communicate_thread = Thread(target=ssl_validation.communicate_call, args=(self, p1, input_list))
            # Sets communicate_thread to be a Daemon thread so that it is stopped when the test finishes
            communicate_thread.daemon = True
            communicate_thread.start()
            # Blocks the calling thread until the communicate_thread terminates normally or until the timeout occurs
            communicate_thread.join(10)

            if communicate_thread.isAlive():
                self.console_logger.info("openSSL should be able to extract SSL certificate chain from the partner's "
                                         "secure server")
            else:
                cert_output = input_list[0]
                cert_error = input_list[1]

            exit = p1.returncode

            if exit != 0:
                self.warn("openSSL should be able to connect to the secure endpoint {0}.".format(domain))
                continue

            output = cert_output.splitlines()
            certificates = []
            current_cert = ''
            process = 0
            error_string = ''

            for line in output:
                if 'BEGIN CERTIFICATE' in line:
                    process = 1
                elif 'END CERTIFICATE' in line:
                    certificates.append(base64.b64decode(current_cert))
                    process = 0
                    current_cert = ''
                elif process == 1:
                    current_cert += line

            error_log = cert_error.splitlines()

            for line in error_log:
                if re.search('SSL3 alert read:warning:(?!close notify)', line):
                    error_string += '\t' + line + '\n'

            self.verifyFalseOrStop(error_string, "Requesting certificate chain should not return errors:\n{0}"
                                   .format(error_string))

            leaf = hashlib.sha1(certificates[0]).hexdigest()
            with open(leaf, 'w+') as f:
                intermediate_chain = ''.join(certificates)
                f.write(intermediate_chain)
                chain =  hashlib.sha1(intermediate_chain).hexdigest()

            with open(leaf, 'rb')as f:
                file = f.read()
                url = "{0}/{1}/{2}".format(sonos_validator, leaf, chain)
                request = requests.post(url, data=file, headers={'Content-Type': 'application/x-sonos-cert-stream',
                                                                 'Accept': 'application/x-sonos-cert-result'})

            os.remove(leaf)

            if request.content:
                result = bytearray(request.content)[:4]
                status = int(''.join('{:02x}'.format(x) for x in result), 16)
                self.verifyTrueOrFailCase(status == 0, "The SSL certificate chain should be valid. Received status "
                                                       "code {0}: {1}".format(status, dictionary_status_codes[status]))

    def failIfNotCi(self, message):
        self.warn(message) if self.validating_numerous_bit else self.fail(message)

def check_type_of_service(url):
    if os.path.isfile('service-catalog.xml'):
        service_catalog = open('service-catalog.xml', 'r')
        try:
            xml_file_content = etree.parse(service_catalog)
            xpath = "//*[@SecureUri='{0}']".format(url)
            if xml_file_content.find(xpath):
                return True
            else:
                return False
        except etree.XMLSyntaxError:
            pass

    return True

# Main
if __name__ == "__main__":
    parser = ServiceConfigOptionParser(sys.argv)
    suite = BasicWorkflowTestSuite("SSL Validation", args=parser.args)
    suite.smapiservice = SMAPIService(parser.config_file, parser.wsdl, getattr(parser.options, 'content_file'),
                                      suite.logger, suite.console_logger)
    suite.client = suite.smapiservice.buildservice()
    suite.client.login()
    f = ssl_validation(suite.client, suite.smapiservice)
    suite.run(f)
