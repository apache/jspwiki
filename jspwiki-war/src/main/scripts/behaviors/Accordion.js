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
Class: Accordion
    Add accordion effects
    - type=accordion : vertical collapsible panes, with toggle buttons on top of each pane
    - type=tabs : vertical collapsible, with tab-like nav buttons on top
    - type=pills : vertical collapsible panes, with pill-like nav buttons left or right
            from the panes

    The styling is based on the panel component of the bootstrap framework.

DOM structure:
(start code)
    //before invocation
    div.accordion
        div.tab-FirstTab ..
        div.tab-SecondTab ..

    //accordion
    div.panel-group.accordion : panel headings are toggles
        div.panel.panel-default
            div.panel-heading.actie
            div  => fx.accordion collapsible content
                div.panel-body
        div.panel.panel-default
            div.panel-heading  => toggle
            div  => fx.accordion collapsible content
                div.panel-body

    //tabbedAccordion : tab toggles, panels without border
    ul.nav.nav-tabs
        li
            a
    div.panel-group.tabbedAccordion
        div.active  => fx.accordion collapsible content
            div.panel-body
        div  => fx.accordion collapsible content
            div.panel-body

    //leftAccordion : pill-toggles, panels with border
    ul.nav.nav-pills.pull-left
        li
            a
    div.panel-group.leftAccordion
        div  => fx.accordion collapsible content
            div.panel.panel-default.panel-body
        div  => fx.accordion collapsible content
            div.panel.panel-default.panel-body

    //rightAccordion : pill-toggles, panels with border
    ul.nav.nav-pills.pull-right
        li
            a
    div.panel-group.leftAccordion
        div  => fx.accordion collapsible content
            div.panel.panel-default.panel-body
        div  => fx.accordion collapsible content
            div.panel.panel-default.panel-body

(end)
*/
var Accordion = new Class({

    Implements: Options,
    Extends: Tab,

    options: {
        //type: "accordion"   //accordion,tabs,pills
        //closed:  boolean  -- initial status of the accordion
        //position: "pull-left" or "pull-right"  (only relevant with "pills")
    },

    initialize: function(container, options){

        var panes = this.getPanes( container ),
            nav, pane, name, toggle, type, position,
            i = 0,
            active = "active",
            toggles = [],
            contents = [],
            panelCSS = "panel".fetchContext(container);

        this.setOptions(options);

        type = this.options.type;
        position = this.options.position;

        if( position ){ position = ".nav-stacked." + position; }

        nav = (type == "tabs") ? "ul.nav.nav-tabs" :
               (type == "pills") ? "ul.nav.nav-pills" + (position || "") :
                 false;

        this.options.closed = !nav && /-close/.test( container.className );

        if( nav ){ nav = nav.slick().inject(container, "before"); }
        container.addClass("panel-group");

        //modify the DOM
        while( pane = panes[i++] ){

            name = this.getName(pane);
            if( nav ){ //tabs or pills style accordion

                nav.grab( toggle = ["li", [ "a", {text: name, id: String.uniqueID()} ]].slick() );
                if( type == "pills" ) { pane.addClass( panelCSS ); }

            } else {  //standard accordion

                toggle = "div.panel-heading".slick({ html: name, id: String.uniqueID() });
                "div".slick({"class": panelCSS}).wraps( pane ).grab( toggle, "top" );

            }

            toggles[toggles.length] = toggle;
            contents[contents.length] = "div".slick().wraps( pane.addClass("panel-body") );

        }

        //invoke the Accordion animation
        new Fx.Accordion( toggles, contents, {

            //height: true,
            display: this.options.closed ? -1 : 0, // initial display status
            alwaysHide: !nav, //allow closing all panes
            initialDisplayFx: false, //do not show effect on initial display

            onComplete: function(){
                var el = $(this.elements[this.current]);
                if(el.offsetHeight > 0){ el.setStyle("height", "auto"); }
            },

            onActive: function(toggle, content){

                toggle.addClass(active);
                content.addClass(active);

            },
            onBackground: function(toggle, content){

                toggle.removeClass(active);
                content.removeClass(active);

            }

        });

    }

});
