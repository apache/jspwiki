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
/*
Class: Behavior
  Behavior is a way to initiate certain UI components for elements on the page
  by a given selector. The callback is only called once of each element.

  Inspired by: https://github.com/arian/elements-util/blob/master/lib/behavior.js
  Extended for jspwiki.

Example:

    var behavior = new Behavior()

    // define a new slider behavior, which initiates a slider class.
    behavior.add('.slider', function(element){
        new Slider(element)
    })
    //this function is invoked once, with all Elements passed as argument
    behavior.once('.slider', function(elements){
        new Slider(elements)
    })

    ...
    window.addEvent('domready', function(){ behavior.update() });

*/
var Behavior = new Class({

    initialize: function(){
        this.behaviors = [];
    },

    add: function(selector, behavior, options, once){

        this.behaviors.push({s: selector, b: behavior, o: options, once: once});
        return this;

    },

    once: function(selector, behavior, options){

        return this.add(selector, behavior, options, true);

    },

    update: function(){

        var cache = "_bhvr", updated, type, isClass, isFunction,
            nodes, node, i = 0, j, item, behavior, options, items;

        while( item = this.behaviors[ i++ ] ){

            //console.log("BEHAVIOR ", item.once?"ONCE ":"", nodes.length, item.s, typeOf(item.b) );
            once = [];
            options = item.o;
            behavior = item.b;
            type = typeOf(behavior);
            isClass = ( type == "class" );
            isFunction = ( type == "function" );

            nodes = $$(item.s); //selector

            if( nodes[0] ){

                for( j=0; node = nodes[ j++ ]; ){

                    updated = node[cache] || (node[cache] = []);

                    if ( updated.indexOf(item) < 0 ){

                        if( item.once ){

                            once.push( node );

                        } else {

                            //if( isString ) node[behavior](options);
                            if( isClass ){ new behavior(node, options); }
                            else if( isFunction ){ behavior.call(node, node, options); }

                        }
                        updated.push( item );
                    }
                }

                if( once[0] ){
                    //console.log("ONCE", item.s , once.length);
                    if( isClass ){ new behavior($$(once), options); }
                    else if( isFunction ){ behavior($$(once), options); }

                }
            }
        }

        return this;
    }

});
