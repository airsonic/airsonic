<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html><head>
    <%@ include file="head.jsp" %>
    <script type="text/javascript" src="<c:url value="/script/scripts-2.0.js"/>"></script>
</head>
<body class="mainframe bgcolor1">

<c:choose>
    <c:when test="${empty model.buildDate}">
        <fmt:message key="common.unknown" var="buildDateString"/>
    </c:when>
    <c:otherwise>
        <fmt:formatDate value="${model.buildDate}" dateStyle="long" var="buildDateString"/>
    </c:otherwise>
</c:choose>

<c:choose>
    <c:when test="${empty model.localVersion}">
        <fmt:message key="common.unknown" var="versionString"/>
    </c:when>
    <c:otherwise>
        <c:set var="versionString" value="${model.localVersion}"/>
    </c:otherwise>
</c:choose>

<h1>
    <img src="<spring:theme code="helpImage"/>" alt="">
    <span style="vertical-align: middle"><fmt:message key="help.title"><fmt:param value="${model.brand}"/></fmt:message></span>
</h1>

<c:if test="${model.newVersionAvailable}">
    <p class="warning"><fmt:message key="help.upgrade"><fmt:param value="${model.brand}"/><fmt:param value="${model.latestVersion}"/></fmt:message></p>
</c:if>

<table width="75%" class="ruleTable indent">

    <tr><td class="ruleTableHeader"><fmt:message key="help.version.title"/></td><td class="ruleTableCell">${versionString} &ndash; ${buildDateString}</td></tr>
    <tr><td class="ruleTableHeader"><fmt:message key="help.server.title"/></td><td class="ruleTableCell">${model.serverInfo} (<sub:formatBytes bytes="${model.usedMemory}"/> / <sub:formatBytes bytes="${model.totalMemory}"/>)</td></tr>
    <tr><td class="ruleTableHeader"><fmt:message key="help.license.title"/></td><td class="ruleTableCell">
        <a href="http://www.gnu.org/copyleft/gpl.html" target="_blank"><img style="float:right;margin-left: 10px" alt="GPL 3.0" src="<c:url value="/icons/default_light/gpl.png"/>"></a>
        <fmt:message key="help.license.text"><fmt:param value="${model.brand}"/></fmt:message></td></tr>
    <tr><td class="ruleTableHeader"><fmt:message key="help.homepage.title"/></td><td class="ruleTableCell"><a target="_blank" href="https://airsonic.github.io/">Airsonic website</a></td></tr>
    <tr><td class="ruleTableHeader"><fmt:message key="help.forum.title"/></td><td class="ruleTableCell"><a target="_blank" href="https://www.reddit.com/r/airsonic">Airsonic on Reddit</a></td></tr>
    <tr><td class="ruleTableHeader"><fmt:message key="help.contact.title"/></td><td class="ruleTableCell"><fmt:message key="help.contact.text"><fmt:param value="${model.brand}"/></fmt:message></td></tr>
</table>

<p></p>

<h2>
    <img src="<spring:theme code="logImage"/>" alt="">
    <span style="vertical-align: middle"><fmt:message key="help.log"/></span>
</h2>

<table cellpadding="2" class="log indent">
    <c:forEach items="${model.logEntries}" var="entry">
        <tr>
            <td>${fn:escapeXml(entry)}</td>
        </tr>
    </c:forEach>
</table>

<p><fmt:message key="help.logfile"><fmt:param value="${model.logFile}"/></fmt:message> </p>

<div class="forward"><a href="help.view?"><fmt:message key="common.refresh"/></a></div>

</body></html>
