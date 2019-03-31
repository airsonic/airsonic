<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<%--@elvariable id="command" type="org.airsonic.player.command.PersonalSettingsCommand"--%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <script type="text/javascript" src="<c:url value="/script/scripts-2.0.js"/>"></script>

    <script type="text/javascript" language="javascript">
        function enableLastFmFields() {
            $("#lastFm").is(":checked") ? $("#lastFmTable").show() : $("#lastFmTable").hide();
        }
    </script>
</head>

<body class="mainframe bgcolor1" onload="enableLastFmFields()">
<script type="text/javascript" src="<c:url value="/script/wz_tooltip.js"/>"></script>
<script type="text/javascript" src="<c:url value="/script/tip_balloon.js"/>"></script>

<c:import url="settingsHeader.jsp">
    <c:param name="cat" value="personal"/>
    <c:param name="restricted" value="${not command.user.adminRole}"/>
    <c:param name="toast" value="${settings_toast}"/>
</c:import>

<fmt:message key="personalsettings.title" var="title"><fmt:param>${command.user.username}</fmt:param></fmt:message>
<h2>${fn:escapeXml(title)}</h2>

<fmt:message key="common.default" var="defaultTitle"/>
<form:form method="post" action="personalSettings.view" commandName="command">

    <table style="white-space:nowrap" class="indent">

        <tr>
            <td><fmt:message key="personalsettings.language"/></td>
            <td>
                <form:select path="localeIndex" cssStyle="width:15em">
                    <form:option value="-1" label="${defaultTitle}"/>
                    <c:forEach items="${command.locales}" var="locale" varStatus="loopStatus">
                        <form:option value="${loopStatus.count - 1}" label="${locale}"/>
                    </c:forEach>
                </form:select>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="language"/></c:import>
            </td>
        </tr>

        <tr>
            <td><fmt:message key="personalsettings.theme"/></td>
            <td>
                <form:select path="themeIndex" cssStyle="width:15em">
                    <form:option value="-1" label="${defaultTitle}"/>
                    <c:forEach items="${command.themes}" var="theme" varStatus="loopStatus">
                        <form:option value="${loopStatus.count - 1}" label="${theme.name}"/>
                    </c:forEach>
                </form:select>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="theme"/></c:import>
            </td>
        </tr>

        <tr>
            <td><fmt:message key="personalsettings.albumlist"/></td>
            <td>
                <form:select path="albumListId" cssStyle="width:15em">
                    <c:forEach items="${command.albumLists}" var="albumList" varStatus="loopStatus">
                        <c:set var="label">
                            <fmt:message key="home.${albumList.id}.title"/>
                        </c:set>
                        <form:option value="${albumList.id}" label="${label}"/>
                    </c:forEach>
                </form:select>
            </td>
        </tr>
    </table>

    <table class="indent">
        <tr>
            <th style="padding:0 0.5em 0.5em 0;text-align:left;"><fmt:message key="personalsettings.display"/></th>
            <th style="padding:0 0.5em 0.5em 0.5em;text-align:center;"><fmt:message key="personalsettings.browse"/></th>
            <th style="padding:0 0 0.5em 0.5em;text-align:center;"><fmt:message key="personalsettings.playlist"/></th>
            <th style="padding:0 0 0.5em 0.5em">
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="visibility"/></c:import>
            </th>
        </tr>
        <tr>
            <td><fmt:message key="personalsettings.tracknumber"/></td>
            <td style="text-align:center"><form:checkbox path="mainVisibility.trackNumberVisible" cssClass="checkbox"/></td>
            <td style="text-align:center"><form:checkbox path="playlistVisibility.trackNumberVisible" cssClass="checkbox"/></td>
        </tr>
        <tr>
            <td><fmt:message key="personalsettings.artist"/></td>
            <td style="text-align:center"><form:checkbox path="mainVisibility.artistVisible" cssClass="checkbox"/></td>
            <td style="text-align:center"><form:checkbox path="playlistVisibility.artistVisible" cssClass="checkbox"/></td>
        </tr>
        <tr>
            <td><fmt:message key="personalsettings.album"/></td>
            <td style="text-align:center"><form:checkbox path="mainVisibility.albumVisible" cssClass="checkbox"/></td>
            <td style="text-align:center"><form:checkbox path="playlistVisibility.albumVisible" cssClass="checkbox"/></td>
        </tr>
        <tr>
            <td><fmt:message key="personalsettings.genre"/></td>
            <td style="text-align:center"><form:checkbox path="mainVisibility.genreVisible" cssClass="checkbox"/></td>
            <td style="text-align:center"><form:checkbox path="playlistVisibility.genreVisible" cssClass="checkbox"/></td>
        </tr>
        <tr>
            <td><fmt:message key="personalsettings.year"/></td>
            <td style="text-align:center"><form:checkbox path="mainVisibility.yearVisible" cssClass="checkbox"/></td>
            <td style="text-align:center"><form:checkbox path="playlistVisibility.yearVisible" cssClass="checkbox"/></td>
        </tr>
        <tr>
            <td><fmt:message key="personalsettings.bitrate"/></td>
            <td style="text-align:center"><form:checkbox path="mainVisibility.bitRateVisible" cssClass="checkbox"/></td>
            <td style="text-align:center"><form:checkbox path="playlistVisibility.bitRateVisible" cssClass="checkbox"/></td>
        </tr>
        <tr>
            <td><fmt:message key="personalsettings.duration"/></td>
            <td style="text-align:center"><form:checkbox path="mainVisibility.durationVisible" cssClass="checkbox"/></td>
            <td style="text-align:center"><form:checkbox path="playlistVisibility.durationVisible" cssClass="checkbox"/></td>
        </tr>
        <tr>
            <td><fmt:message key="personalsettings.format"/></td>
            <td style="text-align:center"><form:checkbox path="mainVisibility.formatVisible" cssClass="checkbox"/></td>
            <td style="text-align:center"><form:checkbox path="playlistVisibility.formatVisible" cssClass="checkbox"/></td>
        </tr>
        <tr>
            <td><fmt:message key="personalsettings.filesize"/></td>
            <td style="text-align:center"><form:checkbox path="mainVisibility.fileSizeVisible" cssClass="checkbox"/></td>
            <td style="text-align:center"><form:checkbox path="playlistVisibility.fileSizeVisible" cssClass="checkbox"/></td>
        </tr>
    </table>

    <table class="indent">
        <tr>
            <td><form:checkbox path="showNowPlayingEnabled" id="nowPlaying" cssClass="checkbox"/></td>
            <td><label for="nowPlaying"><fmt:message key="personalsettings.shownowplaying"/></label></td>
            <td style="padding-left:2em"><form:checkbox path="showArtistInfoEnabled" id="artistInfo" cssClass="checkbox"/></td>
            <td><label for="artistInfo"><fmt:message key="personalsettings.showartistinfo"/></label></td>
        </tr>
        <tr>
            <td><form:checkbox path="nowPlayingAllowed" id="nowPlayingAllowed" cssClass="checkbox"/></td>
            <td><label for="nowPlayingAllowed"><fmt:message key="personalsettings.nowplayingallowed"/></label></td>
            <td style="padding-left:2em"><form:checkbox path="autoHidePlayQueue" id="autoHidePlayQueue" cssClass="checkbox"/></td>
            <td><label for="autoHidePlayQueue"><fmt:message key="personalsettings.autohideplayqueue"/></label></td>
            <td style="padding-left:2em"><form:checkbox path="partyModeEnabled" id="partyModeEnabled" cssClass="checkbox"/></td>
            <td><label for="partyModeEnabled"><fmt:message key="personalsettings.partymode"/></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="partymode"/></c:import>
            </td>
        </tr>
        <tr>
            <td><form:checkbox path="queueFollowingSongs" id="queueFollowingSongs" cssClass="checkbox"/></td>
            <td><label for="queueFollowingSongs"><fmt:message key="personalsettings.queuefollowingsongs"/></label></td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
        </tr>
    </table>

    <table class="indent">
        <tr>
            <td><form:checkbox path="finalVersionNotificationEnabled" id="final" cssClass="checkbox"/></td>
            <td><label for="final"><fmt:message key="personalsettings.finalversionnotification"/></label></td>
        </tr>
        <tr>
            <td><form:checkbox path="betaVersionNotificationEnabled" id="beta" cssClass="checkbox"/></td>
            <td><label for="beta"><fmt:message key="personalsettings.betaversionnotification"/></label></td>
        </tr>
        <tr>
            <td><form:checkbox path="songNotificationEnabled" id="song" cssClass="checkbox"/></td>
            <td><label for="song"><fmt:message key="personalsettings.songnotification"/></label></td>
        </tr>
    </table>

    <table class="indent">
        <tr>
            <td><form:checkbox path="lastFmEnabled" id="lastFm" cssClass="checkbox" onclick="enableLastFmFields()"/></td>
            <td><label for="lastFm"><fmt:message key="personalsettings.lastfmenabled"/></label></td>
        </tr>
    </table>

    <table class="indent">
        <tr>
            <td><form:checkbox path="keyboardShortcutsEnabled" id="keyboardShortcutsEnabled" cssClass="checkbox"/></td>
            <td><label for="keyboardShortcutsEnabled"><fmt:message key="personalsettings.keyboardshortcutsenabled"/></label></td>
        </tr>
    </table>

    <table class="indent">
        <tr>
            <td><fmt:message key="personalsettings.listreloaddelay"/></td>
            <td><form:input path="listReloadDelay" size="24"/></td>
        </tr>
        <tr>
            <td><fmt:message key="personalsettings.paginationsize"/></td>
            <td><form:input path="paginationSize" size="24"/></td>
        </tr>
    </table>

    <table id="lastFmTable" style="padding-left:2em">
        <tr>
            <td><fmt:message key="personalsettings.lastfmusername"/></td>
            <td><form:input path="lastFmUsername" size="24"/></td>
        </tr>
        <tr>
            <td><fmt:message key="personalsettings.lastfmpassword"/></td>
            <td><form:password path="lastFmPassword" size="24"/></td>
        </tr>
    </table>

    <p style="padding-top:1em;padding-bottom:1em">
        <input type="submit" value="<fmt:message key="common.save"/>" style="margin-right:0.3em"/>
        <a href='nowPlaying.view'><input type="button" value="<fmt:message key="common.cancel"/>"></a>
    </p>

    <h2><fmt:message key="personalsettings.avatar.title"/></h2>

    <p style="padding-top:1em">
        <c:forEach items="${command.avatars}" var="avatar">
            <c:url value="avatar.view" var="avatarUrl">
                <c:param name="id" value="${avatar.id}"/>
            </c:url>
            <span style="white-space:nowrap;">
                <form:radiobutton id="avatar-${avatar.id}" path="avatarId" value="${avatar.id}"/>
                <label for="avatar-${avatar.id}"><img src="${avatarUrl}" alt="${avatar.name}" width="${avatar.width}" height="${avatar.height}" style="padding-right:2em;padding-bottom:1em"/></label>
            </span>
        </c:forEach>
    </p>
    <p>
        <form:radiobutton id="noAvatar" path="avatarId" value="-1"/>
        <label for="noAvatar"><fmt:message key="personalsettings.avatar.none"/></label>
    </p>
    <p>
        <form:radiobutton id="customAvatar" path="avatarId" value="-2"/>
        <label for="customAvatar"><fmt:message key="personalsettings.avatar.custom"/>
            <c:if test="${not empty command.customAvatar}">
                <sub:url value="avatar.view" var="avatarUrl">
                    <sub:param name="username" value="${command.user.username}"/>
                    <sub:param name="forceCustom" value="true"/>
                </sub:url>
                <img src="${avatarUrl}" alt="${command.customAvatar.name}" width="${command.customAvatar.width}" height="${command.customAvatar.height}" style="padding-right:2em"/>
            </c:if>
        </label>
    </p>
</form:form>

<form method="post" enctype="multipart/form-data" action="avatarUpload.view?${_csrf.parameterName}=${_csrf.token}">
    <table>
        <tr>
            <td style="padding-right:1em"><fmt:message key="personalsettings.avatar.changecustom"/></td>
            <td style="padding-right:1em"><input type="file" id="file" name="file" size="40"/></td>
            <td style="padding-right:1em"><input type="submit" value="<fmt:message key="personalsettings.avatar.upload"/>"/></td>
        </tr>
    </table>
</form>

<p class="detail" style="text-align:right">
    <fmt:message key="personalsettings.avatar.courtesy"/>
</p>

<c:if test="${settings_reload}">
    <script language="javascript" type="text/javascript">
        parent.location.href="index.view?";
    </script>
</c:if>

</body></html>
