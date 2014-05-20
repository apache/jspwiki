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
Class: Dialog.Chars
	The Dialog.Chars is a Dialog object, to support selection of special
	characters.

Arguments:
	options - optional, see options below

Options:
	others - see Dialog options

Inherits from:
	[Dialog]
*/
Dialog.Chars = new Class({

	Extends: Dialog.Selection,
	
	options: {
		//onAction: function(value){},
		items: '.body td',
		chars: [
			'nbsp|iexcl|cent|pound|curren|yen|brvbar|sect|uml|copy|ordf',
			'laquo|not|reg|macr|deg|plusmn|sup2|sup3|acute|micro|para',
			'middot|cedil|sup1|ordm|raquo|frac14|frac12|frac34|iquest|times|divide',
			'Agrave|Aacute|Acirc|Atilde|Auml|Aring|AElig|Ccedil|Egrave|Eacute|Ecirc',
			'Euml|Igrave|Iacute|Icirc|Iuml|ETH|Ntilde|Ograve|Oacute|Ocirc|Otilde',
			'Ouml|Oslash|Ugrave|Uacute|Ucirc|Uuml|Yacute|THORN|szlig|agrave|aacute',
			'acirc|atilde|auml|aring|aelig|ccedil|egrave|eacute|ecirc|euml|igrave',
			'iacute|icirc|iuml|eth|ntilde|ograve|oacute|ocirc|otilde|ouml|oslash',
			'ugrave|uacute|ucirc|uuml|thorn|yuml|OElig|oelig|Scaron|scaron|Yuml',
			'ndash|mdash|lsquo|rsquo|ldquo|rdquo|dagger|Dagger|bull|hellip|permil',
			'euro|trade|larr|uarr|rarr|darr|harr|crarr|loz|diams|infin'
		]
	},

	initialize:function(options){

        this.setClass('.chars',options);
        //options.cssClass = '.chars'+(options.cssClass||'')
		this.parent(options);

	},

	setBody: function(){

		/* inspired by smarkup */
		var content = this.options.chars.map(function(line){

			return '<tr>' +
				line.split('|').map(function(c){
					return '<td title="&amp;'+ c + ';">&'+ c + ';</td>';
				}).join('') + '</tr>';

		});

		return this.parent( 
		    'table'.slick({ html: '<tbody>'+content.join('')+'</tbody>' }) 
		);
	}

});
