<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html><head>
    <%@ include file="head.jsp" %>
</head><body class="mainframe bgcolor1" onload="document.getElementById('query').focus()">

<h1>Database query</h1>

<form method="post" action="db.view">
    <sec:csrfInput />
    <textarea rows="10" cols="80" id="query" name="query" style="margin-top:1em">${model.query}</textarea>
    <input type="submit" value="<fmt:message key="common.ok"/>">
</form>

<c:if test="${not empty model.result}">
    <h1 style="margin-top:2em">Result</h1>

    <table class="indent ruleTable">
        <c:forEach items="${model.result}" var="row" varStatus="loopStatus">

            <c:if test="${loopStatus.count == 1}">
                <tr>
                    <c:forEach items="${row}" var="entry">
                        <td class="ruleTableHeader">${entry.key}</td>
                    </c:forEach>
                </tr>
            </c:if>
            <tr>
                <c:forEach items="${row}" var="entry">
                    <td class="ruleTableCell">${entry.value}</td>
                </c:forEach>
            </tr>
        </c:forEach>

    </table>
</c:if>

<c:if test="${not empty model.error}">
    <h1 style="margin-top:2em">Error</h1>

    <p class="warning">
        ${model.error}
    </p>
</c:if>

</body></html>