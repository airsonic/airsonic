<%@ include file="include.jsp" %>

<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<c:set var="styleSheet"><spring:theme code="styleSheet"/></c:set>
<link rel="apple-touch-icon" sizes="180x180" href="<c:url value="/icons/apple-touch-icon.png"/>"/>
<link rel="icon" type="image/png" sizes="32x32" href="<c:url value="/icons/favicon-32x32.png"/>"/>
<link rel="icon" type="image/png" sizes="16x16" href="<c:url value="/icons/favicon-16x16.png"/>"/>
<link rel="manifest" href="<c:url value="/icons/site.webmanifest"/>"/>
<link rel="mask-icon" href="<c:url value="/icons/safari-pinned-tab.svg"/>" color="#2f7bd9"/>
<meta name="msapplication-TileColor" content="#ffffff">
<meta name="theme-color" content="#ffffff">
<!-- Included before airsonic stylesheet to allow overriding -->
<link type="text/css" rel="stylesheet" href="<c:url value="/script/mediaelement/mediaelementplayer.min.css"/>">
<link rel="stylesheet" href="<c:url value="/${styleSheet}"/>" type="text/css">
<link rel="stylesheet" href="https://fonts.googleapis.com/css?family=Ubuntu&subset=latin,cyrillic-ext,greek-ext,greek,latin-ext,cyrillic" type="text/css"/>
<link rel="stylesheet" href="https://fonts.googleapis.com/css?family=Roboto:400,300,400italic,500,300italic,500italic,700,700italic,100,100italic" type="text/css"/>
<title>Airsonic</title>

<!-- Import Mousetrap and enable shortcuts if necessary -->
<script>
function isKeyboardShortcutsEnabled() {
    if (window === parent.frames.top) {
        return ${model.keyboardShortcutsEnabled ? 'true' : 'false'};
    } else {
        return parent.frames.top.isKeyboardShortcutsEnabled();
    }
}
</script>

<!-- Bind shortcuts if enabled -->
<script type="text/javascript" src="<c:url value="/script/mousetrap-1.6.0.js"/>"></script>
<script type="text/javascript">
if (isKeyboardShortcutsEnabled()) {
    Mousetrap.bind('space', function() { parent.frames.playQueue.onToggleStartStop(); return false; });
    Mousetrap.bind('left',  function() { parent.frames.playQueue.onPrevious(); });
    Mousetrap.bind('right', function() { parent.frames.playQueue.onNext(); });
    Mousetrap.bind('*',     function() { parent.frames.playQueue.onStarCurrent(); });
    Mousetrap.bind('plus',  function() { parent.frames.playQueue.onGainAdd(+5); });
    Mousetrap.bind('-',     function() { parent.frames.playQueue.onGainAdd(-5); });
    Mousetrap.bind('q',     function() { parent.frames.playQueue.onTogglePlayQueue(); });

    Mousetrap.bind('/',     function() { parent.frames.upper.$("#query").focus(); });
    Mousetrap.bind('m',     function() { parent.frames.upper.toggleLeftFrameVisible(); });

    Mousetrap.bind('g h', function() { parent.frames.main.location.href = "home.view?"; });
    Mousetrap.bind('g p', function() { parent.frames.main.location.href = "playlists.view?"; });
    Mousetrap.bind('g o', function() { parent.frames.main.location.href = "podcastChannels.view?"; });
    Mousetrap.bind('g s', function() { parent.frames.main.location.href = "settings.view?"; });
    Mousetrap.bind('g t', function() { parent.frames.main.location.href = "starred.view?"; });
    Mousetrap.bind('g r', function() { parent.frames.main.location.href = "more.view?"; });
    Mousetrap.bind('g a', function() { parent.frames.main.location.href = "help.view?"; });
    Mousetrap.bind('?',   function() { parent.frames.main.location.href = "more.view#shortcuts"; });
}
</script>
