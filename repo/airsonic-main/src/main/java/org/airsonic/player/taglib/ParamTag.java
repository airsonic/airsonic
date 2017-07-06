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
package org.airsonic.player.taglib;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * A tag representing an URL query parameter.
 *
 * @see ParamTag
 * @author Sindre Mehus
 */
public class ParamTag extends TagSupport {

    private String name;
    private String value;

    public int doEndTag() throws JspTagException {

        // Add parameter name and value to surrounding 'url' tag.
        UrlTag tag = (UrlTag) findAncestorWithClass(this, UrlTag.class);
        if (tag == null) {
            throw new JspTagException("'sub:param' tag used outside 'sub:url'");
        }
        tag.addParameter(name, value);
        return EVAL_PAGE;
    }

    public void release() {
        name = null;
        value = null;
        super.release();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
