<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
</head>
<body class="mainframe bgcolor1">

<c:import url="settingsHeader.jsp">
    <c:param name="cat" value="internetRadio"/>
    <c:param name="toast" value="${settings_toast}"/>
</c:import>

<form method="post" action="internetRadioSettings.view">
<sec:csrfInput />
<table class="indent">
    <tr>
        <th><fmt:message key="internetradiosettings.name"/></th>
        <th><fmt:message key="internetradiosettings.streamurl"/></th>
        <th><fmt:message key="internetradiosettings.homepageurl"/></th>
        <th style="padding-left:1em"><fmt:message key="internetradiosettings.enabled"/></th>
        <th style="padding-left:1em"><fmt:message key="common.delete"/></th>
    </tr>

    <c:forEach items="${model.internetRadios}" var="radio">
        <tr>
            <td><input type="text" name="name[${radio.id}]" size="20" value="${radio.name}"/></td>
            <td><input type="text" name="streamUrl[${radio.id}]" size="40" value="${radio.streamUrl}"/></td>
            <td><input type="text" name="homepageUrl[${radio.id}]" size="40" value="${radio.homepageUrl}"/></td>
            <td align="center" style="padding-left:1em"><input type="checkbox" ${radio.enabled ? "checked" : ""} name="enabled[${radio.id}]" /></td>
            <td align="center" style="padding-left:1em"><input type="checkbox" name="delete[${radio.id}]" /></td>
        </tr>
    </c:forEach>

    <c:if test="${not empty model.internetRadios}">
        <tr>
            <th colspan="5" align="left" style="padding-top:1em"><fmt:message key="internetradiosettings.add"/></th>
        </tr>
    </c:if>

    <tr>
        <td><input type="text" name="name" size="20" placeholder="<fmt:message key='internetradiosettings.name'/>"/></td>
        <td><input type="text" name="streamUrl" size="40" placeholder="<fmt:message key='internetradiosettings.streamurl'/>"/></td>
        <td><input type="text" name="homepageUrl" size="40" placeholder="<fmt:message key='internetradiosettings.homepageurl'/>"/></td>
        <td align="center" style="padding-left:1em"><input name="enabled" checked type="checkbox"/></td>
        <td/>
    </tr>

    <tr>
        <td style="padding-top:1.5em" colspan="5">
            <input type="submit" value="<fmt:message key='common.save'/>" style="margin-right:0.3em">
            <a href='nowPlaying.view'><input type="button" value="<fmt:message key='common.cancel'/>"></a>
        </td>
    </tr>
</table>
</form>


<c:if test="${not empty error}">
    <p class="warning"><fmt:message key="${error}"/></p>
</c:if>

<c:if test="${settings_reload}">
    <script language="javascript" type="text/javascript">parent.frames.left.location.href="left.view?"</script>
</c:if>

</body></html>
