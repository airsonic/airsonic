<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html><head>
    <%@ include file="head.jsp" %>
    <script type="text/javascript">
        if (window != window.top) {
            top.location.href = location.href;
        }
    </script>

</head>
<body class="mainframe bgcolor1" onload="document.getElementById('j_username').focus()">

<form action="<c:url value="/login"/>" method="POST">

    <sec:csrfInput />
    <div class="bgcolor2 shadow" align="center" style="padding:20px 50px 20px 50px; margin-top:100px;margin-left:50px;margin-right:50px">

        <div style="margin-bottom:1em;max-width:50em;text-align:left;"><sub:wiki text="${model.loginMessage}"/></div>

        <table>
        <tr>
            <td colspan="2" align="left" style="padding-bottom:10px">
                <img src="<spring:theme code="logoImage"/>" alt="">
            </td>
        </tr>
        <tr>
            <td align="left" style="padding-right:10px"><fmt:message key="login.username"/></td>
            <td align="left"><input type="text" id="j_username" name="j_username" style="width:12em" tabindex="1"></td>
        </tr>

        <tr>
            <td align="left" style="padding-bottom:10px"><fmt:message key="login.password"/></td>
            <td align="left" style="padding-bottom:10px"><input type="password" name="j_password" style="width:12em" tabindex="2"></td>
        </tr>

        <tr>
            <td align="left"><input name="submit" type="submit" value="<fmt:message key="login.login"/>" tabindex="4"></td>
            <td align="left" class="detail">
                <input type="checkbox" name="_spring_security_remember_me" id="remember" class="checkbox" tabindex="3">
                <label for="remember"><fmt:message key="login.remember"/></label>
            </td>
        </tr>
            <tr>
                <td></td>
                <td align="left" class="detail"><a href="recover.view"><fmt:message key="login.recover"/></a></td>
            </tr>
            
            <c:if test="${model.logout}">
                <tr><td colspan="2" style="padding-top:10px"><b><fmt:message key="login.logout"/></b></td></tr>
            </c:if>
            <c:if test="${model.error}">
                <tr><td colspan="2" style="padding-top:10px"><span class="warning"><fmt:message key="login.error"/></span></td></tr>
            </c:if>

        </table>

        <c:if test="${model.insecure}">
            <p class="warning"><fmt:message key="login.insecure"><fmt:param value="${model.brand}"/></fmt:message></p>
        </c:if>

    </div>
</form>
</body>
</html>
