
var songPlayingTimerId = null;

var javaJukeboxPlayerModel = {
  currentStreamUrl : null,
  playing : false,
  songDuration : null,
  songPosition : 0
}

function refreshView() {
  if (javaJukeboxPlayerModel.playing == true) {
    if (songPlayingTimerId == null) {
      songPlayingTimerId = setInterval(songPlayingTimer, 1000);
    }
    document.getElementById('startIcon').style.display = 'none';
    document.getElementById('pauseIcon').style.display = 'block';
  } else {
    if (songPlayingTimerId != null) {
        clearInterval(songPlayingTimerId);
        songPlayingTimerId = null;
    }
    document.getElementById('pauseIcon').style.display = 'none';
    document.getElementById('startIcon').style.display = 'block';
  }
  if (javaJukeboxPlayerModel.songDuration == null) {
    $("#playingDurationDisplay").html("-:--");
  } else {
    $("#playingDurationDisplay").html(songTimeAsString(javaJukeboxPlayerModel.songDuration));
  }
  $("#playingPositionDisplay").html(songTimeAsString(javaJukeboxPlayerModel.songPosition));
  $("#javaJukeboxSongPositionSlider").slider("value",javaJukeboxPlayerModel.songPosition);
}

function onJavaJukeboxStart() {
  playQueueService.start();
  javaJukeboxPlayerModel.playing = true;
  refreshView();
}

function onJavaJukeboxStop() {
  playQueueService.stop();
  javaJukeboxPlayerModel.playing = false;
  refreshView();
}

function onJavaJukeboxVolumeChanged() {
    var value = $("#javaJukeboxVolumeSlider").slider("value");
    var gain = value / 100;
    playQueueService.setGain(gain);
}

function onJavaJukeboxPositionChanged() {
    var pos = $("#javaJukeboxSongPositionSlider").slider("value");
    playQueueService.setJukeboxPosition(pos);
    javaJukeboxPlayerModel.songPosition = pos;
    refreshView();
}

function updateJavaJukeboxPlayerControlBar(song){
    if (song != null) {
        var playingStream = song.streamUrl;
        if (playingStream != javaJukeboxPlayerModel.currentStreamUrl) {
            javaJukeboxPlayerModel.currentStreamUrl = playingStream;
            newSongPlaying(song);
        }
    }
}

function songTimeAsString(timeInSeconds) {
    var minutes = Math.floor(timeInSeconds / 60);
    var seconds = timeInSeconds - minutes * 60;

    return minutes + ":" + ("00" + seconds).slice(-2);
}

function newSongPlaying(song) {
    javaJukeboxPlayerModel.songDuration = song.duration;
    $("#javaJukeboxSongPositionSlider").slider({max: javaJukeboxPlayerModel.songDuration, value: 0, animate: "fast", range: "min"});
    javaJukeboxPlayerModel.playing = true;
    javaJukeboxPlayerModel.songPosition = 0;
    refreshView();
}

function songPlayingTimer() {
    javaJukeboxPlayerModel.songPosition += 1;
    refreshView();
}

function initJavaJukeboxPlayerControlBar() {
    $("#javaJukeboxSongPositionSlider").slider({max: 100, value: 0, animate: "fast", range: "min"});
    $("#javaJukeboxSongPositionSlider").slider("value",0);
    $("#javaJukeboxSongPositionSlider").on("slidestop", onJavaJukeboxPositionChanged);

    $("#javaJukeboxVolumeSlider").slider({max: 100, value: 50, animate: "fast", range: "min"});
    $("#javaJukeboxVolumeSlider").on("slidestop", onJavaJukeboxVolumeChanged);

    refreshView();
}
