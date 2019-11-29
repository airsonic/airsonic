import selftestinit #ignore @UnusedImport for packaging 
import sys
from sonos.workflow.basicsuite import BasicWorkflowTestSuite
from sonos.workflow.fixture import WorkflowTestFixture
from sonos.smapi.smapiservice import SMAPIService, USERID
from sonos.smapi.smapiclient import SMAPIClient, CR_STANDARD_RANGE
from utility import ServiceConfigOptionParser

def generate_name_strings ():
    yield ( "self.smapiservice.get_support_favorites_track()", "self.smapiservice.get_test_track_id()", "self.smapiservice.get_favorite_tracks_container_id()" )
    yield ( "self.smapiservice.get_support_favorites_album()", "self.smapiservice.get_test_album_id()", "self.smapiservice.get_favorite_albums_container_id()" )
    """Currently not supported"""
    #yield ( "self.smapiservice.get_support_favorites_artist()", "self.smapiservice.get_test_artist_id()", "self.smapiservice.get_favorite_artists_container_id()" )

class Favorites(WorkflowTestFixture, SMAPIClient, SMAPIService):
    """
    This suite will exercise the createItem, deleteItem and getLastUpdate functionality in a user type scenario.
    Each SMAPI method will be invoked using data supplied in the configuration file. Tests will be executed
    in order to verify the expected operations result in the addition/removal of the expected items.
    """

    def __init__(self, client, smapiservice):
        super(Favorites,self).__init__()
        self.client = client
        self.smapiservice = smapiservice
        self.auth = self.smapiservice.get_authType()

    def initialize(self):
        self.favorite_type_add    = generate_name_strings()
        self.favorite_type_remove = generate_name_strings()

    favorite_type_add = None
    def test_combinatorial_add_to_favorites(self, favorite_type_add):
        """
        This will process a createItem() SMAPI call using an ID pulled in from the config file. The pass/fail
        criteria is whether or not the item that was added is found in the Favorites location supplied in the config
        file.

        :param  favorite_type_add: this is a content driller used by the framework
        """
        (support, fav_id, fav_container_id) = favorite_type_add
        print support
        support = eval(support)
        fav_id = eval(fav_id)
        fav_container_id = eval(fav_container_id)
        self.verifyTrueOrSkip(support, "Service must support adding/removing favorite tracks and/or albums"
                                       "([Capabilities]:favoritesTracks in the config file) for this test to run.")
        self.verifyTrueOrSkip(fav_id, "Service must provide a valid album and/or track ID ([Test Content]:track/album "
                                      "in the config file) for this test to run.")
        self.verifyTrueOrSkip(fav_container_id, "Service must provide a valid favorite track and/or album container id "
                                                "([Favorite Containers]:faveAlbums/faveTracks in the config file) for "
                                                "this test to run.")

        try:
            create_item_response = self.client.createItem(fav_id, self)
        except Exception, e:
            self.stop(e)

        self.verifyTrueOrFailCase(create_item_response or (create_item_response == '') or
                                  (create_item_response == None), "createItem should return the id of the item created "
                                                                  "or nothing.")

    favorite_type_remove = None
    def test_combinatorial_remove_from_favorites(self, favorite_type_remove):
        """
        This will process a deleteItem() SMAPI call using a ID pulled in from the config file. The pass/fail
        criteria is whether or not the item that was added is found in the Favorites location supplied in the config
        file.
        If the item to be deleted is not found in the supplied location we will attempt to add it first.

        :param  favorite_type_remove: this is a content driller used by the framework
        """
        (support, fav_id, fav_container_id) = favorite_type_remove
        self.verifyTrueOrSkip(eval(support), "Service must support adding/removing favorite tracks and/or albums"
                                             "([Capabilities]:favoritesTracks in the config file) for this test to "
                                             "run.")

        fav_id_value = eval(fav_id)
        fav_container_id_value = eval(fav_container_id)
        removed = True

        self.verifyTrueOrSkip(fav_id_value, "Service must provide a valid album and/or track ID ([Test Content]:"
                                            "track/album in the config file) for this test to run.")
        self.verifyTrueOrSkip(fav_container_id_value, "Service must provide a valid favorite track and/or album "
                                                      "container ID ([Favorite Containers]:faveAlbums/faveTracks in "
                                                      "the config file) for this test to run.")

        (does_item_exist,_) = self.client.getMetadata(fav_container_id_value, CR_STANDARD_RANGE.index,
                                                      CR_STANDARD_RANGE.count,
                                                      self) #ignore @UndefinedVariable for named tuple
        self.verifyIsNotNoneOrFailCase(does_item_exist, "getMetadata should return something other than None.")
        #use the first favorite item for the test
        self.verifyTrueOrStop(len(does_item_exist.Items) > 0, "There should be at least one favorited item in the favorites folder.")
        fav_id_value = does_item_exist.Items[0].id

        try:
            self.client.deleteItem(fav_id_value, self)
        except Exception, e:
            self.fail(e)

        (now_does_item_exist,_) = self.client.getMetadata(fav_container_id_value, CR_STANDARD_RANGE.index,
                                                          CR_STANDARD_RANGE.count,
                                                          self) #ignore @UndefinedVariable for named tuple
        self.verifyIsNotNoneOrFailCase(now_does_item_exist, "getMetadata should return something other than None.")
        if now_does_item_exist.total > 0: #if there was only one item to begin with - nothing to iterate trough
            for x in now_does_item_exist.Items:
                if fav_id_value in x.id:
                    removed = False
                    break
        self.verifyTrueOrFailCase(removed, "deleteItem should remove the item corresponding to the provided ID "
                                           "([Test Content]:track/album in the config file)from "
                                           "favorites folder. deleteItem was called on ID = {0}".format(fav_id_value))

    def test_getlastupdate_with_change(self):
        """
        This test executes the getLastUpdate() SMAPI call before and after a createItem change. The pass criteria is
        that the after values should not match the before values, which indicates that the service has notified Sonos
        of the change in favorites.
        """
        self.verifyTrueOrSkip(self.smapiservice.get_support_favorites(), "Service must support adding/removing "
                                                                         "favorites tracks and/or albums and/or artists"
                                                                         "([Capabilities]:favoritesTracks/"
                                                                         "favoritesAlbums/favoritesArtists in the "
                                                                         "config file) for this test to run.")

        track_id = self.smapiservice.get_test_track_id()
        self.verifyTrueOrSkip(track_id, "Service must provide a valid track ID ([Test Content]:track in the config) "
                                        "for this test to run.")
        initial_get_last_update = self.client.getLastUpdate(self)
        self.verifyIsNotNoneOrStop(initial_get_last_update, "getLastUpdate should return something other than None.")

        initial_get_last_update_values = []
        for x in initial_get_last_update:
            update_value = x[1]
            initial_get_last_update_values.append(update_value)

        try:
            create_item_response = self.client.createItem(track_id, self)
        except Exception, e:
            self.stop(e)

        self.verifyIsNotNoneOrWarn(create_item_response,"createItem should return the id of the item created.")

        secondary_get_last_update = self.client.getLastUpdate(self)
        self.verifyIsNotNoneOrStop(secondary_get_last_update, "getLastUpdate should return something other than None.")

        # cleanup, remove the newly created item.
        self.client.deleteItem(track_id, self)

        secondary_get_last_update_values = []
        for y in secondary_get_last_update:
            update_value = y[1]
            secondary_get_last_update_values.append(update_value)
        self.verifyNotEqualOrFailCase(initial_get_last_update_values, secondary_get_last_update_values,
                                      "getLastUpdate checksum values should change after a change was made to"
                                      "favorites.")

        #Main
if __name__ == "__main__":

    parser = ServiceConfigOptionParser(sys.argv)

    suite = BasicWorkflowTestSuite("Favorites Validation", args=parser.args)

    suite.smapiservice = SMAPIService(parser.config_file, parser.wsdl, getattr(parser.options, 'content_file'),
                                      suite.logger, suite.console_logger)
    suite.client = suite.smapiservice.buildservice()
    suite.client.login()

    f = Favorites(suite.client, suite.smapiservice)
    f.initialize()

    suite.run(f)