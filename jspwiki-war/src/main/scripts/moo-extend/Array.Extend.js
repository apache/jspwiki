/*
Moo-extend: String-extensions
    Array:  rendAr(), max(), min()
*/


/*
Array.slick()
    Convert array of css-selectors into one or a set of DOM Elements.
    Mini template engine to generate DOM elements from clean, elegant css selectors,
    by utilising the Mootools Slick engine.
    Extensions:
    - "attach" or bind any of the generated DOM elements to a js object properties

Credit: https://github.com/Mr5o1/rendAr by Levi Wheatcroft (leviwheatcroft.com)

Requires:
    - core/Element
    - core/Elements
    - core/Array

Example 
>   new Element('div',{ attach:this });      //this.element now refers to div
>   new Element('div',{ attach:[this] });    //this.element now refers to div
>   new Element('div',{ attach:[this,'myproperty'] }); //this.myproperty now refers to div

Example rendAr()
>   ['div',{attach:[this,'myproperty'] }].slick();
>   ['ul', ['li[text=One]','li[text=Two]','li[text=Three]']].slick();

*/
Element.Properties.attach = {

    set: function( object ){
        if(!object[0]) object = [object];
        object[0][ object[1] || 'element' ] = this;
    }

};

Array.implement({

    slick: function() {

        var elements = [],type;

        this.each( function(item){
            if(item != null){ 
            type = typeOf(item);
            if ( type == 'elements' ) elements.append(item);
            else if ( item.grab /*isElement*/ ) elements.push(item);
            else if ( item.big  /*isString*/ ) elements.push(item.slick());
            else if ( type == 'object' ) elements.getLast().set(item);
            else if ( item.pop /*isArray*/ ) elements.getLast().adopt(item.slick());
            }
        });

        return elements[1] ? new Elements(elements) : elements[0];
    },

    /*
    Function: scale
    
    Example
        [0,50,100].scale() == [0,0.5,1]
        [0,50,100].scale(2) == [0,1,2]
        
    */
    scale: function( scale ) {

        var result = [],
            i, len = this.length,
            min = this.min(),
            rmax = this.max() - min;

        if( rmax == 0 ){ rmax = min; min = 0; }

        for( i=0; i<len; i++) result[i] = (scale||1) * (this[i] - min) / rmax;

        return result;
    },

    max: function(){ return Math.max.apply(null, this); },

    min: function(){ return Math.min.apply(null, this); }

});
