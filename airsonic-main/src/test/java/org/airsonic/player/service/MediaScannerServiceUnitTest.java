package org.airsonic.player.service;

import org.airsonic.player.domain.MediaLibraryStatistics;
import org.airsonic.player.service.search.IndexManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MediaScannerServiceUnitTest {

    @InjectMocks
    MediaScannerService mediaScannerService;

    @Mock
    IndexManager indexManager;

    @Test
    public void neverScanned() {
        when(indexManager.getStatistics()).thenReturn(null);
        assertTrue(mediaScannerService.neverScanned());

        when(indexManager.getStatistics()).thenReturn(new MediaLibraryStatistics());
        assertFalse(mediaScannerService.neverScanned());
    }
}