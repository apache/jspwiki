/*
Dynamic style: %%commentbox

Example:
>  %%commentbox ... /% : floating box to the right
>  %%commentbox-Caption .... /% : commentbox with caption

DOM structure
(start code)
    div.commentbox
        h2|h3|h4 title
        ..body..

    //becomes, based on BOOTSTRAP Panels
    div.panel.panel-default
        div.panel-header
        div.panel-body
(end)
*/
function CommentBox(element, options){

    var header = element.getFirst(),
        caption = options.prefix.sliceArgs(element)[0],
        panelCSS = 'panel'.fetchContext(element);

    element.className='panel-body'; //reset className -- ie remove commentbox-...
    'div.commentbox'.slick().addClass(panelCSS).wraps(element);

    if( caption ){

        caption = 'h4'.slick({ text:caption.deCamelize() });

    } else if( header && header.match('h2,h3,h4') ) {

        caption = header;
    }

    if( caption ){

        'div.panel-heading'.slick()
            .grab(caption.addClass('panel-title'))
                .inject(element, 'before');

    }

}  