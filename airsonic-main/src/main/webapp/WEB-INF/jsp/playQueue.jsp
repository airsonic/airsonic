<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>
<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <script type="text/javascript" src="<c:url value='/script/utils.js'/>"></script>
    <script type="text/javascript" src="<c:url value='/dwr/interface/nowPlayingService.js'/>"></script>
    <script type="text/javascript" src="<c:url value='/dwr/interface/playQueueService.js'/>"></script>
    <script type="text/javascript" src="<c:url value='/dwr/interface/playlistService.js'/>"></script>
    <script type="text/javascript" src="<c:url value='/dwr/engine.js'/>"></script>
    <script type="text/javascript" src="<c:url value='/dwr/util.js'/>"></script>
    <script type="text/javascript" src="<c:url value='/script/mediaelement/mediaelement-and-player.min.js'/>"></script>
    <script type="text/javascript" src="<c:url value='/script/playQueueCast.js'/>"></script>
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

<body class="bgcolor2 playlistframe" onload="init()">

<span id="dummy-animation-target" style="max-width: ${model.autoHide ? 50 : 150}px; display: none"></span>

<script type="text/javascript" language="javascript">

    // These variables store the media player state, received from DWR in the
    // playQueueCallback function below.

    // List of songs (of type PlayQueueInfo.Entry)
    var songs = null;

    // Stream URL of the media being played
    var currentStreamUrl = null;

    // Current song being played
    var currentSong = null;

    // Is autorepeat enabled?
    var repeatEnabled = false;

    // Is the "shuffle radio" playing? (More > Shuffle Radio)
    var shuffleRadioEnabled = false;

    // Is the "internet radio" playing?
    var internetRadioEnabled = false;

    // Is the play queue visible? (Initially hidden if set to "auto-hide" in the settings)
    var isVisible = ${model.autoHide ? 'false' : 'true'};

    // Initialize the Cast player (ChromeCast support)
    var CastPlayer = new CastPlayer();

    function init() {
        <c:if test="${model.autoHide}">initAutoHide();</c:if>

        dwr.engine.setErrorHandler(null);
        startTimer();

        $("#dialog-select-playlist").dialog({resizable: true, height: 220, autoOpen: false,
            buttons: {
                "<fmt:message key="common.cancel"/>": function() {
                    $(this).dialog("close");
                }
            }});

        <c:if test="${model.player.web}">createMediaElementPlayer();</c:if>

        $("#playlistBody").sortable({
            stop: function(event, ui) {
                var indexes = [];
                $("#playlistBody").children().each(function() {
                    var id = $(this).attr("id").replace("pattern", "");
                    if (id.length > 0) {
                        indexes.push(parseInt(id) - 1);
                    }
                });
                onRearrange(indexes);
            },
            cursor: "move",
            axis: "y",
            containment: "parent",
            helper: function(e, tr) {
                var originals = tr.children();
                var trclone = tr.clone();
                trclone.children().each(function(index) {
                    // Set cloned cell sizes to match the original sizes
                    $(this).width(originals.eq(index).width());
                    $(this).css("maxWidth", originals.eq(index).width());
                    $(this).css("border-top", "1px solid black");
                    $(this).css("border-bottom", "1px solid black");
                });
                return trclone;
            }
        });

        /** Toggle between <a> and <span> in order to disable play queue action buttons */
        $.fn.toggleLink = function(newState) {
            $(this).each(function(ix, elt) {

                var node, currentState;
                if (elt.tagName.toLowerCase() === "a") currentState = true;
                else if (elt.tagName.toLowerCase() === "span") currentState = false;
                else return true;
                if (typeof newState === 'undefined') newState = !currentState;
                if (newState === currentState) return true;

                if (newState) node = document.createElement("a");
                else node = document.createElement("span");

                node.innerHTML = elt.innerHTML;
                if (elt.hasAttribute("id")) node.setAttribute("id", elt.getAttribute("id"));
                if (elt.hasAttribute("style")) node.setAttribute("style", elt.getAttribute("style"));
                if (elt.hasAttribute("class")) node.setAttribute("class", elt.getAttribute("class"));

                if (newState) {
                    if (elt.hasAttribute("data-href")) node.setAttribute("href", elt.getAttribute("data-href"));
                    node.classList.remove("disabled");
                    node.removeAttribute("aria-disabled");
                } else {
                    if (elt.hasAttribute("href")) node.setAttribute("data-href", elt.getAttribute("href"));
                    node.classList.add("disabled");
                    node.setAttribute("aria-disabled", "true");
                }

                elt.parentNode.replaceChild(node, elt);
                return true;
            });
        };

        getPlayQueue();
    }

    function onHidePlayQueue() {
      setFrameHeight(50);
      isVisible = false;
      $(".playqueue-shown").hide();
      $(".playqueue-hidden").show();
    }

    function onShowPlayQueue() {
      var height = $("body").height() + 25;
      height = Math.min(height, window.top.innerHeight * 0.8);
      setFrameHeight(height);
      isVisible = true;
      $(".playqueue-shown").show();
      $(".playqueue-hidden").hide();
    }

    function onTogglePlayQueue() {
      if (isVisible) onHidePlayQueue();
      else onShowPlayQueue();
    }

    function initAutoHide() {
        $(window).mouseleave(function (event) {
            if (event.clientY < 30) onHidePlayQueue();
        });

        $(window).mouseenter(function () {
            onShowPlayQueue();
        });
    }

    function setFrameHeight(height) {
        <%-- Disable animation in Chrome. It stopped working in Chrome 44. --%>
        var duration = navigator.userAgent.indexOf("Chrome") != -1 ? 0 : 400;

        $("#dummy-animation-target").stop();
        $("#dummy-animation-target").animate({"max-width": height}, {
            step: function (now, fx) {
                top.document.getElementById("playQueueFrameset").rows = "*," + now;
            },
            duration: duration
        });
    }

    function startTimer() {
        <!-- Periodically check if the current song has changed. -->
        nowPlayingService.getNowPlayingForCurrentPlayer(nowPlayingCallback);
        setTimeout("startTimer()", 10000);
    }

    function nowPlayingCallback(nowPlayingInfo) {
        if (nowPlayingInfo != null && nowPlayingInfo.streamUrl != currentStreamUrl) {
            getPlayQueue();
        <c:if test="${not model.player.web}">
            currentStreamUrl = nowPlayingInfo.streamUrl;
            currentSong = null;
            onPlaying();
        </c:if>
        }
    }

    /**
     * Callback function called when playback for the current song has ended.
     */
    function onEnded() {
        onNext(repeatEnabled);
    }

    /**
     * Callback function called when playback for the current song has started.
     */
    function onPlaying() {
        updateCurrentImage();

        if (currentSong) {
            updateWindowTitle(currentSong);
            <c:if test="${model.notify}">
            showNotification(currentSong);
            </c:if>
        }
    }

    /**
     * Initialize the Media Element Player, including callbacks.
     */
    function createMediaElementPlayer() {
        // Manually run MediaElement.js initialization.
        //
        // Warning: Bugs will happen if MediaElement.js is not initialized when
        // we modify the media elements (e.g. adding event handlers). Running
        // MediaElement.js's automatic initialization does not guarantee that
        // (it depends on when we call createMediaElementPlayer at load time).
        $('#audioPlayer').mediaelementplayer();

        // Once playback reaches the end, go to the next song, if any.
        $('#audioPlayer').on("ended", onEnded);

        // Whenever playback starts, show a notification for the current playing song.
        $('#audioPlayer').on("playing", onPlaying);
    }

    function getPlayQueue() {
        playQueueService.getPlayQueue(playQueueCallback);
    }

    function onClear() {
        var ok = true;
    <c:if test="${model.partyMode}">
        ok = confirm("<fmt:message key="playlist.confirmclear"/>");
    </c:if>
        if (ok) {
            playQueueService.clear(playQueueCallback);
        }
    }

    /**
     * Start/resume playing from the current playlist
     */
    function onStart() {
        if (CastPlayer.castSession) {
            CastPlayer.playCast();
        } else if ($('#audioPlayer').get(0)) {
            if ($('#audioPlayer').get(0).src) {
                $('#audioPlayer').get(0).play();  // Resume playing if the player was paused
            }
            else {
                loadPlayer(0);  // Start the first track if the player was not yet loaded
            }
        } else {
            playQueueService.start(playQueueCallback);
        }
    }

    /**
     * Pause playing
     */
    function onStop() {
        if (CastPlayer.castSession) {
            CastPlayer.pauseCast();
        } else if ($('#audioPlayer').get(0)) {
            $('#audioPlayer').get(0).pause();
        } else {
            playQueueService.stop(playQueueCallback);
        }
    }

    /**
     * Toggle play/pause
     *
     * FIXME: Only works for the Web player for now
     */
    function onToggleStartStop() {
        if (CastPlayer.castSession) {
            var playing = CastPlayer.mediaSession && CastPlayer.mediaSession.playerState == chrome.cast.media.PlayerState.PLAYING;
            if (playing) onStop();
            else onStart();
        } else if ($('#audioPlayer').get(0)) {
            var playing = $("#audioPlayer").get(0).paused != null && !$("#audioPlayer").get(0).paused;
            if (playing) onStop();
            else onStart();
        } else {
            playQueueService.toggleStartStop(playQueueCallback);
        }
    }

    function onGain(gain) {
        playQueueService.setGain(gain);
    }
    function onJukeboxVolumeChanged() {
        var value = parseInt($("#jukeboxVolume").slider("option", "value"));
        onGain(value / 100);
    }
    function onCastVolumeChanged() {
        var value = parseInt($("#castVolume").slider("option", "value"));
        CastPlayer.setCastVolume(value / 100, false);
    }

    /**
     * Increase or decrease volume by a certain amount
     *
     * @param gain amount to add or remove from the current volume
     */
    function onGainAdd(gain) {
        if (CastPlayer.castSession) {
            var volume = parseInt($("#castVolume").slider("option", "value")) + gain;
            if (volume > 100) volume = 100;
            if (volume < 0) volume = 0;
            CastPlayer.setCastVolume(volume / 100, false);
            $("#castVolume").slider("option", "value", volume); // Need to update UI
        } else if ($('#audioPlayer').get(0)) {
            var volume = parseFloat($('#audioPlayer').get(0).volume)*100 + gain;
            if (volume > 100) volume = 100;
            if (volume < 0) volume = 0;
            $('#audioPlayer').get(0).volume = volume / 100;
        } else {
            var volume = parseInt($("#jukeboxVolume").slider("option", "value")) + gain;
            if (volume > 100) volume = 100;
            if (volume < 0) volume = 0;
            onGain(volume / 100);
            $("#jukeboxVolume").slider("option", "value", volume); // Need to update UI
        }
    }

    function onSkip(index) {
    <c:choose>
    <c:when test="${model.player.web}">
        loadPlayer(index);
    </c:when>
    <c:otherwise>
        currentStreamUrl = songs[index].streamUrl;
        if (isJavaJukeboxPresent()) {
            updateJavaJukeboxPlayerControlBar(songs[index]);
        }
        playQueueService.skip(index, playQueueCallback);
    </c:otherwise>
    </c:choose>
    }
    function onNext(wrap) {
        var index = parseInt(getCurrentSongIndex()) + 1;
        if (shuffleRadioEnabled && index >= songs.length) {
            playQueueService.reloadSearchCriteria(function(playQueue) {
                playQueueCallback(playQueue);
                onSkip(index);
            });
            return;
        } else if (wrap) {
            index = index % songs.length;
        }
        onSkip(index);
    }
    function onPrevious() {
        onSkip(parseInt(getCurrentSongIndex()) - 1);
    }
    function onPlay(id) {
        playQueueService.play(id, playQueueCallback);
    }
    function onPlayShuffle(albumListType, offset, size, genre, decade) {
        playQueueService.playShuffle(albumListType, offset, size, genre, decade, playQueueCallback);
    }
    function onPlayPlaylist(id, index) {
        playQueueService.playPlaylist(id, index, playQueueCallback);
    }
    function onPlayInternetRadio(id, index) {
        playQueueService.playInternetRadio(id, index, playQueueCallback);
    }
    function onPlayTopSong(id, index) {
        playQueueService.playTopSong(id, index, playQueueCallback);
    }
    function onPlayPodcastChannel(id) {
        playQueueService.playPodcastChannel(id, playQueueCallback);
    }
    function onPlayPodcastEpisode(id) {
        playQueueService.playPodcastEpisode(id, playQueueCallback);
    }
    function onPlayNewestPodcastEpisode(index) {
        playQueueService.playNewestPodcastEpisode(index, playQueueCallback);
    }
    function onPlayStarred() {
        playQueueService.playStarred(playQueueCallback);
    }
    function onPlayRandom(id, count) {
        playQueueService.playRandom(id, count, playQueueCallback);
    }
    function onPlaySimilar(id, count) {
        playQueueService.playSimilar(id, count, playQueueCallback);
    }
    function onAdd(id) {
        playQueueService.add(id, playQueueCallback);
    }
    function onAddNext(id) {
        playQueueService.addAt(id, getCurrentSongIndex() + 1, playQueueCallback);
    }
    function onAddPlaylist(id) {
        playQueueService.addPlaylist(id, playQueueCallback);
    }
    function onShuffle() {
        playQueueService.shuffle(playQueueCallback);
    }
    function onStar(index) {
        playQueueService.toggleStar(index, playQueueCallback);
    }
    function onStarCurrent() {
        onStar(getCurrentSongIndex());
    }
    function onRemove(index) {
        playQueueService.remove(index, playQueueCallback);
    }
    function onRemoveSelected() {
        var indexes = new Array();
        var counter = 0;
        for (var i = 0; i < songs.length; i++) {
            var index = i + 1;
            if ($("#songIndex" + index).is(":checked")) {
                indexes[counter++] = i;
            }
        }
        playQueueService.removeMany(indexes, playQueueCallback);
    }

    function onRearrange(indexes) {
        playQueueService.rearrange(indexes, playQueueCallback);
    }
    function onToggleRepeat() {
        playQueueService.toggleRepeat(playQueueCallback);
    }
    function onUndo() {
        playQueueService.undo(playQueueCallback);
    }
    function onSortByTrack() {
        playQueueService.sortByTrack(playQueueCallback);
    }
    function onSortByArtist() {
        playQueueService.sortByArtist(playQueueCallback);
    }
    function onSortByAlbum() {
        playQueueService.sortByAlbum(playQueueCallback);
    }
    function onSavePlayQueue() {
        var positionMillis = $('#audioPlayer').get(0) ? Math.round(1000.0 * $('#audioPlayer').get(0).currentTime) : 0;
        playQueueService.savePlayQueue(getCurrentSongIndex(), positionMillis);
        $().toastmessage("showSuccessToast", "<fmt:message key="playlist.toast.saveplayqueue"/>");
    }
    function onLoadPlayQueue() {
        playQueueService.loadPlayQueue(playQueueCallback);
    }
    function onSavePlaylist() {
        playlistService.createPlaylistForPlayQueue(function (playlistId) {
            top.left.updatePlaylists();
            top.left.showAllPlaylists();
            top.main.location.href = "playlist.view?id=" + playlistId;
            $().toastmessage("showSuccessToast", "<fmt:message key="playlist.toast.saveasplaylist"/>");
        });
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
        for (var i = 0; i < songs.length; i++) {
            if ($("#songIndex" + (i + 1)).is(":checked")) {
                mediaFileIds.push(songs[i].id);
            }
        }
        playlistService.appendToPlaylist(playlistId, mediaFileIds, function (){
            top.left.updatePlaylists();
            top.main.location.href = "playlist.view?id=" + playlistId;
            $().toastmessage("showSuccessToast", "<fmt:message key="playlist.toast.appendtoplaylist"/>");
        });
    }

    function isJavaJukeboxPresent() {
        return $("#javaJukeboxPlayerControlBarContainer").length==1;
    }

    function playQueueCallback(playQueue) {
        songs = playQueue.entries;
        repeatEnabled = playQueue.repeatEnabled;
        shuffleRadioEnabled = playQueue.shuffleRadioEnabled;
        internetRadioEnabled = playQueue.internetRadioEnabled;

        // If an internet radio has no sources, display a message to the user.
        if (internetRadioEnabled && songs.length == 0) {
            top.main.$().toastmessage("showErrorToast", "<fmt:message key="playlist.toast.radioerror"/>");
            onStop();
        }

        if ($("#start")) {
            $("#start").toggle(!playQueue.stopEnabled);
            $("#stop").toggle(playQueue.stopEnabled);
        }

        if ($("#toggleRepeat")) {
            if (shuffleRadioEnabled) {
                $("#toggleRepeat").html("<fmt:message key="playlist.repeat_radio"/>");
            } else if (repeatEnabled) {
                $("#toggleRepeat").attr('src', '<spring:theme code="repeatOn"/>');
                $("#toggleRepeat").attr('alt', 'Repeat On');
            } else {
                $("#toggleRepeat").attr('src', '<spring:theme code="repeatOff"/>');
                $("#toggleRepeat").attr('alt', 'Repeat Off');
            }
        }

        // Disable some UI items if internet radio is playing
        $("select#moreActions #loadPlayQueue").prop("disabled", internetRadioEnabled);
        $("select#moreActions #savePlayQueue").prop("disabled", internetRadioEnabled);
        $("select#moreActions #savePlaylist").prop("disabled", internetRadioEnabled);
        $("select#moreActions #downloadPlaylist").prop("disabled", internetRadioEnabled);
        $("select#moreActions #sharePlaylist").prop("disabled", internetRadioEnabled);
        $("select#moreActions #sortByTrack").prop("disabled", internetRadioEnabled);
        $("select#moreActions #sortByAlbum").prop("disabled", internetRadioEnabled);
        $("select#moreActions #sortByArtist").prop("disabled", internetRadioEnabled);
        $("select#moreActions #selectAll").prop("disabled", internetRadioEnabled);
        $("select#moreActions #selectNone").prop("disabled", internetRadioEnabled);
        $("select#moreActions #removeSelected").prop("disabled", internetRadioEnabled);
        $("select#moreActions #download").prop("disabled", internetRadioEnabled);
        $("select#moreActions #appendPlaylist").prop("disabled", internetRadioEnabled);
        $("#shuffleQueue").toggleLink(!internetRadioEnabled);
        $("#repeatQueue").toggleLink(!internetRadioEnabled);
        $("#undoQueue").toggleLink(!internetRadioEnabled);

        if (songs.length == 0) {
            $("#songCountAndDuration").text("");
            $("#empty").show();
        } else {
            $("#songCountAndDuration").html(songs.length + " <fmt:message key="playlist2.songs"/> &ndash; " + playQueue.durationAsString);
            $("#empty").hide();
        }

        // On the web player, the play button is handled by MEJS and does
        // nothing if a first song hasn't been loaded.
        <c:if test="${model.player.web}">
        if (songs.length > 0 && !currentStreamUrl) preparePlayer(0);
        </c:if>

        // Delete all the rows except for the "pattern" row
        dwr.util.removeAllRows("playlistBody", { filter:function(tr) {
            return (tr.id != "pattern");
        }});

        // Create a new set cloned from the pattern row
        for (var i = 0; i < songs.length; i++) {
            var song  = songs[i];
            var id = i + 1;
            dwr.util.cloneNode("pattern", { idSuffix:id });
            if ($("#trackNumber" + id)) {
                $("#trackNumber" + id).text(song.trackNumber);
            }

            if (!internetRadioEnabled) {
                // Show star/remove buttons in all cases...
                $("#starSong" + id).show();
                $("#removeSong" + id).show();
                $("#songIndex" + id).show();

                // Show star rating
                if (song.starred) {
                    $("#starSong" + id).attr("src", "<spring:theme code='ratingOnImage'/>");
                } else {
                    $("#starSong" + id).attr("src", "<spring:theme code='ratingOffImage'/>");
                }
            } else {
                // ...except from when internet radio is playing.
                $("#starSong" + id).hide();
                $("#removeSong" + id).hide();
                $("#songIndex" + id).hide();
            }

            if ($("#currentImage" + id) && song.streamUrl == currentStreamUrl) {
                $("#currentImage" + id).show();
                if (isJavaJukeboxPresent()) {
                    updateJavaJukeboxPlayerControlBar(song);
                }
            }
            if ($("#title" + id)) {
                $("#title" + id).text(song.title);
                $("#title" + id).attr("title", song.title);
            }
            if ($("#titleUrl" + id)) {
                $("#titleUrl" + id).text(song.title);
                $("#titleUrl" + id).attr("title", song.title);
                $("#titleUrl" + id).click(function () {onSkip(this.id.substring(8) - 1)});
            }
            if ($("#album" + id)) {
                $("#album" + id).text(song.album);
                $("#album" + id).attr("title", song.album);
                $("#albumUrl" + id).attr("href", song.albumUrl);
                // Open external internet radio links in new windows
                if (internetRadioEnabled) {
                    $("#albumUrl" + id).attr({
                        target: "_blank",
                        rel: "noopener noreferrer",
                    });
                }
            }
            if ($("#artist" + id)) {
                $("#artist" + id).text(song.artist);
                $("#artist" + id).attr("title", song.artist);
            }
            if ($("#genre" + id)) {
                $("#genre" + id).text(song.genre);
            }
            if ($("#year" + id)) {
                // If song.year is not an int, this will return NaN, which
                // conveniently returns false in all boolean operations.
                if (parseInt(song.year) > 0) {
                    $("#year" + id).text(song.year);
                } else {
                    $("#year" + id).text("");
                }
            }
            if ($("#bitRate" + id)) {
                $("#bitRate" + id).text(song.bitRate);
            }
            if ($("#duration" + id)) {
                $("#duration" + id).text(song.durationAsString);
            }
            if ($("#format" + id)) {
                $("#format" + id).text(song.format);
            }
            if ($("#fileSize" + id)) {
                $("#fileSize" + id).text(song.fileSize);
            }

            $("#pattern" + id).addClass((i % 2 == 0) ? "bgcolor1" : "bgcolor2");

            // Note: show() method causes page to scroll to top.
            $("#pattern" + id).css("display", "table-row");
        }

        if (playQueue.sendM3U) {
            parent.frames.main.location.href="play.m3u?";
        }

        var jukeboxVolume = $("#jukeboxVolume");
        if (jukeboxVolume) {
            jukeboxVolume.slider("option", "value", Math.floor(playQueue.gain * 100));
        }

    <c:if test="${model.player.web}">
        triggerPlayer(playQueue.startPlayerAt, playQueue.startPlayerAtPosition);
    </c:if>
    }

    function triggerPlayer(index, positionMillis) {
        if (index != -1) {
            if (songs.length > index) {
                loadPlayer(index);
                if (positionMillis != 0) {
                    $('#audioPlayer').get(0).currentTime = positionMillis / 1000;
                }
            }
        }
        updateCurrentImage();
        if (songs.length == 0) {
            $('#audioPlayer').get(0).stop();
        }
    }

    /**
     * Return the current Media Element Player instance
     */
    function getMediaElementPlayer() {
        return $('#audioPlayer').get(0);
    }

    /**
     * Prepare playback for a song on the Cast player.
     *
     * @param song         song object
     * @param position     position in the song, in seconds
     */
    function prepareCastPlayer(song, position) {
        // The Cast player does not support preloading a song, so we don't do
        // anything here and return directly.
    }

    /**
     * Prepare playback for a song on the Media Element player.
     *
     * @param song         song object
     * @param position     position in the song, in seconds
     */
    function prepareMediaElementPlayer(song, position) {
        // The Media Element player supports setting the current song URL,
        // which allows the user to start playback by interacting with the
        // controls, without going through the Airsonic code.
        getMediaElementPlayer().src = song.streamUrl;
    }

    /**
     * Start playing a song on the Cast player.
     *
     * @param song         song object
     * @param position     position in the song, in seconds
     */
    function loadCastPlayer(song, position, prepareOnly) {
        // Start playback immediately
        CastPlayer.loadCastMedia(song, position);
        // The Cast player does not handle events, so we call the 'onPlaying' function manually.
        onPlaying();
    }

    /**
     * Start playing a song on the Media Element player.
     *
     * @param song         song object
     * @param position     position in the song, in seconds
     */
    function loadMediaElementPlayer(song, position) {
        // Retrieve the media element player
        var player = getMediaElementPlayer();
        // The player should have been prepared by `preparePlayer`, but if
        // it hasn't we set it here so that the function behaves correctly.
        if (player.src != song.streamUrl) player.src = song.streamUrl;
        // Inform MEJS that we need to load the media source. The
        // 'canplay' event will be fired once playback is possible.
        player.load();
        // The 'skip' function takes a 'position' argument. We don't
        // usually send it, and in this case it's better to do nothing.
        // Otherwise, the 'canplay' event will also be fired after
        // setting 'currentTime'.
        if (position && position > 0) {
            player.currentTime = position;
        }
        // Start playback immediately. The 'onPlaying' function will be called
        // when the player notifies us that playback has started.
        player.play();
    }

    /**
     * Prepare for playing a song by its index on the active player.
     *
     * The song is not played immediately, but set as the current song.
     *
     * The active player is then prepared for playback: in Media Element
     * Player's case, for example, this lets the player start playback using
     * its own controls without relying on Airsonic code.
     *
     * @param index        song index in the play queue
     * @param position     position in the song, in seconds
     */
    function preparePlayer(index, position) {

        // Check that the song index is valid for the current play queue
        if (index < 0 || index >= songs.length) {
            return;
        }

        // Set the current song, index and URL as global variables
        currentSong = songs[index];
        currentStreamUrl = currentSong.streamUrl;

        // Run player-specific preparation code
        if (CastPlayer.castSession) {
            prepareCastPlayer(currentSong, position);
        } else {
            prepareMediaElementPlayer(currentSong, position);
        }

        return currentSong;
    }

    /**
     * Start playing a song by its index on the active player.
     *
     * @param index        song index in the play queue
     * @param position     position in the song, in seconds
     */
    function loadPlayer(index, position) {
        // Prepare playback on the active player
        var song = preparePlayer(index, position);
        if (!song) return;
        // Run player-specific code to start playback
        if (CastPlayer.castSession) {
            loadCastPlayer(song, position);
        } else {
            loadMediaElementPlayer(song, position);
        }
    }

    function updateWindowTitle(song) {
        top.document.title = song.title + " - " + song.artist + " - Airsonic";
    }

    function showNotification(song) {
        if (!("Notification" in window)) {
            return;
        }
        if (Notification.permission === "granted") {
            createNotification(song);
        }
        else if (Notification.permission !== 'denied') {
            Notification.requestPermission(function (permission) {
                Notification.permission = permission;
                if (permission === "granted") {
                    createNotification(song);
                }
            });
        }
    }

    function createNotification(song) {
        var n = new Notification(song.title, {
            tag: "airsonic",
            body: song.artist + " - " + song.album,
            icon: "coverArt.view?id=" + song.id + "&size=110"
        });
        n.onshow = function() {
            setTimeout(function() {n.close()}, 5000);
        }
    }

    function updateCurrentImage() {
        for (var i = 0; i < songs.length; i++) {
            var song  = songs[i];
            var id = i + 1;
            var image = $("#currentImage" + id);

            if (image) {
                if (song.streamUrl == currentStreamUrl) {
                    image.show();
                } else {
                    image.hide();
                }
            }
        }
    }

    function getCurrentSongIndex() {
        for (var i = 0; i < songs.length; i++) {
            if (songs[i].streamUrl == currentStreamUrl) {
                return i;
            }
        }
        return -1;
    }

    <!-- actionSelected() is invoked when the users selects from the "More actions..." combo box. -->
    function actionSelected(id) {
        var selectedIndexes = getSelectedIndexes();
        if (id == "top") {
            return;
        } else if (id == "savePlayQueue") {
            onSavePlayQueue();
        } else if (id == "loadPlayQueue") {
            onLoadPlayQueue();
        } else if (id == "savePlaylist") {
            onSavePlaylist();
        } else if (id == "downloadPlaylist") {
            location.href = "download.view?player=${model.player.id}";
        } else if (id == "sharePlaylist") {
            parent.frames.main.location.href = "createShare.view?player=${model.player.id}&" + getSelectedIndexes();
        } else if (id == "sortByTrack") {
            onSortByTrack();
        } else if (id == "sortByArtist") {
            onSortByArtist();
        } else if (id == "sortByAlbum") {
            onSortByAlbum();
        } else if (id == "selectAll") {
            selectAll(true);
        } else if (id == "selectNone") {
            selectAll(false);
        } else if (id == "removeSelected") {
            onRemoveSelected();
        } else if (id == "download" && selectedIndexes != "") {
            location.href = "download.view?player=${model.player.id}&" + selectedIndexes;
        } else if (id == "appendPlaylist" && selectedIndexes != "") {
            onAppendPlaylist();
        }
        $("#moreActions").prop("selectedIndex", 0);
    }

    function getSelectedIndexes() {
        var result = "";
        for (var i = 0; i < songs.length; i++) {
            if ($("#songIndex" + (i + 1)).is(":checked")) {
                result += "i=" + i + "&";
            }
        }
        return result;
    }

    function selectAll(b) {
        for (var i = 0; i < songs.length; i++) {
            if (b) {
                $("#songIndex" + (i + 1)).attr("checked", "checked");
            } else {
                $("#songIndex" + (i + 1)).removeAttr("checked");
            }
        }
    }

