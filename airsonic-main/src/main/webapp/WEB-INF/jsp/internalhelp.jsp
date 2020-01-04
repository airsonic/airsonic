<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html><head>
    <%@ include file="head.jsp" %>
    <script type="text/javascript" src="<c:url value='/script/utils.js'/>"></script>
</head>
<body class="mainframe bgcolor1">

<c:choose>
    <c:when test="${empty model.buildDate}">
        <fmt:message key="common.unknown" var="buildDateString"/>
    </c:when>
    <c:otherwise>
        <fmt:formatDate value="${model.buildDate}" dateStyle="long" var="buildDateString"/>
    </c:otherwise>
</c:choose>

<c:choose>
    <c:when test="${empty model.localVersion}">
        <fmt:message key="common.unknown" var="versionString"/>
    </c:when>
    <c:otherwise>
        <c:set var="versionString" value="${model.localVersion}"/>
    </c:otherwise>
</c:choose>

<h1>
    <img src="<spring:theme code='helpImage'/>" alt="">
    <span style="vertical-align: middle"><fmt:message key="internalhelp.title"><fmt:param value="${model.brand}"/></fmt:message></span>
</h1>

<c:if test="${model.newVersionAvailable}">
    <p class="warning"><fmt:message key="help.upgrade"><fmt:param value="${model.brand}"/><fmt:param value="${model.latestVersion}"/></fmt:message></p>
</c:if>

<table width="75%" class="ruleTable indent">

    <tr><td class="ruleTableHeader"><fmt:message key="help.version.title"/></td><td class="ruleTableCell">${versionString} &ndash; ${buildDateString}</td></tr>
    <tr><td class="ruleTableHeader"><fmt:message key="help.server.title"/></td><td class="ruleTableCell">${model.serverInfo} (<sub:formatBytes bytes="${model.usedMemory}"/> / <sub:formatBytes bytes="${model.totalMemory}"/>)</td></tr>
    <tr><td class="ruleTableHeader"><fmt:message key="help.license.title"/></td><td class="ruleTableCell">
        <a href="http://www.gnu.org/copyleft/gpl.html" target="_blank"><img style="float:right;margin-left: 10px" alt="GPL 3.0" src="<c:url value='/icons/default_light/gpl.png'/>"></a>
        <fmt:message key="help.license.text"><fmt:param value="${model.brand}"/></fmt:message></td></tr>
    <tr><td class="ruleTableHeader"><fmt:message key="help.homepage.title"/></td><td class="ruleTableCell"><a target="_blank" href="https://airsonic.github.io/" rel="noopener nofererrer">Airsonic website</a></td></tr>
    <tr><td class="ruleTableHeader"><fmt:message key="help.forum.title"/></td><td class="ruleTableCell"><a target="_blank" href="https://www.reddit.com/r/airsonic" rel="noopener noreferrer">Airsonic on Reddit</a></td></tr>
    <tr><td class="ruleTableHeader"><fmt:message key="help.contact.title"/></td><td class="ruleTableCell"><fmt:message key="help.contact.text"><fmt:param value="${model.brand}"/></fmt:message></td></tr>
</table>

<p></p>

<h2>
    <img src="<spring:theme code='logImage'/>" alt="">
    <span style="vertical-align: middle"><fmt:message key="internalhelp.statistics"/></span>
</h2>

<table width="75%" class="ruleTable indent">
    <tr><td class="ruleTableHeader">statAlbumCount</td><td class="ruleTableCell">${model.statAlbumCount}</td></tr>
    <tr><td class="ruleTableHeader">statArtistCount</td><td class="ruleTableCell">${model.statArtistCount}</td></tr>
    <tr><td class="ruleTableHeader">statSongCount</td><td class="ruleTableCell">${model.statSongCount}</td></tr>
    <tr><td class="ruleTableHeader">statLastScanDate</td><td class="ruleTableCell">${model.statLastScanDate}</td></tr>
    <tr><td class="ruleTableHeader">statTotalDurationSeconds</td><td class="ruleTableCell">${model.statTotalDurationSeconds}</td></tr>
    <tr><td class="ruleTableHeader">statTotalLengthBytes</td><td class="ruleTableCell">${model.statTotalLengthBytes}</td></tr>
</table>

<p></p>

