<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html><head>
    <%@ include file="head.jsp" %>
    <script type="text/javascript" src="<c:url value="/dwr/interface/coverArtService.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/engine.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/util.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/script/prototype.js"/>"></script>
    <script type="text/javascript" src="https://www.google.com/jsapi"></script>

    <script type="text/javascript" language="javascript">

        dwr.engine.setErrorHandler(null);
        google.load('search', '1');
        var imageSearch;

        function setImage(imageUrl) {
            $("wait").show();
            $("result").hide();
            $("success").hide();
            $("error").hide();
            $("errorDetails").hide();
            $("noImagesFound").hide();
            var id = dwr.util.getValue("id");
            coverArtService.setCoverArtImage(id, imageUrl, setImageComplete);
        }

        function setImageComplete(errorDetails) {
            $("wait").hide();
            if (errorDetails != null) {
                dwr.util.setValue("errorDetails", "<br/>" + errorDetails, { escapeHtml:false });
                $("error").show();
                $("errorDetails").show();
            } else {
                $("success").show();
            }
        }

        function searchComplete() {

            $("wait").hide();

            if (imageSearch.results && imageSearch.results.length > 0) {

                var images = $("images");
                images.innerHTML = "";

                var results = imageSearch.results;
                for (var i = 0; i < results.length; i++) {
                    var result = results[i];
                    var node = $("template").cloneNode(true);

                    // Rename results to https to avoid mixed contents.
                    result.tbUrl = result.tbUrl.replace('http://', 'https://');

                    var link = node.getElementsByClassName("search-result-link")[0];
                    link.href = "javascript:setImage('" + result.url + "');";

                    var thumbnail = node.getElementsByClassName("search-result-thumbnail")[0];
                    thumbnail.src = result.tbUrl;

                    var title = node.getElementsByClassName("search-result-title")[0];
                    title.innerHTML = result.contentNoFormatting.truncate(30);

                    var dimension = node.getElementsByClassName("search-result-dimension")[0];
                    dimension.innerHTML = result.width + " × " + result.height;

                    var url = node.getElementsByClassName("search-result-url")[0];
                    url.innerHTML = result.visibleUrl;

                    node.show();
                    images.appendChild(node);
                }

                $("result").show();

                addPaginationLinks(imageSearch);

            } else {
                $("noImagesFound").show();
            }
        }

        function addPaginationLinks() {

            // To paginate search results, use the cursor function.
            var cursor = imageSearch.cursor;
            var curPage = cursor.currentPageIndex; // check what page the app is on
            var pagesDiv = document.createElement("div");
            for (var i = 0; i < cursor.pages.length; i++) {
                var page = cursor.pages[i];
                var label;
                if (curPage == i) {
                    // If we are on the current page, then don"t make a link.
                    label = document.createElement("b");
                } else {

                    // Create links to other pages using gotoPage() on the searcher.
                    label = document.createElement("a");
                    label.href = "javascript:imageSearch.gotoPage(" + i + ");";
                }
                label.innerHTML = page.label;
                label.style.marginRight = "1em";
                pagesDiv.appendChild(label);
            }

            // Create link to next page.
            if (curPage < cursor.pages.length - 1) {
                var next = document.createElement("a");
                next.href = "javascript:imageSearch.gotoPage(" + (curPage + 1) + ");";
                next.innerHTML = "<fmt:message key="common.next"/>";
                next.style.marginLeft = "1em";
                pagesDiv.appendChild(next);
            }

            var pages = $("pages");
            pages.innerHTML = "";
            pages.appendChild(pagesDiv);
        }

        function search() {

            $("wait").show();
            $("result").hide();
            $("success").hide();
            $("error").hide();
            $("errorDetails").hide();
            $("noImagesFound").hide();

            var query = dwr.util.getValue("query");
            imageSearch.execute(query);
        }

        function onLoad() {

            imageSearch = new google.search.ImageSearch();
            imageSearch.setSearchCompleteCallback(this, searchComplete, null);
            imageSearch.setNoHtmlGeneration();
            imageSearch.setResultSetSize(8);

            google.search.Search.getBranding("branding");

            $("template").hide();

            search();
        }
        google.setOnLoadCallback(onLoad);


    </script>
</head>
<body class="mainframe bgcolor1">
<h1><fmt:message key="changecoverart.title"/></h1>
<form action="javascript:search()">
    <table class="indent"><tr>
        <td><input id="query" name="query" size="70" type="text" value="${model.artist} ${model.album}" onclick="select()"/></td>
        <td style="padding-left:0.5em"><input type="submit" value="<fmt:message key="changecoverart.search"/>"/></td>
    </tr></table>
</form>

<form action="javascript:setImage(dwr.util.getValue('url'))">
    <table><tr>
        <input id="id" type="hidden" name="id" value="${model.id}"/>
        <td><label for="url"><fmt:message key="changecoverart.address"/></label></td>
        <td style="padding-left:0.5em"><input type="text" name="url" size="50" id="url" value="http://" onclick="select()"/></td>
        <td style="padding-left:0.5em"><input type="submit" value="<fmt:message key="common.ok"/>"></td>
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

<div id="result">

    <div id="pages" style="float:left;padding-left:0.5em;padding-top:0.5em">
    </div>

    <div id="branding" style="float:right;padding-right:1em;padding-top:0.5em">
    </div>

    <div style="clear:both;">
    </div>

    <div id="images" style="width:100%;padding-bottom:2em">
    </div>

    <div style="clear:both;">
    </div>

</div>

<div id="template" style="float:left; height:190px; width:220px;padding:0.5em;position:relative">
    <div style="position:absolute;bottom:0">
        <a class="search-result-link"><img class="search-result-thumbnail" style="padding:1px; border:1px solid #021a40; background-color:white;"></a>
        <div class="search-result-title"></div>
        <div class="search-result-dimension detail"></div>
        <div class="search-result-url detail"></div>
    </div>
</div>

</body></html>