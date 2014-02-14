/*
Class: Textarea
    The textarea class enriches a TEXTAREA element, and provides cross browser
    support to handle text selection: get and set the selected text,
    changing the selection, etc.
    It also provide support to retrieve and validate the caret/cursor position.

Example:
    (start code)
    <script>
        var txta = new Textarea( "mainTextarea" );
    </script>
    (end)
*/
var Textarea = new Class({

    Implements: [Options,Events],

    //options: { onChange:function(e){} );

    initialize: function(el,options){

        var self = this,
            txta = self.ta = $(el),

            lastValue,
            lastLength = -1,
            changeFn = function(e){
                var v = txta.value;
                if( v.length != lastLength || v !== lastValue ){
                    self.fireEvent('change', e);
                    lastLength = v.length;
                    lastValue = v;
                }
            };

        self.setOptions(options);

        txta.addEvents({ change:changeFn, keyup:changeFn });

        //Create shadow div to support pixel measurement of the caret in the textarea
        //self.taShadow = new Element('div',{
        //    styles: { position:'absolute', visibility:'hidden', overflow:'auto'/*,top:0, left:0, zIndex:1, white-space:pre-wrap*/ }
        //})
        self.taShadow = new Element('div[style=position:absolute;visibility:hidden;overflow:auto]')
          .inject(txta,'before')
          .setStyles( txta.getStyles(
            'font-family0font-size0line-height0text-indent0padding-top0padding-right0padding-bottom0padding-left0border-left-width0border-right-width0border-left-style0border-right-style0white-space0word-wrap'
            .split(0)
        ));

        return this;
    },

    /*
    Function: toElement
        Return the DOM textarea element.
        This allows the dollar function to return
        the element when passed an instance of the class. (mootools 1.2.x)

    Example:
    >    var txta = new Textarea('textarea-element');
    >    $('textarea-element') == txta.toElement();
    >    $('textarea-element') == $(txta); //mootools 1.2.x
    */
    toElement: function(){
        return this.ta;
    },

    /*
    Function: getValue
        Returns the value (text content) of the textarea.
    */
    getValue: function(){
        return this.ta.value;
    },
    /*
    Function: slice
        Mimics the string slice function on the value (text content) of the textarea.
    Arguments:
        Ref. javascript slice function
    */
    slice: function(start,end){
        return this.ta.value.slice(start,end);
    },


    /*
    Function: getFromStart
        Returns the first not selected part of the textarea, till the start of the selection.
    */
    getFromStart: function(){
        return this.slice( 0, this.getSelectionRange().start );
    },

    /*
    Function: getTillEnd
        Returns the last not selected part of the textarea, starting from the end of the selection.
    */
    getTillEnd: function(){
        return this.slice( this.getSelectionRange().end );
    },

    /*
    Function: getSelection
        Returns the selected text as a string

    Note:
        IE fixme: this may return any selection, not only selected text in this textarea
            //if(Browser.Engine.trident) return document.selection.createRange().text;
    */
    getSelection: function(){

        var cur = this.getSelectionRange();
        return this.slice(cur.start, cur.end);

    },

    /*
    Function: setSelectionRange
        Selects the selection range of the textarea from start to end

    Arguments:
        start - start position of the selection
        end - (optional) end position of the seletion (default == start)

    Returns:
        Textarea object
    */
    setSelectionRange: function(start, end){

        var txta = this.ta,
            value,diff,range;

        if(!end){ end = start; }

        if( txta.setSelectionRange ){

            txta.setSelectionRange(start, end);

        } else {

            value = txta.value;
            diff = value.slice(start, end - start).replace(/\r/g, '').length;
            start = value.slice(0, start).replace(/\r/g, '').length;

            range = txta.createTextRange();
            range.collapse(1 /*true*/);
            range.moveEnd('character', start + diff);
            range.moveStart('character', start);
            range.select();
            //textarea.scrollTop = scrollPosition;
            //textarea.focus();

        }
        return this;
    },

    /*
    Function: getSelectionRange
        Returns an object describing the textarea selection range.

    Returns:
        {{ { 'start':number, 'end':number, 'thin':boolean } }}
        start - coordinate of the selection
        end - coordinate of the selection
        thin - boolean indicates whether selection is empty (start==end)
    */

/* ffs
    getIERanges: function(){
        this.ta.focus();
        var txta = this.ta,
            range = document.selection.createRange(),
            re = this.createTextRange(),
            dupe = re.duplicate();
        re.moveToBookmark(range.getBookmark());
        dupe.setEndPoint('EndToStart', re);
        return { start: dupe.text.length, end: dupe.text.length + range.text.length, length: range.text.length, text: range.text };
    },
*/
    getSelectionRange: function(){

        var txta = this.ta,
            caret = { start: 0, end: 0 /*, thin: true*/ },
            range, dup, value, offset;

        if( txta.selectionStart!=null ){

            caret = { start: txta.selectionStart, end: txta.selectionEnd };

        } else {

            range = document.selection.createRange();
            //if (!range || range.parentElement() != txta){ return caret; }
            if ( range && range.parentElement() == txta ){
                dup = range.duplicate();
                value = txta.value;
                offset = value.length - value.match(/[\n\r]*$/)[0].length;

                dup.moveToElementText(txta);
                dup.setEndPoint('StartToEnd', range);
                caret.end = offset - dup.text.length;
                dup.setEndPoint('StartToStart', range);
                caret.start = offset - dup.text.length;
            }
        }

        caret.thin = (caret.start==caret.end);

        return caret;
    },

    /*
    Function: setSelection
        Replaces the selection with a new value (concatenation of arguments).
        On return, the selection is set to the replaced text string.

    Arguments:
        string - string to be inserted in the textarea.
            If multiple arguments are passed, all strings will be concatenated.

    Returns:
        Textarea object, with a new selection

    Example:
        > txta.setSelection("new", " value"); //replace selection by 'new value'
    */
    setSelection: function(){

        var value = Array.from(arguments).join('').replace(/\r/g, ''),
            txta = this.ta,
            scrollTop = txta.scrollTop, //cache top
            start,end,v,range;

        if( txta.selectionStart!=null ){

            start = txta.selectionStart;
            end = txta.selectionEnd;
            v = txta.value;
            //txta.value = v.substr(0, start) + value + v.substr(end);
            txta.value = v.slice(0, start) + value + v.substr(end);
            txta.selectionStart = start;
            txta.selectionEnd = start + value.length;

        } else {

            txta.focus();
            range = document.selection.createRange();
            range.text = value;
            range.collapse(1 /*true*/);
            range.moveStart("character", -value.length);
            range.select();

        }
        txta.focus();
        txta.scrollTop = scrollTop;
        txta.fireEvent('change');
        return this;

    },

    /*
    Function: insertAfter
        Inserts the arguments after the selection, and puts caret after inserted value

    Arguments:
        string( one or more) - string to be inserted in the textarea.

    Returns:
        Textarea object
    */
    insertAfter: function(){

        var value = Array.from(arguments).join('');

        return this.setSelection( value )
            .setSelectionRange( this.getSelectionRange().start + value.length );

    },

    /*
    Function: isCaretAtStartOfLine
        Returns boolean indicating whether caret is at the start of a line.
    */
    isCaretAtStartOfLine: function(){

        var i = this.getSelectionRange().start;
        return( (i<1) || ( this.ta.value.charAt( i-1 ).test( /[\n\r]/ ) ) );

    },

    /*
    Function: getCoordinates
        Returns the absolute coordinates (px) of the character at a certain offset in the textarea.
        Default returns pixel coordinates of the selection.

    Credits:
        Inspired by http://github.com/sergeche/tx-content-assist.

    Arguments:
        offset - character index
            If omitted, the pixel position of the caret is returned.

    Returns:
        {{ { top, left, width, height, right, bottom } }}
     */
    getCoordinates: function( offset ){

        var txta = this.ta,
            taShadow = this.taShadow,
            delta = 0,
            el,css,style,t,l,w,h;

        //prepare taShadow
        css = txta.getStyles(['padding-left','padding-right','border-left-width','border-right-width']);
        for(style in css){ delta +=css[style].toInt() }

        //default offset is the position of the caret
        if( !offset ){ offset = this.getSelectionRange().end; }

        el = taShadow.set({
            styles: {
                width: txta.offsetWidth - delta,
                height: txta.getStyle('height')  //ensure proper handling of scrollbars - if any
            },
            //FIXME: should we put the full selection inside the <i></i> bracket ? (iso a single char)
            html: txta.value.slice(0, offset) + '<i>A</i>' + txta.value.slice(offset+1)
        }).getElement('i');

        t = txta.offsetTop + el.offsetTop - txta.scrollTop;
        l = txta.offsetLeft + el.offsetLeft - txta.scrollLeft;
        w = el.offsetWidth;
        h = el.offsetHeight;
        return {top:t, left:l, width:w, height:h, right:l+w, bottom:t+h}

    }

});

