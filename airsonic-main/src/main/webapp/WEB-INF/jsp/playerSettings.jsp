<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<%--@elvariable id="command" type="org.airsonic.player.command.PlayerSettingsCommand"--%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <script type="text/javascript" src="<c:url value="/script/utils.js"/>"></script>
</head>
<body class="mainframe bgcolor1">
<script type="text/javascript" src="<c:url value="/script/wz_tooltip.js"/>"></script>
<script type="text/javascript" src="<c:url value="/script/tip_balloon.js"/>"></script>

<script lang="javascript">

function hideAllTechnologyDepends() {
  $('.technologyDepends').hide();
}

function showTechnologyDepends(technologyName) {
  var selector = '.technologyDepends.' + technologyName;
  $(selector).show();
}

$(document).ready(function() {

  $('.technologyRadio').click(function() {
    hideAllTechnologyDepends();
    var technologyName = $(this).val();
    showTechnologyDepends(technologyName);
  });

  hideAllTechnologyDepends();
  $('.technologyRadio:checked').each(function() {
    var technologyName = $(this).val();
    showTechnologyDepends(technologyName);
  });
});

</script>

<c:import url="settingsHeader.jsp">
    <c:param name="cat" value="player"/>
    <c:param name="toast" value="${settings_toast}"/>
    <c:param name="restricted" value="${not command.admin}"/>
</c:import>

<fmt:message key="common.unknown" var="unknown"/>

