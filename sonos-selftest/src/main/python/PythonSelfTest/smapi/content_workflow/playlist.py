import selftestinit #ignore @UnusedImport for packaging 
import sys
from sonos.workflow.basicsuite import BasicWorkflowTestSuite
from sonos.smapi.smapiservice import SMAPIService, ROOT, ARTIST, TRACK, PLAYLISTCONTAINERTYPE, PLAYLISTROOT, ALBUM
from sonos.smapi.smapiclient import CR_STANDARD_RANGE
from sonos.smapi.smapiexceptions import ItemNotFound, ServiceUnknownError, SonosError, ServiceUnavailable
from utility import ServiceConfigOptionParser
from playlistutil import Playlist
from suds import WebFault


def generate_function_name_strings ():
    yield ( "self.smapiservice.get_test_track_id()" ,"self.smapiservice.get_search_term_track()", TRACK )
    yield ( "self.smapiservice.get_test_album_id()" ,"self.smapiservice.get_search_term_album()", ARTIST )

def generate_items_for_addtoContainer_playlist_test():
    yield ("self.smapiservice.get_test_track_id()", TRACK)
    yield ("self.smapiservice.get_test_album_id()", ALBUM)
    yield ("playlist", PLAYLISTCONTAINERTYPE)

def generate_reordering_data ():
    yield ( "0,1,2",    "END", "UP",   "Can move contiguous tracks to the end of an existing playlist")
    yield ( "0,4",      "END", "UP",   "Can move discontiguous tracks to the end of an existing playlist")
    yield ( "6,7,8",    "0",   "DOWN", "Can move contiguous tracks to beginning of an existing playlist")
    yield ( "5,9",      "0",   "DOWN", "Can move discontiguous tracks to the beginning of an existing playlist")
    yield ( "2,3",      "",    "DOWN", "Can move tracks to the beginning of an existing playlist with an empty TO location")
    yield ( "5,6,7",    4,     "DOWN", "Can move contiguous tracks to the middle of an existing playlist")
    yield ( "6,7",      8,     "UP",   "Can move contiguous tracks to the middle of an existing playlist")
    yield ( "5,7,9",    4,     "DOWN", "Can move discontiguous tracks to the middle of an existing playlist")
    yield ( "5,7,9",    10,    "UP",   "Can move discontiguous tracks to the end of an existing playlist")
    yield ( "0-2,4",    6,     "UP",   "Can move combo of ranges and individual tracks to the middle of an existing playlist")
    yield ( "1,3-5",    6,     "UP",   "Can move combo of ranges and individual tracks to the middle of an existing playlist")
    yield ( "0-2,4-6",  "END", "UP",   "Can move combo of ranges and individual tracks to the end of an existing playlist")
    yield ( "2-4,5-7",  "",    "DOWN", "Can move combo of ranges and individual tracks to the beginning of an existing playlist")

def generate_cannot_reordering_data ():
    yield ( "0,1,2",      "PASTEND",     "Cannot move tracks past the end of an existing playlist")
    yield ( "6,7,8",      "BEFORESTART", "Cannot move tracks before the beginning of an existing playlist")
    yield ( "",           "5",           "Cannot move tracks in an existing playlist with empty indices")
    yield ( "-3",         "6",           "Cannot move tracks in an existing playlist with invalid indices")
    yield ( "1,4-9,5-10", "3",           "Cannot move tracks in an existing playlist with overlapping indices")
    yield ( "a,b-c",      "4",           "Cannot move tracks in an existing playlist with alpha indices")
    yield ( "4,5",        "NOTMINE",     "Cannot move tracks in a playlist that is not owned by user")
    yield ( "2,3",        "NOTPLAYLIST", "Cannot move tracks in something that isn't a playlist")

def generate_items_for_removeFromContainer():
    yield ("1,2",     "Should be able to remove multiple individuals from an editable playlist")
    yield ("3-5",     "Should be able to remove a range from an editable playlist")
    yield ("4-6,1,3", "Should be able to remove ranges + individuals from an editable playlist")
    yield ("1-3,4-6", "Should be able to remove multiple ranges from an editable playlist")
    yield ("3,5,7",   "Should be able to remove individual non-contiguous tracks from an editable playlist")
    yield ("2",       "Should be able to remove an individual track from an editable playlist")

def generate_odd_items_for_removeFromContainer():
    yield ("-1",      "Should not be able to remove track from invalid index in an editable playlist")
    yield ("1-4,2-5", "Should not be able to remove tracks from overlapping ranges in an editable playlist")

def generate_items_for_cannot_removeFromContainer():
    yield ("1,2",     "Should not be able to remove multiple individuals from a non-editable playlist")
    yield ("3-5",     "Should not be able to remove range from a non-editable playlist")
    yield ("4-6,1,3", "Should not be able to remove ranges + individuals from a non-editable playlist")
    yield ("1-3,4-6", "Should not be able to remove multiple ranges from a non-editable playlist")
    yield ("1,3,5",   "Should not be able to remove individual non-contiguous tracks from a non-editable playlist")
    yield ("2",       "Should not be able to remove an individual track from a non-editable playlist")


