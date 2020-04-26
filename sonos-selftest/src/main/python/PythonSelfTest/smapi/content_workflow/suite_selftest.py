"""
Self Test Suite

This script runs all tests needed to verify a SMAPI implementation.

Execution:
suite_selftest.py

"""
import selftestinit #ignore @UnusedImport for packaging
import sys
from os import path, access, R_OK
from utility import ServiceConfigOptionParser
from unittest2.util import safe_str

from sonos.workflow.basicsuite import BasicWorkflowTestSuite
from sonos.smapi.smapiservice import SMAPIService

import smapimethodvalidation
import favorites
import ratings
import authentication
import browse
import httpverifications
import search
import progvalidation
import streamvalidation
import getlastupdate
import stringtable
import albumart
import playlist
import presentationmap
import extendedmetadatavalidations
import smapi_reporting
import updatetestcontent
import ssl_validation
import getuserinfo
#Main

def nightly_mode (config_file):

    ''' Check if the partners list exist'''
    configcount = 0
    try:
        f = open('ServiceList.txt')
        configlist = [path.join("service_configs", configlist.strip()) for configlist in open('ServiceList.txt')]
        configcount = configlist.__len__()
        f.close()
    except IOError:
        ''' If ServiceList.txt is missing - use a default '''
        development_mode(config_file)

    ''' main loop '''
    count = 0
    if configcount:
        while count < configcount:
            servicename = ' '
            servicename = configlist.__getitem__(count)
            count += 1
            development_mode(servicename)
    else:
        sys.exit(1)

