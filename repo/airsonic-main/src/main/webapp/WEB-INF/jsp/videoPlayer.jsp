<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>

<html>
<head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <link rel="stylesheet" type="text/css" href="<c:url value="/style/videoPlayer.css"/>">
    <script type="text/javascript" src="<c:url value="/dwr/engine.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/interface/starService.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/script/cast_sender-v1.js"/>"></script>
    <%@ include file="videoPlayerCast.jsp" %>

    <script type="text/javascript" language="javascript">
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
    </script>
</head>

<body class="mainframe bgcolor1" style="padding-bottom:0.5em">

    <div>
        <div id="overlay">
            <div id="overlay_text">Playing on Chromecast</div>
        </div>
        <video id="videoPlayer" width="640" height="360"></video>
        <div id="media_control">
            <div id="progress_slider"></div>
            <div id="play"></div>
            <div id="pause"></div>
            <div id="progress">0:00</div>
            <div id="duration">0:00</div>
            <div id="audio_on"></div>
            <div id="audio_off"></div>
            <div id="volume_slider"></div>
            <select name="bitrate_menu" id="bitrate_menu">
                <c:forEach items="${model.bitRates}" var="bitRate">
                    <c:choose>
                        <c:when test="${bitRate eq model.defaultBitRate}">
                            <option selected="selected" value="${bitRate}">${bitRate} Kbps</option>
                        </c:when>
                        <c:otherwise>
                            <option value="${bitRate}">${bitRate} Kbps</option>
                        </c:otherwise>
                    </c:choose>
                </c:forEach>
            </select>
            <div id="share"></div>
            <div id="download"></div>
            <div id="casticonactive"></div>
            <div id="casticonidle"></div>
        </div>
    </div>
    <div id="debug"></div>

    <script type="text/javascript">
        var CastPlayer = new CastPlayer();
    </script>


<h1 style="padding-top:1em;padding-bottom:0.5em;">
    <img id="starImage" src="<spring:theme code="${not empty model.video.starredDate ? 'ratingOnImage' : 'ratingOffImage'}"/>"
         onclick="toggleStar(${model.video.id}, '#starImage'); return false;" style="cursor:pointer" alt="">
    <span style="vertical-align:middle">${fn:escapeXml(model.video.title)}</span>
</h1>

<sub:url value="main.view" var="backUrl"><sub:param name="id" value="${model.video.id}"/></sub:url>

<div class="back" style="float:left;padding-right:2em"><a href="${backUrl}"><fmt:message key="common.back"/></a></div>
<div style="clear: both"></div>

</body>
</html>
