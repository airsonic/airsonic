<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>
<%--
  ~ This file is part of Airsonic.
  ~
  ~  Airsonic is free software: you can redistribute it and/or modify
  ~  it under the terms of the GNU General Public License as published by
  ~  the Free Software Foundation, either version 3 of the License, or
  ~  (at your option) any later version.
  ~
  ~  Airsonic is distributed in the hope that it will be useful,
  ~  but WITHOUT ANY WARRANTY; without even the implied warranty of
  ~  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  ~  GNU General Public License for more details.
  ~
  ~  You should have received a copy of the GNU General Public License
  ~  along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.
  ~
  ~  Copyright 2014 (C) Sindre Mehus
  --%>

<%--@elvariable id="model" type="java.util.Map"--%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <script type="text/javascript" src="<c:url value='/script/utils.js'/>"></script>

    <script type="text/javascript">
        var image;
        var id;
        var duration;
        var timer;
        var offset;
        var step;
        var size = 120;

        function startPreview(img, id, duration) {
            stopPreview();
            image = $(img);
            step = Math.max(5, Math.round(duration / 50));
            offset = step;
            this.id = id;
            this.duration = duration;
            updatePreview();
            timer = window.setInterval(updatePreview, 1000);
        }

        function updatePreview() {
            image.attr("src", "coverArt.view?id=" + id + "&size=" + size + "&offset=" + offset);
            offset += step;
            if (offset > duration) {
                stopPreview();
            }
        }

        function stopPreview() {
            if (timer != null) {
                window.clearInterval(timer);
                timer = null;
            }
            if (image != null) {
                image.attr("src", "coverArt.view?id=" + id + "&size=" + size);
            }
        }
        function showAllAlbums() {
            window.location.href = updateQueryStringParameter(window.location.href, "showAll", "1");
        }
    </script>

    <style type="text/css">
        .duration {
            position: absolute;
            bottom: 3px;
            right: 3px;
            color: #d3d3d3;
            background-color: black;
            opacity: 0.8;
            padding-right:3px;
            padding-left:3px;
        }
    </style>

</head><body class="mainframe bgcolor1">

<h1 style="float:left">
    <span style="vertical-align: middle;">
        <c:forEach items="${model.ancestors}" var="ancestor">
            <sub:url value="main.view" var="ancestorUrl">
                <sub:param name="id" value="${ancestor.id}"/>
            </sub:url>
            <a href="${ancestorUrl}">${fn:escapeXml(ancestor.name)}</a> &raquo;
        </c:forEach>
        ${fn:escapeXml(model.dir.name)}
    </span>
</h1>

<%@ include file="viewSelector.jsp" %>
<div style="clear:both;padding-bottom:2em"></div>

<table class="music">
    <c:forEach items="${model.subDirs}" var="subDir" varStatus="loopStatus">
        <tr><td class="truncate" colspan="9"><a href="main.view?id=${subDir.id}" title="${fn:escapeXml(subDir.name)}">${fn:escapeXml(subDir.name)}</a></td></tr>
    </c:forEach>
    <c:if test="${model.viewAsList}">
        <c:forEach items="${model.files}" var="child">
            <c:url value="/videoPlayer.view" var="videoUrl">
                <c:param name="id" value="${child.id}"/>
            </c:url>
            <tr>
                <c:import url="playButtons.jsp">
                    <c:param name="id" value="${child.id}"/>
                    <c:param name="playEnabled" value="${model.user.streamRole and not model.partyMode}"/>
                    <c:param name="downloadEnabled" value="${model.user.downloadRole and not model.partyMode}"/>
                    <c:param name="video" value="${child.video and model.player.web}"/>
                    <c:param name="asTable" value="true"/>
                </c:import>
                <td class="truncate">
                    <a href="${videoUrl}"><span class="songTitle" title="${fn:escapeXml(child.name)}">${fn:escapeXml(child.name)}</span></a>
                </td>
                <td class="fit rightalign detail">${child.year}</td>
                <td class="fit rightalign detail">${fn:toLowerCase(child.format)}</td>
                <td class="fit rightalign detail"><sub:formatBytes bytes="${child.fileSize}"/></td>
                <td class="fit rightalign detail">${child.durationString}</td>
            </tr>
        </c:forEach>
    </c:if>
</table>
<c:if test="${model.thereIsMore && fn:length(model.subDirs) gt 0}">
    <input id="showAllButton" class="albumOverflowButton" type="button" value="<fmt:message key='main.showall'/>" onclick="showAllAlbums()">
</c:if>

<div style="clear:both;height:1.5em"></div>

<c:if test="${not model.viewAsList}">
    <c:forEach items="${model.files}" var="child">
        <c:url value="/videoPlayer.view" var="videoUrl">
            <c:param name="id" value="${child.id}"/>
        </c:url>
        <c:url value="/coverArt.view" var="coverArtUrl">
            <c:param name="id" value="${child.id}"/>
            <c:param name="size" value="120"/>
        </c:url>

        <div class="albumThumb">
            <div class="coverart dropshadow" style="width:213px">
                <div style="position:relative">
                    <div>
                        <a href="${videoUrl}"><img src="${coverArtUrl}" height="120" width="213" alt=""
                                                   onmouseover="startPreview(this, ${child.id}, ${child.durationSeconds})"
                                                   onmouseout="stopPreview()"></a>
                    </div>
                    <div class="detail duration">${child.durationString}</div>
                </div>
                <div class="caption1" title="${fn:escapeXml(child.name)}"><a href="${videoUrl}" title="${fn:escapeXml(child.name)}">${fn:escapeXml(child.name)}</a></div>
            </div>
        </div>
    </c:forEach>
</c:if>

</body>
</html>
