<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <link type="text/css" rel="stylesheet" href="<c:url value="/script/webfx/luna.css"/>">
    <script type="text/javascript" src="<c:url value="/script/scripts-2.0.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/interface/nowPlayingService.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/interface/playQueueService.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/interface/playlistService.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/engine.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/util.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/script/mediaelement/mediaelement-and-player.min.js"/>"></script>
    <%@ include file="playQueueCast.jsp" %>
    <script type="text/javascript" src="<c:url value="/script/playQueue/playQueue.js"/>"></script>
    <script type="text/javascript" language="javascript">
        $(document).ready(function() {
            var model = {
                player : {
                    web : ${model.player.web}
                },
                partyMode : ${model.partyMode},
                notify : ${model.notify},
                autoHide : ${model.autoHide},
            };
            var resources = {
                common : {
                    cancel : '<fmt:message key="common.cancel"/>'
                },
                playlist : {
                    confirmclear : '<fmt:message key="playlist.confirmclear"/>',
                    toast : {
                        saveplayqueue : '<fmt:message key="playlist.toast.saveplayqueue"/>',
                        saveasplaylist : '<fmt:message key="playlist.toast.saveasplaylist"/>',
                        appendtoplaylist : '<fmt:message key="playlist.toast.appendtoplaylist"/>'
                    },
                    repeat_radio : '<fmt:message key="playlist.repeat_radio"/>',
                    repeat_on : '<fmt:message key="playlist.repeat_on"/>',
                    repeat_off : '<fmt:message key="playlist.repeat_off"/>'
                },
                playlist2 : {
                    songs : '<fmt:message key="playlist2.songs"/>'
                }
            };
            libresonic.playQueue.init(model,resources);
        });
    </script>

    <link type="text/css" rel="stylesheet" href="<c:url value="/script/webfx/luna.css"/>">
    <link type="text/css" rel="stylesheet" href="<c:url value="/script/mediaelement/mediaelementplayer.min.css"/>">
    <style type="text/css">
        .ui-slider .ui-slider-handle {
            width: 11px;
            height: 11px;
            cursor: pointer;
        }
        .ui-slider a {
            outline:none;
        }
        .ui-slider {
            cursor: pointer;
        }
    </style>
</head>

<body class="bgcolor2 playlistframe">

<span id="dummy-animation-target" style="max-width: ${model.autoHide ? 50 : 150}px; display: none"></span>