</script>

<c:choose>
    <c:when test="${model.player.javaJukebox}">
        <div id="javaJukeboxPlayerControlBarContainer">
            <%@ include file="javaJukeboxPlayerControlBar.jspf" %>
        </div>
    </c:when>
    <c:otherwise>
        <div class="bgcolor2" style="position:fixed; bottom:0; width:100%;padding-top:10px;">
            <table style="white-space:nowrap; margin-bottom:0;">
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
                            <div id="player" style="width:340px; height:40px">
                                <audio id="audioPlayer" data-mejsoptions='{"alwaysShowControls": true, "enableKeyboard": false}' width="340px" height="40px" tabindex="-1" />
                            </div>
                            <div id="castPlayer" style="display: none">
                                <div style="float:left">
                                    <img alt="Play" id="castPlay" src="<spring:theme code='castPlayImage'/>" onclick="CastPlayer.playCast()" style="cursor:pointer">
                                    <img alt="Pause" id="castPause" src="<spring:theme code='castPauseImage'/>" onclick="CastPlayer.pauseCast()" style="cursor:pointer; display:none">
                                    <img alt="Mute on" id="castMuteOn" src="<spring:theme code='volumeImage'/>" onclick="CastPlayer.castMuteOn()" style="cursor:pointer">
                                    <img alt="Mute off" id="castMuteOff" src="<spring:theme code='muteImage'/>" onclick="CastPlayer.castMuteOff()" style="cursor:pointer; display:none">
                                </div>
                                <div style="float:left">
                                    <div id="castVolume" style="width:80px;height:4px;margin-left:10px;margin-right:10px;margin-top:8px"></div>
                                    <script type="text/javascript">
                                        $("#castVolume").slider({max: 100, value: 50, animate: "fast", range: "min"});
                                        $("#castVolume").on("slidestop", onCastVolumeChanged);
                                    </script>
                                </div>
                            </div>
                        </td>
                        <td>
                            <img alt="Cast on" id="castOn" src="<spring:theme code='castIdleImage'/>" onclick="CastPlayer.launchCastApp()" style="cursor:pointer; display:none">
                            <img alt="Cast off" id="castOff" src="<spring:theme code='castActiveImage'/>" onclick="CastPlayer.stopCastApp()" style="cursor:pointer; display:none">
                        </td>
                    </c:if>

                    <c:if test="${model.user.streamRole and not model.player.web}">
                        <td>
                            <img alt="Start" id="start" src="<spring:theme code='castPlayImage'/>" onclick="onStart()" style="cursor:pointer">
                            <img alt="Stop" id="stop" src="<spring:theme code='castPauseImage'/>" onclick="onStop()" style="cursor:pointer; display:none">
                        </td>
                    </c:if>

                    <c:if test="${model.player.jukebox}">
                        <td style="white-space:nowrap;">
                            <img src="<spring:theme code='volumeImage'/>" alt="">
                        </td>
                        <td style="white-space:nowrap;">
                            <div id="jukeboxVolume" style="width:80px;height:4px"></div>
                            <script type="text/javascript">
                                $("#jukeboxVolume").slider({max: 100, value: 50, animate: "fast", range: "min"});
                                $("#jukeboxVolume").on("slidestop", onJukeboxVolumeChanged);
                            </script>
                        </td>
                    </c:if>

                    <c:if test="${model.player.web}">
                        <td><span class="header">
                            <img src="<spring:theme code='backImage'/>" alt="Play previous" title="Play previous" onclick="onPrevious()" style="cursor:pointer"></span>
                        </td>
                        <td><span class="header">
                            <img src="<spring:theme code='forwardImage'/>" alt="Play next" title="Play next" onclick="onNext(false)" style="cursor:pointer"></span> |
                        </td>
                    </c:if>

                    <td style="white-space:nowrap;">
                      <span class="header">
                        <a href="javascript:onClear()" class="player-control">
                            <img src="<spring:theme code='clearImage'/>" alt="Clear playlist" title="Clear playlist" style="cursor:pointer; height:18px">
                        </a>
                      </span> |</td>

                    <td style="white-space:nowrap;">
                      <span class="header">
                        <a href="javascript:onShuffle()" id="shuffleQueue" class="player-control">
                            <img src="<spring:theme code='shuffleImage'/>" alt="Shuffle" title="Shuffle" style="cursor:pointer; height:18px">
                        </a>
                      </span> |</td>

                    <c:if test="${model.player.web or model.player.jukebox or model.player.external}">
                        <td style="white-space:nowrap;">
                          <span class="header">
                            <a href="javascript:onToggleRepeat()" id="repeatQueue" class="player-control">
                              <img id="toggleRepeat" src="<spring:theme code='repeatOn'/>" alt="Toggle repeat" title="Toggle repeat" style="cursor:pointer; height:18px">
                            </a>
                          </span> |</td>
                    </c:if>

                    <td style="white-space:nowrap;">
                      <span class="header">
                        <a href="javascript:onUndo()" id="undoQueue" class="player-control">
                          <img src="<spring:theme code='undoImage'/>" alt="Undo" title="Undo" style="cursor:pointer; height:18px">
                        </a>
                      </span>  |</td>

                    <c:if test="${model.user.settingsRole}">
                        <td style="white-space:nowrap;">
                          <span class="header">
                            <a href="playerSettings.view?id=${model.player.id}" target="main" class="player-control">
                              <img src="<spring:theme code='settingsImage'/>" alt="Settings" title="Settings" style="cursor:pointer; height:18px">
                            </a>
                          </span> |</td>
                    </c:if>

                    <td style="white-space:nowrap;"><select id="moreActions" onchange="actionSelected(this.options[selectedIndex].id)">
                        <option id="top" selected="selected"><fmt:message key="playlist.more"/></option>
                        <optgroup label="<fmt:message key='playlist.more.playlist'/>">
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
                        <optgroup label="<fmt:message key='playlist.more.selection'/>">
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

                    <c:if test="${not model.autoHide}">
                    <td style="white-space:nowrap; text-align:right; width:100%; padding-right:1.5em">
                      <a href="javascript:onTogglePlayQueue()">
                        <img class="playqueue-shown" src="<spring:theme code='playQueueHide'/>" alt="Hide play queue" title="Hide play queue" style="cursor:pointer; height:18px;"/>
                        <img class="playqueue-hidden" src="<spring:theme code='playQueueShow'/>" alt="Show play queue" title="Show play queue" style="cursor:pointer; height:18px; display: none;"/>
                      </a>
                    </td>
                    </c:if>

                </tr></table>
        </div>
    </c:otherwise>
