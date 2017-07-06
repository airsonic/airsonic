<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <script type="text/javascript" src="<c:url value='/dwr/util.js'/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/engine.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/interface/playlistService.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/interface/starService.js"/>"></script>
    <script type="text/javascript" language="javascript">

        var playlist;
        var songs;

        function init() {
            dwr.engine.setErrorHandler(null);
            $("#dialog-edit").dialog({resizable: true, width:400, autoOpen: false,
                buttons: {
                    "<fmt:message key="common.save"/>": function() {
                        $(this).dialog("close");
                        var name = $("#newName").val();
                        var comment = $("#newComment").val();
                        var shared = $("#newShared").is(":checked");
                        $("#name").text(name);
                        $("#comment").text(comment);
                        playlistService.updatePlaylist(playlist.id, name, comment, shared, function (playlistInfo){playlistCallback(playlistInfo); top.left.updatePlaylists()});
                    },
                    "<fmt:message key="common.cancel"/>": function() {
                        $(this).dialog("close");
                    }
                }});

            $("#dialog-delete").dialog({resizable: false, height: 170, autoOpen: false,
                buttons: {
                    "<fmt:message key="common.delete"/>": function() {
                        $(this).dialog("close");
                        playlistService.deletePlaylist(playlist.id, function (){top.left.updatePlaylists(); location = "playlists.view";});
                    },
                    "<fmt:message key="common.cancel"/>": function() {
                        $(this).dialog("close");
                    } 
                }});

            $("#playlistBody").sortable({
                stop: function(event, ui) {
                    var indexes = [];
                    $("#playlistBody").children().each(function() {
                        var id = $(this).attr("id").replace("pattern", "");
                        if (id.length > 0) {
                            indexes.push(parseInt(id) - 1);
                        }
                    });
                    onRearrange(indexes);
                },
                cursor: "move",
                axis: "y",
                containment: "parent",
                helper: function(e, tr) {
                    var originals = tr.children();
                    var trclone = tr.clone();
                    trclone.children().each(function(index) {
                        // Set cloned cell sizes to match the original sizes
                        $(this).width(originals.eq(index).width());
                        $(this).css("maxWidth", originals.eq(index).width());
                        $(this).css("border-top", "1px solid black");
                        $(this).css("border-bottom", "1px solid black");
                    });
                    return trclone;
                }
            });

            getPlaylist();
        }

        function getPlaylist() {
            playlistService.getPlaylist(${model.playlist.id}, playlistCallback);
        }

        function playlistCallback(playlistInfo) {
            this.playlist = playlistInfo.playlist;
            this.songs = playlistInfo.entries;

            if (songs.length == 0) {
                $("#empty").show();
            } else {
                $("#empty").hide();
            }

            $("#songCount").html(playlist.fileCount);
            $("#duration").html(playlist.durationAsString);

            if (playlist.shared) {
                $("#shared").html("<fmt:message key="playlist2.shared"/>");
            } else {
                $("#shared").html("<fmt:message key="playlist2.notshared"/>");
            }

            // Delete all the rows except for the "pattern" row
            dwr.util.removeAllRows("playlistBody", { filter:function(tr) {
                return (tr.id != "pattern");
            }});

            // Create a new set cloned from the pattern row
            for (var i = 0; i < songs.length; i++) {
                var song  = songs[i];
                var id = i + 1;
                dwr.util.cloneNode("pattern", { idSuffix:id });
                if (song.starred) {
                    $("#starSong" + id).attr("src", "<spring:theme code='ratingOnImage'/>");
                } else {
                    $("#starSong" + id).attr("src", "<spring:theme code='ratingOffImage'/>");
                }
                if (!song.present) {
                    $("#missing" + id).show();
                }
                $("#index" + id).html(id);
                $("#title" + id).html(song.title);
                $("#title" + id).attr("title", song.title);
                $("#album" + id).html(song.album);
                $("#album" + id).attr("title", song.album);
                $("#albumUrl" + id).attr("href", "main.view?id=" + song.id);
                $("#artist" + id).html(song.artist);
                $("#artist" + id).attr("title", song.artist);
                $("#songDuration" + id).html(song.durationAsString);

                // Note: show() method causes page to scroll to top.
                $("#pattern" + id).css("display", "table-row");
            }
        }

        function onPlay(index) {
            top.playQueue.onPlayPlaylist(playlist.id, index);
        }
        function onPlayAll() {
            top.playQueue.onPlayPlaylist(playlist.id);
        }
        function onAdd(index) {
            top.playQueue.onAdd(songs[index].id);
            $().toastmessage('showSuccessToast', '<fmt:message key="main.addlast.toast"/>')
        }
        function onAddNext(index) {
            top.playQueue.onAddNext(songs[index].id);
            $().toastmessage('showSuccessToast', '<fmt:message key="main.addnext.toast"/>')
        }
        function onStar(index) {
            playlistService.toggleStar(playlist.id, index, playlistCallback);
        }
        function onRemove(index) {
            playlistService.remove(playlist.id, index, function (playlistInfo){playlistCallback(playlistInfo); top.left.updatePlaylists()});
        }
        function onRearrange(indexes) {
            playlistService.rearrange(playlist.id, indexes, playlistCallback);
        }
        function onEditPlaylist() {
            $("#dialog-edit").dialog("open");
        }
        function onDeletePlaylist() {
            $("#dialog-delete").dialog("open");
        }

    </script>

    <style type="text/css">
        .playlist-missing {
            color: red;
            border: 1px solid red;
            display: none;
            font-size: 90%;
            padding-left: 5px;
            padding-right: 5px;
            margin-right: 5px;
        }
    </style>

