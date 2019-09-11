<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>
<%@ include file="include.jsp" %>

<%--
  Shows online help as a balloon tool tip.

PARAMETERS
  topic: Refers to a key in the resource bundle containing the text to display in the tool tip.
--%>

<spring:theme code="helpPopupImage" var="imageUrl"/>
<fmt:message key="common.help" var="help"/>

<div id="placeholder-${param.topic}" style="display:none">
    <div style="font-weight:bold;"><fmt:message key="helppopup.${param.topic}.title"><fmt:param value="Airsonic"/></fmt:message></div>
    <div><fmt:message key="helppopup.${param.topic}.text"><fmt:param value="Airsonic"/></fmt:message></div>
</div>
<img src="${imageUrl}" alt="${help}" title="${help}" onmouseover="TagToTip('placeholder-${param.topic}', BALLOON, true, ABOVE, true, OFFSETX, -17, PADDING, 8, WIDTH, -240, CLICKSTICKY, true, CLICKCLOSE, true)" onmouseout="UnTip()"/>
