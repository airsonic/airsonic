package org.airsonic.player.service.search;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class SearchServiceUtilitiesTestCase {

    SearchServiceUtilities utilities = new SearchServiceUtilities();

    @Test
    public void testNextInt() {
        assertNotNull(utilities.nextInt.apply(100));
    }

}