def development_mode (config_file, args=[]):

    servicename = path.basename(config_file) # take the file name like amazon.cfg
    configfile = config_file # the dir + file name
    logfilename = path.splitext(servicename)[0] # remove the extension

    # check for existence of the config file
    if not path.exists(configfile):
        raise Exception("Configuration file %s not found." % configfile)

    suite = BasicWorkflowTestSuite(logfilename, args=args)
    fixtures = []

    '''check if configuration file we read from the ServiceList exist'''
    try:
        path.exists(configfile) and path.isfile(configfile) and access(configfile, R_OK)
    except IOError:
        sys.exit(1)

    suite.configfile   = configfile
    suite.smapiservice = SMAPIService(suite.configfile, wsdl=parser.wsdl, content_file=parser.options.content_file,
                                      logger=suite.logger, console_logger=suite.console_logger)
    suite.client       = suite.smapiservice.buildservice()
    suite.client.login()
    suite.logger.info("Service Name: %s" % safe_str(suite.smapiservice.svcName))
    suite.console_logger.info("Service Name: %s" % safe_str(suite.smapiservice.svcName))

    getuserinfo.suite = suite
    f = getuserinfo.GetUserInfoTest(suite.client, suite.smapiservice)
    fixtures.append(f)

    # Relying on the fact that order is maintained in python lists, the updatetestcontent suite must run before any test
    # case that can change user's favorites (i.e., playlists, favorites, ratings)
    updatetestcontent.suite = suite
    f = updatetestcontent.UpdateTestContent(suite.client, suite.smapiservice)
    f.initialize()
    fixtures.append(f)

    smapimethodvalidation.suite = suite
    f = smapimethodvalidation.SMAPIMethodValidation(suite.client, suite.smapiservice)
    fixtures.append(f)

    playlist.suite = suite
    f = playlist.CreatePlaylist(suite.client, suite.smapiservice)
    f.create_playlist_with_seed_generator = playlist.generate_function_name_strings()
    f.playlist_in_folder_with_seed_generator = playlist.generate_function_name_strings()
    fixtures.append(f)

    f = playlist.DeletePlaylist(suite.client, suite.smapiservice)
    fixtures.append(f)

    f = playlist.RenamePlaylist(suite.client, suite.smapiservice)
    fixtures.append(f)

    f = playlist.AddToPlaylist(suite.client, suite.smapiservice)
    f.cannot_add_to_noneditable_playlist_generator = playlist.generate_items_for_addtoContainer_playlist_test()
    f.can_add_to_playlist_generator = playlist.generate_items_for_addtoContainer_playlist_test()
    fixtures.append(f)

    f = playlist.ReorderPlaylistContainer(suite.client, suite.smapiservice)
    f.generate_can_reorder_data = playlist.generate_reordering_data()
    f.generate_cannot_reorder_data = playlist.generate_cannot_reordering_data()
    fixtures.append(f)

    albumart.suite = suite
    f = albumart.Albumart(suite.client, suite.smapiservice)
    f.create_local_dir()
    f.create_html_result_file()
    fixtures.append(f)

    ratings.suite = suite
    fixtures.append(ratings.Ratings(suite.client, suite.smapiservice))

    favorites.suite = suite
    f = favorites.Favorites(suite.client, suite.smapiservice)
    f.favorite_type_add    = favorites.generate_name_strings()
    f.favorite_type_remove = favorites.generate_name_strings()
    fixtures.append(f)

    browse.suite = suite
    f = browse.Browse(suite.client, suite.smapiservice)
    f.test_scroll_driller = f.generate_iterative_list_drill(f.determiner_browse_scroll)
    f.test_leaf_driller   = f.generate_iterative_list_drill(f.determiner_browse_leaf)
    f.pagination_total_count = f.generate_iterative_list_drill(f.determiner_browse_pagination)
    f.pagination_container = browse.generate_pagination_container(f.generate_iterative_list_drill(f.determiner_pagination_container))
    f.pagination_container_nooverlap = browse.generate_pagination_container(f.generate_iterative_list_drill(f.determiner_pagination_container))
    fixtures.append(f)

    httpverifications.suite = suite
    fixtures.append(httpverifications.HTTPBasedTests(suite.client, suite.smapiservice))

    search.suite = suite
    f = search.Search(suite.client, suite.smapiservice)
    f.initialize()
    fixtures.append(f)

    progvalidation.suite = suite
    f = progvalidation.Progvalidation(suite.client, suite.smapiservice)
    f.test_program_driller = f.generate_iterative_list_drill(f.determiner_program)
    f.test_pagination_total_count = f.generate_iterative_list_drill(f.determiner_browse_pagination)
    fixtures.append(f)

    streamvalidation.suite = suite
    f = streamvalidation.StreamValidation(suite.client, suite.smapiservice)
    f.test_stream_driller = f.generate_iterative_list_drill(f.determiner_stream)
    fixtures.append(f)

    stringtable.suite = suite
    f = stringtable.Stringtable(suite.client, suite.smapiservice)
    f.language = stringtable.generate_language_list()
    fixtures.append(f)

    presentationmap.suite = suite
    f = presentationmap.Presentationmap(suite.client, suite.smapiservice)
    fixtures.append(f)

    extendedmetadatavalidations.suite = suite
    f = extendedmetadatavalidations.ExtendedMetadataValidations(suite.client, suite.smapiservice)
    f.metadata_data = extendedmetadatavalidations.generate_test_data()
    fixtures.append(f)

    getlastupdate.suite = suite
    fixtures.append(getlastupdate.PollingIntervalTest(suite.client, suite.smapiservice))

    authentication.suite = suite
    fixtures.append(authentication.Authentication(suite.client, suite.smapiservice))

    smapi_reporting.suite = suite
    f = smapi_reporting.SMAPIReporting(suite.client, suite.smapiservice)
    fixtures.append(f)

    ssl_validation.suite = suite
    f = ssl_validation.ssl_validation(suite.client, suite.smapiservice)
    fixtures.append(f)

    try:
        import servicecatalog
        import mslogo

        servicecatalog.suite = suite
        f = servicecatalog.ServiceCatalog(suite.client, suite.smapiservice)
        fixtures.append(f)

        mslogo.suite = suite
        f = mslogo.MSLogo(suite.client, suite.smapiservice)
        fixtures.append(f)
    except:
        pass

    # Run it
    suite.run(fixtures)

if __name__ == "__main__":
    parser = ServiceConfigOptionParser(sys.argv)

    ''' Give the configuration file name of the service to test or nothing.
    If nothing given, pass a default smapiConfig.cfg file in case the text
    file with possible partner configurations to execute will be missing'''

    if parser.config_file_passed_by_user:
        development_mode(parser.config_file, parser.args)
    else:
        nightly_mode(parser.config_file)
