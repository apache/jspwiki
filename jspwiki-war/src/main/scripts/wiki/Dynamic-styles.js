/* Dynamic Styles

    Uses global var 'Wiki', and a number of Classes.

*/

!function( wiki ){

var hints, TheSlimbox, T = TableX;


/*
Style: %%graphBar .. /%
*/
wiki.add('div[class^=graphBars]', GraphBar )
    

//FIXME -- OBSOLETE ?? top level TAB of the page
    .add('.page > .tabmenu a:not([href])', Tab )

/*
Style: %%tabbedSection .. /% , %%tabs .. /%, %%pills .. /%
*/
    .add('.tabbedSection,.tabs', Tab )
    .add('.pills', Tab, { nav:'ul.nav.nav-pills' } )

/*
Style: Accordion

>   %%accordion .. /%
>   %%leftAccordion .. /%
>   %%rightAccordion .. /%
>   %%tabbedAccordion .. /%

*/
    .add('[class^=accordion]', Accordion)
    .add('[class^=leftAccordion]', Accordion, { type:'pills', position:'pull-left' })
    .add('[class^=rightAccordion]', Accordion, { type:'pills', position:'pull-right' })
    .add('.tabbedAccordion', Accordion, { type:'tabs' })
    .add('.pillsAccordion', Accordion, { type:'pills' })


/*
Style: %%category .. /%
*/
    .add( '.category a.wikipage', function(element) {

        new Wiki.Category(element, Wiki.toPageName(element.href), Wiki.XHRCategories);

    })

/*
BOOTSTRAP Style: %%alert .. /%
*/
    .add('.alert', function(element){

        element.addClass('alert-warning alert-dismissable').grab(
            'button.close[type="button"][html="&times;"]'.slick()
                .addEvent('click',function(){ element.dispose(); }),
            'top'
        );

    })

/*
BOOTSTRAP Style %%quote .. /%
*/
    .add('.quote', function(element){
        
        'blockquote'.slick().wraps( 'p'.slick().wraps(element));

    })


/*
Plugin: Viewer
>     %%viewer [link to youtube, vimeo, some-wiki-page, http://some-external-site ..] /%
>     [description | url to youtube... | class='viewer']
*/
    .add('a.viewer, div.viewer a', function( a ){

        Viewer.preload(a.href, { width:800, height:600 }, function( element ){

            var next = a.getNext();
            if( next && next.match('img.outlink') ) next.dispose();

            element.addClass('viewport').replaces(a);

        });

    });


/*
Plugin: Viewer.Slimbox
    Injects slimbox button, after each link inside the %%slimbox container. 
    The slimbox button opens a modal overlay box with a rich media viewer.
    When the %%slimbox container contains multiple links, 'next' and 'previous' buttons
    are added to navigate between all media.

Example:
>    %%slimbox [any supported link] /%
>    [link-description | link-url | class='slimbox']

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

//helper function
function filterJSPWikiLinks(element){

    return element.match('a') ? 
        [element] : 
        element.getElements( element.match('.slimbox-attachments') ?
            'a[href].attachment' : 
            //    img:not([src$=/attachment_small.png]):not(.outlink)  
            //    a[href].attachment, 
            //    a[href].external,a[href].wikipage
            'img:not([src$=/attachment_small.png]):not(.outlink),a[href].attachment,a[href].external,a[href].wikipage'
        );
}

wiki.once('body', function( elements ){

        //create singleton TheSlimbox
        TheSlimbox = new Viewer.Slimbox();
        /*TheSlimbox = new Viewer.Slimbox({ 
            hints: {
                //use defaults as much as possible
                //btn: 'Click to view {0}', 
                //caption: ' Direct link to {0}',    
                btn: 'slimbox.btn'.localize(),
                caption: 'slimbox.caption'.localize()
            }
        });*/
    })

    // [ link-description | link-url | class='slimbox-link' ]
    // replaces the link by a slimbox-link
    .add('a.slimbox-link', function( element ){

        TheSlimbox.watch([element]);

    })

    .add('.slimbox-attachments,*[class~=slimbox],*[class~=lightbox]', function( element ){

        var arr = filterJSPWikiLinks(element);
                        
        TheSlimbox.watch(arr, 'button.slimbox-btn');

        //jspwiki -- replace inline images by attachment links
        $$(arr).filter('img[src]').each(function( element ){
            'a.attachment'.slick({
                href:element.src, 
                html:element.title||element.alt
            }).replaces( element );        
        });

        /*FFS: replacing img[src], should also add the info paperclip 
              .grab( [
                    'a.infolink',{href:element.src},[
                        'img[alt="(info)"]',{src:".../attachment_small.png"}
                        ]
                    ].slick()
                )
        */
                    
    })

