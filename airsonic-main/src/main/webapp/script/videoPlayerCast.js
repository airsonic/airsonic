(function () {
    'use strict';

    /**
     * Constants of states for Chromecast device
     **/
    var DEVICE_STATE = {
        'NOT_PRESENT': 0,
        'IDLE': 1,
        'ACTIVE': 2,
        'WARNING': 3,
        'ERROR': 4
    };

    var PLAYER_STATE = {
        'IDLE': 'IDLE',
        'LOADING': 'LOADING',
        'LOADED': 'LOADED',
        'PLAYING': 'PLAYING',
        'PAUSED': 'PAUSED',
        'SEEKING': 'SEEKING'
    };

    /**
     * Cast player object
     * main variables:
     *  - deviceState for Cast mode:
     *    IDLE: Default state indicating that Cast extension is installed, but showing no current activity
     *    ACTIVE: Shown when Chrome has one or more local activities running on a receiver
     *    WARNING: Shown when the device is actively being used, but when one or more issues have occurred
     *    ERROR: Should not normally occur, but shown when there is a failure
     *  - Cast player variables for controlling Cast mode media playback
     *  - Local player variables for controlling local mode media playbacks
     *  - Current media variables for transition between Cast and local modes
     */
    var CastPlayer = function () {

        /* device variables */

        // @type {DEVICE_STATE} A state for device
        this.deviceState = DEVICE_STATE.NOT_PRESENT;

        /* Cast player variables */

        // @type {Object} a chrome.cast.media.Media object
        this.currentMediaSession = null;

        // @type {Number} volume between 0 and 100
        this.currentVolume = 50;

        // @type {Boolean} A flag for autoplay after load
        this.autoplay = true;

        // @type {string} a chrome.cast.Session object
        this.session = null;

        // @type {PLAYER_STATE} A state for Cast media player
        this.castPlayerState = PLAYER_STATE.IDLE;

        /* Local player variables */

        // @type {PLAYER_STATE} A state for local media player
        this.localPlayerState = PLAYER_STATE.IDLE;

        // @type {video} local player
        this.localPlayer = null;

        /* Current media variables */

        // @type {Boolean} Muted audio
        this.muted = false;

        // @type {Number} A number for current media offset
        this.currentMediaOffset = 0;

        // @type {Number} A number for current media time, relative to offset
        this.currentMediaTime = 0;

        // @type {Number} A number for current media duration
        this.currentMediaDuration = ${empty model.duration ? 0: model.duration};

        // @type {Boolean} A boolean to stop timer update of progress when triggered by media status event
        this.seekInProgress = false;

        this.updateDurationLabel();
        this.initializeUI();
        this.initializeLocalPlayer();
        this.initializeCastPlayer();
        this.playMediaLocally(0);
    };

    /**
     * Initialize local media player
     */
    CastPlayer.prototype.initializeLocalPlayer = function () {
        this.localPlayer = $('#videoPlayer').get(0);
        this.localPlayer.volume = this.currentVolume/100;
        this.localPlayer.addEventListener("timeupdate", this.updateLocalProgress.bind(this));
        this.localPlayer.addEventListener("play", this.updateLocalState.bind(this));
        this.localPlayer.addEventListener("pause", this.updateLocalState.bind(this));
        this.localPlayer.addEventListener("playing", this.updateLocalState.bind(this));
        this.localPlayer.addEventListener("paused", this.updateLocalState.bind(this));
    };

    CastPlayer.prototype.updateLocalProgress = function () {
        var newTime = Math.round(this.localPlayer.currentTime);
        if (newTime != this.currentMediaTime && !this.seekInProgress) {
            this.currentMediaTime = newTime;
            this.updateProgressBar();
        }
    };

    CastPlayer.prototype.updateLocalState = function () {
        if (this.localPlayer.paused) {
            this.localPlayerState = PLAYER_STATE.PAUSED;
        } else {
            this.localPlayerState = PLAYER_STATE.PLAYING;
        }
        this.updateMediaControlUI();
    };

    /**
     * Initialize Cast media player
     * Initializes the API. Note that either successCallback and errorCallback will be
     * invoked once the API has finished initialization. The sessionListener and
     * receiverListener may be invoked at any time afterwards, and possibly more than once.
     */
    CastPlayer.prototype.initializeCastPlayer = function () {

        if (!window.chrome) {
            return;
        }

        if (!chrome.cast || !chrome.cast.isAvailable) {
            setTimeout(this.initializeCastPlayer.bind(this), 1000);
            return;
        }
        // request session
        var applicationID = "9EAA0B71";
        var sessionRequest = new chrome.cast.SessionRequest(applicationID);
        var apiConfig = new chrome.cast.ApiConfig(sessionRequest,
                this.sessionListener.bind(this),
                this.receiverListener.bind(this));

        chrome.cast.initialize(apiConfig, this.onInitSuccess.bind(this), this.onError.bind(this));
        this.timer = setInterval(this.incrementMediaTime.bind(this), 1000);
    };

    /**
     * Callback function for init success
     */
    CastPlayer.prototype.onInitSuccess = function () {
        console.log("init success");
        this.updateMediaControlUI();
    };

    /**
     * Generic error callback function
     */
    CastPlayer.prototype.onError = function () {
        console.log("error");
    };

    /**
     * @param {!Object} e A new session
     * This handles auto-join when a page is reloaded
     * When active session is detected, playback will automatically
     * join existing session and occur in Cast mode and media
     * status gets synced up with current media of the session
     */
    CastPlayer.prototype.sessionListener = function (e) {
        this.session = e;
        if (this.session) {
            this.deviceState = DEVICE_STATE.ACTIVE;
            if (this.session.media[0]) {
                this.onMediaDiscovered('activeSession', this.session.media[0]);
            }
            else {
                this.loadMedia();
            }
            this.session.addUpdateListener(this.sessionUpdateListener.bind(this));
        }
    };

    /**
     * @param {string} e Receiver availability
     * This indicates availability of receivers but
     * does not provide a list of device IDs
     */
    CastPlayer.prototype.receiverListener = function (e) {
        if (e === 'available') {
            console.log("receiver found");
            this.deviceState = DEVICE_STATE.IDLE;
        }
        else {
            console.log("receiver list empty");
            this.deviceState = DEVICE_STATE.NOT_PRESENT;
        }
        this.updateMediaControlUI();
    };

    /**
     * session update listener
     */
    CastPlayer.prototype.sessionUpdateListener = function (isAlive) {
        if (!isAlive) {
            this.session = null;
            this.deviceState = DEVICE_STATE.IDLE;
            this.castPlayerState = PLAYER_STATE.IDLE;
            console.log(this.castPlayerState + " (sessionUpdateListener)");
            this.currentMediaSession = null;

            // continue to play media locally
            this.playMediaLocally(this.currentMediaOffset + this.currentMediaTime);
            this.updateMediaControlUI();
        }
    };

    /**
     * Requests that a receiver application session be created or joined. By default, the SessionRequest
     * passed to the API at initialization time is used; this may be overridden by passing a different
     * session request in opt_sessionRequest.
     */
    CastPlayer.prototype.launchApp = function () {
        console.log("launching app...");
        chrome.cast.requestSession(this.onRequestSessionSuccess.bind(this), this.onLaunchError.bind(this));
    };

    /**
     * Callback function for request session success
     * @param {Object} e A chrome.cast.Session object
     */
    CastPlayer.prototype.onRequestSessionSuccess = function (e) {
        console.log("session success: " + e.sessionId);
        this.session = e;
        this.deviceState = DEVICE_STATE.ACTIVE;
        this.stopMediaLocally();
        this.loadMedia();
        this.session.addUpdateListener(this.sessionUpdateListener.bind(this));
    };

    /**
     * Callback function for launch error
     */
    CastPlayer.prototype.onLaunchError = function () {
        console.log("launch error");
        this.deviceState = DEVICE_STATE.ERROR;
    };

    /**
     * Stops the running receiver application associated with the session.
     */
    CastPlayer.prototype.stopApp = function () {
        this.session.stop(this.onStopAppSuccess.bind(this, 'Session stopped'),
                this.onError.bind(this));

    };

    /**
     * Callback function for stop app success
     */
    CastPlayer.prototype.onStopAppSuccess = function (message) {
        console.log(message);
        this.deviceState = DEVICE_STATE.IDLE;
        this.castPlayerState = PLAYER_STATE.IDLE;
        console.log(this.castPlayerState + " (onStopAppSuccess)");
        this.currentMediaSession = null;

        // continue to play media locally
        this.playMediaLocally(this.currentMediaOffset + this.currentMediaTime);
        this.updateMediaControlUI();
    };

    /**
     * Loads media into a running receiver application
     */
    CastPlayer.prototype.loadMedia = function () {
        if (!this.session) {
            console.log("no session");
            return;
        }
        var offset = this.currentMediaOffset + this.currentMediaTime;
        this.currentMediaOffset = offset;
        this.currentMediaTime = 0;

        var url = "${model.remoteStreamUrl}" + "&maxBitRate=" + this.getBitRate() + "&format=mkv&timeOffset=" + offset;
        console.log("casting " + url);
        var mediaInfo = new chrome.cast.media.MediaInfo(url);
        mediaInfo.contentType = 'video/x-matroska';
        mediaInfo.streamType = chrome.cast.media.StreamType.BUFFERED;
        mediaInfo.duration = this.currentMediaDuration;
        mediaInfo.metadata = new chrome.cast.media.MovieMediaMetadata();
        mediaInfo.metadata.metadataType = chrome.cast.media.MetadataType.MOVIE;
        mediaInfo.metadata.title = "${model.video.title}";
        mediaInfo.metadata.images = [new chrome.cast.Image("${model.remoteCoverArtUrl}&size=384")];

        var request = new chrome.cast.media.LoadRequest(mediaInfo);
        request.autoplay = this.autoplay;
        request.currentTime = 0;

        this.castPlayerState = PLAYER_STATE.LOADING;
        console.log(this.castPlayerState + " (loadMedia)");

        this.session.loadMedia(request,
                this.onMediaDiscovered.bind(this, 'loadMedia'),
                this.onLoadMediaError.bind(this));
    };

    /**
     * Callback function for loadMedia success
     * @param {Object} mediaSession A new media object.
     * @param {String} how How the session was discovered.
     */
    CastPlayer.prototype.onMediaDiscovered = function (how, mediaSession) {
        console.log("new media session ID:" + mediaSession.mediaSessionId + ' (' + how + ')');
        this.currentMediaSession = mediaSession;
        if (how == 'loadMedia') {
            this.castPlayerState = this.castPlayerState = PLAYER_STATE.LOADED;
            console.log(this.castPlayerState + " (onMediaDiscovered-loadMedia)");
        }

        if (how == 'activeSession') {
            // TODO: Use currentMediaSession?
            this.castPlayerState = this.session.media[0].playerState;
            console.log(this.castPlayerState + " (onMediaDiscovered-activeSession)");
            this.currentMediaTime = Math.round(this.session.media[0].currentTime);
        }

        this.currentMediaSession.addUpdateListener(this.onMediaStatusUpdate.bind(this));

        // update UI
        this.updateMediaControlUI();
    };

    /**
     * Callback function when media load returns error
     */
    CastPlayer.prototype.onLoadMediaError = function (e) {
        console.log("media error");
        this.castPlayerState = PLAYER_STATE.IDLE;
        console.log(this.castPlayerState + " (onLoadMediaError)");
        this.updateMediaControlUI();
    };

    /**
     * Callback function for media status update from receiver
     * @param {!Boolean} alive whether the media object is still alive.
     */
    CastPlayer.prototype.onMediaStatusUpdate = function (alive) {
        if (!alive) {
            this.castPlayerState = PLAYER_STATE.IDLE;
            console.log(this.castPlayerState + " (onMediaStatusUpdate-dead)");
        } else {
            this.castPlayerState = this.currentMediaSession.playerState;
            console.log(this.castPlayerState + " (onMediaStatusUpdate)");
        }

        this.updateProgressBar();
        this.updateMediaControlUI();
    };

    /**
     * Helper function
     * Increment media current position by 1 second
     */
    CastPlayer.prototype.incrementMediaTime = function () {
        if (this.castPlayerState == PLAYER_STATE.PLAYING) {
            if (this.currentMediaOffset + this.currentMediaTime < this.currentMediaDuration) {
                this.currentMediaTime += 1;
                this.updateProgressBar();
            }
        }
    };

    /**
     * Updates the duration label.
     */
    CastPlayer.prototype.updateDurationLabel = function () {
        $("#duration").html(this.formatDuration(this.currentMediaDuration));
    };

    CastPlayer.prototype.formatDuration = function (duration) {
        var hours = Math.floor(duration / 3600);
        duration = duration % 3600;
        var minutes = Math.floor(duration / 60);
        var seconds = Math.floor(duration % 60);

        var result = "";
        if (hours > 0) {
            result += hours + ":";
            if (minutes < 10) {
                result += "0";
            }
        }
        result += minutes + ":";
        if (seconds < 10) {
            result += "0";
        }
        result += seconds;

        return result;
    };

    /**
     * Play media in Cast mode
     */
    CastPlayer.prototype.playMedia = function () {
        if (!this.currentMediaSession) {
            this.playMediaLocally(0);
            return;
        }

        switch (this.castPlayerState) {
            case PLAYER_STATE.LOADED:
            case PLAYER_STATE.PAUSED:
                this.currentMediaSession.play(null,
                        this.mediaCommandSuccessCallback.bind(this, "playing started for " + this.currentMediaSession.sessionId),
                        this.onError.bind(this));
                this.currentMediaSession.addUpdateListener(this.onMediaStatusUpdate.bind(this));
                this.castPlayerState = PLAYER_STATE.PLAYING;
                console.log(this.castPlayerState + " (playMedia)");
                break;
            case PLAYER_STATE.IDLE:
            case PLAYER_STATE.LOADING:
                this.loadMedia();
                this.currentMediaSession.addUpdateListener(this.onMediaStatusUpdate.bind(this));
                this.castPlayerState = PLAYER_STATE.PLAYING;
                console.log(this.castPlayerState + " (playMedia)");
                break;
            default:
                break;
        }
        this.updateMediaControlUI();
    };

    /**
     * Play media in local player
     * @param {Number} offset A number for media current position
     */
    CastPlayer.prototype.playMediaLocally = function (offset) {
        this.currentMediaDuration = this.localPlayer.duration;

        if (this.localPlayerState == PLAYER_STATE.PLAYING || this.localPlayerState == PLAYER_STATE.PAUSED) {
            this.localPlayer.play();
        } else {
            this.currentMediaOffset = offset;
            this.currentMediaTime = 0;

            var url = "${model.streamUrl}" + "&maxBitRate=" + this.getBitRate() + "&timeOffset=" + offset;
            console.log("playing local: " + url);

            this.localPlayer.src = url;
            this.localPlayer.play();
            this.seekInProgress = false;
        }
        this.updateMediaControlUI();
    };

    CastPlayer.prototype.getBitRate = function () {
        return $("#bitrate_menu").val();
    };

    /**
     * Pause media playback in Cast mode
     */
    CastPlayer.prototype.pauseMedia = function () {
        if (!this.currentMediaSession) {
            this.pauseMediaLocally();
            return;
        }

        if (this.castPlayerState == PLAYER_STATE.PLAYING) {
            this.castPlayerState = PLAYER_STATE.PAUSED;
            console.log(this.castPlayerState + " (pauseMedia)");
            this.currentMediaSession.pause(null,
                    this.mediaCommandSuccessCallback.bind(this, "paused " + this.currentMediaSession.sessionId),
                    this.onError.bind(this));
            this.updateMediaControlUI();
        }
    };

    /**
     * Changes the bit rate.
     */
    CastPlayer.prototype.changeBitRate = function () {
        // This effectively restarts streaming with the new bit rate.
        this.seekMedia();
    };

    /**
     * Share the video.
     */
    CastPlayer.prototype.share = function () {
        location.href = "createShare.view?id=${model.video.id}";
    };

    /**
     * Download the video.
     */
    CastPlayer.prototype.download = function () {
        location.href = "download.view?id=${model.video.id}";
    };

    /**
     * Pause media playback in local player
     */
    CastPlayer.prototype.pauseMediaLocally = function () {
        this.localPlayer.pause();
        this.updateMediaControlUI();
    };

    /**
     * Stop media playback in local player
     */
    CastPlayer.prototype.stopMediaLocally = function () {
        this.localPlayer.pause();
        this.localPlayer.currentTime = 0;
        this.updateMediaControlUI();
    };

    /**
     * Set media volume in local or Cast mode
     * @param {Boolean} mute A boolean
     */
    CastPlayer.prototype.setVolume = function (mute) {
        this.currentVolume = parseInt($("#volume_slider").slider("option", "value"));

        if (!this.currentMediaSession) {
            this.localPlayer.muted = mute;
            if (!mute) {
                this.localPlayer.volume = this.currentVolume / 100;
            }
            return;
        }

        if (mute) {
            this.session.setReceiverMuted(true,
                    this.mediaCommandSuccessCallback.bind(this),
                    this.onError.bind(this));
        } else {
            this.session.setReceiverVolumeLevel(this.currentVolume / 100.0,
                    this.mediaCommandSuccessCallback.bind(this),
                    this.onError.bind(this));
        }
        this.updateMediaControlUI();
    };

    /**
     * Toggle mute in either Cast or local mode
     */
    CastPlayer.prototype.muteMedia = function () {
        this.muted = !this.muted;
        this.setVolume(this.muted);
        $('#audio_on').toggle(!this.muted);
        $('#audio_off').toggle(this.muted);
        this.updateMediaControlUI();
    };

    /**
     * media seek function in either Cast or local mode
     */
    CastPlayer.prototype.seekMedia = function () {

        var offset = parseInt($("#progress_slider").slider("option", "value"));
        this.seekInProgress = true;
        this.currentMediaOffset = offset;
        this.currentMediaTime = 0;

        if (this.localPlayerState == PLAYER_STATE.PLAYING || this.localPlayerState == PLAYER_STATE.PAUSED) {
            this.localPlayerState = PLAYER_STATE.SEEKING;
            this.playMediaLocally(offset);
            return;
        }

        if (this.castPlayerState != PLAYER_STATE.PLAYING && this.castPlayerState != PLAYER_STATE.PAUSED) {
            return;
        }

        this.castPlayerState = PLAYER_STATE.SEEKING;
        this.loadMedia();
        this.updateMediaControlUI();
    };

    /**
     * Callback function for media command success
     */
    CastPlayer.prototype.mediaCommandSuccessCallback = function (info, e) {
        this.currentMediaTime = Math.round(this.session.media[0].currentTime);
        if (info) {
            console.log(info);
        }
    };

    /**
     * Update progress bar with the current media time.
     */
    CastPlayer.prototype.updateProgressBar = function () {
        $("#progress_slider").slider("option", "value", this.currentMediaOffset + this.currentMediaTime);
        $("#progress").html(this.formatDuration(this.currentMediaOffset + this.currentMediaTime));
    };

    CastPlayer.prototype.updateDebug = function () {
        var debug = "\n" + this.currentMediaOffset + "\n"
                + "currentMediaTime: " + this.currentMediaTime + "\n"
                + "localPlayerState: " + this.localPlayerState + "\n"
                + "castPlayerState: " + this.castPlayerState;
        $("#debug").text(debug);
    };

    /**
     * Update media control UI components based on localPlayerState or castPlayerState
     */
    CastPlayer.prototype.updateMediaControlUI = function () {

        var playerState = this.localPlayerState;

        if (this.deviceState == DEVICE_STATE.NOT_PRESENT) {
            $("#casticonactive").hide();
            $("#casticonidle").hide();
            $("#overlay_text").hide();
            var loaded = false;
            if(this.localPlayer.src) {
                loaded = true;
            }
            $("#overlay").toggle(!loaded);
        } else if (this.deviceState == DEVICE_STATE.ACTIVE) {
            $("#casticonactive").show();
            $("#casticonidle").hide();
            $("#overlay_text").show();
            $("#overlay").show();
            playerState = this.castPlayerState;
        } else {
            $("#casticonactive").hide();
            $("#casticonidle").show();
            $("#overlay_text").hide();
            var loaded = false;
            if(this.localPlayer.src) {
                loaded = true;
            }
            $("#overlay").toggle(!loaded);
        }

        switch (playerState) {
            case PLAYER_STATE.LOADED:
            case PLAYER_STATE.PLAYING:
                $("#play").hide();
                $("#pause").show();
                break;
            case PLAYER_STATE.PAUSED:
            case PLAYER_STATE.IDLE:
            case PLAYER_STATE.LOADING:
                $("#play").show();
                $("#pause").hide();
                break;
            default:
                break;
        }
    };

    /**
     * Initialize UI components and add event listeners
     */
    CastPlayer.prototype.initializeUI = function () {

        $("#progress_slider").slider({max: this.currentMediaDuration, animate: "fast", range: "min"});
        $("#volume_slider").slider({max: 100, value: 50, animate: "fast", range: "min"});

        // add event handlers to UI components
        $("#casticonidle").on('click', this.launchApp.bind(this));
        $("#casticonactive").on('click', this.stopApp.bind(this));
        $("#progress_slider").on('slidestop', this.seekMedia.bind(this));
        $("#volume_slider").on('slidestop', this.setVolume.bind(this, false));
        $("#audio_on").on('click', this.muteMedia.bind(this));
        $("#audio_off").on('click', this.muteMedia.bind(this));
        $("#play").on('click', this.playMedia.bind(this));
        $("#pause").on('click', this.pauseMedia.bind(this));
        $("#bitrate_menu").on('change', this.changeBitRate.bind(this));
        $("#share").on('click', this.share.bind(this));
        $("#download").on('click', this.download.bind(this));

        <c:if test="${not model.user.shareRole}">
        $("#share").hide();
        </c:if>
        <c:if test="${not model.user.downloadRole}">
        $("#download").hide();
        </c:if>

//        setInterval(this.updateDebug.bind(this), 100);
    };

    window.CastPlayer = CastPlayer;
})();
