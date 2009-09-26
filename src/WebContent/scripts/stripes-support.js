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

/*
Script: stripes-support.js
	Stripes-related Javascript routines to support JSPWiki, a JSP-based WikiWiki clone.
	This is a TEMPORARY file, for proof-of-concept demonstration.
	The contents of this file will be refactored into jspwiki-common.js later in
	the JSPWiki 3.0 release cycle.

License:
	http://www.apache.org/licenses/LICENSE-2.0

Since:
	v.3.0

Dependencies:
	Based on http://mootools.net/ v1.11
	*	Core, Class,  Native, Element(ex. Dimensions), Window,
	*	Effects(ex. Scroll), Drag(Base), Remote, Plugins(Hash.Cookie, Tips, Accordion)

*/

/*
Class: Stripes
	The main javascript class to support basic jspwiki functions.

	Fixme: update to mootool 1.2.3
*/
var Stripes = {
	executeEvent: function( form, event, divTarget ){
		params = event + "=&" + $(form).toQueryString();

		new Request(form.action, {
			postBody: params,
			method: 'post',
			onComplete: function(response){
				// Clear the results div
				$(divTarget).empty();
				// Build new results if we got a response
				if(response) var results = eval(response);
				if(results){
					var target = $(divTarget).addClass("warning");
					results.each(function(result,i) {
						var p = new Element('p').setHTML(result).injectInside(divTarget);
					});
				}
			}
		}).send();
	}
}
