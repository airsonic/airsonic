<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>
<!DOCTYPE html>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>

    <script type="text/javascript" language="javascript">
        function init() {
            <c:if test="${model.listReloadDelay gt 0}">
            setTimeout("refresh()", ${model.listReloadDelay * 1000});
            </c:if>

            <c:if test="${not model.musicFoldersExist}">
            $().toastmessage("showNoticeToast", "<fmt:message key="top.missing"/>");
            </c:if>

            <c:if test="${model.isIndexBeingCreated}">
            $().toastmessage("showNoticeToast", "<fmt:message key="home.scan"/>");
            </c:if>
        }

        function refresh() {
            top.main.location.href = top.main.location.href;
        }

        function playShuffle() {
            top.playQueue.onPlayShuffle('${model.listType}', ${model.listOffset}, ${model.listSize}, '${model.genre}', '${model.decade}')
        }

    </script>
</head>
<body class="mainframe bgcolor1" onload="init();">
<c:if test="${not empty model.welcomeTitle}">
<h1>
    <img src="<spring:theme code="homeImage"/>" alt="">
    <span style="vertical-align: middle">${model.welcomeTitle}</span>
</h1>
</c:if>

<c:if test="${not empty model.welcomeSubtitle}">
    <h2>${model.welcomeSubtitle}</h2>
</c:if>

<h2>
    <c:forTokens items="random newest starred highest frequent recent decade genre alphabetical" delims=" " var="cat" varStatus="loopStatus">
        <c:if test="${loopStatus.count > 1}">&nbsp;|&nbsp;</c:if>
        <sub:url var="url" value="home.view">
            <sub:param name="listType" value="${cat}"/>
        </sub:url>

        <c:choose>
            <c:when test="${model.listType eq cat}">
                <span class="headerSelected"><fmt:message key="home.${cat}.title"/></span>
            </c:when>
            <c:otherwise>
                <span class="header"><a href="${url}"><fmt:message key="home.${cat}.title"/></a></span>
            </c:otherwise>
        </c:choose>

    </c:forTokens>
</h2>

<%@ include file="homePager.jsp" %>

<c:if test="${not empty model.welcomeMessage}">
    <div style="width:15em;float:right;padding:0 1em 0 1em;border-left:1px solid #<spring:theme code="detailColor"/>">
        ${model.welcomeMessage}
    </div>
</c:if>

<c:forEach items="${model.albums}" var="album" varStatus="loopStatus">

    <c:set var="albumTitle">
        <c:choose>
            <c:when test="${empty album.albumTitle}">
                <fmt:message key="common.unknown"/>
            </c:when>
            <c:otherwise>
                ${album.albumTitle}
            </c:otherwise>
        </c:choose>
    </c:set>

    <c:set var="captionCount" value="2"/>

    <c:if test="${not empty album.playCount}">
        <c:set var="caption3"><fmt:message key="home.playcount"><fmt:param value="${album.playCount}"/></fmt:message></c:set>
        <c:set var="captionCount" value="3"/>
    </c:if>
    <c:if test="${not empty album.lastPlayed}">
        <fmt:formatDate value="${album.lastPlayed}" dateStyle="short" var="lastPlayedDate"/>
        <c:set var="caption3"><fmt:message key="home.lastplayed"><fmt:param value="${lastPlayedDate}"/></fmt:message></c:set>
        <c:set var="captionCount" value="3"/>
    </c:if>
    <c:if test="${not empty album.created}">
        <fmt:formatDate value="${album.created}" dateStyle="short" var="creationDate"/>
        <c:set var="caption3"><fmt:message key="home.created"><fmt:param value="${creationDate}"/></fmt:message></c:set>
        <c:set var="captionCount" value="3"/>
    </c:if>
    <c:if test="${not empty album.year}">
        <c:set var="caption3" value="${album.year}"/>
        <c:set var="captionCount" value="3"/>
    </c:if>

    <div class="albumThumb">
        <c:import url="coverArt.jsp">
            <c:param name="albumId" value="${album.id}"/>
            <c:param name="caption1" value="${fn:escapeXml(album.albumTitle)}"/>
            <c:param name="caption2" value="${fn:escapeXml(album.artist)}"/>
            <c:param name="caption3" value="${caption3}"/>
            <c:param name="captionCount" value="${captionCount}"/>
            <c:param name="coverArtSize" value="${model.coverArtSize}"/>
            <c:param name="showLink" value="true"/>
            <c:param name="appearAfter" value="${loopStatus.count * 30}"/>
            <c:param name="hideOverflow" value="true"/>
        </c:import>

        <c:if test="${not empty album.rating}">
            <c:import url="rating.jsp">
                <c:param name="readonly" value="true"/>
                <c:param name="rating" value="${album.rating}"/>
            </c:import>
        </c:if>

    </div>
</c:forEach>

<c:if test="${model.listSize eq fn:length(model.albums)}">
    <%@ include file="homePager.jsp" %>
</c:if>

</body></html>
