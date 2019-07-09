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
/*global Wiki*/

//uses String.slick(), String.xsubs()

/*
Html2Wiki
    Mini html to wiki markup convertor for JSPWiki.
    Used by the Wiki.Edit.js to handle html which is input via drap&drop, or copy&paste.

    It uses a brute-force find&replace approach,  rather then a fine-grained walk the dome tree approach.
    It gets the job done 90% of the time.

*/
!(function( wiki, document ){

    //convert url to wiki link
    wiki.url2links = function( links ){

        return links ? links.split("\r\n").map(function(link){

            return "\n[" + link + "]\n";

        }) : links;

    }

    //convert html to wiki-markup
    wiki.html2wiki = function( html ){

        if( !html ) return;

        //console.log(html);

        //make temporary dom element to convert html to wiki markup
        var dom = "div".slick({html:html});

        forEachTextNode(dom, /\[/g, "[[");  //url's in text nodes are converted to [link]
        forEachNode(dom, "style", removeElement);

        //inline elements
        substituteEachNode(dom, "b,strong", "__{0}__" );
        substituteEachNode(dom, "i", "''{0}''" );
        substituteEachNode(dom, "sup", "%%sup {0}/%");
        substituteEachNode(dom, "sub", "%%sub {0}/%");
        forEachNode(dom, "a", replaceHtmlA);
        substituteEachNode(dom, "tt", "{{{0}}}" );
        substituteEachNode(dom, "pre,code", "\n{{{\n{0}\n}}}\n" );

        //block elements
        substituteEachNode(dom, "hr", "\n----\n");
        substituteEachNode(dom, "br", " \\\\ ");
        substituteEachNode(dom, "h1,h2", "\n!!! {0}\n");
        substituteEachNode(dom, "h3", "\n!! {0}\n");
        substituteEachNode(dom, "h4", "\n! {0}\n");
        substituteEachNode(dom, "dl dt", "\n;{0}:");

        forEachNode(dom, "li", replaceHtmlLI);

        forEachNode(dom, "tr", replaceHtmlTR);
        substituteEachNode(dom, "p", "\n{0}\n");

        return dom.textContent.replace(/^\s*(.*)\s*$/,"$1")+"\n";
    }


// Helper functions
/*
Function: forEachTextNode
    Iterate over all descendent text nodes and replace their contents.

Example
    forEachTextNode( document, " ", "&nbsp;" );
    forEachTextNode( some_node, /\[/g, "[[");
*/
function forEachTextNode(node, substrOrHandle, newSubstr){

    var n,
        isHandle = typeof substrOrHandle === "function",
        walk = document.createTreeWalker(node, 4 /*NodeFilter.SHOW_TEXT*/, null, false);

    while(( n = walk.nextNode() )){
        isHandle ? substrOrHandle(n) : replaceTextContent(n, substrOrHandle, newSubstr);
    }
    return node;
}

/*
Function: forEachNode
    Loop over each selected node and invoke a function

Example:
    forEachNode( document, "p p", function(p){ p.className = "nested_par"; });
*/
function forEachNode(node, selector, callback){

    ( node.querySelectorAll(selector) || [] ).forEach(callback);
    return node;
}

/*
Function: substituteEachNode
    Loop over each selected node, and replace it contents.
    Use paramater "{0}" to refer to the textContent

Example:
    forEachNode( document, "p", "\n{0}\n");
*/
function substituteEachNode(node, selector, pattern){

    return forEachNode(node, selector, function(n){
        replaceNodeByText(n, pattern.xsubs(n.textContent) );
    });
}

/*
Function: replaceNodeByText
    Replace a Node by a Text-Node
*/
function replaceNodeByText(node, text){

    node.parentNode.replaceChild( document.createTextNode(text), node );

}

/*
Function: replaceTextContent
    Replace the content of a node

Example
    replaceTextContent( some_node, /\[/g, "[[");
*/
function replaceTextContent(node, substrOrRegExp, newSubstr){

    node.textContent = node.textContent.replace(substrOrRegExp, newSubstr);

}

/*
Function: removeElement
*/
function removeElement(node){

    node.parentNode.removeChild(node);

}

/*
Function: getAncestors
    Return array of all ancestors, matching the selector

Example
    getAncestors( some_li_element, "ul,ol" );
*/
function getAncestors(node, selector){

    var ancestors = [];

    while(( node = node.parentElement )){
        if( node.matches(selector) ){ ancestors.push(node); }
    }
    return ancestors;
}


// Wiki Html-to-Markup helper functions

// convert <a href="...">description</a>  into [description|url]
function replaceHtmlA( link ){

    var href = link.href,
        description = link.textContent;

    //escape the [ and ] characters inside the link-description text
    description = description.replace(/\[\[/g,"&lsqb;").replace(/\]/g,"&rsqb;");

    replaceNodeByText( link, " [" + description + "|" + href + "] ");
}

// convert <tr>...</tr>  in wiki table rows with a | or || prefix
function replaceHtmlTR( row ){

    var s = "\n", c, cell;

    for(c=0; (cell = row.cells[c]); c++){

        s += ( cell.tagName.test( /th/i ) ? "||" : "|") +
             ( cell.textContent.trim() || '&nbsp;');
//             ( cell.textContent.trim().replace(/\|/g, '~|') || '&nbsp;');
    }
    replaceNodeByText(row, s );
}

// convert <li> in wiki list items with a * or # prefix
function replaceHtmlLI( li ){

    var listType = li.parentElement.tagName,
        markup = listType.test(/ul/i) ? "*" : listType.test(/ol/i) ? "#" : "*",
        depth = getAncestors(li, "ul,ol").length;

    if(( li = li.firstChild )){
        replaceTextContent(li, /.*/, "\n" + markup.repeat(depth) + " "+ "$&");
    }
}


})( Wiki, document );