class CreatePlaylist(Playlist):
    """
    This class holds the tests that will check the creation of the playlist for the service. These tests
    will check that basic requested content is returned without error or with expected errors and extra
    features are implemented correctly.
    """
    def __init__(self, client, smapiservice):
        super(CreatePlaylist,self).__init__()
        self.client = client
        self.smapiservice = smapiservice
        self.playlist = Playlist()
        self.auth = self.smapiservice.get_authType()

    def initialize(self):
        self.create_playlist_with_seed_generator = generate_function_name_strings()
        self.playlist_in_folder_with_seed_generator = generate_function_name_strings()

    def test_create_empty_playlist_in_folder(self):
        """
        This test creates and verifies that playlist at the root level has been created
        """
        self.verifyTrueOrSkip(self.smapiservice.get_support_usercontent_playlists(), "Service must support user content"
                                                                                     " playlists ([Capabilities]:"
                                                                                     "usercontentPlaylists in the "
                                                                                     "config file) for this test to "
                                                                                     "run.")
        #Since we have no ability to create a folder programmatically - it needs to be provided in the config file 
        folder_id = self.smapiservice.get_test_playlist_folder_id()
        self.verifyTrueOrSkip(folder_id, "Service must provide a valid playlist folder ID ([Test Content]:playlist "
                                         "folder in the config file) for this test to run.")

        (response,createContainerId)  = self.playlist.createUsingValidParameters(self.client, PLAYLISTCONTAINERTYPE,
                                                                                 "SelfTestEmptyPlaylistInFolder",  folder_id, "", folder_id)

        #Verify that playlist is created in the folder
        created = False
        for containers in response.Items:
            if createContainerId in containers.id:
                created = True
                break
        self.verifyTrueOrFailCase(created, "The newly created playlist (ID = {0}) should be in the playlist folder."
                                  .format(createContainerId))

        (before_delete_total, after_delete_total) = self.playlist.deleteInternalContainer(self.client, createContainerId, folder_id)
        self.verifyNotEqualOrFailCase(before_delete_total, after_delete_total, "The number of playlists in the playlist"
                                                                               " folder should decrement after one is "
                                                                               "deleted.")

    def test_create_empty_playlist_in_nonexisting_folder(self):
        """
        This test verifies that an attempt to create a playlist within non-existing folder returns a proper response
        """
        self.verifyTrueOrSkip(self.smapiservice.get_support_usercontent_playlists(), "Service must support user content"
                                                                                     " playlists ([Capabilities]:"
                                                                                     "usercontentPlaylists in the "
                                                                                     "config file) for this test to "
                                                                                     "run.")
        self.verifyTrueOrSkip(self.smapiservice.get_support_create_playlistsfolder(), "Service must support creating "
                                               "playlist folder ([Additional Settings]: supportcreateplaylistfolder "
                                               "in the config file) for this test to run.")

        folder_id = "Non Existing Folder"

        exception = False
        try:
            response = self.client.createPlaylistContainer(PLAYLISTCONTAINERTYPE, "SelfTestEmptyPlaylistInFolder", folder_id, "", self)
            if type(response) is SonosError:
                sonos_error_code = response.webfault.fault.detail.SonosError
                self.verifyIsNotNoneOrFailCase(sonos_error_code, "A SonosError should contain a SonosError code "
                                                                 "inside.")
                exception = True
        except ItemNotFound:
            exception = True

        self.verifyTrueOrFailCase(exception, "createContainer should return ItemNotFound or a SonosError when "
                                             "attempting to add a playlist to non existing folder.")
        # if no error was returned, then the playlists folder should be checked for the newly created playlist and
        # cleaned up
        if exception == False:
            get_metadata_response,_ = self.client.getMetadata(PLAYLISTROOT, CR_STANDARD_RANGE.index,
                                                              CR_STANDARD_RANGE.count, self)

            if len(get_metadata_response) > 0:
                for item in get_metadata_response.Items:
                    if hasattr(item,'title') and item.title == 'SelfTestEmptyPlaylistInFolder':
                        self.client.deletePlaylistContainer(item.id, self)

    playlist_in_folder_with_seed_generator = None
    def test_combinatorial_create_playlist_in_folder_with_seed(self, playlist_in_folder_with_seed_generator):
        """
        This test creates a playlist with content and verifies that content is successfully added to the playlist.
        Verifications for tracks are done using IDs and Names, because some services may substitute
        one or the other based on regional content availability

        :param playlist_in_folder_with_seed_generator: this is a content driller used by the framework
        """
        #See if functionality is supported by the service
        self.verifyTrueOrSkip(self.smapiservice.get_support_usercontent_playlists(), "Service must support user content"
                                                                                     " playlists ([Capabilities]:"
                                                                                     "usercontentPlaylists in the "
                                                                                     "config file) for this test to "
                                                                                     "run.")
        #See if content ID and title are provided in the configuration file
        (get_id, get_title, containerType) = playlist_in_folder_with_seed_generator
        content_id = eval(get_id)
        content_title = eval(get_title)
        self.verifyTrueOrSkip(content_id and content_title, "Service must provide a valid track and/or album title and "
                                                            "ID ([Test Content]:track/track title/album/album title in "
                                                            "the config file)for this test to run")

        folder_id = self.smapiservice.get_test_playlist_folder_id()
        self.verifyTrueOrSkip(folder_id, "Service must provide a valid playlist folder ID ([Test Content]:playlist "
                                         "folder in the config file) for this test to run.")

        initial_get_last_update = self.client.getLastUpdate(self)

        #Create new playlist
        (does_item_exist,createContainerId) = self.playlist.createUsingValidParameters(self.client, PLAYLISTCONTAINERTYPE,
                                                                                       "SelfTest Playlist in Folder with seed track or album", folder_id, content_id, "")
        if containerType == TRACK:
            #Get all the data to sort out later at once
            info_collection = self.playlist.buildVerificationData(does_item_exist)
            #Only warn for the ID, because some partners change ID's based on region.
            self.verifyInOrWarn(content_id, info_collection, "The seed ID ({0})should be in the newly created "
                                                             "playlist.".format(content_id))
            self.verifyInOrFailCase(content_title, info_collection, "The seed title ({0}) should be in the newly "
                                                                    "created playlist.".format(content_title))
        else:
            #Verify that original playlist and seeded have the same amount of items
            self.playlist.verifySeedContent(self.client, content_id, does_item_exist.total)

        self.playlist.verifyDeleteViaLastUpdate(self.client, createContainerId, initial_get_last_update)

    def test_create_empty_root_playlist(self):
        """
        This test creates and verifies that a playlist at the root level has been created using the parentID
        """
        self.verifyTrueOrSkip(self.smapiservice.get_support_usercontent_playlists(), "Service must support user content"
                                                                                     " playlists ([Capabilities]:"
                                                                                     "usercontentPlaylists in the "
                                                                                     "config file) for this test to "
                                                                                     "run.")

        initial_get_last_update = self.client.getLastUpdate(self)

        #Create new playlist
        (response,createContainerId)  = self.playlist.createUsingValidParameters(self.client, PLAYLISTCONTAINERTYPE,
                                                                                 "Empty SelfTest Playlist", PLAYLISTROOT, "", PLAYLISTROOT)

        #Verify that root playlist is created 
        created = False
        for containers in response.Items:
            if createContainerId in containers.id:
                created = True
                break
        self.verifyTrueOrFailCase(created, "The newly created playlist (ID = {0}) should be in the playlist folder."
                                  .format(createContainerId))

        self.playlist.verifyDeletableAndDelete(self.client, containers, initial_get_last_update)

    def test_create_empty_playlist_with_invalid_name(self):
        """
        This test verifies that an attempt to create a playlist without a name returns a proper response
        """
        self.verifyTrueOrSkip(self.smapiservice.get_support_usercontent_playlists(), "Service must support user content"
                                                                                     " playlists ([Capabilities]:"
                                                                                     "usercontentPlaylists in the "
                                                                                     "config file) for this test to "
                                                                                     "run.")
        playlist_invalid_id = ""

        self.playlist.verifyCreateUsingInvalidParametersRaisesException(self.client, PLAYLISTCONTAINERTYPE, playlist_invalid_id, PLAYLISTROOT, "",
                                                                        "Server should return an error when attempting to create a playlist without name.")

    create_playlist_with_seed_generator = None
    def test_combinatorial_create_playlist_with_seed(self, create_playlist_with_seed_generator):
        """
        This test creates a playlist with content and verifies that content is successfully added to the playlist.
        Verifications for tracks is done using IDs and Names, because some services may substitute one
        or another based on regional content availability

        :param create_playlist_with_seed_generator: this is a content driller used by the framework
        """
        #See if functionality is supported by the service
        self.verifyTrueOrSkip(self.smapiservice.get_support_usercontent_playlists(), "Service must support user content"
                                                                                     " playlists ([Capabilities]:"
                                                                                     "usercontentPlaylists in the "
                                                                                     "config file) for this test to "
                                                                                     "run.")
        #See if content ID and title are provided in the configuration file
        (get_id, get_title, containerType) = create_playlist_with_seed_generator
        content_id = eval(get_id)
        content_title = eval(get_title)
        self.verifyTrueOrSkip(content_id and content_title, "Service must provide a valid track and/or album title and "
                                                            "ID ([Test Content]:track/track title/album/album title in "
                                                            "the config file)for this test to run")

        initial_get_last_update = self.client.getLastUpdate(self)

        (does_item_exist,createContainerId)  = self.playlist.createUsingValidParameters(self.client, PLAYLISTCONTAINERTYPE,
                                                                                        "SelfTest Automation Playlist with seed", PLAYLISTROOT, content_id, "")

        if containerType == TRACK:
            #Get all the data to sort out later at once    
            info_collection = self.playlist.buildVerificationData(does_item_exist)
            self.verifyInOrWarn(content_id, info_collection, "The seed ID ({0})should be in the newly created "
                                                             "playlist.".format(content_id))
            self.verifyInOrFailCase(content_title, info_collection, "The seed title ({0}) should be in the newly "
                                                                    "created playlist.".format(content_title))
        else:
            #Verify that original playlist and seeded have the same amount of items
            self.playlist.verifySeedContent(self.client, content_id, does_item_exist.total)

        self.playlist.verifyDeleteViaLastUpdate(self.client, createContainerId, initial_get_last_update)

    def test_create_playlist_with_seed_playlist(self):
        """
        This test creates and verifies that playlists at the root level have been created with the parent ID
        """
        self.verifyTrueOrSkip(self.smapiservice.get_support_usercontent_playlists(), "Service must support user content"
                                                                                     " playlists ([Capabilities]:"
                                                                                     "usercontentPlaylists in the "
                                                                                     "config file) for this test to "
                                                                                     "run.")
        initial_get_last_update = self.client.getLastUpdate(self)

        #Find a playlist container with in the root
        (root_response,_) = self.client.getMetadata(ROOT, CR_STANDARD_RANGE.index, CR_STANDARD_RANGE.count,
                                                    self) #ignore @UndefinedVariable for named tuple
        self.verifyIsNotNoneOrStop(root_response, "getMetadata should return something other than None.")

        seed_id = 0
        for containers in root_response.Items:
            if containers.itemType == PLAYLISTCONTAINERTYPE:
                seed_id = containers.id
                break
        self.verifyTrueOrSkip(seed_id > 0, "Service must have a Playlist type container at the root level for this "
                                           "test to run.")
        #Get the metadata to have items count from this playlist for further verifications
        (seed_response,_) = self.client.getMetadata(seed_id, CR_STANDARD_RANGE.index, CR_STANDARD_RANGE.count,
                                                    self)#ignore @UndefinedVariable for named tuple
        self.verifyIsNotNoneOrStop(seed_response, "getMetadata should return something other than None.")

        #Create new playlist
        (does_item_exist,createContainerId)  = self.playlist.createUsingValidParameters(self.client, PLAYLISTCONTAINERTYPE,
                                                                                        "Empty SelfTest Automation Playlist", "", seed_id, "")

        #Verify that original playlist and seeded have the same amount of items
        self.verifyEqualOrFailCase(does_item_exist.total, seed_response.total, "The seed playlist and newly created "
                                                                               "playlist should have the same quantity "
                                                                               "of items inside.")

        self.playlist.verifyDeleteViaLastUpdate(self.client, createContainerId, initial_get_last_update)

