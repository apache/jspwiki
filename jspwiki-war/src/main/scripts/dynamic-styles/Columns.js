/*
Script: Columns
    Format the page content side by side, in columns, like in a newspaper.
    HR elements (in wiki {{----}} markup) separate the columns.
    Column widths are equal and automatically calculated.
    Optionally, you can specify the width in pixel(px) for the columns.

    FSS: use HTML5/CSS3 columns options if available

Arguments:
    width - (optional) column width in pixel(px)

Example:
(start code)
    %%columms-300
        column-text1 ...
        ----
        column-text1 ...
    /%
(end)

DOM Structure
(start code)
    div-columns
        div.col[styles={width:xx%}]
        div.col[styles={width:xx%}]
(end)
*/
function Columns(element, options){

    var args = options.prefix.sliceArgs(element),
        columnCount = element.getElements('hr').length,
        width;

    if( columnCount /*>0*/ ){

        columnCount++;
        width = ( args[0] ) ? args[0]/columnCount+'px' : 100/columnCount+'%';

        element
            .addClass('columns')
            .grab('hr'.slick(),'top') //add one extra group-start-element at the top

            .groupChildren('hr', 'div.col', function(col){ col.setStyle('width',width); });

    }

}
