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
/*global typeOf, Class, Dialog  */
/*exported Dialog.Selection */

/*
Class: Dialog.Selection
    A simple selection dialog, with a list of selectable items.

Arguments:
    options - see [Dialog] object

Options:
    body - list of selectable items, defined as a string ,array or object.
    onSelect - callback function when an item is clicked
    autoClose - (default true) hide the dialog when an iten is clicked

Inherits from:
    [Dialog]

Example:
    (start code)
    new Dialog.Selection({
        body:"a|b|c|d",
        caption:"Snippet Dialog",
        autoClose:true,
        onClick:function(v){ alert("clicked "+v) }
    });
    new Dialog.Selection({
        body:[a,b,c,d],
        caption:"Snippet Dialog",
        onClick:function(v){ alert("clicked "+v) }
    });
    new Dialog.Selection({
        body:{"avalue":"a","bvalue":"b","cvalue":"c"},
        caption:"Snippet Dialog",
        onClick:function(v){ alert("clicked "+v) }
    });
    (end code)
*/
Dialog.Selection = new Class({

    Extends: Dialog,

    options: {
        //onAction: function(value){},
        //selected: <some value>
        cssClass: "dialog selection",
        match: "^=",  //starts-with
        autoClose: true
    },

    initialize:function( options ){

        var self = this;

        self.setClass(".selection",options);
        self.selected = options.selected || "";
        self.parent( options );

        self.element.addEvent("click:relay(.item)", function(e){
            e.stop();
            self.action( this.get("title") );
        });

        //console.log("Dialog.Selection ", this.element.className);
    },

    setBody: function(content){

        var self = this, items=[];

        //console.log("Dialog.Selection body ",content);
        if( !content ){ content = self.options.body; }

        //convert "multi|value|string" into [array]
        if( typeOf(content) == "string" ){ content = content.split("|"); }

        //convert [array] into {object} with name:value pairs
        if( typeOf(content) == "array" ){

            //value should be html escaped !!
            content = content.reduce( function(accu, item){
                accu[item] = item.escapeHtml();
                return accu;
            }, {});

        }

        //convert {object} in DOM elements (ul/li collection)
        if( typeOf(content) == "object" ){

            Object.each(content, function(value, key){

                items.push( value == "" ?
                    "li.divider" :
                    "li.item", {html: value, title: key}
                );

            });
            content = ["ul", items].slick();

        }

        if( typeOf( content ) == "element" ){

            //first move the content elements into the body and then highlight the selected item
            self.parent( content )
                .setValue( self.selected );

        }

        return self;
    },

    /*
    Function: setValue
        Store the selected value. (this.selected).
        And highlight the selected item (if any)
    */
    setValue: function( value ){

        var self = this, selected = "selected", element,
            target = ".item[title" + self.options.match + value + "]";

        /*ffs
        if( self.hasClass("dialog-filtered") ){

            if( value == "" ){
                this.element.getElements(".item,.divider").show();
            } else {
                this.element.getElements(".item,.divider").hide();
                this.element.getElements(target).show();
            }
        }
        */
        element = self.get("." + selected);
        if( element ){ element.removeClass(selected); }

        element = self.get( target );
        if( element ){ element.addClass(selected); }

        self[selected] = value;

        return self;
    },

    getValue: function(){
        return this.selected;
    },

    action: function( value ){

        //console.log("Dialog.Selection action() ",value);
        this.setValue( value ).parent(value);

    }

});