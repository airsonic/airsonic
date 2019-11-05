<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>

<html>
<head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <script type="text/javascript" src="<c:url value='/dwr/engine.js'/>"></script>
    <script type="text/javascript" src="<c:url value='/dwr/util.js'/>"></script>
    <script type="text/javascript" src="<c:url value='/dwr/interface/nowPlayingService.js'/>"></script>
    <script type="text/javascript" src="<c:url value='/script/utils.js'/>"></script>

    <script type="text/javascript">

        function init() {
            dwr.engine.setErrorHandler(null);

            startGetScanningStatusTimer();

            <c:if test="${model.showNowPlaying}">
            startGetNowPlayingTimer();
            </c:if>

        }

        function startGetNowPlayingTimer() {
            nowPlayingService.getNowPlaying(getNowPlayingCallback);
            setTimeout("startGetNowPlayingTimer()", 10000);
        }

        function getNowPlayingCallback(nowPlaying) {
            var html = nowPlaying.length == 0 ? "" : "<h2><fmt:message key="main.nowplaying"/></h2><table style='width:100%'>";
            for (var i = 0; i < nowPlaying.length; i++) {
                html += "<tr><td colspan='2' class='detail' style='padding-top:1em;white-space:nowrap'>";

                if (nowPlaying[i].avatarUrl != null) {
                    html += "<img alt='Avatar' src='" + nowPlaying[i].avatarUrl + "' style='padding-right:5pt;width:30px;height:30px'>";
                }
                html += "<b>" + nowPlaying[i].username + "</b></td></tr>";

                html += "<tr><td class='detail' style='padding-right:1em'>" +
                        "<a title='" + nowPlaying[i].tooltip + "' target='main' href='" + nowPlaying[i].albumUrl + "'>";

                if (nowPlaying[i].artist != null) {
                    html += nowPlaying[i].artist + "<br/>";
                }

                html += "<span class='songTitle'>" + nowPlaying[i].title + "</span></a><br/>";
                if (nowPlaying[i].lyricsUrl != null) {
                    html += "<span class='forward'><a href='" + nowPlaying[i].lyricsUrl + "' onclick=\"return popupSize(this, 'lyrics', 500, 550)\">" +
                            "<fmt:message key="main.lyrics"/>" + "</a></span>";
                }
                html += "</td><td>" +
                        "<a title='" + nowPlaying[i].tooltip + "' target='main' href='" + nowPlaying[i].albumUrl + "'>" +
                        "<img alt='Cover art' src='" + nowPlaying[i].coverArtUrl + "' class='dropshadow' height='60' width='60'></a>" +
                        "</td></tr>";

                var minutesAgo = nowPlaying[i].minutesAgo;
                if (minutesAgo > 4) {
                    html += "<tr><td class='detail' colspan='2'>" + minutesAgo + " <fmt:message key="main.minutesago"/></td></tr>";
                }
            }
            html += "</table>";
            $("#nowPlaying").html(html);
        }

        function startGetScanningStatusTimer() {
            nowPlayingService.getScanningStatus(getScanningStatusCallback);
        }

        function getScanningStatusCallback(scanInfo) {
            $("#scanCount").text(scanInfo.count);
            if (scanInfo.scanning) {
                $("#scanningStatus").show();
                setTimeout("startGetScanningStatusTimer()", 1000);
            } else {
                $("#scanningStatus").hide();
                setTimeout("startGetScanningStatusTimer()", 15000);
            }
        }

    </script>
</head>
<body class="bgcolor1 rightframe" style="padding-top:2em" onload="init()">

<c:if test="${model.newVersionAvailable}">
    <div class="warning" style="padding-bottom: 1em">
        <fmt:message key="top.upgrade"><fmt:param value="${model.brand}"/><fmt:param value="${model.latestVersion}"/></fmt:message>
    </div>
</c:if>

<div id="scanningStatus" style="display: none;" class="warning">
    <img src="<spring:theme code='scanningImage'/>" title="" alt=""> <fmt:message key="main.scanning"/> <span id="scanCount"></span>
</div>

<div id="nowPlaying"></div>

</body>
</html>
