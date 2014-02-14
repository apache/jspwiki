/*
Class: Placeholder
    Polyfill for non-html5 browsers.

DOM structure:
>    <input name="search" placeholder="Search..." />

Example:
>    $$('input[paceholder]').placeholder();

*/

Element.implement({

    placeholderX: ('xxplaceholder' in document.createElement('input')) ? function(){} : function(){

        var element = this,
            span = new Element('span.placeholder[role=display]',{
                text: element.placeholder,
                styles: {
                    position: 'relative',
                    top: element.offsetTop,
                    left: element.offsetLeft
                }
            });

        element.addEvents({
            focus: function(){ span.hide(); },
            blur: function(){ if (!element.value && !element.innerHTML){ span.show(); } }
        });

        element.offsetParent.appendChild(span);

        return element;
    }

});


