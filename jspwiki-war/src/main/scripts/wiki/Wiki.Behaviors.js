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
/*global $$, Wiki, Cookie,
         TableX, GraphBar, Tab, Accordion, Viewer, Collapsible,
         prettyPrint, CommentBox, Columns, Tips, Flip, AddCSS */

/*
Wiki.Behaviours
    Contains all behaviours added by default to JSPWiki.

    Uses global var "Wiki", and a number of Classes.

Depend on :
    moo-extend/Behavior.js
    moo-extend/Color.js
    moo-extend/Element.Extend.js
    moo-extend/HighlightQuery.js
    moo-extend/String.Extend.js
    moo-extend/Tips.js
    moo-extend/Array.NaturalSort.js

    wiki/Wiki.js
    wiki/Category.js

    behaviors/Collapsible.js
    behaviors/Columns.js
    behaviors/CommentBox.js
    behaviors/GraphBar.js
    behaviors/Element.Reflect.js

    behaviors/Tabs.js
    behaviors/Accordion.js

    behaviors/Viewer.js
    behaviors/Viewer.Slimbox.js
    behaviors/Viewer.Carousel.js

    behaviors/TableX.js
    behaviors/TableX.Sort.js
    behaviors/TableX.Filter.js
    behaviors/TableX.Zebra.js
*/

