/*!
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
 */

/**
 ** Javascript routines to support JSPWiki UserPreferences
 ** since v.2.6.0
 **/

var WikiPreferences =
{
	/*
	Function: initialize()
		Initialze submit and onberforeunload handlers to support the
		UserPreferences page.

		Register a onbeforeunload handler to show a warning popup
		when the user leaves the Preferences page without saving.

		Register a submit handler on the main preferences form,
		in order to save the settings to the UserPref cookies.
		Note: this could be done server side, but some of the prefs are only
		known client-side, only persisted through cookies.
	*/
	initialize: function(){

		var self = this,
			wikiprefs = Wiki.prefs,
			p;

		window.onbeforeunload = function(){

			if( $('prefs').getElements('input, select').some(function(el){

				return ((el.type != "submit") && (el.get('value') != el.getDefaultValue()));

			}) ) return "prefs.areyousure".localize();

		};


		$('setCookie').addEvent('submit', function(){

 			window.onbeforeunload = null;

 			/* see org.apache.wiki.preferences.Preferences.java */
			var prefs = {
				skin:'Skin',
				timeZone:'TimeZone',
				timeFormat:'DateFormat',
				orientation:'Orientation',
				editor:'Editor',
				locale:'Locale',
				sectionEditing:'SectionEditing'
			};

			for( var el in prefs ){
				if( p = $(el) ) wikiprefs.set( prefs[el], p.get('value') );
			};
		});

		/*
		Make an immediate change to the position of the Favorites block
		(aka left-menu) according to the setting prefOrientation dropdown.
		The setting is persisted only when submitting the form. (savePrefs)

		FIXME: value of selection is now LEFT or RIGHT iso fav-left/fav-right
		*/
		$('orientation').addEvent('change',function(){
			$('wikibody')
				.removeClass('fav-left|fav-right')
				.addClass( 'fav-'+this.get('value').toLowerCase() );
		});

 	}

}
window.addEvent('domready', WikiPreferences.initialize );
