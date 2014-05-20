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
    template - (string) html template replacement string, default <span class='highlight'>$1</span>
*/
function HighlightQuery( node, query, template ){

    //recursive node processing function
    function walk(node, regexp){

        if( node ){

            var s, n, nn = node.firstChild;

            //process all DOM children
            while( n = nn ){
                nn = n.nextSibling; //prefetch the next sibling, cause the dom tree is modified
                walk( n, regexp );
            }

            if( node.nodeType == 3 /* this is a text-node */ ){

                s = node.innerText || node.textContent || '';
                s = s.replace(/</g,'&lt;'); // pre text elements may contain <xml> element

                if( regexp.test( s ) ){

                    n = new Element('span',{
                        html: s.replace(regexp, template || "<span class='highlight'>$1</span>")
                    });
                    frag = document.createDocumentFragment();
                    while( n.firstChild ) frag.appendChild( n.firstChild );

                    node.parentNode.replaceChild( frag, node );
                    n = 0;

                }
            }
        }
    };

    //if( !query && document.referrer.test("(?:\\?|&)(?:q|query)=([^&]*)","g") ){ query = RegExp.$1; }
    //if( query ){
    if( query || (query = (document.referrer.match(/(?:\?|&)(?:q|query)=([^&]*)/)||[,''])[1]) ){

        //console.Log("highlight word : ",query);
        var words = decodeURIComponent(query)
                    .stripScripts() //xss vulnerability
                    .replace( /\+/g, " " )
                    .replace( /\s+-\S+/g, "" )
                    .replace( /([\(\[\{\\\^\$\|\)\?\*\.\+])/g, "\\$1" ) //escape metachars
                    //.trim().split(/\s+/).join("|");
                    .trim().replace(/\s+/g,'|');

        walk( node , RegExp( "(" + words + ")" , "gi") );

    }

};
