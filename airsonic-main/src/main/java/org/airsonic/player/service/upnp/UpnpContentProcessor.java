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

  Copyright 2017 (C) Airsonic Authors
  Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
*/
package org.airsonic.player.service.upnp;

import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.ParamSearchResult;
import org.airsonic.player.util.Util;
import org.fourthline.cling.support.contentdirectory.DIDLParser;
import org.fourthline.cling.support.model.BrowseResult;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.SortCriterion;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.container.StorageFolder;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.ParameterizedType;
import java.util.List;

/**
 * @author Allen Petersen
 * @version $Id$
 */
public abstract class UpnpContentProcessor<T extends Object, U extends Object> {

    @Autowired
    private DispatchingContentDirectory dispatchingContentDirectory;

    protected String rootTitle;
    protected String rootId;

    /**
     * Browses the root metadata for a type.
     */
    public BrowseResult browseRootMetadata() throws Exception {
        DIDLContent didl = new DIDLContent();
        didl.addContainer(createRootContainer());
        return createBrowseResult(didl, 1, 1);
    }

    public Container createRootContainer() throws Exception {
        Container container = new StorageFolder();
        container.setId(getRootId());
        container.setTitle(getRootTitle());

        int childCount = getAllItemsSize();
        container.setChildCount(childCount);
        container.setParentID(DispatchingContentDirectory.CONTAINER_ID_ROOT);
        return container;
    }

    /**
     * Browses the top-level content of a type.
     */
    public BrowseResult browseRoot(String filter, long firstResult, long maxResults, SortCriterion[] orderBy) throws Exception {
        DIDLContent didl = new DIDLContent();
        List<T> allItems = getAllItems();
        if (filter != null) {
            // filter items (not implemented yet)
        }
        if (orderBy != null) {
            // sort items (not implemented yet)
        }
        List<T> selectedItems = Util.subList(allItems, firstResult, maxResults);
        for (T item : selectedItems) {
            addItem(didl, item);
        }

        return createBrowseResult(didl, (int) didl.getCount(), allItems.size());
    }

    /**
     * Browses metadata for a child.
     */
    public BrowseResult browseObjectMetadata(String id) throws Exception {
        T item = getItemById(id);
        DIDLContent didl = new DIDLContent();
        addItem(didl, item);
        return createBrowseResult(didl, 1, 1);
    }

    /**
     * Browses a child of the container.
     */
    public BrowseResult browseObject(String id, String filter, long firstResult, long maxResults, SortCriterion[] orderBy) throws Exception {
        T item = getItemById(id);
        List<U> allChildren = getChildren(item);
        if (filter != null) {
            // filter items (not implemented yet)
        }
        if (orderBy != null) {
            // sort items (not implemented yet)
        }
        List<U> selectedChildren = Util.subList(allChildren, firstResult, maxResults);

        DIDLContent didl = new DIDLContent();
        for (U child : selectedChildren) {
            addChild(didl, child);
        }
        return createBrowseResult(didl, selectedChildren.size(), allChildren.size());
    }

    protected BrowseResult createBrowseResult(DIDLContent didl, int count, int totalMatches) throws Exception {
        return new BrowseResult(new DIDLParser().generate(didl), count, totalMatches);
    }

    public BrowseResult searchByName(String name,
                                     long firstResult, long maxResults,
                                     SortCriterion[] orderBy) {
        DIDLContent didl = new DIDLContent();

        Class clazz =  (Class) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];

        try {
            List<MusicFolder> allFolders = getDispatchingContentDirectory().getSettingsService().getAllMusicFolders();
            ParamSearchResult<T> result = getDispatcher().getSearchService().searchByName(name, (int) firstResult, (int) maxResults, allFolders, clazz);
            List<T> selectedItems = result.getItems();
            for (T item : selectedItems) {
                addItem(didl, item);
            }

            return createBrowseResult(didl, (int) didl.getCount(), result.getTotalHits());
        } catch (Exception e) {
            return null;
        }
    }

    public DispatchingContentDirectory getDispatchingContentDirectory() {
        return dispatchingContentDirectory;
    }
    public void setDispatchingContentDirectory(DispatchingContentDirectory dispatchingContentDirectory) {
        this.dispatchingContentDirectory = dispatchingContentDirectory;
    }
    public DispatchingContentDirectory getDispatcher() {
        return getDispatchingContentDirectory();
    }

    public void addItem(DIDLContent didl, T item) {
        didl.addContainer(createContainer(item));
    }

    // this can probably be optimized in some cases
    public int getAllItemsSize() throws Exception {
        return getAllItems().size();
    }

    public abstract Container createContainer(T item);

    public abstract List<T> getAllItems() throws Exception;

    public abstract T getItemById(String id);

    public abstract List<U> getChildren(T item) throws Exception;

    public abstract void addChild(DIDLContent didl, U child);

    public String getRootTitle() {
        return rootTitle;
    }
    public void setRootTitle(String rootTitle) {
        this.rootTitle = rootTitle;
    }
    public String getRootId() {
        return rootId;
    }
    public void setRootId(String rootId) {
        this.rootId = rootId;
    }
}

