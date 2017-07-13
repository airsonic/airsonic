<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE wml PUBLIC "-//WAPFORUM//DTD WML 1.1//EN" "http://www.wapforum.org/DTD/wml_1.1.xml">

<%@ page language="java" contentType="text/vnd.wap.wml; charset=utf-8" pageEncoding="iso-8859-1"%>

<wml>

    <%@ include file="head.jsp" %>
    <card id="main" title="Airsonic" newcontext="false">
        <p><small>

            <c:choose>
                <c:when test="${model.creatingIndex}">
                    <fmt:message key="wap.searchresult.index"/>
                </c:when>

                <c:otherwise>
                    <c:forEach items="${model.hits}" var="hit">
                        <sub:url var="url" value="/wap/browse.view">
                            <sub:param name="path" value="${hit.path}"/>
                        </sub:url>
                        <a href="${url}">${fn:escapeXml(hit.title)}</a><br/>
                    </c:forEach>
                </c:otherwise>
            </c:choose>
        </small></p>
    </card>

</wml>

