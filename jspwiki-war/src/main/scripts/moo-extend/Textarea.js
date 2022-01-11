/*
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); fyou may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
*/

/*eslint-env browser*/
/*global Class, Options, Events  */
/*exported Textarea */

/*exported Textarea */

/*
Class: Textarea
    The textarea class enriches a TEXTAREA element, and provides cross browser
    support to handle text selection: get and set the selected text,
    changing the selection, etc.
    It also provide support to retrieve and validate the caret/cursor position.

Example:
    (start code)
    <script>
        var ta = new Textarea( "mainTextarea" );
    </script>
    (end)
*/
var Textarea = new Class({

    Implements: [Options, Events],

    initialize: function(el, options){

        var self = this,
            ta = self.ta = document.id(el);
            //fireChange = function( event ){ self.fireEvent("change", event); };

        self.setOptions(options);

        //ta.addEvents({ change: fireChange, keyup: fireChange });

        //Create a shadow div to support getCoordinates() of any character in the textarea
        //This only works if the textarea font is monospace (?)
        self.taShadow = "div[style=position:absolute;visibility:hidden;overflow:auto]".slick()
          .setStyles( ta.getStyles(
            "font-family0font-size0line-height0text-indent0padding-top0padding-right0padding-bottom0padding-left0border-left-width0border-right-width0border-left-style0border-right-style0white-space0word-wrap"
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
    >    var ta = new Textarea("textarea-element");
    >    $("textarea-element") == ta.toElement();
    >    $("textarea-element") == $(ta); //mootools 1.2.x
    */
    toElement: function(){
        return this.ta;
    },
    focus: function(){
        this.ta.focus();
    },

    /*
    Function: getValue
        Returns the value (text content) of the textarea.
    */
    getValue: function(){
        return this.ta.value;
    },

    setValue: function(value){
        this.ta.value = value;
        this.setSelectionRange(0,0);
        return this;
    },
    /*
    Function: slice
        Invokes slice(..) on the value of the textarea
    */
    slice: function(start, end){
        return this.ta.value.slice(start, end);
    },
    /*
    Function: indexOf
        Invokes indexOf(..) on the value of the textarea
    */
    indexOf: function(searchValue, fromIndex){
        return this.ta.value.indexOf(searchValue, fromIndex);
    },

    /*
    Function: getFromStart
        Returns the start of the textarea, upto the start of the selection.
    */
    getFromStart: function(){
        return this.slice( 0, this.getSelectionRange().start );
    },

    /*
    Function: getTillEnd
        Returns the end of the textarea, starting from the end of the selection.
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
        Set a new selection range of the textarea

    Arguments:
        start - start position of the new selection
        end - (optional) end position of the new seletion (default is start)

    Returns:
        Textarea object
    */
    setSelectionRange: function(start, end){

        var ta = this.ta,
            value, diff, range;

        if( !end ){ end = start; }

        if( ta.setSelectionRange ){

            ta.setSelectionRange(start, end);

        } else {

            value = ta.value;
            diff = value.slice(start, end - start).replace(/\r/g, "").length;
            start = value.slice(0, start).replace(/\r/g, "").length;

            range = ta.createTextRange();
            range.collapse( true );
            range.moveEnd("character", start + diff);
            range.moveStart("character", start);
            range.select();
            //textarea.scrollTop = scrollPosition;
            //textarea.focus();

        }
        ta.fireEvent("change");
        return this;
    },

    /*
    Function: getSelectionRange
        Returns an object describing the textarea selection range.

    Returns: Object:
        start - (number) start position of the selection
        end - (number) end position of the selection
        thin - (boolean) indicates whether selection is empty (start==end)
    */

    /* ffs
    getIERanges: function(){
        this.ta.focus();
        var ta = this.ta,
            range = document.selection.createRange(),
            re = this.createTextRange(),
            dupe = re.duplicate();
        re.moveToBookmark(range.getBookmark());
        dupe.setEndPoint("EndToStart", re);
        return { start: dupe.text.length, end: dupe.text.length + range.text.length, length: range.text.length, text: range.text };
    },
    */
    getSelectionRange: function(){

        var ta = this.ta,
            start = 0,
            end = 0,
            range, dup, value, offset;

        if( ta.selectionStart != null ){

            //caret = { start: ta.selectionStart, end: ta.selectionEnd };
            start = ta.selectionStart;
            end = ta.selectionEnd;

        } else {

            range = document.selection.createRange();

            if ( range && range.parentElement() == ta ){
                dup = range.duplicate();
                value = ta.value;
                offset = value.length - value.match(/[\n\r]*$/)[0].length;

                dup.moveToElementText(ta);
                dup.setEndPoint("StartToEnd", range);
                end = offset - dup.text.length;

                dup.setEndPoint("StartToStart", range);
                start = offset - dup.text.length;
            }
        }

        return { start: start, end: end, thin: start == end };
    },

    /*
    Function: setSelection
        Replaces the selection with a new string (concatenation of arguments).
        On return, the selection is set to the replaced text string.

    Arguments:
        string - string to be inserted in the textarea.
            If multiple arguments are passed, all strings will be concatenated.

    Returns:
        Textarea object, with a new selection

    Example:
        > ta.setSelection("new", " value"); //replace selection by "new value"
    */
    setSelection: function(){

        var value = Array.from(arguments).join("").replace(/\r/g, ""),
            ta = this.ta,
            scrollTop = ta.scrollTop, //cache top
            start, end, v, range;

        if( ta.selectionStart != null ){

            start = ta.selectionStart;
            end = ta.selectionEnd;
            v = ta.value;
            ta.value = v.slice(0, start) + value + v.substr(end);
            ta.selectionStart = start;
            ta.selectionEnd = start + value.length;

        } else {

            ta.focus();
            range = document.selection.createRange();
            range.text = value;
            range.collapse(1 /*true*/);
            range.moveStart("character", -value.length);
            range.select();

        }
        ta.focus();
        ta.scrollTop = scrollTop;
        ta.fireEvent("change");
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

        var value = Array.from(arguments).join("");

        return this.setSelection( value )
            .setSelectionRange( this.getSelectionRange().start + value.length );

    },

    /*
    Function: isCaretAtStartOfLine
        Returns boolean indicating whether caret (or start of the selection)
        is at the start of a line.
        (previous char is \n)
    */
    isCaretAtStartOfLine: function(){

        var start = this.getSelectionRange().start;
        return ( (start < 1) || ( this.ta.value.charAt(start - 1).test( /[\n\r]/ ) ) );

    },
    /*
    Function: isCaretAtEndOfLine
        Returns boolean indicating whether the caret or the end of the selection
        is at the end of a line.
        (last char is \n)
    */
    isCaretAtEndOfLine: function(){

        var end = this.getSelectionRange().end;
        return ( (end == this.ta.value.length) || ( this.slice(end - 1, end + 1).test( /[\n\r]/ ) ) );

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

        var ta = this.ta,
            //make sure the shadow element is always just before of the textarea
            taShadow = this.taShadow.inject(ta, "before"),
            value = ta.value.replace(/[<>&]/g,"X"),
            el, t, l, w, h;

        //default character offset is the position of the caret (cursor or begin of the selection)
        if( offset == undefined ){ offset = this.getSelectionRange().end; }

        el = taShadow.set({
            styles: {
                width: ta.offsetWidth,
                height: ta.getStyle("height")  //ensure proper handling of scrollbars - if any
            },
            html: value.slice(0, offset) + "<i>A</i>" + value.slice(offset + 1)
        }).getElement("i");

        t = ta.offsetTop + el.offsetTop - ta.scrollTop;
        l = ta.offsetLeft + el.offsetLeft - ta.scrollLeft;
        w = el.offsetWidth;
        h = el.offsetHeight;

        //console.log(offset, ta.offsetTop, "top: "+t, ta.offsetLeft, "left: "+l, "width: "+w, "height: "+h, "right: "+(l + w), "bottom: "+(t + h) );
        return { top: t, left: l, width: w, height: h, right: l + w, bottom: t + h };

    },


    /*
    Function: onDragAndDrop
        Add Drag&Drop handlers on the Textarea
        Inspired by https://github.com/github/paste-markdown

    */
    onDragAndDrop: function(processData, onSuccess){

        var self = this,
            ta = this.ta;

        ta.addEventListener('dragover', function(event /*DragEvent*/){

            var dataTransfer = event.dataTransfer;
            if (dataTransfer){ dataTransfer.dropEffect = 'copy'; }
        });

        ta.addEventListener('drop', function(event /*DragEvent*/){

            var dataTransfer = event.dataTransfer;
            if (dataTransfer && (dataTransfer.files.length == 0)){
                insertData(event, dataTransfer);
            }
        });

        ta.addEventListener('paste', function(event /*ClipboardEvent*/){

            insertData(event, event.clipboardData);
        });

        function insertData(event, dataTransfer){

            var content = processData(dataTransfer);

            if ( content ) {

                event.stopPropagation();
                event.preventDefault();
                self.insertAfter(content);

                if( onSuccess ){ onSuccess(); }
            }
        }
    }


});