class DeletePlaylist(Playlist):
    """
    This class holds the tests that will check the deletion of the playlist or verify that all the
    container fields are setup correctly to make delete functionality available.
    Most of delete functional testing is done as a part of create tests 
    """
    def __init__(self, client, smapiservice):
        super(DeletePlaylist,self).__init__()
        self.client = client
        self.smapiservice = smapiservice
        self.playlist = Playlist()

    def initialize(self):
        pass

    def test_properties_for_not_deletable_playlist(self):
        """
        For this test to run the service must have a playlist that cannot be edited by the user in the 'playlist share'
        entry within the config file.
        Verification is done by asserting the property fields 
        """
        self.verifyTrueOrSkip(self.smapiservice.get_support_usercontent_playlists(), "Service must support user content"
                                                                                     " playlists ([Capabilities]:"
                                                                                     "usercontentPlaylists in the "
                                                                                     "config file) for this test to "
                                                                                     "run.")
        playlist_share_id = self.smapiservice.get_test_playlist_share_id()
        self.verifyTrueOrSkip(playlist_share_id, "Service must provide a shared playlist ([Test Content]:playlist share"
                                                 " in the config file) for this test to run.")

        #Get a list of playlist containers
        (root_response,_) = self.client.getMetadata(PLAYLISTROOT, CR_STANDARD_RANGE.index, CR_STANDARD_RANGE.count,
                                                    self) #ignore @UndefinedVariable for named tuple
        self.verifyIsNotNoneOrStop(root_response, "getMetadata should return something other than None")
        #Find playlist that cannot be deleted
        for containers in root_response.Items:
            if containers.id == playlist_share_id:
                self.verifyEqualOrFailCase(str(containers._readOnly).upper(),    'TRUE',  "shared playlists should have"
                                                                                          " their readOnly property "
                                                                                          "set to True.")
                self.verifyEqualOrFailCase(str(containers._userContent).upper(), 'FALSE', "shared playlists should have"
                                                                                          " their userContent property"
                                                                                          " set to False.")
                break

