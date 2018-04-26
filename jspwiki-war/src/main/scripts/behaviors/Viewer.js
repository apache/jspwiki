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
Viewer object
    match(url,options): function to retrieve the media parms based on a url
    preload(url,options,callback): preloads (created DOM object) the correct viewer
        with the correct rendering paramaters (eg: used by Viewer.Slimbox)
    preloads(elements,options,callback): preloads a set of viewer objects,
        and calculates the minimals viewer-box dimensions (eg: used by Viewer.Carousel)

    LIB: data-array of pairs [["re1",{parms1}],["re2",{parms2}],..]
        used by match() and preload() to identify the correct viewer & parameters.
        This array can be customised and extended.

        regexp - (string) match against the url
        parms - (function or object) returns the viewer parameters
            "img" - to be rendered as IMG, directly in the browser
            "url" - to be rendered as OBJECT (eg flash)
            "src" - to be rendered as IFRAME
            other-parameters with additional details
*/
!(function(){

this.Viewer = {

    LIB: [],

    match: function(url, options){

        var result = {};

        if( typeOf(url) == "element" ){ url = url.src || url.href; }
        this.LIB.some( function(item){

            return url.test( item[0], "i" )
                //item[1] can be a value or a function(url,options)
                && (result = Function.from(item[1])(url, options) );

        });

        //options.type = "img"|"url"|"src"
        //options.type = "img";
        //console.log(options.type);
        if( options.type && !result[options.type] ){ return false; }

        return result;
    },


    preload: function(element, options, callback ){

        var match = this.match(element, options),
            preload;

        function done(){
            //console.log( match.width, preload.width.toInt(), match.height, preload.height.toInt(), preload);
            if( callback ){ callback( preload, preload.width.toInt(), preload.height.toInt() ); }
        }

        options = {
            id: "VIEW." + String.uniqueID(),
            width: options.width,
            height: options.height
        };

        if( match.img ){

            preload = new Image();
            preload.onload = done;  //delayed onload to know height&width of the image
            preload.alt = element.alt;
            //preload.title = element.title;
            preload.src = match.img;

        } else {

            //ffs: add branch for html5 enabled browsers & html5 enabled video sources
            //if( match.url5 ) ...
            if( match.url ){

                preload = $(new Swiff( match.url, Object.merge(options, {
                    params: {
                        wmode: "transparent",//"opaque", //"direct",
                        //bgcolor: "#FFFFFF",
                        allowfullscreen: "true",
                        allowscriptaccess: "always"
                    }
                }, match ) ) );

            } else if( match.src ){

                preload = new IFrame( Object.merge( options, match, { frameborder: 0 }));
                //The actual loading starts only when the iframe gets added to the dom
                //By default, the iframe is hidden during loading. (see Viewer.less)
                document.body.adopt(preload);

            } else {

                preload = new Element("p.error[html=\"Error\"]");

            }

            done.delay(1); //sniff callback

        }
        //console.log("PRELOAD ", typeOf( preload ), preload );

    },

    preloads: function(elements, options, callback){

        var countdown = elements.length, //counting the preloaded objects !
            preloads = [],
            w = options.width,
            h = options.height;

        function preloadCallback(preload, width, height){
                preloads[preloads.length] = preload ;
                w = w.max(width);
                h = h.max(height.toInt());
                //console.log("preloads.length, w,h);
                if( !--countdown && callback ){ callback( preloads, w, h ); }
        }

        if( !countdown ){ return this.preload(elements, options, callback); }

        while( elements[0] ){

            this.preload( elements.shift(), options, preloadCallback);

        }

    }

};

var AdobeFlashPlayer = "clsid:D27CDB6E-AE6D-11cf-96B8-444553540000";
//Avoid Mixed Content errors by requesting the resource from whatever protocol
//the browser is viewing that current page
var protocol = location.protocol + "//";

Viewer.LIB.append([

    [".(bmp|gif|png|jpg|jpeg|tiff)$", function(url){
        return { img: url };
    }],

    //google viewer for common formats
    [".(tiff|pdf|doc|docx|xls|xlsx|ppt|pptx|zip|rar)$", function(url){
        return {
            src: protocol+"docs.google.com/viewer?embedded=true&url=" + encodeURIComponent(url), //url,
            width: 850,
            height: 500
        };
    }],

    //google maps
    //FIXME
    ["^https?://maps.google.com/maps?", function(url){

        //href="https://maps.google.com/maps?q=Atomium,%20City%20of%20Brussels,%20Belgium&amp;output=embed"
        return {
            src: url + "&output=embed",
            //src: url.replace("maps?","maps/embed?"),
            //src: url.replace("maps.google.com/maps?q=","www.google.com/maps/embed?pb="),
            width: 850,
            height: 500
        };

    }],

    ["^https?://www.google.com/maps/place/", function(url){

        //href=https://www.google.com/maps/place/Atomium/@50.894941,4.341547,17z/data=!3m1!4b1!4m2!3m1!1s0x0:0x5293071d68a63709?hl=en-US
        return {
            src: url.replace("www.google.com/maps/place/", "/maps.google.com/maps?q=") + "&output=embed",
            width: 850,
            height: 500
        };

    }],

    //add some mp3/audio player here

    ["facebook.com/v/", function(url){
          url = protocol+"www.facebook.com/v/" + url.split("v=")[1].split("&")[0];
          return {
            url: url,
            movie: url,
            classid: AdobeFlashPlayer,
            width: 756, //320,
            height: 424 //240
        };
    }],

    ["flickr.com", function(url){
        return {
            url: protocol+"www.flickr.com/apps/video/stewart.swf",
            classid: AdobeFlashPlayer,
            width: 500,
            height: 375,
            params: {flashvars: "photo_id=" + url.split("/")[5] + "&amp;show_info_box=true" }
        };
    }],

    ["youtube.com/watch", function(url){
        return {
            //src: "http://www.youtube.com/embed/"+url.split("v=")[1],
            url: protocol+"www.youtube.com/v/" + url.split("v=")[1],
            width: 640,
            height: 385  //385
        };
    }],

    //youtube playlist
    ["youtube.com/view", function(url){
        return {
            url: protocol+"www.youtube.com/p/" + url.split("p=")[1],
            width: 480,
            height: 385
        };
    }],


    ["dailymotion.com/video", function(url){
        return {
            src: protocol+"www.dailymotion.com/embed/video/" + url.split("/")[4].split("_")[0],
            //url:url,
            width: 480,
            height: 270 //381
        };
    }],

    ["metacafe.com/watch", function(url){
        //http://www.metacafe.com/watch/<id>/<title>/ => http://www.metacafe.com/fplayer/<id>/<title>.swf
        return {
            url: protocol+"www.metacafe.com/fplayer/" + url.split("/")[4] + "/.swf?playerVars=autoPlay=no",
            //url:"http://www.metacafe.com/fplayer/" + url.split("watch/")[1].slice(0,-1) +".swf?playerVars=autoPlay=no",
            width: 400,        //540    600
            height: 350        //304    338
        };
    }],

    ["vids.myspace.com", function(url){
        return {
            url: url,
            width: 425,        //540    600
            height: 360        //304    338
        };
    }],

    ["veoh.com/watch/", function(url){
        //url: "http://www.veoh.com/static/swf/webplayer/WebPlayer.swf?version=AFrontend.5.5.2.1001&permalinkId="+url.split("watch/")[1]+"&player=videodetailsembedded&videoAutoPlay=false&id=anonymous",
        return {
            url: protocol+"www.veoh.com/veohplayer.swf?permalinkId=" + url.split("watch/")[1] + "&player=videodetailsembedded&videoAutoPlay=false&id=anonymous",
            width: 410,
            height: 341
        };
    }],


    ["https?://www.ted.com/talks/", function(url){
    //http://www.ted.com/talks/doris_kim_sung_metal_that_breathes.html
    //<iframe src="http://embed.ted.com/talks/doris_kim_sung_metal_that_breathes.html" width="560" height="315" frameborder="0" scrolling="no" webkitallowfullscreen="" mozallowfullscreen="" allowfullscreen=""></iframe>
        return {
            src: protocol+"embed.ted.com/talks/" + url.split("/")[4],
            width: 853, //640, //560,
            height: 480, //360, //315
            scrolling: "no"
        };
    }],

    ["viddler.com/", function(url){
        return {
            url: url,
            classid: AdobeFlashPlayer,
            width: 545, //437,
            height: 451, //370,
            params: {id: "viddler_" + url.split("/")[4], movie: url }
        };
    }],

    ["https?://vimeo.com/", function(url){
        return {
            //html5 compatible --- use iframe
            //src: "http://player.vimeo.com/video/" + url.split("/")[3],
            url: protocol+"www.vimeo.com/moogaloop.swf?server=www.vimeo.com&amp;clip_id=" + url.split("/")[3],
            width: 640,
            height: 360
        };
    }],

    ["https?://jsfiddle.net/", function(url){
        return {
            src: url + "embedded/",
            width: 800,
            height: 250
        };
    }],


//http://codepen.io/loktar00/pen/fczsA
//<iframe src="//codepen.io/loktar00/embed/fczsA?height=350&amp;theme-id=1&amp;default-tab=result" ></iframe>

    ["https?://codepen.io/[^/]/pen/", function(url){
        return {
            src: url.replace("/pen/","/embed/")+"?height=400",
            width: 800,
            height: 400
        };
    }],

    ["https?://", function(url){
        return {
            //default : catch iframe urls ; allow scrolling of eg external site"s
            src: url
        };
    }]

]);

})();
