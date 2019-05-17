<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<%--@elvariable id="command" type="org.airsonic.player.command.AdvancedSettingsCommand"--%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <script type="text/javascript" src="<c:url value="/script/utils.js"/>"></script>
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
    <c:param name="toast" value="${settings_toast}"/>
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

    <table class="indent">
        <tr>
            <td><fmt:message key="advancedsettings.smtpFrom"/></td>
            <td>
                <form:input path="smtpFrom" size="50"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="smtpFrom"/></c:import>
            </td>
        </tr>

        <tr>
            <td><fmt:message key="advancedsettings.smtpServer"/></td>
            <td>
                <form:input path="smtpServer" size="50"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="smtpServer"/></c:import>
            </td>
        </tr>

        <tr>
            <td><fmt:message key="advancedsettings.smtpPort"/></td>
            <td>
                <form:input path="smtpPort" size="5"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="smtpPort"/></c:import>
            </td>
        </tr>

        <tr>
            <td><fmt:message key="advancedsettings.smtpEncryption"/></td>
            <td>
                <form:select path="smtpEncryption" cssStyle="width:8em">
                    <fmt:message key="advancedsettings.smtpEncryption.none" var="none"/>
                    <fmt:message key="advancedsettings.smtpEncryption.starttls" var="starttls"/>
                    <fmt:message key="advancedsettings.smtpEncryption.ssl" var="ssl"/>

                    <form:option value="None" label="None"/>
                    <form:option value="STARTTLS" label="STARTTLS"/>
                    <form:option value="SSL/TLS" label="SSL/TLS"/>
                </form:select>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="smtpEncryption"/></c:import>
            </td>
        </tr>

        <tr>
            <td><fmt:message key="advancedsettings.smtpUser"/></td>
            <td>
                <form:input path="smtpUser" size="20"/>
                <fmt:message key="advancedsettings.smtpPassword"/>
                <form:password path="smtpPassword" size="20"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="smtpCredentials"/></c:import>
            </td>
        </tr>
    </table>

    <table class="indent">
        <tr>
            <td colspan="2">
                <form:checkbox path="captchaEnabled" id="enablecaptcha"/>
                <label for="enablecaptcha">
                    <fmt:message key="advancedsettings.enableCaptcha"/>
                </label>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="captcha"/></c:import>
            </td>
        </tr>

        <tr>
            <td><fmt:message key="advancedsettings.recaptchaSiteKey"/></td>
            <td>
                <form:input path="recaptchaSiteKey" size="50"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="recaptchaSiteKey"/></c:import>
            </td>
        </tr>

        <tr>
            <td><fmt:message key="advancedsettings.recaptchaSecretKey"/></td>
            <td>
                <form:input path="recaptchaSecretKey" size="50"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="recaptchaSecretKey"/></c:import>
            </td>
        </tr>
    </table>

    <table class="indent"><tr><td>
        <form:checkbox path="ldapEnabled" id="ldap" onclick="enableLdapFields()"/>
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

    <p class="warning"><fmt:message key="advancedsettings.ldapRequiresRestart"/></p>

    <input type="submit" value="<fmt:message key="common.save"/>" style="margin-right:0.3em">
    <a href="nowPlaying.view"><input type="button" value="<fmt:message key="common.cancel"/>"></a>

</form:form>

<c:if test="${settings_reload}">
    <script language="javascript" type="text/javascript">
        parent.frames.left.location.href="left.view?";
        parent.frames.playQueue.location.href="playQueue.view?";
    </script>
</c:if>

</body></html>
