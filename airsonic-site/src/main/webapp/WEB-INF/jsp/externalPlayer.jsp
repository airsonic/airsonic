<%--@elvariable id="model" type="java.util.Map"--%>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>

<html>
<head>
    <%@ include file="head.jsp" %>
    <script type="text/javascript" src="<c:url value="/script/swfobject.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/script/prototype.js"/>"></script>

    <meta name="og:type" content="album"/>

    <c:if test="${not empty model.songs}">
        <meta name="og:title" content="${fn:escapeXml(model.songs[0].artist)} &mdash; ${fn:escapeXml(model.songs[0].albumName)}"/>
        <meta name="og:image" content="${model.songs[0].coverArtUrl}"/>
    </c:if>

    <script type="text/javascript">
        function init() {
            var flashvars = {
                id:"player1",
                screencolor:"000000",
                frontcolor:"<spring:theme code="textColor"/>",
                backcolor:"<spring:theme code="backgroundColor"/>",
                "playlist.position": "bottom",
                "playlist.size": 300,
                repeat: "list"
            };
            var params = {
                allowfullscreen:"true",
                allowscriptaccess:"always"
            };
            var attributes = {
                id:"player1",
                name:"player1"
            };
            swfobject.embedSWF("<c:url value="/flash/jw-player-5.10.swf"/>", "placeholder", "500", "600", "9.0.0", false, flashvars, params, attributes);
        }

        function playerReady(thePlayer) {
            var player = $("player1");
            var list = new Array();

        <c:forEach items="${model.songs}" var="song" varStatus="loopStatus">
            <%--@elvariable id="song" type="org.airsonic.player.domain.MediaFileWithUrlInfo"--%>

            // TODO: Use video provider for aac, m4a
            list[${loopStatus.count - 1}] = {
                file: "${song.streamUrl}",
                image: "${song.coverArtUrl}",
                title: "${fn:escapeXml(song.title)}",
                provider: "${song.video ? "video" : "sound"}",
                description: "${fn:escapeXml(song.artist)}"
            };

        <c:if test="${not empty song.durationSeconds}">
            list[${loopStatus.count-1}].duration = ${song.durationSeconds};
        </c:if>

        </c:forEach>

            player.sendEvent("LOAD", list);
            player.sendEvent("PLAY");
        }

    </script>
</head>

<body class="mainframe bgcolor1" style="padding-top:2em" onload="init();">

<div style="margin:auto;width:500px">
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
    <div style="float:left;padding-right:1.5em">
        <h2 style="margin:0;">${empty model.share.description ? model.songs[0].albumName : fn:escapeXml(model.share.username)}</h2>
    </div>
    <div class="detail" style="float:right">Streaming by <a href="https://airsonic.github.io/" target="_blank"><b>Airsonic</b></a></div>

    <div style="clear:both;padding-top:1em">
        <div id="placeholder">
            <a href="http://www.adobe.com/go/getflashplayer" target="_blank"><fmt:message key="playlist.getflash"/></a>
        </div>
    </div>
</div>
</body>
</html>
