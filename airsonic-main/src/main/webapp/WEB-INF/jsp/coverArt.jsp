<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<%@ include file="include.jsp" %>

<%--
PARAMETERS
  albumId: ID of album.
  playlistId: ID of playlist.
  podcastChannelId: ID of podcast channel
  coverArtSize: Height and width of cover art.
  caption1: Caption line 1
  caption2: Caption line 2
  caption3: Caption line 3
  captionCount: Number of caption lines to display (default 0)
  showLink: Whether to make the cover art image link to the album page.
  showZoom: Whether to display a link for zooming the cover art.
  showChange: Whether to display a link for changing the cover art.
  appearAfter: Fade in after this many milliseconds, or nil if no fading in should happen.
  hideOverflow: Hide cover art overflow when height is greater than width
--%>
<c:choose>
    <c:when test="${empty param.coverArtSize}">
        <c:set var="size" value="auto"/>
    </c:when>
    <c:otherwise>
        <c:set var="size" value="${param.coverArtSize}px"/>
    </c:otherwise>
</c:choose>

<c:set var="captionCount" value="${empty param.captionCount ? 0 : param.captionCount}"/>

<str:randomString count="5" type="alphabet" var="divId"/>
<str:randomString count="5" type="alphabet" var="imgId"/>
<str:randomString count="5" type="alphabet" var="playId"/>
<str:randomString count="5" type="alphabet" var="addId"/>

<div class="coverart dropshadow">
    <div style="width:${size};max-width:${size};height:${size};max-height:${size};cursor:pointer;<c:if test="${param.hideOverflow}">overflow:hidden</c:if>;" title="${param.caption1}" id="${divId}">

        <c:if test="${not empty param.albumId}">
            <c:url value="main.view" var="targetUrl">
                <c:param name="id" value="${param.albumId}"/>
            </c:url>
        </c:if>
        <c:if test="${not empty param.playlistId}">
            <c:url value="playlist.view" var="targetUrl">
                <c:param name="id" value="${param.playlistId}"/>
            </c:url>
        </c:if>
        <c:if test="${not empty param.podcastChannelId}">
            <c:url value="podcastChannel.view" var="targetUrl">
                <c:param name="id" value="${param.podcastChannelId}"/>
            </c:url>
        </c:if>

        <c:url value="/coverArt.view" var="coverArtUrl">
            <c:if test="${not empty param.coverArtSize}">
                <c:param name="size" value="${param.coverArtSize}"/>
            </c:if>
            <c:if test="${not empty param.albumId}">
                <c:param name="id" value="${param.albumId}"/>
            </c:if>
            <c:if test="${not empty param.podcastChannelId}">
                <c:param name="id" value="pod-${param.podcastChannelId}"/>
            </c:if>
            <c:if test="${not empty param.playlistId}">
                <c:param name="id" value="pl-${param.playlistId}"/>
            </c:if>
        </c:url>

        <c:url value="/coverArt.view" var="zoomCoverArtUrl">
            <c:param name="id" value="${param.albumId}"/>
        </c:url>

        <div style="position: relative; width: 0; height: 0">
            <img src="<spring:theme code="playOverlayImage"/>" id="${playId}"
                 style="position: relative; top: 8px; left: 8px; z-index: 2; display:none" >
        </div>

        <c:if test="${not empty param.albumId}">
          <div style="position: relative; width: 0; height: 0">
              <img src="<spring:theme code="addOverlayImage"/>" id="${addId}"
                   style="position: relative; top: 8px; left: 48px; z-index: 2; display:none" >
          </div>
        </c:if>

        <c:choose>
        <c:when test="${param.showLink}"><a href="${targetUrl}" title="${param.caption1}"></c:when>
        <c:when test="${param.showZoom}"><a href="${zoomCoverArtUrl}" rel="zoom" title="${param.caption1}"></c:when>
            </c:choose>
            <img src="${coverArtUrl}" id="${imgId}" alt="${param.caption1}"
                 style="display:none">
            <c:if test="${param.showLink or param.showZoom}"></a></c:if>
    </div>

    <c:if test="${captionCount gt 0}">
        <div class="caption1" style="width:${param.coverArtSize - 16}px"><a href="${targetUrl}" title="${param.caption1}">${param.caption1}</a></div>
    </c:if>
    <c:if test="${captionCount gt 1}">
        <div class="caption2" style="width:${param.coverArtSize - 16}px">${param.caption2}&nbsp;</div>
    </c:if>
    <c:if test="${captionCount gt 2}">
        <div class="caption3" style="width:${param.coverArtSize - 16}px">${param.caption3}&nbsp;</div>
    </c:if>
</div>

<c:if test="${param.showChange or param.showZoom}">
    <div style="padding-top:6px;text-align:right">
        <c:if test="${param.showChange}">
            <c:url value="/changeCoverArt.view" var="changeCoverArtUrl">
                <c:param name="id" value="${param.albumId}"/>
            </c:url>
            <a class="detail" href="${changeCoverArtUrl}"><fmt:message key="coverart.change"/></a>
        </c:if>

        <c:if test="${param.showZoom and param.showChange}">
            |
        </c:if>

        <c:if test="${param.showZoom}">
            <a class="detail" rel="zoom" title="${param.caption1}" href="${zoomCoverArtUrl}"><fmt:message key="coverart.zoom"/></a>
        </c:if>
    </div>
</c:if>

<script type="text/javascript">
    $(document).ready(function () {
        setTimeout("$('#${imgId}').fadeIn(500)", ${empty param.appearAfter ? 0 : param.appearAfter});
    });

    $("#${divId}").mouseenter(function () {
        $("#${playId}").show();
        $("#${addId}").show();
        $("#${imgId}").stop();
        $("#${imgId}").animate({opacity: 0.7}, 150);
    });
    $("#${divId}").mouseleave(function () {
        $("#${playId}").hide();
        $("#${addId}").hide();
        $("#${imgId}").stop();
        $("#${imgId}").animate({opacity: 1.0}, 150);
    });
    $("#${playId}").click(function () {
        <c:if test="${not empty param.albumId}">
        top.playQueue.onPlay(${param.albumId});
        </c:if>
        <c:if test="${not empty param.playlistId}">
        top.playQueue.onPlayPlaylist(${param.playlistId});
        </c:if>
        <c:if test="${not empty param.podcastChannelId}">
        top.playQueue.onPlayPodcastChannel(${param.podcastChannelId});
        </c:if>
    });
    $("#${addId}").click(function () {
        <c:if test="${not empty param.albumId}">
        top.playQueue.onAdd(${param.albumId});
        </c:if>
    });
</script>
