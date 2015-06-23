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
!function(wiki){

    var datapref = "[data-pref]"; //data preference elements

    function windowUnload( onbeforeunload ){
        window.onbeforeunload = onbeforeunload || function(){};
    }

    wiki.once("#setCookie", function(form){

        windowUnload( function(){

            if( form.getElements( datapref ).some(function(el){

                //a checkbox.get("value") returns "on" when checked;
                //so getDefaultValue() should also return "on"
                return (el.get("value") != el.getDefaultValue());

            }) ){ return "prefs.areyousure".localize(); }
        } );

        form.addEvent("submit", function(){

            this.getElements( datapref ).each( function(el){

                wiki.prefs.set( el.get( datapref ), el.get("value") );

            });
            windowUnload();

        });
    })

    .once("#clearCookie", function(form){

        form.addEvent("submit", function(){

            windowUnload();
            wiki.erase();

        });

    });

}(Wiki);
