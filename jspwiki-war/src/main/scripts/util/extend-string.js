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
/*global $ */

/*
Function: escapeHml()

Example:
>   "ab<span>cd".escapeHtml() => "ab&#60;span&#62;cd"
*/
String.prototype.escapeHtml = function (s) {
    return this.replace(/[<>'"&]/g, function (s) {
        return '&#' + s.charCodeAt(0) + ';';
    });
}

/*
Function: escapeRegExp()

Example:
>   "animals.sheep[1]"".escapeRegExp() =>  "animals\.sheep\[1\]"
*/
String.prototype.escapeRegExp = function () {
    // Credit: XRegExp 0.6.1 (c) 2007-2008 Steven Levithan <http://stevenlevithan.com/regex/xregexp/> MIT License
    return this.replace(/[-[\]{}()*+?.\\^$|,#\s]/g, '\\$&');
}

/*
Function: xsubs (object || arguments)
    Substitutes {keywords} in a string using an object or indexed arguments.
    Removes undefined keywords and ignores escaped keywords.

Example:
>   //Object with key - value pairs:  named {keywords}
>   "Hello {text}".xsubs({text:"world"}) ==>  "Hello world"
>   "Hello escaped \\{text}".xsubs({text:"world"}) ==>  "Hello escaped {text}"

>   //List of anonymous arguments: indexed keywords starting from 0 {0}, {1} ...
>   "Hello {0}{1}".xsubs("world", "!") ===  "Hello world!"
>   "Hello {0}{1}".xsubs(["world", "!"]) ===  "Hello world!"
*/
String.prototype.xsubs = function (object, regexp) {

    if (typeOf(object) != "object") {

        object = Array.prototype.slice.call(arguments);
        regexp = null;
    }

    return this.replace(regexp || (/\\?\{([^{}]+)\}/g), function (match, name) {

        if (match.charAt(0) == '\\') return match.slice(1);

        return (object[name] != null) ? object[name] : '';
    });
}


/*
Function: deCamelize
    Convert camelCase string to space-separated set of words.

Example:
>    "CamelCase".deCamelize() === "Camel Case";
*/
String.prototype.deCamelize = function(){

    return this.replace(/([a-z\xe0-\xfd])([A-Z\xc0-\xdd])/g,"$1 $2");
}

/*
Function: capitalize
    Converts the first letter of each word in a string to uppercase.
    Supporting sepcial chars such as éàè, not matched by \b

Example:
>   "i like cookies".capitalize() === 'I Like Cookies'
>   "je vais à l'école".capitalize() === 'Je Vais À L'école'   //not "Je Vais à L'éCole"
*/
String.prototype.capitalize = function(){

    return this.replace(/(\s|^)\S/g, function(match){

        return match.toUpperCase();
    });
}

/*
Function: localize
    Localize a string with optional parameters. (indexed or named)

Require:
    I18N - (global hash) repository of {{name:value}} pairs
        name - starts with a prefix {{javascript.}}
        value - (server generated) localized string, with optional {parameters}
            in curly braces

Examples:
    (start code)
    //initialize the translation strings
    String.I18N = {
        "javascript.moreInfo": "More",
        "javascript.imageInfo": "Image {0} of {1}",  //indexed parms
        "javascript.imageInfo2": "Image {imgCount} of {totalCount}"   //named parms
        "javascript.curlyBraces": "Show \\{Curly Braces}",  //escaped curly braces
    }
    String.I18N.PREFIX="javascript.";

    "moreInfo".localize() === "More";
    "imageInfo".localize(2, 4) ===  "Image 2 of 4"
    "imageInfo2".localize({totalCount: 4, imgCount: 2}) === "Image 2 of 4"
    "curlyBraces".localize() === "Show {Curly Braces}"

    (end)
*/
String.prototype.localize = function( params ){

    var I18N = String.I18N;

    return ( I18N[I18N.PREFIX + this] || this ).xsubs(

        ( typeOf(params) == "object" ) ? params : Array.prototype.slice.call(arguments)

    );
}



