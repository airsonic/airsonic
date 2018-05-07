<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>

<html>
<head>
    <%@ include file="../include.jsp" %>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <link rel="stylesheet" href="../<spring:theme code="styleSheet"/>" type="text/css">

    <c:url value="/rest/stream.view" var="streamUrl">
        <c:param name="c" value="${model.c}"/>
        <c:param name="v" value="${model.v}"/>
        <c:param name="id" value="${model.id}"/>
    </c:url>

    <script type="text/javascript" src="<c:url value="/script/swfobject.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/script/prototype.js"/>"></script>
    <script type="text/javascript" language="javascript">

        var player;
        var position;
        var maxBitRate = ${model.maxBitRate};
        var timeOffset = ${model.timeOffset};

        function init() {
            var flashvars = {
                id:"player1",
                skin:"<c:url value="/flash/whotube.zip"/>",
                screencolor:"000000",
                autostart:false,
                bufferlength:4,
                backcolor:"<spring:theme code="backgroundColor"/>",
                frontcolor:"<spring:theme code="textColor"/>",
                provider:"video"
            };
            var params = {
                allowfullscreen:"true",
                allowscriptaccess:"always"
            };
            var attributes = {
                id:"player1",
                name:"player1"
            };

            swfobject.embedSWF("<c:url value="/flash/jw-player-5.10.swf"/>", "placeholder1", "360", "240", "9.0.0", false, flashvars, params, attributes);
        }

        function playerReady(thePlayer) {
            player = $("player1");
            player.addModelListener("TIME", "timeListener");

        <c:if test="${model.autoplay}">
            play();
        </c:if>
        }

        function play() {
            var list = new Array();
            list[0] = {
                file:"${streamUrl}&maxBitRate=" + maxBitRate + "&timeOffset=" + timeOffset + "&p=${model.p}" + "&u=${model.u}",
                duration:${model.duration} - timeOffset,
                provider:"video"
            };
            player.sendEvent("LOAD", list);
            player.sendEvent("PLAY");
        }

        function timeListener(obj) {
            var newPosition = Math.round(obj.position);
            if (newPosition != position) {
                position = newPosition;
                updatePosition();
            }
        }

        function updatePosition() {
            var pos = parseInt(timeOffset) + parseInt(position);

            var minutes = Math.round(pos / 60);
            var seconds = pos % 60;

            var result = minutes + ":";
            if (seconds < 10) {
                result += "0";
            }
            result += seconds;
            $("position").innerHTML = result;
        }

        function changeTimeOffset() {
            timeOffset = $("timeOffset").getValue();
            play();
        }

        function changeBitRate() {
            maxBitRate = $("maxBitRate").getValue();
            timeOffset = parseInt(timeOffset) + parseInt(position);
            play();
        }

    </script>
</head>

<body class="mainframe bgcolor1" onload="init();">
<h1>${model.video.title}</h1>

<div id="wrapper" style="padding-top:1em">
    <div id="placeholder1"><span class="warning"><a href="http://www.adobe.com/go/getflashplayer"><fmt:message key="playlist.getflash"/></a></span></div>
</div>

<div style="padding-top:1.3em;padding-bottom:0.7em;font-size:16px">

    <span id="progress" style="padding-right:0.5em">0:00</span>
    <select id="timeOffset" onchange="changeTimeOffset();" style="padding-left:0.25em;padding-right:0.25em;margin-right:0.5em;font-size:16px">
        <c:forEach items="${model.skipOffsets}" var="skipOffset">
            <c:choose>
                <c:when test="${skipOffset.value eq model.timeOffset}">
                    <option selected="selected" value="${skipOffset.value}">${skipOffset.key}</option>
                </c:when>
                <c:otherwise>
                    <option value="${skipOffset.value}">${skipOffset.key}</option>
                </c:otherwise>
            </c:choose>
        </c:forEach>
    </select>

    <select id="maxBitRate" onchange="changeBitRate();" style="padding-left:0.25em;padding-right:0.25em;margin-right:0.5em;font-size:16px">
        <c:forEach items="${model.bitRates}" var="bitRate">
            <c:choose>
                <c:when test="${bitRate eq model.maxBitRate}">
                    <option selected="selected" value="${bitRate}">${bitRate} Kbps</option>
                </c:when>
                <c:otherwise>
                    <option value="${bitRate}">${bitRate} Kbps</option>
                </c:otherwise>
            </c:choose>
        </c:forEach>
    </select>
</div>

</body>
</html>
