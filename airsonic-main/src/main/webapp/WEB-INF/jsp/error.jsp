<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>


<html><head>
     <%@ include file="head.jsp" %>
    <!--[if lt IE 7.]>
    <script defer type="text/javascript" src="script/pngfix.js"></script>
    <![endif]-->
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">

</head>

<body class="mainframe bgcolor1">
<h1>
    <img src="<spring:theme code="errorImage"/>" alt=""/>
    <span style="vertical-align: middle">Error</span>
</h1>

<p>
    Airsonic encountered an internal error. You can report this error in the
    <a href="https://www.reddit.com/r/airsonic" target="_blank">Airsonic Forum</a>.
    Please include the information below.
</p>

<%

    long totalMemory = Runtime.getRuntime().totalMemory();
    long freeMemory = Runtime.getRuntime().freeMemory();
    long usedMemory = totalMemory - freeMemory;
%>

<table class="ruleTable indent">
    <tr><td class="ruleTableHeader">Status</td>
        <td class="ruleTableCell"><c:out value="${status}" /></td></tr>
    <tr><td class="ruleTableHeader">Error</td>
        <td class="ruleTableCell"><c:out value="${error}" /></td></tr>
    <tr><td class="ruleTableHeader">Message</td>
        <td class="ruleTableCell"><c:out value="${message}" /></td></tr>
    <tr><td class="ruleTableHeader">Path</td>
        <td class="ruleTableCell"><c:out value="${path}" /></td></tr>
    <tr><td class="ruleTableHeader">Time</td>
        <td class="ruleTableCell"><c:out value="${timestamp}" /></td></tr>
    <tr><td class="ruleTableHeader">Exception</td>
        <td class="ruleTableCell"><c:out value="${exception}" /></td></tr>
    <tr><td class="ruleTableHeader">Java version</td>
        <td class="ruleTableCell"><%=System.getProperty("java.vendor") + ' ' + System.getProperty("java.version")%></td></tr>
    <tr><td class="ruleTableHeader">Operating system</td>
        <td class="ruleTableCell"><%=System.getProperty("os.name") + ' ' + System.getProperty("os.version")%></td></tr>
    <tr><td class="ruleTableHeader">Server</td>
        <td class="ruleTableCell"><%=application.getServerInfo()%></td></tr>
    <tr><td class="ruleTableHeader">Memory</td>
        <td class="ruleTableCell">Used <%=usedMemory/1024L/1024L%> of <%=totalMemory/1024L/1024L%> MB</td></tr>
    <c:if test="${not empty trace}">
        <tr>
            <td class="ruleTableHeader" style="vertical-align:top;">Stack trace</td>
            <td class="ruleTableCell" style="white-space:pre">
                <pre>
                        ${fn:escapeXml(trace)}
                </pre>
            </td>
        </tr>
    </c:if>
</table>

</body>
</html>
