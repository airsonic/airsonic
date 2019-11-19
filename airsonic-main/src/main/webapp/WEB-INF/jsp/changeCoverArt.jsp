<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <script type="text/javascript" src="<c:url value='/dwr/interface/coverArtService.js'/>"></script>
    <script type="text/javascript" src="<c:url value='/dwr/engine.js'/>"></script>
    <script type="text/javascript" src="<c:url value='/dwr/util.js'/>"></script>

    <script type="text/javascript" language="javascript">
        dwr.engine.setErrorHandler(function() {
            $("#wait").hide();
            dwr.util.setValue("errorDetails", "Sorry, an error occurred while searching for cover art.");
            $("#errorDetails").show();
        });

        function setImage(imageUrl) {
            $("#wait").show();
            $("#result").hide();
            $("#success").hide();
            $("#error").hide();
            $("#errorDetails").hide();
            $("#noImagesFound").hide();
            coverArtService.setCoverArtImage(${model.id}, imageUrl, setImageComplete);
        }

        function setImageComplete(errorDetails) {
            $("#wait").hide();
            if (errorDetails != null) {
                dwr.util.setValue("errorDetails", errorDetails, { escapeHtml:false });
                $("#error").show();
                $("#errorDetails").show();
            } else {
                $("#success").show();
            }
        }

        function searchComplete(searchResults) {
            $("#wait").hide();

            if (searchResults.length > 0) {
                var images = $("#images");
                images.empty();

                for (var i = 0; i < searchResults.length; i++) {
                    var result = searchResults[i];
                    var node = $("#template").clone();

                    node.find(".search-result-link").attr("href", "javascript:setImage('" + result.imageUrl + "');");
                    node.find(".search-result-image").attr("src", result.imageUrl);
                    node.find(".search-result-artist").text(result.artist);
                    node.find(".search-result-album").text(result.album);

                    node.show();
                    node.appendTo(images);
                }

                $("#result").show();
            } else {
                $("#noImagesFound").show();
            }
        }

        function search() {
            $("#wait").show();
            $("#result").hide();
            $("#success").hide();
            $("#error").hide();
            $("#errorDetails").hide();
            $("#noImagesFound").hide();

            var artist = dwr.util.getValue("artist");
            var album = dwr.util.getValue("album");
            coverArtService.searchCoverArt(artist, album, searchComplete);
        }
    </script>
</head>
<body class="mainframe bgcolor1" onload="search()">
<h1><fmt:message key="changecoverart.title"/></h1>
<form action="javascript:search()">
    <sec:csrfInput />
    <table class="indent"><tr>
        <td><input id="artist" name="artist" placeholder="<fmt:message key='changecoverart.artist'/>" size="35" type="text" value="${model.artist}" onclick="select()"/></td>
        <td><input id="album" name="album" placeholder="<fmt:message key='changecoverart.album'/>" size="35" type="text" value="${model.album}" onclick="select()"/></td>
        <td style="padding-left:0.5em"><input type="submit" value="<fmt:message key='changecoverart.search'/>"/></td>
    </tr></table>
</form>

<form action="javascript:setImage(dwr.util.getValue('url'))">
    <sec:csrfInput />
    <table><tr>
        <td><label for="url"><fmt:message key="changecoverart.address"/></label></td>
        <td style="padding-left:0.5em"><input type="text" name="url" size="50" id="url" value="http://" onclick="select()"/></td>
        <td style="padding-left:0.5em"><input type="submit" value="<fmt:message key='common.ok'/>"></td>
    </tr></table>
</form>
<sub:url value="main.view" var="backUrl"><sub:param name="id" value="${model.id}"/></sub:url>
<div style="padding-top:0.5em;padding-bottom:0.5em">
    <div class="back"><a href="${backUrl}"><fmt:message key="common.back"/></a></div>
</div>

<h2 id="wait" style="display:none"><fmt:message key="changecoverart.wait"/></h2>
<h2 id="noImagesFound" style="display:none"><fmt:message key="changecoverart.noimagesfound"/></h2>
<h2 id="success" style="display:none"><fmt:message key="changecoverart.success"/></h2>
<h2 id="error" style="display:none"><fmt:message key="changecoverart.error"/></h2>
<div id="errorDetails" class="warning" style="display:none">
</div>

<div id="result" style="padding-top:2em">
    <div style="clear:both;"></div>
    <div id="images"></div>
    <div style="clear:both;"></div>
    <a href="https://last.fm/" target="_blank" rel="noopener noreferrer">
        <img alt="Lastfm icon" src="<c:url value='/icons/lastfm.gif'/>">
    </a>
    <span class="detail" style="padding-left:1em"><fmt:message key="changecoverart.courtesy"/></span>
</div>

<div id="template" class="coverart dropshadow" style="float:left;margin-right:2.0em;margin-bottom:2.0em;width:250px;display:none">
    <div>
        <a class="search-result-link">
            <img alt="Search result" class="search-result-image" style="width:250px;height:250px">
        </a>
        <div class="search-result-artist caption1"></div>
        <div class="search-result-album caption2"></div>
    </div>
</div>

</body></html>