/*
Plugin: Viewer.Carousel (embed auto-rotating media viewer into a wiki page)
> %%carousel [link-1] [link-2] .. [link-n]/%  =>  carousel viewer next,previous
> %%carousel-auto [link-1] [link-2] .. [link-n]/%  =>  with auto-rotation
*/
    .add( '.carousel', function( element ){

        new Viewer.Carousel( filterJSPWikiLinks( element ), {
            container: element,
        });


    });

  
/*
Plugin: Collapsible.Box, Collapsible.List
    Create collabsible boxes and (un)ordered lists.
    The collapse status (open/close) is persisted in a cookie.
    
Depends on:
    Wiki, Cookie, Cookie.Flag, Collapsible, Collapsible.Box, Collapsible.List

>    %%collapse
>    %%collapsebox
>    %%collapsebox-closed
*/

//helper function
function collapseFn(element, cookie){

        var TCollapsible = Collapsible,
            clazz = element.className,
            list = "collapse",
            box = list+"box";

        cookie = new Cookie.Flags( 
            'JSPWikiCollapse' + (cookie || wiki.PageName), 
            { path:wiki.BasePath, duration:20 }
        );

        if( clazz == list ){

            new TCollapsible.List(element,{ cookie:cookie });

        } else if( clazz.indexOf(box)==0 ){
        
            new TCollapsible.Box(element,{ 
                cookie:cookie, 
                collapsed:clazz.indexOf(box+'-closed')==0 
            });

        }

}

wiki
    .add('.page div[class^=collapse]',collapseFn )
    .add('.sidebar div[class^=collapse]',collapseFn, 'Sidebar')

/*
Style: Comment Box

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
    .add('div[class^=commentbox]', CommentBox, { prefix:'commentbox' } )

        
/*
Style: Columns
>    %%columns(-width) .. /%
*/
    .add( 'div[class*=columns]', Columns, { prefix:'columns' } )

/*
Dynamic Style: Code-Prettifier
    JSPWiki wrapper around http://google-code-prettify.googlecode.com/svn/trunk/README.html

    TODO: add option to overrule the choice of language:
    >    "bsh", "c", "cc", "cpp", "cs", "csh", "cyc", "cv", "htm", "html",
    >    "java", "js", "m", "mxml", "perl", "pl", "pm", "py", "rb", "sh",
    >    "xhtml", "xml", "xsl"

Example:
>    %%prettify {{{
>        some code snippet here ...
>    }}} /%

*/
    .add('div.prettify pre, div.prettify code', function(element){

        element.addClass('prettyprint');

        //brute-force line-number injection
        'pre.prettylines'.slick({
            html: element.innerHTML.trim().split('\n').map(function(line,i){ return i+1 }).join('\n')
        }).inject(element,'before');

    })
    
    .once('.prettyprint', prettyPrint)  //after element.prettyPrint decoration, prettify them


/*
Style: Reflection for images
>    %%reflection-30-50    //size of reflection images is 30% height by 50% wide
*/
    .add('div[class*=reflection]', function(element){

        var args = "reflection".sliceArgs( element );
        
        if(args) element.getElements('img').reflect({
            height:args[0]/100,
            width:args[1]/100
        });

    })
    
