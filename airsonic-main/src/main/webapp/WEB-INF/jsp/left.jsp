<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>
<!DOCTYPE html>
<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <script type="text/javascript" src="<c:url value="/script/utils.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/engine.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/interface/playlistService.js"/>"></script>
    <script type="text/javascript" language="javascript">

        var playlists;

        function init() {
            dwr.engine.setErrorHandler(null);
            updatePlaylists();

            var mainLocation = top.main.location.href;
            if (${model.musicFolderChanged}) {
                if (mainLocation.indexOf("/home.view") != -1) {
                    top.main.location.href = mainLocation;
                }
            }
        }

        function updatePlaylists() {
            playlistService.getReadablePlaylists(playlistCallback);
        }

        function createEmptyPlaylist() {
            showAllPlaylists();
            playlistService.createEmptyPlaylist(playlistCallback);
        }

        function showAllPlaylists() {
            $('#playlistOverflow').show('blind');
            $('#showAllPlaylists').hide('blind');
        }

        function playlistCallback(playlists) {
            this.playlists = playlists;

            $("#playlists").empty();
            $("#playlistOverflow").empty();
            for (var i = 0; i < playlists.length; i++) {
                var playlist = playlists[i];
                var overflow = i > 9;
                $("<p class='dense'><a target='main' href='playlist.view?id=" +
                        playlist.id + "'>" + escapeHtml(playlist.name) + "&nbsp;(" + playlist.fileCount + ")</a></p>").appendTo(overflow ? "#playlistOverflow" : "#playlists");
            }

            if (playlists.length > 10 && !$('#playlistOverflow').is(":visible")) {
                $('#showAllPlaylists').show();
            }
        }
    </script>
</head>

<body class="bgcolor2 leftframe" onload="init()">
<a name="top"></a>

<div style="padding-bottom:1.5em">
    <a href="home.view" target="main">
      <img src="<spring:theme code="logoImage"/>" style="width:196px" title="<fmt:message key="top.help"/>" alt="">
    </a>
</div>

<c:if test="${fn:length(model.musicFolders) > 1}">
    <div style="padding-bottom:1.0em">
    <select name="musicFolderId" style="width:100%" onchange="location='left.view?musicFolderId=' + options[selectedIndex].value;">
            <option value="-1"><fmt:message key="left.allfolders"/></option>
            <c:forEach items="${model.musicFolders}" var="musicFolder">
                <option ${model.selectedMusicFolder.id == musicFolder.id ? "selected" : ""} value="${musicFolder.id}">${fn:escapeXml(musicFolder.name)}</option>
            </c:forEach>
        </select>
    </div>
</c:if>

<div style="margin-bottom:0.5em;padding-left: 2px" class="bgcolor1">
    <c:forEach items="${model.indexes}" var="index">
        <a href="#${index.index}" accesskey="${index.index}">${index.index}</a>
    </c:forEach>
</div>

<div style="padding-bottom:0.5em">
    <div class="forward">
        <c:choose>
            <c:when test="${model.scanning}">
                <a href="left.view"><fmt:message key="common.refresh"/></a>
            </c:when>
            <c:otherwise>
                <a href="left.view?refresh=true"><fmt:message key="common.refresh"/></a>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<c:if test="${not empty model.shortcuts}">
    <h2 class="bgcolor1" style="padding-left: 2px"><fmt:message key="left.shortcut"/></h2>
    <c:forEach items="${model.shortcuts}" var="shortcut">
        <p class="dense" style="padding-left:2px">
            <sub:url value="main.view" var="mainUrl">
                <sub:param name="id" value="${shortcut.id}"/>
            </sub:url>
            <a target="main" href="${mainUrl}">${fn:escapeXml(shortcut.name)}</a>
        </p>
    </c:forEach>
</c:if>

<h2 class="bgcolor1" style="padding-left: 2px"><fmt:message key="left.playlists"/></h2>
<div id="playlistWrapper" style='padding-left:2px'>
    <div id="playlists"></div>
    <div id="playlistOverflow" style="display:none"></div>
    <div style="padding-top: 0.3em"></div>
    <div class="forward" id="showAllPlaylists" style="display: none"><a href="#" onclick="showAllPlaylists()"><fmt:message key="left.showallplaylists"/></a></div>
    <div class="forward"><a href="#" onclick="createEmptyPlaylist()"><fmt:message key="left.createplaylist"/></a></div>
    <div class="forward"><a href="importPlaylist.view" target="main"><fmt:message key="left.importplaylist"/></a></div>