<div class="bgcolor2" style="position:fixed; bottom:0; width:100%;padding-top:10px;">
    <table style="white-space:nowrap;">
        <tr style="white-space:nowrap;">
            <c:if test="${model.user.settingsRole and fn:length(model.players) gt 1}">
                <td style="padding-right: 5px"><select name="player" onchange="location='playQueue.view?player=' + options[selectedIndex].value;">
                    <c:forEach items="${model.players}" var="player">
                        <option ${player.id eq model.player.id ? "selected" : ""} value="${player.id}">${player.shortDescription}</option>
                    </c:forEach>
                </select></td>
            </c:if>
            <c:if test="${model.player.web}">
                <td>
                    <div id="player" style="width:340px; height:40px;padding-right:10px">
                        <audio id="audioPlayer" class="mejs__player" data-mejsoptions='{"alwaysShowControls": "true"}' width="340px" height"40px"/>
                    </div>
                    <div id="castPlayer" style="display: none">
                        <div style="float:left">
                            <img id="castPlay" src="<spring:theme code="castPlayImage"/>" onclick="CastPlayer.playCast()" style="cursor:pointer">
                            <img id="castPause" src="<spring:theme code="castPauseImage"/>" onclick="CastPlayer.pauseCast()" style="cursor:pointer; display:none">
                            <img id="castMuteOn" src="<spring:theme code="volumeImage"/>" onclick="CastPlayer.castMuteOn()" style="cursor:pointer">
                            <img id="castMuteOff" src="<spring:theme code="muteImage"/>" onclick="CastPlayer.castMuteOff()" style="cursor:pointer; display:none">
                        </div>
                        <div style="float:left">
                            <div id="castVolume" style="width:80px;height:4px;margin-left:10px;margin-right:10px;margin-top:8px"></div>
                            <script type="text/javascript">
                                $("#castVolume").slider({max: 100, value: 50, animate: "fast", range: "min"});
                                $("#castVolume").on("slidestop", libresonic.playQueue.onCastVolumeChanged);
                            </script>
                        </div>
                    </div>
                </td>
                <td>
                    <img id="castOn" src="<spring:theme code="castIdleImage"/>" onclick="CastPlayer.launchCastApp()" style="cursor:pointer; display:none">
                    <img id="castOff" src="<spring:theme code="castActiveImage"/>" onclick="CastPlayer.stopCastApp()" style="cursor:pointer; display:none">
                </td>
            </c:if>

            <c:if test="${model.user.streamRole and not model.player.web}">
                <td>
                    <img id="start" src="<spring:theme code="castPlayImage"/>" onclick="libresonic.playQueue.onStart()" style="cursor:pointer">
                    <img id="stop" src="<spring:theme code="castPauseImage"/>" onclick="libresonic.playQueue.onStop()" style="cursor:pointer; display:none">
                </td>
            </c:if>

            <c:if test="${model.player.jukebox}">
                <td style="white-space:nowrap;">
                    <img src="<spring:theme code="volumeImage"/>" alt="">
                </td>
                <td style="white-space:nowrap;">
                    <div id="jukeboxVolume" style="width:80px;height:4px"></div>
                    <script type="text/javascript">
                        $("#jukeboxVolume").slider({max: 100, value: 50, animate: "fast", range: "min"});
                        $("#jukeboxVolume").on("slidestop", libresonic.playQueue.onJukeboxVolumeChanged);
                    </script>
                </td>
            </c:if>

            <c:if test="${model.player.web}">
                <td><span class="header">
                    <img src="<spring:theme code="backImage"/>" alt="" onclick="libresonic.playQueue.onPrevious()" style="cursor:pointer"></span>
                </td>
                <td><span class="header">
                    <img src="<spring:theme code="forwardImage"/>" alt="" onclick="libresonic.playQueue.onNext(false)" style="cursor:pointer"></span>
                </td>
            </c:if>

            <td style="white-space:nowrap;"><span class="header"><a href="javascript:libresonic.playQueue.onClear()"><fmt:message key="playlist.clear"/></a></span> |</td>
            <td style="white-space:nowrap;"><span class="header"><a href="javascript:libresonic.playQueue.onShuffle()"><fmt:message key="playlist.shuffle"/></a></span> |</td>

            <c:if test="${model.player.web or model.player.jukebox or model.player.external}">
                <td style="white-space:nowrap;"><span class="header"><a href="javascript:libresonic.playQueue.onToggleRepeat()"><span id="toggleRepeat"><fmt:message key="playlist.repeat_on"/></span></a></span>  |</td>
            </c:if>

            <td style="white-space:nowrap;"><span class="header"><a href="javascript:libresonic.playQueue.onUndo()"><fmt:message key="playlist.undo"/></a></span>  |</td>

            <c:if test="${model.user.settingsRole}">
                <td style="white-space:nowrap;"><span class="header"><a href="playerSettings.view?id=${model.player.id}" target="main"><fmt:message key="playlist.settings"/></a></span>  |</td>
            </c:if>

            <td style="white-space:nowrap;"><select id="moreActions" onchange="libresonic.playQueue.actionSelected(this.options[selectedIndex].id)">
                <option id="top" selected="selected"><fmt:message key="playlist.more"/></option>
                <optgroup label="<fmt:message key="playlist.more.playlist"/>">
                    <option id="savePlayQueue"><fmt:message key="playlist.saveplayqueue"/></option>
                    <option id="loadPlayQueue"><fmt:message key="playlist.loadplayqueue"/></option>
                    <option id="savePlaylist"><fmt:message key="playlist.save"/></option>
                    <c:if test="${model.user.downloadRole}">
                    <option id="downloadPlaylist"><fmt:message key="common.download"/></option>
                    </c:if>
                    <c:if test="${model.user.shareRole}">
                    <option id="sharePlaylist"><fmt:message key="main.more.share"/></option>
                    </c:if>
                    <option id="sortByTrack"><fmt:message key="playlist.more.sortbytrack"/></option>
                    <option id="sortByAlbum"><fmt:message key="playlist.more.sortbyalbum"/></option>
                    <option id="sortByArtist"><fmt:message key="playlist.more.sortbyartist"/></option>
                </optgroup>
                <optgroup label="<fmt:message key="playlist.more.selection"/>">
                    <option id="selectAll"><fmt:message key="playlist.more.selectall"/></option>
                    <option id="selectNone"><fmt:message key="playlist.more.selectnone"/></option>
                    <option id="removeSelected"><fmt:message key="playlist.remove"/></option>
                    <c:if test="${model.user.downloadRole}">
                        <option id="download"><fmt:message key="common.download"/></option>
                    </c:if>
                    <option id="appendPlaylist"><fmt:message key="playlist.append"/></option>
                </optgroup>
            </select>
            </td>

        </tr></table>
