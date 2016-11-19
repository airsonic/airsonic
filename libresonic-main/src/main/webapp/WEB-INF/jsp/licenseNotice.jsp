<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<c:if test="${licenseInfo.trial}">
    <fmt:formatDate value="${licenseInfo.trialExpires}" dateStyle="long" var="expiryDate"/>

    <p class="warning" style="padding-top:1em">
        <c:choose>
            <c:when test="${licenseInfo.trialExpired}">
                <fmt:message key="common.trialexpired"><fmt:param>${expiryDate}</fmt:param></fmt:message>
            </c:when>
            <c:otherwise>
                <fmt:message key="common.trialnotexpired"><fmt:param>${expiryDate}</fmt:param></fmt:message>
            </c:otherwise>
        </c:choose>
    </p>
</c:if>

