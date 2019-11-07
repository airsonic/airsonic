<%@ page language="java" contentType="text/xml; charset=utf-8" pageEncoding="iso-8859-1"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<rss version="2.0">
    <channel>
        <title>Airsonic Podcast</title>
        <link>${model.url}</link>
        <description>Airsonic Podcast</description>
        <language>en-us</language>
        <image>
            <url>http://www.airsonic.org/pages/inc/img/airsonic_logo.png</url>
            <title>Airsonic Podcast</title>
        </image>

        <c:forEach var="podcast" items="${model.podcasts}">
            <item>
                <title>${fn:escapeXml(podcast.name)}</title>
                <link>${model.url}</link>
                <description>Airsonic playlist "${fn:escapeXml(podcast.name)}"</description>
                <pubDate>${podcast.publishDate}</pubDate>
                <enclosure url="${podcast.enclosureUrl}" length="${podcast.length}" type="${podcast.type}"/>
            </item>
        </c:forEach>

    </channel>
</rss>
