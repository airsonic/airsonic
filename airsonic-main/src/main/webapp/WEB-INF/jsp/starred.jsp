<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <script type="text/javascript" src="<c:url value="/script/utils.js"/>"></script>
    <script type="text/javascript" src="<c:url value='/dwr/util.js'/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/engine.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/interface/starService.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/interface/playlistService.js"/>"></script>
    <script type="text/javascript" language="javascript">

        function toggleStar(mediaFileId, imageId) {
            if ($(imageId).attr("src").indexOf("<spring:theme code="ratingOnImage"/>") != -1) {
                $(imageId).attr("src", "<spring:theme code="ratingOffImage"/>");
                starService.unstar(mediaFileId);
            }
            else if ($(imageId).attr("src").indexOf("<spring:theme code="ratingOffImage"/>") != -1) {
                $(imageId).attr("src", "<spring:theme code="ratingOnImage"/>");
                starService.star(mediaFileId);
            }
        }

        function onSavePlaylist() {
            playlistService.createPlaylistForStarredSongs(function (playlistId) {
                top.left.updatePlaylists();
                top.left.showAllPlaylists();
                top.main.location.href = "playlist.view?id=" + playlistId;
                $().toastmessage("showSuccessToast", "<fmt:message key="playlist.toast.saveasplaylist"/>");
            });
        }

        function onPlayAll() {
            top.playQueue.onPlayStarred();
        }

    </script>
</head>
<body class="mainframe bgcolor1">

<h1>
    <img src="<spring:theme code="starredImage"/>" alt="">
    <span style="vertical-align: middle"><fmt:message key="starred.title"/></span>
</h1>

<c:if test="${empty model.artists and empty model.albums and empty model.songs}">
    <p style="padding-top: 1em"><em><fmt:message key="starred.empty"/></em></p>
</c:if>

<c:if test="${not empty model.albums}">
    <h2><fmt:message key="search.hits.albums"/></h2>

<div style="padding-top:0.5em">
    <c:forEach items="${model.albums}" var="album" varStatus="loopStatus">

        <c:set var="albumTitle">
            <c:choose>
                <c:when test="${empty album.name}">
                    <fmt:message key="common.unknown"/>
                </c:when>
                <c:otherwise>
                    ${fn:escapeXml(album.name)}
                </c:otherwise>
            </c:choose>
        </c:set>

        <div class="albumThumb">
            <c:import url="coverArt.jsp">
                <c:param name="albumId" value="${album.id}"/>
                <c:param name="caption1" value="${albumTitle}"/>
                <c:param name="caption2" value="${fn:escapeXml(album.artist)}"/>
                <c:param name="captionCount" value="2"/>
                <c:param name="coverArtSize" value="${model.coverArtSize}"/>
                <c:param name="showLink" value="true"/>
                <c:param name="appearAfter" value="${loopStatus.count * 30}"/>
                <c:param name="hideOverflow" value="true"/>
            </c:import>
        </div>
    </c:forEach>
    </c:if>
</div>

<c:if test="${not empty model.artists}">
    <h2><fmt:message key="search.hits.artists"/></h2>
    <table class="music indent">
        <c:forEach items="${model.artists}" var="artist">

            <c:url value="/main.view" var="mainUrl">
                <c:param name="id" value="${artist.id}"/>
            </c:url>

            <tr>
                <c:import url="playButtons.jsp">
                    <c:param name="id" value="${artist.id}"/>
                    <c:param name="playEnabled" value="${model.user.streamRole and not model.partyModeEnabled}"/>
                    <c:param name="addEnabled" value="${model.user.streamRole and (not model.partyModeEnabled or not artist.directory)}"/>
                    <c:param name="starEnabled" value="true"/>
                    <c:param name="starred" value="${not empty artist.starredDate}"/>
                    <c:param name="asTable" value="true"/>
                </c:import>
                <td class="truncate">
                    <a href="${mainUrl}">${fn:escapeXml(artist.name)}</a>
                </td>
            </tr>
        </c:forEach>
    </table>
</c:if>

<c:if test="${not empty model.songs}">
    <h2><fmt:message key="search.hits.songs"/></h2>
    <table class="music indent">
        <c:forEach items="${model.songs}" var="song">

            <sub:url value="/main.view" var="mainUrl">
                <sub:param name="path" value="${song.parentPath}"/>
            </sub:url>

            <tr>
                <c:import url="playButtons.jsp">
                    <c:param name="id" value="${song.id}"/>
                    <c:param name="playEnabled" value="${model.user.streamRole and not model.partyModeEnabled}"/>
                    <c:param name="addEnabled" value="${model.user.streamRole and not model.partyModeEnabled}"/>
                    <c:param name="starEnabled" value="true"/>
                    <c:param name="starred" value="${not empty song.starredDate}"/>
                    <c:param name="video" value="false"/>
                    <c:param name="asTable" value="true"/>
                </c:import>

                <td class="truncate">
                        ${fn:escapeXml(song.title)}
                </td>

                <td class="truncate">
                    <a href="${mainUrl}"><span class="detail">${fn:escapeXml(song.albumName)}</span></a>
                </td>

                <td class="truncate">
                    <span class="detail">${fn:escapeXml(song.artist)}</span>
                </td>
            </tr>

        </c:forEach>
    </table>

    <div class="forward" style="float:left;padding-right:1.5em">
        <a href="#" onclick="onSavePlaylist()"><fmt:message key="playlist.save"/></a>
    </div>
    <div class="forward" style="float: left">
        <a href="#" onclick="onPlayAll()"><fmt:message key="main.playall"/></a>
    </div>
    <div style="clear: both"></div>

</c:if>

<c:if test="${not empty model.videos}">
    <h2><fmt:message key="search.hits.videos"/></h2>
    <table class="music indent">
        <c:forEach items="${model.videos}" var="video">

            <c:url value="/videoPlayer.view" var="videoUrl">
                <c:param name="id" value="${video.id}"/>
            </c:url>

            <tr>
                <c:import url="playButtons.jsp">
                    <c:param name="id" value="${video.id}"/>
                    <c:param name="playEnabled" value="${model.user.streamRole and not model.partyModeEnabled}"/>
                    <c:param name="addEnabled" value="${model.user.streamRole and not model.partyModeEnabled}"/>
                    <c:param name="starEnabled" value="true"/>
                    <c:param name="starred" value="${not empty video.starredDate}"/>
                    <c:param name="video" value="${model.player.web}"/>
                    <c:param name="asTable" value="true"/>
                </c:import>
                <td class="truncate">
                    <a href="${videoUrl}">${fn:escapeXml(video.name)}</a>
                </td>
            </tr>
        </c:forEach>
    </table>
</c:if>

</body></html>
