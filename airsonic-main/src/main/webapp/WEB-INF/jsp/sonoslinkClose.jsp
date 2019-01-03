<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>
<!DOCTYPE html>

<html><head>
    <%@ include file="head.jsp" %>
    <script type="text/javascript">
        if (window != window.top) {
            top.location.href = location.href;
        }
    </script>

</head>
<body style="min-width:550px" class="mainframe bgcolor1" onload="document.getElementById('j_username').focus()">

        <div id="loginframe" class="bgcolor2 shadow">

            <img src="<spring:theme code="logoImage"/>" alt="">

            <div class="loginmessagetop"><fmt:message key="${model.messageKey}"/></div>
        </div>

</body>
</html>
