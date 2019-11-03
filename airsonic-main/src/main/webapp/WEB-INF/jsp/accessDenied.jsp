<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>

<html>
<head>
    <%@ include file="head.jsp" %>
</head>

<body class="mainframe bgcolor1">

<h1>
    <img src="<spring:theme code='errorImage'/>" alt=""/>
    <span style="vertical-align: middle"><fmt:message key="accessDenied.title"/></span>
</h1>

<p>
    <fmt:message key="accessDenied.text"/>
</p>

<div class="back"><a href="javascript:history.go(-1)"><fmt:message key="common.back"/></a></div>

</body>
</html>