/*
Dynamic Style: %%sortable, %%table-filter, %%zebra

>    %%zebra ... /%              => default odd row colors (light grey)
>    %%zebra-table ... /%     => default odd row colors (light grey)
>    %%zebra-eee ... /%      => odd rows get backgroundcolor #eee
>    %%zebra-pink ... /%      => odd rows get backgroundcolor red
>    %%zebra-eee-red ... /%     => odd rows: #eee, even rows: red
*/
    .add('.sortable table', T.Sort, {hints: 
        Object.map({
            sort: "sort.click",
            atoz: "sort.ascending",
            ztoa: "sort.descending"
        },String.localize)
    })

    .add('.table-filter table', T.Filter, {
        hint:"filter.hint".localize()
    })
    /*
    .add('.table-filter table', function(element){
        new T_TableX.Filter(element,{ /--list:['one$','James'],--/ hint:hints.filter});
      })
    */

    .add('.zebra,div[class*=zebra]', function(element){
    
        var args = 'zebra'.sliceArgs(element);
        element.getElements('table').each(function(table){
            new T.Zebra(table, { colors:args });
        });
        
    })

/*
TODO
Combined table styling
%%table-striped-bordered-hover-condensed-filter-sort-<color>
%%sortable .. /%
%%table-filter .. /%
%%zebra-table .. /%
FFS %%table-scrollable  (keep head fixed, max height for the table)

    .add('div[class^=table-]',function(element){

        var args = 'table'.sliceArgs(element), arg,
            tables = element.getElements('table'),
            hints =  Object.map({
                sort: "sort.click",
                atoz: "sort.ascending",
                ztoa: "sort.descending",
                filter: "filter.hint"
            },String.localize);
        
        while(args[0]){

            arg = shift(args);
            
            if( arg.test('striped|bordered|hover|condensed'){
                tables.addClass('table-'+arg);
            } 
            else if( arg == 'filter' ){
                tables.each( function(t){ new T.Filter(t, {hint:hints.filter}); });
            }
            else if( arg == 'sort' ){
                tables.each( function(t){ new T.Sort(t, {hints:hints}); });
            }

        }
    
    })

*/


/*
Add BOOTSTRAP Styles
    Scrollable pre area's
*/
    .add('div.scrollable > pre', function(element){ 

        element.addClass('pre-scrollable');  //bootstrap class

    })


    .add('*[class^=list]', function(element){ 
    
        var args = "list".sliceArgs(element),
            lists = element.getElements("ul|ol");

        args.each( function( arg ){
            if( arg.test('unstyled|hover|group|nostyle') ){ 
                lists.addClass( 'list-'+arg );
            } 
            if( arg.test('group') ){ 
                lists.each( function(item){ 
                    item.getElements('li').addClass('list-group-item'); 
                });
            }
        });

    })

/*
    Labels
    Support %%label, %%label-default, %%label-primary, %%label-info, %%label-success; %%label-warning, %%label-danger
*/
    .add('*[class^=label]',function(element){

        element.addClass( 'label'.fetchContext(element) );

    })


/*
Plugin: Tips
    Add mouse-hover Tips to your pages. Depends on Mootools Tips plugin.

Wiki-markup:
    > %%tip ... /%
    > %%tip-Caption ... /%

DOM Structure:
(start code)
    //before
    div.tip-TipCaption ...tip-body... 

    //after
    a.tooltip-anchor  Tip Caption
        div.tip-TipCaption ...tip-body...

(end)
*/
    .once('*[class^=tip]', function(tips){

        var caption, more = 'tip.default.title'.localize();

        tips = tips.map( function(tip){

            caption = (tip.className.split('-')[1]||more).deCamelize();
            return 'a.tip-link'.slick({ text: caption }).wraps(tip);

        });
        
        Tips( tips ); //activate tips behavior

    });


}( Wiki );
