<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>
<%--@elvariable id="model" type="java.util.Map"--%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
</head><body class="mainframe bgcolor1">

<h1 style="padding-bottom: 1em">
    <img src="<spring:theme code='playlistImage'/>" alt="">
    <span style="vertical-align: middle"><fmt:message key="left.playlists"/></span>
</h1>

<c:if test="${empty model.playlists}">
    <p><em><fmt:message key="playlist2.noplaylists"/></em></p>
</c:if>

<c:forEach items="${model.playlists}" var="playlist" varStatus="loopStatus">

    <c:set var="caption2">
        ${playlist.fileCount} <fmt:message key="playlist2.songs"/> &ndash; ${playlist.durationAsString}
    </c:set>
    <div class="albumThumb">
        <c:import url="coverArt.jsp">
            <c:param name="playlistId" value="${playlist.id}"/>
            <c:param name="coverArtSize" value="200"/>
            <c:param name="caption1" value="${fn:escapeXml(playlist.name)}"/>
            <c:param name="caption2" value="${caption2}"/>
            <c:param name="captionCount" value="2"/>
            <c:param name="showLink" value="true"/>
            <c:param name="appearAfter" value="${loopStatus.count * 30}"/>
        </c:import>
    </div>

</c:forEach>

</body>
</html>
