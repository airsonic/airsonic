<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%--@elvariable id="model" type="java.util.Map"--%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <script type="text/javascript" src="<c:url value="/script/scripts-2.0.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/engine.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/interface/starService.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/interface/playlistService.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/interface/multiService.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/script/fancyzoom/FancyZoom.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/script/fancyzoom/FancyZoomHTML.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/script/util.js"/>"></script>

</head><body class="mainframe bgcolor1" onload="init();">

<sub:url value="createShare.view" var="shareUrl">
    <sub:param name="id" value="${model.dir.id}"/>
</sub:url>
<sub:url value="download.view" var="downloadUrl">
    <sub:param name="id" value="${model.dir.id}"/>
</sub:url>

<script type="text/javascript" language="javascript">
    function init() {
        setupZoom('<c:url value="/"/>');

        $("#dialog-select-playlist").dialog({resizable: true, height: 350, autoOpen: false,
            buttons: {
                "<fmt:message key="common.cancel"/>": function() {
                    $(this).dialog("close");
                }
            }});

        <c:if test="${model.showArtistInfo}">
        loadArtistInfo();
        </c:if>
    }

    function loadArtistInfo() {
        multiService.getArtistInfo(${model.dir.id}, 8, 0, function (artistInfo) {
            if (artistInfo.similarArtists.length > 0) {
                var html = "";
                for (var i = 0; i < artistInfo.similarArtists.length; i++) {
                    html += "<a href='main.view?id=" + artistInfo.similarArtists[i].mediaFileId + "' target='main'>" +
                            escapeHtml(artistInfo.similarArtists[i].artistName) + "</a>";
                    if (i < artistInfo.similarArtists.length - 1) {
                        html += " <span class='similar-artist-divider'>|</span> ";
                    }
                }
                $("#similarArtists").append(html);
                $("#similarArtists").show();
                $("#similarArtistsTitle").show();
                $("#similarArtistsRadio").show();
            }
        });
    }

    <!-- actionSelected() is invoked when the users selects from the "More actions..." combo box. -->
    function actionSelected(id) {
        var selectedIndexes = getSelectedIndexes();

        if (id == "top") {
            return;
        } else if (id == "selectAll") {
            selectAll(true);
        } else if (id == "selectNone") {
            selectAll(false);
        } else if (id == "share" && selectedIndexes != "") {
            location.href = "${shareUrl}&" + selectedIndexes;
        } else if (id == "download" && selectedIndexes != "") {
            location.href = "${downloadUrl}&" + selectedIndexes;
        } else if (id == "appendPlaylist" && selectedIndexes != "") {
            onAppendPlaylist();
        }
        $("#moreActions").prop("selectedIndex", 0);
    }

    function getSelectedIndexes() {
        var result = "";
        for (var i = 0; i < ${fn:length(model.files)}; i++) {
            var checkbox = $("#songIndex" + i);
            if (checkbox != null  && checkbox.is(":checked")) {
                result += "i=" + i + "&";
            }
        }
        return result;
    }

    function selectAll(b) {
        for (var i = 0; i < ${fn:length(model.files)}; i++) {
            var checkbox = $("#songIndex" + i);
            if (checkbox != null) {
                if (b) {
                    checkbox.attr("checked", "checked");
                } else {
                    checkbox.removeAttr("checked");
                }
            }
        }
    }

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

    function playAll() {
        top.playQueue.onPlay(${model.dir.id});
    }

    function playRandom() {
        top.playQueue.onPlayRandom(${model.dir.id}, 40);
    }

    function addAll() {
        top.playQueue.onAdd(${model.dir.id});
    }

    function playSimilar() {
        top.playQueue.onPlaySimilar(${model.dir.id}, 50);
    }

    function onAppendPlaylist() {
        playlistService.getWritablePlaylists(playlistCallback);
    }
    function playlistCallback(playlists) {
        $("#dialog-select-playlist-list").empty();
        for (var i = 0; i < playlists.length; i++) {
            var playlist = playlists[i];
            $("<p class='dense'><b><a href='#' onclick='appendPlaylist(" + playlist.id + ")'>" + escapeHtml(playlist.name)
                    + "</a></b></p>").appendTo("#dialog-select-playlist-list");
        }
        $("#dialog-select-playlist").dialog("open");
    }
    function appendPlaylist(playlistId) {
        $("#dialog-select-playlist").dialog("close");

        var mediaFileIds = new Array();
        for (var i = 0; i < ${fn:length(model.files)}; i++) {
            var checkbox = $("#songIndex" + i);
            if (checkbox && checkbox.is(":checked")) {
                mediaFileIds.push($("#songId" + i).html());
            }
        }
        playlistService.appendToPlaylist(playlistId, mediaFileIds, function (){
            top.left.updatePlaylists();
            $().toastmessage("showSuccessToast", "<fmt:message key="playlist.toast.appendtoplaylist"/>");
        });
    }
    function showAllAlbums() {
        window.location.href = updateQueryStringParameter(window.location.href, "showAll", "1");
    }
