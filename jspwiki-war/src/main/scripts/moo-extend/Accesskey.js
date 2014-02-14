/*
Function: Accesskey
    Highlight the available accesskeys to the user:
    - underline the accesskey ( wrap inside span.accesskey )
    - add a suffix to the title attribute of the element with the accesskey
      in square brackets : "title [ key ]"

Arguments:
    element - DOM element
    template - (string) html template replacement string, default <span class='accesskey'>$1</span>
*/

function Accesskey(element, template){

    if( !element.getElement('span.accesskey') ){

        var key = element.get('accesskey'),
            title = element.get('title');

        if( key ){

            element.set({
                html: element.get('html').replace(
                    RegExp( '('+key+')', 'i'),
                    template || "<span class='accesskey'>$1</span>" )
            });
            if( title ){ element.set('title', title + ' [ '+key+' ]'); }

           //console.log("ACCESSKEY ::",key, element.get('text'), element.get('title') );

        }
    }
}