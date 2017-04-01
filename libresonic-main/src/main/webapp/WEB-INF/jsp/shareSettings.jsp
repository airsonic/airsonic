<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<%--@elvariable id="model" type="Map"--%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
</head>
<body class="mainframe bgcolor1">

<c:import url="settingsHeader.jsp">
    <c:param name="cat" value="share"/>
    <c:param name="toast" value="${settings_toast}"/>
    <c:param name="restricted" value="${not model.user.adminRole}"/>
</c:import>

<form method="post" action="shareSettings.view">
    <sec:csrfInput />

    <table class="music indent">
        <tr>
            <th style="padding-left:1em"><fmt:message key="sharesettings.name"/></th>
            <th style="padding-left:1em"><fmt:message key="sharesettings.owner"/></th>
            <th style="padding-left:1em"><fmt:message key="sharesettings.description"/></th>
            <th style="padding-left:1em"><fmt:message key="sharesettings.lastvisited"/></th>
            <th style="padding-left:1em"><fmt:message key="sharesettings.visits"/></th>
            <th style="padding-left:1em"><fmt:message key="sharesettings.files"/></th>
            <th style="padding-left:1em"><fmt:message key="sharesettings.expires"/></th>
            <th style="padding-left:1em"><fmt:message key="sharesettings.expirein"/></th>
            <th style="padding-left:1em"><fmt:message key="common.delete"/></th>
        </tr>

        <c:forEach items="${model.shareInfos}" var="shareInfo" varStatus="loopStatus">
            <c:set var="share" value="${shareInfo.share}"/>

            <c:url value="main.view" var="albumUrl">
                <c:param name="id" value="${shareInfo.dir.id}"/>
            </c:url>

            <tr>
                <td style="padding-left:1em"><a href="${shareInfo.shareUrl}" target="_blank">${share.name}</a></td>
                <td style="padding-left:1em">${fn:escapeXml(share.username)}</td>
                <td style="padding-left:1em"><input type="text" name="description[${share.id}]" size="40" value="${share.description}"/></td>
                <td style="padding-left:1em"><fmt:formatDate value="${share.lastVisited}" type="date" dateStyle="medium"/></td>
                <td style="padding-left:1em; text-align:right">${share.visitCount}</td>
                <td style="padding-left:1em"><a href="${albumUrl}" title="${shareInfo.dir.name}"><str:truncateNicely upper="30">${fn:escapeXml(shareInfo.dir.name)}</str:truncateNicely></a></td>
                <td style="padding-left:1em"><fmt:formatDate value="${share.expires}" type="date" dateStyle="medium"/></td>
                <td style="padding-left:1em">
                    <label><input type="radio" name="expireIn[${share.id}]" value="7"><fmt:message key="sharesettings.expirein.week"/></label>
                    <label><input type="radio" name="expireIn[${share.id}]" value="30"><fmt:message key="sharesettings.expirein.month"/></label>
                    <label><input type="radio" name="expireIn[${share.id}]" value="365"><fmt:message key="sharesettings.expirein.year"/></label>
                    <label><input type="radio" name="expireIn[${share.id}]" value="0"><fmt:message key="sharesettings.expirein.never"/></label>
                </td>
                <td style="padding-left:1em" align="center" style="padding-left:1em"><input type="checkbox" name="delete[${share.id}]" class="checkbox"/></td>
            </tr>
        </c:forEach>

    </table>

    <p style="padding-top:1em">
        <input type="submit" value="<fmt:message key="common.save"/>" style="margin-right:0.3em">
        <input type="button" value="<fmt:message key="common.cancel"/>" onclick="location.href='nowPlaying.view'" style="margin-right:2.0em">
        <input type="checkbox" id="deleteExpired" name="deleteExpired" class="checkbox"/>
        <label for="deleteExpired"><fmt:message key="sharesettings.deleteexpired"/></label>
    </p>

</form>

</body></html>
