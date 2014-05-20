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
---

name: Request.File
description: Uploading files with FormData
license: MIT-style license.
authors: [Arian Stolwijk, Djamil Legato]
requires: [Request]
provides: Request.File
credits: https://gist.github.com/a77b537e729aff97429c

...
*/

(function(){

var progressSupport = ('onprogress' in new Browser.Request());

Request.File = new Class({

    Extends: Request,

    options: {
        emulation: false,
        urlEncoded: false
    },

    initialize: function(options){
        this.xhr = new Browser.Request();
        this.formData = new FormData();
        this.setOptions(options);
        this.headers = this.options.headers;
    },

    append: function(key, value){
        this.formData.append(key, value);
        return this.formData;
    },

    reset: function(){
        this.formData = new FormData();
    },

    send: function(options){
        if (!this.check(options)) return this;
//console.log(this.formData);
        this.options.isSuccess = this.options.isSuccess || this.isSuccess;
        this.running = true;

        var xhr = this.xhr;
        if (progressSupport){
            xhr.onloadstart = this.loadstart.bind(this);
            xhr.onprogress = this.progress.bind(this);
            xhr.upload.onprogress = this.progress.bind(this);
        }

        xhr.open('POST', this.options.url, true);
        xhr.onreadystatechange = this.onStateChange.bind(this);

        Object.each(this.headers, function(value, key){
            try {
                xhr.setRequestHeader(key, value);
            } catch (e){
                this.fireEvent('exception', [key, value]);
            }
        }, this);

        this.fireEvent('request');
        xhr.send(this.formData);

        if (!this.options.async) this.onStateChange();
        if (this.options.timeout) this.timer = this.timeout.delay(this.options.timeout, this);
        return this;
    }

});

})();