class RenamePlaylist(Playlist):
    """
    This class holds the tests that will check the renaming of the playlist for the service. These tests
    will check that adding content to a playlist does not return any errors and invalid naming is not permitted.
    """
    def __init__(self, client, smapiservice):
        super(RenamePlaylist,self).__init__()
        self.client = client
        self.smapiservice = smapiservice
        self.playlist = Playlist()
        self.auth = self.smapiservice.get_authType()

    def initialize(self):
        pass

    def test_rename_playlist_to_validname(self):
        """
        This test checks that appropriate renaming does not return any errors.
        """
        renameSuccess = False
        self.verifyTrueOrSkip(self.smapiservice.get_support_usercontent_playlists(), "Service must support user content"
                                                                                     " playlists ([Capabilities]:"
                                                                                     "usercontentPlaylists in the "
                                                                                     "config file) for this test to "
                                                                                     "run.")
        # Create new play-list
        (_,containerId)  = self.playlist.createUsingValidParameters(self.client, PLAYLISTCONTAINERTYPE,
                                                                    "!Original Playlist Name!", PLAYLISTROOT, "", PLAYLISTROOT)
        #get 'total' count prior to rename
        (originalCount, _) = self.client.getMetadata(containerId, CR_STANDARD_RANGE.index, CR_STANDARD_RANGE.count,
                                                     self)#ignore @UndefinedVariable for named tuple
        self.verifyIsNotNoneOrStop(originalCount,"getMetadata should return something other than None.")

        new_name = "!New Playlist Name!"
        #rename playlist
        renameContainer = self.client.renamePlaylistContainer(containerId, new_name, self)
        if renameContainer.__str__() == "\n" or renameContainer.__str__() == "":
            renameSuccess = True
        elif "SonosError" in renameContainer.__str__(): #This is a 'known' invalid case
            renameSuccess = True
        else:#    This is an 'unknown' invalid and a failure case
            renameSuccess = False
        #Now verify name change was successful, verify fixture
        self.verifyTrueOrFailCase(renameSuccess, "renameContainer with a valid name should return an empty response.")

        #next verify rename successful by getMetadata on Playlists and iterating to find new name
        (does_item_exist, _) = self.client.getMetadata(PLAYLISTROOT, CR_STANDARD_RANGE.index, CR_STANDARD_RANGE.count,
                                                       self)#ignore @UndefinedVariable for named tuple
        self.verifyIsNotNoneOrStop(does_item_exist, "getMetadata should return something other than None.")

        for mediaCollectionItem in does_item_exist.Items:
            if(mediaCollectionItem.id == containerId):
                self.verifyEqualOrFailCase(new_name, mediaCollectionItem.title,  "The playlist with the original name "
                                                                                 "should no longer be present in the "
                                                                                 "playlists folder." )
                break

        #now verify original 'total' matches 'total' after rename
        (newCount, _) = self.client.getMetadata(containerId, CR_STANDARD_RANGE.index, CR_STANDARD_RANGE.count,
                                                self)#ignore @UndefinedVariable for named tuple
        self.verifyIsNotNoneOrStop(newCount, "getMetadata should return something other than None.")

        self.verifyEqualOrFailCase(newCount.total, originalCount.total,  "The quantity of objects in the playlist "
                                                                         "should remain the same after the playlist"
                                                                         " renamed.")

        #next clean-up, delete the playlist
        self.client.deletePlaylistContainer(containerId, self)

    def test_rename_playlist_to_invalidname(self):
        """
        This test checks that invalid naming is not permitted.
        """
        self.verifyTrueOrSkip(self.smapiservice.get_support_usercontent_playlists(), "Service must support user content"
                                                                                     " playlists ([Capabilities]:"
                                                                                     "usercontentPlaylists in the "
                                                                                     "config file) for this test to "
                                                                                     "run.")
        # Create new play-list
        (_,containerId)  = self.playlist.createUsingValidParameters(self.client, PLAYLISTCONTAINERTYPE,
                                                                    "!Original Playlist Name!", PLAYLISTROOT, "", PLAYLISTROOT)
        #get 'total' count prior to rename
        (originalCount, _) = self.client.getMetadata(containerId, CR_STANDARD_RANGE.index, CR_STANDARD_RANGE.count,
                                                     self)#ignore @UndefinedVariable for named tuple
        self.verifyIsNotNoneOrStop(originalCount, "getMetadata should return something other than None.")

        invalid_names = {"All white spaces": "   ", "260 Characters Long": '1234567890123456789012345678901234567890123'
                                                                           '4567890123456789012345678901234567890123456'
                                                                           '7890123456789012345678901234567890123456789'
                                                                           '0123456789012345678901234567890123456789012'
                                                                           '3456789012345678901234567890123456789012345'
                                                                           '6789012345678901234567890123456789012345678'
                                                                           '90'}

        for description, new_name in invalid_names.iteritems():

            #rename playlist
            try:
                message = "Testing with an invalid name: {0}.".format(description)
                self.logger.info(message)
                self.console_logger.info(message)

                renameContainer = self.client.renamePlaylistContainer(containerId, new_name, self)

                if type(renameContainer) is SonosError:
                    sonos_error_code = renameContainer.webfault.fault.detail.SonosError
                    self.verifyIsNotNoneOrFailCase(sonos_error_code, "A SonosError should contain a SonosError code "
                                                                     "inside.")
                else:
                    self.warn("renamePlaylistContainer with an invalid name ({0}) should return a SonosError.".format(description))

            except ItemNotFound, e:
                message = "Renaming a playlist to an invalid name returned the following fault {0} with no " \
                          "SonosError code.".format(e)
                self.logger.warn(message)
                self.console_logger.warn(message)
            except ServiceUnknownError:
                self.fail("Renaming a playlist to an invalid name returned an incorrect soap fault (ServiceUnknownError"
                          "). This should return a \"Client.ItemNotFound\".")

            #now verify original 'total' matches 'total' after failed rename
            (newCount, _) = self.client.getMetadata(containerId, CR_STANDARD_RANGE.index, CR_STANDARD_RANGE.count,
                                                    self)#ignore @UndefinedVariable for named tuple
            self.verifyIsNotNoneOrStop(newCount, "getMetadata should return something other than None.")
            self.verifyEqualOrFailCase(newCount.total, originalCount.total,  "The quantity of objects in the "
                                                                             "playlist should remain the same "
                                                                             "after the playlist was renamed.")

        #next clean-up, delete the playlist
        self.client.deletePlaylistContainer(containerId, self)


    def test_rename_non_editable_playlist(self):
        """
        This test checks that playlists which do not belong to the current user cannot be renamed.
        """
        self.verifyTrueOrSkip(self.smapiservice.get_support_usercontent_playlists(), "Service must support user content"
                                                                                     " playlists ([Capabilities]:"
                                                                                     "usercontentPlaylists in the "
                                                                                     "config file) for this test to "
                                                                                     "run.")

        (playlists, _) = self.client.getMetadata(PLAYLISTROOT, CR_STANDARD_RANGE.index, CR_STANDARD_RANGE.count,
                                                 self)#ignore @UndefinedVariable for named tuple
        self.verifyIsNotNoneOrStop(playlists, "getMetadata should return something other than None.")

        containerId = self.playlist.findNonEditablePlaylist(self.client)
        self.verifyTrueOrSkip(containerId, "A non-editable playlist must be discovered in the playlists folder for this"
                                           " test to run.")
        #get 'total' count prior to rename
        (originalCount, _) = self.client.getMetadata(containerId, CR_STANDARD_RANGE.index, CR_STANDARD_RANGE.count,
                                                     self)#ignore @UndefinedVariable for named tuple
        self.verifyIsNotNoneOrStop(originalCount, "getMetadata should return something other than None.")

        exception = False
        #rename playlist
        try:
            renameContainer = self.client.renamePlaylistContainer(containerId, "Should not be able to rename this "
                                                                               "playlist", self)
            self.verifyTrueOrStop(renameContainer, "renameContainer should return something other than None.")
        except ServiceUnknownError or ServiceUnavailable:
            exception = True
        except Exception:
            renameContainer = None

        if hasattr(renameContainer, 'fault_client_message') and renameContainer.fault_client_message.split()[0] \
                in ['Server.ServiceUnknownError', 'Server.ServiceUnavailable']:
                exception = True
        self.verifyTrueOrFailCase(exception, "renamePlaylistContainer with a non-editable playlist should return a "
                                             "SonosError.")

        #now verify original 'total' matches 'total' after failed rename
        (newCount, _) = self.client.getMetadata(containerId, CR_STANDARD_RANGE.index, CR_STANDARD_RANGE.count,
                                                self)#ignore @UndefinedVariable for named tuple
        self.verifyIsNotNoneOrStop(newCount, "getMetadata should return something other than None.")

        self.verifyEqualOrFailCase(newCount.total, originalCount.total,  "The quantity of objects in the playlist "
                                                                         "should remain the same after the playlist "
                                                                         "was renamed.")

