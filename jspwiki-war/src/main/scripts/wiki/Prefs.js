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
/*global Wiki */


/*
Javascript routines to support JSPWiki UserPreferences
    PreferencesContent.jsp
    PreferencesTab.jsp

    *  prefSkin:"SkinName",
    *  prefTimeZone:"TimeZone",
    *  prefTimeFormat:"DateFormat",
    *  prefOrientation:"Orientation",
    *  editor:"editor",
    *  prefLanguage:"Language",
    *  prefSectionEditing:"SectionEditing" =>checkbox "on"
*/
!(function( wiki ){

    var datapref = "[data-pref]"; //data preference form elements

    function getValue( el ){
        return (el.type == "checkbox") ? el.checked : el.value;
    }

    function windowUnload( onbeforeunload ){
        window.onbeforeunload = onbeforeunload || function(){};
    }

    function deleteCookie(event){
        $.cookie.delete( this.getAttribute("data-delete-cookie") );
        this.closest("tr").remove();
    }

    wiki.add("#preferences", function( form ){

        //when leaving this pages check for changed preferences. If so, ask first.
        windowUnload( function(){

            //if( $$(datapref, form).some( function(el){
            if( form.getElements( datapref ).some( function(el){

                return ( getValue(el) != el.getDefaultValue() );

            }) ){ return "prefs.areyousure".localize(); }

        });

        //setAssertedName
        //$("[name=action][value=setAssertedName]", form).onclick = function(event){
        form.getElement("[name=action][value=setAssertedName]").onclick = function(event){

            form.getElements( datapref ).each( function(el){
                if( el.type != "radio" || el.checked ){
                    wiki.prefs( el.get( "data-pref" ), getValue(el) );
                }
            });
            windowUnload();
        }
        //clearAssertedName: cookie is reset by the server
        //FFS: no need for an AreYouSure dialog ??

        //$.bind( $$("[data-delete-cookie]",form), "click", deleteCookie);
        form.getElements("[data-delete-cookie]").forEach( function(element){
            element.onclick = deleteCookie;
        });

        //FFS: add click-triggers to some preferences:  prefLayout, prefOrientation,
        form.prefAppearance.onchange = function(event){
            console.log( this, getValue(this) );

        }

        form.prefLayout.onclick = function(event){
            console.log( this, getValue(this) );


        }

        form.prefOrientation.onclick = function(event){
            console.log( this, getValue(this) );


        }

    });

})(Wiki);