!(function( wiki ){

var TheSlimbox, T = TableX;


/*
Behavior: Broken images
    Replace broken image browser icons
*/
wiki.once( "img:not(outlink)", function(imgs){

    imgs.addEvent("error", function(){

        var img = $(this);
        [ "span.danger.img-error", {
                text: "broken.image".localize()
            },
            [
                "span", { text: img.alt || img.src }
            ]
        ].slick().replaces(img);

    });

});

/*
Behavior: GraphBars, Progress-bars

%%progress-red-striped 50/%  =>  %%graphBars-progress-red-striped-minv0-maxv100 %%gBar 50/% /%
%%progress-red-striped 50/75 /%  =>  %%graphBars-progress-red-striped-minv0-maxv75 %%gBar 50/% /%

%%progress 50 /%
%%progress 50/75 /%
%%progress-inside 50 /%
%%progress-after 50 /%
%%progress-red 50 /%
%%progress-red-striped 50 /%
*/
wiki.add("*[class^=progress]", function(element){

    var maxv = 100,
        value = element.innerHTML.trim(),
        clazz = ".graphBars-" + element.className;

    //also support  "value / max-value"  format
    if( /^(\d+)\s*\/\s*(\d+)$/.test(value) ){
        element.innerHTML = RegExp.$1;
        maxv = RegExp.$2;
    }

    ( element.get("tag") + clazz + "-minv0-maxv" + maxv  ).slick().wraps(element);
    element.className = "gBar";

    })

/*
Behavior: pie

Credit: Lea Verou,  Static Pie

%%pie 20% /%
%%pie-red-blue 20% /%
%%pie-red-blue-r50 20% /%

*/
    .add(".pie", function(pie){

        pie.style.animationDelay = '-' + parseFloat(pie.textContent) + 's';
        pie.setAttribute('data-percent', parseFloat(pie.textContent)+"%");

    })

/*
Behavior:%%graphBar .. /%
*/
    .add("*[class^=graphBars]", GraphBar )


/*
Behavior:tabs & pills
>   %%tabbedSection .. /%
>   %%tabs .. /%
>   %%pills .. /%
*/
    .add(".tabbedSection,.tabs", Tab )
    .add(".pills", Tab, { nav: "ul.nav.nav-pills" } )

/*
Behavior:Accordion
>   %%accordion .. /%
>   %%leftAccordion .. /%
>   %%left-accordion .. /%
>   %%rightAccordion .. /%
>   %%right-accordion .. /%
>   %%tabbedAccordion .. /%
>   %%tabbed-accordion .. /%
>   %%pillsAccordion .. /%
>   %%pills-accordion .. /%
*/
    .add("[class^=accordion]", Accordion)
    .add("[class^=leftAccordion],[class^=left-accordion]", Accordion, { type: "pills", position: "pull-left" })
    .add("[class^=rightAccordion],[class^=right-accordion]", Accordion, { type: "pills", position: "pull-right" })
    .add(".tabbedAccordion,.tabbed-accordion", Accordion, { type: "tabs" })
    .add(".pillsAccordion,.pills-accordion", Accordion, { type: "pills" })

/*
Behavior: Categories
>   %%category .. /%
*/
    .add( ".category a.wikipage", function(element) {

        new wiki.Category(element, wiki.toPageName(element.href), wiki.XHRCategories);

    })

/*
Behavior:Alert (based on Bootstrap)
>   %%alert .. /%
*/
    .add(".alert", function(element){

        element.addClass("alert-dismissable").appendChild(

            "button.close[type=button][html=&times;]".slick()
                .addEvent("click", function(){
                    element.remove();

                }),
            "top"

        );

    })

/*
Behavior: Quote (based on Bootstrap)
>   %%quote .. /%
*/
    .add(".quote", function(element){

        "blockquote".slick().wraps( element );

    })


    .add(".caps", function(element){

        element.mapTextNodes( function(s){ return s.toLowerCase(); });

    })


/*
Behavior: Viewer
>     %%viewer [link to youtube, vimeo, some-wiki-page, http://some-external-site ..] /%
>     [description | url to youtube... | class="viewer"]
*/
    .add("a.viewer, div.viewer a, span.viewer a", function( a ){

        Viewer.preload(a.href, { width: 800, height: 600 }, function( element ){

            var next = a.getNext();
            if( next && next.matches("img.outlink") ){ next.remove(); }

            element.addClass("viewport").replaces(a);

        });
    })


    .add(".maps,.map", function( map ){

        var address = map.textContent.trim(),
            //mapSvc = map.className.replace("-maps","").replace(/maps?/,"google"),
            url = "https://maps.google.com/maps?q=" + encodeURIComponent( address );

        Viewer.preload(url, { width: 800, height: 600 }, function( element ){

            element.addClass("viewport").replaces(map);

        });
    });



/*
Behavior: Viewer.Slimbox
    Injects slimbox button, after each link inside the %%slimbox container.
    The slimbox button opens a modal overlay box with a rich media viewer.
    When the %%slimbox container encloses multiple links, "next" and "previous" buttons
    are added to navigate between all media.

Example:
>    %%slimbox [any supported link] /%
>    [link-description | link-url | class="slimbox"]

DOM structure:

JSPWiki support attachment links (with paperclip), inline images and external links.
Notice how inline images are converted to attachement links.
(start code)
    div.slimbox
        a.attachment[href="url.(png|bmp|tiff|jpg|jpeg|gif)"] Image link
        a.infolink[href="url]
            img[src=attachment_small.png]   (small jspwiki paperclip)

        img.inline[src="url2"]

        a.external[href="url3"] External link
        img.outlink[src=out.png]
(end)
becomes
(start code)
    div.slimbox
        a.attachment[href="url1"] Image link
        a.slimboxbtn[href="url1"][title=Image link] &raquo;
        a.infolink[href="url]
            img[src=attachment_small.png]   (small paperclip)

        a.attachment[href="url2"] url2
        a.slimboxbtn[href="url2"][title=url2] &raquo;

        a.external[href="url3"] External link
        a.slimboxbtn[href="url3"][title=External link]
        img.outlink[src=out.png]
(end)

Example of the short notation with the .slimbox class
>    a.slimbox[href="url"] Link
becomes
>    a.slimbox[href="url"] Link

*/

//helper function, to collect the links to be converted
function filterJSPWikiLinks(element){

    return element.matches("a") ?
        element :
        element.getElements( element.matches(".slimbox-attachments") ?
            "a[href].attachment" :
            // otherwise,  catch several different cases in one go
            //    img:not([href$=/attachment_small.png]):not(.outlink)  ::jspwiki small icons
            //    img:not([src$=/attachment_small.png]):not(.outlink)  ::jspwiki small icons
            //    a[href].attachment,
            //    a[href].external,
            //    a[href].wikipage,
            //    a[href].interwiki
            //    .recentchanges td:not(:nth-child(3)) a:first-child
            "img:not([href$=/attachment_small.png]):not([src$=/attachment_small.png]):not(.outlink),a[href].attachment,a[href].external,a[href].wikipage, a[href].interwiki, .recentchanges td:not(:nth-child(3n)) a:first-child"
        );
}

wiki.once("body", function( /*elements*/ ){

        //create singleton TheSlimbox
        TheSlimbox = new Viewer.Slimbox({
            hints: {
                //use defaults as much as possible
                btn: "slimbox.btn".localize(),
                size: "slimbox.size".localize()
            }
        });
    })

    // [ link-description | link-url | class="slimbox-link" ]
    // replaces the link by a slimbox-link
    .add("a.slimbox-link", function( element ){

        TheSlimbox.watch(element);

    })

    .add(".slimbox-attachments,*[class~=slimbox],*[class~=lightbox]", function( element ){

        var arr = filterJSPWikiLinks( element );

        TheSlimbox.watch(arr, "button.slimbox-btn");

        //jspwiki -- replace inlined images by attachment links
        $$(arr).filter("img[src]").each( function( img ){

            "a.attachment".slick({
                href: img.src,
                html: img.title || img.alt || img.src
            }).replaces( img );
        });

        /*FFS: replacing img[src], should also add the info paperclip
              .grab( [
                    "a.infolink",{href:element.src},[
                        "img[alt="(info)"]",{src:".../attachment_small.png"}
                        ]
                    ].slick()
                )
        */

    })

/*
Behavior: Viewer.Carousel (embed auto-rotating media viewer into a wiki page)
> %%carousel [link-1] [link-2] .. [link-n]/%  =>  carousel viewer next,previous
> %%carousel-auto [link-1] [link-2] .. [link-n]/%  =>  with auto-rotation
*/
    .add( ".carousel", function( element ){

        new Viewer.Carousel( filterJSPWikiLinks( element ), {
            container: element
        });


    });


/*
Behavior: Collapsible.Box, Collapsible.List
    Create collabsible boxes and (un)ordered lists.
    The collapse status (open/close) is persisted in a cookie.

Depends on:
    Wiki, Cookie, Cookie.Flag, Collapsible, Collapsible.Box, Collapsible.List

>    %%collapse
>    %%collapsebox
>    %%collapsebox-closed
*/

//helper function
function collapseFn(elements, pagename){

    new Collapsible( elements, {
        cookie: {
            name: "JSPWiki.Collapse." + (pagename || wiki.PageName),
            path: wiki.BaseUrl,
            duration: 20
        }
    });

}

wiki
    .once(".page div[class^=collapse]", collapseFn )
    .once(".sidebar div[class^=collapse]", collapseFn, "Sidebar")

/*
Behavior:Comment Box

Wiki Markup:
(start code)
    %%commentbox .. /%
    %%commentbox-Caption .... /%
    %%commentbox
        !Caption
        ..
    /%
(end)
*/
    .add("div[class^=commentbox]", CommentBox, { prefix: "commentbox" } )


/*
Behavior:Columns

>    %%columns .. /%
*/
    .add( "div[class^=columns]", Columns, { prefix: "columns" } )

/*
Dynamic Style: Code-Prettifier
    JSPWiki wrapper around http://google-code-prettify.googlecode.com/svn/trunk/README.html

    TODO: add option to set the choice of language:
    >    "bsh", "c", "cc", "cpp", "cs", "csh", "cyc", "cv", "htm", "html",
    >    "java", "js", "m", "mxml", "perl", "pl", "pm", "py", "rb", "sh",
    >    "xhtml", "xml", "xsl"

Example:
>    %%prettify {{{
>        some code snippet here ...
>    }}} /%

*/
    .add("div.prettify pre:not(.prettyprint), div.prettify code:not(.prettyprint)", function(element){

        //brute-force line-number injection
        "div".slick().wraps(element).grab(
            "pre.prettylines".slick({

                html: element.innerHTML.trim().split("\n").map( function(line, i){
                    return i + 1; }
                ).join("\n")

            }),"top");

        element.addClass("prettyprint");
        /*html5 expects  <pre><code>  */
        if( element.matches("pre") ){
            element.innerHTML = "<code>" + element.innerHTML + "</code>";
        }

    })
    .add("[class~=prettify-nonum] pre:not(.prettyprint), [class~=prettify-nonum] code:not(.prettyprint)", function(element){

        element.addClass("prettyprint");
        /*html5 expects  <pre><code>  */
        if( element.matches("pre") ){
            element.innerHTML = "<code>" + element.innerHTML + "</code>";
        }

    })

    .once(".prettyprint", prettyPrint)  //after element.prettyPrint decoration, prettify them


/*
Behavior:Reflection for images
>    %%reflection-30-50    //size of reflection images is 30% height by 50% wide
*/
    .add( "[class^=reflection]", function(element){

        var args = "reflection".sliceArgs( element );

        if( args ){
            element.getElements("img").reflect({
                height: args[0] / 100,
                width: args[1] / 100
            });
        }

    })

/*
Behavior: Table behaviors

(start code)
    %%zebra ... /%              => default odd row colors (light grey)
    %%zebra-table ... /%     => default odd row colors (light grey)
    %%zebra-eee ... /%      => odd rows get backgroundcolor #eee
    %%zebra-pink ... /%      => odd rows get backgroundcolor red
    %%zebra-eee-red ... /%     => odd rows: #eee, even rows: red

    %%table-striped-bordered-hover-condensed-fit-filter-sort-noborder
    %%sortable .. /%
    %%table-filter .. /%

    FFS %%table-scrollable  (keep head fixed, max height for the table)
(end)

*/
    .add(".zebra,div[class|=zebra]", function(element){

        var args = "zebra".sliceArgs(element);

        element.getElements("table").each(function(table){
            //console.log("zebra", args, table);
            new T.Zebra(table, { colors: args });
        });

    })

    .add(".sortable,div[class*=table-]", function(element){

        element.ifClass(element.matches(".sortable"), "table-sort");

        var args = "table".sliceArgs(element),
            arg,
            tables = element.getElements("table:not(.imageplugin)"),
            hints = Object.map({
                sort: "sort.click",
                atoz: "sort.ascending",
                ztoa: "sort.descending"
            }, String.localize);

        while( args && args[0] ){

            arg = args.shift();

            if( arg.test("striped|bordered|hover|condensed|fit|noborder")){

                tables.addClass("table-"+arg);

            }

            else if( arg == "filter" ){

                tables.each( function(t){ new T.Filter(t, {hint: "filter.hint".localize() /*list:["predef1"...]*/}); });

            }

            else if( arg == "sort" ){

                element.addClass("sortable");
                tables.each( function(t){ new T.Sort(t, {hints: hints}); });

            }

        }

    })


/*
Behavior: Scrollable pre area with maximum size (based on BOOTSTRAP)

>   %%scrollable {{{ ... }}}        //max-height=240px (default)
>   %%scrollable-150 {{{ ... }}}    //max-height=150px

*/
    .add("[class|=scrollable]", function(element){

        var maxHeight = "scrollable".sliceArgs(element)[0] || "240";

        element.getElements("pre")
            .addClass("pre-scrollable")
            .setStyle("maxHeight", maxHeight + "px");

        //FFS : support scollable > table

    })

/*
Behavior: Font Icon style (based on BOOTSTRAP)
    Convert .icon-<icon-name> into appropriate class-name depending on the font family

    //Glyphicon :  .glyphicon.glyphicon-<icon-name>
    //Font-Awesome: .fa.fa-<icon-name>
    FontJspwiki (via icomoon) : .icon-<icon-name>
*/
/*
    .add("[class^=icon-]", function( element ){

        //element.className="glyphicon glyph"+element.className;
        //element.className = "fa fa-"+element.className.slice(5);

    })
*/
/*
Behavior: List (based on BOOTSTRAP)

>   %%list-unstyled-hover-group-nostyle

*/
    .add("[class*=list-]", function(element){

        var args = "list".sliceArgs(element),
            lists = element.getElements("ul|ol");

        if( !args ) return;
        args.each( function( arg ){

            if( arg.test("unstyled|hover|group|nostyle") ){

                lists.addClass( "list-"+arg );

            }

            if( arg.test("group") ){

                lists.each( function(item){
                    item.getElements("li").addClass("list-group-item");
                });

            }
        });
    })

/*
Behavior: Labels (based on Bootstrap)
    Support %%label, %%label-default, %%label-primary, %%label-info, %%label-success; %%label-warning, %%label-danger
*/
    .add("[class^=label]", function(element){

        element.addClass( "label".fetchContext(element) );

    })

/*
Behavior: Tips
    Add mouse-hover Tips to your pages. Depends on Mootools Tips plugin.

Wiki-markup:
    > %%tip ... /%
    > %%tip-Caption ... /%

DOM Structure:
(start code)
    //before
    div.tip-TipCaption ...tip-body...

    //after
    a.tip-link  Tip Caption
        div.tip-TipCaption ...tip-body...

(end)
*/
    .once("span[class^=tip],div[class^=tip]", function(tips){

        var caption, more = "tip.default.title".localize();

        tips = tips.map( function(tip){

            caption = (tip.className.split("-")[1] || more).deCamelize();
            return "a.tip-link".slick({ text: caption }).wraps(tip);

        });

        Tips( tips ); //activate tips behavior

    })

/*
Behavior: Magnify
    Add magnifying image glass

Wiki-markup:
    > %%magnify <img> /%
    > [{Image src='...' class='magnify' }]

*/
    .once(".magnify img", Magnify)


/*
Behavior: DropCaps
    Convert the first character of a paragraph to a large "DropCap" character

>    %%dropcaps .. /%

*/
    .add("div.dropcaps", function(element){

        var content, node = element.firstChild;

        if( node.nodeType == 3 ){   // aha, this is a text-node

            content = node.textContent.trim();
            node.textContent = content.slice(1);  //remove first character
            //and inject the dropcap character in front of the content
            "span.dropcaps".slick({text: content.slice(0, 1)}).inject(element, "top");

        }

    })

/*
Behavior: Add-CSS

>   %%add-css ... /%
>   %%add-css [some-remote-wiki-page] /%
*/
    .add(".add-css", AddCSS)


/*
Behavior: Invisibles
    Show hidden characters such as tabs and line breaks.
    Credit: http://prismjs.com/plugins/show-invisibles/

CSS:
(start code)
.token.tab:not(:empty):before,
.token.cr:before,
.token.lf:before { color: hsl(24, 20%, 85%); }

.token.tab:not(:empty):before { content: '\21E5'; }
.token.cr:before { content: '\240D'; }
.token.lf:before { content: '\240A'; }
(end)
*/
    .add(".invisibles pre, .reveal pre", function(element){

        var token = "<span class='token {0}'>$&</span>";

        element.innerHTML = element.innerHTML
            .replace( /\t/g, token.xsubs("tab") )
            .replace( /\n/g, token.xsubs("lf") )
            .replace( /\r/g, token.xsubs("cr") );

    })


/*
wiki-slides

*/
    .once(".page-content.wiki-slides", function(elements){

        var divider = "hr";

        elements
            .grab(divider.slick(), "top") //add one extra group-start-element at the top
            .groupChildren(divider, "div.slide");

    })


/*
Behviour:  Background
    Move image to the background of a page.
    Also support additional image styles on background images.

Case1:
div[this is the parent container]
    img.bg[src=<imageurl>]
    ...
    div other content
    ...

Case2:
div[this is the parent container]
    table.imageplugin
        tr
            td.bg
                img[src=<imageurl>]
    ...
    div other content
    ...

Case3:
div[this is the parent container]
    div.bg
        img[src=<imageurl>]
    ...
    div other content
    ...


After
div[this is the parent container]
    span.background[background-image=<image-url>]
    div.background-overlay[z-index=2]
        ...
        div other content
        ...


%%bg [<image link>] /%
%bg [{IMAGE src='<image link>' }]/%
[{IMAGE src='<image link>' class='bg' }]

%%bg-image.bg-fixed [<image link>] /%
[{IMAGE src='<image link>' class='bg-image bg-fixed' }]

*/
    .add(".bg > table.imageplugin img, .bg > img", function( image ){

        var bgBox = image.getParent(".bg"),
            clazz = bgBox.className; //contains possibly other styles to be applied to the background image

        if( bgBox && bgBox.matches("td") ){
            bgBox = bgBox.getParent("table");
        }

        if( bgBox ){

            bgBox
                .addClass("bg")   //need .bg as trigger for groupChildren() !
                .getParent()      //move up to the containing element
                .addClass("has-background")
                .groupChildren(".bg", "div.bg-overlay.clearfix", function(wrapper, bg){

                    //use a extra container span to allow additional effects
                    //on the background image without impact on the overlay content ...
                    var element = "span".slick();
                    element.className = clazz;
                    element.style.backgroundImage = "url(" + image.src + ")";
                    element.inject(bg, "before");

            });
            //bgBox.destroy();   //not really needed as per default css the .bg  element is hidden
            //bgBox.parentNode.removeChild(bgBox);
        }

    })

/*
Behvior:  Image Caption

DOM Structure

Case1
from::
    div.caption(-arrow)(-overlay).other-class
        img.inline[src='...']
        caption-text

to::
    figure.caption(-arrow)(-overlay).other-class
        figcaption.other-class
            caption-text
        img.inline[src='...']


Case2
from::
    div.caption(-arrow)(-overlay).other-class
        table.imageplugin
            tr
                td
                    img[src='...']
        caption-text

to::
    div.caption(-arrow)(-overlay)
        table.imageplugin
            caption.other-class
                caption-text
            tr
                td
                    img[src='...']

*/
    .add("[class^=caption] > .imageplugin", function( imageplugin ){

        var caption = imageplugin.getParent(),
            oldcaption = imageplugin.getFirst("caption");

        if( !oldcaption ){

            imageplugin.wraps(caption,"top");

            "caption".slick({
                html: caption.innerHTML,
                "class": caption.className
            }).replaces(caption);

        }

    })
    .add("[class^=caption] > img.inline", function( img ){

        var caption = img.getParent();

        "figure".slick().grab(img).wraps(caption,"top");

        "figcaption".slick({
            html: caption.innerHTML,
            "class": caption.className
        }).replaces(caption);

    })

/*
Experimental
svg pie,
credit: lea verou

*/
    .add(".pie2", function( pie ){

        var p = parseFloat(pie.textContent),
            NS = "http://www.w3.org/2000/svg",
            svg = document.createElementNS(NS, "svg"),
            circle = document.createElementNS(NS, "circle"),
            title = document.createElementNS(NS, "title");

        circle.setAttribute("r", 16);
        circle.setAttribute("cx", 16);
        circle.setAttribute("cy", 16);
        circle.setAttribute("stroke-dasharray", p + " 100");

        svg.setAttribute("viewBox", "0 0 32 32");
        title.textContent = pie.textContent;
        pie.textContent = "";
        svg.appendChild(title);
        svg.appendChild(circle);
        pie.appendChild(svg);

    })

/*
Behavior:Flip, Flop

>    %%flip(-w<idth>-h<eight>-primary-info-success-warning-danger-error) .. /%
>    %%flop(-w<idth>-h<eight>-primary-info-success-warning-danger-error) .. /%
*/
    .add( "div[class|=flip]", Flip, { prefix: "flip" } )
    .add( "div[class|=flop]", Flip, { prefix: "flop" } );


})( Wiki );
