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
Behavior: Flip & Flop

Markup
(start code)
%%flip-h300-w500-none-default-primary-info-warning-success-danger
Front
----
Back
/%
(end)

DOM Structure
(start code)
div.flip-container
  div.flip
    div.face
        ..front face..
    div.face
        ..back face
(end)
*/
function Flip(element, options){

    var args = options.prefix.sliceArgs(element), arg,
        divider = "hr",
        css = {},
        frontback = ["default", "default"];

    //require exactly to blocks: one for front and one for back
    if( element.getChildren(divider).length == 1 ){

        while( args.length ){

            arg = args.pop();
            if( !arg.indexOf("w") /*index==0*/ ){ css.width = arg.slice(1).toInt(); }
            else if( !arg.indexOf("h") /*index==0*/ ){ css.height = arg.slice(1).toInt(); }
            else if( arg.test(/none|default|success|info|warning|danger/ )){ frontback[frontback.length] = arg; }

        }

        "div.flip-container".slick({ styles: css}).wraps(element);

        element
            .addClass(options.prefix)
            .grab( divider.slick(), "top") //add one extra group-start-element at the top
            .groupChildren( divider, "div.face", function(face){
                var clazz;
                if( frontback.length ){
                    clazz = frontback.pop();
                    face.ifClass( clazz != "none", clazz );
                }

            });

    }

}
