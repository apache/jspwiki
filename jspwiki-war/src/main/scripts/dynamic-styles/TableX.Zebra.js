/*
Class: TableX.Zebra
    Simple class to add odd/even coloring to tables.

    When the first color == 'table' or '' the predefined css class ''.odd''
    is used to color the alternative rows.

Usage:
    > new TableX.Zebra( table-element, {colors:['eee','fff']});
    > new TableX.Zebra( table-element, {colors:['red']});

*/
TableX.Zebra = function(table, options){

    function stripe(){

        this.rows.filter( Element.isVisible ).each( function(row,j){

            j &= 1; //0,1,0,1...
            if( isArr ){
                row.setStyle('background-color', colors[j]||'');
            } else {
                row.ifClass(j, 'odd', '');
            }
        });
    };

    var colors = options.colors,
        isArr = colors[0];

    if ( isArr ){ colors = colors.map( function(c){ return new Color(c); }); }

    //console.log("ZEBRA ",options.colors, colors[0],colors[1]);
    stripe.call( new TableX(table, { onRefresh:stripe }) );

}
