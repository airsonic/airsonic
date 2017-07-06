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

import org.apache.commons.lang.ObjectUtils;

import java.io.Serializable;

/**
 * @author Sindre Mehus
 */
public class Pair<S, T> implements Serializable {

    private final S first;
    private final T second;

    public static <S, T> Pair<S, T> create(S first, T second) {
        return new Pair<S, T>(first, second);
    }

    public Pair(S first, T second) {
        this.first = first;
        this.second = second;
    }

    public S getFirst() {
        return first;
    }

    public T getSecond() {
        return second;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Pair pair = (Pair) o;

        return ObjectUtils.equals(first, pair.first) && ObjectUtils.equals(second, pair.second);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hashCode(first) * ObjectUtils.hashCode(second);
    }
}
