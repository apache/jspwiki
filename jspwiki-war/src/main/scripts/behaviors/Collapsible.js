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
/*eslint-env browser */
/*global $ */

/*
Class: Collapsible
    Implement collapsible lists and collapsible boxes.
    The state is stored in a cookie.
    Keyboard navigation is supported: press the spacebar or enter-key to toggle  a section.
    Expanding or collapsing sections can be animated. (with css support)

Options:
    elements -
    cookie - (optional) store the state of all collapsibles to a next page load

Usage:
> new Collapsible( $$(".page div[class^=collapse]"),{  cookie:{name:...} })
*/

!function () {

    var _UID = 0,
        _CollapseButton = "button.collapse-btn",
        _CollapseBodyClass = "collapse-body",
        _ClosedStateClass = "closed",
        _AriaExpanded = "aria-expanded";

    /*
    Collapsible list

    DOM structure BEFORE:
    (start code)
        div.collapse
            ul
                li
                    List-item-text
                    ul
                        li ...
    (end)

    DOM structure AFTER:
    (start code)
        div.collapse
            ul
                li
                  button.collapse-btn#UID List-item-text
                  ul.collapse-body
                    li ...
    (end)
    */
    function buildCollapsibleList(li) {

        var collapseBody = li.getElement("> ul,> ol");

        if (collapseBody && collapseBody.firstChild) {

            li.ifClass(collapseBody.matches("ol"), _ClosedStateClass);
            collapseBody.addClass(_CollapseBodyClass);
        }
        return li;
    }

    /*
    Collapsible box

    DOM structure BEFORE:
    (start code)
        div.collapsebox
          h4 title
          ... body ...
    (end)

    DOM structure AFTER:
    (start code)
        div.collapsebox
          button.collapse-btn#UID
          h4 title
          div.collapse-body
            ... body ...
    (end)
    */
    function buildCollapsibleBox(el) {

        var header = el.firstElementChild,
            next,
            collapseBody;

        if (header && header.nextSibling) {

            collapseBody = ("div." + _CollapseBodyClass).slick();
            while ((next = header.nextSibling)) { collapseBody.appendChild(next); }

            if (collapseBody.textContent.trim() == '') return el;

            el.appendChild(collapseBody); //append after the header

            if (el.className.test(/-closed\b/)) { el.addClass(_ClosedStateClass); }
            //if( el.hasClass("closed"){ ... }
        }
        return el;
    }

    /*
    A11Y
    */
    function setAriaExpanded(el, state) {

        el.setAttribute(_AriaExpanded, state);
    }

    /*
    Function: addCollapseToggle(el, index)
        Add a collapse BUTTON to the dom and set the proper event handlers

    DOM structure AFTER:
    (start code)
        <ELEMENT el>
          label#UID.collapse-label   #text-content
          <ELEMENT>.collapse-body
            ...
    */
    function addCollapseToggle(el, index) {

        var id = this.UID + index,
            isCollapsed = el.hasClass(_ClosedStateClass),
            collapseBody = el.getElement("> ." + _CollapseBodyClass),
            button;

        if (this.flags[index]) { isCollapsed = (this.flags[index] == 'T'); }
        this.flags[index] = isCollapsed ? "T" : "F";

        //put the label with open/close collapse icon (in css)
        //$.create( _CollapseButton, {id:id, disabled:!collapseBody, start:el});
        button = _CollapseButton.slick({ id: id });
        button.disabled = !collapseBody;
        el.insertBefore(button, el.firstChild);

        if (collapseBody) {

            button.addEvent("click", toggle.bind(this));
            button.addEvent("keydown", keyToggle);

            if (isCollapsed) { collapseBody.style.height = 0; }
            setAriaExpanded(collapseBody, !isCollapsed);
            setAriaExpanded(button, !isCollapsed);

            collapseBody.addEvent("transitionend", animationEnd);
        }
    }

    /*
    EventHandler: keyToggle
        Collapse/Expand the body by pressing the spacebar or return-key on a collapse button with :focus
    */
    function keyToggle(ev) {

        var code = ev.keyCode;

        if (code === 32 || code === 13) {

            ev.preventDefault();
            ev.target.click();  //trigger button
        }
    }

    /*
    EventHandler: animationEnd
        Runs after completing the "height" animation on the collapseBody.

        - if state=collapsed,  (height =  0px)
            aria-expanded is still true,
            now set the aria-expanded to false, which sets "display:none" (in the css)
            to make sure the nested buttons, links etc. are not reachable anymore via te keyboard

        - if state = expanded,  (height = n px)
            aria-expanded is already true
            now remove "height" from the inline style to return back to 'auto' height
    */
    function animationEnd() {

        if (this.style.height == "0px") {

            setAriaExpanded(this, false); //finalize the collapsed state of the body

        } else {

            this.style.height = null;

        }
    }

    /*
    EventHandler: toggle(event)
        Store the new state in a cookie; and make sure all animations work.
    */
    function toggle(ev) {

        var button = ev.target,
            collapseBody = button.getElement("~ ." + _CollapseBodyClass), //get next-child .collapse-body
            collapseBodyTransition = collapseBody.style.transition,
            isExpanded = !collapseBody.style.height; //height = null(expanded) || 0px (collapsed)


        function animateHeight(animate2steps, collapseMe) {

            requestAnimationFrame(function () {

                collapseBody.style.height = collapseMe ? 0 : collapseBody.scrollHeight + "px";

                if (animate2steps) {

                    collapseBody.style.transition = collapseBodyTransition;
                    animateHeight(false, true);
                }
            });
        }

        if (isExpanded) {

            // *** transition from expanded to collapsed ***

            // first temporarily disable css transitions
            collapseBody.style.transition = "";

            // on the next frame, explicitly set the height to its current pixel height, removing the 'auto' height
            // then put height=0 to collapse the boddy
            // finally, at the end of the transition ( see animationEnd() ):
            // set the aria-expanded to false, which sets "display:none" (in the css)
            // to make sure the nested [tabindex=0] are not reachable anymore
            animateHeight(true);

        } else {

            // *** transition from collapsed to expanded ***
            //first set the ariaExpanded=true,  which removes the "display:none" style (in the css)
            setAriaExpanded(collapseBody, true);

            // on the next frame, set the height (which is 0) to the real height
            // finally, at the end of the transition ( see animationEnd() )
            // remove the "height" from the inline style to return it back to 'auto' height
            animateHeight();

        }

        setAriaExpanded(button, !isExpanded);
        this.flags[button.id.split("-")[1]] = isExpanded ? "T" : "F";  //new state: collapsed=="T"

        if (this.pims) {
            $.cookie(this.pims, this.flags.join(""));
        }
    }

    /*
    Collapsible
        Creates a new instance,
        adding collapsible behaviour to a set of elements, and keeping their states in a cookie.
    */
    this.Collapsible = function (elements, options) {

        var self = this, els = [];

        self.UID = "C0llapse" + _UID++ + "-";
        self.pims = options.cookie;
        self.flags = ((self.pims && $.cookie(self.pims)) || "").split("");

        elements.forEach(function (el) {

            if (el.matches(".collapse")) {

                el.getElements("li").forEach(function (el) {
                    els.push(buildCollapsibleList(el));
                });
            }
            else /*if( el.matches(".collapsebox") )*/ {

                els.push(buildCollapsibleBox(el));
            }
        });
        els.forEach(addCollapseToggle, /*bind to:*/ self);
        //console.log(self.flags.join(""));
    }

}();
