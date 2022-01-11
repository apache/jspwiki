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
/*global typeOf, Class, Events, Snipe  */
/*
Snippet:
    init - initialize snippets; detect shortcut-keys, and suggestion dialogs

    get - retrieve and validate a snippet. Returns false when not found.

    inScope - check whether a snippet is inScope at the current cursor position
    toggle - ...

    match - retrieve snippet based on cmd entered at cursor position
    matchSuggest - retrieve suggestion dialog based on cmd entered at cursor position
    shortcut -  match key => fireEvent(action,cmd)

Example
    var sn = new Snipe.Snips(textarea, { snippets... } });
    sn.dialogs => snippet dialogs
    sn.suggest => suggest snippets
    sn.keys => shortcut keys

*/

Snipe.Snips = new Class({

    Implements: Events,

    initialize: function( workarea, snips ){

        var self = this,
            cmd, snip, key, suggest,
            control = ( navigator.platform.match( /mac/i ) ? "meta+" : "control+");

        self.workarea = workarea;  //Textarea class
        self.keys = {};
        self.dialogs = {};
        self.snips = snips; //{};
        self.suggestions = {};

        for( cmd in snips ){

            snip = $.toFunction( snips[cmd] )( workarea, cmd );

            // short format of snip
            if( typeOf(snip) == "string" ){ snip = { snippet:snip }; }

            //shortcut keys
            if( (key = snip.key) ){

                // key: "f" => "control+f" ;  key: "shift+enter" (no change)
                if( !key.contains( "+" ) ){ key = control + key; }
                self.keys[ key.toLowerCase() ] = cmd;

            }

            if( (suggest = snip.suggest) ){

                //this is a suggest snippets
                if( typeOf(suggest) == "string" ){

                    snip.suggest = {
                        lback: RegExp( suggest + "$" ),
                        match: RegExp( "^" + suggest )
                    }

                }

                self.suggestions[cmd] = snip;

             //otherwise regular snippet
             //} else {

            }

            //check for snip dialogs -- they have the same name as the command
            //TODO better:  use the dialog property !
            if( snip[cmd] ){ self.dialogs[cmd] = snip[cmd]; }  //deprecated
            if( snip.dialog ){ self.dialogs[cmd] = snip.dialog; }

            snips[cmd] = snip;

        }
        //console.log(this.keys, this.suggest, this.snip);

    },

    /*
    Function: match
        Lookup a cmd entered just in front of the caret/cursor of the workarea.
    */
    match: function(){

        var cmd, fromStart = this.workarea.getFromStart();

        for( cmd in this.snips ){

            if( fromStart.endsWith( cmd ) ){ return cmd; }

        }

        return false;

    },

    /*
    Function: matchSuggest
        Lookup a cmd which matches the suggestion look-back and match reg-exps.

        snip.suggest => {
            start: (number) start position,
            match: (string) matched string (selected?)
            cmd:  (string) command
        }
    */
    matchSuggest: function(){

        var cmd, snip, lback, match, result, suggest,
            suggestions = this.suggestions,
            workarea = this.workarea,
            caret = workarea.getSelectionRange(),
            fromStart = workarea.getFromStart();

        for( cmd in suggestions ){

            snip = suggestions[cmd];

            if( this.inScope(snip, fromStart) ){

                suggest = snip.suggest;

                if( suggest.lback ){

                    lback = fromStart.match( suggest.lback );

                    if( lback ){

                        //console.log("SUGGEST Look-Back ", cmd, suggest.lback, lback.getLast() );
                        lback = lback.getLast(); //match last (x)

                        match = workarea.slice( caret.start - lback.length )
                                         .match( suggest.match );

                        //console.log("SUGGEST Match ", suggest.match, match );

                        if( match ){

                            result = { lback: lback, match: match.getLast() } ;

                        }

                    }

                } else {

                    result = $.toFunction(suggest)(workarea, caret, fromStart);

                }

                if( result ){

                    result.cmd = cmd;
                    return result;

                }
            }
        }
        return false;

    },

    /*
    Function: get
        Retrieve and validate the snippet.
        Returns false when the snippet is not found or not in scope.

    Arguments:
        cmd - snippet key
    */
    get: function( cmd ){

        var self = this,
            snip = self.snips[cmd];

        if( snip && snip.alias ){ snip = self.snips[snip.alias]; }

        return ( snip && self.inScope( snip, self.workarea.getFromStart() ) ) ? snip : false;

    },

    /*
    Function: inScope
        Sometimes it is useful to restrict the scope of a snippet, and only allow
        the snippet expansion in specific parts of the text. The scope parameter allows
        you to do that by defining start and end delimiting strings.
        For example, the following "fn" snippet will only expands when it appears
        inside the scope of a script tag.

        (start code)
        "fn": {
            snippet: "function( {args} )\{ \n    {body}\n\}\n",
            scope: {"<script":"</script"} //should be inside this bracket
        }
        (end)

        The opposite is possible too. Use the "nScope" or not-in-scope parameter
        to make sure the snippet is only inserted when not in scope.

        (start code)
        "special": {
            snippet: "{special}",
            nScope: { "%%(":")" } //should not be inside this bracket
        },
        (end)

    Arguments:
        snip - Snippet Object
        text - (string) used to check for open scope items

    Returns:
        True when the snippet is in scope, false otherwise.
    */
    inScope: function(snip, text){

        var pattern, pos,
            scope = snip.scope,
            nscope = snip.nscope;

        function parse(patterns, inscope){

            for( pattern in patterns ){

                pos = text.lastIndexOf( pattern );

                if( (pos > -1) && (text.indexOf( patterns[pattern], pos ) == -1) ){
                        return inscope;
                }
            }
            return !inscope;
        }


        if( scope ){

            return typeOf(scope)=="function" ? scope( this.textarea ) : parse(scope, true);

        }

        if( nscope ){

            return parse(nscope /*,false*/);

        }

        return true;
    }

});