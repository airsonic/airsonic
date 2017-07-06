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
  ~  Copyright 2015 (C) Sindre Mehus
  --%>

<%--@elvariable id="model" type="java.util.Map"--%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
</head><body class="mainframe bgcolor1">

<h1 style="padding-bottom: 1em">
    <img src="<spring:theme code="podcastLargeImage"/>" alt="">
    <span style="vertical-align: middle"><fmt:message key="podcastreceiver.title"/></span>
</h1>

<c:if test="${empty model.channels}">
    <p><em><fmt:message key="podcastreceiver.empty"/></em></p>
</c:if>

<c:forEach items="${model.channels}" var="channel" varStatus="loopStatus">

    <c:set var="caption2">
        <fmt:message key="podcastreceiver.episodes"><fmt:param value="${fn:length(channel.value)}"/></fmt:message>
    </c:set>
    <div class="albumThumb">
        <c:import url="coverArt.jsp">
            <c:param name="podcastChannelId" value="${channel.key.id}"/>
            <c:param name="coverArtSize" value="200"/>
            <c:param name="caption1" value="${fn:escapeXml(channel.key.title)}"/>
            <c:param name="caption2" value="${caption2}"/>
            <c:param name="captionCount" value="2"/>
            <c:param name="showLink" value="true"/>
            <c:param name="appearAfter" value="${loopStatus.count * 30}"/>
        </c:import>
    </div>

</c:forEach>

<c:if test="${not empty model.newestEpisodes}">
    <h2 style="margin-top:1em"><fmt:message key="podcastreceiver.newestepisodes"/></h2>
    <table class="music indent">
        <c:forEach items="${model.newestEpisodes}" var="episode" varStatus="i">
            <tr>
                <c:import url="playButtons.jsp">
                    <c:param name="id" value="${episode.mediaFileId}"/>
                    <c:param name="podcastEpisodeId" value="${episode.id}"/>
                    <c:param name="playEnabled" value="${model.user.streamRole and not model.partyMode}"/>
                    <c:param name="addEnabled" value="${model.user.streamRole and not model.partyMode}"/>
                    <c:param name="asTable" value="true"/>
                    <c:param name="onPlay" value="top.playQueue.onPlayNewestPodcastEpisode(${i.index})"/>
                </c:import>
                <c:set var="channelTitle" value="${model.channelMap[episode.channelId].title}"/>

                <td class="truncate">
                    <span title="${episode.title}" class="songTitle">${episode.title}</span>
                </td>

                <td class="truncate">
                    <a href="podcastChannel.view?id=${episode.channelId}"><span class="detail" title="${channelTitle}">${channelTitle}</span></a>
                </td>

                <td class="fit">
                    <span class="detail">${episode.duration}</span>
                </td>

                <td class="fit">
                    <span class="detail"><fmt:formatDate value="${episode.publishDate}" dateStyle="medium"/></span>
                </td>

            </tr>
        </c:forEach>
    </table>
</c:if>

<table style="padding-top:1em"><tr>
    <c:if test="${model.user.podcastRole}">
        <td style="padding-right:2em"><div class="forward"><a href="podcastReceiverAdmin.view?refresh"><fmt:message key="podcastreceiver.check"/></a></div></td>
    </c:if>
    <c:if test="${model.user.adminRole}">
        <td style="padding-right:2em"><div class="forward"><a href="podcastSettings.view?"><fmt:message key="podcastreceiver.settings"/></a></div></td>
    </c:if>
</tr></table>

<c:if test="${model.user.podcastRole}">
    <form:form method="post" action="podcastReceiverAdmin.view?">
        <table>
            <tr>
                <td><fmt:message key="podcastreceiver.subscribe"/></td>
                <td><input type="text" name="add" value="http://" style="width:30em" onclick="select()"/></td>
                <td><input type="submit" value="<fmt:message key="common.ok"/>"/></td>
            </tr>
        </table>
    </form:form>
</c:if>

</body>
</html>