<c:choose>
    <c:when test="${empty command.players}">
        <p><fmt:message key="playersettings.noplayers"/></p>
    </c:when>
    <c:otherwise>

        <c:url value="playerSettings.view" var="deleteUrl">
            <c:param name="delete" value="${command.playerId}"/>
        </c:url>
        <c:url value="playerSettings.view" var="cloneUrl">
            <c:param name="clone" value="${command.playerId}"/>
        </c:url>

        <table class="indent">
            <tr>
                <td><b><fmt:message key="playersettings.title"/></b></td>
                <td>
                    <select name="player" onchange="location='playerSettings.view?id=' + options[selectedIndex].value;">
                        <c:forEach items="${command.players}" var="player">
                            <option ${player.id eq command.playerId ? "selected" : ""}
                                    value="${player.id}">${fn:escapeXml(player.description)}</option>
                        </c:forEach>
                    </select>
                </td>
            </tr>
            <tr>
                <td style="padding-right:1em"><div class="forward"><a href="${deleteUrl}"><fmt:message key="playersettings.forget"/></a></div></td>
                <td><div class="forward"><a href="${cloneUrl}"><fmt:message key="playersettings.clone"/></a></div></td>
            </tr>
        </table>

        <form:form modelAttribute="command" method="post" action="playerSettings.view">
            <form:hidden path="playerId"/>

            <table class="ruleTable indent">
                <c:forEach items="${command.technologyHolders}" var="technologyHolder">
                    <c:set var="technologyName">
                        <fmt:message key="playersettings.technology.${fn:toLowerCase(technologyHolder.name)}.title"/>
                    </c:set>

                    <tr>
                        <td class="ruleTableHeader">
                            <form:radiobutton class="technologyRadio" id="radio-${technologyName}" path="technologyName" value="${technologyHolder.name}"/>
                            <b><label for="radio-${technologyName}">${technologyName}</label></b>
                        </td>
                        <td class="ruleTableCell" style="width:40em">
                            <fmt:message key="playersettings.technology.${fn:toLowerCase(technologyHolder.name)}.text"/>
                        </td>
                    </tr>
                </c:forEach>
            </table>

            <table class="indent" style="border-spacing:3pt;">
                <tr>
                    <td style="padding-right: 1.5em"><fmt:message key="playersettings.type"/></td>
                    <td>
                        <c:choose>
                            <c:when test="${empty command.type}">${unknown}</c:when>
                            <c:otherwise>${command.type}</c:otherwise>
                        </c:choose>
                    </td>
                </tr>
                <tr>
                    <td style="padding-right: 1.5em"><fmt:message key="playersettings.lastseen"/></td>
                    <td><fmt:formatDate value="${command.lastSeen}" type="both" dateStyle="long" timeStyle="medium"/></td>
                </tr>
            </table>

            <table class="indent" style="border-spacing:3pt;">
                <tr>
                    <td><fmt:message key="playersettings.name"/></td>
                    <td><form:input path="name" size="16"/></td>
                    <td colspan="2"><c:import url="helpToolTip.jsp"><c:param name="topic" value="playername"/></c:import></td>
                </tr>

                <tr class="technologyDepends JAVA_JUKEBOX">
                    <td><fmt:message key="playersettings.javaJukeboxMixer"/></td>
                    <td>
                        <form:select path="javaJukeboxMixer">
                            <c:forEach items="${command.javaJukeboxMixers}" var="javaJukeboxMixer">
                                <form:option value="${javaJukeboxMixer}" label="${javaJukeboxMixer}"/>
                            </c:forEach>
                        </form:select>
                    </td>
                    <td colspan="2"></td>
                </tr>


                <%// Max bitrate %>
                <tr class="technologyDepends WEB EXTERNAL EXTERNAL_WITH_PLAYLIST JUKEBOX">
                    <td><fmt:message key="playersettings.maxbitrate"/></td>
                    <td>
                        <form:select path="transcodeSchemeName" cssStyle="width:8em">
                            <c:forEach items="${command.transcodeSchemeHolders}" var="transcodeSchemeHolder">
                                <form:option value="${transcodeSchemeHolder.name}" label="${transcodeSchemeHolder.description}"/>
                            </c:forEach>
                        </form:select>
                    </td>
                    <td>
                        <c:import url="helpToolTip.jsp"><c:param name="topic" value="transcode"/></c:import>
                    </td>
                    <td class="warning">
                        <c:if test="${not command.transcodingSupported}">
                            <fmt:message key="playersettings.notranscoder"/>
                        </c:if>
                    </td>
                </tr>
            </table>

            <table class="indent technologyDepends WEB EXTERNAL EXTERNAL_WITH_PLAYLIST JUKEBOX" style="border-spacing:3pt">
                <tr>
                    <td>
                        <form:checkbox path="dynamicIp" id="dynamicIp" cssClass="checkbox"/>
                        <label for="dynamicIp"><fmt:message key="playersettings.dynamicip"/></label>
                    </td>
                    <td><c:import url="helpToolTip.jsp"><c:param name="topic" value="dynamicip"/></c:import></td>
                </tr>

                <tr>
                    <td>
                        <form:checkbox path="autoControlEnabled" id="autoControlEnabled" cssClass="checkbox"/>
                        <label for="autoControlEnabled"><fmt:message key="playersettings.autocontrol"/></label>
                    </td>
                    <td><c:import url="helpToolTip.jsp"><c:param name="topic" value="autocontrol"/></c:import></td>
                </tr>

                <tr>
                    <td>
                        <form:checkbox path="m3uBomEnabled" id="m3uBomEnabled" cssClass="checkbox"/>
                        <label for="m3uBomEnabled"><fmt:message key="playersettings.m3ubom"/></label>
                    </td>
                    <td><c:import url="helpToolTip.jsp"><c:param name="topic" value="m3ubom"/></c:import></td>
                </tr>
            </table>

            <c:if test="${not empty command.allTranscodings}">
                <table class="indent technologyDepends WEB EXTERNAL EXTERNAL_WITH_PLAYLIST JUKEBOX">
                    <tr><td><b><fmt:message key="playersettings.transcodings"/></b></td></tr>
                    <c:forEach items="${command.allTranscodings}" var="transcoding" varStatus="loopStatus">
                        <c:if test="${loopStatus.count % 3 == 1}"><tr></c:if>
                        <td style="padding-right:2em">
                            <form:checkbox path="activeTranscodingIds" id="transcoding${transcoding.id}" value="${transcoding.id}" cssClass="checkbox"/>
                            <label for="transcoding${transcoding.id}">${transcoding.name}</label>
                        </td>
                        <c:if test="${loopStatus.count % 3 == 0 or loopStatus.count eq fn:length(command.allTranscodings)}"></tr></c:if>
                    </c:forEach>
                </table>
            </c:if>

            <input type="submit" value="<fmt:message key="common.save"/>" style="margin-top:1em;margin-right:0.3em">
            <a href='nowPlaying.view'><input type="button" value="<fmt:message key="common.cancel"/>" style="margin-top:1em"></a>
        </form:form>
    </c:otherwise>
</c:choose>

<c:if test="${settings_reload}">
    <script language="javascript" type="text/javascript">parent.frames.playQueue.location.href="playQueue.view?"</script>
</c:if>

</body></html>
