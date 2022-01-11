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
/*global $, $$ */

// ELEMENT
// convenience dom manipulation functions
// FFS:  patch on Element.prototype;  or global $ namespace

$.replaces = function (newElement, existingElement) {

    existingElement.parentNode.replaceChild(newElement, existingElement);
}

/*
Remove a collection of elements  (note: not in Blizz    ???CHECKME)
Uses: $$
FIXME
*/
$.remove = function (selector, context) {

    //$$(selector, context).forEach(function (child) {
    (context || document).querySelectorAll(selector).forEach(function (child) {
        child.remove();
    });
}

// syntax sugar for element.classList add/remove ; and polyfill for ie11
// FIMXE better function on element.addClass and [el1,el2...].addClass ...
$.hasClass = function (element, clazz) {

    //return element.classList.contains(clazz)
    return element.matches("." + clazz);
}

$.addClass = function (elements, clazz) {

    element.classList.add(clazz);
}

$.removeClass = function (elements, clazz) {

    element.classList.remove(clazz);
}

//Element.classList mini polyfill for IE11
if (!!document.createElement('div').classList) {

    $.addClass = function (element, clazz) {
        if (!$.hasClass(element, clazz)) {
            element.className = element.className + ' ' + clazz;
        }
    }
    $.removeClass = function (element, clazz) {
        element.className = element.className.replace(RegExp('(^|\\s)' + clazz + '(?:\\s|$)'), '$1');
    }
}


//credit: mootools more
$.isVisible = function (element) {

    var w = element.offsetWidth,
        h = element.offsetHeight;

    return (w == 0 && h == 0) ? false : (w > 0 && h > 0) ? true : element.style.display != 'none';
}

/*
Function: ifClass
    Add and/or remove a css class from an element depending on a condition flag.

Arguments:
    flag : (boolean)
    trueClass : (string) css class name, add on true, remove on false
    falseClass : (string) css class name, remove on true, add on false

Returns:
    (element) - This Element

Examples:
>    $.ifClass($("page"), i > 5, "hideMe" );
>    $("page")._.ifClass($("page"), i > 5, "hideMe" );
*/
//$.ifClass = function (element, flag, trueClass = "", falseClass = "") {
$.ifClass = function (element, flag, trueClass, falseClass) {

    trueClass = trueClass || "";
    falseClass = falseClass || "";
    $.addClass(element, flag ? trueClass : falseClass);
    $.removeClass(element, flag ? falseClass : trueClass);
    return element;
}

/*
Function: wrapChildren(start, grab)
    This function groups a lists of children, delimited by certain DOM elements.

Arguments
    - delimitter : (string) css selector to match delimiting DOM elements
    - wrapper : (string) css selector, grabs a subset of dom elements
                and replaces the delimitter element
    - replacesFn: (callback function) called at the point of replacing the
            delimitter-element with the childContainer-element

DOM Structure:
(start code)
    // before
    a
    delimitter1
    b
    b
    delimitter2
    c

    // after wrapChildren(delimitter,childContainer)
    childContainer  (replaces (the missing) injected delimitter )
        a
    childContainer (replaces delimitter1)
        b
        b
    childContainer (replaces delimitter2)
        c

Example:
>   $.wrapChildren(el, /hr/i,"div.col");
>   $.wrapChildren(el, /h[1-6]/i,"div.col");
>   $.wrapChildren(el, container.getTag(), "div");
*/
$.wrapChildren = function (container, delimitter, wrapper, replaceDelimitter, startWithDelimitter) {

    var next,
        isInContainer,
        childContainer = wrapper.slick();

    //if( ?? ){ container.insertBefore(childContainer, container.firstChild); }

    if (container.getElement(delimitter)) {

        container.insertBefore(childContainer, container.firstChild); //insert at the top

        while ((next = childContainer.nextSibling)) {

            if ((next.matches) && next.matches(delimitter)) {

                //if the childContainer is already inserted in the container, create a new one
                if (isInContainer) { childContainer = wrapper.slick(); }

                //now invoke the callback prior to replacing the delimitter by this childContainer
                if (replaceDelimitter) { replaceDelimitter(childContainer, next); }

                $.replaces(childContainer, next);

                isInContainer = true;

            } else {

                childContainer.appendChild(next);
            }
        }
    }
}
