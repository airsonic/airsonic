<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>
<!DOCTYPE html>

<html><head>
    <%@ include file="head.jsp" %>
</head>
<body class="mainframe bgcolor1">

<h1 style="padding-bottom:0.5em">
    <fmt:message key="importPlaylist.title"/>
</h1>

<c:if test="${not empty model.playlist}">
    <p>
        <fmt:message key="importPlaylist.success"><fmt:param value="${model.playlist.name}"/></fmt:message>
        <script type="text/javascript" language="javascript">
            top.left.updatePlaylists();
            top.main.location.href = "playlist.view?id=${model.playlist.id}";
        </script>
    </p>
</c:if>

<c:if test="${not empty model.error}">
    <p class="warning">
        <fmt:message key="importPlaylist.error"><fmt:param value="${model.error}"/></fmt:message>
    </p>
</c:if>

<div style="padding-bottom: 0.25em">
    <fmt:message key="importPlaylist.text"/>
</div>
<form method="post" enctype="multipart/form-data" action="importPlaylist.view?${_csrf.parameterName}=${_csrf.token}">
    <input type="file" id="file" name="file" size="40"/>
    <input type="submit" value="<fmt:message key="common.ok"/>"/>
</form>


</body></html>
