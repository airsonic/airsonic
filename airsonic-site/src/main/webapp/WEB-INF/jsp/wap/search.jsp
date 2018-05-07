<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE wml PUBLIC "-//WAPFORUM//DTD WML 1.1//EN" "http://www.wapforum.org/DTD/wml_1.1.xml">

<%@ page language="java" contentType="text/vnd.wap.wml; charset=utf-8" pageEncoding="iso-8859-1"%>

<wml>
    <%@ include file="head.jsp" %>
    <card id="main" title="Airsonic" newcontext="false">
        <p>
            <input name="query" value="" size="10"/>
            <anchor><fmt:message key="wap.search.title"/>
                <go href="<c:url value="/wap/searchResult.view"/>" method="get">
                    <postfield name="query" value="$query"/>
                </go>
            </anchor>
        </p>
    </card>
</wml>

