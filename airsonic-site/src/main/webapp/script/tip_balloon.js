/*
tip_balloon.js  v. 1.81

The latest version is available at
http://www.walterzorn.com
or http://www.devira.com
or http://www.walterzorn.de

Initial author: Walter Zorn
Last modified: 2.2.2009

Extension for the tooltip library wz_tooltip.js.
Implements balloon tooltips.
*/

// Make sure that the core file wz_tooltip.js is included first
if(typeof config == "undefined")
	alert("Error:\nThe core tooltip script file 'wz_tooltip.js' must be included first, before the plugin files!");

// Here we define new global configuration variable(s) (as members of the
// predefined "config." class).
// From each of these config variables, wz_tooltip.js will automatically derive
// a command which can be passed to Tip() or TagToTip() in order to customize
// tooltips individually. These command names are just the config variable
// name(s) translated to uppercase,
// e.g. from config. Balloon a command BALLOON will automatically be
// created.

//===================  GLOBAL TOOLTIP CONFIGURATION  =========================//
config. Balloon				= false	// true or false - set to true if you want this to be the default behaviour
config. BalloonImgPath		= "script/tip_balloon/" // Path to images (border, corners, stem), in quotes. Path must be relative to your HTML file.
// Sizes of balloon images
config. BalloonEdgeSize		= 6		// Integer - sidelength of quadratic corner images
config. BalloonStemWidth	= 15	// Integer
config. BalloonStemHeight	= 19	// Integer
config. BalloonStemOffset	= -7	// Integer - horizontal offset of left stem edge from mouse (recommended: -stemwidth/2 to center the stem above the mouse)
config. BalloonImgExt		= "gif";// File name extension of default balloon images, e.g. "gif" or "png"
//=======  END OF TOOLTIP CONFIG, DO NOT CHANGE ANYTHING BELOW  ==============//


// Create a new tt_Extension object (make sure that the name of that object,
// here balloon, is unique amongst the extensions available for wz_tooltips.js):
var balloon = new tt_Extension();

// Implement extension eventhandlers on which our extension should react