class AddToPlaylist(Playlist):
    """
    This class contains the tests that verify that adding content to playlists functions correctly.
    """
    def __init__(self, client, smapiservice):
        super(AddToPlaylist,self).__init__()
        self.client = client
        self.smapiservice = smapiservice
        self.playlist = Playlist()
        self.auth = self.smapiservice.get_authType()

    def initialize(self):
        self.cannot_add_to_noneditable_playlist_generator = generate_items_for_addtoContainer_playlist_test()
        self.can_add_to_playlist_generator = generate_items_for_addtoContainer_playlist_test()

    can_add_to_playlist_generator = None
    def test_combinatorial_add_content_to_playlist(self, can_add_to_playlist_generator):
        """
        This test checks that adding content to a playlist does not return any errors.

        :param can_add_to_playlist_generator: this is a content driller used by the framework
        """
        self.verifyTrueOrSkip(self.smapiservice.get_support_usercontent_playlists(), "Service must support user content"
                                                                                     " playlists ([Capabilities]:"
                                                                                     "usercontentPlaylists in the "
                                                                                     "config file) for this test to "
                                                                                     "run.")

        (item, containerType) = can_add_to_playlist_generator

        #yield 'playlist' does not require eval
        if containerType == PLAYLISTCONTAINERTYPE:
            #we will build a temporary playlist here, which we will try to add to another playlist later below
            (_,itemId)  = self.playlist.createUsingValidParameters(self.client, PLAYLISTCONTAINERTYPE,
                                                                   "Automation Sub-Playlist", PLAYLISTROOT, "", PLAYLISTROOT)
            ALBUM_ID = self.smapiservice.get_test_album_id()
            self.verifyTrueOrSkip(ALBUM_ID, "Service must provide a valid album ([Test Content]:album in the config "
                                            "file) for this test to run.")
            addToSubContainer = self.client.addToPlaylistContainer(ALBUM_ID, itemId, 0, self)
        else:
            itemId = eval(item)
            self.verifyTrueOrSkip(itemId, "Service must provide a valid {0} ([Test Content]:{0} "
                                          "in the config file) for this test to run.".format(containerType))

        #itemId will be the resource (track/album/playlist) to be added to a playlist
        # Next, create an empty playlist to which we will add the itemId
        (_,containerId)  = self.playlist.createUsingValidParameters(self.client, PLAYLISTCONTAINERTYPE,
                                                                    "Automation Playlist", PLAYLISTROOT, "", PLAYLISTROOT)

        if containerType == ALBUM or containerType == PLAYLISTCONTAINERTYPE:
            #get track count on the album/sub-playlist
            (trackCount, _) = self.client.getMetadata(itemId, CR_STANDARD_RANGE.index, CR_STANDARD_RANGE.count,
                                                      self)#ignore @UndefinedVariable for named tuple
            expectedCount = trackCount.total
        elif containerType == TRACK:
            #track count for a track case
            expectedCount = 1

        #add itemId (track/album/playlist) to the empty playlist
        addToContainer = self.client.addToPlaylistContainer(itemId, containerId, 0, self)

        #now check track count after adding tracks to the playlist
        (actualCount, _) = self.client.getMetadata(containerId, CR_STANDARD_RANGE.index, CR_STANDARD_RANGE.count,
                                                   self)#ignore @UndefinedVariable for named tuple
        self.verifyIsNotNoneOrStop(actualCount, "getMetadata should return something other than None.")

        self.verifyEqualOrFailCase(int(actualCount.total), int(expectedCount),  "The newly created playlist should "
                                                                                "contain the same quantity of tracks "
                                                                                "as the number which were added to it.")

        #next clean-up, delete the playlist
        self.client.deletePlaylistContainer(containerId, self)
        if containerType == PLAYLISTCONTAINERTYPE:
            #remove the sub-playlist created
            self.client.deletePlaylistContainer(itemId, self)

    cannot_add_to_noneditable_playlist_generator = None
    def test_combinatorial_cannot_add_content_to_noneditable_playlist(self, cannot_add_to_noneditable_playlist_generator):
        self.verifyTrueOrSkip(self.smapiservice.get_support_usercontent_playlists(), "Service must support user content"
                                                                                     " playlists ([Capabilities]:"
                                                                                     "usercontentPlaylists in the "
                                                                                     "config file) for this test to "
                                                                                     "run.")

        (playlists, _) = self.client.getMetadata(PLAYLISTROOT, CR_STANDARD_RANGE.index, CR_STANDARD_RANGE.count,
                                                 self)#ignore @UndefinedVariable for named tuple
        self.verifyIsNotNoneOrStop(playlists, "getMetadata should return something other than None.")

        noneditable_playlist_id = self.playlist.findNonEditablePlaylist(self.client)
        self.verifyTrueOrSkip(noneditable_playlist_id, "A non-editable playlist must be discovered in the playlists "
                                                       "folder for this test to run.")

        (item, containerType) = cannot_add_to_noneditable_playlist_generator
        #yield 'playlist' does not require eval
        if containerType == PLAYLISTCONTAINERTYPE:
            #we will build a temporary playlist here, which we will try to add to a non-editable list later below
            (_,itemId)  = self.playlist.createUsingValidParameters(self.client, PLAYLISTCONTAINERTYPE,
                                                                   "Automation Playlist", PLAYLISTROOT, "", PLAYLISTROOT)
            ALBUM_ID = self.smapiservice.get_test_album_id()
            self.verifyTrueOrSkip(ALBUM_ID, "Service must provide a valid album ([Test Content]:album in the config "
                                            "file) for this test to run.")
            addToSubContainer = self.client.addToPlaylistContainer(ALBUM_ID, itemId, 0, self)
        else:
            itemId = eval(item)

        #add album to non-editable playlist
        addToContainer = self.client.addToPlaylistContainer(itemId, noneditable_playlist_id, 0, self)

        #assuming the response contains a SOAP Fault
        self.verifyTrueOrFailCase(hasattr(addToContainer, 'fault'), "addToPlaylistContainer with a non-editable "
                                                                    "playlist should return a fault.")
        #finally remove the temporary playlist created
        if str(item) == PLAYLISTCONTAINERTYPE:
            self.client.deletePlaylistContainer(itemId, self)

