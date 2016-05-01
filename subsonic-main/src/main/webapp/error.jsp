<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" isErrorPage="true" %>
<%@ page import="java.io.PrintWriter, java.io.StringWriter"%>

<html><head>
    <!--[if lt IE 7.]>
    <script defer type="text/javascript" src="script/pngfix.js"></script>
    <![endif]-->
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <link rel="stylesheet" href="style/default.css" type="text/css"/>
</head>

<body>
<h1>
    <img src="icons/default_light/error.png" alt=""/>
    <span style="vertical-align: middle">Error</span>
</h1>

<p>
    Subsonic encountered an internal error. You can report this error in the
    <a href="http://forum.subsonic.org" target="_blank">Subsonic Forum</a>.
    Please include the information below.
</p>

<%
    StringWriter sw = new StringWriter();
    exception.printStackTrace(new PrintWriter(sw));

    long totalMemory = Runtime.getRuntime().totalMemory();
    long freeMemory = Runtime.getRuntime().freeMemory();
    long usedMemory = totalMemory - freeMemory;
%>

<table class="ruleTable indent">
    <tr><td class="ruleTableHeader">Exception</td>
        <td class="ruleTableCell"><%=exception.getClass().getName()%></td></tr>
    <tr><td class="ruleTableHeader">Message</td>
        <td class="ruleTableCell"><%=exception.getMessage()%></td></tr>
    <tr><td class="ruleTableHeader">Java version</td>
        <td class="ruleTableCell"><%=System.getProperty("java.vendor") + ' ' + System.getProperty("java.version")%></td></tr>
    <tr><td class="ruleTableHeader">Operating system</td>
        <td class="ruleTableCell"><%=System.getProperty("os.name") + ' ' + System.getProperty("os.version")%></td></tr>
    <tr><td class="ruleTableHeader">Server</td>
        <td class="ruleTableCell"><%=application.getServerInfo()%></td></tr>
    <tr><td class="ruleTableHeader">Memory</td>
        <td class="ruleTableCell">Used <%=usedMemory/1024L/1024L%> of <%=totalMemory/1024L/1024L%> MB</td></tr>
    <tr><td class="ruleTableHeader" style="vertical-align:top;">Stack trace</td>
        <td class="ruleTableCell" style="white-space:pre"><%=sw.getBuffer()%></td></tr>
</table>

</body>
</html>
