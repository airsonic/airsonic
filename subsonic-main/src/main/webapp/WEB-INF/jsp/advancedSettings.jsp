<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<%--@elvariable id="command" type="net.sourceforge.subsonic.command.AdvancedSettingsCommand"--%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <script type="text/javascript" src="<c:url value="/script/scripts-2.0.js"/>"></script>
    <script type="text/javascript" language="javascript">
        function enableLdapFields() {
            $("#ldap").is(":checked") ? $("#ldapTable").show() : $("#ldapTable").hide();
        }
    </script>
</head>

<body class="mainframe bgcolor1" onload="enableLdapFields()">
<script type="text/javascript" src="<c:url value="/script/wz_tooltip.js"/>"></script>
<script type="text/javascript" src="<c:url value="/script/tip_balloon.js"/>"></script>

<c:import url="settingsHeader.jsp">
    <c:param name="cat" value="advanced"/>
    <c:param name="toast" value="${command.toast}"/>
</c:import>

<form:form method="post" action="advancedSettings.view" commandName="command">

    <table style="white-space:nowrap" class="indent">
        <tr>
            <td><fmt:message key="advancedsettings.downloadlimit"/></td>
            <td>
                <form:input path="downloadLimit" size="8"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="downloadlimit"/></c:import>
            </td>
        </tr>

        <tr>
            <td><fmt:message key="advancedsettings.uploadlimit"/></td>
            <td>
                <form:input path="uploadLimit" size="8"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="uploadlimit"/></c:import>
            </td>
        </tr>
    </table>

    <table class="indent"><tr><td>
        <form:checkbox path="ldapEnabled" id="ldap" cssClass="checkbox" onclick="enableLdapFields()"/>
        <label for="ldap"><fmt:message key="advancedsettings.ldapenabled"/></label>
        <c:import url="helpToolTip.jsp"><c:param name="topic" value="ldap"/></c:import>
    </td></tr></table>

    <table class="indent" id="ldapTable" style="padding-left:2em;padding-bottom: 1em">
        <tr>
            <td><fmt:message key="advancedsettings.ldapurl"/></td>
            <td colspan="3">
                <form:input path="ldapUrl" size="70"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="ldapurl"/></c:import>
            </td>
        </tr>

        <tr>
            <td><fmt:message key="advancedsettings.ldapsearchfilter"/></td>
            <td colspan="3">
                <form:input path="ldapSearchFilter" size="70"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="ldapsearchfilter"/></c:import>
            </td>
        </tr>

        <tr>
            <td><fmt:message key="advancedsettings.ldapmanagerdn"/></td>
            <td>
                <form:input path="ldapManagerDn" size="20"/>
            </td>
            <td><fmt:message key="advancedsettings.ldapmanagerpassword"/></td>
            <td>
                <form:password path="ldapManagerPassword" size="20"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="ldapmanagerdn"/></c:import>
            </td>
        </tr>

        <tr>
            <td colspan="5">
                <form:checkbox path="ldapAutoShadowing" id="ldapAutoShadowing" cssClass="checkbox"/>
                <label for="ldapAutoShadowing"><fmt:message key="advancedsettings.ldapautoshadowing"><fmt:param value="${command.brand}"/></fmt:message></label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="ldapautoshadowing"/></c:import>
            </td>
        </tr>
    </table>

    <input type="submit" value="<fmt:message key="common.save"/>" style="margin-right:0.3em">
    <input type="button" value="<fmt:message key="common.cancel"/>" onclick="location.href='nowPlaying.view'">

</form:form>

<c:if test="${command.reloadNeeded}">
    <script language="javascript" type="text/javascript">
        parent.frames.left.location.href="left.view?";
        parent.frames.playQueue.location.href="playQueue.view?";
    </script>
</c:if>

</body></html>