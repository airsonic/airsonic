<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>

<html>
<head>
    <%@ include file="head.jsp" %>
</head>
<body class="mainframe bgcolor1">

<h1>
    <img src="<spring:theme code="settingsImage"/>" alt=""/>
    <span style="vertical-align: middle"><fmt:message key="avataruploadresult.title"/></span>
</h1>

<c:choose>
    <c:when test="${empty model.error}">
        <p>
            <fmt:message key="avataruploadresult.success"><fmt:param value="${fn:escapeXml(model.avatar.name)}"/></fmt:message>
            <sub:url value="avatar.view" var="avatarUrl">
                <sub:param name="username" value="${model.username}"/>
                <sub:param name="forceCustom" value="true"/>
            </sub:url>
            <img src="${avatarUrl}" alt="${model.avatar.name}" width="${model.avatar.width}"
                 height="${model.avatar.height}" style="padding-left:2em"/>
        </p>
    </c:when>
    <c:otherwise>
        <p class="warning">
            <fmt:message key="avataruploadresult.failure"/>
        </p>
    </c:otherwise>
</c:choose>

<div class="back"><a href="personalSettings.view?"><fmt:message key="common.back"/></a></div>

</body>
</html>
