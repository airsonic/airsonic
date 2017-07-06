<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<%@ include file="include.jsp" %>

<c:if test="${param.toast}">
    <script type="text/javascript">
        $(document).ready(function () {
            $().toastmessage("showSuccessToast", "<fmt:message key="common.settingssaved"/>");
        });
    </script>
</c:if>

<c:set var="categories" value="${param.restricted ? 'personal password player share' : 'musicFolder general advanced personal user player share dlna sonos transcoding internetRadio podcast database'}"/>
<h1>
    <img src="<spring:theme code="settingsImage"/>" alt=""/>
    <span style="vertical-align: middle"><fmt:message key="settingsheader.title"/></span>
</h1>

<h2>
<c:forTokens items="${categories}" delims=" " var="cat" varStatus="loopStatus">
    <c:choose>
        <c:when test="${loopStatus.count > 1 and  (loopStatus.count - 1) % 7 != 0}">&nbsp;|&nbsp;</c:when>
        <c:otherwise></h2><h2></c:otherwise>
    </c:choose>

    <c:url var="url" value="${cat}Settings.view?"/>

    <c:choose>
        <c:when test="${param.cat eq cat}">
            <span class="headerSelected"><fmt:message key="settingsheader.${cat}"/></span>
        </c:when>
        <c:otherwise>
            <span class="header"><a href="${url}"><fmt:message key="settingsheader.${cat}"/></a></span>
        </c:otherwise>
    </c:choose>

</c:forTokens>
</h2>

<p></p>
