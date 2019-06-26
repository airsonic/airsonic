<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>
<%@ include file="include.jsp" %>

<%--
  Shows online help as a balloon tool tip.

PARAMETERS
  topic: Refers to a key in the resource bundle containing the text to display in the tool tip.
--%>

<script>
$( function() {
	$( document ).tooltip();
});
</script>

<style>
	div.ui-tooltip {
		max-width: 400px;
	}
</style>

<spring:theme code="helpPopupImage" var="imageUrl"/>
<fmt:message key="common.help" var="help"/>

<div id="placeholder-${param.topic}" style="display:none">
    <div style="font-weight:bold;"><fmt:message key="helppopup.${param.topic}.title"><fmt:param value="Airsonic"/></fmt:message></div>
    <div></div>
</div>
<img src="${imageUrl}" alt="<fmt:message key="helppopup.${param.topic}.text"><fmt:param value="Airsonic"/></fmt:message>" title="<fmt:message key="helppopup.${param.topic}.text"><fmt:param value="Airsonic"/></fmt:message>" />