class ReorderPlaylistContainer(Playlist):
    """
    This class holds the tests that verify that reordering content in playlists functions correctly.
    """
    def __init__(self, client, smapiservice):
        super(ReorderPlaylistContainer,self).__init__()
        self.client = client
        self.smapiservice = smapiservice
        self.playlist = Playlist()

    def initialize(self):
        self.generate_can_reorder_data = generate_reordering_data()
        self.generate_cannot_reorder_data = generate_cannot_reordering_data()

    generate_cannot_reorder_data = 0
    def test_combinatorial_cannot_move_tracks(self, generate_cannot_reorder_data):
        """
        This test verifies that under specified conditions playlist cannot be rearranged or written to

        :param  generate_cannot_reorder_data: this is a content driller used by the framework
        """
        expectedFailure = 0
        #see if functionality is supported by the service          
        self.verifyTrueOrSkip(self.smapiservice.get_support_usercontent_playlists(), "Service must support user content"
                                                                                     " playlists ([Capabilities]:"
                                                                                     "usercontentPlaylists in the "
                                                                                     "config file) for this test to "
                                                                                     "run.")
        (tracksToMoveIndex, positionToMoveTo, message) = generate_cannot_reorder_data

        #create a playlist based on the config file album info
        (does_item_exist,createContainerId) = self.playlist.createPlaylistReorderContainer(self.smapiservice, self.client)

        #some special cases
        if positionToMoveTo == "PASTEND":
            positionToMoveTo = int(does_item_exist.total) + 2
        elif positionToMoveTo == "BEFORESTART":
            positionToMoveTo = -1
        elif positionToMoveTo == "NOTMINE":
            positionToMoveTo = 2
            (before_delete_total, after_delete_total) = self.playlist.deleteInternalContainer \
                (self.client, createContainerId, PLAYLISTROOT)
            self.verifyNotEqualOrFailCase \
                (before_delete_total, after_delete_total, "The quantity of playlists in the playlists folder should "
                                                          "change after one of the playlists is deleted.")
            #browse to find a read only playlist
            createContainerId = self.playlist.findNonEditablePlaylist(self.client)
            self.verifyTrueOrSkip(createContainerId, "A non-editable playlist must be discovered in the playlists "
                                                     "folder for this test to run.")
        elif positionToMoveTo == "NOTPLAYLIST":
            #get album ID from config file to use it as an invalid playlist
            (before_delete_total, after_delete_total) = self.playlist.deleteInternalContainer\
                (self.client, createContainerId, PLAYLISTROOT)
            self.verifyNotEqualOrFailCase\
                (before_delete_total, after_delete_total, "The quantity of playlists in the playlists folder should "
                                                          "change after one of the playlists is deleted.")
            createContainerId = self.smapiservice.get_test_album_id()
            self.verifyTrueOrSkip(createContainerId, "Service must provide a valid album ([Test Content]:album "
                                          "in the config file) for this test to run.")
        #since we are not enforcing any specific error messages - catch whatever comes in
        #there are conditions when the call does return the known value in the response,
        #in this case we do not fall into except case
        #there are conditions when there is no return value, than we set it up ourselves
        try:
            expectedFailure = self.client.reorderPlaylistContainer(createContainerId, tracksToMoveIndex,
                                                                   str(positionToMoveTo), 0, self)
        except Exception:
            expectedFailure = 1

        #do the actual test verification    
        self.verifyTrueOrFailCase(expectedFailure != 0,  message)
        #clean
        if positionToMoveTo != "NOTPLAYLIST":
            (before_delete_total, after_delete_total) = self.playlist.deleteInternalContainer(self.client, createContainerId, PLAYLISTROOT)
            self.verifyNotEqualOrFailCase(before_delete_total, after_delete_total, "The number of playlists in the "
                                                                                   "playlist folder should decrement "
                                                                                   "after one is deleted.")


    generate_can_reorder_data = 0
    def test_combinatorial_can_move_tracks(self, generate_can_reorder_data):
        """
        This test verifies that tracks can be shifted in position in an editable playlist

        :param  generate_can_reorder_data: this is a content driller used by the framework
        """
        #see if functionality is supported by the service
        self.verifyTrueOrSkip(self.smapiservice.get_support_usercontent_playlists(), "Service must support user content"
                                                                                     " playlists ([Capabilities]:"
                                                                                     "usercontentPlaylists in the "
                                                                                     "config file) for this test to "
                                                                                     "run.")
        (tracksToMoveIndex, positionToMoveTo, direction, _) = generate_can_reorder_data

        #create a playlist based on the config file album info
        (does_item_exist,createContainerId) = self.playlist.createPlaylistReorderContainer(self.smapiservice, self.client)

        #convert the char list into int list
        reorder = []
        for x in tracksToMoveIndex.split(','):
            if '-' in x:
                data = self.playlist.convertRangeToLinear(x)
                for x in data:
                    reorder.append(x)
            else:
                reorder.append(int(x))

        #get original corresponding track ids to compare later
        (tracksToMoveId, moveSize) = self.playlist.getTrackIdsTestSet(self.client, createContainerId, reorder)

        if positionToMoveTo == "END":
            positionToMoveTo = does_item_exist.total

        #reorder
        try:
            reorder_result = self.client.reorderPlaylistContainer(createContainerId, tracksToMoveIndex,
                                                                  positionToMoveTo, 0, self)
            if type(reorder_result) is SonosError:
                self.warn("{0}. Tracks must be contiguous".format(reorder_result.webfault.message))
            else:
                #get new index for the moved tracks
                if positionToMoveTo == "":
                    positionToMoveTo = 0 # convert empty string to null
                tracksMovedIndex = self.playlist.getTracksMovedNewIndex(int(positionToMoveTo), moveSize, direction)

                #get new  track ids set
                (tracksMoveId, moveSize) = self.playlist.getTrackIdsTestSet(self.client, createContainerId, tracksMovedIndex)

                #verification block
                self.verifyEqualOrFailCase(tracksToMoveId, tracksMoveId, "The track IDs in a playlist should remain the same"
                                                                         "when reorderContainer is used to change their order.")
                #clean
                (before_delete_total, after_delete_total) = self.playlist.deleteInternalContainer(self.client, createContainerId, PLAYLISTROOT)
                self.verifyNotEqualOrFailCase(before_delete_total, after_delete_total, "The number of playlists in the "
                                                                                       "playlists folder should decrement "
                                                                                       "when one is deleted.")
        except WebFault, e:
            self.stop("The WebFault, {0}, was caught: {1}".format(type(e).__name__,
                                                                         e.fault.fault.faultstring))


