<%--@elvariable id="model" type="java.util.Map"--%>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>

<html>
<head>
    <%@ include file="head.jsp" %>
    <meta name="og:type" content="album"/>
    <script type="text/javascript" src="<c:url value="/script/prototype.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/script/mediaelement/mediaelement-and-player.min.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/script/mediaelement/playlist.min.js"/>"></script>
    <link type="text/css" rel="stylesheet" href="<c:url value="/script/mediaelement/playlist.min.css"/>">
    <c:if test="${not empty model.songs}">
        <meta name="og:title"
              content="${fn:escapeXml(model.songs[0].artist)} &mdash; ${fn:escapeXml(model.songs[0].albumName)}"/>
        <meta name="og:image" content="${model.songs[0].coverArtUrl}"/>
    </c:if>
</head>

<body class="mainframe bgcolor1" style="height:100%;margin:0;">
<div class="external box">
    <div class="header">
        <h1>
            <c:choose>
                <c:when test="${empty model.share or empty model.songs}">
                    Sorry, the content is not available.
                </c:when>
                <c:otherwise>
                    ${empty model.share.description ? model.songs[0].artist : fn:escapeXml(model.share.description)}
                </c:otherwise>
            </c:choose>
        </h1>
        <div>
            <h2 style="margin:0;">${empty model.share.description ? model.songs[0].albumName : fn:escapeXml(model.share.username)}</h2>
        </div>
    </div>

    <audio id='player'>
        <c:forEach items="${model.songs}" var="song" varStatus="loopStatus">
            <source
                    src="${song.streamUrl}"
                    title="${fn:escapeXml(song.title)}"
                    type="${song.getMediaType()=='MUSIC'?'audio':'video'}/${fn:escapeXml(song.getFormat())}"
                    data-playlist-thumbnail="${song.coverArtUrl}"
                    data-playlist-description="${fn:escapeXml(song.artist)}"
            >
        </c:forEach>
    </audio>

    <div class="detail" style="text-align:center;">Streaming by <a href="https://airsonic.github.io/"
								   rel="noopener noreferrer"
                                                                   target="_blank"><b>Airsonic</b></a></div>

</div>

<script type="text/javascript">
    new MediaElementPlayer('player', {
        features: ['playpause', 'playlist', 'current', 'progress', 'duration', 'volume'],
        currentMessage: "",
        audioWidth: 600,
    });
</script>
<style>
    .external .mejs-container.mejs-audio, .mejs__container.mejs__audio {
        margin: auto;
        margin-top: 2%;
        margin-bottom: 2%;
        flex-grow: 1;
        flex-shrink: 1;
        flex-basis: auto;
    }
    .external.box {
        display: flex;
        flex-flow: column;
        height: 100%;
    }
    .external > .header {
        padding-top: 2em;
        margin: auto;
        width: 500px;
        flex-grow: 0;
        flex-shrink: 1;
        flex-basis: auto;
    }
    .external > .detail {
        flex-grow: 0;
        flex-shrink: 1;
        flex-basis: 40px;
    }
</style>
</body>
</html>
