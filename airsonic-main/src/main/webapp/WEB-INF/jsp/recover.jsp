<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<!DOCTYPE html>

<html>
<head>
    <%@ include file="head.jsp" %>
</head>
<body class="mainframe bgcolor1">

<form action="recover.view" method="POST">
    <sec:csrfInput />
    <div class="bgcolor2 shadow" style="padding:20px 50px 20px 50px; margin-top:100px;margin-left:50px;margin-right:50px">

        <div style="margin-left: auto; margin-right: auto; width: 45em">

            <h1><fmt:message key="recover.title"/></h1>
            <p style="padding-top: 1em; padding-bottom: 0.5em"><fmt:message key="recover.text"/></p>

            <c:if test="${empty model.sentTo}">
                <input type="text" id="usernameOrEmail" autofocus name="usernameOrEmail" style="width:18em;margin-right: 1em">
                <input name="submit" type="submit" value="<fmt:message key="recover.send"/>">
            </c:if>

            <c:if test="${not empty model.recaptchaSiteKey and empty model.sentTo}">
                <p style="padding-top: 1em">
                    <script src="https://www.google.com/recaptcha/api.js" async defer></script>
                    <div class="g-recaptcha" data-sitekey="${model.recaptchaSiteKey}"></div>
                </p>
            </c:if>

            <c:if test="${not empty model.sentTo}">
                <p style="padding-top: 1em"><fmt:message key="recover.success"><fmt:param value="${model.sentTo}"/></fmt:message></p>
            </c:if>

            <c:if test="${not empty model.error}">
                <p style="padding-top: 1em" class="warning"><fmt:message key="${model.error}"/></p>
            </c:if>

            <div class="back" style="margin-top: 1.5em"><a href="login.view"><fmt:message key="common.back"/></a></div>

        </div>
    </div>
</form>
</body>
</html>