class RemoveFromPlaylist(Playlist):
    """
    This class holds the tests that verify that removing content from playlists function correctly.
    """
    def __init__(self, client, smapiservice):
        super(RemoveFromPlaylist,self).__init__()
        self.client = client
        self.smapiservice = smapiservice
        self.playlist = Playlist()

    def initialize(self):
        self.remove_from_playlist_generator = generate_items_for_removeFromContainer()
        self.remove_from_playlist_odd_generator = generate_odd_items_for_removeFromContainer()
        self.remove_from_noneditable_playlist_generator = generate_items_for_cannot_removeFromContainer()

    remove_from_playlist_generator = None
    def test_combinatorial_remove_items_from_editable_playlist(self, remove_from_playlist_generator):
        """
        This test verifies that tracks can be removed from an editable playlist

        :param  remove_from_playlist_generator: this is a content driller used by the framework
        """
        self.verifyTrueOrSkip(self.smapiservice.get_support_usercontent_playlists(), "Service must support user content"
                                                                                     " playlists ([Capabilities]:"
                                                                                     "usercontentPlaylists in the "
                                                                                     "config file) for this test to "
                                                                                     "run.")
        album_id = self.smapiservice.get_test_album_id()
        self.verifyTrueOrSkip(album_id, "Service must provide a valid album ([Test Content]:album in the config file) "
                                        "for this test to run.")

        (index, message) = remove_from_playlist_generator

        # Create new playlist
        (_,containerId)  = self.playlist.createUsingValidParameters(self.client, PLAYLISTCONTAINERTYPE,
                                                                    "Automation Playlist", PLAYLISTROOT, "", PLAYLISTROOT)
        (original, _) = self.client.getMetadata(containerId, CR_STANDARD_RANGE.index, CR_STANDARD_RANGE.count,
                                                self)#ignore @UndefinedVariable for named tuple
        self.verifyIsNotNoneOrStop(original, "getMetadata should return something other than None.")

        #check if sufficient tracks in the album, else STOP
        (albumInfo, _) = self.client.getMetadata(album_id, CR_STANDARD_RANGE.index, CR_STANDARD_RANGE.count,
                                                 self)#ignore @UndefinedVariable for named tuple
        self.verifyIsNotNoneOrStop(albumInfo, "getMetadata should return something other than None.")
        self.verifyGreaterEqualOrStop(int(albumInfo.total), 10, "Service must provide an album ([Test Content]:album in"
                                                                " the config file) with 10+ tracks in the config file"
                                                                " for this test to run.")
        #next lets add some tracks to the playlist, we'll add an entire album
        addToContainer = self.client.addToPlaylistContainer(album_id, containerId, 0, self)

        #get list of tracks with IDs prior to remove
        (original, _) = self.client.getMetadata(containerId, CR_STANDARD_RANGE.index, CR_STANDARD_RANGE.count,
                                                self)#ignore @UndefinedVariable for named tuple
        self.verifyIsNotNoneOrStop(original, "getMetadata should return something other than None.")

        #convert ranges to linear
        removeIndices={}
        for x in index.split(','):
            if '-' in x:
                data = self.playlist.convertRangeToLinear(x)
                for x in data:
                    removeIndices[int(x)]=original.Items[int(x)]['id']
            else:
                removeIndices[int(x)]=original.Items[int(x)]['id']

        #now remove tracks from playlist
        removeItems = self.client.removeFromPlaylistContainer(containerId, index, self)

        #get list of tracks with IDs after remove
        (final, _) = self.client.getMetadata(containerId, CR_STANDARD_RANGE.index, CR_STANDARD_RANGE.count,
                                             self)#ignore @UndefinedVariable for named tuple
        self.verifyIsNotNoneOrStop(final, "getMetadata should return something other than None.")

        for ind, removedtrackID in removeIndices.items():
            for mediaMetadata in final.Items:
                actualTrackID = mediaMetadata.id
                # Check if the removed track ID is present in the playlist, FAIL if found
                self.verifyNotEqualOrFailCase(str(removedtrackID), str(actualTrackID), message)

        #CLEAN-UP: finally delete the playlist
        self.client.deletePlaylistContainer(containerId, self)

    remove_from_playlist_odd_generator = None
    def test_combinatorial_remove_odd_items_from_editable_playlist(self, remove_from_playlist_odd_generator):
        """
        This test verifies that certain corner cases of removing tracks from an editable playlist behave as expected

        :param  remove_from_playlist_odd_generator: this is a content driller used by the framework
        """
        album_id = self.smapiservice.get_test_album_id()
        self.verifyTrueOrSkip(album_id, "Service must provide a valid album ([Test Content]:album in the config file) "
                                        "for this test to run.")
        removeFailed = False
        self.verifyTrueOrSkip(self.smapiservice.get_support_usercontent_playlists(), "Service must support user content"
                                                                                     " playlists ([Capabilities]:"
                                                                                     "usercontentPlaylists in the "
                                                                                     "config file) for this test to "
                                                                                     "run.")

        (index, message) = remove_from_playlist_odd_generator

        # Create new playlist
        (_,containerId)  = self.playlist.createUsingValidParameters(self.client, PLAYLISTCONTAINERTYPE,
                                                                    "Automation Playlist", PLAYLISTROOT, "", PLAYLISTROOT)
        (original, _) = self.client.getMetadata(containerId, CR_STANDARD_RANGE.index, CR_STANDARD_RANGE.count,
                                                self)#ignore @UndefinedVariable for named tuple
        self.verifyIsNotNoneOrStop(original, "getMetadata should return something other than None.")

        #next lets add some tracks to the playlist, we'll add an entire album
        addToContainer = self.client.addToPlaylistContainer(album_id, containerId, 0, self)

        #get 'total' count prior to remove
        (original, _) = self.client.getMetadata(containerId, CR_STANDARD_RANGE.index, CR_STANDARD_RANGE.count,
                                                self)#ignore @UndefinedVariable for named tuple
        self.verifyIsNotNoneOrStop(original, "getMetadata should return something other than None.")
        originalCount = original.total

        #now remove
        try:
            self.client.removeFromPlaylistContainer(containerId, index, self)
        except Exception, e:
            removeFailed = True
        if removeFailed:
            #get 'total' count after remove
            (final, _) = self.client.getMetadata(containerId, CR_STANDARD_RANGE.index, CR_STANDARD_RANGE.count,
                                                 self)#ignore @UndefinedVariable for named tuple
            self.verifyIsNotNoneOrStop(final, "getMetadata should return something other than None.")
            finalCount = final.total

            expectedCount = int(originalCount)

            #verify counts match up
            self.verifyEqualOrFailCase(int(expectedCount), int(finalCount), message)
        else:
            self.verifyTrueOrFailCase(removeFailed, message)
        #CLEAN-UP: finally delete the playlist
        self.client.deletePlaylistContainer(containerId, self)

    remove_from_noneditable_playlist_generator = None
    def test_combinatorial_cannot_remove_items_from_noneditable_playlist(self, remove_from_noneditable_playlist_generator):
        """
        This test verifies that tracks cannot be removed from non-editable playlists

        :param  remove_from_noneditable_playlist_generator: this is a content driller used by the framework
        """
        self.verifyTrueOrSkip(self.smapiservice.get_support_usercontent_playlists(), "Service must support user content"
                                                                                     " playlists ([Capabilities]:"
                                                                                     "usercontentPlaylists in the "
                                                                                     "config file) for this test to "
                                                                                     "run.")

        (index, message) = remove_from_noneditable_playlist_generator

        containerId = self.playlist.findNonEditablePlaylist(self.client)
        self.verifyTrueOrSkip(containerId, "A non-editable playlist must be discovered in the playlists folder for this"
                                           " test to run.")

        #get 'total' count prior to remove
        (original, _) = self.client.getMetadata(containerId, CR_STANDARD_RANGE.index, CR_STANDARD_RANGE.count,
                                                self)#ignore @UndefinedVariable for named tuple
        self.verifyIsNotNoneOrStop(original, "getMetadata should return something other than None.")
        originalCount = original.total

        #now attempt to remove
        removeResponse = self.client.removeFromPlaylistContainer(containerId, index, self)

        #assuming the response contains a SOAP Fault
        self.verifyTrueOrFailCase(hasattr(removeResponse, 'fault'), "removeFromContainer with a non-editable playlist"
                                                                    " should return a fault.")

        #get 'total' count after remove
        (final, _) = self.client.getMetadata(containerId, CR_STANDARD_RANGE.index, CR_STANDARD_RANGE.count,
                                             self)#ignore @UndefinedVariable for named tuple
        self.verifyIsNotNoneOrStop(final, "getMetadata should return something other than None.")
        finalCount = final.total

        expectedCount = int(originalCount)

        #verify counts match up
        self.verifyEqualOrFailCase(int(expectedCount), int(finalCount), message)

#Main
if __name__ == "__main__":

    parser = ServiceConfigOptionParser(sys.argv)

    suite = BasicWorkflowTestSuite("Playlist-editing Validation", args=parser.args)
    suite.smapiservice = SMAPIService(parser.config_file, parser.wsdl, getattr(parser.options, 'content_file'),
                                      suite.logger, suite.console_logger)
    suite.client = suite.smapiservice.buildservice()
    suite.client.login()

    playlist = []
    create = CreatePlaylist(suite.client, suite.smapiservice)
    create.initialize()
    playlist.append(create)

    delete = DeletePlaylist(suite.client, suite.smapiservice)
    playlist.append(delete)

    rename = RenamePlaylist(suite.client, suite.smapiservice)
    playlist.append(rename)

    add = AddToPlaylist(suite.client, suite.smapiservice)
    add.initialize()
    playlist.append(add)

    reorder = ReorderPlaylistContainer(suite.client, suite.smapiservice)
    reorder.initialize()
    playlist.append(reorder)

    remove = RemoveFromPlaylist(suite.client, suite.smapiservice)
    remove.initialize()
    playlist.append(remove)

    suite.run(playlist)