<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>

<html><head>
    <%@ include file="head.jsp" %>
</head><body>

<c:forEach items="${model.reloadFrames}" var="reloadFrame">
    <script language="javascript" type="text/javascript">parent.frames.${reloadFrame.frame}.location.href="${reloadFrame.view}"</script>
</c:forEach>

</body></html>
