/*!
 * jQuery FancyZoom Plugin
 * version: 1.0.1 (20-APR-2014)
 * @requires jQuery v1.6.2 or later
 *
 * Examples and documentation at: http://github.com/keegnotrub/jquery.fancyzoom/
 * Licensed under the MIT license:
 *   http://www.opensource.org/licenses/mit-license.php
 */
(function ($) {
  "use strict";

  $.extend(jQuery.easing, {
    easeInOutCubic: function (x, t, b, c, d) {
      if ((t/=d/2) < 1) return c/2*t*t*t + b;
      return c/2*((t-=2)*t*t + 2) + b;
    }
  });

  $.fn.fancyZoom = function(settings) {
    var options = $.extend({
      minBorder: 90
    }, settings);

    var fz = new FancyZoom(options);
    
    this.each(function(e) {
      var $this = $(this);
      $this.mouseover(fz.preload);
      $this.click(fz.show);
    });
    
    function elementGeometry(elemFind) {
      var $elemFind = $(elemFind);

      if ($elemFind.children().length > 0) {
        $elemFind = $elemFind.children(":first");
      }
      
      var elemX = $elemFind.offset().left;
      var elemY = $elemFind.offset().top;
      var elemW = $elemFind.width() || 50;
      var elemH = $elemFind.height() || 12;

      return { left: elemX, top: elemY, width: elemW, height: elemH };
    }

    function windowGeometry() {
      var $window = $(window);
      var w = $window.width();
      var h = $window.height();
      var x = $window.scrollLeft();
      var y = $window.scrollTop();
      return { width: w, height: h, scrollX: x, scrollY: y };
    }
    
    function FancyZoom(opts) {
      var options = opts;  
      var zooming = false;
      var preloading = false;
      var pImage = new Image();
      var pTimer = 0;
      var pFrame = 0;
      var eGeometry, wGeometry;
      
      var $zoom, $zoom_img, $zoom_close, $zoom_spin;

      var self = this;

      $zoom = $("#zoom");
      if ($zoom.length === 0) {
        $zoom = $(document.createElement("div"));
        $zoom.attr("id", "zoom");
        $("body").append($zoom);
      }
      
      $zoom_img = $("#zoom_img");
      if ($zoom_img.length === 0) {
        $zoom_img = $(document.createElement("img"));
        $zoom_img.attr("id", "zoom_img");
        $zoom.append($zoom_img);
      }
      
      $zoom_close = $("#zoom_close");
      if ($zoom_close.length === 0) {
        $zoom_close = $(document.createElement("div"));
        $zoom_close.attr("id", "zoom_close");
        $zoom.append($zoom_close);
      }
      
      $zoom_spin = $("#zoom_spin");
      if ($zoom_spin.length === 0) {
        $zoom_spin = $(document.createElement("div"));
        $zoom_spin.attr("id", "zoom_spin");
        $("body").append($zoom_spin);
      }
      
      this.preload = function(e) {
        var href = this.getAttribute("href");
        
        if (pImage.src !== href) {
          preloading = true;
          pImage = new Image();
          $(pImage).on('load', function() {
            preloading = false;
          });
          pImage.src = href;
        }
      };

      this.show = function(e) {      
        wGeometry = windowGeometry();
        eGeometry = elementGeometry(this);

        self.preload.call(this, e);

        if (preloading) {
          if (pTimer === 0) {
            startSpinner(this);
          }
        }
        else {
          zoomIn(this);
        }
        
        e.preventDefault();
      };
      
      function runSpinner(from) {
        if (preloading) {
          $zoom_spin.css("backgroundPosition", "0px " + (pFrame * -50) + "px");
          pFrame = (pFrame + 1) % 12;
        }
        else {
          clearInterval(pTimer);
          pTimer = 0;
          pFrame = 0;
          $zoom_spin.hide();
          zoomIn(from);
        }
      }

      function startSpinner(from) {
        $zoom_spin.css({
          left: ((wGeometry.width / 2) + wGeometry.scrollX) + "px",
          top: ((wGeometry.height / 2) + wGeometry.scrollY) + "px",
          backgroundPosition: "0px 0px",
          display: "block"
        });
        pFrame = 0;
        pTimer = setInterval(function() {
          runSpinner(from);
        }, 100);
      }
      
      function zoomIn(from) {
        if (zooming) return false;
        zooming = true;
        
        $zoom_img.attr("src", from.getAttribute("href"));

        var endW = pImage.width;
        var endH = pImage.height;
        
        var sizeRatio = endW / endH;
        if (endW > wGeometry.width - options.minBorder) {
          endW = wGeometry.width - options.minBorder;
          endH = endW / sizeRatio;
        }
        if (endH > wGeometry.height - options.minBorder) {
          endH = wGeometry.height - options.minBorder;
          endW = endH * sizeRatio;
        }
        
        var endTop = (wGeometry.height/2) - (endH/2) + wGeometry.scrollY;
        var endLeft = (wGeometry.width/2) - (endW/2) + wGeometry.scrollX;

        $zoom_close.hide();
        $zoom.hide().css({
          left      : eGeometry.left + "px",
          top       : eGeometry.top + "px",
          width     : eGeometry.width + "px",
          height    : eGeometry.height + "px",
          opacity   : "hide"
        });
        
        $zoom.animate({
          left    : endLeft + 'px',
          top     : endTop + 'px',
          width   : endW + "px",
          height  : endH + "px",
          opacity : "show"
        }, 200, "easeInOutCubic", function() {
          $zoom_close.fadeIn();
          $zoom_close.click(zoomOut);
          $zoom.click(zoomOut);
          $(document).keyup(closeOnEscape);
          zooming = false;
        });
      }
      
      function zoomOut() {
        if (zooming) return false;
        zooming = true;
        
        $zoom_close.hide();
        $zoom.animate({
          left    : eGeometry.left + "px",
          top     : eGeometry.top + "px",
          opacity : "hide",
          width   : eGeometry.width + "px",
          height  : eGeometry.height + "px"
        }, 200, "easeInOutCubic", function() {
          zooming = false;
        });

        $zoom.unbind('click', zoomOut);
        $zoom_close.unbind('click', zoomOut);
        $(document).unbind('keyup', closeOnEscape);
      }
      
      function closeOnEscape(event){
        if (event.keyCode == 27) {
          zoomOut();
        }
      }
    }
  };

})(jQuery);
