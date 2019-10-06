/*
 * This file is part of Airsonic.
 *
 *  Airsonic is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Airsonic is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Copyright 2014 (C) Sindre Mehus
 */

package org.airsonic.player.domain;

import junit.framework.TestCase;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SortableArtistTestCase extends TestCase {

    private Collator collator;

    @Override
    public void setUp() {
        collator = Collator.getInstance(Locale.US);
    }

    public void testSorting() {
        List<TestSortableArtist> artists = new ArrayList<TestSortableArtist>();

        artists.add(new TestSortableArtist("ABBA"));
        artists.add(new TestSortableArtist("Abba"));
        artists.add(new TestSortableArtist("abba"));
        artists.add(new TestSortableArtist("ACDC"));
        artists.add(new TestSortableArtist("acdc"));
        artists.add(new TestSortableArtist("ACDC"));
        artists.add(new TestSortableArtist("abc"));
        artists.add(new TestSortableArtist("ABC"));

        Collections.sort(artists);
        assertEquals("[abba, Abba, ABBA, abc, ABC, acdc, ACDC, ACDC]", artists.toString());
    }

    public void testSortingWithAccents() {
        List<TestSortableArtist> artists = new ArrayList<TestSortableArtist>();

        TestSortableArtist a1 = new TestSortableArtist("Sea");
        TestSortableArtist a2 = new TestSortableArtist("SEB");
        TestSortableArtist a3 = new TestSortableArtist("Seb");
        TestSortableArtist a4 = new TestSortableArtist("S\u00e9b");
        TestSortableArtist a5 = new TestSortableArtist("Sed");
        TestSortableArtist a6 = new TestSortableArtist("See");

        assertTrue(a1.compareTo(a1) == 0);
        assertTrue(a1.compareTo(a2) < 0);
        assertTrue(a1.compareTo(a3) < 0);
        assertTrue(a1.compareTo(a4) < 0);
        assertTrue(a1.compareTo(a5) < 0);
        assertTrue(a1.compareTo(a6) < 0);

        assertTrue(a2.compareTo(a1) > 0);
        assertTrue(a3.compareTo(a1) > 0);
        assertTrue(a4.compareTo(a1) > 0);
        assertTrue(a5.compareTo(a1) > 0);
        assertTrue(a6.compareTo(a1) > 0);

        assertTrue(a4.compareTo(a1) > 0);
        assertTrue(a4.compareTo(a2) > 0);
        assertTrue(a4.compareTo(a3) > 0);
        assertTrue(a4.compareTo(a4) == 0);
        assertTrue(a4.compareTo(a5) < 0);
        assertTrue(a4.compareTo(a6) < 0);

        artists.add(a1);
        artists.add(a2);
        artists.add(a3);
        artists.add(a4);
        artists.add(a5);
        artists.add(a6);

        Collections.shuffle(artists);
        Collections.sort(artists);
        assertEquals("[Sea, Seb, SEB, S\u00e9b, Sed, See]", artists.toString());
    }

    public void testCollation() {
        List<TestSortableArtist> artists = new ArrayList<TestSortableArtist>();

        artists.add(new TestSortableArtist("p\u00e9ch\u00e9"));
        artists.add(new TestSortableArtist("peach"));
        artists.add(new TestSortableArtist("p\u00eache"));

        Collections.sort(artists);
        assertEquals("[peach, p\u00e9ch\u00e9, p\u00eache]", artists.toString());
    }

    private class TestSortableArtist extends MusicIndex.SortableArtist {

        public TestSortableArtist(String sortableName) {
            super(sortableName, sortableName, collator);
        }

        @Override
        public String toString() {
            return getSortableName();
        }
    }
}