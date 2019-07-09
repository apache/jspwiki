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
/*global $ */
/*exported AddCSS */


/* Behavior: Add-CSS
        Inject any custom css into a wiki page.
        You can either directly insert the css definitions in your page or
        include it from another wiki page.

        Carefull with CSS-injection (https://www.owasp.org/index.php/Testing_for_CSS_Injection_%28OTG-CLIENT-005%29)
        - injection of "</style><script>...</script><style>" not possible, as all <,> are escaped by jspwiki
        - take care of attribute selector attacks ??

>   %%add-css ... /%
>   %%add-css [some-remote-wiki-page] /%
*/
function AddCSS(element) {

    function insertStyle(elements) {

        var css = "",    //css = "".concat(...elements);
            item;

        //concatenate all css to be inserted
        while( (item = elements.shift()) ){ css += item.innerHTML; }

        css = css //cascading replaces

            //allow google fonts @import url(https://fonts.googleapis.com/css?family=XXXX);
            .replace(/@import url\(https:\/\/fonts.googleapis.com\/css\?family=/gi, "\xa4")

            //fixme: allow data:image/svg+xml
            .replace(/url\("data:image\/svg\+xml/gi,"\xa6")

            //replace wiki-image links to css url()
            //xss protection: remove invalid url's;  only allow url([wiki-attachement])
            .replace(/url\(<a class="attachment" href="([^"]+.woff)".*><\/a>\)/gi, 'url(<\xa5$1")')
            .replace(/url\(<a class="attachment" href="([^"]+.ttf)".*><\/a>\)/gi, 'url(<\xa5$1")')
            .replace(/url\(<a class="attachment" href="([^"]+.otf)".*><\/a>\)/gi, 'url(<\xa5$1")')

            //remaining unmarked urls are invalid
            .replace(/url\(<a[^>]+>\)/gi, "url(invalid)") //remove remaining url(<a...)
            .replace(/url\([^<][^)]+\)/gi, "url(invalid)")  //remove remaining url(xxx)

            .replace(/@import/gi, "invalid") //xss protection: remove the remaining @import statements

            //restore svg images
            .replace(/\xa6/g, "url(\"data:image/svg+xml")

            //restore google font urls
            .replace(/\xa4/g, "@import url(https://fonts.googleapis.com/css?family=") //google fonts -part2

            .replace(/expression|behavior/gi, "invalid") //xss protection: remove IE dynamic properties

            .replace(/url\(<img class="inline" .*?src="([^"]+)[^>]*>\)/gi, "url($1)")
            .replace(/<\xa5([^"]+)"/gi, "$1")  //attached font files- part2

            .replace(/<p>|<\/p>/gi, "") //jspwiki inserts <p/> for empty lines, remove them
            .replace(/&amp;/g, "&")
            .replace(/&gt;/g, ">")
            .replace(/&lt;/g, "<");

        css = "style[type=text/css]".slick({ text: css });

        /*
        Sequence to insert CSS is :
            (1) jspwiki.css (<HEAD>)
            (2) sidebar/favorites
            (3) in-page additional styles

        Because the side-bar is located at the end of the DOM, the additional sidebar styles
        need to be inserted at the top of the DOM, i.e. just at the top of the BODY element.
        Other CCS is injected in the order of appearance.
        */
        if (element.closest(".sidebar")) {

            var body = document.body;
            body.insertBefore(css, body.firstChild);
            //$.start(css, document.body);
            element.remove();

        } else {

            css.replaces(element);

        }
    }

    if (element.innerHTML.test(/^\s*<a class="wikipage" href="([^"]+)">/)) {

        //%%add-css [some-wikipage] /%
        //read another wiki page, and select all div.page-content div.add-css blocks
        new Request.HTML({
            url: RegExp.$1,
            filter: "div.page-content div.add-css",
            onSuccess: insertStyle,
            evalScripts: false
        }).get();

    } else {

        insertStyle([element]);

    }
}
