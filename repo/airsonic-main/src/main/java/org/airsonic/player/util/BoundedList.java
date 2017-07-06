/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.airsonic.player.util;

import java.util.LinkedList;

/**
 * Simple implementation of a bounded list. If the maximum size is reached, adding a new element will
 * remove the first element in the list.
 *
 * @author Sindre Mehus
 * @version $Revision: 1.1 $ $Date: 2005/05/09 20:01:25 $
 */
public class BoundedList<E> extends LinkedList<E> {
    private int maxSize;

    /**
     * Creates a new bounded list with the given maximum size.
     * @param maxSize The maximum number of elements the list may hold.
     */
    public BoundedList(int maxSize) {
        this.maxSize = maxSize;
    }

    /**
     * Adds an element to the tail of the list. If the list is full, the first element is removed.
     * @param e The element to add.
     * @return Always <code>true</code>.
     */
    public boolean add(E e) {
        if (isFull()) {
            removeFirst();
        }
        return super.add(e);
    }

    /**
     * Adds an element to the head of list. If the list is full, the last element is removed.
     * @param e The element to add.
     */
    public void addFirst(E e) {
        if (isFull()) {
            removeLast();
        }
        super.addFirst(e);
    }

    /**
     * Returns whether the list if full.
     * @return Whether the list is full.
     */
    private boolean isFull() {
        return size() == maxSize;
    }
}