<h2>
    <img src="<spring:theme code='logImage'/>" alt="">
    <span style="vertical-align: middle"><fmt:message key="internalhelp.index"/></span>
</h2>

<table width="75%" class="ruleTable indent">
    <tr><td class="ruleTableHeader">indexLuceneVersion</td><td class="ruleTableCell">${model.indexLuceneVersion}</td></tr>


    <tr><td class="ruleTableHeader">indexSongDeletedCount</td><td class="ruleTableCell">${model.indexSongDeletedCount}</td></tr>
    <tr><td class="ruleTableHeader">indexAlbumDeletedCount</td><td class="ruleTableCell">${model.indexAlbumDeletedCount}</td></tr>
    <tr><td class="ruleTableHeader">indexArtistDeletedCount</td><td class="ruleTableCell">${model.indexArtistDeletedCount}</td></tr>
    <tr><td class="ruleTableHeader">indexAlbumId3DeletedCount</td><td class="ruleTableCell">${model.indexAlbumId3DeletedCount}</td></tr>
    <tr><td class="ruleTableHeader">indexArtistId3DeletedCount</td><td class="ruleTableCell">${model.indexArtistId3DeletedCount}</td></tr>

    <tr><td class="ruleTableHeader">indexSongCount</td><td class="ruleTableCell">${model.indexSongCount}</td></tr>
    <tr><td class="ruleTableHeader">indexAlbumCount</td><td class="ruleTableCell">${model.indexAlbumCount}</td></tr>
    <tr><td class="ruleTableHeader">indexArtistCount</td><td class="ruleTableCell">${model.indexArtistCount}</td></tr>
    <tr><td class="ruleTableHeader">indexAlbumId3Count</td><td class="ruleTableCell">${model.indexAlbumId3Count}</td></tr>
    <tr><td class="ruleTableHeader">indexArtistId3Count</td><td class="ruleTableCell">${model.indexArtistId3Count}</td></tr>
</table>

<p></p>

<h2>
    <img src="<spring:theme code='logImage'/>" alt="">
    <span style="vertical-align: middle"><fmt:message key="internalhelp.database"/></span>
</h2>

<table width="75%" class="ruleTable indent">

    <tr>
        <td colspan="2" class="ruleTableCell">
            <c:choose>
                <c:when test="${model.dbLogSizeBytes < 268435456}">
                    <img src="<spring:theme code='checkImage'/>" alt="OK">
                    The database log file (db/airsonic.log) appears healthy.
                </c:when>
                <c:otherwise>
                    <img src="<spring:theme code='alertImage'/>" alt="Warning">
                    The database log file (db/airsonic.log) is large (greater than 256M). Run a scan to clean it up.
                </c:otherwise>
            </c:choose>
        </td>
    </tr>

    <tr><td class="ruleTableHeader">dbDriverName</td><td class="ruleTableCell">${model.dbDriverName}</td></tr>
    <tr><td class="ruleTableHeader">dbDriverVersion</td><td class="ruleTableCell">${model.dbDriverVersion}</td></tr>
    <tr><td class="ruleTableHeader">dbDirectorySize</td><td class="ruleTableCell">${model.dbDirectorySize}</td></tr>
    <tr><td class="ruleTableHeader">dbLogSize</td><td class="ruleTableCell">${model.dbLogSize}</td></tr>

    <tr>
        <td colspan="2" class="ruleTableCell">
            <c:choose>
                <c:when test="${model.dbMediaFileAlbumNonPresentCount + model.dbMediaFileDirectoryNonPresentCount + model.dbMediaFileMusicNonPresentCount + model.dbMediaFilePodcastNonPresentCount == 0}">
                    <img src="<spring:theme code='checkImage'/>" alt="OK">
                    The database does not contain non-present items.
                </c:when>
                <c:otherwise>
                    <img src="<spring:theme code='alertImage'/>" alt="Warning">
                    The database contains non-present items. Run "clean-up database" to clean them up.
                </c:otherwise>
            </c:choose>
        </td>
    </tr>

    <tr><td class="ruleTableHeader">dbMediaFileMusicNonPresentCount</td><td class="ruleTableCell">${model.dbMediaFileMusicNonPresentCount}</td></tr>
    <tr><td class="ruleTableHeader">dbMediaFilePodcastNonPresentCount</td><td class="ruleTableCell">${model.dbMediaFilePodcastNonPresentCount}</td></tr>
    <tr><td class="ruleTableHeader">dbMediaFileDirectoryNonPresentCount</td><td class="ruleTableCell">${model.dbMediaFileDirectoryNonPresentCount}</td></tr>
    <tr><td class="ruleTableHeader">dbMediaFileAlbumNonPresentCount</td><td class="ruleTableCell">${model.dbMediaFileAlbumNonPresentCount}</td></tr>

    <tr><td class="ruleTableHeader">dbMediaFileMusicPresentCount</td><td class="ruleTableCell">${model.dbMediaFileMusicPresentCount}</td></tr>
    <tr><td class="ruleTableHeader">dbMediaFilePodcastPresentCount</td><td class="ruleTableCell">${model.dbMediaFilePodcastPresentCount}</td></tr>
    <tr><td class="ruleTableHeader">dbMediaFileDirectoryPresentCount</td><td class="ruleTableCell">${model.dbMediaFileDirectoryPresentCount}</td></tr>
    <tr><td class="ruleTableHeader">dbMediaFileAlbumPresentCount</td><td class="ruleTableCell">${model.dbMediaFileAlbumPresentCount}</td></tr>

    <tr><td class="ruleTableHeader">dbMediaFileDistinctAlbumCount</td><td class="ruleTableCell">${model.dbMediaFileDistinctAlbumCount}</td></tr>
    <tr><td class="ruleTableHeader">dbMediaFileDistinctArtistCount</td><td class="ruleTableCell">${model.dbMediaFileDistinctArtistCount}</td></tr>
    <tr><td class="ruleTableHeader">dbMediaFileDistinctAlbumArtistCount</td><td class="ruleTableCell">${model.dbMediaFileDistinctAlbumArtistCount}</td></tr>

    <c:forEach var="tableCount" items="${model.dbTableCount}">
        <tr><td class="ruleTableHeader">${tableCount.key} count</td><td class="ruleTableCell">${tableCount.value}</td></tr>
    </c:forEach>
