<%@ page contentType="text/html; charset=utf-8" pageEncoding="utf-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring"%>

<table>
    <tr>
        <c:if test="${not empty model.musicFolder}">
            <td style="padding-right: 2em">
                <div style="border:1px solid #<spring:theme code='detailColor'/>; padding-left: 0.5em;padding-right: 0.5em">
                        ${fn:escapeXml(model.musicFolder.name)}
                </div>
            </td>
        </c:if>
        <c:choose>
            <c:when test="${model.listType eq 'random'}">
                <td style="padding-left: 2em;">
                    <a href="home.view?listType=random">
                        <img src="<spring:theme code="refreshImage"/>" alt="Refresh" style="height:16px;">
                        <fmt:message key="common.refresh"/>
                    </a>
                </td>
            </c:when>
            <c:otherwise>
                <sub:url value="home.view" var="previousUrl">
                    <sub:param name="listType" value="${model.listType}"/>
                    <sub:param name="listOffset" value="${model.listOffset - model.listSize}"/>
                    <sub:param name="genre" value="${model.genre}"/>
                    <sub:param name="decade" value="${model.decade}"/>
                </sub:url>
                <sub:url value="home.view" var="nextUrl">
                    <sub:param name="listType" value="${model.listType}"/>
                    <sub:param name="listOffset" value="${model.listOffset + model.listSize}"/>
                    <sub:param name="genre" value="${model.genre}"/>
                    <sub:param name="decade" value="${model.decade}"/>
                </sub:url>

                <c:if test="${fn:length(model.albums) gt 0}">
                    <td style="padding-right:0.5em">
                        <fmt:message key="home.albums">
                            <fmt:param value="${model.listOffset + 1}"/>
                            <fmt:param value="${model.listOffset + fn:length(model.albums)}"/>
                        </fmt:message>
                    </td>
                </c:if>

                <c:if test="${model.listOffset gt 0}">
                    <td><a href="${previousUrl}"><img src="<spring:theme code='backImage'/>" alt=""></a></td>
                </c:if>

                <c:if test="${fn:length(model.albums) eq model.listSize}">
                    <td><a href="${nextUrl}"><img src="<spring:theme code='forwardImage'/>" alt=""></a></td>
                </c:if>

                <c:if test="${model.listType eq 'decade'}">
                    <td style="padding-left: 2em">
                        <fmt:message key="home.decade.text"/>
                    </td>
                    <td>
                        <select name="decade" onchange="location='home.view?listType=${model.listType}&amp;decade=' + options[selectedIndex].value">
                            <c:forEach items="${model.decades}" var="decade">
                                <option
                                ${decade eq model.decade ? "selected" : ""} value="${decade}">${decade}</option>
                            </c:forEach>
                        </select>
                    </td>
                </c:if>
                <c:if test="${model.listType eq 'genre'}">
                    <td style="padding-left: 2em">
                        <fmt:message key="home.genre.text"/>
                    </td>
                    <td>
                        <select name="genre" onchange="location='home.view?listType=${model.listType}&amp;genre=' + encodeURIComponent(options[selectedIndex].value)">
                            <c:forEach items="${model.genres}" var="genre">
                                <option ${genre.name eq model.genre ? "selected" : ""} value="${genre.name}">${genre.name} (${genre.albumCount})</option>
                            </c:forEach>
                        </select>
                    </td>
                </c:if>
            </c:otherwise>
        </c:choose>

        <c:if test="${not empty model.albums}">
            <td style="padding-left: 2em">
                <a href="javascript:playShuffle()">
                  <img src="<spring:theme code='shuffleImage'/>" alt="Shuffle" style="height:16px;">
                  <fmt:message key="home.shuffle"/>
                </a>
            </td>
        </c:if>
    </tr>
</table>
