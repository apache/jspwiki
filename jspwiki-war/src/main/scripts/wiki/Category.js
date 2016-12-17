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
Class: Categories
    Turn wikipage links into AJAXed popups.

Depends:
    Wiki

DOM structure before:
    (start code)
    span.category
        a.wikipage Category-Page
    (end)

DOM structure after:
    (start code)
    div|span.category
        span
            a.wikipage.category-link[href=".."] Category-Page
            div.popup (.hidden|.loading|.active)
                div.title
                    a.wikipage[href=".."]Category-Page
                ul
                    li
                        a
                br
                a.morelink ..and x more
                br
    (end)
*/

Wiki.Category = function(element, pagename, xhrURL){

    function poppy(event){

        var popup = this.getNext();
        event.stop();
        popup.swapClass("hide", "loading");
        element.set("title", "").removeEvents();

        new Request.HTML({
            url: xhrURL, //+"?page="+pagename,
            data: { page: decodeURIComponent(pagename) },
            update: popup,
            onSuccess: function(){
                popup.swapClass("loading", "active");
            }
        }).send();
    }

    ["span", ["div.popup.hide"]].slick().wraps(element, "top");

    element.set({
        "class": "category-link",
        title: "category.title".localize( pagename ),
        events: { click: poppy }
    });

};
