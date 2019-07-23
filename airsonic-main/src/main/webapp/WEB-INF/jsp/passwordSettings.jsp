<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>
<%--@elvariable id="command" type="org.airsonic.player.command.passwordsettingscommand"--%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
</head>
<body class="mainframe bgcolor1">

<c:import url="settingsHeader.jsp">
    <c:param name="cat" value="password"/>
    <c:param name="toast" value="${settings_toast}"/>
    <c:param name="restricted" value="true"/>
</c:import>

<c:choose>

    <c:when test="${command.ldapAuthenticated}">
        <p><fmt:message key="usersettings.passwordnotsupportedforldap"/></p>
    </c:when>

    <c:otherwise>
        <fmt:message key="passwordsettings.title" var="title"><fmt:param>${command.username}</fmt:param></fmt:message>
        <h2>${fn:escapeXml(title)}</h2>
        <form:form method="post" action="passwordSettings.view" commandName="command">
            <form:hidden path="username"/>
            <table class="indent">
                <tr>
                    <td><fmt:message key="usersettings.newpassword"/></td>
                    <td><form:password path="password"/></td>
                    <td class="warning"><form:errors path="password"/></td>
                </tr>
                <tr>
                    <td><fmt:message key="usersettings.confirmpassword"/></td>
                    <td><form:password path="confirmPassword"/></td>
                    <td/>
                </tr>
                <tr>
                    <td colspan="3" style="padding-top:1.5em">
                        <input type="submit" value="<fmt:message key="common.save"/>" style="margin-right:0.3em">
                        <a href='nowPlaying.view'><input type="button" value="<fmt:message key="common.cancel"/>"></a>
                    </td>
                </tr>

            </table>
        </form:form>
    </c:otherwise>
</c:choose>

</body></html>
