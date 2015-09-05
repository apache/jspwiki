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
            //shortcut = ((navigator.userAgent.indexOf("Mac OS X")!=-1) ? "meta+" : "control+");
            //control = (Browser.platform == "mac" ? "meta+" : "control+");
            control = ( navigator.platform.match( /mac/i ) ? "meta+" : "control+");



        self.workarea = workarea;  //Textarea class
        self.snips = snips;

        self.keys = {};
        self.dialogs = {};
        self.suggestions = {};

        for( cmd in snips ){

            snip = Function.from( snips[cmd] )( workarea, cmd );

            // short format of snip
            if( typeOf(snip)=="string" ){ snip = { snippet:snip }; }

            //Not needed sofar: Function.from( snip.initialize )(cmd, snip);

            if( key = snip.key ){

                // key:"f" => key:"control+f" ;  key:"shift+enter" (no change)
                if( !key.contains("+") ){ key = control + key; }
                self.keys[ key.toLowerCase() ] = cmd;
                snip.key = key;

            }

            if( suggest = snip.suggest ){

                if( typeOf(suggest)== "string" ){

                    snip.suggest = {
                        pfx: RegExp( suggest + "$" ),
                        match: RegExp( "^" + suggest )
                    }
                    //console.log( snip.suggest );
                }
                self.suggestions[cmd] = snip;

            }

            //check for snip dialogs -- they have the same name as the command
            //EG:  find: { find:<this is a snip dialog> }
            if( snip[cmd] ){ self.dialogs[cmd] = snip[cmd]; }

            snips[cmd] = snip;

        }
        //console.log(this.keys);

    },

    /*
    Function: match
        Lookup a cmd entered just in front of the caret/cursor of the workarea.
    */
    match: function(){

        var cmd, fromStart = this.workarea.getFromStart();

        for( cmd in this.snips ){
            if( fromStart.test( cmd + "$" ) ) return cmd;
        }

        return false;
    },

    /*
    Function: matchSuggest
        Lookup a cmd enter just in front of the caret/cursor of the workarea..

        snip.suggest => {
            start: <pos>,
            match: <match-string-before-caret>,
            tail:  <length-match-after-caret>
        }
    */
    matchSuggest: function(){

        var cmd, snip, pfx, result, suggest,
            snips = this.suggestions,
            workarea = this.workarea,
            caret = workarea.getSelectionRange(),
            fromStart = workarea.getFromStart();

        //"selectInline", "selectBlock", "selectStartOfLine";

        var SOL = workarea.isCaretAtStartOfLine();
        var EOL = workarea.isCaretAtEndOfLine();

        for( cmd in snips ){

            snip = snips[cmd];
            suggest = snip.suggest;

            if( this.inScope(snip, fromStart) ){

                if( suggest.pfx ){

                    pfx = fromStart.match( suggest.pfx );

                    if( pfx ){

                        console.log("SUGGEST Prefix ", cmd, suggest.pfx, pfx.getLast() );
                        pfx = pfx.getLast(); //match last (x)
                        result = workarea.slice( caret.start - pfx.length )
                                         .match( suggest.match );

                        console.log("SUGGEST Match ", suggest.match, result );

                        if( result ){
                            result = { pfx:pfx, match:result.getLast() } ;
                        }

                    }

                } else {

                    result = Function.from(suggest)(workarea, caret, fromStart);

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
        snips - snippet collection object for lookup of the cmd
        cmd - snippet key. If not present, retrieve the cmd from
            the textarea just to the left of the caret. (i.e. tab-completion)

    Example:
        (start code)
        sn.get( "bold" );

        returned_object = false || {
                key: "snippet-key",
                snippet: " snippet-string ",
                text: " converted snippet-string, no-parameter braces, auto-indented ",
                parms: [parm1, parm2, "last-snippet-string" ]
            }
        (end)
    */
    get: function( cmd ){

        var self = this,
            txta = self.workarea,
            fromStart = txta.getFromStart(),
            snip = self.snips[cmd],
            parms = [],
            s,last;

        if( snip && snip.synonym ){ snip = self.snips[snip.synonym]; }

        if( !snip || !self.inScope(snip, fromStart) ){ return false; }

        s = snip.snippet || "";

        //parse snippet {parameters}
        s = s.replace( /\\?\{([^{}]+)\}/g, function(match, name){

            if( match.charAt(0) == "{" ){
                parms.push(name);
                return name;
            } else {
                return match.slice(1)
            }

        }).replace( /\\\{/g, "{" );
        //and end by replacing all escaped "\{" by real "{" chars

        //also push the last piece of the snippet onto the parms[] array
        last = parms.getLast();
        if( last ){ parms.push( s.slice(s.lastIndexOf(last) + last.length) ); }

        //collapse \n of previous line if the snippet starts with \n
        if( s.test(/^\n/) && ( fromStart.test( /(^|[\n\r]\s*)$/ ) ) ) {
            s = s.slice(1);
            //console.log("remove leading \\n");
        }

        //collapse \n of subsequent line when the snippet ends with a \n
        if( s.test(/\n$/) && ( txta.getTillEnd().test( /^\s*[\n\r]/ ) ) ) {
            s = s.slice(0,-1);
            //console.log("remove trailing \\n");
        }

        //finally auto-indent the snippet"s internal newlines \n
        var prevline = fromStart.split(/\r?\n/).pop(),
            indent = prevline.match(/^\s+/);
        if( indent ){ s = s.replace( /\n/g, "\n" + indent[0] ); }

        //complete the snip object
        snip.text = s;
        snip.parms = parms;

        //console.log("Snipe.Snips:get() ",snip.text, JSON.encode(snip),"***" );
        return snip;
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

        var pattern, pos, scope=snip.scope, nscope=snip.nscope;

        if( scope ){

            if( typeOf(scope)=="function" ){

                return scope( this.textarea );

            } else {

                for( pattern in scope ){

                    pos = text.lastIndexOf(pattern);
                    if( (pos > -1) && (text.indexOf( scope[pattern], pos ) == -1) ){

                        return true;

                    }

                }
                return false;
            }
        }

        if( nscope ){

            for( pattern in nscope ){

                pos = text.lastIndexOf(pattern);
                if( (pos > -1) && (text.indexOf( nscope[pattern], pos ) == -1) ){

                    return false;

                }

            }

        }
        return 1 /*true*/;
    },


    /*
    Function: toggle
        Toggle the prefix and suffix of a snippet.
        Eg. toggle between {{__text__}} and {{text}}.
        The selection will be matched against the snippet.

    Precondition:
        - the selection is not empty (caret.thin = false)
        - the snippet has exatly one {parameter}

    Arguments:
        txta - Textarea object
        snip - Snippet object
        caret - Caret object {start, end, thin}

    Returns:
        - (string) replacement string for the selection.
            By default, returns snip.text
        - the snip.parms will be set to [] is toggle was executed successfully
        Eventually the selection will be extended if the
        prefix and suffix were just outside the selection.
    */
    toggle: function(txta, snip, caret){

        var s = snip.text,
            //get the first and last textual parts of the snippet
            arr = s.trim().split( snip.parms[0] ),
            fst = arr[0],
            lst = arr[1],
            re = new RegExp( "^\\s*" + fst.trim().escapeRegExp() + "\\s*(.*)\\s*" + lst.trim().escapeRegExp() + "\\s*$" );

        if( (fst + lst)!="" ){

            s = txta.getSelection();
            snip.parms = [];

            // if pfx & sfx (with optional whitespace) are matched: remove them
            if( s.test(re) ){

                s = s.replace( re, "$1" );

            // if pfx/sfx are matched just outside the selection: extend selection
            } else if( txta.getFromStart().test(fst.escapeRegExp()+"$") && txta.getTillEnd().test("^"+lst.escapeRegExp()) ){

                txta.setSelectionRange(caret.start-fst.length, caret.end+lst.length);

            // otherwise, insert snippet and set caret between pfx and sfx
            } else {

                txta.setSelection( fst+lst ).setSelectionRange( caret.start + fst.length );
            }
        }

        return s;
    }
});