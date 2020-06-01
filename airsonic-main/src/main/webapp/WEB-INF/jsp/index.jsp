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
<!--
<frameset rows="80,*,0" border="0" framespacing="0" frameborder="0">
    <frame name="upper" src="top.view?" class="bgcolor2">
    <frameset id="mainFrameset" cols=${model.showSideBar ? "230,*" : "0,*"} border="0" framespacing="0" frameborder="0">
        <frame name="left" src="left.view?" marginwidth="0" marginheight="0" class="bgcolor2">

        <frameset id="playQueueFrameset" rows=${model.autoHidePlayQueue ? "*,50" : "*,150"} border="0" framespacing="0" frameborder="0">
            <frameset cols="*,${model.showRight ? 235 : 0}" border="0" framespacing="0" frameborder="0">
                <frame name="main" src="nowPlaying.view?" marginwidth="0" marginheight="0" class="bgcolor1">
                <frame name="right" src="right.view?" class="bgcolor1">
            </frameset>
            <frame name="playQueue" src="playQueue.view?" class="bgcolor2">
        </frameset>
    </frameset>
    <frame name="hidden" frameborder="0" noresize="noresize">

</frameset>-->

</html>