</script>

<div style="float:left">
    <h1>
        <img id="starImage" src="<spring:theme code="${not empty model.dir.starredDate ? 'ratingOnImage' : 'ratingOffImage'}"/>"
             onclick="toggleStar(${model.dir.id}, '#starImage'); return false;" style="cursor:pointer" alt="">

        <span style="vertical-align: middle">
            <c:forEach items="${model.ancestors}" var="ancestor">
                <sub:url value="main.view" var="ancestorUrl">
                    <sub:param name="id" value="${ancestor.id}"/>
                </sub:url>
                <a href="${ancestorUrl}">${fn:escapeXml(ancestor.name)}</a> &raquo;
            </c:forEach>
            ${fn:escapeXml(model.dir.name)}
        </span>

        <c:if test="${model.averageRating gt 0}">
            &nbsp;&nbsp;
            <c:import url="rating.jsp">
                <c:param name="readonly" value="true"/>
                <c:param name="rating" value="${model.averageRating}"/>
            </c:import>
        </c:if>
    </h1>

    <c:if test="${not model.partyMode}">
        <h2>
            <c:if test="${model.navigateUpAllowed}">
                <sub:url value="main.view" var="upUrl">
                    <sub:param name="id" value="${model.parent.id}"/>
                </sub:url>
                <span class="header"><a href="${upUrl}"><fmt:message key="main.up"/></a></span>
                <c:set var="needSep" value="true"/>
            </c:if>

            <c:if test="${model.user.streamRole}">
                <c:if test="${needSep}">|</c:if>
                <span class="header"><a href="javascript:playAll()"><fmt:message key="main.playall"/></a></span> |
                <span class="header"><a href="javascript:playRandom()"><fmt:message key="main.playrandom"/></a></span> |
                <span class="header"><a href="javascript:addAll()"><fmt:message key="main.addall"/></a></span>
                <c:set var="needSep" value="true"/>
            </c:if>

            <c:if test="${model.user.downloadRole}">
                <c:if test="${needSep}">|</c:if>
                <span class="header"><a href="${downloadUrl}"><fmt:message key="main.downloadall"/></a></span>
                <c:set var="needSep" value="true"/>
            </c:if>

            <c:if test="${model.user.coverArtRole}">
                <sub:url value="editTags.view" var="editTagsUrl">
                    <sub:param name="id" value="${model.dir.id}"/>
                </sub:url>
                <c:if test="${needSep}">|</c:if>
                <span class="header"><a href="${editTagsUrl}"><fmt:message key="main.tags"/></a></span>
                <c:set var="needSep" value="true"/>
            </c:if>

            <c:if test="${model.user.commentRole}">
                <c:if test="${needSep}">|</c:if>
                <span class="header"><a href="javascript:toggleComment()"><fmt:message key="main.comment"/></a></span>
            </c:if>
        </h2>
    </c:if>
</div>

<%@ include file="viewSelector.jsp" %>
<div style="clear:both"></div>