</div>

<c:if test="${not empty model.radios}">
    <h2 class="bgcolor1" style="padding-left: 2px"><fmt:message key="left.radio"/></h2>
    <c:forEach items="${model.radios}" var="radio">
        <p class="dense" style="padding-left: 2px">
            <a target="hidden" href="${radio.streamUrl}">
                <img src="<spring:theme code="playImage"/>" alt="<fmt:message key="common.play"/>" title="<fmt:message key="common.play"/>"></a>
            <span style="vertical-align: middle">
                <c:choose>
                <c:when test="${empty radio.homepageUrl}">
                        ${fn:escapeXml(radio.name)}
                    </c:when>
                    <c:otherwise>
                    <a target="main" href="${radio.homepageUrl}">${fn:escapeXml(radio.name)}</a>
                    </c:otherwise>
                    </c:choose>
            </span>
        </p>
    </c:forEach>
</c:if>

<c:forEach items="${model.indexedArtists}" var="entry">
    <table class="bgcolor1" style="width:100%;padding:0;margin:1em 0 0 0;border:0">
        <tr style="padding:0;margin:0;border:0">
            <th style="text-align:left;padding:0;margin:0;border:0"><a name="${fn:escapeXml(entry.key.index)}"></a>
                <h2 style="padding:0;margin:0;border:0">${fn:escapeXml(entry.key.index)}</h2>
            </th>
            <th style="text-align:right;">
                <a href="#top"><img src="<spring:theme code="upImage"/>" alt="" style="height:18px;"></a>
            </th>
        </tr>
    </table>

    <c:forEach items="${entry.value}" var="artist">
        <p class="dense" style="padding-left:2px">
            <span title="${artist.name}">
                <sub:url value="main.view" var="mainUrl">
                    <c:forEach items="${artist.mediaFiles}" var="mediaFile">
                        <sub:param name="id" value="${mediaFile.id}"/>
                    </c:forEach>
                </sub:url>
                <a target="main" href="${mainUrl}"><str:truncateNicely upper="${35}">${fn:escapeXml(artist.name)}</str:truncateNicely></a>
            </span>
        </p>
    </c:forEach>
</c:forEach>

<div style="padding-top:1em"></div>

<c:forEach items="${model.singleSongs}" var="song">
    <p class="dense" style="padding-left:2px">
        <span class="songTitle" title="${fn:escapeXml(song.title)}">
            <c:import url="playButtons.jsp">
                <c:param name="id" value="${song.id}"/>
                <c:param name="playEnabled" value="${model.user.streamRole and not model.partyMode}"/>
                <c:param name="addEnabled" value="${model.user.streamRole}"/>
                <c:param name="downloadEnabled" value="${model.user.downloadRole and not model.partyMode}"/>
                <c:param name="video" value="${song.video and model.player.web}"/>
            </c:import>
            <str:truncateNicely upper="${35}">${fn:escapeXml(song.title)}</str:truncateNicely>
        </span>
    </p>
</c:forEach>

<c:if test="${model.statistics.songCount gt 0}">
    <div class="detail" style="padding-top: 0.6em; padding-left: 2px">
        <fmt:message key="left.statistics">
            <fmt:param value="${model.statistics.artistCount}"/>
            <fmt:param value="${model.statistics.albumCount}"/>
            <fmt:param value="${model.statistics.songCount}"/>
            <fmt:param value="${model.bytes}"/>
            <fmt:param value="${model.hours}"/>
        </fmt:message>
    </div>
</c:if>

<div style="height:5em"></div>


<div class="bgcolor2" style="opacity: 1.0; clear: both; position: fixed; bottom: 0; right: 0; left: 0;
      padding: 0.25em 0.75em 0.25em 0.75em; border-top:1px solid black; max-width: 850px;">
    <c:forEach items="${model.indexes}" var="index">
        <a href="#${index.index}">${index.index}</a>
    </c:forEach>
</div>

</body></html>
