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
//Wiki.AddCSS function( element ){
function AddCSS( element ){

    function insertStyle ( elements ){

        var css = "", item;

        //collect all css to be inserted
        while( item = elements.shift() ){ css += item.innerHTML; }

        //magic to replace the inline wiki-image links to css url()
        //xss protection: remove invalid url's;  only allow url([wiki-attachement])
        css = css.replace( /url\(\<[^i][^)]*\)/gi, "url(invalid)" ); //remove url(<a...)
        css = css.replace( /url\([^<][^)]*\)/gi, "url(invalid)" );  //remove url(xxx)
        css = css.replace( /url\(<img class="inline" src="([^"]+)[^>]+>\)/gi, "url($1)" );

        css = css.replace( /<p>|<\/p>/gi, "" ); //jspwiki inserts <p/> for empty lines

        css = "style[type=text/css]".slick({text: css});

        /*
        Sequence to insert CSS is :
            (1) jspwiki.css (<HEAD>)
            (2) sidebar/favorites
            (3) in-page additional styles

        Because the side-bar is located at the end of the DOM, the additional sidebar styles
        need to be inserted at the top of the DOM, i.e. just at the top of the BODY element.
        Other CCS is injected in the order of appearance.
        */
        if( element.getParent( ".sidebar" ) ){

            $(document.body).grab(css, "top");
            element.destroy();

        } else {

            css.replaces( element );

        }

    };

    if( element.innerHTML.test( /^\s*<a class="wikipage" href="([^"]+)">/ ) ){

        //%%add-css [some-wikipage] /%
        //go and read the %%add-css blocks from another remote page -- how hard is that ?
        //then filter all div.page-content div.add-css elements

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
