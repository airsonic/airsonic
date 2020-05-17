var entityMap = {
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': '&quot;',
    "'": '&#39;',
    "/": '&#x2F;'
};

function escapeHtml(string) {
    return String(string).replace(/[&<>"'\/]/g, function (s) {
        return entityMap[s];
    });
}

function popup(mylink, windowname) {
    return popupSize(mylink, windowname, 400, 200);
}

function popupSize(mylink, windowname, width, height) {
    var href;
    if (typeof(mylink) == "string") {
        href = mylink;
    } else {
        href = mylink.href;
    }

    var w = window.open(href, windowname, "width=" + width + ",height=" + height + ",scrollbars=yes,resizable=yes");
    w.focus();
    w.moveTo(300, 200);
    return false;
}

function updateQueryStringParameter(uri, key, value) {
    var re = new RegExp("([?&])" + key + "=.*?(&|$)", "i");
    if (uri.match(re)) {
        return uri.replace(re, '$1' + key + "=" + value + '$2');
    } else {
        var separator = uri.indexOf('?') !== -1 ? "&" : "?";
        return uri + separator + key + "=" + value;
    }
}

function getJQueryUiDialogPlaylistSize(origin) {
    var width = window.localStorage.getItem("dialog-select-playlist-" + origin + "-width");
    var height = window.localStorage.getItem("dialog-select-playlist-" + origin + "-height");
    if (!width) {
        width = 300;
    }
    if (!height) {
        height = 250;
    }
    return {width: width, height: height};
}

function setJQueryUiDialogPlaylistSize(origin, size) {
    window.localStorage.setItem("dialog-select-playlist-" + origin + "-width", parseInt(size.width));
    window.localStorage.setItem("dialog-select-playlist-" + origin + "-height", parseInt(size.height));
}
