<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html><head>
    <%@ include file="head.jsp" %>
    <link rel="stylesheet" type="text/css" href="style/index.css">
    <link rel="alternate" type="application/rss+xml" title="Airsonic Podcast" href="podcast.view?suffix=.rss">
</head>

<body class="bgcolor2">
    <div class="entire-panel">
        <iframe id="upper" name="upper" scrolling="no" src="top.view?" onload="this.style.height=(this.contentWindow.document.body.clientHeight+15)+'px';" class="bgcolor2 main-navigation"></iframe>

        <div class="lower">
            <div class="bgcolor2 left-nav-container" ${!model.showSideBar ? "style='display: none;'" : ""}>
                <iframe id="left" name="left" src="left.view?" class="left-navigation"></iframe>
            </div>
            <div class="non-left-navigation-container">
                <div class="main-right-container">
                    <iframe id="main" name="main" src="nowPlaying.view?" class="bgcolor1 main-panel"></iframe>
                    <div class="bgcolor1 right-info-container" ${!model.showRight ? "style='display: none;'" : ""}>
                        <iframe id="right" name="right" src="right.view?" class="right-info"></iframe>
                    </div>
                </div>
                <div class="bgcolor2 playqueue-container" style="height: ${model.autoHidePlayQueue ? "50" : "150"}px;">
                    <iframe name="playQueue" src="playQueue.view?" class="bgcolor2">
                </div>
            </div>
        </div>

        <iframe id="hidden" name="hidden" class="hidden-panel"></iframe>
    </div>
</body>

</html>