</table>

<p></p>

<h2>
    <img src="<spring:theme code='logImage'/>" alt="">
    <span style="vertical-align: middle"><fmt:message key="internalhelp.filesystem"/></span>
</h2>

<table width="75%" class="ruleTable indent">
    <tr><td class="ruleTableHeader"><fmt:message key="internalhelp.fsusage"/></td><td class="ruleTableCell">${model.fsHomeUsableSpace} / ${model.fsHomeTotalSpace}</td></tr>
    <tr><td class="ruleTableHeader"><fmt:message key="internalhelp.fshomesize"/></td><td class="ruleTableCell">${model.fsHomeDirectorySize}</td></tr>
    <c:forEach var="musicFolderStatistics" items="${model.fsMusicFolderStatistics}">
        <tr>
            <td colspan="2" class="ruleTableCell">
                <c:choose>
                    <c:when test="${musicFolderStatistics.value.readable}">
                        <img src="<spring:theme code='checkImage'/>" alt="OK">
                        Airsonic appears to have the correct permissions for music folder "${musicFolderStatistics.key}".
                    </c:when>
                    <c:otherwise>
                        <img src="<spring:theme code='alertImage'/>" alt="Warning">
                        Airsonic does not have the correct permissions for music folder "${musicFolderStatistics.key}".
                    </c:otherwise>
                </c:choose>
            </td>
        </tr>
        <tr><td class="ruleTableHeader"><fmt:message key="internalhelp.folderisreadable"><fmt:param value="${musicFolderStatistics.key}"/></fmt:message></td><td class="ruleTableCell">${musicFolderStatistics.value.readable}</td></tr>
        <tr><td class="ruleTableHeader"><fmt:message key="internalhelp.folderiswritable"><fmt:param value="${musicFolderStatistics.key}"/></fmt:message></td><td class="ruleTableCell">${musicFolderStatistics.value.writable}</td></tr>
        <tr><td class="ruleTableHeader"><fmt:message key="internalhelp.folderfsusage"><fmt:param value="${musicFolderStatistics.key}"/></fmt:message></td><td class="ruleTableCell">${musicFolderStatistics.value.freeFilesystemSizeBytes} / ${musicFolderStatistics.value.totalFilesystemSizeBytes}</td></tr>
    </c:forEach>
