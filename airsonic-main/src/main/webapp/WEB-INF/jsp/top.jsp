<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <script type="text/javascript" src="<c:url value='/dwr/engine.js'/>"></script>
    <script type="text/javascript" src="<c:url value='/dwr/interface/multiService.js'/>"></script>

    <script type="text/javascript">
        var previousQuery = "";
        var instantSearchTimeout;
        var showSideBar = ${model.showSideBar ? 'true' : 'false'};

        function triggerInstantSearch() {
            if (instantSearchTimeout) {
                window.clearTimeout(instantSearchTimeout);
            }
            instantSearchTimeout = window.setTimeout(executeInstantSearch, 300);
        }

        function executeInstantSearch() {
            var query = $("#query").val().trim();
            if (query.length > 1 && query != previousQuery) {
                previousQuery = query;
                document.searchForm.submit();
            }
        }

        function showLeftFrame() {
            $("#show-left-frame").hide();
            $("#hide-left-frame").show();
            toggleLeftFrame(230);
            multiService.setShowSideBar(true);
            showSideBar = true;
        }

        function hideLeftFrame() {
            $("#hide-left-frame").hide();
            $("#show-left-frame").show();
            toggleLeftFrame(0);
            multiService.setShowSideBar(false);
            showSideBar = false;
        }

        function toggleLeftFrameVisible() {
            if (showSideBar) hideLeftFrame();
            else showLeftFrame();
        }

        function toggleLeftFrame(width) {
            <%-- Disable animation in Chrome. It stopped working in Chrome 44. --%>
            var duration = navigator.userAgent.indexOf("Chrome") != -1 ? 0 : 400;

            $("#dummy-animation-target").stop();
            $("#dummy-animation-target").animate({"max-width": width}, {
                step: function (now, fx) {
                    top.document.getElementById("mainFrameset").cols = now + ",*";
                },
                duration: duration
            });
        }
    </script>
</head>

<body class="bgcolor2 topframe" style="margin:0.4em 1em 0 1em;">

<span id="dummy-animation-target" style="max-width:0;display: none"></span>

<fmt:message key="top.home" var="home"/>
<fmt:message key="top.now_playing" var="nowPlaying"/>
<fmt:message key="top.starred" var="starred"/>
<fmt:message key="left.playlists" var="playlists"/>
<fmt:message key="top.settings" var="settings"/>
<fmt:message key="top.status" var="status" />
<fmt:message key="top.podcast" var="podcast"/>
<fmt:message key="top.more" var="more"/>
<fmt:message key="top.help" var="help"/>
<fmt:message key="top.search" var="search"/>