<div class="detail">
    <c:if test="${model.user.commentRole}">
        <c:import url="rating.jsp">
            <c:param name="id" value="${model.dir.id}"/>
            <c:param name="readonly" value="false"/>
            <c:param name="rating" value="${model.userRating}"/>
        </c:import>
    </c:if>

    <c:if test="${model.user.shareRole}">
        <span class="header"><a href="${shareUrl}"><img src="<spring:theme code="shareSmallImage"/>" alt=""></a>
            <a href="${shareUrl}"><fmt:message key="main.sharealbum"/></a> </span> |
    </c:if>

    <c:if test="${not empty model.artist and not empty model.album}">
        <sub:url value="https://www.google.com/search" var="googleUrl" encoding="UTF-8">
            <sub:param name="q" value="\"${model.artist}\" \"${model.album}\""/>
        </sub:url>
        <sub:url value="https://en.wikipedia.org/wiki/Special:Search" var="wikipediaUrl" encoding="UTF-8">
            <sub:param name="search" value="\"${model.album}\""/>
            <sub:param name="go" value="Go"/>
        </sub:url>
        <sub:url value="allmusic.view" var="allmusicUrl">
            <sub:param name="album" value="${model.album}"/>
        </sub:url>
        <sub:url value="https://www.last.fm/search" var="lastFmUrl" encoding="UTF-8">
            <sub:param name="q" value="\"${model.artist}\" \"${model.album}\""/>
            <sub:param name="type" value="album"/>
        </sub:url>
        <span class="header"><fmt:message key="top.search"/> <a target="_blank" href="${googleUrl}">Google</a></span> |
        <span class="header"><a target="_blank" rel="noopener noreferrer" href="${wikipediaUrl}">Wikipedia</a></span> |
        <span class="header"><a target="_blank" rel="noopener noreferrer" href="${allmusicUrl}">allmusic</a></span> |
        <span class="header"><a target="_blank" rel="noopener noreferrer" href="${lastFmUrl}">Last.fm</a></span> |
        <c:if test="${not empty model.musicBrainzReleaseId}">
          <sub:url value="https://musicbrainz.org/release/${model.musicBrainzReleaseId}" var="musicBrainzUrl" encoding="UTF-8">
          </sub:url>
          <span class="header"><a target="_blank" rel="noopener noreferrer" href="${musicBrainzUrl}">MusicBrainz</a></span> |
        </c:if>
        <span class="header">
            <fmt:message key="main.playcount"><fmt:param value="${model.dir.playCount}"/></fmt:message>
            <c:if test="${not empty model.dir.lastPlayed}">
                <fmt:message key="main.lastplayed">
                    <fmt:param><fmt:formatDate type="date" dateStyle="long" value="${model.dir.lastPlayed}"/></fmt:param>
                </fmt:message>
            </c:if>
        </span>

    </c:if>
</div>

<div id="comment" class="albumComment"><sub:wiki text="${model.dir.comment}"/></div>

<div id="commentForm" style="display:none">
    <form method="post" action="setMusicFileInfo.view">
        <sec:csrfInput />
        <input type="hidden" name="action" value="comment">
        <input type="hidden" name="id" value="${model.dir.id}">
        <textarea name="comment" rows="6" cols="70">${model.dir.comment}</textarea>
        <input type="submit" value="<fmt:message key="common.save"/>">
    </form>
    <fmt:message key="main.wiki"/>
</div>

<script type='text/javascript'>
    function toggleComment() {
        $("#commentForm").toggle();
        $("#comment").toggle();
    }
</script>


