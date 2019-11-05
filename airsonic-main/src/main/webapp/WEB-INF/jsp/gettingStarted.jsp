<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html><head>
    <%@ include file="head.jsp" %>
    <script type="text/javascript" language="javascript">
        function hideGettingStarted() {
            alert("<fmt:message key="gettingStarted.hidealert"/>");
            location.href = "gettingStarted.view?hide";
        }
    </script>
</head>
<body class="mainframe bgcolor1">

<h1 style="padding-bottom:0.5em">
    <img src="<spring:theme code='homeImage'/>" alt="">
    <fmt:message key="gettingStarted.title"/>
</h1>

<fmt:message key="gettingStarted.text"/>

<c:if test="${model.runningAsRoot}">
    <h2 class="warning"><fmt:message key="gettingStarted.root"/></h2>
</c:if>

<table style="padding-top:1em;padding-bottom:2em;width:60%">
    <tr>
        <td style="font-size:26pt;padding:20pt">1</td>
        <td>
            <div style="font-size:14pt"><a href="userSettings.view?userIndex=0"><fmt:message key="gettingStarted.step1.title"/></a></div>
            <div style="padding-top:5pt"><fmt:message key="gettingStarted.step1.text"/></div>
        </td>
    </tr>
    <tr>
        <td style="font-size:26pt;padding:20pt">2</td>
        <td>
            <div style="font-size:14pt"><a href="musicFolderSettings.view"><fmt:message key="gettingStarted.step2.title"/></a></div>
            <div style="padding-top:5pt"><fmt:message key="gettingStarted.step2.text"/></div>
        </td>
    </tr>
    <tr>
        <td style="font-size:26pt;padding:20pt">3</td>
        <td>
            <div style="font-size:14pt"><fmt:message key="gettingStarted.step3.title"/></div>
            <div style="padding-top:5pt"><fmt:message key="gettingStarted.step3.text"/></div>
        </td>
    </tr>

</table>

<div class="forward"><a href="javascript:hideGettingStarted()"><fmt:message key="gettingStarted.hide"/></a></div>

</body></html>
