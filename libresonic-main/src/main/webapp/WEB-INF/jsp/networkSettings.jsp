<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<%--@elvariable id="command" type="org.libresonic.player.command.NetworkSettingsCommand"--%>

<html>
<head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <script type="text/javascript" src="<c:url value="/dwr/interface/multiService.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/engine.js"/>"></script>
    <script type="text/javascript" language="javascript">

        function init() {
            enableUrlRedirectionFields();
            refreshStatus();
        }

        function refreshStatus() {
            multiService.getNetworkStatus(updateStatus);
        }

        function updateStatus(networkStatus) {
            $("#portForwardingStatus").html(networkStatus.portForwardingStatusText);
            $("#urlRedirectionStatus").html(networkStatus.urlRedirectionStatusText);
            window.setTimeout("refreshStatus()", 1000);
        }

        function enableUrlRedirectionFields() {
            var urlRedirectionEnabled = $("#urlRedirectionEnabled").is(":checked");
            var normal = $("#urlRedirectTypeNormal").is(":checked");
            var custom = $("#urlRedirectTypeCustom").is(":checked");

            $("#urlRedirectFrom").prop("disabled", !urlRedirectionEnabled || !normal);
            $("#urlRedirectCustomUrl").prop("disabled", !urlRedirectionEnabled || !custom);
            $("#urlRedirectTypeNormal").prop("disabled", !urlRedirectionEnabled);
            $("#urlRedirectTypeCustom").prop("disabled", !urlRedirectionEnabled);
        }

    </script>
</head>
<body class="mainframe bgcolor1" onload="init()">
<script type="text/javascript" src="<c:url value="/script/wz_tooltip.js"/>"></script>
<script type="text/javascript" src="<c:url value="/script/tip_balloon.js"/>"></script>

<c:import url="settingsHeader.jsp">
    <c:param name="cat" value="network"/>
    <c:param name="toast" value="${command.toast}"/>
</c:import>

<p style="padding-top:1em"><fmt:message key="networksettings.text"/></p>

<form:form commandName="command" action="networkSettings.view" method="post">
    <p style="padding-top:1em">
        <form:checkbox id="portForwardingEnabled" path="portForwardingEnabled"/>
        <label for="portForwardingEnabled"><fmt:message key="networksettings.portforwardingenabled"/></label>
    </p>

    <div style="padding-left:2em;max-width:60em">
        <p>
            <fmt:message key="networksettings.portforwardinghelp"><fmt:param>${command.port}</fmt:param></fmt:message>
        </p>

        <p class="detail">
            <fmt:message key="networksettings.status"/>
            <span id="portForwardingStatus" style="margin-left:0.25em"></span>
        </p>
    </div>

    <p style="padding-top:1em"><form:checkbox id="urlRedirectionEnabled" path="urlRedirectionEnabled"
                                              onclick="enableUrlRedirectionFields()"/>
        <label for="urlRedirectionEnabled"><fmt:message key="networksettings.urlredirectionenabled"/></label>
    </p>

    <div style="padding-left:2em">

        <p>
            <form:radiobutton id="urlRedirectTypeNormal" path="urlRedirectType" value="NORMAL" onclick="enableUrlRedirectionFields()"/>
            <label for="urlRedirectTypeNormal"><fmt:message key="networksettings.normalurl"/></label>
            http://<form:input id="urlRedirectFrom" path="urlRedirectFrom" size="16" cssStyle="margin-left:0.25em"/>.libresonic.org (<fmt:message key="networksettings.urlRedirectDisabled"/>)
        </p>

        <p>
            <form:radiobutton id="urlRedirectTypeCustom" path="urlRedirectType" value="CUSTOM" onclick="enableUrlRedirectionFields()"/>
            <label for="urlRedirectTypeCustom"><fmt:message key="networksettings.customurl"/></label>
            <form:input id="urlRedirectCustomUrl" path="urlRedirectCustomUrl" size="50" cssStyle="margin-left:0.5em"/>
        </p>
        <p class="detail">
            <fmt:message key="networksettings.status"/>
            <span id="urlRedirectionStatus" style="margin-left:0.25em"></span>
            <span id="urlRedirectionTestStatus" style="margin-left:0.25em"></span>
        </p>
    </div>

    <c:set var="licenseInfo" value="${command.licenseInfo}"/>
    <%@ include file="licenseNotice.jsp" %>

    <p style="padding-top:1em">
        <input type="submit" value="<fmt:message key="common.save"/>" style="margin-right:0.3em">
        <input type="button" value="<fmt:message key="common.cancel"/>" onclick="location.href='nowPlaying.view'">
    </p>

</form:form>
</body>
</html>
