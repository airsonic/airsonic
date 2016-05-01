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

import net.sourceforge.subsonic.util.*;
import org.springframework.web.servlet.support.*;

import javax.servlet.http.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;
import java.io.*;
import java.util.*;

/**
 * Converts a byte-count to a formatted string suitable for display to the user, with respect
 * to the current locale.
 * <p/>
 * For instance:
 * <ul>
 * <li><code>format(918)</code> returns <em>"918 B"</em>.</li>
 * <li><code>format(98765)</code> returns <em>"96 KB"</em>.</li>
 * <li><code>format(1238476)</code> returns <em>"1.2 MB"</em>.</li>
 * </ul>
 * This class assumes that 1 KB is 1024 bytes.
 *
 * @author Sindre Mehus
 */
public class FormatBytesTag extends BodyTagSupport {

    private long bytes;

    public int doStartTag() throws JspException {
        return EVAL_BODY_BUFFERED;
    }

    public int doEndTag() throws JspException {
        Locale locale = RequestContextUtils.getLocale((HttpServletRequest) pageContext.getRequest());
        String result = StringUtil.formatBytes(bytes, locale);

        try {
            pageContext.getOut().print(result);
        } catch (IOException x) {
            throw new JspTagException(x);
        }
        return EVAL_PAGE;
    }

    public void release() {
        bytes = 0L;
        super.release();
    }

    public long getBytes() {
        return bytes;
    }

    public void setBytes(long bytes) {
        this.bytes = bytes;
    }
}
