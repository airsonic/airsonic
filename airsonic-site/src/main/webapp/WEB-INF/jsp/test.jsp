<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>

<html>
<head>
    <%@ include file="head.jsp" %>
</head>
<body style="padding-top: 10em">


<audio id="demo" autoplay preload="none" controls src="http://localhost:4040/stream?id=1009&maxBitRate=64&foo=3bar"></audio>
<%--<audio id="demo" autoplay preload="none" controls src="http://localhost:4040/stream?id=98"></audio>--%>
<div>
    <button onclick="document.getElementById('demo').play()">Play the Audio</button>
    <button onclick="document.getElementById('demo').pause()">Pause the Audio</button>
    <button onclick="document.getElementById('demo').volume+=0.1">Increase Volume</button>
    <button onclick="document.getElementById('demo').volume-=0.1">Decrease Volume</button>
</div>

<script>
    var a = document.getElementById("demo");
    a.addEventListener("seeked", function() { console.log("seeked") }, true);
//    a.addEventListener("timeupdate", function() { console.log("timeupdate " + a.currentTime) }, true);
    a.addEventListener("progress", function() { console.log("progress " + a.buffered.end(0)) }, true);
</script>
</body>
</html>