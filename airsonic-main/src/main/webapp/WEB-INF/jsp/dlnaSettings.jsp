<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<%--
  ~ This file is part of Airsonic.
  ~
  ~ Airsonic is free software: you can redistribute it and/or modify
  ~ it under the terms of the GNU General Public License as published by
  ~ the Free Software Foundation, either version 3 of the License, or
  ~ (at your option) any later version.
  ~
  ~ Airsonic is distributed in the hope that it will be useful,
  ~ but WITHOUT ANY WARRANTY; without even the implied warranty of
  ~ MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  ~ GNU General Public License for more details.
  ~
  ~ You should have received a copy of the GNU General Public License
  ~ along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.
  ~
  ~ Copyright 2013 (C) Sindre Mehus
  --%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <script type="text/javascript" src="<c:url value="/script/utils.js"/>"></script>
</head>

<body class="mainframe bgcolor1">
<script type="text/javascript" src="<c:url value="/script/wz_tooltip.js"/>"></script>
<script type="text/javascript" src="<c:url value="/script/tip_balloon.js"/>"></script>

<c:import url="settingsHeader.jsp">
    <c:param name="cat" value="dlna"/>
    <c:param name="toast" value="${settings_toast}"/>
</c:import>

<form method="post" action="dlnaSettings.view">
    <sec:csrfInput />

    <div>
        <input type="checkbox" name="dlnaEnabled" id="dlnaEnabled" {model.dlnaEnabled? "checked": ""}/>
        <label for="dlnaEnabled"><fmt:message key="dlnasettings.enabled"/></label>
    </div>
    <p class="detail" style="width:60%;white-space:normal">
        <fmt:message key="dlnasettings.description"/>
    </p>

    <div>
        <fmt:message key="dlnasettings.servername"/>
        <input name="dlnaServerName" id="dlnaServerName" size="40"
               value="<c:out value="${model.dlnaServerName}" escapeXml="true"/>"/>
    </div>
    <p class="detail" style="width:60%;white-space:normal;padding-top:0">
        <fmt:message key="dlnasettings.servername.description"/>
    </p>

    <div>
        <label for="dlnaBaseLANURL" ><fmt:message key="dlnasettings.baselanurl"/></label>
        <input type="text" size="50" name="dlnaBaseLANURL" id="dlnaBaseLANURL"
               value="<c:out value="${model.dlnaBaseLANURL}" />" />
    </div>
    <p class="detail" style="width:60%;white-space:normal">
        <fmt:message key="dlnasettings.lanurl.description"/>
    </p>

    <p>
        <input type="submit" value="<fmt:message key="common.save"/>" style="margin-right:0.3em">
        <a href='nowPlaying.view'><input type="button" value="<fmt:message key="common.cancel"/>"></a>
    </p>

</form>

</body></html>