<table style="margin:0;padding-top:5px">
    <tr>
        <td style="padding-right:4.5em;">
            <img id="show-left-frame" src="<spring:theme code='sidebarImage'/>" onclick="showLeftFrame()" alt="" style="display:${model.showSideBar ? 'none' : 'inline'};cursor:pointer">
            <img id="hide-left-frame" src="<spring:theme code='sidebarImage'/>" onclick="hideLeftFrame()" alt="" style="display:${model.showSideBar ? 'inline' : 'none'};cursor:pointer">
        </td>
        <td style="min-width:3em;padding-right:1em;text-align: center">
            <a href="home.view?" target="main"><img src="<spring:theme code='homeImage'/>" title="${home}" alt="${home}"></a>
            <div class="topHeader"><a href="home.view?" target="main">${home}</a></div>
        </td>
        <td style="min-width:3em;padding-right:1em;text-align: center">
            <a href="nowPlaying.view?" target="main"><img src="<spring:theme code='nowPlayingImage'/>" title="${nowPlaying}" alt="${nowPlaying}"></a>
            <div class="topHeader"><a href="nowPlaying.view?" target="main">${nowPlaying}</a></div>
        </td>
        <td style="min-width:3em;padding-right:1em;text-align: center">
            <a href="starred.view?" target="main"><img src="<spring:theme code='starredImage'/>" title="${starred}" alt="${starred}"></a>
            <div class="topHeader"><a href="starred.view?" target="main">${starred}</a></div>
        </td>
        <td style="min-width:3em;padding-right:1em;text-align: center">
            <a href="playlists.view?" target="main"><img src="<spring:theme code='playlistImage'/>" title="${playlists}" alt="${playlists}"></a>
            <div class="topHeader"><a href="playlists.view?" target="main">${playlists}</a></div>
        </td>
        <td style="min-width:4em;padding-right:1em;text-align: center">
            <a href="podcastChannels.view?" target="main"><img src="<spring:theme code='podcastLargeImage'/>" title="${podcast}" alt="${podcast}"></a>
            <div class="topHeader"><a href="podcastChannels.view?" target="main">${podcast}</a></div>
        </td>
        <c:if test="${model.user.settingsRole}">
            <td style="min-width:3em;padding-right:1em;text-align: center">
                <a href="settings.view?" target="main"><img src="<spring:theme code='settingsImage'/>" title="${settings}" alt="${settings}"></a>
                <div class="topHeader"><a href="settings.view?" target="main">${settings}</a></div>
            </td>
        </c:if>
        <td style="min-width:3em;padding-right:1em;text-align: center">
            <a href="status.view?" target="main"><img src="<spring:theme code='statusImage'/>" title="${status}" alt="${status}"></a>
            <div class="topHeader"><a href="status.view?" target="main">${status}</a></div>
        </td>
        <td style="min-width:3em;padding-right:1em;text-align: center">
            <a href="more.view?" target="main"><img src="<spring:theme code='moreImage'/>" title="${more}" alt="${more}"></a>
            <div class="topHeader"><a href="more.view?" target="main">${more}</a></div>
        </td>
        <td style="min-width:3em;padding-right:1em;text-align: center">
            <a href="help.view?" target="main"><img src="<spring:theme code='helpImage'/>" title="${help}" alt="${help}"></a>
            <div class="topHeader"><a href="help.view?" target="main">${help}</a></div>
        </td>

        <td style="padding-left:1em">
            <form method="post" action="search.view" target="main" name="searchForm">
                <td><input required type="text" name="query" id="query" size="28" placeholder="${search}" onclick="select();"
                           onkeyup="triggerInstantSearch();"></td>
                <td><a href="javascript:document.searchForm.submit()"><img src="<spring:theme code='searchImage'/>" alt="${search}" title="${search}"></a></td>
            </form>
        </td>

        <td style="padding-left:15pt;padding-right:5pt;vertical-align: middle;width: 100%;text-align: center">
            <c:if test="${model.user.settingsRole}"><a href="personalSettings.view" target="main"></c:if>
            <c:choose>
                <c:when test="${model.showAvatar}">
                    <sub:url value="avatar.view" var="avatarUrl">
                        <sub:param name="username" value="${model.user.username}"/>
                    </sub:url>
                    <div style="padding-bottom: 4px">
                        <img src="${avatarUrl}" alt="User" width="30" height="30">
                    </div>
                </c:when>
                <c:otherwise>
                    <img src="<spring:theme code='userImage'/>" alt="User" height="24">
                </c:otherwise>
            </c:choose>

            <div class="detail">
                <c:out value="${model.user.username}" escapeXml="true"/>
            </div>
            <c:if test="${model.user.settingsRole}"></a></c:if>
        </td>

        <td style="padding-left:15pt;padding-right:5pt;vertical-align: right;width: 100%;text-align: center">
            <a href="<c:url value='/logout'/>" target="_top">
                <img src="<spring:theme code='logoutImage'/>" alt="logout" height="24">
                <div class="detail">
                    <fmt:message key="top.logout" var="logout"></fmt:message>
                    <c:out value="${logout}"/>
                </div>
            </a>
        </td>

    </tr></table>

</body></html>
