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
/*global $, typeOf */

/*
Mootools Extension: String.Extend.js
    String-extensions: capitalize,() deCamelize(), trunc(), xsubs()
*/
String.implement({

    /*
    Function: capitalize
        Converts the first letter of each word in a string to uppercase.

    Example:
    >   "i like cookies".capitalize() === 'I Like Cookies'
    >   "je vais à l'école".capitalize() === 'Je Vais À L'école'   //not "Je Vais à L'éCole"
    */
    capitalize: function(){
        //fix mootools implementation , supporting i18n chars such as éàè, not matched by \b
        //return String(this).replace(/\b[a-z]/g, function(match){
        return this.replace(/(\s|^)\S/g, function(match){
            return match.toUpperCase();
        });
    },

    /*
    Function: escapeHtml
    */
    escapeHtml: function(){
    	return this.replace(/[<>'"&]/g, function(s) {
    	    return {'<':'&lt;','>':'&gt;',"'":'&apos;','"':'&quot;','&':'&amp;'}[s];
	    	//return '&#' + s.charCodeAt(0) + ';';
	    });
    },

    /*
    Function: deCamelize
        Convert camelCase string to space-separated set of words.

    Example:
    >    "CamelCase".deCamelize() === "Camel Case";
    */
    deCamelize: function(){
        //return this.replace(/([a-z])([A-Z])/g,"$1 $2");
        //return this.replace(/([a-zà-ý])([A-ZÀ-Ý])/g,"$1 $2");
        return this.replace(/([a-z\xe0-\xfd])([A-Z\xc0-\xdd])/g,"$1 $2");
    },

	//ES6 polyfill
	startsWith: function( match ){
		return !this.indexOf(match);
	},
	endsWith: function( match ){
		return this.slice( -match.length ) == match ;
	},

    /*
    Function: trunc
        Truncate a string to a maximum length

    Arguments:
        size - maximum length of the string, excluding the length of the elips
        ellipsis - (optional) replaces the truncated part (defaults to '...')

    Alternative
        Use css propoerty
            white-space:nowrap;
            overflow:hidden;
            text-overflow:ellipsis;

    Example:
    > "this is a long string".trunc(7) === "this is..."
    > "this is a long string".trunc(7,'__') === "this is__"
    */
    trunc: function (size, ellipsis){

        return this.slice(0, size-1) + ((this.length<size) ? '' : (ellipsis||'…'));

    },


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
            "javascript.curlyBraces": "Show \{Curly Braces}",  //escaped curly braces
        }
        String.I18N.PREFIX="javascript.";

        "moreInfo".localize() === "More";
        "imageInfo".localize(2, 4) ===  "Image 2 of 4"
        "imageInfo2".localize({totalCount: 4, imgCount: 2}) === "Image 2 of 4"
        "curlyBraces".localize() === "Show {Curly Braces}"

        (end)
    */
    localize: function( params ){

        var I18N = String.I18N;

        return ( I18N[I18N.PREFIX + this] || this ).substitute(

            ( typeOf(params) == "object" ) ? params : Array.from(arguments)

        );

    },


    /*
    Function: setHash
        Set HASH without page jump

	Example:
	>	"here is a new hash".setHash();
    */
    setHash:function( ){

        if( history.pushState ){

            //history.pushState( state-object, title-ffs, "#" + hash );
            history.pushState( null, "", "#" + this);

        }

        else {

            var el = $( this ),
                id = el && el.id;

            el && el.removeAttribute( id );

            location.hash = "#" + this ;

            el && el.setAttribute("id", id);

        }

    },


    /*
    Function: xsubs (extended Substitute)
        Equal to substitute(), but also supports anonymous arguments.

        Named arguments:
        >    "Hello {text}".xsubs({text:"world"}) ==>  "Hello world"
        Anonymous arguments:
        >    "Hello {0}{1}".xsubs("world", "!") ===  "Hello world!"
    */
    xsubs: function(object, regexp){

        if( typeOf(object) != "object" ){
            object = Array.from(arguments);
            regexp = null;
        }
        return this.substitute(object, regexp);
    },

    /*
    Function:slick(props)
        Fancy DOM Element builder

    Example:
    >    "input#someID.someClass1.someClass2[disabled=true]".slick({text:"Hi"});
    */
    slick: function(props){

        return new Element(this + "", props);

    },

    /*
    Function: sliceArgs
        Parse the command arguments of a string or an element's class-name.
        Pattern: <command>(-arg1)(-arg2)...
        Returns an array of arguments.

        > <command>.sliceArgs( args (, regexp) );

    Arguments:
        args : (string) or dom-element with classname
        regexp : (optional string) pattern match for the arguments, defaults (-\w+)*

    Example
        > "zebra".sliceArgs( "zebra-eee-ffa" ); //returns ['eee','ffa']
        > "zebra".sliceArgs( "horse" );  //returns null
        > "zebra".sliceArgs( "zebra" );  //returns []
        > "zebra".sliceArgs( "horse zebra-eee-ffa" ); //returns ['eee','ffa']
        > "zebra".sliceArgs( "zebra-eee-ffa monkey" ); //returns ['eee','ffa']
        > "zebra".sliceArgs( "horse zebra-eee-ffa monkey" ); //returns ['eee','ffa']

    */
    sliceArgs: function(element, regexp){

        var args = element.className || String(element);

        if( !regexp) regexp = "(?:^|\\s)("+this+"(?:-\\w+)*)(?:\\s|$)"; //default '-' separated arguments

        args = args.match( regexp );
        return args && args[1].split("-").slice(1);

    },

    /*
    Function: fetchContext
        Match an elements classname or string against one of the bootstrap contextual patterns.
        Supported contexts: default, primary, success, info, warning, danger

        Return a (string) classname to invoke the contextual colors.

    Example
    >    "panel".fetchContext( "accordion-danger") => 'panel panel-danger'
    >    "panel".fetchContext( "commentbox-success") => 'panel panel-success'

    */
    fetchContext : function(element){

        var name = element.grab /*isElement*/ ? element.className : element;

        name = (name.match( /\b(default|primary|success|info|warning|danger)(\b|$)/ )||[0,'default'])[1];

        return this + " " + this + "-" + name ;

    }

});