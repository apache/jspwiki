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
    *  prefSectionEditing:'SectionEditing' =>checkbox 'on'
*/
Wiki.once('#setCookie', function(form){

        window.onbeforeunload = function(){

            if( form.getElements('[data-pref]').some(function(el){

                //a checkbox.get('value') returns 'on' when checked; 
                //so getDefaultValue() should also return 'on'
                return (el.get('value') != el.getDefaultValue());

            }) ) return 'prefs.areyousure'.localize();      
        };

        form.addEvent('submit', function(e){
    
            this.getElements('[data-pref]').each( function(el){
                Wiki.set( el.get('data-pref'), el.get('value') ); 
            });
            window.onbeforeunload = function(){};

        });
    })

    .once('#clearCookie', function(form){

        form.addEvent('submit', function(){ 

            window.onbeforeunload = function(){}; 
            Wiki.erase(); 

        });
});