<table cellpadding="0" style="width:100%;padding-top: 0.3em;padding-bottom: 1em">
    <tr style="vertical-align:top;">
        <td style="vertical-align:top;padding-bottom: 1em">
            <table class="music" style="width: 100%">
                <c:forEach items="${model.files}" var="song" varStatus="loopStatus">
                    <%--@elvariable id="song" type="org.airsonic.player.domain.MediaFile"--%>
                    <tr style="margin:0;padding:0;border:0">
                        <c:import url="playButtons.jsp">
                            <c:param name="id" value="${song.id}"/>
                            <c:param name="video" value="${song.video and model.player.web}"/>
                            <c:param name="playEnabled" value="${model.user.streamRole and not model.partyMode}"/>
                            <c:param name="addEnabled" value="${model.user.streamRole and (not model.partyMode or not song.directory)}"/>
                            <c:param name="starEnabled" value="true"/>
                            <c:param name="starred" value="${not empty song.starredDate}"/>
                            <c:param name="asTable" value="true"/>
                        </c:import>

                        <td class="fit"><input type="checkbox" id="songIndex${loopStatus.count - 1}">
                            <span id="songId${loopStatus.count - 1}" style="display: none">${song.id}</span></td>

                        <c:if test="${model.visibility.trackNumberVisible}">
                            <td class="fit rightalign">
                                <span class="detail">${song.trackNumber}</span>
                            </td>
                        </c:if>

                        <td class="truncate">
                            <span class="songTitle" title="${fn:escapeXml(song.title)}">${fn:escapeXml(song.title)}</span>
                        </td>

                        <c:if test="${model.visibility.albumVisible}">
                            <td class="truncate">
                                <span class="detail" title="${fn:escapeXml(song.albumName)}">${fn:escapeXml(song.albumName)}</span>
                            </td>
                        </c:if>

                        <c:if test="${model.visibility.artistVisible}">
                            <td class="truncate">
                                <span class="detail" title="${fn:escapeXml(song.artist)}">${fn:escapeXml(song.artist)}</span>
                            </td>
                        </c:if>

                        <c:if test="${model.visibility.genreVisible}">
                            <td class="fit rightalign">
                                <span class="detail">${fn:escapeXml(song.genre)}</span>
                            </td>
                        </c:if>

                        <c:if test="${model.visibility.yearVisible}">
                            <td class="fit rightalign">
                                <span class="detail">${song.year}</span>
                            </td>
                        </c:if>

                        <c:if test="${model.visibility.formatVisible}">
                            <td class="fit rightalign">
                                <span class="detail">${fn:toLowerCase(song.format)}</span>
                            </td>
                        </c:if>

                        <c:if test="${model.visibility.fileSizeVisible}">
                            <td class="fit rightalign">
                                <span class="detail"><sub:formatBytes bytes="${song.fileSize}"/></span>
                            </td>
                        </c:if>

                        <c:if test="${model.visibility.durationVisible}">
                            <td class="fit rightalign">
                                <span class="detail">${song.durationString}</span>
                            </td>
                        </c:if>

                        <c:if test="${model.visibility.bitRateVisible}">
                            <td class="fit rightalign">
                                <span class="detail">
                                    <c:if test="${not empty song.bitRate}">
                                        ${song.bitRate} Kbps ${song.variableBitRate ? "vbr" : ""}
                                    </c:if>
                                    <c:if test="${song.video and not empty song.width and not empty song.height}">
                                        (${song.width}x${song.height})
                                    </c:if>
                                </span>
                            </td>
                        </c:if>
                    </tr>
                </c:forEach>
            </table>
        </td>

        <td class="fit" style="vertical-align:top;" rowspan="3">
            <div class="albumThumb">
                <c:import url="coverArt.jsp">
                    <c:param name="albumId" value="${model.dir.id}"/>
                    <c:param name="coverArtSize" value="${model.coverArtSizeLarge}"/>
                    <c:param name="showZoom" value="true"/>
                    <c:param name="showChange" value="${model.user.coverArtRole}"/>
                </c:import>
            </div>
        </td>
    </tr>

    <tr>
        <td>
            <select id="moreActions" onchange="actionSelected(this.options[selectedIndex].id);" style="margin-bottom:1.0em">
                <option id="top" selected="selected"><fmt:message key="main.more.selection"/></option>
                <option id="selectAll">&nbsp;&nbsp;<fmt:message key="playlist.more.selectall"/></option>
                <option id="selectNone">&nbsp;&nbsp;<fmt:message key="playlist.more.selectnone"/></option>
                <c:if test="${model.user.downloadRole}">
                    <option id="download">&nbsp;&nbsp;<fmt:message key="common.download"/></option>
                </c:if>
                <c:if test="${model.user.shareRole}">
                    <option id="share">&nbsp;&nbsp;<fmt:message key="main.more.share"/></option>
                </c:if>
                <option id="appendPlaylist">&nbsp;&nbsp;<fmt:message key="playlist.append"/></option>
            </select>
        </td>
    </tr>

    <tr>
        <td style="vertical-align:top;height: 100%">
            <table class="music indent">
                <c:forEach items="${model.subDirs}" var="child" varStatus="loopStatus">
                    <sub:url value="main.view" var="albumUrl">
                        <sub:param name="id" value="${child.id}"/>
                    </sub:url>
                    <tr>
                        <c:import url="playButtons.jsp">
                            <c:param name="id" value="${child.id}"/>
                            <c:param name="playEnabled" value="${model.user.streamRole and not model.partyModeEnabled}"/>
                            <c:param name="addEnabled" value="${model.user.streamRole and not model.partyModeEnabled}"/>
                            <c:param name="asTable" value="true"/>
                        </c:import>
                        <td class="truncate"><a href="${albumUrl}" title="${fn:escapeXml(child.name)}">${fn:escapeXml(child.name)}</a></td>
                        <td></td>
                    </tr>
                </c:forEach>
                <c:if test="${model.viewAsList}">
                    <c:forEach items="${model.sieblingAlbums}" var="album" varStatus="loopStatus">
                        <tr>
                            <c:import url="playButtons.jsp">
                                <c:param name="id" value="${album.id}"/>
                                <c:param name="playEnabled" value="${model.user.streamRole and not model.partyModeEnabled}"/>
                                <c:param name="addEnabled" value="${model.user.streamRole and not model.partyModeEnabled}"/>
                                <c:param name="asTable" value="true"/>
                            </c:import>
                            <td class="truncate"><a href="main.view?id=${album.id}" title="${fn:escapeXml(album.name)}">${fn:escapeXml(album.name)}</a></td>
                            <td class="fit rightalign detail">${album.year}</td>
                        </tr>
                    </c:forEach>
                </c:if>
            </table>
        </td>

    </tr>