balloon.OnLoadConfig = function()
{
	if(tt_aV[BALLOON])
	{
		// Turn off native style properties which are not appropriate
		balloon.padding = Math.max(tt_aV[PADDING] - tt_aV[BALLOONEDGESIZE], 0);
		balloon.width = tt_aV[WIDTH];
		//if(tt_bBoxOld)
		//	balloon.width += (balloon.padding << 1);
		tt_aV[BORDERWIDTH] = 0;
		tt_aV[WIDTH] = 0;
		tt_aV[PADDING] = 0;
		tt_aV[BGCOLOR] = "";
		tt_aV[BGIMG] = "";
		tt_aV[SHADOW] = false;
		// Append slash to img path if missing
		if(tt_aV[BALLOONIMGPATH].charAt(tt_aV[BALLOONIMGPATH].length - 1) != '/')
			tt_aV[BALLOONIMGPATH] += "/";
		return true;
	}
	return false;
};
balloon.OnCreateContentString = function()
{
	if(!tt_aV[BALLOON])
		return false;

	var aImg, sImgZ, sCssCrn, sVaT, sVaB, sCss0;

	// Cache balloon images in advance:
	// Either use the pre-cached default images...
	if(tt_aV[BALLOONIMGPATH] == config.BalloonImgPath)
		aImg = balloon.aDefImg;
	// ...or load images from different directory
	else
		aImg = Balloon_CacheImgs(tt_aV[BALLOONIMGPATH], tt_aV[BALLOONIMGEXT]);
	sCss0 = 'padding:0;margin:0;border:0;line-height:0;overflow:hidden;';
	sCssCrn = ' style="position:relative;width:' + tt_aV[BALLOONEDGESIZE] + 'px;' + sCss0 + 'overflow:hidden;';
	sVaT = 'vertical-align:top;" valign="top"';
	sVaB = 'vertical-align:bottom;" valign="bottom"';
	sImgZ = '" style="' + sCss0 + '" />';

	tt_sContent = '<table border="0" cellpadding="0" cellspacing="0" style="width:auto;padding:0;margin:0;left:0;top:0;"><tr>'
		// Left-top corner
		+ '<td' + sCssCrn + sVaB + '>'
		+ '<img src="' + aImg[1].src + '" width="' + tt_aV[BALLOONEDGESIZE] + '" height="' + tt_aV[BALLOONEDGESIZE] + sImgZ
		+ '</td>'
		// Top border
		+ '<td valign="bottom" style="position:relative;' + sCss0 + '">'
		+ '<img id="bALlOOnT" style="position:relative;top:1px;z-index:1;display:none;' + sCss0 + '" src="' + aImg[9].src + '" width="' + tt_aV[BALLOONSTEMWIDTH] + '" height="' + tt_aV[BALLOONSTEMHEIGHT] + '" />'
		+ '<div style="position:relative;z-index:0;top:0;' + sCss0 + 'width:auto;height:' + tt_aV[BALLOONEDGESIZE] + 'px;background-image:url(' + aImg[2].src + ');">'
		+ '</div>'
		+ '</td>'
		// Right-top corner
		+ '<td' + sCssCrn + sVaB + '>'
		+ '<img src="' + aImg[3].src + '" width="' + tt_aV[BALLOONEDGESIZE] + '" height="' + tt_aV[BALLOONEDGESIZE] + sImgZ
		+ '</td>'
		+ '</tr><tr>'
		// Left border (background-repeat fix courtesy Dirk Schnitzler)
		+ '<td style="position:relative;background-repeat:repeat;' + sCss0 + 'width:' + tt_aV[BALLOONEDGESIZE] + 'px;background-image:url(' + aImg[8].src + ');">'
		// Redundant image for bugous old Geckos which won't auto-expand TD height to 100%
		+ '<img width="' + tt_aV[BALLOONEDGESIZE] + '" height="100%" src="' + aImg[8].src + sImgZ
		+ '</td>'
		// Content
		+ '<td id="bALlO0nBdY" style="position:relative;line-height:normal;background-repeat:repeat;'
		+ ';background-image:url(' + aImg[0].src + ')'
		+ ';color:' + tt_aV[FONTCOLOR]
		+ ';font-family:' + tt_aV[FONTFACE]
		+ ';font-size:' + tt_aV[FONTSIZE]
		+ ';font-weight:' + tt_aV[FONTWEIGHT]
		+ ';text-align:' + tt_aV[TEXTALIGN]
		+ ';padding:' + balloon.padding + 'px'
		+ ';width:' + ((balloon.width > 0) ? (balloon.width + 'px') : 'auto')
		+ ';">' + tt_sContent + '</td>'
		// Right border
		+ '<td style="position:relative;background-repeat:repeat;' + sCss0 + 'width:' + tt_aV[BALLOONEDGESIZE] + 'px;background-image:url(' + aImg[4].src + ');">'
		// Image redundancy for bugous old Geckos that won't auto-expand TD height to 100%
		+ '<img width="' + tt_aV[BALLOONEDGESIZE] + '" height="100%" src="' + aImg[4].src + sImgZ
		+ '</td>'
		+ '</tr><tr>'
		// Left-bottom corner
		+ '<td' + sCssCrn + sVaT + '>'
		+ '<img src="' + aImg[7].src + '" width="' + tt_aV[BALLOONEDGESIZE] + '" height="' + tt_aV[BALLOONEDGESIZE] + sImgZ
		+ '</td>'
		// Bottom border
		+ '<td valign="top" style="position:relative;' + sCss0 + '">'
		+ '<div style="position:relative;left:0;top:0;' + sCss0 + 'width:auto;height:' + tt_aV[BALLOONEDGESIZE] + 'px;background-image:url(' + aImg[6].src + ');"></div>'
		+ '<img id="bALlOOnB" style="position:relative;top:-1px;left:2px;z-index:1;display:none;' + sCss0 + '" src="' + aImg[10].src + '" width="' + tt_aV[BALLOONSTEMWIDTH] + '" height="' + tt_aV[BALLOONSTEMHEIGHT] + '" />'
		+ '</td>'
		// Right-bottom corner
		+ '<td' + sCssCrn + sVaT + '>'
		+ '<img src="' + aImg[5].src + '" width="' + tt_aV[BALLOONEDGESIZE] + '" height="' + tt_aV[BALLOONEDGESIZE] + sImgZ
		+ '</td>'
		+ '</tr></table>';//alert(tt_sContent);
	return true;
};
balloon.OnSubDivsCreated = function()
{
	if(tt_aV[BALLOON])
	{
		var bdy = tt_GetElt("bALlO0nBdY");

		// Insert a TagToTip() HTML element into the central body TD
		if (tt_t2t && !tt_aV[COPYCONTENT] && bdy)
			tt_MovDomNode(tt_t2t, tt_GetDad(tt_t2t), bdy);
		balloon.iStem = tt_aV[ABOVE] * 1;
		balloon.aStem = [tt_GetElt("bALlOOnT"), tt_GetElt("bALlOOnB")];
		balloon.aStem[balloon.iStem].style.display = "inline";
		if (balloon.width < -1)
			Balloon_MaxW(bdy);
		return true;
	}
	return false;
};
// Display the stem appropriately
balloon.OnMoveAfter = function()
{
	if(tt_aV[BALLOON])
	{
		var iStem = (tt_aV[ABOVE] != tt_bJmpVert) * 1;

		// Tooltip position vertically flipped?
		if(iStem != balloon.iStem)
		{
			// Display opposite stem
			balloon.aStem[balloon.iStem].style.display = "none";
			balloon.aStem[iStem].style.display = "inline";
			balloon.iStem = iStem;
		}

		balloon.aStem[iStem].style.left = Balloon_CalcStemX() + "px";
		return true;
	}
	return false;
};
function Balloon_CalcStemX()
{
	var x = tt_musX - tt_x + tt_aV[BALLOONSTEMOFFSET] - tt_aV[BALLOONEDGESIZE];
	return Math.max(Math.min(x, tt_w - tt_aV[BALLOONSTEMWIDTH] - (tt_aV[BALLOONEDGESIZE] << 1) - 2), 2);
}
function Balloon_CacheImgs(sPath, sExt)
{
	var asImg = ["background", "lt", "t", "rt", "r", "rb", "b", "lb", "l", "stemt", "stemb"],
	n = asImg.length,
	aImg = new Array(n),
	img;

	while(n)
	{--n;
		img = aImg[n] = new Image();
		img.src = sPath + asImg[n] + "." + sExt;
	}
	return aImg;
}
function Balloon_MaxW(bdy)
{
	if (bdy)
	{
		var iAdd = tt_bBoxOld ? (balloon.padding << 1) : 0, w = tt_GetDivW(bdy);
		if (w > -balloon.width + iAdd)
			bdy.style.width = (-balloon.width + iAdd) + "px";
	}
}
// This mechanism pre-caches the default images specified by
// congif.BalloonImgPath, so, whenever a balloon tip using these default images
// is created, no further server connection is necessary.
function Balloon_PreCacheDefImgs()
{
	// Append slash to img path if missing
	if(config.BalloonImgPath.charAt(config.BalloonImgPath.length - 1) != '/')
		config.BalloonImgPath += "/";
	// Preload default images into array
	balloon.aDefImg = Balloon_CacheImgs(config.BalloonImgPath, config.BalloonImgExt);
}
Balloon_PreCacheDefImgs();
