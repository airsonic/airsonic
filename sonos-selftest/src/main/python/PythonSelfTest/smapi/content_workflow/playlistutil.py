import selftestinit #ignore @UnusedImport for packaging 
from sonos.workflow.fixture import WorkflowTestFixture
from sonos.smapi.smapiservice import SMAPIService, PLAYLISTCONTAINERTYPE, PLAYLISTROOT
from sonos.smapi.smapiclient import SMAPIClient, CR_STANDARD_RANGE
from sonos.smapi.smapiexceptions import ItemNotFound, SonosError

class Playlist(WorkflowTestFixture, SMAPIClient, SMAPIService):
    minStuffingRequirements = 10
    def verifyCreateUsingInvalidParametersRaisesException(self, client, containerType, title, parentID, seedID, message):
        exception = False
        try:
            response = client.createPlaylistContainer(containerType, title, parentID, "", self)
            if type(response) is SonosError:
                sonos_error_code = response.webfault.fault.detail.SonosError
                self.verifyIsNotNoneOrFailCase(sonos_error_code, "A SonosError should contain a SonosError code "
                                                                 "inside.")
                exception = True
        except ItemNotFound:
            exception = True
        self.verifyTrueOrFailCase(exception, message)


    def createUsingValidParameters(self, client, containerType, title, parentID, seedID, metadataID):
        createContainer = client.createPlaylistContainer(containerType, title, parentID, seedID, self)
        #Verify that container was created
        self.verifyIsNotNoneOrStop(createContainer, "createContainer should return something other than None.")

        if metadataID == "":
            metadataID = createContainer.id

        #Get metadata for for further verifications
        (does_item_exist, _) = client.getMetadata(metadataID, CR_STANDARD_RANGE.index, CR_STANDARD_RANGE.count,
                                                  self)#ignore @UndefinedVariable for named tuple
        self.verifyIsNotNoneOrStop(does_item_exist, "getMetadata should return something other than None.")
        return (does_item_exist, createContainer.id)

    def verifyLastUpdate(self, client, initialValue):
        updatedValue = client.getLastUpdate(self)
        self.verifyNotEqualOrFailCase(initialValue, updatedValue, "getLastUpdate checksum values should not remain the "
                                                                  "same after a change was made to favorites.")
        return updatedValue

    def verifySeedContent(self, client, contentID, originalTotal):
        #Get the metadata to have items count from this playlist for further verifications
        (seed_response,_) = client.getMetadata(contentID, CR_STANDARD_RANGE.index, CR_STANDARD_RANGE.count,
                                               self)#ignore @UndefinedVariable for named tuple
        self.verifyIsNotNoneOrStop(seed_response,"getMetadata should return something other than None.")
        #Verify that original playlist and seeded have the same amount of items
        self.verifyEqualOrFailCase(originalTotal, seed_response.total, "The seed playlist and newly created playlist "
                                                                       "should have the same quantity of items inside.")

    def buildVerificationData(self, container):
        info_collection = []
        #Get all the data to sort out later at ones
        for containerType in container.Items:
            info_collection.append(containerType.id)
            info_collection.append(containerType.title)
        return info_collection

    def verifyDeleteViaLastUpdate(self, client, container, initial_get_last_update):
        secondary_get_last_update = self.verifyLastUpdate(client, initial_get_last_update)
        client.deletePlaylistContainer(container, self)
        self.verifyLastUpdate(client, secondary_get_last_update)

    def verifyDeletableAndDelete(self, client, container, initial_get_last_update):
        secondary_get_last_update = self.verifyLastUpdate(client, initial_get_last_update)
        #Verify that container is created as deletable
        self.verifyEqualOrFailCase(str(container._readOnly).upper(),    'FALSE', "Newly created playlists should have"
                                                                                 " their readOnly property set to "
                                                                                 "False.")
        self.verifyEqualOrFailCase(str(container._renameable).upper(),  'TRUE',  "Newly created playlists should have"
                                                                                 " their renameable property set to "
                                                                                 "True.")
        client.deletePlaylistContainer(container.id, self)
        self.verifyLastUpdate(client, secondary_get_last_update)

    def deleteInternalContainer(self, client, containerID, level):
        #Get metadata on the playlists before delete to capture total amount
        (before_delete, _) = client.getMetadata(level, CR_STANDARD_RANGE.index, CR_STANDARD_RANGE.count,
                                                self)#ignore @UndefinedVariable for named tuple
        self.verifyIsNotNoneOrStop(before_delete, "getMetadata should return something other than None.")
        client.deletePlaylistContainer(containerID, self)
        #Get metadata on the playlists after delete to capture total amount
        (after_delete,_) = client.getMetadata(level, CR_STANDARD_RANGE.index, CR_STANDARD_RANGE.count,
                                              self)#ignore @UndefinedVariable for named tuple
        self.verifyIsNotNoneOrStop(after_delete, "getMetadata should return something other than None.")
        return (before_delete.total, after_delete.total)

    def findNonEditablePlaylist(self, client):
        (playlists, _) = client.getMetadata(PLAYLISTROOT, CR_STANDARD_RANGE.index, CR_STANDARD_RANGE.count,
                                            self)#ignore @UndefinedVariable for named tuple
        self.verifyIsNotNoneOrStop(playlists, "getMetadata should return something other than None.")

        for mediaCollection in playlists.Items:
            if hasattr(mediaCollection,'_readOnly') and str(mediaCollection._readOnly).upper() == 'TRUE':
                return mediaCollection.id
        return False

    def createPlaylistReorderContainer(self, service, client):
        content_id = service.get_test_album_id()
        self.verifyTrueOrSkip(content_id, "Service must provide a valid album ([Test Content]:album in the config file)"
                                          " for this test to run.")
        content_title = service.get_search_term_album()
        #Get the metadata to have items count from this playlist for further verifications
        (seed_response,_) = client.getMetadata(content_id, CR_STANDARD_RANGE.index, CR_STANDARD_RANGE.count,
                                               self)#ignore @UndefinedVariable for named tuple
        self.verifyIsNotNoneOrStop(seed_response, "getMetadata should return something other than None.")
        #Verify that we have enough tracks in the album to run the test
        self.verifyGreaterEqualOrStop(int(seed_response.total),self.minStuffingRequirements, "Service must provide an "
                                                                                             "album ([Test Content]:"
                                                                                             "album in the config file)"
                                                                                             " with 10+ tracks in the"
                                                                                             " config file for this "
                                                                                             "test to run.")

        self.verifyTrueOrSkip(content_id and content_title,"Service must provide a valid album title and ID ([Test "
                                                           "Content]:album/album title in the config file)for this "
                                                           "test to run")

        (does_item_exist,createContainerId)  = self.createUsingValidParameters(client, PLAYLISTCONTAINERTYPE,
                                                                               "SelfTest Automation Playlist Reorder", PLAYLISTROOT, content_id, "")

        #Verify that original playlist and seeded have the same amount of items
        self.verifySeedContent(client, content_id, does_item_exist.total)
        return (does_item_exist,createContainerId)

    def getTrackIdsTestSet(self, client, containerID, reorder):
        tracksToMoveId = []

        #Get the metadata to have items count from this playlist for further verifications
        (response,_) = client.getMetadata(containerID, CR_STANDARD_RANGE.index, CR_STANDARD_RANGE.count,
                                          self)#ignore @UndefinedVariable for named tuple
        self.verifyIsNotNoneOrStop(response, "getMetadata should return something other than None.")
        for position in reorder:
            tracksToMoveId.append(response.Items[position].id)
        return (tracksToMoveId, len(reorder))

    def getTracksMovedNewIndex(self, positionToMoveTo, moveSize, direction):
        tracksMovedIndex = []
        if direction == "UP":
            #Since we count from 0
            positionToMoveTo -= 1
            while moveSize > 0:
                tracksMovedIndex.append(positionToMoveTo)
                moveSize -= 1
                positionToMoveTo -= 1
            tracksMovedIndex.reverse()
        else:
            while moveSize > 0:
                tracksMovedIndex.append(positionToMoveTo)
                moveSize -= 1
                positionToMoveTo += 1
        return tracksMovedIndex

    def convertRangeToLinear(self, reorderrange):
        linearRange = []
        rangeList = reorderrange.split('-')
        linearRange.append(int(rangeList[0]))
        data = int(rangeList[0])
        rangeLen = int(rangeList[1]) - int(rangeList[0])
        for rangeList[0] in range(rangeLen):
            data += 1
            linearRange.append(data)
        return linearRange