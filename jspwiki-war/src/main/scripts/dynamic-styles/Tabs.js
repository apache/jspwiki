/*
Class: TabbedSection
    Creates tabs, based on some css-class information

Wiki Markup:
(start code)
    //Original markup
    %%tabbedSection
    %%tab-FirstTab
        ...
    /%
    %%tab-FirstTab
        ...
    /%
    /%

    //Simplified markup
    //Every tab is defined by headers tag. (!, !!, !!!)
    //The level of the first header element determines the header-level of all tabs.
    %%tabs
    !First tab title
        ...
    !Second tab title
        ...
    /%
(end)

DOM structure before:
    (start code)
    //original syntax
    div.tabbedSection
        div.tab-FirstTab
        div.tab-SecondTab

    //simplified syntax
    div.tabs (or div.pills)
        h1 FirstTab
        h1 SecondTab
        h1 ThirdTab
    (end)

DOM structure after:  (based on Bootstrap conventions)
(start code)
    ul.nav.nav-tabs(.nav-pills)
        li
            a[href="#home"] FirstTab
        li
            a[href="#profile"] SecondTab
        li
            a[href="#messages"] ThirdTab

    div.tab-content
        div.tab-pane.active[id="FirstTab"] ... 
        div.tab-pane[id="SecondTab"] ...
        div.tab-pane[id="ThirdTab"] ...
(end)
*/
var Tab = new Class({

    Implements: Options,

    options:{
        nav: 'ul.nav.nav-tabs' //navigation section with all tab labels
    },

    initialize:function(container,options){

        this.setOptions( options );

        var panes = this.getPanes( container ),
            pane, id, i=0,
            items=[]; //collection of li for the tab navigation section
            
        while( pane = panes[i++] ){

            items.push('li',[ 'a', {text:this.getName(pane)} ]);   

        };

        if( items[0] ){

            items[0] +='.active';
            
            [this.options.nav, {events:{'click:relay(a)':this.show}}, items]
                .slick()
                .inject(container,'before');            

            panes.addClass('tab-pane')[0].addClass('active');
            container.addClass('tab-content');
        
        }

    },

    getName:function(pane){

        var name = pane.className.slice(4).deCamelize();
        if( !pane.id ) pane.id = name;   //CHECKME : support #<tab-name> urls ; eg h1 id="section-Tabbed+Section-Usage"
        return name;

    },

    /*
    Function: getPanes
        Generic function to collect the tab panes.  Reused by accordions, etc.
        1) panes = sections enclosed by tab-container elements, the tab caption is derived from the classname
        2) panes = sections divided by h<n> elements; 
        
    Arguments:
        container - container DOM element
        isPane - (string) selector to match predefined tab container
    */
    getPanes:function( container ){

        var isPane = '[class^=tab-]',
            first = container.getFirst(),
            header = first.get('tag'),
            hasPane = first && first.match(isPane);  //predefined tab-panel containers

        //avoid double runs -- obsolete, covered by behavior
        //if( first.match('> .nav.nav-tabs') ) return null;

        if( (!hasPane) && ( header.test(/h1|h2|h3|h4/) ) ){     //replace header by tab-panel containers
        
            //first remove unwanted elements from the header
            container.getChildren(header).getElements('.hashlink,.edit-section,.labels')
                .each(function( el ){ el.destroy(); });
                
            //then create div.tab-<pane-title> groups
            container.groupChildren(header, 'div', function(pane,header){    
                pane.addClass( "tab-" + header.get('text').trim().replace(/\s+/g,'-').camelCase() );
                pane.id = header.id;
            });
                           
        }
        return container.getChildren(isPane);

    },
    
    /*
    Click-handler to toggle the visibilities of the tab panes.    
    */
    show:function(event){
    
        var active = 'active',
            nav = this.getParent('ul'),
            index = nav.getElements('a').indexOf(this);

        event.stop();
        nav.getChildren().removeClass(active)[index].addClass(active);
        nav.getNext().getChildren().removeClass(active)[index].addClass(active);

    }

});