</head>
<body class="mainframe bgcolor1" onload="init()">

<div style="float:left;margin-right:1.5em;margin-bottom:1.5em">
<c:import url="coverArt.jsp">
    <c:param name="playlistId" value="${model.playlist.id}"/>
    <c:param name="coverArtSize" value="200"/>
</c:import>
</div>

<h1><a href="playlists.view"><fmt:message key="left.playlists"/></a> &raquo; <span id="name">${fn:escapeXml(model.playlist.name)}</span></h1>
<h2>
    <span class="header"><a href="javascript:void(0)" onclick="onPlayAll();"><fmt:message key="common.play"/></a></span>

    <c:if test="${model.user.downloadRole}">
        <c:url value="download.view" var="downloadUrl"><c:param name="playlist" value="${model.playlist.id}"/></c:url>
        | <span class="header"><a href="${downloadUrl}"><fmt:message key="common.download"/></a></span>
    </c:if>
    <c:if test="${model.user.shareRole}">
        <c:url value="createShare.view" var="shareUrl"><c:param name="playlist" value="${model.playlist.id}"/></c:url>
        | <span class="header"><a href="${shareUrl}"><fmt:message key="share.title"/></a></span>
    </c:if>
    <c:if test="${model.editAllowed}">
        | <span class="header"><a href="javascript:void(0)" onclick="onEditPlaylist();"><fmt:message key="common.edit"/></a></span>
        | <span class="header"><a href="javascript:void(0)" onclick="onDeletePlaylist();"><fmt:message key="common.delete"/></a></span>
    </c:if>
    <c:url value="exportPlaylist.view" var="exportUrl"><c:param name="id" value="${model.playlist.id}"/></c:url>
    | <span class="header"><a href="${exportUrl}"><fmt:message key="playlist2.export"/></a></span>

</h2>

<div id="comment" class="detail" style="padding-top:0.2em">${fn:escapeXml(model.playlist.comment)}</div>

<div class="detail" style="padding-top:0.2em">
    <span id="songCount"></span> <fmt:message key="playlist2.songs"/> &ndash; <span id="duration"></span>
