<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html><head>
    <%@ include file="head.jsp" %>
</head>

<body onload="document.allmusic.submit();" class="mainframe bgcolor1">
<h2><fmt:message key="allmusic.text"><fmt:param value="${album}"/></fmt:message></h2>

<form name="allmusic" action="http://www.allmusic.com/search" method="POST" accept-charset="iso-8859-1">
    <input type="hidden" name="search_term" value="${album}"/>
    <input type="hidden" name="search_type" value="album"/>
</form>

</body>
</html>