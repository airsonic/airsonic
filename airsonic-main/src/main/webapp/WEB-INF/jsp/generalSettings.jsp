<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<%--@elvariable id="command" type="org.airsonic.player.command.GeneralSettingsCommand"--%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <script type="text/javascript" src="<c:url value="/script/scripts-2.0.js"/>"></script>
</head>

<body class="mainframe bgcolor1">
<script type="text/javascript" src="<c:url value="/script/wz_tooltip.js"/>"></script>
<script type="text/javascript" src="<c:url value="/script/tip_balloon.js"/>"></script>

<c:import url="settingsHeader.jsp">
    <c:param name="cat" value="general"/>
    <c:param name="toast" value="${settings_toast}"/>
</c:import>

<form:form method="post" action="generalSettings.view" commandName="command">

    <table style="white-space:nowrap" class="indent">

        <tr>
            <td><fmt:message key="generalsettings.musicmask"/></td>
            <td>
                <form:input path="musicFileTypes" size="70"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="musicmask"/></c:import>
            </td>
        </tr>

        <tr>
            <td><fmt:message key="generalsettings.videomask"/></td>
            <td>
                <form:input path="videoFileTypes" size="70"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="videomask"/></c:import>
            </td>
        </tr>

        <tr>
            <td><fmt:message key="generalsettings.coverartmask"/></td>
            <td>
                <form:input path="coverArtFileTypes" size="70"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="coverartmask"/></c:import>
            </td>
        </tr>

        <tr>
            <td><fmt:message key="generalsettings.playlistfolder"/></td>
            <td>
                <form:input path="playlistFolder" size="70"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="playlistfolder"/></c:import>
            </td>
        </tr>

        <tr><td colspan="2">&nbsp;</td></tr>

        <tr>
            <td><fmt:message key="generalsettings.index"/></td>
            <td>
                <form:input path="index" size="70"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="index"/></c:import>
            </td>
        </tr>

        <tr>
            <td><fmt:message key="generalsettings.ignoredarticles"/></td>
            <td>
                <form:input path="ignoredArticles" size="70"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="ignoredarticles"/></c:import>
            </td>
        </tr>

        <tr>
            <td><fmt:message key="generalsettings.shortcuts"/></td>
            <td>
                <form:input path="shortcuts" size="70"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="shortcuts"/></c:import>
            </td>
        </tr>

        <tr><td colspan="2">&nbsp;</td></tr>

        <tr>
            <td><fmt:message key="generalsettings.language"/></td>
            <td>
                <form:select path="localeIndex" cssStyle="width:15em">
                    <c:forEach items="${command.locales}" var="locale" varStatus="loopStatus">
                        <form:option value="${loopStatus.count - 1}" label="${locale}"/>
                    </c:forEach>
                </form:select>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="language"/></c:import>
            </td>
        </tr>

        <tr>
            <td><fmt:message key="generalsettings.theme"/></td>
            <td>
                <form:select path="themeIndex" cssStyle="width:15em">
                    <c:forEach items="${command.themes}" var="theme" varStatus="loopStatus">
                        <form:option value="${loopStatus.count - 1}" label="${theme.name}"/>
                    </c:forEach>
                </form:select>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="theme"/></c:import>
            </td>
        </tr>

        <tr><td colspan="2">&nbsp;</td></tr>

        <tr>
            <td>
            </td>
            <td>
                <form:checkbox path="sortAlbumsByYear" id="sortAlbumsByYear"/>
                <label for="sortAlbumsByYear"><fmt:message key="generalsettings.sortalbumsbyyear"/></label>
            </td>
        </tr>
        <tr>
            <td>
            </td>
            <td>
                <form:checkbox path="gettingStartedEnabled" id="gettingStartedEnabled"/>
                <label for="gettingStartedEnabled"><fmt:message key="generalsettings.showgettingstarted"/></label>
            </td>
        </tr>

        <tr><td colspan="2">&nbsp;</td></tr>

        <tr>
            <td><fmt:message key="generalsettings.welcometitle"/></td>
            <td>
                <form:input path="welcomeTitle" size="70"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="welcomemessage"/></c:import>
            </td>
        </tr>
        <tr>
            <td><fmt:message key="generalsettings.welcomesubtitle"/></td>
            <td>
                <form:input path="welcomeSubtitle" size="70"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="welcomemessage"/></c:import>
            </td>
        </tr>
        <tr>
            <td style="vertical-align:top;"><fmt:message key="generalsettings.welcomemessage"/></td>
            <td>
                <form:textarea path="welcomeMessage" rows="5" cols="70"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="welcomemessage"/></c:import>
            </td>
        </tr>
        <tr>
            <td style="vertical-align:top;"><fmt:message key="generalsettings.loginmessage"/></td>
            <td>
                <form:textarea path="loginMessage" rows="5" cols="70"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="loginmessage"/></c:import>
                <fmt:message key="main.wiki"/>
            </td>
        </tr>

        <tr>
            <td colspan="2" style="padding-top:1.5em">
                <input type="submit" value="<fmt:message key="common.save"/>" style="margin-right:0.3em">
                <a href='nowPlaying.view'><input type="button" value="<fmt:message key="common.cancel"/>"></a>
            </td>
        </tr>

    </table>
</form:form>

<c:if test="${settings_reload}">
    <script language="javascript" type="text/javascript">
        parent.frames.left.location.href="left.view?";
        parent.frames.playQueue.location.href="playQueue.view?";
    </script>
</c:if>

</body></html>
