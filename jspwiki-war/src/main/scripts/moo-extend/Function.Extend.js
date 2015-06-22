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
Function.implement({

/*
Event: debounce
    Returns a function, that, as long as it continues to be invoked, will not
    be triggered. The function will be called after it stops being called for
    N milliseconds. If `immediate` is passed, trigger the function on the
    leading edge, instead of the trailing.
    I.e. collapse a number of events into a single event.

Credits:
    http://davidwalsh.name/function-debounce

Example:
    el.addEvent('resize', resizePage.debounce(250, true).bind(this) );
*/
    debounce: function(wait, immediate){

        var func = this, timer;

        return function(){

            var args = arguments,
                context = this,
                callNow = immediate && !timer;

            function later(){
                timer = null;
                if( !immediate ){ func.apply(context, args); }
            }

            clearTimeout(timer);
            timer = setTimeout(later, wait || 250);

            if( callNow ){ func.apply(context, args); }

        };

    }

});
