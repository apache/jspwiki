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

        this.behaviors.push({s: selector, b: behavior, o: options, once:once});
        return this;

    },

    once: function(selector, behavior, options){

        return this.add(selector, behavior, options, true);

    },

    update: function(){

        //console.log(this.behaviors);
        var cache = "_bhvr", updated, type, nodes;

        this.behaviors.each( function( behavior ){

            nodes = $$(behavior.s);
            type = typeOf(behavior.b);
            //console.log("BEHAVIOR ", behavior.once?"ONCE ":"", nodes.length, behavior.s, typeOf(behavior.b) );

            if( behavior.once && nodes[0] ){

                if( type == 'class'){ new behavior.b(nodes, behavior.o); }
                else if( type == 'function'){ behavior.b(nodes, behavior.o); }

            } else {

                nodes.each( function(node){

                    updated = node[cache] || (node[cache] = []);

                    if ( updated.indexOf(behavior) == -1 ){

                        //if( type == 'string' ) node[behavior.b](behavior.o);
                        if( type == 'class'){ new behavior.b(node, behavior.o); }
                        else if( type == 'function'){ behavior.b.call(node, node, behavior.o); }

                        updated.push( behavior );
                    }
                });
            }

        })

        return this;
    }

});
