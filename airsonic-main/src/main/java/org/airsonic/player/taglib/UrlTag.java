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

import org.airsonic.player.filter.ParameterDecodingFilter;
import org.airsonic.player.util.StringUtil;
import org.apache.commons.lang.CharUtils;
import org.apache.taglibs.standard.tag.common.core.UrlSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyTagSupport;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates a URL with optional query parameters. Similar to 'c:url', but
 * you may specify which character encoding to use for the URL query
 * parameters. If no encoding is specified, the following steps are performed:
 * <ul>
 * <li>Parameter values are encoded as the hexadecimal representation of the UTF-8 bytes of the original string.</li>
 * <li>Parameter names are prepended with the suffix "Utf8Hex"</li>
 * <li>Note: Nothing is done with the parameter name or value if the value only contains ASCII alphanumeric characters.</li>
 * </ul>
 * <p/>
 * (The problem with c:url is that is uses the same encoding as the http response,
 * but most(?) servlet container assumes that ISO-8859-1 is used.)
 *
 * @author Sindre Mehus
 */
public class UrlTag extends BodyTagSupport {

    private String DEFAULT_ENCODING = "Utf8Hex";
    private static final Logger LOG = LoggerFactory.getLogger(UrlTag.class);

    private String var;
    private String value;
    private String encoding = DEFAULT_ENCODING;
    private List<Parameter> parameters = new ArrayList<Parameter>();

    public int doStartTag() throws JspException {
        parameters.clear();
        return EVAL_BODY_BUFFERED;
    }

    public int doEndTag() throws JspException {

        // Rewrite and encode the url.
        String result = formatUrl();

        // Store or print the output
        if (var != null)
            pageContext.setAttribute(var, result, PageContext.PAGE_SCOPE);
        else {
            try {
                pageContext.getOut().print(result);
            } catch (IOException x) {
                throw new JspTagException(x);
            }
        }
        return EVAL_PAGE;
    }

    private String formatUrl() throws JspException {
        String baseUrl = UrlSupport.resolveUrl(value, null, pageContext);

        StringBuffer result = new StringBuffer();
        result.append(baseUrl);
        if (!parameters.isEmpty()) {
            result.append('?');

            for (int i = 0; i < parameters.size(); i++) {
                Parameter parameter = parameters.get(i);
                try {
                    result.append(parameter.getName());
                    if (isUtf8Hex() && !isAsciiAlphaNumeric(parameter.getValue())) {
                        result.append(ParameterDecodingFilter.PARAM_SUFFIX);
                    }

                    result.append('=');
                    if (parameter.getValue() != null) {
                        result.append(encode(parameter.getValue()));
                    }
                    if (i < parameters.size() - 1) {
                        result.append("&");
                    }

                } catch (UnsupportedEncodingException x) {
                    throw new JspTagException(x);
                }
            }
        }
        return result.toString();
    }

    private String encode(String s) throws UnsupportedEncodingException {
        if (isUtf8Hex()) {
            if (isAsciiAlphaNumeric(s)) {
                return s;
            }

            try {
                return StringUtil.utf8HexEncode(s);
            } catch (Exception x) {
                LOG.error("Failed to utf8hex-encode the string '" + s + "'.", x);
                return s;
            }
        }

        return URLEncoder.encode(s, encoding);
    }

    private boolean isUtf8Hex() {
        return DEFAULT_ENCODING.equals(encoding);
    }

    private boolean isAsciiAlphaNumeric(String s) {
        if (s == null) {
            return true;
        }

        for (int i = 0; i < s.length(); i++) {
            if (!CharUtils.isAsciiAlphanumeric(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public void release() {
        var = null;
        value = null;
        encoding = DEFAULT_ENCODING;
        parameters.clear();
        super.release();
    }

    public void addParameter(String name, String value) {
        parameters.add(new Parameter(name, value));
    }

    public String getVar() {
        return var;
    }

    public void setVar(String var) {
        this.var = var;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * A URL query parameter.
     */
    private static class Parameter {
        private String name;
        private String value;

        private Parameter(String name, String value) {
            this.name = name;
            this.value = value;
        }

        private String getName() {
            return name;
        }

        private String getValue() {
            return value;
        }
    }
}
