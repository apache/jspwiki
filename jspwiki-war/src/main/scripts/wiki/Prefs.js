/*
Javascript routines to support JSPWiki UserPreferences
    PreferencesContent.jsp
    PreferencesTab.jsp

    *  prefSkin:'SkinName',
    *  prefTimeZone:'TimeZone',
    *  prefTimeFormat:'DateFormat',
    *  prefOrientation:'Orientation',
    *  editor:'editor',
    *  prefLanguage:'Language',
    *  prefSectionEditing:'SectionEditing' =>checkbox 'on', T/F ???fixme
*/
Wiki.once('.context-prefs form', function(forms){


    window.onbeforeunload = function(){

        //a checkbox get('value') returns 'on' when checked; getDefaultValue() should also return 'on'
        if( forms[1].getElements('[data-pref]').some(function(el){
            //console.log(el.get('data-pref'),el.checked,el.get('name'), el.get('value') , el.getDefaultValue());
            return (el.get('value') != el.getDefaultValue());
        }) ) return 'prefs.areyousure'.localize();
        
        //return 'always popup dialog for testing';

    };

    forms[1].addEvent('submit', function(){
    
        this.getElements('[data-pref]').each( function(el){
            Wiki.set( el.get('data-pref'), el.get('value') ); 
        });
        //alert("stop");

    });

    forms[2].addEvent('submit', function(){ Wiki.erase(); });

});
