<%--
  ~ This file is part of Airsonic.
  ~
  ~  Airsonic is free software: you can redistribute it and/or modify
  ~  it under the terms of the GNU General Public License as published by
  ~  the Free Software Foundation, either version 3 of the License, or
  ~  (at your option) any later version.
  ~
  ~  Airsonic is distributed in the hope that it will be useful,
  ~  but WITHOUT ANY WARRANTY; without even the implied warranty of
  ~  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  ~  GNU General Public License for more details.
  ~
  ~  You should have received a copy of the GNU General Public License
  ~  along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.
  ~
  ~  Copyright 2015 (C) Sindre Mehus
  --%>

<%@ page contentType="text/html; charset=utf-8" pageEncoding="utf-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<div style="float:right;padding-right:1em">
    <c:url value="main.view" var="changeViewUrl">
        <c:param name="id" value="${model.dir.id}"/>
        <c:param name="viewAsList" value="${not model.viewAsList}"/>
    </c:url>
    <c:choose>
        <c:when test="${model.viewAsList}">
            <img src="<spring:theme code='viewAsListImage'/>" alt="" class="headerSelected" style="margin-right:8px"/>
            <a href="${changeViewUrl}"><img src="<spring:theme code='viewAsGridImage'/>" alt=""/></a>
        </c:when>
        <c:otherwise>
            <a href="${changeViewUrl}"><img src="<spring:theme code='viewAsListImage'/>" alt="" style="margin-right:8px"/></a>
            <img src="<spring:theme code='viewAsGridImage'/>" alt="" class="headerSelected"/>
        </c:otherwise>
    </c:choose>
</div>

