<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<%@ include file="include.jsp" %>

<%--
PARAMETERS
  id: ID of file.
  video: Whether the file is a video (default false).
  playEnabled: Whether to show play button (default true).
  addEnabled: Whether to show add next/last buttons (default true).
  downloadEnabled: Whether to show download button (default false).
  starEnabled: Whether to show star/unstar controls (default false).
  starred: Whether the file is currently starred.
  asTable: Whether to put the images in td tags.
  onPlay: Overrides the javascript used for the play action.
--%>

<c:if test="${param.starEnabled}">
    <c:if test="${param.asTable}"><td class="fit"></c:if>
    <c:choose>
        <c:when test="${param.starred}">
            <img id="starImage${param.id}" src="<spring:theme code="ratingOnImage"/>" alt="" style="cursor:pointer"
                 onclick="toggleStar(${param.id}, '#starImage${param.id}'); return false;">
        </c:when>
        <c:otherwise>
            <img id="starImage${param.id}" src="<spring:theme code="ratingOffImage"/>" alt="" style="cursor:pointer"
                 onclick="toggleStar(${param.id}, '#starImage${param.id}'); return false;">
        </c:otherwise>
    </c:choose>
    <c:if test="${param.asTable}"></td></c:if>
</c:if>

<c:if test="${param.asTable}"><td class="fit"></c:if>
<c:if test="${empty param.playEnabled or param.playEnabled}">
    <c:choose>
        <c:when test="${param.video}">
            <sub:url value="/videoPlayer.view" var="videoUrl">
                <sub:param name="id" value="${param.id}"/>
            </sub:url>
            <a href="${videoUrl}" target="main">
                <img src="<spring:theme code="playImage"/>" alt="<fmt:message key="common.play"/>"
                     title="<fmt:message key="common.play"/>"></a>
        </c:when>
        <c:when test="${not empty param.onPlay}">
            <img src="<spring:theme code="playImage"/>" alt="<fmt:message key="common.play"/>" style="cursor:pointer"
                 onclick="${param.onPlay}; return false;" title="<fmt:message key="common.play"/>">
        </c:when>
        <c:otherwise>
            <img src="<spring:theme code="playImage"/>" alt="<fmt:message key="common.play"/>" style="cursor:pointer"
                 onclick="top.playQueue.onPlay(${param.id}); return false;" title="<fmt:message key="common.play"/>">
        </c:otherwise>
    </c:choose>
</c:if>
<c:if test="${param.asTable}"></td></c:if>

<c:if test="${param.asTable}"><td class="fit"></c:if>
<c:if test="${(empty param.addEnabled or param.addEnabled) and not param.video}">
    <img id="add${param.id}" src="<spring:theme code="addImage"/>" alt="<fmt:message key="main.addlast"/>"
         onclick="top.playQueue.onAdd(${param.id}); $().toastmessage('showSuccessToast', '<fmt:message key="main.addlast.toast"/>'); return false;"
         style="cursor:pointer" title="<fmt:message key="main.addlast"/>">
</c:if>
<c:if test="${param.asTable}"></td></c:if>

<c:if test="${param.asTable}"><td class="fit"></c:if>
<c:if test="${(empty param.addEnabled or param.addEnabled) and not param.video}">
    <img id="add${param.id}" src="<spring:theme code="addNextImage"/>" alt="<fmt:message key="main.addnext"/>"
         onclick="top.playQueue.onAddNext(${param.id}); $().toastmessage('showSuccessToast', '<fmt:message key="main.addnext.toast"/>'); return false;"
         style="cursor:pointer" title="<fmt:message key="main.addnext"/>">
</c:if>
<c:if test="${param.asTable}"></td></c:if>

<c:if test="${param.asTable}"><td class="fit"></c:if>
<c:if test="${param.downloadEnabled}">
    <sub:url value="/download.view" var="downloadUrl">
        <sub:param name="id" value="${param.id}"/>
    </sub:url>
    <a href="${downloadUrl}">
        <img src="<spring:theme code="downloadImage"/>" alt="<fmt:message key="common.download"/>"
             title="<fmt:message key="common.download"/>" ></a>
</c:if>
<c:if test="${param.asTable}"></td></c:if>
