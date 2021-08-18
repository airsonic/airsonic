<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html><head>
    <%@ include file="head.jsp" %>
    <script type="text/javascript">
        if (window != window.top) {
            top.location.href = location.href;
        }
    </script>

</head>
<body style="min-width:550px" class="mainframe bgcolor1">

    <form method="post" action="/sonoslink">
        <sec:csrfInput />

        <div id="loginframe" class="bgcolor2 shadow">

            <img src="<spring:theme code='logoImage'/>" alt="">

            <div class="loginmessagetop">${model.loginMessage}</div>

            <input type="hidden" value="${model.linkCode}" name="linkCode"/>

            <input required type="text" autofocus id="j_username" name="j_username" tabindex="1" placeholder="<fmt:message key='login.username'/>">

            <input required type="password" autocomplete="off"  name="j_password" tabindex="2" placeholder="<fmt:message key='login.password'/>">

            <input name="submit" type="submit" value="<fmt:message key='login.login'/>" tabindex="4"></td>

            <c:if test="${model.error}">
                <div class="loginmessagebottom">
                    <span class="warning"><fmt:message key="login.error"/></span>
                </div>
            </c:if>

        </div>
    </form>

</body>
</html>
