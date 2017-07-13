<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE wml PUBLIC "-//WAPFORUM//DTD WML 1.1//EN" "http://www.wapforum.org/DTD/wml_1.1.xml">

<%@ page language="java" contentType="text/vnd.wap.wml; charset=utf-8" pageEncoding="iso-8859-1"%>

<wml>

    <%@ include file="head.jsp" %>
    <card id="main" title="Airsonic" newcontext="false">
        <p><small>
            <b><a href="<c:url value="/wap/index.view"/>">[<fmt:message key="common.home"/>]</a><br/></b>
            <b><a href="#player">[<fmt:message key="wap.settings.selectplayer"/>]</a></b>
        </small></p>
    </card>

    <card id="player" title="Airsonic" newcontext="false">
        <p><small>

            <b><a href="<c:url value="/wap/index.view"/>">[<fmt:message key="common.home"/>]</a><br/></b>
        </small></p><p><small>

        <c:choose>
            <c:when test="${empty model.playerId}">
                <fmt:message key="wap.settings.allplayers"/>
            </c:when>
            <c:otherwise>
                <a href="<c:url value="/wap/selectPlayer.view"/>"><fmt:message key="wap.settings.allplayers"/></a>
            </c:otherwise>
        </c:choose>
        <br/>

        <c:forEach items="${model.players}" var="player">
            <c:choose>
                <c:when test="${player.id eq model.playerId}">
                    ${player}
                </c:when>
                <c:otherwise>
                    <a href="<c:url value="/wap/selectPlayer.view?playerId=${player.id}"/>">${player}</a>
                </c:otherwise>
            </c:choose>
            <br/>
        </c:forEach>
    </small></p>
    </card>

</wml>