</table>

<p></p>

<h2>
    <img src="<spring:theme code='logImage'/>" alt="">
    <span style="vertical-align: middle"><fmt:message key="internalhelp.locale"/></span>
</h2>

<table width="75%" class="ruleTable indent">
    <tr>
        <td colspan="2" class="ruleTableCell">
            <c:choose>
                <c:when test="${fn:contains(model.localeDefaultCharset, 'UTF-8')}">
                    <img src="<spring:theme code='checkImage'/>" alt="OK">
                    Java default charset appears to have UTF-8 support.
                </c:when>
                <c:otherwise>
                    <img src="<spring:theme code='alertImage'/>" alt="Warning">
                    Java default charset appears to have no UTF-8 support. International characters may be partially supported.
                </c:otherwise>
            </c:choose>
        </td>
    </tr>
    <tr><td class="ruleTableHeader"><fmt:message key="internalhelp.defaultcharset"/></td><td class="ruleTableCell">${model.localeDefaultCharset}</td></tr>
    <tr><td class="ruleTableHeader"><fmt:message key="internalhelp.defaultlocale"/></td><td class="ruleTableCell">${model.localeDefault}</td></tr>
    <tr><td class="ruleTableHeader">user.language</td><td class="ruleTableCell">${model.localeUserLanguage}</td></tr>
    <tr><td class="ruleTableHeader">user.country</td><td class="ruleTableCell">${model.localeUserCountry}</td></tr>
    <tr>
        <td colspan="2" class="ruleTableCell">
            <c:choose>
                <c:when test="${fn:contains(model.localeFileEncoding, 'UTF-8')}">
                    <img src="<spring:theme code='checkImage'/>" alt="OK">
                    Java file encoding appears to have UTF-8 support.
                </c:when>
                <c:otherwise>
                    <img src="<spring:theme code='alertImage'/>" alt="Warning">
                    Java file encoding appears to have no UTF-8 support. International characters may be partially supported.
                </c:otherwise>
            </c:choose>
        </td>
    </tr>
    <tr><td class="ruleTableHeader">file.encoding</td><td class="ruleTableCell">${model.localeFileEncoding}</td></tr>
    <tr><td class="ruleTableHeader">sun.jnu.encoding</td><td class="ruleTableCell">${model.localeSunJnuEncoding}</td></tr>
    <tr><td class="ruleTableHeader">sun.io.unicode.encoding</td><td class="ruleTableCell">${model.localeSunIoUnicodeEncoding}</td></tr>

    <c:if test="${not empty model.localeLang and not fn:contains(model.localeLang, 'UTF-8')}">
        <tr>
            <td colspan="2" class="ruleTableCell">
                <img src="<spring:theme code='alertImage'/>" alt="Warning">
                The LANG environment variable is defined but appears to disable UTF-8 support. International characters may be partially supported.
            </td>
        </tr>
    </c:if>
    <tr><td class="ruleTableHeader">LANG</td><td class="ruleTableCell">${model.localeLang}</td></tr>

    <c:if test="${not empty model.localeLcAll and not fn:contains(model.localeLcAll, 'UTF-8')}">
        <tr>
            <td colspan="2" class="ruleTableCell">
                <img src="<spring:theme code='alertImage'/>" alt="Warning">
                The LC_ALL environment variable is defined but appears to disable UTF-8 support. International characters may be partially supported.
            </td>
        </tr>
    </c:if>
    <tr><td class="ruleTableHeader">LC_ALL</td><td class="ruleTableCell">${model.localeLcAll}</td></tr>
</table>

<p></p>

<h2>
    <img src="<spring:theme code='logImage'/>" alt="">
    <span style="vertical-align: middle"><fmt:message key="help.log"/></span>
</h2>

<table cellpadding="2" class="log indent">
    <c:forEach items="${model.logEntries}" var="entry">
        <tr>
            <td>${fn:escapeXml(entry)}</td>
        </tr>
    </c:forEach>
</table>

<p><fmt:message key="help.logfile"><fmt:param value="${model.logFile}"/></fmt:message> </p>

<div class="forward"><a href="internalhelp.view?"><fmt:message key="common.refresh"/></a></div>

</body></html>
