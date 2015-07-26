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
!function( wiki ){

    var datapref = "*[data-pref]"; //data preference form elements

    function getValue( el ){
        return ( el.match( "[type=checkbox]" ) ? el.checked : el.value );
    }

    function windowUnload( onbeforeunload ){
        window.onbeforeunload = onbeforeunload || function(){};
    }

    wiki.add("#preferences", function( form ){

        //when leaving this pages check for changed preferences. If so, ask first.
        windowUnload( function(){

            if( form.getElements( datapref ).some( function(el){

                //if(getValue(el) != el.getDefaultValue()){ console.log(el.get('data-pref'),getValue(el),el.getDefaultValue());}
                return ( getValue(el) != el.getDefaultValue() );

            }) ){ return "prefs.areyousure".localize(); }

        });

        //save & clear button handlers
        //form.getElements("[name=action]").addEvent( function(event){..});
        form.action[0].onclick = form.action[1].onclick = function(event){

            switch( event.target.value ){

                case "setAssertedName" :

                    form.getElements( datapref ).each( function(el){

                        wiki.prefs.set( el.get( "data-pref" ), getValue(el) );

                    });
                    break;

                default :  //"clearAssertedName"

                    //FFS: no need for an AreYouSure dialog ??
                    wiki.prefs.empty();

            };

            //on normal submit, leave the page without asking confirmation
            windowUnload();

        };

    });

}(Wiki);