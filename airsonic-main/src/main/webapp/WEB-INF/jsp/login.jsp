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
<body style="min-width:550px" class="mainframe bgcolor1">

    <form action="<c:url value="/login"/>" method="POST">
        <sec:csrfInput />

        <div id="loginframe" class="bgcolor2 shadow">

            <img src="<spring:theme code="logoImage"/>" alt="">

            <div class="loginmessagetop"><sub:wiki text="${model.loginMessage}"/></div>

            <input type="text" autofocus id="j_username" name="j_username" tabindex="1" placeholder="<fmt:message key="login.username"/>">

            <input type="password" name="j_password" tabindex="2" placeholder="<fmt:message key="login.password"/>">

            <input name="submit" type="submit" value="<fmt:message key="login.login"/>" tabindex="4"></td>

            <div class="details">
                <div id="loginremember">
                    <label for="remember"><fmt:message key="login.remember"/></label>
                    <input type="checkbox" name="remember-me" id="remember" class="checkbox" tabindex="3">
                </div>

                <a href="recover.view"><fmt:message key="login.recover"/></a>
            </div>

            <c:if test="${model.logout}">
                <div class="loginmessagebottom">
                    <b><fmt:message key="login.logout"/></b>
                </div>
            </c:if>

            <c:if test="${model.error}">
                <div class="loginmessagebottom">
                    <span class="warning"><fmt:message key="login.error"/></span>
                </div>
            </c:if>

            <c:if test="${model.insecure}">
                <div class="loginmessagebottom">
                    <p class="warning"><fmt:message key="login.insecure"><fmt:param value="${model.brand}"/></fmt:message></p>
                </div>
            </c:if>

        </div>
    </form>

</body>
</html>
