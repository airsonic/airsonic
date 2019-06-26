<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<%--@elvariable id="command" type="org.airsonic.player.command.MusicFolderSettingsCommand"--%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>

    <script type="text/javascript">
        function init() {
            $("#newMusicFolderName").attr("placeholder", "<fmt:message key="musicfoldersettings.name"/>");
            $("#newMusicFolderPath").attr("placeholder", "<fmt:message key="musicfoldersettings.path"/>");

            <c:if test="${settings_reload}">
            parent.frames.upper.location.href="top.view?";
            parent.frames.left.location.href="left.view?";
            parent.frames.right.location.href="right.view?";
            </c:if>
        }
    </script>
</head>
<body class="mainframe bgcolor1" onload="init()">
<script type="text/javascript" src="<c:url value="/script/wz_tooltip.js"/>"></script>
<script type="text/javascript" src="<c:url value="/script/tip_balloon.js"/>"></script>


<c:import url="settingsHeader.jsp">
    <c:param name="cat" value="musicFolder"/>
    <c:param name="toast" value="${settings_toast}"/>
</c:import>

<form:form commandName="command" action="musicFolderSettings.view" method="post">

<table class="indent">
    <tr>
        <th><fmt:message key="musicfoldersettings.name"/></th>
        <th><fmt:message key="musicfoldersettings.path"/></th>
        <th style="padding-left:1em"><fmt:message key="musicfoldersettings.enabled"/></th>
        <th style="padding-left:1em"><fmt:message key="common.delete"/></th>
        <th></th>
    </tr>

    <c:forEach items="${command.musicFolders}" var="folder" varStatus="loopStatus">
        <tr>
            <td><form:input path="musicFolders[${loopStatus.count-1}].name" size="20"/></td>
            <td><form:input path="musicFolders[${loopStatus.count-1}].path" size="40"/></td>
            <td align="center" style="padding-left:1em"><form:checkbox path="musicFolders[${loopStatus.count-1}].enabled" cssClass="checkbox"/></td>
            <td align="center" style="padding-left:1em"><form:checkbox path="musicFolders[${loopStatus.count-1}].delete" cssClass="checkbox"/></td>
            <td><c:if test="${not folder.existing}"><span class="warning"><fmt:message key="musicfoldersettings.notfound"/></span></c:if></td>
        </tr>
    </c:forEach>

    <c:if test="${not empty command.musicFolders}">
        <tr>
            <th colspan="4" align="left" style="padding-top:1em"><fmt:message key="musicfoldersettings.add"/></th>
        </tr>
    </c:if>

    <tr>
        <td><form:input id="newMusicFolderName" path="newMusicFolder.name" size="20"/></td>
        <td><form:input id="newMusicFolderPath" path="newMusicFolder.path" size="40"/></td>
        <td align="center" style="padding-left:1em"><form:checkbox path="newMusicFolder.enabled" cssClass="checkbox"/></td>
        <td></td>
    </tr>

</table>

    <p class="forward"><a href="userSettings.view"><fmt:message key="musicfoldersettings.access"/></a></p>
    <p class="detail" style="width:60%;white-space:normal;margin-top:-10px;">
        <fmt:message key="musicfoldersettings.access.description"/>
    </p>

    <div>
	<fmt:message key="musicfoldersettings.excludepattern"/>
	<form:input path="excludePatternString" size="70"/>
        <c:import url="helpToolTip.jsp"><c:param name="topic" value="excludepattern"/></c:import>
    </div>

    <div>
	<form:checkbox path="ignoreSymLinks" id="ignoreSymLinks"/>
     	<form:label path="ignoreSymLinks"><fmt:message key="musicfoldersettings.ignoresymlinks"/></form:label>
    </div>

    <div style="padding-top: 0.5em;padding-bottom: 0.3em">
        <span style="white-space: nowrap">
            <fmt:message key="musicfoldersettings.scan"/>
            <form:select path="interval">
                <fmt:message key="musicfoldersettings.interval.never" var="never"/>
                <fmt:message key="musicfoldersettings.interval.one" var="one"/>
                <form:option value="-1" label="${never}"/>
                <form:option value="1" label="${one}"/>

                <c:forTokens items="2 3 7 14 30 60" delims=" " var="interval">
                    <fmt:message key="musicfoldersettings.interval.many" var="many"><fmt:param value="${interval}"/></fmt:message>
                    <form:option value="${interval}" label="${many}"/>
                </c:forTokens>
            </form:select>
            <form:select path="hour">
                <c:forEach begin="0" end="23" var="hour">
                    <fmt:message key="musicfoldersettings.hour" var="hourLabel"><fmt:param value="${hour}"/></fmt:message>
                    <form:option value="${hour}" label="${hourLabel}"/>
                </c:forEach>
            </form:select>
        </span>
    </div>

    <table>
        <tr>
            <td><div class="forward"><a href="musicFolderSettings.view?scanNow"><fmt:message key="musicfoldersettings.scannow"/></a></div></td>
            <td><c:import url="helpToolTip.jsp"><c:param name="topic" value="scanMediaFolders"/></c:import></td>
        </tr>
    </table>

    <c:if test="${command.scanning}">
        <p style="width:60%"><b><fmt:message key="musicfoldersettings.nowscanning"/></b></p>
    </c:if>

    <div>
        <form:checkbox path="fastCache" cssClass="checkbox" id="fastCache"/>
        <form:label path="fastCache"><fmt:message key="musicfoldersettings.fastcache"/></form:label>
    </div>

    <p class="detail" style="width:60%;white-space:normal;">
        <fmt:message key="musicfoldersettings.fastcache.description"/>
    </p>

    <p class="forward"><a href="musicFolderSettings.view?expunge"><fmt:message key="musicfoldersettings.expunge"/></a></p>
    <p class="detail" style="width:60%;white-space:normal;margin-top:-10px;">
        <fmt:message key="musicfoldersettings.expunge.description"/>
    </p>

    <%--<div>--%>
        <%--<form:checkbox path="organizeByFolderStructure" cssClass="checkbox" id="organizeByFolderStructure"/>--%>
        <%--<form:label path="organizeByFolderStructure"><fmt:message key="musicfoldersettings.organizebyfolderstructure"/></form:label>--%>
    <%--</div>--%>

    <%--<p class="detail" style="width:60%;white-space:normal;">--%>
        <%--<fmt:message key="musicfoldersettings.organizebyfolderstructure.description"/>--%>
    <%--</p>--%>

    <p >
        <input type="submit" value="<fmt:message key="common.save"/>" style="margin-right:0.3em">
        <a href='nowPlaying.view'><input type="button" value="<fmt:message key="common.cancel"/>"></a>
    </p>

</form:form>

</body></html>
