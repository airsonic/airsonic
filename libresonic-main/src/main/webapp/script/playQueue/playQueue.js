var libresonic = {};

libresonic.playQueue = (function() {

  var _debug = false;

  var _model;
  var _resources;
  var _isVisible = false;
  var _songs = null;
  var _currentStreamUrl = null;
  var _repeatEnabled = false;
  var _radioEnabled = false;
  var _CastPlayer = new CastPlayer();

  var _cancelToto;

  function onHidePlayQueue() {
    setFrameHeight(50);
    _isVisible = false;
  }

  function onShowPlayQueue() {
    var height = $("body").height() + 25;
    height = Math.min(height, window.top.innerHeight * 0.8);
    setFrameHeight(height);
    _isVisible = true;
  }

  function onTogglePlayQueue() {
    if (_isVisible) onHidePlayQueue();
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
      // Disable animation in Chrome. It stopped working in Chrome 44.
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
      // Periodically check if the current song has changed.
      nowPlayingService.getNowPlayingForCurrentPlayer(nowPlayingCallback);
      setTimeout(startTimer, 10000);
  }

  function nowPlayingCallback(nowPlayingInfo) {
      if (nowPlayingInfo != null && nowPlayingInfo.streamUrl != _currentStreamUrl) {
          getPlayQueue();
          if (! _model.player.web) {
              _currentStreamUrl = nowPlayingInfo.streamUrl;
              updateCurrentImage();
          }
      }
  }

  function onEnded() {
      onNext(_repeatEnabled);
  }

  function onNext(wrap) {
      var index = parseInt(getCurrentSongIndex()) + 1;
      if (_radioEnabled && index >= _songs.length) {
          playQueueService.reloadSearchCriteria(function(playQueue) {
              playQueueCallback(playQueue);
              onSkip(index);
          });
          return;
      } else if (wrap) {
          index = index % _songs.length;
      }
      onSkip(index);
  }


  function createPlayer() {
      $('#audioPlayer').get(0).addEventListener("ended", onEnded);
  }

  function getPlayQueue() {
      playQueueService.getPlayQueue(playQueueCallback);
  }

  /**
   * Toggle play/pause
   *
   * FIXME: Only works for the Web player for now
   */
  function onToggleStartStop() {
      if (_CastPlayer.castSession) {
          var playing = _CastPlayer.mediaSession && _CastPlayer.mediaSession.playerState == chrome.cast.media.PlayerState.PLAYING;
          if (playing) onStop();
          else onStart();
      } else if ($('#audioPlayer')) {
          if (!$('#audioPlayer').get(0).paused) onStop();
          else onStart();
      } else {
          playQueueService.toggleStartStop(playQueueCallback);
      }
  }

  function onGain(gain) {
      playQueueService.setGain(gain);
  }

  /**
   * Increase or decrease volume by a certain amount
   *
   * @param amount to add or remove from the current volume
   */
  function onGainAdd(gain) {
      if (_CastPlayer.castSession) {
          var volume = parseInt($("#castVolume").slider("option", "value")) + gain;
          if (volume > 100) volume = 100;
          if (volume < 0) volume = 0;
          _CastPlayer.setCastVolume(volume / 100, false);
          $("#castVolume").slider("option", "value", volume); // Need to update UI
      } else if ($('#audioPlayer')) {
          var volume = parseInt($('#audioPlayer').get(0).volume) + gain;
          if (volume > 100) volume = 100;
          if (volume < 0) volume = 0;
          $('#audioPlayer').get(0).volume = volume;
      } else {
          var volume = parseInt($("#jukeboxVolume").slider("option", "value")) + gain;
          if (volume > 100) volume = 100;
          if (volume < 0) volume = 0;
          onGain(volume / 100);
          $("#jukeboxVolume").slider("option", "value", volume); // Need to update UI
      }
  }

  function onSkip(index) {
    if (_model.player.web) {
      skip(index);
    } else {
      _currentStreamUrl = _songs[index].streamUrl;
      playQueueService.skip(index, playQueueCallback);
    }
  }

  function onPlayShuffle(albumListType, offset, size, genre, decade) {
      playQueueService.playShuffle(albumListType, offset, size, genre, decade, playQueueCallback);
  }

  function onPlayPlaylist(id, index) {
      playQueueService.playPlaylist(id, index, playQueueCallback);
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

  function onStarCurrent() {
      onStar(getCurrentSongIndex());
  }

  function onRemoveSelected() {
      var indexes = new Array();
      var counter = 0;
      for (var i = 0; i < _songs.length; i++) {
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
      var positionMillis = $('#audioPlayer') ? Math.round(1000.0 * $('#audioPlayer').get(0).currentTime) : 0;
      playQueueService.savePlayQueue(getCurrentSongIndex(), positionMillis);
      $().toastmessage("showSuccessToast", _resources.playlist.toast.saveplayqueue);
  }

  function onLoadPlayQueue() {
      playQueueService.loadPlayQueue(playQueueCallback);
  }

  function onSavePlaylist() {
      playlistService.createPlaylistForPlayQueue(function (playlistId) {
          top.left.updatePlaylists();
          top.left.showAllPlaylists();
          top.main.location.href = "playlist.view?id=" + playlistId;
          $().toastmessage("showSuccessToast", _resources.playlist.toast.saveasplaylist);
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
      for (var i = 0; i < _songs.length; i++) {
          if ($("#songIndex" + (i + 1)).is(":checked")) {
              mediaFileIds.push(_songs[i].id);
          }
      }
      playlistService.appendToPlaylist(playlistId, mediaFileIds, function (){
          top.left.updatePlaylists();
          top.main.location.href = "playlist.view?id=" + playlistId;
          $().toastmessage("showSuccessToast", _resources.playlist.toast.appendtoplaylist);
      });
  }

  function playQueueCallback(playQueue) {
      if (_debug) {
        console.debug("playQueueCallback");
      }
      _songs = playQueue.entries;
      _repeatEnabled = playQueue.repeatEnabled;
      _radioEnabled = playQueue.radioEnabled;
      if ($("#start")) {
          $("#start").toggle(!playQueue.stopEnabled);
          $("#stop").toggle(playQueue.stopEnabled);
      }

      if ($("#toggleRepeat")) {
          if (_radioEnabled) {
              $("#toggleRepeat").html(_resources.playlist.repeat_radio);
          } else if (_repeatEnabled) {
              $("#toggleRepeat").html(_resources.playlist.repeat_on);
          } else {
              $("#toggleRepeat").html(_resources.playlist.repeat_off);
          }
      }

      if (_songs.length == 0) {
          $("#songCountAndDuration").html("");
          $("#empty").show();
      } else {
          $("#songCountAndDuration").html(_songs.length + " " + _resources.playlist2.songs + "&ndash; " + playQueue.durationAsString);
          $("#empty").hide();
      }

      // Delete all the rows except for the "pattern" row
      dwr.util.removeAllRows("playlistBody", { filter:function(tr) {
          return (tr.id != "pattern");
      }});

      // Create a new set cloned from the pattern row
      for (var i = 0; i < _songs.length; i++) {
          var song  = _songs[i];
          var id = i + 1;
          dwr.util.cloneNode("pattern", { idSuffix:id });
          if ($("#trackNumber" + id)) {
              $("#trackNumber" + id).html(song.trackNumber);
          }
          if (song.starred) {
              $("#starSong" + id).attr("src", "<spring:theme code='ratingOnImage'/>");
          } else {
              $("#starSong" + id).attr("src", "<spring:theme code='ratingOffImage'/>");
          }
          if ($("#currentImage" + id) && song.streamUrl == _currentStreamUrl) {
              $("#currentImage" + id).show();
          }
          if ($("#title" + id)) {
              $("#title" + id).html(song.title);
              $("#title" + id).attr("title", song.title);
          }
          if ($("#titleUrl" + id)) {
              $("#titleUrl" + id).html(song.title);
              $("#titleUrl" + id).attr("title", song.title);
              $("#titleUrl" + id).click(function () {onSkip(this.id.substring(8) - 1)});
          }
          if ($("#album" + id)) {
              $("#album" + id).html(song.album);
              $("#album" + id).attr("title", song.album);
              $("#albumUrl" + id).attr("href", song.albumUrl);
          }
          if ($("#artist" + id)) {
              $("#artist" + id).html(song.artist);
              $("#artist" + id).attr("title", song.artist);
          }
          if ($("#genre" + id)) {
              $("#genre" + id).html(song.genre);
          }
          if ($("#year" + id)) {
              $("#year" + id).html(song.year);
          }
          if ($("#bitRate" + id)) {
              $("#bitRate" + id).html(song.bitRate);
          }
          if ($("#duration" + id)) {
              $("#duration" + id).html(song.durationAsString);
          }
          if ($("#format" + id)) {
              $("#format" + id).html(song.format);
          }
          if ($("#fileSize" + id)) {
              $("#fileSize" + id).html(song.fileSize);
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

      if (_model.player.web) {
        triggerPlayer(playQueue.startPlayerAt, playQueue.startPlayerAtPosition);
      }
  }

  function triggerPlayer(index, positionMillis) {
      if (index != -1) {
          if (_songs.length > index) {
              skip(index);
              if (positionMillis != 0) {
                  $('#audioPlayer').get(0).currentTime = positionMillis / 1000;
              }
          }
      }
      updateCurrentImage();
      if (_songs.length == 0) {
          $('#audioPlayer').get(0).stop();
      }
  }

  function skip(index, position) {
      if (index < 0 || index >= _songs.length) {
          return;
      }

      var song = _songs[index];
      _currentStreamUrl = song.streamUrl;
      updateCurrentImage();

      if (_CastPlayer.castSession) {
          _CastPlayer.loadCastMedia(song, position);
      } else {
          $('#audioPlayer').get(0).src = song.streamUrl;
          $('#audioPlayer').get(0).load();
          $('#audioPlayer').get(0).play();
          console.log(song.streamUrl);
      }

      updateWindowTitle(song);

      if (_model.notify) {
        showNotification(song);
      }
  }

  function updateWindowTitle(song) {
      top.document.title = song.title + " - " + song.artist + " - Libresonic";
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
          tag: "libresonic",
          body: song.artist + " - " + song.album,
          icon: "coverArt.view?id=" + song.id + "&size=110"
      });
      n.onshow = function() {
          setTimeout(function() {n.close()}, 5000);
      }
  }

  function updateCurrentImage() {
      for (var i = 0; i < _songs.length; i++) {
          var song  = _songs[i];
          var id = i + 1;
          var image = $("#currentImage" + id);

          if (image) {
              if (song.streamUrl == _currentStreamUrl) {
                  image.show();
              } else {
                  image.hide();
              }
          }
      }
  }

  function getCurrentSongIndex() {
      for (var i = 0; i < _songs.length; i++) {
          if (_songs[i].streamUrl == _currentStreamUrl) {
              return i;
          }
      }
      return -1;
  }

  function getSelectedIndexes() {
      var result = "";
      for (var i = 0; i < _songs.length; i++) {
          if ($("#songIndex" + (i + 1)).is(":checked")) {
              result += "i=" + i + "&";
          }
      }
      return result;
  }

  function selectAll(b) {
      for (var i = 0; i < _songs.length; i++) {
          if (b) {
              $("#songIndex" + (i + 1)).attr("checked", "checked");
          } else {
              $("#songIndex" + (i + 1)).removeAttr("checked");
          }
      }
  }


  /* public functions */

  return {
    init : function (model,resources) {
      _model = model;
      _resources = resources;
      _isVisible = !_model.autoHide;

      if (_model.autoHide == true) {
        initAutoHide();
      }

      dwr.engine.setErrorHandler(null);
      startTimer();

      $("#dialog-select-playlist").dialog({resizable: true, height: 220, autoOpen: false,
          buttons : [
              {
                text : _resources.common.cancel,
                click : function() {
                    $(this).dialog("close");
                }
              }
          ]
      });

      if (_model.player.web) {
        createPlayer();
      }

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

      getPlayQueue();
    }, // init
    onClear : function() {
        var ok = true;
        if (_model.partyMode) {
          ok = confirm(_resources.playlist.confirmclear);
        }
        if (ok) {
            playQueueService.clear(playQueueCallback);
        }
    }, //onClear
    /**
     * Start playing from the current playlist
     */
    onStart : function() {
        if (_CastPlayer.castSession) {
            _CastPlayer.playCast();
        } else if ($('#audioPlayer')) {
            var audioPlayer = $('#audioPlayer');
            if(audioPlayer.paused) {
                skip(0, audioPlayer.currentTime);
            }
        } else {
            playQueueService.start(playQueueCallback);
        }
    }, //onStart
    /**
     * Pause playing
     */
    onStop : function() {
        if (_CastPlayer.castSession) {
            _CastPlayer.pauseCast();
        } else if ($('#audioPlayer')) {
            $('#audioPlayer').get(0).pause();
        } else {
            playQueueService.stop(playQueueCallback);
        }
    }, // onStop
    onJukeboxVolumeChanged : function() {
        var value = parseInt($("#jukeboxVolume").slider("option", "value"));
        onGain(value / 100);
    }, // onJukeboxVolumeChanged
    onCastVolumeChanged : function() {
        var value = parseInt($("#castVolume").slider("option", "value"));
        _CastPlayer.setCastVolume(value / 100, false);
    }, // onCastVolumeChanged
    onNext : function(wrap) {
      onNext(wrap);
    }, // onNext
    onPrevious : function() {
        onSkip(parseInt(getCurrentSongIndex()) - 1);
    }, // onPrevious
    onShuffle : function() {
        playQueueService.shuffle(playQueueCallback);
    }, // onShuffle
    onStar : function(index) {
        playQueueService.toggleStar(index, playQueueCallback);
    }, // onStar
    onRemove : function(index) {
        playQueueService.remove(index, playQueueCallback);
    }, //onRemove
    onToggleRepeat : function() {
        playQueueService.toggleRepeat(playQueueCallback);
    }, // onToggleRepeat
    onUndo : function() {
        playQueueService.undo(playQueueCallback);
    }, // onUndo
    // actionSelected() is invoked when the users selects from the "More actions..." combo box.
    actionSelected : function(id) {
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
    }, // actionSelected
    onPlay : function(id) {
        if (_debug) {
          console.debug("onPlay");
        }
        playQueueService.play(id, playQueueCallback);
    }
  }
})();
