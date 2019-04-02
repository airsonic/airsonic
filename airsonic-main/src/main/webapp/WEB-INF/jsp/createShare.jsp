<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>

<html>
<head>
    <%@ include file="head.jsp" %>
</head>
<body class="mainframe bgcolor1">

<h1>
    <img src="<spring:theme code="shareImage"/>" alt="">
    <span style="vertical-align: middle"><fmt:message key="share.title"/></span>
</h1>

<fmt:message key="share.warning"/>
<p>
    <a href="https://www.facebook.com/sharer.php?u=${model.playUrl}" target="_blank"><img src="<spring:theme code="shareFacebookImage"/>" alt=""></a>&nbsp;
    <a href="https://www.facebook.com/sharer.php?u=${model.playUrl}" target="_blank"><fmt:message key="share.facebook"/></a>
</p>

<p>
    <a href="https://twitter.com/?status=Listening to ${model.playUrl}" target="_blank"><img src="<spring:theme code="shareTwitterImage"/>" alt=""></a>&nbsp;
    <a href="https://twitter.com/?status=Listening to ${model.playUrl}" target="_blank"><fmt:message key="share.twitter"/></a>
</p>
<p>
    <fmt:message key="share.link">
        <fmt:param>${model.playUrl}</fmt:param>
    </fmt:message>
</p>

<div style="padding-top:1em">
    <c:if test="${not empty model.dir}">
        <sub:url value="main.view" var="backUrl"><sub:param name="path" value="${model.dir.path}"/></sub:url>
        <div class="back" style="float:left;padding-right:10pt"><a href="${backUrl}"><fmt:message key="common.back"/></a></div>
    </c:if>
    <c:if test="${model.user.settingsRole}">
        <div class="forward" style="float:left"><a href="shareSettings.view"><fmt:message key="share.manage"/></a></div>
    </c:if>
</div>
</body>
</html>
