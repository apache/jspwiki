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
Dynamic style: %%commentbox

Example:
>  %%commentbox ... /% : floating box to the right
>  %%commentbox-Caption .... /% : commentbox with caption

DOM structure
(start code)
    div.commentbox
        h2|h3|h4 title
        ..body..

    //becomes, based on BOOTSTRAP Panels
    div.panel.panel-default
        div.panel-header
        div.panel-body
(end)
*/
function CommentBox(element, options){

    var header = element.firstElementChild,
        caption = options.prefix.sliceArgs(element)[0],
        panelCSS = "panel".fetchContext(element);

    element.className="panel-body"; //reset className -- ie remove commentbox-...
    "div.commentbox".slick().addClass(panelCSS).wraps(element);

    if( caption ){

        caption = "h4".slick({ text:caption.deCamelize() });

    } else if( header && header.matches("h2,h3,h4") ) {

        caption = header;
    }

    if( caption ){

        "div.panel-heading".slick()
            .grab(caption.addClass("panel-title"))
                .inject(element, "before");

    }

}