/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2009 (C) Sindre Mehus
 */
package net.sourceforge.subsonic.taglib;

import org.radeox.api.engine.*;
import org.radeox.api.engine.context.*;
import org.radeox.engine.*;
import org.radeox.engine.context.*;
import org.apache.commons.lang.*;

import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;
import java.io.*;

/**
 * Renders a Wiki text with markup to HTML, using the Radeox render engine.
 *
 * @author Sindre Mehus
 */
public class WikiTag extends BodyTagSupport {

    private static final RenderContext RENDER_CONTEXT = new BaseRenderContext();
    private static final RenderEngine RENDER_ENGINE = new BaseRenderEngine();

    private String text;

    public int doStartTag() throws JspException {
        return EVAL_BODY_BUFFERED;
    }

    public int doEndTag() throws JspException {
        String result;
        synchronized (RENDER_ENGINE) {
            result = RENDER_ENGINE.render(StringEscapeUtils.unescapeXml(text), RENDER_CONTEXT);
        }
        try {
            pageContext.getOut().print(result);
        } catch (IOException x) {
            throw new JspTagException(x);
        }
        return EVAL_PAGE;
    }

    public void release() {
        text = null;
        super.release();
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
