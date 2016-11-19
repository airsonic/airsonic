<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<%--
  ~ This file is part of Libresonic.
  ~
  ~ Libresonic is free software: you can redistribute it and/or modify
  ~ it under the terms of the GNU General Public License as published by
  ~ the Free Software Foundation, either version 3 of the License, or
  ~ (at your option) any later version.
  ~
  ~ Libresonic is distributed in the hope that it will be useful,
  ~ but WITHOUT ANY WARRANTY; without even the implied warranty of
  ~ MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  ~ GNU General Public License for more details.
  ~
  ~ You should have received a copy of the GNU General Public License
  ~ along with Libresonic.  If not, see <http://www.gnu.org/licenses/>.
  ~
  ~ Copyright 2013 (C) Sindre Mehus
  --%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <script type="text/javascript" src="<c:url value="/script/scripts-2.0.js"/>"></script>
</head>

<body class="mainframe bgcolor1">
<script type="text/javascript" src="<c:url value="/script/wz_tooltip.js"/>"></script>
<script type="text/javascript" src="<c:url value="/script/tip_balloon.js"/>"></script>

<c:import url="settingsHeader.jsp">
    <c:param name="cat" value="dlna"/>
    <c:param name="toast" value="${model.toast}"/>
</c:import>

<form method="post" action="dlnaSettings.view">

    <div>
        <input type="checkbox" name="dlnaEnabled" id="dlnaEnabled" class="checkbox"
               <c:if test="${model.dlnaEnabled}">checked="checked"</c:if>/>
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

    <c:set var="licenseInfo" value="${model.licenseInfo}"/>
    <%@ include file="licenseNotice.jsp" %>

    <p>
        <input type="submit" value="<fmt:message key="common.save"/>" style="margin-right:0.3em">
        <input type="button" value="<fmt:message key="common.cancel"/>" onclick="location.href='nowPlaying.view'">
    </p>

</form>

</body></html>