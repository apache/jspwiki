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
/*global Class, Dialog  */
/*exported Dialog.Chars */
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
        chars: [

            'lsquo|rsquo|ldquo|rdquo|lsaquo|rsaquo|laquo|raquo|apos|quot|sbquo|bdquo',
            'ndash|mdash|sect|para|dagger|Dagger|amp|lt|gt|copy|reg|trade',
            'rarr|rArr|bull|middot|deg|plusmn|brvbar|times|divide|frac14|frac12|frac34',
            'hellip|micro|cent|pound|euro|iquest|iexcl|uml|acute|cedil|circ|tilde',
            'Agrave|Aacute|Acirc|Atilde|Auml|Aring|AElig|Ccedil|Egrave|Eacute|Ecirc|Euml',
            'Igrave|Iacute|Icirc|Iuml|ETH|Ntilde|Ograve|Oacute|Ocirc|Otilde|Ouml|Oslash',
            'OElig|Scaron|Ugrave|Uacute|Ucirc|Uuml|Yacute|Yuml|THORN|szlig|agrave|aacute',
            'acirc|atilde|auml|aring|aelig|ccedil|egrave|eacute|ecirc|euml|igrave|iacute',
            'icirc|iuml|eth|ntilde|ograve|oacute|ocirc|otilde|ouml|oslash|oelig|scaron',
            'ugrave|uacute|ucirc|uuml|yacute|yuml|thorn|ordf|ordm|alpha|Omega|infin',
            'not|sup2|sup3|permil|larr|uarr|darr|harr|hArr|crarr|loz|diams'
        ]
    },

    initialize:function(options){

        this.setClass('.chars',options);
        this.parent(options);

    },

    setBody: function(){

        var rows = [];
        this.options.chars.map( function(line){
            var cells = [];
            line.split('|').each( function(item){
                cells.push( 'td.item[title=&'+item+';]', {html:'&'+item+';'});
            });
            rows.push('tr',cells);

        });
        return this.parent( ['table',rows].slick() );

    }

});
