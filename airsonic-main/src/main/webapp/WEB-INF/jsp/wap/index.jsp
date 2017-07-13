<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE wml PUBLIC "-//WAPFORUM//DTD WML 1.1//EN" "http://www.wapforum.org/DTD/wml_1.1.xml">

<%@ page language="java" contentType="text/vnd.wap.wml; charset=utf-8" pageEncoding="iso-8859-1" %>

<wml>

    <%@ include file="head.jsp" %>

    <card id="main" title="Airsonic" newcontext="false">
        <p>
            <small>

                <c:choose>
                <c:when test="${empty model.artists}">

                <b>
                    <a href="<c:url value="/wap/playlist.view"/>">[<fmt:message key="wap.index.playlist"/>]</a>
                </b>
                <br/>
                <b>
                    <a href="<c:url value="/wap/search.view"/>">[<fmt:message key="wap.index.search"/>]</a>
                </b>
                <br/>
                <b>
                    <a href="<c:url value="/wap/settings.view"/>">[<fmt:message key="wap.index.settings"/>]</a>
                </b>
                <br/>
            </small>
        </p>
        <p>
            <small>
                <c:forEach items="${model.indexes}" var="index">
                    <sub:url var="url" value="/wap/index.view">
                        <sub:param name="index" value="${index.index}"/>
                    </sub:url>
                    <a href="${url}">${index.index}</a>
                </c:forEach>
                </c:when>

                <c:otherwise>
                    <c:forEach items="${model.artists}" var="artist">
                        <c:forEach items="${artist.musicFiles}" var="mediaFile">
                            <sub:url var="url" value="/wap/browse.view">
                                <sub:param name="path" value="${mediaFile.path}"/>
                            </sub:url>
                            <a href="${url}">${fn:escapeXml(mediaFile.title)}</a>
                            <br/>
                        </c:forEach>
                    </c:forEach>
                </c:otherwise>
                </c:choose>

                <c:if test="${model.noMusic}">
                    <fmt:message key="wap.index.missing"/>
                </c:if>

            </small>
        </p>
    </card>
</wml>

