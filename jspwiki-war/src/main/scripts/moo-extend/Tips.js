/*
Dynamic Style: Tips
    Add Tip behavior to a set of DOM Elements

Bootstrap
(start code)
    //tip anchors
    <element> Caption
        <body> ...body... </body>
    </element>        

    //layout of the tip, with absolute position
    div.tooltip(.active)(.top|.left|.right|.bottom)
        div.tooltip-inner
            <body> ... </body>                        
        div.tooltip-arrow
(end)
*/
var Tips = function Tips(elements,options){

        var tt = 'div.tooltip',
            TheTip = [tt,[tt+'-inner'/*,tt+'-arrow'*/]].slick().inject(document.body),
            inner = TheTip.getFirst();

        $$(elements).addEvents({

        	mousemove: function(e){
		        TheTip.setStyles({ top:e.page.y +10, left:e.page.x + 10 });
        	},

        	mouseenter: function(e){
		        inner.adopt( this.getFirst() ) ;
	    	    TheTip.addClass('in'); //.fade('in');
    	    },
			
        	mouseleave: function(e){
		        TheTip.removeClass('in'); //.fade('out');
        	    this.adopt( inner.getFirst() );
        	}
        });
}



/*TIP position logic
	position: function(event){
		
		var windowPadding={x:0, y:0};

		var size = window.getSize(), 
		    scroll = window.getScroll(),
			tip = {x: this.tip.offsetWidth, y: this.tip.offsetHeight},
			props = {x: 'left', y: 'top'},
			bounds = {y: false, x2: false, y2: false, x: false},
			obj = {};

		for (var z in props){

			obj[props[z]] = event.page[z] + this.options.offset[z];

			if (obj[props[z]] < 0) bounds[z] = true;

			if ((obj[props[z]] + tip[z] - scroll[z]) > size[z] - windowPadding[z]){

				obj[props[z]] = event.page[z] - this.options.offset[z] - tip[z];
				bounds[z+'2'] = true;
			}
		}

		this.tip.setStyles(obj);
	},

*/