</c:choose>


<h2 style="float:left"><fmt:message key="playlist.more.playlist"/></h2>
<h2 id="songCountAndDuration" style="float:right;padding-right:1em"></h2>
<div style="clear:both"></div>
<p id="empty"><em><fmt:message key="playlist.empty"/></em></p>

<table class="music indent" style="cursor:pointer">
    <tbody id="playlistBody">
        <tr id="pattern" style="display:none;margin:0;padding:0;border:0">
            <td class="fit">
                <img id="starSong" onclick="onStar(this.id.substring(8) - 1)" src="<spring:theme code='ratingOffImage'/>"
                     style="cursor:pointer;height:18px;" alt="" title=""></td>
            <td class="fit">
                <img id="removeSong" onclick="onRemove(this.id.substring(10) - 1)" src="<spring:theme code='removeImage'/>"
                     style="cursor:pointer; height:18px;" alt="<fmt:message key='playlist.remove'/>" title="<fmt:message key='playlist.remove'/>"></td>
            <td class="fit"><input type="checkbox" class="checkbox" id="songIndex"></td>

            <c:if test="${model.visibility.trackNumberVisible}">
                <td class="fit rightalign"><span class="detail" id="trackNumber">1</span></td>
            </c:if>

            <td class="truncate">
                <img id="currentImage" src="<spring:theme code='currentImage'/>" alt="" style="display:none;padding-right: 0.5em">
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

<div id="dialog-select-playlist" title="<fmt:message key='main.addtoplaylist.title'/>" style="display: none;">
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
