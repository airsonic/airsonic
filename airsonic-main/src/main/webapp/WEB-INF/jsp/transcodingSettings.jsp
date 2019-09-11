<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
</head>
<body class="mainframe bgcolor1">
<script type="text/javascript" src="<c:url value="/script/wz_tooltip.js"/>"></script>
<script type="text/javascript" src="<c:url value="/script/tip_balloon.js"/>"></script>

<c:import url="settingsHeader.jsp">
    <c:param name="cat" value="transcoding"/>
    <c:param name="toast" value="${settings_toast}"/>
</c:import>

<form method="post" action="transcodingSettings.view">
<sec:csrfInput />
<table class="indent">
    <tr>
        <th><fmt:message key="transcodingsettings.name"/></th>
        <th><fmt:message key="transcodingsettings.sourceformat"/></th>
        <th><fmt:message key="transcodingsettings.targetformat"/></th>
        <th><fmt:message key="transcodingsettings.step1"/></th>
        <th><fmt:message key="transcodingsettings.step2"/></th>
        <th style="padding-left:1em"><fmt:message key="common.delete"/></th>
    </tr>

    <c:forEach items="${model.transcodings}" var="transcoding">
        <tr>
            <td><input class="monospace" name="name[${transcoding.id}]" size="10" value="${fn:escapeXml(transcoding.name)}"/></td>
            <td><input class="monospace" name="sourceFormats[${transcoding.id}]" size="36" value="${transcoding.sourceFormats}"/></td>
            <td><input class="monospace" name="targetFormat[${transcoding.id}]" size="10" value="${transcoding.targetFormat}"/></td>
            <td><input class="monospace" name="step1[${transcoding.id}]" size="60" value="${transcoding.step1}"/></td>
            <td><input class="monospace" name="step2[${transcoding.id}]" size="22" value="${transcoding.step2}"/></td>
            <td align="center" style="padding-left:1em"><input type="checkbox" name="delete[${transcoding.id}]"/></td>
        </tr>
    </c:forEach>

    <tr>
        <th colspan="6" align="left" style="padding-top:1em"><fmt:message key="transcodingsettings.add"/></th>
    </tr>

    <tr>
        <td><input class="monospace" name="name" size="10" placeholder="<fmt:message key="transcodingsettings.name"/>" value="${fn:escapeXml(newTranscoding.name)}"/></td>
        <td><input class="monospace" name="sourceFormats" size="36" placeholder="<fmt:message key="transcodingsettings.sourceformat"/>" value="${newTranscoding.sourceFormats}"/></td>
        <td><input class="monospace" name="targetFormat" size="10" placeholder="<fmt:message key="transcodingsettings.targetformat"/>" value="${newTranscoding.targetFormat}"/></td>
        <td><input class="monospace" name="step1" size="60" placeholder="<fmt:message key="transcodingsettings.step1"/>" value="${newTranscoding.step1}"/></td>
        <td><input class="monospace" name="step2" size="22" placeholder="<fmt:message key="transcodingsettings.step2"/>" value="${newTranscoding.step2}"/></td>
        <td/>
    </tr>

    <tr>
        <td colspan="6" style="padding-top:0.1em">
            <input type="checkbox" id="defaultActive" name="defaultActive" checked/>
            <label for="defaultActive"><fmt:message key="transcodingsettings.defaultactive"/></label>
        </td>
    </tr>
</table>


    <table style="white-space:nowrap" class="indent">
        <tr>
            <td style="font-weight: bold;">
                <fmt:message key="advancedsettings.downsamplecommand"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="downsamplecommand"/></c:import>
            </td>
            <td>
                <input class="monospace" type="text" name="downsampleCommand" size="100" value="${model.downsampleCommand}"/>
            </td>
        </tr>
        <tr>
            <td style="font-weight: bold;">
                <fmt:message key="advancedsettings.hlscommand"/>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="hlscommand"/></c:import>
            </td>
            <td>
                <input class="monospace" type="text" name="hlsCommand" size="100" value="${model.hlsCommand}"/>
            </td>
        </tr>
    </table>


    <p style="padding-top:0.75em">
        <input type="submit" value="<fmt:message key="common.save"/>" style="margin-right:0.3em">
        <a href='nowPlaying.view'><input type="button" value="<fmt:message key="common.cancel"/>" style="margin-right:1.3em"></a>
        <a href="https://airsonic.github.io/docs/transcode/" target="_blank" rel="noopener noreferrer"><fmt:message key="transcodingsettings.recommended"/></a>
    </p>

</form>

<c:if test="${not empty error}">
    <p class="warning"><fmt:message key="${error}"/></p>
</c:if>

<div style="width:60%">
    <fmt:message key="transcodingsettings.info"><fmt:param value="${model.transcodeDirectory}"/><fmt:param value="${model.brand}"/></fmt:message>
</div>
</body></html>