</div>

<h2 style="float:left"><fmt:message key="playlist.more.playlist"/></h2>
<h2 id="songCountAndDuration" style="float:right;padding-right:1em"></h2>
<div style="clear:both"></div>
<p id="empty"><em><fmt:message key="playlist.empty"/></em></p>

<table class="music indent" style="cursor:pointer">
    <tbody id="playlistBody">
        <tr id="pattern" style="display:none;margin:0;padding:0;border:0">
            <td class="fit">
                <img id="starSong" onclick="libresonic.playQueue.onStar(this.id.substring(8) - 1)" src="<spring:theme code="ratingOffImage"/>"
                     style="cursor:pointer" alt="" title=""></td>
            <td class="fit">
                <img id="removeSong" onclick="libresonic.playQueue.onRemove(this.id.substring(10) - 1)" src="<spring:theme code="removeImage"/>"
                     style="cursor:pointer" alt="<fmt:message key="playlist.remove"/>" title="<fmt:message key="playlist.remove"/>"></td>
            <td class="fit"><input type="checkbox" class="checkbox" id="songIndex"></td>

            <c:if test="${model.visibility.trackNumberVisible}">
                <td class="fit rightalign"><span class="detail" id="trackNumber">1</span></td>
            </c:if>

            <td class="truncate">
                <img id="currentImage" src="<spring:theme code="currentImage"/>" alt="" style="display:none;padding-right: 0.5em">
                <c:choose>
                    <c:when test="${model.player.externalWithPlaylist}">
                        <span id="title" class="songTitle">Title</span>
                    </c:when>
                    <c:otherwise>
                        <span class="songTitle"><a id="titleUrl" href="javascript:void(0)">Title</a></span>
                    </c:otherwise>
                </c:choose>
            </td>

            <c:if test="${model.visibility.albumVisible}">
                <td class="truncate"><a id="albumUrl" target="main"><span id="album" class="detail">Album</span></a></td>
            </c:if>
            <c:if test="${model.visibility.artistVisible}">
                <td class="truncate"><span id="artist" class="detail">Artist</span></td>
            </c:if>
            <c:if test="${model.visibility.genreVisible}">
                <td class="truncate"><span id="genre" class="detail">Genre</span></td>
            </c:if>
            <c:if test="${model.visibility.yearVisible}">
                <td class="fit rightalign"><span id="year" class="detail">Year</span></td>
            </c:if>
            <c:if test="${model.visibility.formatVisible}">
                <td class="fit rightalign"><span id="format" class="detail">Format</span></td>
            </c:if>
            <c:if test="${model.visibility.fileSizeVisible}">
                <td class="fit rightalign"><span id="fileSize" class="detail">Format</span></td>
            </c:if>
            <c:if test="${model.visibility.durationVisible}">
                <td class="fit rightalign"><span id="duration" class="detail">Duration</span></td>
            </c:if>
            <c:if test="${model.visibility.bitRateVisible}">
                <td class="fit rightalign"><span id="bitRate" class="detail">Bit Rate</span></td>
            </c:if>
        </tr>
    </tbody>
</table>

<div style="height:3.2em"></div>

<div id="dialog-select-playlist" title="<fmt:message key="main.addtoplaylist.title"/>" style="display: none;">
    <p><fmt:message key="main.addtoplaylist.text"/></p>
    <div id="dialog-select-playlist-list"></div>
</div>

<script type="text/javascript">
    window['__onGCastApiAvailable'] = function(isAvailable) {
        if (isAvailable) {
            CastPlayer.initializeCastPlayer();
        }
    };
</script>
<script type="text/javascript" src="https://www.gstatic.com/cv/js/sender/v1/cast_sender.js?loadCastFramework=1"></script>

</body></html>
