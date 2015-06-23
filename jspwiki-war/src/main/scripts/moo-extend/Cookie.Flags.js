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
Class: Cookie.Flags
    Descendent of the Mootools Cookie class.
    Stores the True/False state of a set of dom-elements in a cookie.
    Encoding: per element in order of appearance, a character 'T' of 'F' is stored.

    Side-effect: you always FIRST need to get all elements in sequence, to build
    the internal elements[] array.
    Then you can change the status flags.

Example:
(start code)
    var cookie = new Cookie.Flags( 'mycookie_name', {duration:20});

    default_state = false;
    state = cookie.get(element_1, default_state); //add item to the cookie-repo ?? checkme...
    .. repeat this for all dom-elements..

    cookie.write(element_x, true);
(end)
*/
Cookie.Flags = new Class({
    Extends: Cookie,

    initialize: function(key,options){

        var self = this;
        self.flags = ''; //sequence of state-flags, eg 'TTFTFFT'
        self.elements = []; //array of elements, mapping one on one to the flags[]
        self.parent(key,options);
        self.pims = self.read();
        //console.log(this.key, 'pims:',this.pims);

    },

    get: function(element, bool){

        var self = this,
            cookie = self.pims,
            index = self.flags.length;

        if( cookie && (index < cookie.length) ){
            bool = (cookie.charAt(index)=='T');
        }
        self.flags += (bool?'T':'F');
        self.elements.push(element);
        //console.log("Cookie.Flags.get", cookie, index, this.flags)
        return bool;

    },

    write: function(element, bool){

        var self = this,
            flags = self.flags,
            index = self.elements.indexOf(element);

        if( index >= 0 ){
            flags = flags.slice(0,index) + (bool?'T':'F') + flags.slice(index+1);
            self.parent(flags); //write cookie
        }
        //console.log("Cookie.Flags.write", flags, index, bool);

    }

});
