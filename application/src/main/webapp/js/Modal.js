var Modal = function(title, html) {
	    	var self = this;

	    	this.options = {
	    	height : "200",
	    	width : "350",
	    	title:title,
	    	description: html,
	    	top: "center",
	    	left: "center",
	    	};

	    	var overlay= function() {
	    	var el = $('<div class="weemo_overlay"></div>');
	    	$(el).appendTo('body');
	    	};

	    	var defaultStyles = function() {
	    	var pageHeight = $(document).height();
	    	var pageWidth = $(window).width();

	    	if(self.options.top == "center") {
	    	self.options.top = (pageHeight / 2) - (self.options.height);
	    	}

	    	if(self.options.left == "center") {
	    	self.options.left = (pageWidth / 2) - (self.options.width/2);
	    	}

	    	$('.weemo_overlay').css({
	    	'position':'absolute',
	    	'top':'0',
	    	'left':'0',
	    	'background-color':'rgba(0,0,0,0.6)',
	    	'height':pageHeight,
	    	'width':pageWidth,
	    	'z-index':'9999999',
	    	'filter':'alpha(opacity = 60)',
	    	'-ms-filter':'alpha(opacity = 60)'
	    	});

	    	$('.weemo_modal_box').css({
	    	'position':'absolute',
	    	'left':self.options.left,
	    	'top':self.options.top,
	    	'display':'none',
	    	'height': self.options.height + 'px',
	    	'width': self.options.width + 'px',
	    	'z-index':'50',
	    	});
	    	$('.weemo_modal_close').css({
	    	'position':'relative',
	    	'top':'0',
	    	'left':'-10px',
	    	'float':'right',
	    	'display':'block',
	    	'color':'#000',
	    	'font-size':'10px',
	    	'text-decoration':'none',
	    	});
	    	$('.weemo_inner_modal_box').css({
	    	'background-color':'#fff',
	    	'height':(self.options.height - 50) + 'px',
	    	'width':(self.options.width - 50) + 'px',
	    	'padding':'20px'
	    	});
	    	};

	    	var weemo_box = function() {
	    	var box = $('<div class="weemo_modal_box"><a href="#" class="weemo_modal_close">close</a><div class="weemo_inner_modal_box"><h2>' + self.options.title + '</h2>' + self.options.description + '</div></div>');
	    	$(box).appendTo('.weemo_overlay');
	    	$('.weemo_modal_close').click(function(){
	    	$(this).parent().fadeOut().remove();
	    	$('.weemo_overlay').fadeOut().remove();
	    	});
	    	};

	    	this.show = function() {
	    	overlay();
	    	weemo_box();
	    	defaultStyles();
	    	$('.weemo_modal_box').fadeIn();
	    	};

	    	this.close = function() {
	    	$('.weemo_modal_close').parent().fadeOut().remove();
	    	        $('.weemo_overlay').fadeOut().remove();
	    	};
	    	};