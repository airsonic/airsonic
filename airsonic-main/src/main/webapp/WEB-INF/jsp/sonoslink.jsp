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

    <form action="<c:url value="/sonoslink"/>" method="POST">
        <sec:csrfInput />

        <input type="hidden" name="householdid" value="${model.householdid}">
        <input type="hidden" name="linkCode" value="${model.linkCode}">
        <div id="loginframe" class="bgcolor2 shadow">

            <img src="<c:url value="/icons/sonoslink.png"/>" alt="">

            <div class="loginmessagetop"><sub:wiki text="${model.loginMessage}"/></div>

            <input type="text" id="j_username" name="j_username" tabindex="1" placeholder="<fmt:message key="login.username"/>">

            <input type="password" name="j_password" tabindex="2" placeholder="<fmt:message key="login.password"/>">

            <input name="submit" type="submit" value="<fmt:message key="sonos.login"/>" tabindex="4"></td>

            <c:if test="${not empty model.errorCode}">
                <div class="loginmessagebottom">
                    <span class="warning"><fmt:message key="${model.errorCode}"/></span>
                </div>
            </c:if>
        </div>
    </form>

</body>
</html>