</div>
<div class="detail" style="padding-top:0.2em">
    <fmt:message key="playlist2.created" var="created">
        <fmt:param>${model.playlist.username}</fmt:param>
        <fmt:param><fmt:formatDate type="date" dateStyle="long" value="${model.playlist.created}"/></fmt:param>
    </fmt:message>
    ${fn:escapeXml(created)}.
</div>
<div class="detail" style="padding-top:0.2em">
    <span id="shared"></span>.
</div>

<div style="height:0.7em;clear:both"></div>

<p id="empty" style="display: none;"><em><fmt:message key="playlist2.empty"/></em></p>

<table class="music" style="cursor:pointer">
    <tbody id="playlistBody">
    <tr id="pattern" style="display:none;margin:0;padding:0;border:0">
        <td class="fit">
            <img id="starSong" onclick="onStar(this.id.substring(8) - 1)" src="<spring:theme code="ratingOffImage"/>"
                 style="cursor:pointer" alt="" title=""></td>
        <td class="fit">
            <img id="play" src="<spring:theme code="playImage"/>" alt="<fmt:message key="common.play"/>" title="<fmt:message key="common.play"/>"
                 style="padding-right:0.1em;cursor:pointer" onclick="onPlay(this.id.substring(4) - 1)"></td>
        <td class="fit">
            <img id="add" src="<spring:theme code="addImage"/>" alt="<fmt:message key="common.add"/>" title="<fmt:message key="common.add"/>"
                 style="padding-right:0.1em;cursor:pointer" onclick="onAdd(this.id.substring(3) - 1)"></td>
        <td class="fit" style="padding-right:30px">
            <img id="addNext" src="<spring:theme code="addNextImage"/>" alt="<fmt:message key="main.addnext"/>" title="<fmt:message key="main.addnext"/>"
                 style="padding-right:0.1em;cursor:pointer" onclick="onAddNext(this.id.substring(7) - 1)"></td>

        <td class="fit rightalign"><span id="index">1</span></td>
        <td class="fit"><span id="missing" class="playlist-missing"><fmt:message key="playlist.missing"/></span></td>
        <td class="truncate"><span id="title" class="songTitle">Title</span></td>
        <td class="truncate"><a id="albumUrl" target="main"><span id="album" class="detail">Album</span></a></td>
        <td class="truncate"><span id="artist" class="detail">Artist</span></td>
        <td class="fit rightalign"><span id="songDuration" class="detail">Duration</span></td>

        <c:if test="${model.editAllowed}">
            <td class="fit">
                <img id="removeSong" onclick="onRemove(this.id.substring(10) - 1)" src="<spring:theme code="removeImage"/>"
                     style="cursor:pointer" alt="<fmt:message key="playlist.remove"/>" title="<fmt:message key="playlist.remove"/>"></td>
        </c:if>
    </tr>
    </tbody>
</table>

<div id="dialog-delete" title="<fmt:message key="common.confirm"/>" style="display: none;">
    <p><span class="ui-icon ui-icon-alert" style="float:left; margin:0 7px 20px 0;"></span>
        <fmt:message key="playlist2.confirmdelete"/></p>
</div>

<div id="dialog-edit" title="<fmt:message key="common.edit"/>" style="display: none;">
    <form>
        <label for="newName" style="display:block;"><fmt:message key="playlist2.name"/></label>
        <input type="text" name="newName" id="newName" value="${fn:escapeXml(model.playlist.name)}" class="ui-widget-content"
               style="display:block;width:95%;"/>
        <label for="newComment" style="display:block;margin-top:1em"><fmt:message key="playlist2.comment"/></label>
        <input type="text" name="newComment" id="newComment" value="${fn:escapeXml(model.playlist.comment)}" class="ui-widget-content"
               style="display:block;width:95%;"/>
        <input type="checkbox" name="newShared" id="newShared" ${model.playlist.shared ? "checked='checked'" : ""} style="margin-top:1.5em" class="ui-widget-content"/>
        <label for="newShared"><fmt:message key="playlist2.public"/></label>
    </form>
</div>

</body></html>
