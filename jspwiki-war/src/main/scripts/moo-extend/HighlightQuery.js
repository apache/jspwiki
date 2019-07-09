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
Class: HighlightQuery
    Highlight any word or phrase of a previously search query.
    The query can be passed in as a parameter or will be read
    from the documents referrer url.

Credit:
    Inspired by http://www.kryogenix.org/code/browser/searchhi/
    Refactored for JSPWiki -- now based on regexp's.

Arguments
    node - (DOM-element)
    query - (optional) query string, default is document referrer query string
    template - (string) html template replacement string, default <mark>$&</mark>
*/
/*eslint-env browser*/
/*exported HighlightQuery */

function HighlightQuery( node, query, template ){

    if( query || (query = (document.referrer.match(/(?:\?|&)(?:q|query)=([^&]*)/)||[,''])[1]) ){

    try {

        var words = decodeURIComponent(query)
                .escapeHtml() //xss vulnerability
                .replace( /\+/g, " " )
                .replace( /\s+-\S+/g, "" )
                .replace( /([([{\\^$|)?*.+])/g, "\\$1" ) //escape metachars
                .trim().replace(/\s+/g,'|'),

            matchQuery = RegExp( "(" + words + ")" , "gi");

    } catch(e) {
        console.error(e);
        return;
    }

        //console.log("highlight word : ",query, words, matchQuery);

        node.mapTextNodes( function(s){

            return s.replace( /</g, "&lt;" ) //pre elements may contain xml <
                    .replace( matchQuery, template || "<mark>$&</mark>" );


        }, true /* includePreCodeNodes */ );
    }
}
