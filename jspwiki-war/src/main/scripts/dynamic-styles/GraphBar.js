 /*
Class: GraphBar
    Generate horizontal or vertical bars, without using images.
    Support any color, gradient bars, progress and gauge bars.
    The length of the bars can be based on numbers or dates.
    Allow to specify maximum and minimum values.

>    %%graphBars
>        %%gbBar 25 /%
>    /%

    Graphbar parameters can be passed in the js class constructor (options)
    or as css class parameters.

>    %%graphBars-min50-max3000-progress-lime-0f0f0f
>        %%gbBar 25 /%
>    /%

    Other examples of wiki-markup:
> %%graphBars-e0e0e0 ... %%    use color #e0e0e0, default size 120
> %%graphBars-blue-red ... %%  blend colors from blue to red
> %%graphBars-red-40 ... %%    use color red, maxsize 40 chars
> %%graphBars-vertical ... %%  vertical bars
> %%graphBars-progress ... %%  progress bars in 2 colors
> %%graphBars-gauge ... %%     gauge bars in gradient colors

Options:
    gbBar - CSS classname of the bar value elements (default = gbBar)
    gbBarContainer - CSS classname of parent element (default = graphBars)
    lowerbound - lowerbound of bar values (default:20px)
    upperbound - upperbound of bar values (default:320px)
    vwidth - vertical bar width in px(default:20px)
    isHorizontal - horizontal or vertical bars (default:true)
    isProgress - progress bar show 2 bars, always summing up to 100%
    isGauge - gauge bars have colour gradient related to the size/value of the bar


DOM-structure:
(start code)
    // original DOM-structure
    div.graphBar-(options)
        span.gbBar 100 

    //becomes, based on BOOTSTRAP
    //horizontal bar
    span.gb-group(.striped.active)[width:125px] 
      span.gb-bar[style="background:blue;width:40%"]
    span.gBar 100
    
    //vertical bar
    span.gb-group(.striped.active)(.vertical)[heigh:125px] 
      span.gb-bar[style="background:blue;height:100%;width:100%"]
    span.gBar 100

    //progress bar
    span.gb-group[width:125px] 
      span.gb-bar[style="background:blue;width:40%"]
      span.gb-bar[style="background:red;width:60%"]
    span.gbBar 100
(end)

Examples:
>    new GraphBar( dom-element, { options });

*/

var GraphBar = new Class({

    Implements: Options,

    options: {
        container: "graphBars", //classname of container
        gBar: "gBar", //classname of value tags

        gbGroup: "span.gb-group",
        gbBar: "span.gb-bar",
        
        offset:20, //(px) smallest bar = offset
        size:300, //(px) tallest bar = offset+size

        isHorizontal:true,
        isProgress:false,
        isGauge:false
    },

    initialize: function(el, options){


        var self = this,
            args = el.className,
            bars, data, i, len, table, clazz;

        self.setOptions(options);
        
        clazz = this.options.container;
        
        if( args.indexOf( clazz )==0 ){

            options = self.getArguments( args.slice(clazz.length) );

            bars = el.getElements('.'+ options.gBar + options.barName);

            if( !bars[0] && ( table = el.getElement('table') )){ 

                bars = new TableX( table ).filter(options.barName);

            }

            if( bars && bars[0] && (data = self.toNumbers(bars)) ){

                data = data.scale();
                for( i=0, len=bars.length; i<len; i++) self.render( bars[i], data[i], (i+1)/len );

            }
        }
    },

    getArguments: function( args ){

        var options = this.options,
            p,min,max,size;

        args = args.split('-');
        options.barName = args.shift(); //first param is optional barName
        min = options.offset;
        max = min + options.size;

        while( args.length ){

            p = args.shift().toLowerCase();

            if( p == "vertical" ){ options.isHorizontal = false; }
            else if( p == "gauge" ){ options.isGauge = true; }
            else if( p == "progress" ){ options.isProgress = true;  }

            else if( p == "striped" ){ options.gbGroup += "." + p; }
            else if( p == "active" ){ options.gbGroup += ".striped." + p; }
            else if( p.test(/success|info|warning|danger/ )){ options.gbBar += ".progress-bar-"+p; }

            else if( !p.indexOf("min") /*index==0*/){ min = p.slice(3).toInt(); }
            else if( !p.indexOf("max") /*index==0*/){ max = p.slice(3).toInt(); }
            else if( p != "" ){

                p = new Color(p); 
                if( p.hex ){ 
                    if( !options.color1 ){ options.color1 = p; }
                    else if( !options.color2 ){ options.color2 = p; }
                }

            }
        }
    
        size = max-min;           
        options.offset = (size > 0) ? min : max;
        options.size = size.abs();        

        return options;        
    },


    /*
    Function: render
        Render a graphBar and add it before or inside the element.
    
    Arguments:
        el - element
        val - converted value in range 0-100, 
        percent - position of the graphBar in the group, converted to a %
        
    DOM Structure of a graphbar
    (start code)
        span.graphBar-Group[stle='width=..px']
            span.graphBar[style='width=..%,background-color=..']
            span.graphBar[style='width=..%,background-color=..'] //only for progress bars
    (end)    
    */
    render: function(el, val, percent){

        var options = this.options,
            size = options.size,
            offset = options.offset,
            color1 = options.color1,
            color2 = options.color2,
            isGauge = options.isGauge,
            isProgress = options.isProgress,
            //isHorizontal = options.isHorizontal,
            dom, css;


        //color invertor
        if( !color2 && color1 && (isGauge||isProgress)) color2 = color1.invert();

        //color mixer
        if( !isProgress && color2 ) color1 = color1.mix(color2, 100*(isGauge ? val : percent)); 

        val = val*100;

        //first calculate bar sizes: group-bar, bar1, (optional) bar2
        css = isProgress ? 
            [offset+size, val+"%", (100-val)+"%"] : 
                [offset+val/100*(offset+size), "100%" ];

        //then convert sizes to bar css styles
        css = css.map( function(size){ 
            return options.isHorizontal ? {width:size} : {height:size, width:20}; 
        });

        //finally, add colors to the bar1 and bar2 css styles
        if( color1 ) css[1].backgroundColor = color1.hex;
        if( isProgress && color2 ) css[2].backgroundColor = color2.hex

        //build slick template
        dom = [options.gbGroup, { styles:css[0] }, [ options.gbBar, {styles:css[1]} ] ];
        if( isProgress && color1) dom[2].push( options.gbBar, {styles:css[2]} );

        dom.slick().inject(el, el.match('td') ? 'top' : 'before');

    },

    /*
    Function: parseBarData
        Parse bar data types and scale according to lbound and size
        
        Convert values to %, min=0% and max=100%
                
    */
    toNumbers: function(nodes){

        var num=[], dmy=[], val, 
            len=nodes.length, i,
            min, scale;

        //check -- reuse Array.makeSortable(some-array)...
        for( i=0; i<len; i++) {
            val = nodes[i].get('text').trim();

            if( num && isNaN( num[i] = +val ) ) num=0;
            
            //Only strings with non-numeric values
            if( dmy && ( num || isNaN( dmy[i] = Date.parse(val) ) ) ) dmy=0;
            
        };

        return dmy || num || false; 

    }

});