</table>
<c:if test="${model.thereIsMore}">
    <input id="showAllButton" class="albumOverflowButton" type="button" value="<fmt:message key="main.showall"/>" onclick="showAllAlbums()">
</c:if>

<c:if test="${not model.viewAsList}">
    <div style="float: left">
        <c:forEach items="${model.sieblingAlbums}" var="album" varStatus="loopStatus">
            <div class="albumThumb">
                <c:import url="coverArt.jsp">
                    <c:param name="albumId" value="${album.id}"/>
                    <c:param name="caption1" value="${fn:escapeXml(album.name)}"/>
                    <c:param name="caption2" value="${album.year}"/>
                    <c:param name="captionCount" value="2"/>
                    <c:param name="coverArtSize" value="${model.coverArtSizeMedium}"/>
                    <c:param name="showLink" value="true"/>
                    <c:param name="appearAfter" value="${loopStatus.count * 30}"/>
                    <c:param name="hideOverflow" value="true"/>
                </c:import>
            </div>
        </c:forEach>
        <c:if test="${model.thereIsMore}">
            <input id="showAllButton" class="albumOverflowButton" type="button" value="<fmt:message key="main.showall"/>" onclick="showAllAlbums()">
        </c:if>
    </div>
</c:if>

<table style="width: 90%">
    <tr><td>
        <span id="similarArtistsTitle" style="padding-right: 0.5em; display: none"><fmt:message key="main.similarartists"/>:</span>
        <span id="similarArtists"></span>
    </td></tr>
    <tr><td style="padding-bottom: 0.5em">
        <div id="similarArtistsRadio" class="forward" style="display: none">
            <a href="javascript:playSimilar()"><fmt:message key="main.startradio"/></a>
        </div>
    </td></tr>
    <tr><td style="height: 100%"></td></tr>
</table>

<div id="dialog-select-playlist" title="<fmt:message key="main.addtoplaylist.title"/>" style="display: none;">
    <p><fmt:message key="main.addtoplaylist.text"/></p>
    <div id="dialog-select-playlist-list"></div>
</div>

</body>
</html>
